package com.shrishailacademy.security.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RecoveryStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-backed Bucket4j implementation (distributed rate limiter).
 *
 * Designed to be used behind a fallback wrapper so local dev/tests do not
 * require Redis.
 */
public class LettuceRedisBucket4jRateLimiterBackend implements RateLimiterBackend {

    private static final Logger log = LoggerFactory.getLogger(LettuceRedisBucket4jRateLimiterBackend.class);

    private final RedisClient redisClient;
    private final StatefulRedisConnection<byte[], byte[]> connection;
    private final ProxyManager<String> proxyManager;

    private final Map<String, BucketProxy> bucketCache = new ConcurrentHashMap<>();

    public LettuceRedisBucket4jRateLimiterBackend(RedisURI redisUri) {
        this.redisClient = RedisClient.create(redisUri);

        RedisCodec<byte[], byte[]> codec = ByteArrayCodec.INSTANCE;
        this.connection = redisClient.connect(codec);

        ProxyManager<byte[]> proxyManagerBytes = LettuceBasedProxyManager
                .builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();

        this.proxyManager = proxyManagerBytes.withMapper(k -> k.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public RateLimitResult tryConsume(String key, RateLimitPolicy policy) {
        String scopedKey = key + ":" + policy.capacity() + ":" + policy.window().toSeconds();
        BucketProxy bucket = bucketCache.computeIfAbsent(scopedKey, k -> proxyManager.builder()
                .withRecoveryStrategy(RecoveryStrategy.RECONSTRUCT)
                .build(k, policy::toBucketConfiguration));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return RateLimitResult.permit();
        }
        return RateLimitResult.deny(Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            log.debug("Failed to close Redis connection", e);
        }
        try {
            redisClient.shutdown();
        } catch (Exception e) {
            log.debug("Failed to shutdown Redis client", e);
        }
    }
}

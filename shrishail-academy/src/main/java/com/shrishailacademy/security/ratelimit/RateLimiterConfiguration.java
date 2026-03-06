package com.shrishailacademy.security.ratelimit;

import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfiguration {

    @Value("${rate.limit.backend:inmemory}")
    private String backend;

    @Bean
    public RateLimiterBackend rateLimiterBackend(RedisProperties redisProperties,
            InMemoryBucket4jRateLimiterBackend inMemory) {
        String mode = backend == null ? "inmemory" : backend.trim().toLowerCase();

        if ("inmemory".equals(mode)) {
            return inMemory;
        }

        if ("redis".equals(mode)) {
            // Explicit Redis mode: initialize eagerly; if Redis is down, application
            // startup will fail.
            LettuceRedisBucket4jRateLimiterBackend redisBackend = new LettuceRedisBucket4jRateLimiterBackend(
                    buildRedisUri(redisProperties));
            return new AutoClosingRedisBackend(redisBackend, redisBackend);
        }

        // auto: lazily try Redis; fall back to in-memory if Redis is down.
        LazyRedisBucket4jRateLimiterBackend lazyRedis = new LazyRedisBucket4jRateLimiterBackend(
                () -> buildRedisUri(redisProperties));
        RateLimiterBackend delegating = new FallbackRateLimiterBackend(lazyRedis, inMemory, Duration.ofSeconds(10));
        return new AutoClosingRedisBackend(delegating, lazyRedis);
    }

    private RedisURI buildRedisUri(RedisProperties props) {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(props.getHost())
                .withPort(props.getPort())
                .withTimeout(props.getTimeout() == null ? Duration.ofSeconds(2) : props.getTimeout());

        if (props.getPassword() != null && !props.getPassword().isEmpty()) {
            builder.withPassword(props.getPassword().toCharArray());
        }
        if (props.getSsl() != null && props.getSsl().isEnabled()) {
            builder.withSsl(true);
        }
        return builder.build();
    }

    private static final class AutoClosingRedisBackend implements RateLimiterBackend, DisposableBean {
        private final RateLimiterBackend delegate;
        private final Object redisResource;

        private AutoClosingRedisBackend(RateLimiterBackend delegate, Object redisResource) {
            this.delegate = delegate;
            this.redisResource = redisResource;
        }

        @Override
        public RateLimitResult tryConsume(String key, RateLimitPolicy policy) {
            return delegate.tryConsume(key, policy);
        }

        @Override
        public void destroy() {
            if (redisResource instanceof LettuceRedisBucket4jRateLimiterBackend eager) {
                eager.close();
            } else if (redisResource instanceof LazyRedisBucket4jRateLimiterBackend lazy) {
                lazy.closeIfInitialized();
            }
        }
    }
}

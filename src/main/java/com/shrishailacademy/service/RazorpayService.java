package com.shrishailacademy.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.shrishailacademy.model.Payment;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.enabled:false}")
    private boolean razorpayEnabled;

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    public RazorpayService(ObjectProvider<RazorpayClient> razorpayClientProvider) {
        this.razorpayClient = razorpayClientProvider.getIfAvailable();
    }

    public boolean isEnabled() {
        return razorpayEnabled
                && razorpayClient != null
                && StringUtils.hasText(razorpayKeyId)
                && StringUtils.hasText(razorpayKeySecret);
    }

    public String getKeyId() {
        return razorpayKeyId;
    }

    public Map<String, Object> createOrder(Payment payment) {
        if (!isEnabled()) {
            throw new IllegalStateException("Razorpay is not enabled for this environment");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", toPaise(payment.getAmount()));
            options.put("currency", "INR");
            options.put("receipt", payment.getReceiptNumber());

            JSONObject notes = new JSONObject();
            notes.put("paymentId", payment.getId());
            notes.put("courseId", payment.getCourse().getId());
            notes.put("userId", payment.getUser().getId());
            options.put("notes", notes);

            Order order = razorpayClient.orders.create(options);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orderId", String.valueOf(order.get("id")));
            result.put("amount", order.get("amount"));
            result.put("currency", String.valueOf(order.get("currency")));
            result.put("keyId", razorpayKeyId);
            result.put("paymentId", payment.getId());
            result.put("receiptNumber", payment.getReceiptNumber());
            return result;
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for paymentId={}: {}", payment.getId(), e.getMessage());
            throw new IllegalStateException("Unable to create Razorpay order", e);
        }
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (!isEnabled()) {
            return false;
        }

        String expectedSignature = hmacSha256Hex(orderId + "|" + paymentId, razorpayKeySecret);
        return expectedSignature.equals(signature);
    }

    private int toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).intValueExact();
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to verify Razorpay signature", e);
        }
    }
}
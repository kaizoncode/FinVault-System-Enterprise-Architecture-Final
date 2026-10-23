package com.finvault.module5_admin_audit.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * RateLimiterServiceImpl: Triển khai kiểm soát tần suất truy cập bằng Bucket4j
 *
 * Chính sách phân phối Token & Quản lý bộ nhớ:
 * - Sử dụng Caffeine In-Memory Cache với expireAfterAccess 10 phút và maximumSize 50,000
 *   để chống rò rỉ bộ nhớ (Memory Leak) do lưu hàng triệu IP lạ.
 * - Sức chứa tối đa (Capacity): 60 tokens cho mỗi IP / Client
 * - Tốc độ nạp (Refill Rate): 60 tokens / phút (1 token / giây)
 * - Tự động cách ly và ngăn chặn các cuộc tấn công DoS / Brute-force tần suất cao
 * ==============================================================================
 */
@Service
public class RateLimiterServiceImpl implements RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterServiceImpl.class);

    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(50000)
            .build();

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(60)
                .refillGreedy(60, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket resolveBucket(String key) {
        return bucketCache.get(key, k -> {
            if (k.startsWith("AUTH:") || k.startsWith("LOGIN:")) {
                return createAuthBucket();
            }
            return createNewBucket();
        });
    }

    @Override
    public boolean tryConsume(String key) {
        return tryConsume(key, 1);
    }

    @Override
    public boolean tryConsume(String key, long numTokens) {
        Bucket bucket = resolveBucket(key);
        boolean consumed = bucket.tryConsume(numTokens);
        if (!consumed) {
            log.warn("Rate limit vượt ngưỡng cho client key: [{}]", key);
        }
        return consumed;
    }

    @Override
    public long getAvailableTokens(String key) {
        Bucket bucket = resolveBucket(key);
        return bucket.getAvailableTokens();
    }
}

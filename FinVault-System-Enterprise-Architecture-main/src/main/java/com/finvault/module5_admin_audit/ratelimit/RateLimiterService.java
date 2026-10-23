package com.finvault.module5_admin_audit.ratelimit;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * RateLimiterService: Interface kiểm soát giới hạn tần suất yêu cầu (Token Bucket Algorithm)
 * ==============================================================================
 */
public interface RateLimiterService {

    boolean tryConsume(String key);

    boolean tryConsume(String key, long numTokens);

    long getAvailableTokens(String key);
}

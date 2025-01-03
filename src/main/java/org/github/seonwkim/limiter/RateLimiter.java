package org.github.seonwkim.limiter;

/**
 * 요구사항
 * - Bucket에 토큰을 주기적으로 채우자!
 * - 요청을 처리하기 위해서는 토큰이 필요
 */
public interface RateLimiter {

    /**
     * 처리하고자하는 작업
     *
     * @param runnable
     */
    void execute(Runnable runnable);
}

package org.github.seonwkim.limiter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketRateLimiter implements RateLimiter {

    private final long bucketSize;
    private final double tokenFillRatePerMilli;
    private final AtomicLong lastFillTimeMillis;
    private final AtomicLong tokenCount;
    private final ReentrantLock lock;

    /**
     * 10 tokens per second -> tokenFillRate should be 10, TimeUnit should be Seconds
     *
     * @param bucketSize
     * @param tokenFillRate
     * @param tokenFillRateUnit
     */
    public TokenBucketRateLimiter(
            long bucketSize,
            double tokenFillRate,
            TimeUnit tokenFillRateUnit) {
        this.bucketSize = bucketSize;

        // 10 tokens per second
        // tokenFillRate = 10
        // tokenFillRateUnit = seconds
        // tokenFillRateMillis = 10 / (seconds -> milliseconds)
        this.tokenFillRatePerMilli = tokenFillRate / tokenFillRateUnit.toMillis(1);
        this.lastFillTimeMillis = new AtomicLong(System.currentTimeMillis());
        this.tokenCount = new AtomicLong(bucketSize);
        this.lock = new ReentrantLock();
    }

    @Override
    public void execute(Runnable runnable) {
        var tokenCount = this.tokenCount.get();
        if (tokenCount == 0) {
            try {
                lock.lock();
                if (this.tokenCount.get() == 0) {
                    addTokensIfPossible();
                }
            } finally {
                lock.unlock();
            }

            if (this.tokenCount.get() > 0) {
                executeWithBuffer(runnable);
            } else {
                throw new IllegalStateException("No more token");
            }
        } else {
            if (this.tokenCount.compareAndSet(tokenCount, tokenCount - 1)) {
                runnable.run();
            } else {
                executeWithBuffer(runnable);
            }
        }
    }

    private void addTokensIfPossible() {
        long lastFillTimeMillis = this.lastFillTimeMillis.get();
        long nowMillis = System.currentTimeMillis();

        long currentTokenCount = this.tokenCount.get();
        long addableTokens = (long) ((nowMillis - lastFillTimeMillis) * tokenFillRatePerMilli);

        if (addableTokens > 0) {
            this.tokenCount.set(Math.min(bucketSize, currentTokenCount + addableTokens));
            System.out.println("token count: " + this.tokenCount.get());
            this.lastFillTimeMillis.set(System.currentTimeMillis());
        }
    }

    private void executeWithBuffer(Runnable runnable) {
        try {
            Thread.sleep(1000);
            runnable.run();
        } catch (Exception e) {
            // do nothing
        }
    }
}

package org.github.seonwkim;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenBucketRateLimiterTest {

    @Test
    void initial_bucket_size_test() throws InterruptedException {
        RateLimiter rateLimiter = new TokenBucketRateLimiter(
                10,
                10,
                TimeUnit.SECONDS
        );

        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = () -> System.out.println("counter value: " + counter.incrementAndGet());

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    rateLimiter.execute(task);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        assertEquals(10, counter.get());
    }

    @Test
    void exception_thrown_when_there_is_no_more_token() throws InterruptedException {
        RateLimiter rateLimiter = new TokenBucketRateLimiter(
                10, // bucket size
                10, // token fill rate
                TimeUnit.SECONDS
        );

        Runnable task = () -> System.out.println("Task executed");

        int threadCount = 11;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    rateLimiter.execute(task);
                } catch (IllegalStateException e) {
                    exceptionCount.incrementAndGet();
                    System.out.println("Exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        // Verify that an exception was thrown for the 11th task
        assertEquals(1, exceptionCount.get());
    }

    @Test
    void ten_tasks_per_second() throws InterruptedException {
        RateLimiter rateLimiter = new TokenBucketRateLimiter(
                10, // bucket size
                10, // token fill rate
                TimeUnit.SECONDS
        );

        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = () -> System.out.println("counter value: " + counter.incrementAndGet());

        int threadCount = 10;
        for (int iteration = 0; iteration < 5; iteration++) {
            CountDownLatch latch = new CountDownLatch(threadCount);
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        rateLimiter.execute(task);
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await(1000, TimeUnit.MILLISECONDS);

            // add 200 milliseconds as buffer
            Thread.sleep(1200);
        }

        assertEquals(50, counter.get());
    }

    @Test
    void one_token_per_hundred_milliseconds() throws InterruptedException {
        RateLimiter rateLimiter = new TokenBucketRateLimiter(
                10, // bucket size
                10, // token fill rate
                TimeUnit.SECONDS
        );

        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = () -> System.out.println("counter value: " + counter.incrementAndGet());

        int taskCount = 10;
        for (int i = 0; i < taskCount; i++) {
            rateLimiter.execute(task);
            Thread.sleep(110); // execute tasks serially with duration of 110 ms
        }

        // Wait for 1 second to ensure all tasks are executed
        Thread.sleep(1000);

        assertEquals(10, counter.get());
    }
}

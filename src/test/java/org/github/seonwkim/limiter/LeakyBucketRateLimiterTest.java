package org.github.seonwkim.limiter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeakyBucketRateLimiterTest {

    @Test
    void initial_queue_size_test() throws InterruptedException {
        LeakyBucketRateLimiter LeakyBucketRateLimiter = new LeakyBucketRateLimiter(
                10, // max queue size
                100, // delay
                TimeUnit.MILLISECONDS,
                10 // executor size
        );

        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = () -> System.out.println("counter value: " + counter.incrementAndGet());

        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            LeakyBucketRateLimiter.execute(() -> {
                task.run();
                latch.countDown();
            });
        }

        LeakyBucketRateLimiter.start();
        latch.await(2, TimeUnit.SECONDS);

        assertEquals(10, counter.get());
    }

    @Test
    void exception_thrown_when_queue_is_full() {
        LeakyBucketRateLimiter LeakyBucketRateLimiter = new LeakyBucketRateLimiter(
                10,
                100,
                TimeUnit.SECONDS,
                10
        );

        Runnable task = () -> System.out.println("Task executed");

        for (int i = 0; i < 10; i++) {
            LeakyBucketRateLimiter.execute(task);
        }

        assertThrows(IllegalArgumentException.class, () -> LeakyBucketRateLimiter.execute(task));
    }

    @Test
    void ten_tasks_per_second() throws InterruptedException {
        LeakyBucketRateLimiter LeakyBucketRateLimiter = new LeakyBucketRateLimiter(
                10,
                100,
                TimeUnit.MILLISECONDS,
                10
        );

        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = () -> System.out.println("counter value: " + counter.incrementAndGet());

        LeakyBucketRateLimiter.start();
        int taskCount = 10;
        for (int iteration = 0; iteration < 5; iteration++) {
            CountDownLatch latch = new CountDownLatch(taskCount);
            for (int i = 0; i < taskCount; i++) {
                LeakyBucketRateLimiter.execute(() -> {
                    task.run();
                    latch.countDown();
                });
            }

            latch.await(1200, TimeUnit.MILLISECONDS);
        }

        assertEquals(50, counter.get());
    }

    @Test
    void one_task_per_hundred_milliseconds() throws InterruptedException {
        LeakyBucketRateLimiter LeakyBucketRateLimiter = new LeakyBucketRateLimiter(
                10, // max queue size
                100, // delay in milliseconds
                TimeUnit.MILLISECONDS,
                1 // executor size
        );

        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = () -> System.out.println("counter value: " + counter.incrementAndGet());

        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            LeakyBucketRateLimiter.execute(() -> {
                task.run();
                latch.countDown();
            });
        }

        LeakyBucketRateLimiter.start();
        latch.await(2, TimeUnit.SECONDS);

        assertEquals(10, counter.get());
    }
}

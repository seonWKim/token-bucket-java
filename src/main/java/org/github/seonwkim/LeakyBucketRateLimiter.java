package org.github.seonwkim;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 요구사항
 * - Queue에 요청을 담음
 * - 주기적으로 queue의 요청을 수행
 */
public class LeakyBucketRateLimiter implements RateLimiter {

    private final Deque<Runnable> queue;
    private final int maxQueueSize;
    private final ScheduledExecutorService scheduler;
    private final long delay;
    private final TimeUnit timeUnit;
    private final Executor executor;

    public LeakyBucketRateLimiter(
            int maxQueueSize,
            long delay,
            TimeUnit timeUnit,
            int executorSize) {
        this.queue = new ArrayDeque<>(maxQueueSize);
        this.maxQueueSize = maxQueueSize;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.delay = delay;
        this.timeUnit = timeUnit;
        this.executor = Executors.newFixedThreadPool(executorSize);
    }

    public void start() {
        run();
    }

    private void run() {
        if (!queue.isEmpty()) {
            final Runnable task = queue.pollFirst();
            executor.execute(task);
        }

        scheduler.schedule(this::run, delay, timeUnit);
    }

    @Override
    public void execute(Runnable runnable) {
        final int queueSize = queue.size();
        if (queueSize >= this.maxQueueSize) {
            throw new IllegalArgumentException("Can't execute task");
        }

        this.queue.addLast(runnable);
    }
}

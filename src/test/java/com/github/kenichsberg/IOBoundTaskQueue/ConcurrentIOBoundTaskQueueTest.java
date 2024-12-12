package com.github.kenichsberg.IOBoundTaskQueue;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.String;
import java.util.Objects;
import java.util.concurrent.*;

class ConcurrentIOBoundTaskQueueTest {
    ConcurrentIOBoundTaskQueue concurrentIOBoundTaskQueue = new ConcurrentIOBoundTaskQueue();
    final BlockingQueue<String> results = new LinkedBlockingQueue<>();
    final RetryableTaskCallback<String> callback = new RetryableTaskCallback<>() {
        @Override
        public void onSuccess(RetryableTask<String> task, String result) {
            boolean isSuccess = result.equals("Success");
            if (isSuccess) {
                try {
                    results.put("SUCCESS");
                } catch(InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        @Override
        public void onFailure(RetryableTask<String> task, Throwable throwable) {
            try {
                results.put(throwable.getMessage());
                concurrentIOBoundTaskQueue.put(task);
            } catch(InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
    };

    @BeforeEach
    void setUp() {
        results.clear();
        concurrentIOBoundTaskQueue.start();
    }

    @AfterEach
    void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        concurrentIOBoundTaskQueue.shutdown();
    }

    @Test
    void put() throws InterruptedException {
        final Callable<String> f = () -> "Success";
        final RetryableTaskBuilder<String> builder = new RetryableTaskBuilder<>(f);
        final RetryableTask<String> task = builder.setCallback(callback).build();
        final boolean wasPut = concurrentIOBoundTaskQueue.offer(task);
        assumeTrue(wasPut);

        final String result = results.poll(1, TimeUnit.SECONDS);
        assumeTrue(Objects.equals(result, "SUCCESS"));
    }

    @Test
    void putWithRetries() throws InterruptedException {
        final Callable<String> f = () -> {
            throw(new Exception("Something is wrong!"));
        };
        final RetryableTaskBuilder<String> builder = new RetryableTaskBuilder<>(f);
        final RetryableTask<String> task = builder.setMaxRetries(3)
                .setDelayOnRetyrMs(10)
                .setCallback(callback)
                .build();
        final boolean wasPut = concurrentIOBoundTaskQueue.offer(task);
        assumeTrue(wasPut);

        String result;
        for (int i = 0; i < 4; i++){
            System.out.println(i);
            result = results.poll(1, TimeUnit.SECONDS);
            assumeTrue(Objects.equals(result, "Something is wrong!"));
        }
        assumeTrue(
                Objects.equals( results.poll(1, TimeUnit.SECONDS), null)
        );
    }

    @Test
    void stop() throws ExecutionException, InterruptedException, TimeoutException {
        concurrentIOBoundTaskQueue.shutdown();
        assumeTrue(concurrentIOBoundTaskQueue.dequeueingThreadFuture == null);
    }

}
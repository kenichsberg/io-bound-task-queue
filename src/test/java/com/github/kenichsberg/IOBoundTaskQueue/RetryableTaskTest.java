package com.github.kenichsberg.IOBoundTaskQueue;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class RetryableTaskTest {
    final BlockingQueue<String> acc = new LinkedBlockingQueue<>();
    final RetryableTaskCallback<String> callback = new RetryableTaskCallback<>() {
        @Override
        public void onSuccess(RetryableTask<String> task, String result) {
            boolean isSuccess = result.equals("Success");
            if (isSuccess) {
                try {
                    acc.put("SUCCESS");
                } catch(InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        @Override
        public void onFailure(RetryableTask<String> task, Throwable throwable) {
            try {
                acc.put(throwable.getMessage());
            } catch(InterruptedException e) {
                System.out.println(e.getMessage());
            }
            task.run();
        }
    };

    @BeforeEach
    void setUp() {
        acc.clear();
    }

    @Test
    void run() throws Exception {
        final Callable<String> f = () -> "Success";
        final RetryableTaskBuilder<String> builder = new RetryableTaskBuilder<>(f);
        final RetryableTask<String> task = builder.setCallback(callback).build();
        task.run();

        final String elem = acc.poll(1, TimeUnit.SECONDS);
        System.out.println(elem);
        assumeTrue(Objects.equals(elem, "SUCCESS"));

    }

    @Test
    void runWithFailures() throws Exception {
        final Callable<String> f = () -> {
            throw(new Exception("Something is wrong!"));
        };
        final RetryableTaskBuilder<String> builder = new RetryableTaskBuilder<>(f);
        final RetryableTask<String> task = builder.setMaxRetries(3)
                .setDelayOnRetyrMs(10)
                .setCallback(callback)
                .build();
        try {
            task.run();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        String result;
        for (int i = 0; i < 4; i++){
            System.out.println(i);
            result = acc.poll(1, TimeUnit.SECONDS);
            assumeTrue(Objects.equals(result, "Something is wrong!"));
        }
        final String elem = acc.poll(1, TimeUnit.SECONDS);
        System.out.println(elem);
        assumeTrue(Objects.equals(elem, null));

    }
}
package com.github.kenichsberg.IOBoundTaskQueue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class RetryableTask<V> implements Runnable {
    public final long delayOnRetryMs;
    public long attemptsAvailable;
    protected final Callable<V> callable;
    protected final RetryableTaskCallback<V> callback;
    protected final ExecutorService callbackExecutor;
    protected boolean isFirstAttempt;

    public RetryableTask(RetryableTaskBuilder<V> retryableTaskBuilder) {
        this.delayOnRetryMs = retryableTaskBuilder.getDelayOnRetryMs();
        this.attemptsAvailable = retryableTaskBuilder.getAttemptsAvailable();
        this.callable = retryableTaskBuilder.getCallable();
        this.callback = retryableTaskBuilder.getCallback();
        this.callbackExecutor = retryableTaskBuilder.getCallbackExecutor();
        this.isFirstAttempt = true;
    }


    @Override
    public void run() {
        if (attemptsAvailable-- <= 0) return;

        if (isFirstAttempt) {
            isFirstAttempt = false;
        } else {
            try {
                Thread.sleep(delayOnRetryMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        V result;
        try {
            result = callable.call();
        } catch (Exception e) {
            callbackExecutor.submit(
                    () -> callback.onFailure(this, e)
            );
            throw new RuntimeException(e);
        }

        callbackExecutor.submit(
                () -> callback.onSuccess(this, result)
        );
    }
}
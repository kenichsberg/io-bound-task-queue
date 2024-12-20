package com.github.kenichsberg.IOBoundTaskQueue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RetryableTaskBuilder<V> {
    final protected Callable<V> callable;
    protected ExecutorService callbackExecutor = Executors.newVirtualThreadPerTaskExecutor();
    protected long attemptsAvailable = 1;
    protected long delayOnRetryMs = 2000;
    protected RetryableTaskCallback<V> callback;

    public RetryableTaskBuilder(Callable<V> callable) {
        this.callable = callable;
    }


    public long getAttemptsAvailable() {
        return attemptsAvailable;
    }

    public long getDelayOnRetryMs() {
        return delayOnRetryMs;
    }

    public Callable<V> getCallable() {
        return callable;
    }

    public RetryableTaskCallback<V> getCallback() {
        return callback;
    }

    public ExecutorService getCallbackExecutor() {
        return callbackExecutor;
    }


    public RetryableTaskBuilder<V> setMaxRetries(long maxRetries){
        this.attemptsAvailable = ++maxRetries;
        return this;
    }

    public RetryableTaskBuilder<V> setDelayOnRetyrMs(long delayOnRetryMs){
        this.delayOnRetryMs = delayOnRetryMs;
        return this;
    }

    public RetryableTaskBuilder<V> setCallback(RetryableTaskCallback<V> callback){
        this.callback = callback;
        return this;
    }

    public RetryableTaskBuilder<V> setCallbackExecutor(ExecutorService callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
        return this;
    }


    public RetryableTask<V> build() {
        return new RetryableTask<>(this);
    }
}

package com.github.kenichsberg.IOBoundTaskQueue;

public interface RetryableTaskCallback<V> {
    void onSuccess(RetryableTask<V> task, V result);
    void onFailure(RetryableTask<V> task, Throwable throwable);
}

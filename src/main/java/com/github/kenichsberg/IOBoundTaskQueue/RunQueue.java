package com.github.kenichsberg.IOBoundTaskQueue;

import java.util.Collection;
import java.util.concurrent.*;

public interface RunQueue<E> extends BlockingQueue<E> {
    void start() throws InterruptedException;
    void shutdown() throws InterruptedException, ExecutionException;

    default E remove() {
        throw new UnsupportedOperationException("Not Supported");
    }
    default E poll() {
        throw new UnsupportedOperationException("Not Supported");
    }
    default E element() {
        throw new UnsupportedOperationException("Not Supported");
    }
    default E peek() {
        throw new UnsupportedOperationException("Not Supported");
    }
    default E take() {
        throw new UnsupportedOperationException("Not Supported");
    }
    default boolean remove(Object o) {
        throw new UnsupportedOperationException("Not Supported");
    }
    default E poll(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException("Not Supported");
    }
    default boolean containsAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Not Supported");
    }
    default boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Not Supported");
    }
    default boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Not Supported");
    }
    default void clear() {
        throw new UnsupportedOperationException("Not Supported");
    }
    default boolean contains(Object o) {
        throw new UnsupportedOperationException("Not Supported");
    }
}

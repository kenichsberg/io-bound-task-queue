package com.github.kenichsberg.IOBoundTaskQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.*;

public class ConcurrentIOBoundTaskQueue implements AutoDequeueingQueue<Runnable> {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final BlockingQueue<Runnable> queue;
    private final Semaphore sem;
    private Future<?> dequeueingThreadFuture;

    public ConcurrentIOBoundTaskQueue() {
        this(null);
    }

    public ConcurrentIOBoundTaskQueue(Integer concurrentExecutionLimit) {
        this.queue = new LinkedBlockingQueue<>();
        this.sem = (concurrentExecutionLimit == null) ?  null : new Semaphore(concurrentExecutionLimit);
    }

    /**
     * @return
     */
    @Override
    public synchronized boolean startDequeueing() {
        if (dequeueingThreadFuture != null) return false;

        dequeueingThreadFuture = executor.submit(() -> {
            while (!(Thread.currentThread().isInterrupted())) {
                try {
                    final Runnable task = queue.take();
                    runTask(task);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    dequeueingThreadFuture = null;
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        });
        return true;
    }


    private void runTask(Runnable task) throws InterruptedException {
        if (sem == null) {
            executor.submit(task);
            return;
        }

        executor.submit(() -> {
            try {
                sem.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                executor.submit(task);
            } finally {
                sem.release();
            }
        });
    }


    /**
     * @return boolean
     */
    @Override
    public synchronized boolean stopDequeueing() throws ExecutionException, InterruptedException, TimeoutException {
        if (dequeueingThreadFuture == null) return true;
        if (dequeueingThreadFuture.cancel(true) || dequeueingThreadFuture.isDone()) {
            dequeueingThreadFuture = null;
            return true;
        }

        dequeueingThreadFuture.get(3, TimeUnit.SECONDS);
        if (dequeueingThreadFuture.isDone()) {
            dequeueingThreadFuture = null;
            return true;
        }

        return false;
    }

    /**
     * @param task
     * @throws InterruptedException
     */
    @Override
    public void put(Runnable task) throws InterruptedException {
        queue.put(task);
    }

    /**
     * @param task
     * @return
     */
    @Override
    public boolean add(Runnable task) {
        return queue.add(task);
    }

    /**
     * @param task
     * @return
     */
    @Override
    public boolean offer(Runnable task) {
        return queue.offer(task);
    }

    /**
     * @param task
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean offer(Runnable task, long timeout, TimeUnit unit) throws InterruptedException {
        return queue.offer(task, timeout, unit);
    }

    /**
     * @return 
     */
    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    /**
     * @param collection 
     * @return
     */
    @Override
    public boolean addAll(Collection<? extends Runnable> collection) {
        return queue.addAll(collection);
    }

    /**
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        return queue.equals(o);
    }

    /**
     * @return
     */
    @Override
    public int hashCode() {
        return queue.hashCode();
    }

    /**
     * @return 
     */
    @Override
    public int size() {
        return queue.size();
    }

    /**
     * @return 
     */
    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * @return 
     */
    @Override
    public Iterator<Runnable> iterator() {
        return queue.iterator();
    }

    /**
     * @return 
     */
    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    /**
     * @param ts 
     * @param <T>
     * @return
     */
    @Override
    public <T> T[] toArray(T[] ts) {
        return queue.toArray(ts);
    }

    /**
     * @param collection 
     * @return
     */
    @Override
    public int drainTo(Collection<? super Runnable> collection) {
        return queue.drainTo(collection);
    }

    /**
     * @param collection 
     * @param i
     * @return
     */
    @Override
    public int drainTo(Collection<? super Runnable> collection, int i) {
        return queue.drainTo(collection, i);
    }


}

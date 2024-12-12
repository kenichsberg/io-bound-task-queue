package com.github.kenichsberg.IOBoundTaskQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.*;

public class ConcurrentIOBoundTaskQueue implements AutoDequeueingQueue<Runnable> {
    protected final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    protected final BlockingQueue<Runnable> queue;
    protected final Semaphore sem;
    protected Future<?> dequeueingThreadFuture;

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
    public synchronized void start() {
        if (dequeueingThreadFuture != null) return;

        dequeueingThreadFuture = executor.submit(() -> {
            while (!(Thread.currentThread().isInterrupted())) {
                try {
                    final Runnable task = queue.poll(1, TimeUnit.SECONDS);
                    if (task == null) continue;
                    runTask(task);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Exception occurred in dequeueing thread: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        return;
    }


    protected void runTask(Runnable task) {
        if (sem != null && !sem.tryAcquire()) {
            executor.submit(() -> {
                try {
                    Thread.sleep(3000);
                    queue.put(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Task re-enqueue interrupted: " + e.getMessage());                    System.err.println("Task re-enqueue interrupted: " + e.getMessage());                }
            });
            return;
        }

        try {
            executor.submit(task);
        } finally {
            if (sem != null) sem.release();
        }
    }


    @Override
    public synchronized void shutdown() throws ExecutionException, InterruptedException, TimeoutException {
        stopDequeueing();
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.err.println("Executor did not terminate within timeout. Forcing shutdown.");
            executor.shutdownNow();
        }
    }


    /**
     * @return boolean
     */
    protected synchronized void stopDequeueing() throws ExecutionException, InterruptedException, TimeoutException {
        if (dequeueingThreadFuture == null) return;
        dequeueingThreadFuture.cancel(true);

        try {
            dequeueingThreadFuture.get(10, TimeUnit.SECONDS);
        } catch (CancellationException ignored) {
            System.out.println("Dequeueing thread was canceled");
        } catch (TimeoutException ignored) {
            System.out.println("Could not terminate dequeueing thread until timeout");
        } finally {
            dequeueingThreadFuture = null;
        }

        return;
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

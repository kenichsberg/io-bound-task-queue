package com.github.kenichsberg.IOBoundTaskQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.*;

/**
 * A concurrent queue designed for executing I/O-bound tasks with optional limits on concurrent execution.
 * Utilizes virtual threads for efficiency and supports task re-enqueuing when resources are unavailable.
 */
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
     * Starts dequeueing tasks from the queue and executing them asynchronously.
     *
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
    }


    /**
     * Executes a task, enforcing concurrency limits if applicable.
     * If the semaphore is unavailable, the task is re-enqueued after a delay.
     *
     * @param task the task to execute.
     */
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


    /**
     * Shuts down the task queue and stops the dequeueing thread. Waits for active tasks to complete before shutting down.
     *
     * @throws ExecutionException if the dequeueing thread terminates with an exception.
     * @throws InterruptedException if the shutdown process is interrupted.
     */
    @Override
    public synchronized void shutdown() throws ExecutionException, InterruptedException {
        stopDequeueing();
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.err.println("Executor did not terminate within timeout. Forcing shutdown.");
            executor.shutdownNow();
        }
    }


    /**
     * Stops the dequeueing thread gracefully.
     *
     * @throws ExecutionException if the thread terminates with an exception.
     * @throws InterruptedException if the operation is interrupted.
     */
    protected synchronized void stopDequeueing() throws ExecutionException, InterruptedException {
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
    }


    /**
     * Adds a task to the queue, blocking if the queue is full.
     *
     * @param task the task to add.
     * @throws InterruptedException if the thread is interrupted while waiting to add the task.
     */
    @Override
    public void put(Runnable task) throws InterruptedException {
        queue.put(task);
    }


    /**
     * Adds a task to the queue, throwing an exception if the queue is full.
     *
     * @param task the task to add.
     * @return true if the task was added successfully.
     */
    @Override
    public boolean add(Runnable task) {
        return queue.add(task);
    }

    /**
     * Adds a task to the queue if space is available.
     *
     * @param task the task to add.
     * @return true if the task was added, false otherwise.
     */
    @Override
    public boolean offer(Runnable task) {
        return queue.offer(task);
    }


    /**
     * Attempts to add a task to the queue, waiting for space to become available within the timeout period.
     *
     * @param task the task to add.
     * @param timeout the maximum time to wait.
     * @param unit the time unit of the timeout argument.
     * @return true if the task was added, false otherwise.
     * @throws InterruptedException if interrupted while waiting.
     */
    @Override
    public boolean offer(Runnable task, long timeout, TimeUnit unit) throws InterruptedException {
        return queue.offer(task, timeout, unit);
    }


    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }


    @Override
    public boolean addAll(Collection<? extends Runnable> collection) {
        return queue.addAll(collection);
    }


    @Override
    public int size() {
        return queue.size();
    }


    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }


    @Override
    public Iterator<Runnable> iterator() {
        return queue.iterator();
    }


    @Override
    public Object[] toArray() {
        return queue.toArray();
    }


    @Override
    public <T> T[] toArray(T[] ts) {
        return queue.toArray(ts);
    }


    @Override
    public int drainTo(Collection<? super Runnable> collection) {
        return queue.drainTo(collection);
    }


    @Override
    public int drainTo(Collection<? super Runnable> collection, int i) {
        return queue.drainTo(collection, i);
    }


}

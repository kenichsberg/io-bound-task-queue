# IO Bound Task Queue

A task queue implementation with a flexible retry functionality.

Requires JDK >21, since it uses Virtual Threads to execute each task.

Each queue element is supposed to be an IO-bound task and is automatically dequeued and executed.

## Example
```java
// Without Semaphore
ConcurrentIOBoundTaskQueue concurrentIOBoundTaskQueue = new ConcurrentIOBoundTaskQueue();

// With Semaphore
ConcurrentIOBoundTaskQueue concurrentIOBoundTaskQueueWithSemaphore = new ConcurrentIOBoundTaskQueue(100);


final Runnable task = () -> MyTask.run(); // IO-bound task
        
// .put()
concurrentIOBoundTaskQueue.put(task);

// .offer()
concurrentIOBoundTaskQueue.offer(task, 500, TimeUnit.MILLISECONDS);
```

## With retries
`RetryableTask` can be used to retry tasks.

In `OnSuccess` and `OnFailure` of `RetryableTaskCallback`, the first parameter is the `RetryableTask` itself which is being executed.
You can rerun it on arbitrary conditions as you define.

```java
final Callable<String> callable = () -> "Success";
final RetryableTaskCallback<String> callback = new RetryableTaskCallback<>() {
    @Override
    public void onSuccess(RetryableTask<String> task, String result) {
        // ... do something
    }

    @Override
    public void onFailure(RetryableTask<String> task, Throwable throwable) {
        try {
            // retry via queue
            concurrentIOBoundTaskQueue.put(task);
        } catch(Exception e) {
            // ... do something
        }
    }
};

final RetryableTaskBuilder<String> builder = new RetryableTaskBuilder<>(callable);
final RetryableTask<String> task = builder.setMaxRetries(3)
        .setDelayOnRetyrMs(2000)
        .setCallback(callback)
        .build();


concurrentIOBoundTaskQueue.put(task);
```

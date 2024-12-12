# IO Bound Task Queue

A task queue implementation with a flexible retry functionality.

Requires JDK >21, since it uses Virtual Threads to execute each task.

Each queue element is supposed to be a `Runnable` object and is automatically dequeued and executed.

## Example
```java
// Without Semaphore
final IOBoundTaskQueue taskQueue = new IOBoundTaskQueue();

//// With Semaphore
// final IOBoundTaskQueue taskQueueWithSemaphore = new IOBoundTaskQueue(100);

// init
taskQueue.start();

// given IO-bound task
final Runnable task = () -> MyTask.run();
        
// .put()
taskQueue.put(task);

// .offer()
taskQueue.offer(task, 500, TimeUnit.MILLISECONDS);
```

## With retries
`RetryableTask` is available to retry tasks.

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
            taskQueue.put(task);
        } catch(Exception e) {
            // ... do something
        }
    }
};

final RetryableTaskBuilder<String> builder = new RetryableTaskBuilder<>(callable);

final RetryableTask<String> task =
        builder.setMaxRetries(3)
                .setDelayOnRetyrMs(2000)
                .setCallback(callback)
                .build();


taskQueue.put(task);
```

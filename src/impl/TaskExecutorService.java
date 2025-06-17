package impl;

import service.Main;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implements the TaskExecutor interface
 */
public class TaskExecutorService implements Main.TaskExecutor {

    private final ExecutorService executorService;
    private final BlockingQueue<TaskWrapper<?>> taskQueue;
    private final Map<UUID, ReentrantReadWriteLock> groupLocks = new ConcurrentHashMap<>();
    private final Thread dispatcherThread;

    public TaskExecutorService(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
        this.taskQueue = new LinkedBlockingQueue<>();

        dispatcherThread = new Thread(this::dispatchTasks);
        dispatcherThread.start();
    }

    /**
     * Runs continuously to take tasks from the queue and submit them to the thread pool.
     * Ensures FIFO execution order
     * Chooses readLock() or writeLock() based on TaskType
     */
    private void dispatchTasks() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                TaskWrapper<?> wrapper = taskQueue.take();

                executorService.submit(() -> {
                    UUID groupId = wrapper.task.taskGroup().groupUUID();
                    ReentrantReadWriteLock rwLock = groupLocks.computeIfAbsent(groupId, id -> new ReentrantReadWriteLock());
                    Lock lock = (wrapper.task.taskType() == Main.TaskType.READ)
                            ? rwLock.readLock()
                            : rwLock.writeLock();

                    lock.lock();
                    try {
                        System.out.println("Running "+ wrapper.task.taskType()+" task"+wrapper.task.taskUUID()+" from group " + groupId);
                        wrapper.future.complete(wrapper.task.taskAction().call());
                    } catch (Exception e) {
                        wrapper.future.completeExceptionally(e);
                    } finally {
                        lock.unlock();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public <T> Future<T> submitTask(Main.Task<T> task) {
        TaskWrapper<T> wrapper = new TaskWrapper<>(task);
        taskQueue.offer(wrapper);
        return wrapper.future;
    }

    public void shutdown() {
        dispatcherThread.interrupt();
        executorService.shutdown();
    }
    //Wraps Task<T> and a CompletableFuture<T> to manage result asynchronously
    private static class TaskWrapper<T> {
        final Main.Task<T> task;
        final CompletableFuture future;

        TaskWrapper(Main.Task<T> task) {
            this.task = task;
            this.future = new CompletableFuture<>();
        }
    }
}

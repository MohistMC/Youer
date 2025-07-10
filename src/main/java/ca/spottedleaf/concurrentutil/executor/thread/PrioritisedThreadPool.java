package ca.spottedleaf.concurrentutil.executor.thread;

import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.executor.queue.PrioritisedTaskQueue;
import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.concurrentutil.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class PrioritisedThreadPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrioritisedThreadPool.class);

    private final Consumer<Thread> threadModifier;
    private final COWArrayList<ExecutorGroup> executors = new COWArrayList<>(ExecutorGroup.class);
    private final COWArrayList<PrioritisedThread> threads = new COWArrayList<>(PrioritisedThread.class);
    private final COWArrayList<PrioritisedThread> aliveThreads = new COWArrayList<>(PrioritisedThread.class);

    private static final Priority HIGH_PRIORITY_NOTIFY_THRESHOLD = Priority.HIGH;
    private static final Priority QUEUE_SHUTDOWN_PRIORITY = Priority.HIGH;

    private boolean shutdown;

    public PrioritisedThreadPool(final Consumer<Thread> threadModifier) {
        this.threadModifier = threadModifier;

        if (threadModifier == null) {
            throw new NullPointerException("Thread factory may not be null");
        }
    }

    public Thread[] getAliveThreads() {
        final PrioritisedThread[] threads = this.aliveThreads.getArray();

        return Arrays.copyOf(threads, threads.length, Thread[].class);
    }

    public Thread[] getCoreThreads() {
        final PrioritisedThread[] threads = this.threads.getArray();

        return Arrays.copyOf(threads, threads.length, Thread[].class);
    }

    /**
     * Prevents creation of new queues, shutdowns all non-shutdown queues if specified
     */
    public void halt(final boolean shutdownQueues) {
        synchronized (this) {
            this.shutdown = true;
        }

        if (shutdownQueues) {
            for (final ExecutorGroup group : this.executors.getArray()) {
                for (final ExecutorGroup.ThreadPoolExecutor executor : group.executors.getArray()) {
                    executor.shutdown();
                }
            }
        }

        for (final PrioritisedThread thread : this.threads.getArray()) {
            thread.halt(false);
        }
    }

    /**
     * Waits until all threads in this pool have shutdown, or until the specified time has passed.
     * @param msToWait Maximum time to wait.
     * @return {@code false} if the maximum time passed, {@code true} otherwise.
     */
    public boolean join(final long msToWait) {
        try {
            return this.join(msToWait, false);
        } catch (final InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Waits until all threads in this pool have shutdown, or until the specified time has passed.
     * @param msToWait Maximum time to wait.
     * @return {@code false} if the maximum time passed, {@code true} otherwise.
     * @throws InterruptedException If this thread is interrupted.
     */
    public boolean joinInterruptable(final long msToWait) throws InterruptedException {
        return this.join(msToWait, true);
    }

    protected final boolean join(final long msToWait, final boolean interruptable) throws InterruptedException {
        final long nsToWait = msToWait * (1000 * 1000);
        final long start = System.nanoTime();
        final long deadline = start + nsToWait;
        boolean interrupted = false;
        try {
            for (final PrioritisedThread thread : this.aliveThreads.getArray()) {
                for (;;) {
                    if (!thread.isAlive()) {
                        break;
                    }
                    final long current = System.nanoTime();
                    if (current >= deadline && msToWait > 0L) {
                        return false;
                    }

                    try {
                        thread.join(msToWait <= 0L ? 0L : Math.max(1L, (deadline - current) / (1000 * 1000)));
                    } catch (final InterruptedException ex) {
                        if (interruptable) {
                            throw ex;
                        }
                        interrupted = true;
                    }
                }
            }

            return true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Shuts down this thread pool, optionally waiting for all tasks to be executed.
     * This function will invoke {@link PrioritisedExecutor#shutdown()} on all created executors on this
     * thread pool.
     * @param wait Whether to wait for tasks to be executed
     */
    public void shutdown(final boolean wait) {
        synchronized (this) {
            this.shutdown = true;
        }

        for (final ExecutorGroup group : this.executors.getArray()) {
            for (final ExecutorGroup.ThreadPoolExecutor executor : group.executors.getArray()) {
                executor.shutdown();
            }
        }


        for (final PrioritisedThread thread : this.threads.getArray()) {
            // none of these can be true or else NPE
            thread.close(false, false);
        }

        if (wait) {
            this.join(0L);
        }
    }

    private void die(final PrioritisedThread thread) {
        this.aliveThreads.remove(thread);
    }

    public void adjustThreadCount(final int threads) {
        synchronized (this) {
            if (this.shutdown) {
                return;
            }

            final PrioritisedThread[] currentThreads = this.threads.getArray();
            if (threads == currentThreads.length) {
                // no adjustment needed
                return;
            }

            if (threads < currentThreads.length) {
                // we need to trim threads
                for (int i = 0, difference = currentThreads.length - threads; i < difference; ++i) {
                    final PrioritisedThread remove = currentThreads[currentThreads.length - i - 1];

                    remove.halt(false);
                    this.threads.remove(remove);
                }
            } else {
                // we need to add threads
                for (int i = 0, difference = threads - currentThreads.length; i < difference; ++i) {
                    final PrioritisedThread thread = new PrioritisedThread();

                    this.threadModifier.accept(thread);
                    this.aliveThreads.add(thread);
                    this.threads.add(thread);

                    thread.start();
                }
            }
        }
    }

    private static int compareInsideGroup(final ExecutorGroup.ThreadPoolExecutor src, final Priority srcPriority,
                                          final ExecutorGroup.ThreadPoolExecutor dst, final Priority dstPriority) {
        final int priorityCompare = srcPriority.ordinal() - dstPriority.ordinal();
        if (priorityCompare != 0) {
            return priorityCompare;
        }

        final int parallelismCompare = src.currentParallelism - dst.currentParallelism;
        if (parallelismCompare != 0) {
            return parallelismCompare;
        }

        return TimeUtil.compareTimes(src.lastRetrieved, dst.lastRetrieved);
    }

    private static int compareOutsideGroup(final ExecutorGroup.ThreadPoolExecutor src, final Priority srcPriority,
                                           final ExecutorGroup.ThreadPoolExecutor dst, final Priority dstPriority) {
        if (src.getGroup().division == dst.getGroup().division) {
            // can only compare priorities inside the same division
            final int priorityCompare = srcPriority.ordinal() - dstPriority.ordinal();
            if (priorityCompare != 0) {
                return priorityCompare;
            }
        }

        final int parallelismCompare = src.getGroup().currentParallelism - dst.getGroup().currentParallelism;
        if (parallelismCompare != 0) {
            return parallelismCompare;
        }

        return TimeUtil.compareTimes(src.lastRetrieved, dst.lastRetrieved);
    }

    private ExecutorGroup.ThreadPoolExecutor obtainQueue() {
        final long time = System.nanoTime();
        synchronized (this) {
            ExecutorGroup.ThreadPoolExecutor ret = null;
            Priority retPriority = null;

            for (final ExecutorGroup executorGroup : this.executors.getArray()) {
                ExecutorGroup.ThreadPoolExecutor highest = null;
                Priority highestPriority = null;
                for (final ExecutorGroup.ThreadPoolExecutor executor : executorGroup.executors.getArray()) {
                    final int maxParallelism = executor.maxParallelism;
                    if (maxParallelism > 0 && executor.currentParallelism >= maxParallelism) {
                        continue;
                    }

                    final Priority priority = executor.getTargetPriority();

                    if (priority == null) {
                        continue;
                    }

                    if (highestPriority == null || compareInsideGroup(highest, highestPriority, executor, priority) > 0) {
                        highest = executor;
                        highestPriority = priority;
                    }
                }

                if (highest == null) {
                    continue;
                }

                if (ret == null || compareOutsideGroup(ret, retPriority, highest, highestPriority) > 0) {
                    ret = highest;
                    retPriority = highestPriority;
                }
            }

            if (ret != null) {
                ret.lastRetrieved = time;
                ++ret.currentParallelism;
                ++ret.getGroup().currentParallelism;
                return ret;
            }

            return ret;
        }
    }

    private void returnQueue(final ExecutorGroup.ThreadPoolExecutor executor) {
        synchronized (this) {
            --executor.currentParallelism;
            --executor.getGroup().currentParallelism;
        }

        if (executor.isShutdown() && executor.queue.hasNoScheduledTasks()) {
            executor.getGroup().executors.remove(executor);
        }
    }

    private void notifyAllThreads() {
        for (final PrioritisedThread thread : this.threads.getArray()) {
            thread.notifyTasks();
        }
    }

    public ExecutorGroup createExecutorGroup(final int division, final int flags) {
        synchronized (this) {
            if (this.shutdown) {
                throw new IllegalStateException("Queue is shutdown: " + this.toString());
            }

            final ExecutorGroup ret = new ExecutorGroup(division, flags);

            this.executors.add(ret);

            return ret;
        }
    }

    private final class PrioritisedThread extends PrioritisedQueueExecutorThread {

        private final AtomicBoolean alertedHighPriority = new AtomicBoolean();

        public PrioritisedThread() {
            super(null);
        }

        public boolean alertHighPriorityExecutor() {
            if (!this.notifyTasks()) {
                if (!this.alertedHighPriority.get()) {
                    this.alertedHighPriority.set(true);
                }
                return false;
            }

            return true;
        }

        private boolean isAlertedHighPriority() {
            return this.alertedHighPriority.get() && this.alertedHighPriority.getAndSet(false);
        }

        @Override
        protected void die() {
            PrioritisedThreadPool.this.die(this);
        }

        @Override
        protected boolean pollTasks() {
            boolean ret = false;

            for (;;) {
                if (this.halted) {
                    break;
                }

                final ExecutorGroup.ThreadPoolExecutor executor = PrioritisedThreadPool.this.obtainQueue();
                if (executor == null) {
                    break;
                }
                final long deadline = System.nanoTime() + executor.queueMaxHoldTime;
                do {
                    try {
                        if (this.halted || executor.halt) {
                            break;
                        }
                        if (!executor.executeTask()) {
                            // no more tasks, try next queue
                            break;
                        }
                        ret = true;
                    } catch (final Throwable throwable) {
                        LOGGER.error("Exception thrown from thread '" + this.getName() + "' in queue '" + executor.toString() + "'", throwable);
                    }
                } while (!this.isAlertedHighPriority() && System.nanoTime() <= deadline);

                PrioritisedThreadPool.this.returnQueue(executor);
            }


            return ret;
        }
    }

    public final class ExecutorGroup {

        private final AtomicLong subOrderGenerator = new AtomicLong();
        private final COWArrayList<ThreadPoolExecutor> executors = new COWArrayList<>(ThreadPoolExecutor.class);

        private final int division;
        private int currentParallelism;

        private ExecutorGroup(final int division, final int flags) {
            this.division = division;
        }

        public ThreadPoolExecutor[] getAllExecutors() {
            return this.executors.getArray().clone();
        }

        private PrioritisedThreadPool getThreadPool() {
            return PrioritisedThreadPool.this;
        }

        public ThreadPoolExecutor createExecutor(final int maxParallelism, final long queueMaxHoldTime, final int flags) {
            synchronized (PrioritisedThreadPool.this) {
                if (PrioritisedThreadPool.this.shutdown) {
                    throw new IllegalStateException("Queue is shutdown: " + PrioritisedThreadPool.this.toString());
                }

                final ThreadPoolExecutor ret = new ThreadPoolExecutor(maxParallelism, queueMaxHoldTime, flags);

                this.executors.add(ret);

                return ret;
            }
        }

        public final class ThreadPoolExecutor implements PrioritisedExecutor {

            private final PrioritisedTaskQueue queue = new PrioritisedTaskQueue();

            private volatile int maxParallelism;
            private final long queueMaxHoldTime;
            private volatile int currentParallelism;
            private volatile boolean halt;
            private long lastRetrieved = System.nanoTime();

            private ThreadPoolExecutor(final int maxParallelism, final long queueMaxHoldTime, final int flags) {
                this.maxParallelism = maxParallelism;
                this.queueMaxHoldTime = queueMaxHoldTime;
            }

            private ExecutorGroup getGroup() {
                return ExecutorGroup.this;
            }

            private boolean canNotify() {
                if (this.halt) {
                    return false;
                }

                final int max = this.maxParallelism;
                return max < 0 || this.currentParallelism < max;
            }

            private void notifyHighPriority() {
                if (!this.canNotify()) {
                    return;
                }
                for (final PrioritisedThread thread : this.getGroup().getThreadPool().threads.getArray()) {
                    if (thread.alertHighPriorityExecutor()) {
                        return;
                    }
                }
            }

            private void notifyScheduled() {
                if (!this.canNotify()) {
                    return;
                }
                for (final PrioritisedThread thread : this.getGroup().getThreadPool().threads.getArray()) {
                    if (thread.notifyTasks()) {
                        return;
                    }
                }
            }

            /**
             * Removes this queue from the thread pool without shutting the queue down or waiting for queued tasks to be executed
             */
            public void halt() {
                this.halt = true;

                ExecutorGroup.this.executors.remove(this);
            }

            /**
             * Returns whether this executor is scheduled to run tasks or is running tasks, otherwise it returns whether
             * this queue is not halted and not shutdown.
             */
            public boolean isActive() {
                if (this.halt) {
                    return this.currentParallelism > 0;
                } else {
                    if (!this.isShutdown()) {
                        return true;
                    }

                    return !this.queue.hasNoScheduledTasks();
                }
            }

            @Override
            public boolean shutdown() {
                if (!this.queue.shutdown()) {
                    return false;
                }

                if (this.queue.hasNoScheduledTasks()) {
                    ExecutorGroup.this.executors.remove(this);
                }

                return true;
            }

            @Override
            public boolean isShutdown() {
                return this.queue.isShutdown();
            }

            public void setMaxParallelism(final int maxParallelism) {
                this.maxParallelism = maxParallelism;
                // assume that we could have increased the parallelism
                if (this.getTargetPriority() != null) {
                    ExecutorGroup.this.getThreadPool().notifyAllThreads();
                }
            }

            Priority getTargetPriority() {
                final Priority ret = this.queue.getHighestPriority();
                if (!this.isShutdown()) {
                    return ret;
                }

                return ret == null ? QUEUE_SHUTDOWN_PRIORITY : Priority.max(ret, QUEUE_SHUTDOWN_PRIORITY);
            }

            @Override
            public long getTotalTasksScheduled() {
                return this.queue.getTotalTasksScheduled();
            }

            @Override
            public long getTotalTasksExecuted() {
                return this.queue.getTotalTasksExecuted();
            }

            @Override
            public long generateNextSubOrder() {
                return ExecutorGroup.this.subOrderGenerator.getAndIncrement();
            }

            @Override
            public boolean executeTask() {
                return this.queue.executeTask();
            }

            @Override
            public PrioritisedTask queueTask(final Runnable task) {
                final PrioritisedTask ret = this.createTask(task);

                ret.queue();

                return ret;
            }

            @Override
            public PrioritisedTask queueTask(final Runnable task, final Priority priority) {
                final PrioritisedTask ret = this.createTask(task, priority);

                ret.queue();

                return ret;
            }

            @Override
            public PrioritisedTask queueTask(final Runnable task, final Priority priority, final long subOrder) {
                final PrioritisedTask ret = this.createTask(task, priority, subOrder);

                ret.queue();

                return ret;
            }

            @Override
            public PrioritisedTask createTask(final Runnable task) {
                return this.createTask(task, Priority.NORMAL);
            }

            @Override
            public PrioritisedTask createTask(final Runnable task, final Priority priority) {
                return this.createTask(task, priority, this.generateNextSubOrder());
            }

            @Override
            public PrioritisedTask createTask(final Runnable task, final Priority priority, final long subOrder) {
                return new WrappedTask(this.queue.createTask(task, priority, subOrder));
            }

            private final class WrappedTask implements PrioritisedTask {

                private final PrioritisedTask wrapped;

                private WrappedTask(final PrioritisedTask wrapped) {
                    this.wrapped = wrapped;
                }

                @Override
                public PrioritisedExecutor getExecutor() {
                    return ThreadPoolExecutor.this;
                }

                @Override
                public boolean queue() {
                    if (this.wrapped.queue()) {
                        final Priority priority = this.getPriority();
                        if (priority != Priority.COMPLETING) {
                            if (priority.isHigherOrEqualPriority(HIGH_PRIORITY_NOTIFY_THRESHOLD)) {
                                ThreadPoolExecutor.this.notifyHighPriority();
                            } else {
                                ThreadPoolExecutor.this.notifyScheduled();
                            }
                        }
                        return true;
                    }

                    return false;
                }

                @Override
                public boolean isQueued() {
                    return this.wrapped.isQueued();
                }

                @Override
                public boolean cancel() {
                    return this.wrapped.cancel();
                }

                @Override
                public boolean execute() {
                    return this.wrapped.execute();
                }

                @Override
                public Priority getPriority() {
                    return this.wrapped.getPriority();
                }

                @Override
                public boolean setPriority(final Priority priority) {
                    if (this.wrapped.setPriority(priority)) {
                        if (priority.isHigherOrEqualPriority(HIGH_PRIORITY_NOTIFY_THRESHOLD)) {
                            ThreadPoolExecutor.this.notifyHighPriority();
                        }
                        return true;
                    }

                    return false;
                }

                @Override
                public boolean raisePriority(final Priority priority) {
                    if (this.wrapped.raisePriority(priority)) {
                        if (priority.isHigherOrEqualPriority(HIGH_PRIORITY_NOTIFY_THRESHOLD)) {
                            ThreadPoolExecutor.this.notifyHighPriority();
                        }
                        return true;
                    }

                    return false;
                }

                @Override
                public boolean lowerPriority(final Priority priority) {
                    return this.wrapped.lowerPriority(priority);
                }

                @Override
                public long getSubOrder() {
                    return this.wrapped.getSubOrder();
                }

                @Override
                public boolean setSubOrder(final long subOrder) {
                    return this.wrapped.setSubOrder(subOrder);
                }

                @Override
                public boolean raiseSubOrder(final long subOrder) {
                    return this.wrapped.raiseSubOrder(subOrder);
                }

                @Override
                public boolean lowerSubOrder(final long subOrder) {
                    return this.wrapped.lowerSubOrder(subOrder);
                }

                @Override
                public boolean setPriorityAndSubOrder(final Priority priority, final long subOrder) {
                    if (this.wrapped.setPriorityAndSubOrder(priority, subOrder)) {
                        if (priority.isHigherOrEqualPriority(HIGH_PRIORITY_NOTIFY_THRESHOLD)) {
                            ThreadPoolExecutor.this.notifyHighPriority();
                        }
                        return true;
                    }

                    return false;
                }
            }
        }
    }

    private static final class COWArrayList<E> {

        private volatile E[] array;

        public COWArrayList(final Class<E> clazz) {
            this.array = (E[])Array.newInstance(clazz, 0);
        }

        public E[] getArray() {
            return this.array;
        }

        public void add(final E element) {
            synchronized (this) {
                final E[] array = this.array;

                final E[] copy = Arrays.copyOf(array, array.length + 1);
                copy[array.length] = element;

                this.array = copy;
            }
        }

        public boolean remove(final E element) {
            synchronized (this) {
                final E[] array = this.array;
                int index = -1;
                for (int i = 0, len = array.length; i < len; ++i) {
                    if (array[i] == element) {
                        index = i;
                        break;
                    }
                }

                if (index == -1) {
                    return false;
                }

                final E[] copy = (E[])Array.newInstance(array.getClass().getComponentType(), array.length - 1);

                System.arraycopy(array, 0, copy, 0, index);
                System.arraycopy(array, index + 1, copy, index, (array.length - 1) - index);

                this.array = copy;
            }

            return true;
        }
    }
}

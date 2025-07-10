package ca.spottedleaf.concurrentutil.executor.queue;

import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import ca.spottedleaf.concurrentutil.util.Priority;
import java.lang.invoke.VarHandle;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class PrioritisedTaskQueue implements PrioritisedExecutor {

    /**
     * Required for tie-breaking in the queue
     */
    private final AtomicLong taskIdGenerator = new AtomicLong();
    private final AtomicLong scheduledTasks = new AtomicLong();
    private final AtomicLong executedTasks = new AtomicLong();
    private final AtomicLong subOrderGenerator = new AtomicLong();
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private final ConcurrentSkipListMap<PrioritisedQueuedTask.Holder, Boolean> tasks = new ConcurrentSkipListMap<>(PrioritisedQueuedTask.COMPARATOR);

    @Override
    public long getTotalTasksScheduled() {
        return this.scheduledTasks.get();
    }

    @Override
    public long getTotalTasksExecuted() {
        return this.executedTasks.get();
    }

    @Override
    public long generateNextSubOrder() {
        return this.subOrderGenerator.getAndIncrement();
    }

    @Override
    public boolean shutdown() {
        return !this.shutdown.getAndSet(true);
    }

    @Override
    public boolean isShutdown() {
        return this.shutdown.get();
    }

    public PrioritisedTask peekFirst() {
        final Map.Entry<PrioritisedQueuedTask.Holder, Boolean> firstEntry = this.tasks.firstEntry();
        return firstEntry == null ? null : firstEntry.getKey().task;
    }

    public Priority getHighestPriority() {
        final Map.Entry<PrioritisedQueuedTask.Holder, Boolean> firstEntry = this.tasks.firstEntry();
        return firstEntry == null ? null : Priority.getPriority(firstEntry.getKey().priority);
    }

    public boolean hasNoScheduledTasks() {
        final long executedTasks = this.executedTasks.get();
        final long scheduledTasks = this.scheduledTasks.get();

        return executedTasks == scheduledTasks;
    }

    public PrioritySubOrderPair getHighestPrioritySubOrder() {
        final Map.Entry<PrioritisedQueuedTask.Holder, Boolean> firstEntry = this.tasks.firstEntry();
        if (firstEntry == null) {
            return null;
        }

        final PrioritisedQueuedTask.Holder holder = firstEntry.getKey();

        return new PrioritySubOrderPair(Priority.getPriority(holder.priority), holder.subOrder);
    }

    public Runnable pollTask() {
        for (;;) {
            final Map.Entry<PrioritisedQueuedTask.Holder, Boolean> firstEntry = this.tasks.pollFirstEntry();
            if (firstEntry != null) {
                final PrioritisedQueuedTask.Holder task = firstEntry.getKey();
                task.markRemoved();
                if (!task.task.cancel()) {
                    continue;
                }
                return task.task.execute;
            }

            return null;
        }
    }

    @Override
    public boolean executeTask() {
        for (;;) {
            final Map.Entry<PrioritisedQueuedTask.Holder, Boolean> firstEntry = this.tasks.pollFirstEntry();
            if (firstEntry != null) {
                final PrioritisedQueuedTask.Holder task = firstEntry.getKey();
                task.markRemoved();
                if (!task.task.execute()) {
                    continue;
                }
                return true;
            }

            return false;
        }
    }

    @Override
    public PrioritisedTask createTask(final Runnable task) {
        return this.createTask(task, Priority.NORMAL, this.generateNextSubOrder());
    }

    @Override
    public PrioritisedTask createTask(final Runnable task, final Priority priority) {
        return this.createTask(task, priority, this.generateNextSubOrder());
    }

    @Override
    public PrioritisedTask createTask(final Runnable task, final Priority priority, final long subOrder) {
        return new PrioritisedQueuedTask(task, priority, subOrder);
    }

    @Override
    public PrioritisedTask queueTask(final Runnable task) {
        return this.queueTask(task, Priority.NORMAL, this.generateNextSubOrder());
    }

    @Override
    public PrioritisedTask queueTask(final Runnable task, final Priority priority) {
        return this.queueTask(task, priority, this.generateNextSubOrder());
    }

    @Override
    public PrioritisedTask queueTask(final Runnable task, final Priority priority, final long subOrder) {
        final PrioritisedQueuedTask ret = new PrioritisedQueuedTask(task, priority, subOrder);

        ret.queue();

        return ret;
    }

    private final class PrioritisedQueuedTask implements PrioritisedExecutor.PrioritisedTask {
        public static final Comparator<PrioritisedQueuedTask.Holder> COMPARATOR = (final PrioritisedQueuedTask.Holder t1, final PrioritisedQueuedTask.Holder t2) -> {
            final int priorityCompare = t1.priority - t2.priority;
            if (priorityCompare != 0) {
                return priorityCompare;
            }

            final int subOrderCompare = Long.compare(t1.subOrder, t2.subOrder);
            if (subOrderCompare != 0) {
                return subOrderCompare;
            }

            return Long.compare(t1.id, t2.id);
        };

        private static final class Holder {
            private final PrioritisedQueuedTask task;
            private final int priority;
            private final long subOrder;
            private final long id;

            private volatile boolean removed;
            private static final VarHandle REMOVED_HANDLE = ConcurrentUtil.getVarHandle(Holder.class, "removed", boolean.class);

            private Holder(final PrioritisedQueuedTask task, final int priority, final long subOrder,
                           final long id) {
                this.task = task;
                this.priority = priority;
                this.subOrder = subOrder;
                this.id = id;
            }

            /**
             * Returns true if marked as removed
             */
            public boolean markRemoved() {
                return !(boolean)REMOVED_HANDLE.getAndSet((Holder)this, (boolean)true);
            }
        }

        private final long id;
        private final Runnable execute;

        private Priority priority;
        private long subOrder;
        private Holder holder;

        public PrioritisedQueuedTask(final Runnable execute, final Priority priority, final long subOrder) {
            if (!Priority.isValidPriority(priority)) {
                throw new IllegalArgumentException("Invalid priority " + priority);
            }

            this.execute = execute;
            this.priority = priority;
            this.subOrder = subOrder;
            this.id = PrioritisedTaskQueue.this.taskIdGenerator.getAndIncrement();
        }

        @Override
        public PrioritisedExecutor getExecutor() {
            return PrioritisedTaskQueue.this;
        }

        @Override
        public boolean queue() {
            synchronized (this) {
                if (this.holder != null || this.priority == Priority.COMPLETING) {
                    return false;
                }

                if (PrioritisedTaskQueue.this.isShutdown()) {
                    throw new IllegalStateException("Queue is shutdown");
                }

                final Holder holder = new Holder(this, this.priority.priority, this.subOrder, this.id);
                this.holder = holder;

                PrioritisedTaskQueue.this.scheduledTasks.getAndIncrement();
                PrioritisedTaskQueue.this.tasks.put(holder, Boolean.TRUE);
            }

            if (PrioritisedTaskQueue.this.isShutdown()) {
                this.cancel();
                throw new IllegalStateException("Queue is shutdown");
            }


            return true;
        }

        @Override
        public boolean isQueued() {
            synchronized (this) {
                return this.holder != null && this.priority != Priority.COMPLETING;
            }
        }

        @Override
        public boolean cancel() {
            synchronized (this) {
                if (this.priority == Priority.COMPLETING) {
                    return false;
                }

                this.priority = Priority.COMPLETING;

                if (this.holder != null) {
                    if (this.holder.markRemoved()) {
                        PrioritisedTaskQueue.this.tasks.remove(this.holder);
                    }
                    PrioritisedTaskQueue.this.executedTasks.getAndIncrement();
                }

                return true;
            }
        }

        @Override
        public boolean execute() {
            final boolean increaseExecuted;

            synchronized (this) {
                if (this.priority == Priority.COMPLETING) {
                    return false;
                }

                this.priority = Priority.COMPLETING;

                if (increaseExecuted = (this.holder != null)) {
                    if (this.holder.markRemoved()) {
                        PrioritisedTaskQueue.this.tasks.remove(this.holder);
                    }
                }
            }

            try {
                this.execute.run();
                return true;
            } finally {
                if (increaseExecuted) {
                    PrioritisedTaskQueue.this.executedTasks.getAndIncrement();
                }
            }
        }

        @Override
        public Priority getPriority() {
            synchronized (this) {
                return this.priority;
            }
        }

        @Override
        public boolean setPriority(final Priority priority) {
            synchronized (this) {
                if (this.priority == Priority.COMPLETING || this.priority == priority) {
                    return false;
                }

                this.priority = priority;

                if (this.holder != null) {
                    if (this.holder.markRemoved()) {
                        PrioritisedTaskQueue.this.tasks.remove(this.holder);
                    }
                    this.holder = new Holder(this, priority.priority, this.subOrder, this.id);
                    PrioritisedTaskQueue.this.tasks.put(this.holder, Boolean.TRUE);
                }

                return true;
            }
        }

        @Override
        public boolean raisePriority(final Priority priority) {
            synchronized (this) {
                if (this.priority == Priority.COMPLETING || this.priority.isHigherOrEqualPriority(priority)) {
                    return false;
                }

                this.priority = priority;

                if (this.holder != null) {
                    if (this.holder.markRemoved()) {
                        PrioritisedTaskQueue.this.tasks.remove(this.holder);
                    }
                    this.holder = new Holder(this, priority.priority, this.subOrder, this.id);
                    PrioritisedTaskQueue.this.tasks.put(this.holder, Boolean.TRUE);
                }

                return true;
            }
        }

        @Override
        public boolean lowerPriority(Priority priority) {
            synchronized (this) {
                if (this.priority == Priority.COMPLETING || this.priority.isLowerOrEqualPriority(priority)) {
                    return false;
                }

                this.priority = priority;

                if (this.holder != null) {
                    if (this.holder.markRemoved()) {
                        PrioritisedTaskQueue.this.tasks.remove(this.holder);
                    }
                    this.holder = new Holder(this, priority.priority, this.subOrder, this.id);
                    PrioritisedTaskQueue.this.tasks.put(this.holder, Boolean.TRUE);
                }

                return true;
            }
        }

        @Override
        public long getSubOrder() {
            synchronized (this) {
                return this.subOrder;
            }
        }

        @Override
        public boolean setSubOrder(final long subOrder) {
            synchronized (this) {
                if (this.priority == Priority.COMPLETING || this.subOrder == subOrder) {
                    return false;
                }

                this.subOrder = subOrder;

                if (this.holder != null) {
                    if (this.holder.markRemoved()) {
                        PrioritisedTaskQueue.this.tasks.remove(this.holder);
                    }
                    this.holder = new Holder(this, priority.priority, this.subOrder, this.id);
                    PrioritisedTaskQueue.this.tasks.put(this.holder, Boolean.TRUE);
                }

                return true;
            }
        }

        @Override
        public boolean raiseSubOrder(long subOrder) {
            synchronized (this) {
                if (this.priority == Priority.COMPLETING || this.subOrder >= subOrder) {
                    return false;
                }

                this.subOrder = subOrder;

                if (this.holder != null) {
                    if (this.holder.markRemoved()) {
                        PrioritisedTaskQueue.this.tasks.remove(this.holder);
                    }
                    this.holder = new Holder(this, priority.priority, this.subOrder, this.id);
                    PrioritisedTaskQueue.this.tasks.put(this.holder, Boolean.TRUE);
                }

                return true;
            }
        }

        @Override
        public boolean lowerSubOrder(final long subOrder) {
            synchronized (this) {
                if (this.priority == Priority.COMPLETING || this.subOrder <= subOrder) {
                    return false;
                }

                this.subOrder = subOrder;

                if (this.holder != null) {
                    if (this.holder.markRemoved()) {
                        PrioritisedTaskQueue.this.tasks.remove(this.holder);
                    }
                    this.holder = new Holder(this, priority.priority, this.subOrder, this.id);
                    PrioritisedTaskQueue.this.tasks.put(this.holder, Boolean.TRUE);
                }

                return true;
            }
        }

        @Override
        public boolean setPriorityAndSubOrder(final Priority priority, final long subOrder) {
            synchronized (this) {
                if (this.priority == Priority.COMPLETING || (this.priority == priority && this.subOrder == subOrder)) {
                    return false;
                }

                this.priority = priority;
                this.subOrder = subOrder;

                if (this.holder != null) {
                    if (this.holder.markRemoved()) {
                        PrioritisedTaskQueue.this.tasks.remove(this.holder);
                    }
                    this.holder = new Holder(this, priority.priority, this.subOrder, this.id);
                    PrioritisedTaskQueue.this.tasks.put(this.holder, Boolean.TRUE);
                }

                return true;
            }
        }
    }

    public static record PrioritySubOrderPair(Priority priority, long subOrder) {}
}

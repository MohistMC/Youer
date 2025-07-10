package ca.spottedleaf.concurrentutil.executor;

import ca.spottedleaf.concurrentutil.util.Priority;

public interface PrioritisedExecutor {

    /**
     * Returns the number of tasks that have been scheduled are pending to be scheduled.
     */
    public long getTotalTasksScheduled();

    /**
     * Returns the number of tasks that have been executed.
     */
    public long getTotalTasksExecuted();

    /**
     * Generates the next suborder id.
     * @return The next suborder id.
     */
    public long generateNextSubOrder();

    /**
     * Executes the next available task.
     * <p>
     *     If there is a task with priority {@link Priority#BLOCKING} available, then that such task is executed.
     * </p>
     * <p>
     *     If there is a task with priority {@link Priority#IDLE} available then that task is only executed
     *     when there are no other tasks available with a higher priority.
     * </p>
     * <p>
     *     If there are no tasks that have priority {@link Priority#BLOCKING} or {@link Priority#IDLE}, then
     *     this function will be biased to execute tasks that have higher priorities.
     * </p>
     *
     * @return {@code true} if a task was executed, {@code false} otherwise
     * @throws IllegalStateException If the current thread is not allowed to execute a task
     */
    public boolean executeTask() throws IllegalStateException;

    /**
     * Prevent further additions to this executor. Attempts to add after this call has completed (potentially during) will
     * result in {@link IllegalStateException} being thrown.
     * <p>
     *     This operation is atomic with respect to other shutdown calls
     * </p>
     * <p>
     *     After this call has completed, regardless of return value, this executor will be shutdown.
     * </p>
     *
     * @return {@code true} if the executor was shutdown, {@code false} if it has shut down already
     * @see #isShutdown()
     */
    public boolean shutdown();

    /**
     * Returns whether this executor has shut down. Effectively, returns whether new tasks will be rejected.
     * This method does not indicate whether all the tasks scheduled have been executed.
     * @return Returns whether this executor has shut down.
     */
    public boolean isShutdown();

    /**
     * Queues or executes a task at {@link Priority#NORMAL} priority.
     * @param task The task to run.
     *
     * @throws IllegalStateException If this executor has shutdown.
     * @throws NullPointerException If the task is null
     * @return {@code null} if the current thread immediately executed the task, else returns the prioritised task
     *         associated with the parameter
     */
    public PrioritisedTask queueTask(final Runnable task);

    /**
     * Queues or executes a task.
     *
     * @param task The task to run.
     * @param priority The priority for the task.
     *
     * @throws IllegalStateException If this executor has shutdown.
     * @throws NullPointerException If the task is null
     * @throws IllegalArgumentException If the priority is invalid.
     * @return {@code null} if the current thread immediately executed the task, else returns the prioritised task
     *         associated with the parameter
     */
    public PrioritisedTask queueTask(final Runnable task, final Priority priority);

    /**
     * Queues or executes a task.
     *
     * @param task The task to run.
     * @param priority The priority for the task.
     * @param subOrder The task's suborder.
     *
     * @throws IllegalStateException If this executor has shutdown.
     * @throws NullPointerException If the task is null
     * @throws IllegalArgumentException If the priority is invalid.
     * @return {@code null} if the current thread immediately executed the task, else returns the prioritised task
     *         associated with the parameter
     */
    public PrioritisedTask queueTask(final Runnable task, final Priority priority, final long subOrder);

    /**
     * Creates, but does not queue or execute, a task at {@link Priority#NORMAL} priority.
     * @param task The task to run.
     *
     * @throws NullPointerException If the task is null
     * @return {@code null} if the current thread immediately executed the task, else returns the prioritised task
     *         associated with the parameter
     */
    public PrioritisedTask createTask(final Runnable task);

    /**
     * Creates, but does not queue or execute, a task at {@link Priority#NORMAL} priority.
     *
     * @param task The task to run.
     * @param priority The priority for the task.
     *
     * @throws NullPointerException If the task is null
     * @throws IllegalArgumentException If the priority is invalid.
     * @return {@code null} if the current thread immediately executed the task, else returns the prioritised task
     *         associated with the parameter
     */
    public PrioritisedTask createTask(final Runnable task, final Priority priority);

    /**
     * Creates, but does not queue or execute, a task at {@link Priority#NORMAL} priority.
     *
     * @param task The task to run.
     * @param priority The priority for the task.
     * @param subOrder The task's suborder.
     *
     * @throws NullPointerException If the task is null
     * @throws IllegalArgumentException If the priority is invalid.
     * @return {@code null} if the current thread immediately executed the task, else returns the prioritised task
     *         associated with the parameter
     */
    public PrioritisedTask createTask(final Runnable task, final Priority priority, final long subOrder);

    public static interface PrioritisedTask extends Cancellable {

        /**
         * Returns the executor associated with this task.
         * @return The executor associated with this task.
         */
        public PrioritisedExecutor getExecutor();

        /**
         * Causes a lazily queued task to become queued or executed
         *
         * @throws IllegalStateException If the backing executor has shutdown
         * @return {@code true} If the task was queued, {@code false} if the task was already queued/cancelled/executed
         */
        public boolean queue();

        /**
         * Returns whether this task has been queued and is not completing.
         * @return {@code true} If the task has been queued, {@code false} if the task has not been queued or is marked
         *         as completing.
         */
        public boolean isQueued();

        /**
         * Forces this task to be marked as completed.
         *
         * @return {@code true} if the task was cancelled, {@code false} if the task has already completed
         *         or is being completed.
         */
        @Override
        public boolean cancel();

        /**
         * Executes this task. This will also mark the task as completing.
         * <p>
         *     Exceptions thrown from the runnable will be rethrown.
         * </p>
         *
         * @return {@code true} if this task was executed, {@code false} if it was already marked as completed.
         */
        public boolean execute();

        /**
         * Returns the current priority. Note that {@link Priority#COMPLETING} will be returned
         * if this task is completing or has completed.
         */
        public Priority getPriority();

        /**
         * Attempts to set this task's priority level to the level specified.
         *
         * @param priority Specified priority level.
         *
         * @throws IllegalArgumentException If the priority is invalid
         * @return {@code true} if successful, {@code false} if this task is completing or has completed or the queue
         *         this task was scheduled on was shutdown, or if the priority was already at the specified level.
         */
        public boolean setPriority(final Priority priority);

        /**
         * Attempts to raise the priority to the priority level specified.
         *
         * @param priority Priority specified
         *
         * @throws IllegalArgumentException If the priority is invalid
         * @return {@code false} if the current task is completing, {@code true} if the priority was raised to the
         *          specified level or was already at the specified level or higher.
         */
        public boolean raisePriority(final Priority priority);

        /**
         * Attempts to lower the priority to the priority level specified.
         *
         * @param priority Priority specified
         *
         * @throws IllegalArgumentException If the priority is invalid
         * @return {@code false} if the current task is completing, {@code true} if the priority was lowered to the
         *          specified level or was already at the specified level or lower.
         */
        public boolean lowerPriority(final Priority priority);

        /**
         * Returns the suborder id associated with this task.
         * @return The suborder id associated with this task.
         */
        public long getSubOrder();

        /**
         * Sets the suborder id associated with this task. Ths function has no effect when this task
         * is completing or is completed.
         *
         * @param subOrder Specified new sub order.
         *
         * @return {@code true} if successful, {@code false} if this task is completing or has completed or the queue
         *         this task was scheduled on was shutdown, or if the current suborder is the same as the new sub order.
         */
        public boolean setSubOrder(final long subOrder);

        /**
         * Attempts to raise the suborder to the suborder specified.
         *
         * @param subOrder Specified new sub order.
         *
         * @return {@code false} if the current task is completing, {@code true} if the suborder was raised to the
         *          specified suborder or was already at the specified suborder or higher.
         */
        public boolean raiseSubOrder(final long subOrder);

        /**
         * Attempts to lower the suborder to the suborder specified.
         *
         * @param subOrder Specified new sub order.
         *
         * @return {@code false} if the current task is completing, {@code true} if the suborder was lowered to the
         *          specified suborder or was already at the specified suborder or lower.
         */
        public boolean lowerSubOrder(final long subOrder);

        /**
         * Sets the priority and suborder id associated with this task. Ths function has no effect when this task
         * is completing or is completed.
         *
         * @param priority Priority specified
         * @param subOrder Specified new sub order.
         * @return {@code true} if successful, {@code false} if this task is completing or has completed or the queue
         *         this task was scheduled on was shutdown, or if the current priority and suborder are the same as
         *         the parameters.
         */
        public boolean setPriorityAndSubOrder(final Priority priority, final long subOrder);
    }
}

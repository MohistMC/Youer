package ca.spottedleaf.concurrentutil.util;

public enum Priority {

    /**
     * Priority value indicating the task has completed or is being completed.
     * This priority cannot be used to schedule tasks.
     */
    COMPLETING(-1),

    /**
     * Absolute highest priority, should only be used for when a task is blocking a time-critical thread.
     */
    BLOCKING(),

    /**
     * Should only be used for urgent but not time-critical tasks.
     */
    HIGHEST(),

    /**
     * Two priorities above normal.
     */
    HIGHER(),

    /**
     * One priority above normal.
     */
    HIGH(),

    /**
     * Default priority.
     */
    NORMAL(),

    /**
     * One priority below normal.
     */
    LOW(),

    /**
     * Two priorities below normal.
     */
    LOWER(),

    /**
     * Use for tasks that should eventually execute, but are not needed to.
     */
    LOWEST(),

    /**
     * Use for tasks that can be delayed indefinitely.
     */
    IDLE();

    // returns whether the priority can be scheduled
    public static boolean isValidPriority(final Priority priority) {
        return priority != null && priority != priority.COMPLETING;
    }

    // returns the higher priority of the two
    public static Priority max(final Priority p1, final Priority p2) {
        return p1.isHigherOrEqualPriority(p2) ? p1 : p2;
    }

    // returns the lower priroity of the two
    public static Priority min(final Priority p1, final Priority p2) {
        return p1.isLowerOrEqualPriority(p2) ? p1 : p2;
    }

    public boolean isHigherOrEqualPriority(final Priority than) {
        return this.priority <= than.priority;
    }

    public boolean isHigherPriority(final Priority than) {
        return this.priority < than.priority;
    }

    public boolean isLowerOrEqualPriority(final Priority than) {
        return this.priority >= than.priority;
    }

    public boolean isLowerPriority(final Priority than) {
        return this.priority > than.priority;
    }

    public boolean isHigherOrEqualPriority(final int than) {
        return this.priority <= than;
    }

    public boolean isHigherPriority(final int than) {
        return this.priority < than;
    }

    public boolean isLowerOrEqualPriority(final int than) {
        return this.priority >= than;
    }

    public boolean isLowerPriority(final int than) {
        return this.priority > than;
    }

    public static boolean isHigherOrEqualPriority(final int priority, final int than) {
        return priority <= than;
    }

    public static boolean isHigherPriority(final int priority, final int than) {
        return priority < than;
    }

    public static boolean isLowerOrEqualPriority(final int priority, final int than) {
        return priority >= than;
    }

    public static boolean isLowerPriority(final int priority, final int than) {
        return priority > than;
    }

    static final Priority[] PRIORITIES = Priority.values();

    /** includes special priorities */
    public static final int TOTAL_PRIORITIES = PRIORITIES.length;

    public static final int TOTAL_SCHEDULABLE_PRIORITIES = TOTAL_PRIORITIES - 1;

    public static Priority getPriority(final int priority) {
        return PRIORITIES[priority + 1];
    }

    private static int priorityCounter;

    private static int nextCounter() {
        return priorityCounter++;
    }

    public final int priority;

    private Priority() {
        this(nextCounter());
    }

    private Priority(final int priority) {
        this.priority = priority;
    }
}
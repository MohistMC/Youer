package org.bukkit.command;

import com.mohistmc.youer.util.ExceptionHandler;

/**
 * Thrown when an unhandled exception occurs during the execution of a Command
 */
@SuppressWarnings("serial")
public class CommandException extends RuntimeException {

    /**
     * Creates a new instance of <code>CommandException</code> without detail
     * message.
     */
    public CommandException() {}

    /**
     * Constructs an instance of <code>CommandException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public CommandException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of <code>CommandException</code> with the
     * specified detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link #getCause()} method).
     */
    public CommandException(String msg, Throwable cause) {
        super(msg, cause);
        ExceptionHandler.onException(this);
    }
}

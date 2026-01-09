package com.heronix.scheduler.solver.exception;

/**
 * Base exception for scheduling solver errors
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-22
 */
public class SchedulingSolverException extends RuntimeException {

    public SchedulingSolverException(String message) {
        super(message);
    }

    public SchedulingSolverException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.heronix.scheduler.solver.exception;

/**
 * Exception thrown when solver execution fails
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-22
 */
public class SolverExecutionException extends SchedulingSolverException {

    public SolverExecutionException(String message) {
        super(message);
    }

    public SolverExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

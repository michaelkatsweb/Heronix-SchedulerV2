package com.heronix.scheduler.solver.exception;

/**
 * Exception thrown when scheduling problem input is invalid
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-22
 */
public class InvalidScheduleProblemException extends SchedulingSolverException {

    public InvalidScheduleProblemException(String message) {
        super(message);
    }

    public InvalidScheduleProblemException(String message, Throwable cause) {
        super(message, cause);
    }
}

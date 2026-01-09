package com.heronix.scheduler.solver.exception;

/**
 * Exception thrown when a schedule cannot be found
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since December 25, 2025
 */
public class ScheduleNotFoundException extends RuntimeException {

    private final Long scheduleId;

    /**
     * Constructor with schedule ID
     *
     * @param scheduleId ID of the schedule that was not found
     */
    public ScheduleNotFoundException(Long scheduleId) {
        super("Schedule not found with ID: " + scheduleId);
        this.scheduleId = scheduleId;
    }

    /**
     * Constructor with schedule ID and message
     *
     * @param scheduleId ID of the schedule that was not found
     * @param message Custom error message
     */
    public ScheduleNotFoundException(Long scheduleId, String message) {
        super(message);
        this.scheduleId = scheduleId;
    }

    /**
     * Constructor with schedule ID, message, and cause
     *
     * @param scheduleId ID of the schedule that was not found
     * @param message Custom error message
     * @param cause Underlying cause
     */
    public ScheduleNotFoundException(Long scheduleId, String message, Throwable cause) {
        super(message, cause);
        this.scheduleId = scheduleId;
    }

    /**
     * Get the schedule ID that was not found
     */
    public Long getScheduleId() {
        return scheduleId;
    }
}

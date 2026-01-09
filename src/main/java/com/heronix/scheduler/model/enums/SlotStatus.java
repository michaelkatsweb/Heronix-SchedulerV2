package com.heronix.scheduler.model.enums;

/**
 * Slot Status Enum
 * Location: src/main/java/com/eduscheduler/model/enums/SlotStatus.java
 * 
 * Represents the various states a schedule slot can be in
 * 
 * @author Heronix Scheduling System Team
 * @version 1.1.0 - Added SCHEDULED and CONFLICT statuses
 * @since 2025-10-11
 */
public enum SlotStatus {
    /**
     * Slot is active and scheduled normally
     */
    ACTIVE("Active"),

    /**
     * Slot is cancelled (e.g., class cancelled, field trip)
     */
    CANCELLED("Cancelled"),

    /**
     * Slot needs a substitute teacher
     */
    SUBSTITUTE_NEEDED("Substitute Needed"),

    /**
     * Substitute has been assigned
     */
    SUBSTITUTE_ASSIGNED("Substitute Assigned"),

    /**
     * Slot is temporarily blocked (e.g., school event)
     */
    BLOCKED("Blocked"),

    /**
     * Slot is pending approval
     */
    PENDING("Pending"),

    /**
     * Slot is a draft (not finalized)
     */
    DRAFT("Draft"),

    /**
     * Slot is archived (past term)
     */
    ARCHIVED("Archived"),

    /**
     * Slot has been successfully scheduled
     * Used during schedule generation to mark completed assignments
     */
    SCHEDULED("Scheduled"),

    /**
     * Slot has a scheduling conflict
     * Indicates double-booking, constraint violation, or resource conflicts
     */
    CONFLICT("Conflict");

    private final String displayName;

    SlotStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this status indicates the slot is usable
     */
    public boolean isUsable() {
        return this == ACTIVE || this == SUBSTITUTE_ASSIGNED || this == SCHEDULED;
    }

    /**
     * Check if this status indicates the slot needs attention
     */
    public boolean needsAttention() {
        return this == SUBSTITUTE_NEEDED || this == PENDING || this == CONFLICT;
    }

    /**
     * Check if this status indicates the slot is unavailable
     */
    public boolean isUnavailable() {
        return this == CANCELLED || this == BLOCKED || this == ARCHIVED;
    }

    /**
     * Check if this status indicates a problem
     */
    public boolean hasIssue() {
        return this == CONFLICT || this == SUBSTITUTE_NEEDED || this == CANCELLED;
    }
}
package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.RoomAssignmentType;
import com.heronix.scheduler.model.enums.UsagePattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Course Room Assignment Entity
 * Phase 6E: Multi-Room Courses
 *
 * Represents a room assignment for a course in multi-room scenarios.
 * Supports team teaching, lab/lecture splits, overflow rooms, and rotating patterns.
 *
 * @since Phase 6E - December 3, 2025
 */
@Entity
@Table(name = "course_room_assignments",
    indexes = {
        @Index(name = "idx_course_room_assignments_course", columnList = "course_id"),
        @Index(name = "idx_course_room_assignments_room", columnList = "room_id"),
        @Index(name = "idx_course_room_assignments_active", columnList = "active")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseRoomAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Course this room is assigned to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Room being assigned
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /**
     * Type of room assignment
     * PRIMARY, SECONDARY, OVERFLOW, BREAKOUT, ROTATING
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", length = 50, nullable = false)
    private RoomAssignmentType assignmentType = RoomAssignmentType.PRIMARY;

    /**
     * Usage pattern for this room
     * ALWAYS, ALTERNATING_DAYS, ODD_DAYS, EVEN_DAYS, FIRST_HALF, SECOND_HALF
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_pattern", length = 50, nullable = false)
    private UsagePattern usagePattern = UsagePattern.ALWAYS;

    /**
     * Priority of this room assignment (1 = highest priority)
     * Used to determine which room is preferred when conflicts occur
     */
    @Column(name = "priority")
    private Integer priority = 1;

    /**
     * Whether this assignment is currently active
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Additional notes about this room assignment
     * Example: "Lab activities only", "Video feed from primary room", etc.
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Specific days configuration for SPECIFIC_DAYS pattern
     * Comma-separated list of days: "MONDAY,WEDNESDAY,FRIDAY"
     */
    @Column(name = "specific_days", length = 200)
    private String specificDays;

    /**
     * Start time offset (minutes from period start) for time-based patterns
     * Used for FIRST_HALF, SECOND_HALF patterns
     */
    @Column(name = "start_offset_minutes")
    private Integer startOffsetMinutes;

    /**
     * Duration (minutes) for time-based patterns
     * Used for FIRST_HALF, SECOND_HALF patterns
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /**
     * Timestamp when this assignment was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this assignment was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if this assignment is the primary room
     */
    public boolean isPrimary() {
        return assignmentType == RoomAssignmentType.PRIMARY;
    }

    /**
     * Check if this assignment uses day-based pattern
     */
    public boolean isDayBasedPattern() {
        return usagePattern != null && usagePattern.isDayBased();
    }

    /**
     * Check if this assignment uses time-based pattern
     */
    public boolean isTimeBasedPattern() {
        return usagePattern != null && usagePattern.isTimeBased();
    }

    /**
     * Get display name for this assignment
     */
    public String getDisplayName() {
        if (room == null) return "No Room";

        StringBuilder sb = new StringBuilder();
        sb.append(room.getRoomNumber());

        if (assignmentType != null && assignmentType != RoomAssignmentType.PRIMARY) {
            sb.append(" (").append(assignmentType.getDisplayName()).append(")");
        }

        if (usagePattern != null && usagePattern != UsagePattern.ALWAYS) {
            sb.append(" - ").append(usagePattern.getDisplayName());
        }

        return sb.toString();
    }

    /**
     * Check if this assignment is active and usable
     */
    public boolean isUsable() {
        return Boolean.TRUE.equals(active) && room != null && course != null;
    }

    @Override
    public String toString() {
        return "CourseRoomAssignment{" +
                "id=" + id +
                ", course=" + (course != null ? course.getCourseCode() : "null") +
                ", room=" + (room != null ? room.getRoomNumber() : "null") +
                ", assignmentType=" + assignmentType +
                ", usagePattern=" + usagePattern +
                ", priority=" + priority +
                ", active=" + active +
                '}';
    }
}

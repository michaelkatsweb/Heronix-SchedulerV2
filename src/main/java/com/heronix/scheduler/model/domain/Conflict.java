package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.ConflictSeverity;
import com.heronix.scheduler.model.enums.ConflictType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Conflict Entity
 * Represents a scheduling conflict detected in the system
 *
 * Location: src/main/java/com/eduscheduler/model/domain/Conflict.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - Conflict Detection
 */
@Entity
@Table(name = "conflicts", indexes = {
    @Index(name = "idx_conflict_schedule", columnList = "schedule_id"),
    @Index(name = "idx_conflict_severity", columnList = "severity"),
    @Index(name = "idx_conflict_type", columnList = "conflict_type"),
    @Index(name = "idx_conflict_status", columnList = "is_resolved"),
    @Index(name = "idx_conflict_detected", columnList = "detected_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // CONFLICT CLASSIFICATION
    // ========================================================================

    /**
     * Type of conflict
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_type", nullable = false, length = 50)
    private ConflictType conflictType;

    /**
     * Severity level
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ConflictSeverity severity;

    /**
     * Category (derived from type, for quick filtering)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private ConflictType.ConflictCategory category;

    // ========================================================================
    // CONFLICT DESCRIPTION
    // ========================================================================

    /**
     * Short title/summary of the conflict
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Detailed description of the conflict
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Suggested resolution
     */
    @Column(name = "suggested_resolution", columnDefinition = "TEXT")
    private String suggestedResolution;

    // ========================================================================
    // CONFLICT RELATIONSHIPS
    // ========================================================================

    /**
     * Associated schedule
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    /**
     * Affected schedule slots (if applicable)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "conflict_schedule_slots",
        joinColumns = @JoinColumn(name = "conflict_id"),
        inverseJoinColumns = @JoinColumn(name = "schedule_slot_id")
    )
    @Builder.Default
    private List<ScheduleSlot> affectedSlots = new ArrayList<>();

    /**
     * Affected teachers (if applicable)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "conflict_teachers",
        joinColumns = @JoinColumn(name = "conflict_id"),
        inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    @Builder.Default
    private List<Teacher> affectedTeachers = new ArrayList<>();

    /**
     * Affected students (if applicable)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "conflict_students",
        joinColumns = @JoinColumn(name = "conflict_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Builder.Default
    private List<Student> affectedStudents = new ArrayList<>();

    /**
     * Affected rooms (if applicable)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "conflict_rooms",
        joinColumns = @JoinColumn(name = "conflict_id"),
        inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    @Builder.Default
    private List<Room> affectedRooms = new ArrayList<>();

    /**
     * Affected courses (if applicable)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "conflict_courses",
        joinColumns = @JoinColumn(name = "conflict_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @Builder.Default
    private List<Course> affectedCourses = new ArrayList<>();

    // ========================================================================
    // CONFLICT STATUS
    // ========================================================================

    /**
     * Is the conflict resolved?
     */
    @Column(name = "is_resolved", nullable = false)
    @Builder.Default
    private Boolean isResolved = false;

    /**
     * Resolution method/notes
     */
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    /**
     * User who resolved the conflict
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    /**
     * When the conflict was resolved
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ========================================================================
    // CONFLICT METADATA
    // ========================================================================

    /**
     * When the conflict was detected
     */
    @Column(name = "detected_at", nullable = false)
    @Builder.Default
    private LocalDateTime detectedAt = LocalDateTime.now();

    /**
     * Detection method (auto, manual, import)
     */
    @Column(name = "detection_method", length = 50)
    private String detectionMethod;

    /**
     * Additional metadata (JSON)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Is this conflict ignored by user?
     */
    @Column(name = "is_ignored", nullable = false)
    @Builder.Default
    private Boolean isIgnored = false;

    /**
     * Reason for ignoring
     */
    @Column(name = "ignore_reason", length = 500)
    private String ignoreReason;

    // ========================================================================
    // TIMESTAMPS
    // ========================================================================

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Mark conflict as resolved
     */
    public void resolve(User user, String notes) {
        this.isResolved = true;
        this.resolvedBy = user;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
    }

    /**
     * Mark conflict as ignored
     */
    public void ignore(String reason) {
        this.isIgnored = true;
        this.ignoreReason = reason;
    }

    /**
     * Unignore conflict
     */
    public void unignore() {
        this.isIgnored = false;
        this.ignoreReason = null;
    }

    /**
     * Check if conflict is active (not resolved and not ignored)
     */
    public boolean isActive() {
        return !isResolved && !isIgnored;
    }

    /**
     * Get conflict summary for display
     */
    public String getSummary() {
        return String.format("%s [%s] - %s",
            severity.getIcon(),
            conflictType.getDisplayName(),
            title);
    }

    /**
     * Get affected entities count
     */
    public int getAffectedEntitiesCount() {
        return affectedSlots.size() +
               affectedTeachers.size() +
               affectedStudents.size() +
               affectedRooms.size() +
               affectedCourses.size();
    }

    /**
     * Get conflict age in days
     */
    public long getAgeInDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(detectedAt, LocalDateTime.now());
    }
}

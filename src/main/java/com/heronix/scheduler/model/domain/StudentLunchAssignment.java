package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Student Lunch Assignment Entity (Scheduler Shadow Copy)
 *
 * Maps a student to a specific lunch wave within a schedule.
 * Minimal representation for scheduling purposes only.
 * Authoritative data lives in SIS microservice.
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "student_lunch_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentLunchAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_ref_id")
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lunch_wave_id")
    private LunchWave lunchWave;

    @Column(name = "assignment_method", length = 100)
    private String assignmentMethod;

    @Builder.Default
    @Column(name = "manual_override")
    private Boolean manualOverride = false;

    @Builder.Default
    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    /**
     * Get is locked flag
     */
    public Boolean getIsLocked() {
        return isLocked != null ? isLocked : false;
    }

    /**
     * Get manual override flag
     */
    public Boolean getManualOverride() {
        return manualOverride != null ? manualOverride : false;
    }

    /**
     * Get priority
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Set priority
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * Lock this assignment
     */
    public void lock(String lockedBy) {
        this.isLocked = true;
        this.lastModifiedBy = lockedBy;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Unlock this assignment
     */
    public void unlock(String unlockedBy) {
        this.isLocked = false;
        this.lastModifiedBy = unlockedBy;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Mark as manual override
     */
    public void markAsManualOverride() {
        this.manualOverride = true;
    }

    /**
     * Update modification tracking
     */
    public void updateModification(String modifiedBy) {
        this.lastModifiedBy = modifiedBy;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Set last modified by
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Set last modified at
     */
    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    /**
     * Set manual override flag
     */
    public void setManualOverride(Boolean manualOverride) {
        this.manualOverride = manualOverride;
    }
}

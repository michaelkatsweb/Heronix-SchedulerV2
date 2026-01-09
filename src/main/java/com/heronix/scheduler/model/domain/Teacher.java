package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Teacher Entity (Scheduler Shadow Copy)
 *
 * This is a minimal entity representation for scheduling purposes only.
 * The authoritative Teacher data lives in the SIS microservice.
 * This class is populated from TeacherDTO fetched via SISApiClient.
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", unique = true, length = 50)
    private String employeeId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 150)
    private String email;

    @Column(length = 100)
    private String department;

    @Column(name = "active")
    private Boolean active = true;

    // Certifications & Qualifications
    @ElementCollection
    @CollectionTable(name = "teacher_certifications", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "certification")
    private List<String> certifications = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "teacher_qualifications", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "qualification")
    private List<String> qualifications = new ArrayList<>();

    // Scheduling Preferences
    @Column(name = "home_room_id")
    private Long homeRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_room_ref_id")
    private Room homeRoom;

    @ElementCollection
    @CollectionTable(name = "teacher_preferred_rooms", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "room_id")
    private List<Long> preferredRoomIds = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "teacher_room_preferences",
        joinColumns = @JoinColumn(name = "teacher_id"),
        inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    private List<Room> roomPreferences = new ArrayList<>();

    @Column(name = "restricted_to_rooms")
    private Boolean restrictedToRooms = false;

    // Scheduling limits
    @Column(name = "max_periods_per_day")
    private Integer maxPeriodsPerDay;

    @Column(name = "max_hours_per_week")
    private Integer maxHoursPerWeek;

    // Lunch Assignment
    @Column(name = "lunch_wave_id")
    private Long lunchWaveId;

    @Column(name = "has_supervision_duty")
    private Boolean hasSupervisionDuty = false;

    @Column(name = "is_duty_free")
    private Boolean isDutyFree = false;

    @Column(name = "has_duty_during_other_waves")
    private Boolean hasDutyDuringOtherWaves = false;

    // Primary Campus (for multi-campus support)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_campus_id")
    private Campus primaryCampus;

    // For scheduling purposes - courses this teacher teaches
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "assignedTeacher")
    private List<Course> courses = new ArrayList<>();

    // Scheduling constraints
    @ElementCollection
    @CollectionTable(name = "teacher_unavailable_time_blocks", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "time_block")
    private List<String> unavailableTimeBlocks = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "teacher_certified_subjects", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "subject")
    private List<String> certifiedSubjects = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "teacher_subject_certifications", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "subject")
    private List<String> subjectCertifications = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "teacher_special_assignments", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "assignment")
    private List<String> specialAssignments = new ArrayList<>();

    // Audit fields
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "manual_override")
    private Boolean manualOverride = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get display name (alias for getFullName for compatibility)
     */
    public String getName() {
        return getFullName();
    }

    /**
     * Check if teacher is active
     */
    public Boolean getActive() {
        return active != null ? active : true;
    }

    /**
     * Check if teacher is active (boolean primitive)
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    /**
     * Get count of courses taught by this teacher
     */
    public int getCourseCount() {
        return courses != null ? courses.size() : 0;
    }

    /**
     * Get certified subjects
     */
    public List<String> getCertifiedSubjects() {
        return certifiedSubjects != null ? certifiedSubjects : new ArrayList<>();
    }

    /**
     * Get unavailable time blocks
     */
    public List<String> getUnavailableTimeBlocks() {
        return unavailableTimeBlocks != null ? unavailableTimeBlocks : new ArrayList<>();
    }

    /**
     * Set unavailable time blocks
     */
    public void setUnavailableTimeBlocks(List<String> unavailableTimeBlocks) {
        this.unavailableTimeBlocks = unavailableTimeBlocks;
    }

    /**
     * Get special assignments
     */
    public List<String> getSpecialAssignments() {
        return specialAssignments != null ? specialAssignments : new ArrayList<>();
    }

    /**
     * Check if teacher has supervision duty
     */
    public Boolean getHasSupervisionDuty() {
        return hasSupervisionDuty != null ? hasSupervisionDuty : false;
    }

    /**
     * Check if teacher has duty-free lunch
     */
    public Boolean getIsDutyFree() {
        return isDutyFree != null ? isDutyFree : false;
    }

    /**
     * Assign supervision duty to teacher
     */
    public void assignSupervisionDuty() {
        this.hasSupervisionDuty = true;
        this.isDutyFree = false;
    }

    /**
     * Remove supervision duty from teacher
     */
    public void removeSupervisionDuty() {
        this.hasSupervisionDuty = false;
        this.isDutyFree = true;
    }

    /**
     * Set duty during other waves flag
     */
    public void setHasDutyDuringOtherWaves(Boolean hasDutyDuringOtherWaves) {
        this.hasDutyDuringOtherWaves = hasDutyDuringOtherWaves;
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

    /**
     * Get home room
     */
    public Room getHomeRoom() {
        return homeRoom;
    }

    /**
     * Get room preferences
     */
    public List<Room> getRoomPreferences() {
        return roomPreferences != null ? roomPreferences : new ArrayList<>();
    }

    /**
     * Get subject certifications
     */
    public List<String> getSubjectCertifications() {
        return subjectCertifications != null ? subjectCertifications : new ArrayList<>();
    }

    /**
     * Get maximum periods per day
     */
    public Integer getMaxPeriodsPerDay() {
        return maxPeriodsPerDay;
    }

    /**
     * Get maximum hours per week
     */
    public Integer getMaxHoursPerWeek() {
        return maxHoursPerWeek;
    }

    /**
     * Check if teacher has room preferences
     */
    public boolean hasRoomPreferences() {
        return roomPreferences != null && !roomPreferences.isEmpty();
    }

    /**
     * Check if teacher is restricted to specific rooms
     */
    public boolean isRestrictedToRooms() {
        return Boolean.TRUE.equals(restrictedToRooms);
    }

    /**
     * Check if teacher prefers a specific room
     */
    public boolean prefersRoom(Room room) {
        if (roomPreferences == null || room == null) {
            return false;
        }
        return roomPreferences.stream()
            .anyMatch(r -> r.getId().equals(room.getId()));
    }

    /**
     * Check if teacher can use a specific room
     */
    public boolean canUseRoom(Room room) {
        if (room == null) {
            return false;
        }
        // If not restricted to rooms, can use any room
        if (!isRestrictedToRooms()) {
            return true;
        }
        // If restricted, check if room is in preferences
        return prefersRoom(room);
    }

    /**
     * Check if teacher has certification for a subject
     */
    public boolean hasCertificationForSubject(String subject) {
        if (subject == null) {
            return false;
        }
        if (subjectCertifications != null && subjectCertifications.contains(subject)) {
            return true;
        }
        if (certifiedSubjects != null && certifiedSubjects.contains(subject)) {
            return true;
        }
        return certifications != null && certifications.stream()
            .anyMatch(cert -> cert.toLowerCase().contains(subject.toLowerCase()));
    }

    /**
     * Check if teacher is available at a specific time
     */
    public boolean isAvailableAt(String timeBlock) {
        if (timeBlock == null || unavailableTimeBlocks == null) {
            return true;
        }
        return !unavailableTimeBlocks.contains(timeBlock);
    }

    /**
     * Get notes about this teacher
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Set notes about this teacher
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Get planning period for scheduling
     */
    public String getPlanningPeriod() {
        // Return default planning period
        return "DEFAULT";
    }

    /**
     * Check if teacher has duty-free lunch for a specific wave
     * @param checkAllWaves if true, checks all waves; if false, checks current assignment
     * @return true if teacher has duty-free lunch
     */
    public boolean isDutyFree(boolean checkAllWaves) {
        // Default implementation - can be enhanced with actual duty tracking
        return false;
    }
}

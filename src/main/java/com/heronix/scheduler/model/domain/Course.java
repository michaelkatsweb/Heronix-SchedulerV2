package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Course Entity (Scheduler Shadow Copy)
 *
 * This is a minimal entity representation for scheduling purposes only.
 * The authoritative Course data lives in the SIS microservice.
 * This class is populated from CourseDTO fetched via SISApiClient.
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "course_name", length = 200)
    private String courseName;

    @Column(length = 100)
    private String subject;

    @Column(length = 100)
    private String department;

    private Double credits;
    private Integer capacity;

    @Column(name = "course_type", length = 50)
    private String courseType;

    // Scheduling requirements
    @Column(name = "requires_lab")
    private boolean requiresLab;

    @Column(name = "uses_multiple_rooms")
    private Boolean usesMultipleRooms = false;

    @Column(name = "requires_computers")
    private Boolean requiresComputers = false;

    @Column(name = "requires_projector")
    private Boolean requiresProjector = false;

    @Column(name = "requires_smartboard")
    private Boolean requiresSmartboard = false;

    @Column(name = "required_room_type", length = 100)
    private String requiredRoomType;

    @Column(name = "periods_per_week")
    private Integer periodsPerWeek;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "max_room_distance_minutes")
    private Integer maxRoomDistanceMinutes;

    @Column(name = "activity_type", length = 100)
    private String activityType;

    @Column(name = "additional_equipment", length = 500)
    private String additionalEquipment;

    // Course constraints
    @Column(length = 50)
    private String level;

    @Column(name = "min_gpa_required")
    private Double minGPARequired;

    @ElementCollection
    @CollectionTable(name = "course_required_certifications", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "certification")
    private List<String> requiredCertifications = new ArrayList<>();

    @Column(name = "min_students")
    private Integer minStudents;

    @Column(name = "max_waitlist")
    private Integer maxWaitlist;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_teacher_id")
    private Teacher assignedTeacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_room_id")
    private Room assignedRoom;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "course_room_assignments",
        joinColumns = @JoinColumn(name = "course_id"),
        inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    private List<Room> roomAssignments = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "course_enrollments",
        joinColumns = @JoinColumn(name = "course_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private List<Student> enrolledStudents = new ArrayList<>();

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(name = "current_enrollment")
    private Integer currentEnrollment = 0;

    @Column(name = "is_singleton")
    private Boolean isSingleton = false;

    @Column(name = "num_sections_needed")
    private Integer numSectionsNeeded;

    /**
     * Check if course is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    /**
     * Get Boolean active status
     */
    public Boolean getActive() {
        return active != null ? active : true;
    }

    /**
     * Get enrolled student count
     */
    public int getEnrollmentCount() {
        return enrolledStudents != null ? enrolledStudents.size() : 0;
    }

    /**
     * Get assigned teacher
     */
    public Teacher getTeacher() {
        return assignedTeacher;
    }

    /**
     * Get assigned room
     */
    public Room getRoom() {
        return assignedRoom;
    }

    /**
     * Get sessions per week (alias for periodsPerWeek)
     */
    public Integer getSessionsPerWeek() {
        return periodsPerWeek;
    }

    /**
     * Get maximum students allowed
     */
    public Integer getMaxStudents() {
        return maxStudents != null ? maxStudents : capacity;
    }

    /**
     * Get current enrollment count
     */
    public Integer getCurrentEnrollment() {
        if (currentEnrollment != null && currentEnrollment > 0) {
            return currentEnrollment;
        }
        return enrolledStudents != null ? enrolledStudents.size() : 0;
    }

    /**
     * Get course level
     */
    public String getLevel() {
        return level;
    }

    /**
     * Get duration in minutes
     */
    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    /**
     * Get maximum room distance in minutes
     */
    public Integer getMaxRoomDistanceMinutes() {
        return maxRoomDistanceMinutes;
    }

    /**
     * Get minimum GPA required
     */
    public Double getMinGPARequired() {
        return minGPARequired;
    }

    /**
     * Get required certifications
     */
    public List<String> getRequiredCertifications() {
        return requiredCertifications != null ? requiredCertifications : new ArrayList<>();
    }

    /**
     * Check if course requires lab
     */
    public boolean getRequiresLab() {
        return requiresLab;
    }

    /**
     * Check if course uses multiple rooms
     */
    public Boolean getUsesMultipleRooms() {
        return usesMultipleRooms != null ? usesMultipleRooms : false;
    }

    /**
     * Set uses multiple rooms flag
     */
    public void setUsesMultipleRooms(Boolean usesMultipleRooms) {
        this.usesMultipleRooms = usesMultipleRooms;
    }

    /**
     * Check if course has available seats
     */
    public boolean hasAvailableSeats() {
        int currentCount = getCurrentEnrollment();
        int maxCount = getMaxStudents();
        return currentCount < maxCount;
    }

    /**
     * Check if course should accept waitlist
     */
    public boolean shouldAcceptWaitlist() {
        return !hasAvailableSeats();
    }

    /**
     * Get activity type
     */
    public String getActivityType() {
        return activityType;
    }

    /**
     * Get additional equipment requirements
     */
    public String getAdditionalEquipment() {
        return additionalEquipment;
    }

    /**
     * Get maximum waitlist size
     */
    public Integer getMaxWaitlist() {
        return maxWaitlist;
    }

    /**
     * Get minimum students required
     */
    public Integer getMinStudents() {
        return minStudents;
    }

    /**
     * Check if course requires computers
     */
    public Boolean getRequiresComputers() {
        return requiresComputers != null ? requiresComputers : false;
    }

    /**
     * Check if course requires projector
     */
    public Boolean getRequiresProjector() {
        return requiresProjector != null ? requiresProjector : false;
    }

    /**
     * Check if course requires smartboard
     */
    public Boolean getRequiresSmartboard() {
        return requiresSmartboard != null ? requiresSmartboard : false;
    }

    /**
     * Check if course requires lab (Boolean wrapper)
     */
    public Boolean isRequiresLab() {
        return requiresLab;
    }

    /**
     * Increment enrollment count
     */
    public void incrementEnrollment() {
        if (currentEnrollment == null) {
            currentEnrollment = 0;
        }
        currentEnrollment++;
    }

    /**
     * Check if course is full
     */
    public boolean isFull() {
        return !hasAvailableSeats();
    }

    /**
     * Check if course is at minimum capacity
     */
    public boolean isAtMinimumCapacity() {
        if (minStudents == null) {
            return true;
        }
        return getCurrentEnrollment() >= minStudents;
    }

    /**
     * Check if course is at optimal capacity
     */
    public boolean isAtOptimalCapacity() {
        int current = getCurrentEnrollment();
        int max = getMaxStudents();
        return current >= (max * 0.85);
    }

    /**
     * Check if course is a singleton (only one section allowed)
     */
    public Boolean getIsSingleton() {
        return isSingleton != null ? isSingleton : false;
    }

    /**
     * Get number of sections needed for this course
     */
    public Integer getNumSectionsNeeded() {
        return numSectionsNeeded;
    }

    /**
     * Get room assignments for this course
     */
    public List<Room> getRoomAssignments() {
        return roomAssignments != null ? roomAssignments : new ArrayList<>();
    }

    /**
     * Get complexity score for scheduling optimization
     */
    public Integer getComplexityScore() {
        int complexity = 0;

        // Base complexity from course type
        if (Boolean.TRUE.equals(requiresLab)) complexity += 5;
        if (Boolean.TRUE.equals(requiresComputers)) complexity += 3;
        if (Boolean.TRUE.equals(requiresProjector)) complexity += 1;
        if (Boolean.TRUE.equals(requiresSmartboard)) complexity += 2;

        // Add complexity for singleton courses
        if (Boolean.TRUE.equals(isSingleton)) complexity += 10;

        return complexity;
    }

    /**
     * Get priority level for scheduling
     */
    public Integer getPriorityLevel() {
        // Default priority based on complexity and constraints
        return getComplexityScore() + (Boolean.TRUE.equals(isSingleton) ? 20 : 0);
    }

    /**
     * Check if course is fully assigned
     */
    public <T> boolean isFullyAssigned(T context) {
        return assignedTeacher != null && assignedRoom != null;
    }

    /**
     * Check if course is partially assigned
     */
    public <T> boolean isPartiallyAssigned(T context) {
        return (assignedTeacher != null && assignedRoom == null) ||
               (assignedTeacher == null && assignedRoom != null);
    }

    /**
     * Check if course is unassigned
     */
    public <T> boolean isUnassigned(T context) {
        return assignedTeacher == null && assignedRoom == null;
    }

    /**
     * Get planning period (for scheduling purposes)
     */
    public String getPlanningPeriod() {
        // Return a default planning period or derive from schedule
        return "DEFAULT";
    }

    /**
     * Set teacher assignment
     */
    public void setTeacher(Teacher teacher) {
        this.assignedTeacher = teacher;
    }
}

package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Student Entity (Scheduler Shadow Copy)
 *
 * This is a minimal entity representation for scheduling purposes only.
 * The authoritative Student data lives in the SIS microservice.
 * This class is populated from StudentDTO fetched via SISApiClient.
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", unique = true, length = 50)
    private String studentId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "grade_level", length = 10)
    private String gradeLevel;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "enrollment_status", length = 50)
    private String enrollmentStatus;

    // Special Education Flags
    @Column(name = "has_iep")
    private Boolean hasIEP = false;

    @Column(name = "has_504_plan")
    private Boolean has504Plan = false;

    @Column(name = "is_gifted")
    private Boolean isGifted = false;

    @Column(name = "accommodation_review_date")
    private LocalDate accommodationReviewDate;

    // Academic information
    @Column(name = "current_gpa")
    private Double currentGPA;

    // Contact information
    @Column(name = "emergency_contact", length = 200)
    private String emergencyContact;

    @Column(name = "emergency_phone", length = 50)
    private String emergencyPhone;

    // Special needs
    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions;

    @Column(name = "accommodation_notes", columnDefinition = "TEXT")
    private String accommodationNotes;

    // Lunch Assignment
    @Column(name = "lunch_wave_id")
    private Long lunchWaveId;

    // Campus (for multi-campus support)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    // Active status
    @Column(name = "active")
    private Boolean active = true;

    // For scheduling purposes - courses this student is enrolled in
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "enrolledStudents")
    private List<Course> enrolledCourses = new ArrayList<>();

    // Schedule slots assigned to this student
    // This is the inverse side of the ScheduleSlot.students ManyToMany relationship
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "students")
    private List<ScheduleSlot> scheduleSlots = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if student is actively enrolled
     */
    public boolean isActive() {
        if (active != null && !active) {
            return false;
        }
        return "ENROLLED".equalsIgnoreCase(enrollmentStatus) ||
               "ACTIVE".equalsIgnoreCase(enrollmentStatus);
    }

    /**
     * Get active status as Boolean
     */
    public Boolean getActive() {
        return active != null ? active : true;
    }

    /**
     * Get has IEP flag
     */
    public Boolean getHasIEP() {
        return hasIEP != null ? hasIEP : false;
    }

    /**
     * Get has 504 plan flag
     */
    public Boolean getHas504Plan() {
        return has504Plan != null ? has504Plan : false;
    }

    /**
     * Get current GPA
     */
    public Double getCurrentGPA() {
        return currentGPA;
    }

    /**
     * Get emergency contact
     */
    public String getEmergencyContact() {
        return emergencyContact;
    }

    /**
     * Get emergency phone
     */
    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    /**
     * Get medical conditions
     */
    public String getMedicalConditions() {
        return medicalConditions;
    }

    /**
     * Get accommodation notes
     */
    public String getAccommodationNotes() {
        return accommodationNotes;
    }

    /**
     * Check if student meets GPA requirement
     */
    public boolean meetsGPARequirement(Double requiredGPA) {
        if (requiredGPA == null) {
            return true;
        }
        if (currentGPA == null) {
            return false;
        }
        return currentGPA >= requiredGPA;
    }

    /**
     * Get GPA (alias for getCurrentGPA)
     */
    public Double getGpa() {
        return currentGPA;
    }

    /**
     * Get is gifted flag
     */
    public Boolean getIsGifted() {
        return isGifted != null ? isGifted : false;
    }

    /**
     * Get accommodation review date
     */
    public LocalDate getAccommodationReviewDate() {
        return accommodationReviewDate;
    }

    /**
     * Get schedule slots for this student
     */
    public List<ScheduleSlot> getScheduleSlots() {
        return scheduleSlots != null ? scheduleSlots : new ArrayList<>();
    }
}

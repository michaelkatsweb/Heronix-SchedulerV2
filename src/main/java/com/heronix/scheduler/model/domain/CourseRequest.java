package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "course_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "priority_rank")
    private Integer priorityRank = 1; // 1 = first choice, 2 = second, etc.

    @ManyToOne
    @JoinColumn(name = "alternate_course1_id")
    private Course alternateCourse1;

    @ManyToOne
    @JoinColumn(name = "alternate_course2_id")
    private Course alternateCourse2;

    @ManyToOne
    @JoinColumn(name = "alternate_course3_id")
    private Course alternateCourse3;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status")
    private RequestStatus requestStatus = RequestStatus.PENDING;

    @Column(name = "is_required_for_graduation")
    private Boolean isRequiredForGraduation = false;

    @Column(name = "is_prerequisite_met")
    private Boolean isPrerequisiteMet = true;

    @Column(name = "student_weight")
    private Integer studentWeight = 0; // Priority weighting

    @Column(name = "request_year")
    private Integer requestYear;

    @Column(name = "request_semester")
    private Integer requestSemester;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "requested_at")
    private java.time.LocalDateTime requestedAt = java.time.LocalDateTime.now();

    @Column(name = "processed_at")
    private java.time.LocalDateTime processedAt;

    @Column(name = "fulfilled_at")
    private java.time.LocalDateTime fulfilledAt;

    public enum RequestStatus {
        PENDING,        // Not yet processed
        FULFILLED,      // Got requested course
        ALTERNATE1,     // Got first alternate
        ALTERNATE2,     // Got second alternate
        ALTERNATE3,     // Got third alternate
        WAITLISTED,     // On waitlist
        DENIED,         // Could not fulfill
        CANCELLED       // Student cancelled
    }
}

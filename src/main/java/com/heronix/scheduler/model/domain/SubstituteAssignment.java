package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a substitute assignment to replace a staff member
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Entity
@Table(name = "substitute_assignments")
public class SubstituteAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The substitute assigned to this job
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_id", nullable = false)
    private Substitute substitute;

    /**
     * The teacher being replaced (if staff type is TEACHER)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_teacher_id")
    private Teacher replacedTeacher;

    /**
     * Type of staff being replaced (teacher, co-teacher, paraprofessional)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "replaced_staff_type", nullable = false, length = 50)
    private StaffType replacedStaffType;

    /**
     * Name of the staff member being replaced (for non-teacher staff)
     */
    @Column(name = "replaced_staff_name", length = 200)
    private String replacedStaffName;

    /**
     * Date of the assignment
     */
    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    /**
     * Start time of the assignment
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * End time of the assignment
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Duration type (hourly, half-day, full-day, multi-day, long-term)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "duration_type", nullable = false, length = 50)
    private AssignmentDuration durationType;

    /**
     * End date for multi-day or long-term assignments
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Status of the assignment (pending, confirmed, in-progress, completed, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AssignmentStatus status;

    /**
     * Reason for the absence
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "absence_reason", length = 50)
    private AbsenceReason absenceReason;

    /**
     * Source of this assignment (Frontline, Manual, Auto-Generated, Import)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_source", nullable = false, length = 50)
    private AssignmentSource assignmentSource;

    /**
     * External job ID from Frontline or other system
     */
    @Column(name = "frontline_job_id", length = 100)
    private String frontlineJobId;

    /**
     * Notes about this specific assignment
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Room or location for the assignment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    /**
     * Course being taught (if applicable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    /**
     * List of schedule slots covered by this assignment
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "assignment_schedule_slots",
        joinColumns = @JoinColumn(name = "assignment_id"),
        inverseJoinColumns = @JoinColumn(name = "schedule_slot_id")
    )
    private List<ScheduleSlot> scheduleSlots = new ArrayList<>();

    /**
     * Whether this is a floater assignment (covering multiple classes)
     */
    @Column(name = "is_floater")
    private Boolean isFloater = false;

    /**
     * Total hours for this assignment
     */
    @Column(name = "total_hours")
    private Double totalHours;

    /**
     * Pay amount for this assignment
     */
    @Column(name = "pay_amount")
    private Double payAmount;

    /**
     * Date when this assignment was created
     */
    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    /**
     * Date when this assignment was last modified
     */
    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    /**
     * Date when the substitute confirmed the assignment
     */
    @Column(name = "date_confirmed")
    private LocalDateTime dateConfirmed;

    /**
     * Default constructor
     */
    public SubstituteAssignment() {
        this.dateCreated = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.status = AssignmentStatus.PENDING;
    }

    /**
     * Constructor with required fields
     */
    public SubstituteAssignment(Substitute substitute, LocalDate assignmentDate,
                               LocalTime startTime, LocalTime endTime,
                               AssignmentDuration durationType, StaffType replacedStaffType) {
        this();
        this.substitute = substitute;
        this.assignmentDate = assignmentDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationType = durationType;
        this.replacedStaffType = replacedStaffType;
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Substitute getSubstitute() {
        return substitute;
    }

    public void setSubstitute(Substitute substitute) {
        this.substitute = substitute;
        this.lastModified = LocalDateTime.now();
    }

    public Teacher getReplacedTeacher() {
        return replacedTeacher;
    }

    public void setReplacedTeacher(Teacher replacedTeacher) {
        this.replacedTeacher = replacedTeacher;
        this.lastModified = LocalDateTime.now();
    }

    public StaffType getReplacedStaffType() {
        return replacedStaffType;
    }

    public void setReplacedStaffType(StaffType replacedStaffType) {
        this.replacedStaffType = replacedStaffType;
        this.lastModified = LocalDateTime.now();
    }

    public String getReplacedStaffName() {
        return replacedStaffName;
    }

    public void setReplacedStaffName(String replacedStaffName) {
        this.replacedStaffName = replacedStaffName;
        this.lastModified = LocalDateTime.now();
    }

    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }

    public void setAssignmentDate(LocalDate assignmentDate) {
        this.assignmentDate = assignmentDate;
        this.lastModified = LocalDateTime.now();
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        this.lastModified = LocalDateTime.now();
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
        this.lastModified = LocalDateTime.now();
    }

    public AssignmentDuration getDurationType() {
        return durationType;
    }

    public void setDurationType(AssignmentDuration durationType) {
        this.durationType = durationType;
        this.lastModified = LocalDateTime.now();
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.lastModified = LocalDateTime.now();
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
        this.lastModified = LocalDateTime.now();
        if (status == AssignmentStatus.CONFIRMED && this.dateConfirmed == null) {
            this.dateConfirmed = LocalDateTime.now();
        }
    }

    public AbsenceReason getAbsenceReason() {
        return absenceReason;
    }

    public void setAbsenceReason(AbsenceReason absenceReason) {
        this.absenceReason = absenceReason;
        this.lastModified = LocalDateTime.now();
    }

    public AssignmentSource getAssignmentSource() {
        return assignmentSource;
    }

    public void setAssignmentSource(AssignmentSource assignmentSource) {
        this.assignmentSource = assignmentSource;
        this.lastModified = LocalDateTime.now();
    }

    public String getFrontlineJobId() {
        return frontlineJobId;
    }

    public void setFrontlineJobId(String frontlineJobId) {
        this.frontlineJobId = frontlineJobId;
        this.lastModified = LocalDateTime.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.lastModified = LocalDateTime.now();
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
        this.lastModified = LocalDateTime.now();
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
        this.lastModified = LocalDateTime.now();
    }

    public List<ScheduleSlot> getScheduleSlots() {
        return scheduleSlots;
    }

    public void setScheduleSlots(List<ScheduleSlot> scheduleSlots) {
        this.scheduleSlots = scheduleSlots;
        this.lastModified = LocalDateTime.now();
    }

    public Boolean getIsFloater() {
        return isFloater;
    }

    public void setIsFloater(Boolean isFloater) {
        this.isFloater = isFloater;
        this.lastModified = LocalDateTime.now();
    }

    public Double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Double totalHours) {
        this.totalHours = totalHours;
        this.lastModified = LocalDateTime.now();
    }

    public Double getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(Double payAmount) {
        this.payAmount = payAmount;
        this.lastModified = LocalDateTime.now();
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public LocalDateTime getDateConfirmed() {
        return dateConfirmed;
    }

    public void setDateConfirmed(LocalDateTime dateConfirmed) {
        this.dateConfirmed = dateConfirmed;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Add a schedule slot to this assignment
     */
    public void addScheduleSlot(ScheduleSlot slot) {
        this.scheduleSlots.add(slot);
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Remove a schedule slot from this assignment
     */
    public void removeScheduleSlot(ScheduleSlot slot) {
        this.scheduleSlots.remove(slot);
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Get the name of the person being replaced
     */
    public String getReplacedPersonName() {
        if (replacedTeacher != null) {
            return replacedTeacher.getName();
        }
        return replacedStaffName != null ? replacedStaffName : "Unknown";
    }

    /**
     * Check if this is a multi-day assignment
     */
    public boolean isMultiDay() {
        return durationType == AssignmentDuration.MULTI_DAY ||
               durationType == AssignmentDuration.LONG_TERM;
    }

    /**
     * Calculate total days for multi-day assignments
     */
    public long getTotalDays() {
        if (isMultiDay() && endDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(assignmentDate, endDate) + 1;
        }
        return 1;
    }

    @Override
    public String toString() {
        return "SubstituteAssignment{" +
                "id=" + id +
                ", substitute=" + (substitute != null ? substitute.getFullName() : "null") +
                ", replacedPerson='" + getReplacedPersonName() + '\'' +
                ", date=" + assignmentDate +
                ", durationType=" + durationType +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubstituteAssignment that = (SubstituteAssignment) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

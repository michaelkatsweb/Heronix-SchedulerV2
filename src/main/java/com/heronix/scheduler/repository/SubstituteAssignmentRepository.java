package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Substitute;
import com.heronix.scheduler.model.domain.SubstituteAssignment;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.enums.AssignmentDuration;
import com.heronix.scheduler.model.enums.AssignmentStatus;
import com.heronix.scheduler.model.enums.StaffType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SubstituteAssignment entity
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Repository
public interface SubstituteAssignmentRepository extends JpaRepository<SubstituteAssignment, Long> {

    /**
     * Find all assignments for a specific substitute
     */
    List<SubstituteAssignment> findBySubstitute(Substitute substitute);

    /**
     * Find all assignments for a specific substitute by ID
     */
    List<SubstituteAssignment> findBySubstituteId(Long substituteId);

    /**
     * Find all assignments for a specific date
     */
    List<SubstituteAssignment> findByAssignmentDate(LocalDate date);

    /**
     * Find all assignments between two dates
     */
    List<SubstituteAssignment> findByAssignmentDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find all assignments for a specific substitute on a specific date
     */
    List<SubstituteAssignment> findBySubstituteAndAssignmentDate(Substitute substitute, LocalDate date);

    /**
     * Find all assignments for a specific teacher being replaced
     */
    List<SubstituteAssignment> findByReplacedTeacher(Teacher teacher);

    /**
     * Find all assignments for a specific teacher being replaced by ID
     */
    List<SubstituteAssignment> findByReplacedTeacherId(Long teacherId);

    /**
     * Find all assignments by status
     */
    List<SubstituteAssignment> findByStatus(AssignmentStatus status);

    /**
     * Find all assignments by status on a specific date
     */
    List<SubstituteAssignment> findByStatusAndAssignmentDate(AssignmentStatus status, LocalDate date);

    /**
     * Find all assignments by duration type
     */
    List<SubstituteAssignment> findByDurationType(AssignmentDuration durationType);

    /**
     * Find all assignments by staff type
     */
    List<SubstituteAssignment> findByReplacedStaffType(StaffType staffType);

    /**
     * Find all floater assignments
     */
    List<SubstituteAssignment> findByIsFloaterTrue();

    /**
     * Find all floater assignments for a specific date
     */
    List<SubstituteAssignment> findByIsFloaterTrueAndAssignmentDate(LocalDate date);

    /**
     * Find assignment by Frontline job ID
     */
    Optional<SubstituteAssignment> findByFrontlineJobId(String frontlineJobId);

    /**
     * Find all assignments with a specific Frontline job ID prefix
     */
    @Query("SELECT sa FROM SubstituteAssignment sa WHERE sa.frontlineJobId LIKE CONCAT(:prefix, '%')")
    List<SubstituteAssignment> findByFrontlineJobIdStartingWith(@Param("prefix") String prefix);

    /**
     * Find all pending assignments for a specific date
     */
    @Query("SELECT sa FROM SubstituteAssignment sa WHERE sa.status = 'PENDING' AND sa.assignmentDate = :date")
    List<SubstituteAssignment> findPendingAssignmentsForDate(@Param("date") LocalDate date);

    /**
     * Find all confirmed assignments for a substitute on a specific date
     */
    @Query("SELECT sa FROM SubstituteAssignment sa WHERE sa.substitute.id = :substituteId " +
           "AND sa.status = 'CONFIRMED' AND sa.assignmentDate = :date")
    List<SubstituteAssignment> findConfirmedAssignmentsForSubstituteOnDate(
            @Param("substituteId") Long substituteId,
            @Param("date") LocalDate date);

    /**
     * Find all active assignments (confirmed or in-progress) for a date
     */
    @Query("SELECT sa FROM SubstituteAssignment sa WHERE sa.assignmentDate = :date " +
           "AND sa.status IN ('CONFIRMED', 'IN_PROGRESS')")
    List<SubstituteAssignment> findActiveAssignmentsForDate(@Param("date") LocalDate date);

    /**
     * Find all long-term assignments (multi-day or long-term) that span a date
     */
    @Query("SELECT sa FROM SubstituteAssignment sa WHERE sa.durationType IN ('MULTI_DAY', 'LONG_TERM') " +
           "AND sa.assignmentDate <= :date AND sa.endDate >= :date")
    List<SubstituteAssignment> findLongTermAssignmentsSpanningDate(@Param("date") LocalDate date);

    /**
     * Find all assignments for a substitute in a date range
     */
    @Query("SELECT sa FROM SubstituteAssignment sa WHERE sa.substitute.id = :substituteId " +
           "AND sa.assignmentDate BETWEEN :startDate AND :endDate")
    List<SubstituteAssignment> findAssignmentsForSubstituteInDateRange(
            @Param("substituteId") Long substituteId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count assignments for a substitute on a specific date
     */
    long countBySubstituteAndAssignmentDate(Substitute substitute, LocalDate date);

    /**
     * Count assignments by status
     */
    long countByStatus(AssignmentStatus status);

    /**
     * Count assignments for a specific date
     */
    long countByAssignmentDate(LocalDate date);

    /**
     * Count floater assignments for a specific date
     */
    long countByIsFloaterTrueAndAssignmentDate(LocalDate date);

    /**
     * Check if a Frontline job ID already exists
     */
    boolean existsByFrontlineJobId(String frontlineJobId);

    /**
     * Get total hours for a substitute in a date range
     */
    @Query("SELECT SUM(sa.totalHours) FROM SubstituteAssignment sa WHERE sa.substitute.id = :substituteId " +
           "AND sa.assignmentDate BETWEEN :startDate AND :endDate")
    Double getTotalHoursForSubstituteInDateRange(
            @Param("substituteId") Long substituteId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total pay for a substitute in a date range
     */
    @Query("SELECT SUM(sa.payAmount) FROM SubstituteAssignment sa WHERE sa.substitute.id = :substituteId " +
           "AND sa.assignmentDate BETWEEN :startDate AND :endDate")
    Double getTotalPayForSubstituteInDateRange(
            @Param("substituteId") Long substituteId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find assignments that need scheduling (missing schedule slots)
     */
    @Query("SELECT sa FROM SubstituteAssignment sa WHERE sa.status IN ('CONFIRMED', 'IN_PROGRESS') " +
           "AND sa.scheduleSlots IS EMPTY AND sa.assignmentDate >= :fromDate")
    List<SubstituteAssignment> findAssignmentsNeedingScheduling(@Param("fromDate") LocalDate fromDate);
}

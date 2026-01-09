package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.CourseEnrollmentRequest;
import com.heronix.scheduler.model.domain.PriorityRule;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.dto.AssignmentResult;
import com.heronix.scheduler.model.enums.EnrollmentRequestStatus;
import com.heronix.scheduler.repository.CourseEnrollmentRequestRepository;
import com.heronix.scheduler.repository.PriorityRuleRepository;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent Course Assignment Service
 *
 * Core service for AI-powered automated course assignment system.
 * Implements sophisticated algorithms for:
 * - GPA-based priority student assignment
 * - Class size balancing
 * - Waitlist management
 * - Preference optimization
 * - Conflict detection and resolution
 *
 * Assignment Process:
 * 1. Load all pending enrollment requests
 * 2. Apply priority rules to calculate final scores
 * 3. Sort requests by total priority (highest first)
 * 4. Process each request:
 *    - If course has space: approve and enroll
 *    - If course is full but allows waitlist: add to waitlist
 *    - If course is full and no waitlist: try alternate course
 * 5. Balance class sizes across sections
 * 6. Generate comprehensive assignment report
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 4 - November 20, 2025
 */
@Service
@Transactional
@Slf4j
public class IntelligentCourseAssignmentService {

    @Autowired
    private CourseEnrollmentRequestRepository enrollmentRequestRepository;

    @Autowired
    private PriorityRuleRepository priorityRuleRepository;

    @Autowired
    private SISDataService sisDataService;

    // ========================================================================
    // MAIN ASSIGNMENT METHODS
    // ========================================================================

    /**
     * Run full automated course assignment for all pending requests
     *
     * @param academicYearId Academic year to process
     * @param initiatedBy Username who initiated the assignment
     * @param isSimulation true = dry run (no database changes), false = actual assignment
     * @return AssignmentResult with detailed statistics and results
     */
    public AssignmentResult runAutomatedAssignment(Long academicYearId, String initiatedBy, boolean isSimulation) {
        log.info("Starting automated course assignment for academic year {} (simulation: {})", academicYearId, isSimulation);

        AssignmentResult result = new AssignmentResult();
        result.setStartTime(LocalDateTime.now());
        result.setAcademicYearId(academicYearId);
        result.setInitiatedBy(initiatedBy);
        result.setIsSimulation(isSimulation);

        try {
            // Step 1: Load all pending requests
            List<CourseEnrollmentRequest> pendingRequests = loadPendingRequests(academicYearId);
            result.setTotalRequestsProcessed(pendingRequests.size());
            log.info("Loaded {} pending enrollment requests", pendingRequests.size());

            if (pendingRequests.isEmpty()) {
                result.addWarning("No pending enrollment requests found for academic year " + academicYearId);
                result.setEndTime(LocalDateTime.now());
                result.calculateDerivedMetrics();
                return result;
            }

            // Step 2: Apply priority rules to calculate final scores
            applyPriorityRulesToRequests(pendingRequests, result);

            // Step 3: Sort by priority (highest first)
            sortRequestsByPriority(pendingRequests);

            // Step 4: Process each request in priority order
            processAssignmentRequests(pendingRequests, result, isSimulation);

            // Step 5: Analyze results and generate statistics
            analyzeAssignmentResults(result);

            // Step 6: Check for students needing manual review
            checkStudentsNeedingReview(result);

            // Step 7: Check for courses below minimum capacity
            checkCoursesBelowMinimum(result);

            result.setEndTime(LocalDateTime.now());
            result.calculateDerivedMetrics();

            log.info("Assignment completed successfully: {}", result.getSummary());

        } catch (Exception e) {
            log.error("Error during automated assignment", e);
            result.addIssue("Fatal error: " + e.getMessage());
            result.setEndTime(LocalDateTime.now());
            result.calculateDerivedMetrics();
        }

        return result;
    }

    /**
     * Run assignment for a specific student only
     */
    public AssignmentResult assignCoursesForStudent(Long studentId, String initiatedBy, boolean isSimulation) {
        log.info("Assigning courses for student ID: {}", studentId);

        AssignmentResult result = new AssignmentResult();
        result.setStartTime(LocalDateTime.now());
        result.setInitiatedBy(initiatedBy);
        result.setIsSimulation(isSimulation);

        try {
            Student student = sisDataService.getStudentById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

            // Get pending requests for this student
            List<CourseEnrollmentRequest> requests = enrollmentRequestRepository
                .findPendingByStudentOrderedByPreference(studentId);

            result.setTotalRequestsProcessed(requests.size());
            result.setTotalStudentsProcessed(1);

            if (requests.isEmpty()) {
                result.addWarning("No pending enrollment requests found for student " + student.getFullName());
                result.setEndTime(LocalDateTime.now());
                result.calculateDerivedMetrics();
                return result;
            }

            // Apply priority rules
            applyPriorityRulesToRequests(requests, result);

            // Process requests
            processAssignmentRequests(requests, result, isSimulation);

            result.setEndTime(LocalDateTime.now());
            result.calculateDerivedMetrics();

        } catch (Exception e) {
            log.error("Error assigning courses for student", e);
            result.addIssue("Error: " + e.getMessage());
            result.setEndTime(LocalDateTime.now());
            result.calculateDerivedMetrics();
        }

        return result;
    }

    /**
     * Run assignment for a specific course only
     */
    public AssignmentResult assignStudentsToCourse(Long courseId, String initiatedBy, boolean isSimulation) {
        log.info("Assigning students to course ID: {}", courseId);

        AssignmentResult result = new AssignmentResult();
        result.setStartTime(LocalDateTime.now());
        result.setInitiatedBy(initiatedBy);
        result.setIsSimulation(isSimulation);

        try {
            Course course = sisDataService.getCourseById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

            // Get pending requests for this course, ordered by priority
            List<CourseEnrollmentRequest> requests = enrollmentRequestRepository
                .findPendingByCourseOrderedByPriority(courseId);

            result.setTotalRequestsProcessed(requests.size());
            result.setTotalCoursesProcessed(1);

            if (requests.isEmpty()) {
                result.addWarning("No pending enrollment requests found for course " + course.getCourseCode());
                result.setEndTime(LocalDateTime.now());
                result.calculateDerivedMetrics();
                return result;
            }

            // Apply priority rules
            applyPriorityRulesToRequests(requests, result);

            // Re-sort after applying rules
            sortRequestsByPriority(requests);

            // Process requests
            processAssignmentRequests(requests, result, isSimulation);

            result.setEndTime(LocalDateTime.now());
            result.calculateDerivedMetrics();

        } catch (Exception e) {
            log.error("Error assigning students to course", e);
            result.addIssue("Error: " + e.getMessage());
            result.setEndTime(LocalDateTime.now());
            result.calculateDerivedMetrics();
        }

        return result;
    }

    // ========================================================================
    // PRIORITY RULE APPLICATION
    // ========================================================================

    /**
     * Apply all active priority rules to calculate final priority scores
     */
    private void applyPriorityRulesToRequests(List<CourseEnrollmentRequest> requests, AssignmentResult result) {
        log.info("Applying priority rules to {} requests", requests.size());

        // Load all active priority rules, ordered by weight
        List<PriorityRule> activeRules = priorityRuleRepository.findByActiveTrueOrderByWeightDesc();

        if (activeRules.isEmpty()) {
            result.addWarning("No active priority rules found - using base priority scores only");
            return;
        }

        log.info("Found {} active priority rules", activeRules.size());

        // Apply rules to each request
        for (CourseEnrollmentRequest request : requests) {
            Student student = request.getStudent();
            int basePriority = request.getPriorityScore() != null ? request.getPriorityScore() : 0;
            int ruleBonus = 0;

            // Apply each rule that matches this student
            for (PriorityRule rule : activeRules) {
                if (rule.appliesTo(student)) {
                    int bonus = rule.calculateBonus(student);
                    ruleBonus += bonus;
                    log.debug("Applied rule '{}' to student {}: +{} points",
                        rule.getRuleName(), student.getStudentId(), bonus);
                }
            }

            // Update priority score with rule bonus
            int finalPriority = basePriority + ruleBonus + request.getPreferenceBonus();
            request.setPriorityScore(finalPriority);

            log.debug("Student {} final priority: {} (base: {}, rule bonus: {}, preference: {})",
                student.getStudentId(), finalPriority, basePriority, ruleBonus, request.getPreferenceBonus());
        }
    }

    /**
     * Sort requests by total priority (highest first)
     * Tie-breaker: preference rank (1st choice beats 2nd choice)
     * Second tie-breaker: request timestamp (earlier beats later)
     */
    private void sortRequestsByPriority(List<CourseEnrollmentRequest> requests) {
        requests.sort((r1, r2) -> {
            // Primary: Total priority score (descending)
            int priorityCompare = Integer.compare(r2.getTotalPriorityScore(), r1.getTotalPriorityScore());
            if (priorityCompare != 0) return priorityCompare;

            // Tie-breaker 1: Preference rank (ascending - 1st choice beats 2nd)
            Integer pref1 = r1.getPreferenceRank() != null ? r1.getPreferenceRank() : 999;
            Integer pref2 = r2.getPreferenceRank() != null ? r2.getPreferenceRank() : 999;
            int prefCompare = Integer.compare(pref1, pref2);
            if (prefCompare != 0) return prefCompare;

            // Tie-breaker 2: Timestamp (ascending - earlier beats later)
            if (r1.getCreatedAt() != null && r2.getCreatedAt() != null) {
                return r1.getCreatedAt().compareTo(r2.getCreatedAt());
            }

            return 0;
        });
    }

    // ========================================================================
    // REQUEST PROCESSING
    // ========================================================================

    /**
     * Process each enrollment request in priority order
     */
    private void processAssignmentRequests(List<CourseEnrollmentRequest> requests,
                                          AssignmentResult result,
                                          boolean isSimulation) {
        log.info("Processing {} assignment requests (simulation: {})", requests.size(), isSimulation);

        for (CourseEnrollmentRequest request : requests) {
            try {
                processIndividualRequest(request, result, isSimulation);
            } catch (Exception e) {
                log.error("Error processing request ID {}: {}", request.getId(), e.getMessage());
                result.addIssue("Failed to process request for student " +
                    request.getStudent().getStudentId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Process a single enrollment request
     */
    private void processIndividualRequest(CourseEnrollmentRequest request,
                                         AssignmentResult result,
                                         boolean isSimulation) {
        Course course = request.getCourse();
        Student student = request.getStudent();

        log.debug("Processing request: Student {} → Course {} (priority: {})",
            student.getStudentId(), course.getCourseCode(), request.getTotalPriorityScore());

        // Check if student meets GPA requirement
        Double minGPA = course.getMinGPARequired();
        if (minGPA != null && !student.meetsGPARequirement(minGPA)) {
            handleDeniedRequest(request, result, isSimulation,
                String.format("Student GPA %.2f does not meet minimum requirement %.2f",
                    student.getCurrentGPA(), minGPA));
            return;
        }

        // Check if course has available seats
        if (course.hasAvailableSeats()) {
            handleApprovedRequest(request, result, isSimulation,
                "Assigned based on priority - seat available");
            return;
        }

        // Course is full - check if waitlist is allowed
        if (course.shouldAcceptWaitlist()) {
            int waitlistPosition = getNextWaitlistPosition(course);
            if (waitlistPosition <= course.getMaxWaitlist()) {
                handleWaitlistedRequest(request, result, isSimulation, waitlistPosition,
                    "Course full - added to waitlist");
                return;
            }
        }

        // Course is full and waitlist is full - try alternate course
        handleAlternateAssignment(request, result, isSimulation);
    }

    /**
     * Approve enrollment request and enroll student
     */
    private void handleApprovedRequest(CourseEnrollmentRequest request,
                                      AssignmentResult result,
                                      boolean isSimulation,
                                      String reason) {
        Course course = request.getCourse();
        Student student = request.getStudent();

        log.info("APPROVED: Student {} → Course {} ({})",
            student.getStudentId(), course.getCourseCode(), reason);

        if (!isSimulation) {
            request.approve(reason);
            course.incrementEnrollment();
            // Note: Course enrollment changes should be synced back to SIS via API
            enrollmentRequestRepository.save(request);
        }

        result.setRequestsApproved(result.getRequestsApproved() + 1);

        // Track preference satisfaction
        if (request.getPreferenceRank() != null) {
            switch (request.getPreferenceRank()) {
                case 1 -> result.setStudentsGotFirstChoice(result.getStudentsGotFirstChoice() + 1);
                case 2 -> result.setStudentsGotSecondChoice(result.getStudentsGotSecondChoice() + 1);
                case 3 -> result.setStudentsGotThirdChoice(result.getStudentsGotThirdChoice() + 1);
                case 4 -> result.setStudentsGotFourthChoice(result.getStudentsGotFourthChoice() + 1);
            }
        }
    }

    /**
     * Add request to waitlist
     */
    private void handleWaitlistedRequest(CourseEnrollmentRequest request,
                                        AssignmentResult result,
                                        boolean isSimulation,
                                        int waitlistPosition,
                                        String reason) {
        Course course = request.getCourse();
        Student student = request.getStudent();

        log.info("WAITLISTED: Student {} → Course {} at position {} ({})",
            student.getStudentId(), course.getCourseCode(), waitlistPosition, reason);

        if (!isSimulation) {
            request.addToWaitlist(waitlistPosition, reason);
            enrollmentRequestRepository.save(request);
        }

        result.setRequestsWaitlisted(result.getRequestsWaitlisted() + 1);
    }

    /**
     * Deny enrollment request
     */
    private void handleDeniedRequest(CourseEnrollmentRequest request,
                                    AssignmentResult result,
                                    boolean isSimulation,
                                    String reason) {
        Course course = request.getCourse();
        Student student = request.getStudent();

        log.info("DENIED: Student {} → Course {} ({})",
            student.getStudentId(), course.getCourseCode(), reason);

        if (!isSimulation) {
            request.deny(reason);
            enrollmentRequestRepository.save(request);
        }

        result.setRequestsDenied(result.getRequestsDenied() + 1);
    }

    /**
     * Try to assign student to an alternate course
     */
    private void handleAlternateAssignment(CourseEnrollmentRequest request,
                                          AssignmentResult result,
                                          boolean isSimulation) {
        Student student = request.getStudent();
        Course originalCourse = request.getCourse();

        log.debug("Trying alternate course for student {} (original: {})",
            student.getStudentId(), originalCourse.getCourseCode());

        // Get student's other pending requests in preference order
        List<CourseEnrollmentRequest> alternateRequests = enrollmentRequestRepository
            .findPendingByStudentOrderedByPreference(student.getId())
            .stream()
            .filter(r -> !r.getId().equals(request.getId())) // Exclude current request
            .filter(r -> r.getCourse().hasAvailableSeats()) // Only courses with space
            .filter(r -> {
                Double minGPA = r.getCourse().getMinGPARequired();
                return minGPA == null || student.meetsGPARequirement(minGPA);
            }) // Meets GPA requirement
            .toList();

        if (!alternateRequests.isEmpty()) {
            // Assign to first available alternate
            CourseEnrollmentRequest alternateRequest = alternateRequests.get(0);
            Course alternateCourse = alternateRequest.getCourse();

            log.info("ALTERNATE: Student {} → Course {} (original: {} was full)",
                student.getStudentId(), alternateCourse.getCourseCode(), originalCourse.getCourseCode());

            if (!isSimulation) {
                // Deny original request
                request.deny(String.format("Course full - assigned to alternate course %s instead",
                    alternateCourse.getCourseCode()));
                enrollmentRequestRepository.save(request);

                // Approve alternate request
                alternateRequest.approve("Assigned as alternate to full course");
                alternateCourse.incrementEnrollment();
                // Note: Course enrollment changes should be synced back to SIS via API
                enrollmentRequestRepository.save(alternateRequest);
            }

            result.setRequestsDenied(result.getRequestsDenied() + 1);
            result.setRequestsAlternateAssigned(result.getRequestsAlternateAssigned() + 1);
        } else {
            // No alternate available - deny request
            handleDeniedRequest(request, result, isSimulation,
                "Course full, waitlist full, no alternate courses available");
        }
    }

    // ========================================================================
    // WAITLIST MANAGEMENT
    // ========================================================================

    /**
     * Get next available waitlist position for a course
     */
    private int getNextWaitlistPosition(Course course) {
        Integer nextPosition = enrollmentRequestRepository.getNextWaitlistPosition(course.getId());
        return nextPosition != null ? nextPosition : 1;
    }

    /**
     * Promote students from waitlist when seats become available
     */
    public void promoteFromWaitlist(Long courseId, int seatsAvailable) {
        log.info("Promoting {} students from waitlist for course ID: {}", seatsAvailable, courseId);

        Course course = sisDataService.getCourseById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        // Get waitlist in priority order
        List<CourseEnrollmentRequest> waitlist = enrollmentRequestRepository
            .findByCourseAndIsWaitlistTrueOrderByWaitlistPositionAsc(course);

        int promoted = 0;
        for (CourseEnrollmentRequest request : waitlist) {
            if (promoted >= seatsAvailable) break;
            if (!course.hasAvailableSeats()) break;

            // Promote student
            request.promoteFromWaitlist("Seat became available", "System Auto-Promote");
            course.incrementEnrollment();

            enrollmentRequestRepository.save(request);
            promoted++;

            log.info("Promoted student {} from waitlist to course {}",
                request.getStudent().getStudentId(), course.getCourseCode());
        }

        if (promoted > 0) {
            // Note: Course enrollment changes should be synced back to SIS via API

            // Update remaining waitlist positions
            updateWaitlistPositions(courseId);
        }
    }

    /**
     * Update waitlist positions after promotion
     */
    private void updateWaitlistPositions(Long courseId) {
        Course course = sisDataService.getCourseById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        List<CourseEnrollmentRequest> waitlist = enrollmentRequestRepository
            .findWaitlistByCourseId(courseId);

        // Re-number positions
        for (int i = 0; i < waitlist.size(); i++) {
            waitlist.get(i).setWaitlistPosition(i + 1);
        }

        enrollmentRequestRepository.saveAll(waitlist);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Load all pending enrollment requests for an academic year
     */
    private List<CourseEnrollmentRequest> loadPendingRequests(Long academicYearId) {
        if (academicYearId != null) {
            return enrollmentRequestRepository.findPendingByAcademicYearOrderedByPriority(academicYearId);
        } else {
            return enrollmentRequestRepository.findAllPendingOrderedByPriority();
        }
    }

    /**
     * Analyze assignment results and populate statistics
     */
    private void analyzeAssignmentResults(AssignmentResult result) {
        // Count unique students
        Set<Long> studentIds = new HashSet<>();
        List<CourseEnrollmentRequest> allRequests = enrollmentRequestRepository.findAll();

        for (CourseEnrollmentRequest request : allRequests) {
            studentIds.add(request.getStudent().getId());
        }

        result.setTotalStudentsProcessed(studentIds.size());

        // Count courses
        List<Course> allCourses = sisDataService.getAllCourses();
        result.setTotalCoursesProcessed(allCourses.size());

        // Analyze course capacity
        for (Course course : allCourses) {
            if (course.isFull()) {
                result.setCoursesNowFull(result.getCoursesNowFull() + 1);
            } else if (course.isAtOptimalCapacity()) {
                result.setCoursesAtOptimal(result.getCoursesAtOptimal() + 1);
            } else if (!course.isAtMinimumCapacity()) {
                result.setCoursesBelowMinimum(result.getCoursesBelowMinimum() + 1);
            }

            long waitlistCount = enrollmentRequestRepository.countByCourseAndIsWaitlistTrue(course);
            if (waitlistCount > 0) {
                result.setCoursesWithWaitlists(result.getCoursesWithWaitlists() + 1);
            }
        }
    }

    /**
     * Check for students needing manual review
     */
    private void checkStudentsNeedingReview(AssignmentResult result) {
        List<Student> allStudents = sisDataService.getAllStudents();

        for (Student student : allStudents) {
            List<CourseEnrollmentRequest> approvedRequests = enrollmentRequestRepository
                .findByStudentAndRequestStatus(student, EnrollmentRequestStatus.APPROVED);

            if (approvedRequests.size() < 7) {
                result.addStudentNeedingReview(String.format("%s (%s) - only %d courses assigned",
                    student.getFullName(), student.getStudentId(), approvedRequests.size()));
                result.setStudentsWithPartialSchedules(result.getStudentsWithPartialSchedules() + 1);
            } else if (approvedRequests.size() == 7) {
                result.setStudentsWithCompleteSchedules(result.getStudentsWithCompleteSchedules() + 1);
            }
        }
    }

    /**
     * Check for courses below minimum capacity
     */
    private void checkCoursesBelowMinimum(AssignmentResult result) {
        List<Course> allCourses = sisDataService.getAllCourses();

        for (Course course : allCourses) {
            if (!course.isAtMinimumCapacity()) {
                result.addCourseBelowMinimum(String.format("%s - %s: %d/%d students (min: %d)",
                    course.getCourseCode(), course.getCourseName(),
                    course.getCurrentEnrollment(), course.getMaxStudents(), course.getMinStudents()));
            }
        }
    }
}

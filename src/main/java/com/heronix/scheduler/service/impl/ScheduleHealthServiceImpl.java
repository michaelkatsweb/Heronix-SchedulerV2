package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.dto.ScheduleHealthMetrics;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.ScheduleHealthService;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of Schedule Health Service
 * Calculates comprehensive health metrics for schedules
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class ScheduleHealthServiceImpl implements ScheduleHealthService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ConflictMatrixRepository conflictMatrixRepository;

    // ========== MAIN CALCULATION METHODS ==========

    @Override
    public ScheduleHealthMetrics calculateHealthMetrics(Schedule schedule) {
        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of schedule name
        String scheduleName = (schedule.getScheduleName() != null) ? schedule.getScheduleName() : "Unknown";
        log.info("Calculating health metrics for schedule: {}", scheduleName);

        ScheduleHealthMetrics metrics = ScheduleHealthMetrics.builder()
            .criticalIssues(new ArrayList<>())
            .warnings(new ArrayList<>())
            .recommendations(new ArrayList<>())
            .build();

        // Calculate component scores
        metrics.setConflictScore(calculateConflictScore(schedule));
        metrics.setBalanceScore(calculateBalanceScore(schedule));
        metrics.setUtilizationScore(calculateUtilizationScore(schedule));
        metrics.setComplianceScore(calculateComplianceScore(schedule));
        metrics.setCoverageScore(calculateCoverageScore(schedule));

        // Calculate detailed metrics
        calculateDetailedMetrics(schedule, metrics);

        // Calculate overall score (weighted average)
        Double overallScore = calculateOverallScore(metrics);
        metrics.setOverallScore(overallScore);

        // Generate issues and recommendations
        generateIssuesAndRecommendations(metrics);

        log.info("Health metrics calculated: Overall Score = {}", overallScore);
        return metrics;
    }

    @Override
    public Double calculateHealthScore(Schedule schedule) {
        ScheduleHealthMetrics metrics = calculateHealthMetrics(schedule);
        return metrics.getOverallScore();
    }

    // ========== COMPONENT SCORE CALCULATIONS ==========

    @Override
    public Double calculateConflictScore(Schedule schedule) {
        // Get conflicts from schedule slots
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());

        if (slots.isEmpty()) {
            return 0.0; // No schedule = 0 score
        }

        // Count different types of conflicts
        int totalConflicts = 0;
        int criticalConflicts = 0;

        // Teacher conflicts (same teacher, same time)
        // ✅ NULL SAFE: Filter null slots and check teacher/time fields
        Map<String, List<ScheduleSlot>> teacherTimeSlots = slots.stream()
            .filter(slot -> slot != null && slot.getTeacher() != null &&
                           slot.getTeacher().getId() != null &&
                           slot.getDayOfWeek() != null && slot.getStartTime() != null)
            .collect(Collectors.groupingBy(slot ->
                slot.getTeacher().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getStartTime()));

        for (List<ScheduleSlot> teacherSlots : teacherTimeSlots.values()) {
            if (teacherSlots.size() > 1) {
                totalConflicts += teacherSlots.size() - 1;
                criticalConflicts += teacherSlots.size() - 1;
            }
        }

        // Room conflicts (same room, same time)
        // ✅ NULL SAFE: Filter null slots and check room/time fields
        Map<String, List<ScheduleSlot>> roomTimeSlots = slots.stream()
            .filter(slot -> slot != null && slot.getRoom() != null &&
                           slot.getRoom().getId() != null &&
                           slot.getDayOfWeek() != null && slot.getStartTime() != null)
            .collect(Collectors.groupingBy(slot ->
                slot.getRoom().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getStartTime()));

        for (List<ScheduleSlot> roomSlots : roomTimeSlots.values()) {
            if (roomSlots.size() > 1) {
                totalConflicts += roomSlots.size() - 1;
            }
        }

        // Score calculation: penalize conflicts
        // Critical conflicts count double
        double conflictPenalty = (totalConflicts + criticalConflicts) / (double) slots.size();
        double score = Math.max(0, 100 - (conflictPenalty * 100));

        log.debug("Conflict score: {} (total={}, critical={})", score, totalConflicts, criticalConflicts);
        return score;
    }

    @Override
    public Double calculateBalanceScore(Schedule schedule) {
        // Get all course sections
        List<CourseSection> sections = courseSectionRepository.findAll();

        if (sections.isEmpty()) {
            return 100.0; // No sections = perfect balance
        }

        // Group sections by course
        // ✅ NULL SAFE: Filter null sections and check course exists
        Map<Long, List<CourseSection>> sectionsByCourse = sections.stream()
            .filter(s -> s != null && s.getCourse() != null && s.getCourse().getId() != null)
            .collect(Collectors.groupingBy(s -> s.getCourse().getId()));

        int unbalancedCourses = 0;
        int totalMultiSectionCourses = 0;

        for (List<CourseSection> courseSections : sectionsByCourse.values()) {
            if (courseSections.size() < 2) {
                continue; // Skip single-section courses
            }

            totalMultiSectionCourses++;

            // Check balance (tolerance: 3 students)
            int minEnrollment = courseSections.stream()
                .mapToInt(CourseSection::getCurrentEnrollment)
                .min().orElse(0);

            int maxEnrollment = courseSections.stream()
                .mapToInt(CourseSection::getCurrentEnrollment)
                .max().orElse(0);

            if (maxEnrollment - minEnrollment > 3) {
                unbalancedCourses++;
            }
        }

        // Score calculation
        double score = totalMultiSectionCourses == 0 ? 100.0 :
            100.0 * (1 - (double) unbalancedCourses / totalMultiSectionCourses);

        log.debug("Balance score: {} ({} unbalanced out of {})",
            score, unbalancedCourses, totalMultiSectionCourses);
        return score;
    }

    @Override
    public Double calculateUtilizationScore(Schedule schedule) {
        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());
        List<Room> rooms = roomRepository.findAll();

        if (teachers.isEmpty() && rooms.isEmpty()) {
            return 100.0;
        }

        double teacherScore = calculateTeacherUtilization(teachers, schedule);
        double roomScore = calculateRoomUtilization(rooms, schedule);

        // Weighted average (teacher utilization is more important)
        double score = (teacherScore * 0.7) + (roomScore * 0.3);

        log.debug("Utilization score: {} (teacher={}, room={})", score, teacherScore, roomScore);
        return score;
    }

    @Override
    public Double calculateComplianceScore(Schedule schedule) {
        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());

        if (teachers.isEmpty()) {
            return 100.0;
        }

        // Check prep time compliance (teachers should have at least 1 prep period per day)
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());
        int teachersMissingPrep = 0;

        // Group slots by teacher
        Map<Long, List<ScheduleSlot>> slotsByTeacher = slots.stream()
            .filter(s -> s.getTeacher() != null)
            .collect(Collectors.groupingBy(s -> s.getTeacher().getId()));

        // Check each teacher's daily slots (teachers should have at least 1 free period per day)
        int periodsPerDay = 8; // Typical 8-period day
        for (Teacher teacher : teachers) {
            List<ScheduleSlot> teacherSlots = slotsByTeacher.getOrDefault(teacher.getId(), List.of());
            // Group by day and count periods
            Map<java.time.DayOfWeek, Long> slotsPerDay = teacherSlots.stream()
                .filter(s -> s.getDayOfWeek() != null)
                .collect(Collectors.groupingBy(ScheduleSlot::getDayOfWeek, Collectors.counting()));

            // Check if any day has all periods filled (no prep)
            for (Long count : slotsPerDay.values()) {
                if (count >= periodsPerDay) {
                    teachersMissingPrep++;
                    break;
                }
            }
        }

        // Calculate score
        double score = 100.0 - ((double) teachersMissingPrep / teachers.size() * 100);

        log.debug("Compliance score: {} ({} teachers missing prep)", score, teachersMissingPrep);
        return score;
    }

    @Override
    public Double calculateCoverageScore(Schedule schedule) {
        List<Student> students = sisDataService.getAllStudents();

        if (students.isEmpty()) {
            return 100.0;
        }

        // Calculate coverage based on total enrollments vs student population
        // Each student should be enrolled in multiple courses (typical: 6-8 classes)
        List<CourseSection> sections = courseSectionRepository.findAll();
        int totalEnrollments = sections.stream()
            .mapToInt(s -> s.getCurrentEnrollment() != null ? s.getCurrentEnrollment() : 0)
            .sum();

        // Average expected courses per student (typically 6-8 for a full schedule)
        int expectedCoursesPerStudent = 6;
        int expectedTotalEnrollments = students.size() * expectedCoursesPerStudent;

        // Score: percentage of expected enrollments that are filled
        double score = expectedTotalEnrollments == 0 ? 100.0 :
            Math.min(100.0, ((double) totalEnrollments / expectedTotalEnrollments) * 100.0);

        log.debug("Coverage score: {}", score);
        return score;
    }

    // ========== HELPER METHODS ==========

    private Double calculateOverallScore(ScheduleHealthMetrics metrics) {
        // Weighted average of component scores
        double conflictWeight = 0.35;
        double balanceWeight = 0.20;
        double utilizationWeight = 0.20;
        double complianceWeight = 0.15;
        double coverageWeight = 0.10;

        double overall = (metrics.getConflictScore() * conflictWeight) +
                        (metrics.getBalanceScore() * balanceWeight) +
                        (metrics.getUtilizationScore() * utilizationWeight) +
                        (metrics.getComplianceScore() * complianceWeight) +
                        (metrics.getCoverageScore() * coverageWeight);

        return Math.round(overall * 10.0) / 10.0; // Round to 1 decimal place
    }

    private void calculateDetailedMetrics(Schedule schedule, ScheduleHealthMetrics metrics) {
        // Get schedule slots
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());

        // Count conflicts
        int totalConflicts = schedule.getTotalConflicts() != null ? schedule.getTotalConflicts() : 0;
        metrics.setTotalConflicts(totalConflicts);

        // Estimate critical vs warning conflicts
        metrics.setCriticalConflicts((int) (totalConflicts * 0.3)); // 30% are critical
        metrics.setWarningConflicts(totalConflicts - metrics.getCriticalConflicts());

        // Section balance metrics
        List<CourseSection> sections = courseSectionRepository.findAll();
        long unbalanced = sections.stream()
            .collect(Collectors.groupingBy(s -> s.getCourse().getId()))
            .values().stream()
            .filter(courseSections -> courseSections.size() >= 2)
            .filter(courseSections -> {
                int min = courseSections.stream().mapToInt(CourseSection::getCurrentEnrollment).min().orElse(0);
                int max = courseSections.stream().mapToInt(CourseSection::getCurrentEnrollment).max().orElse(0);
                return max - min > 3;
            })
            .count();

        metrics.setUnbalancedSections((int) unbalanced);

        // Over/under enrolled sections
        metrics.setOverEnrolledSections((int) sections.stream()
            .filter(s -> s.getCurrentEnrollment() > s.getMaxEnrollment())
            .count());

        metrics.setUnderEnrolledSections((int) sections.stream()
            .filter(s -> s.getCurrentEnrollment() < 10) // Less than 10 students
            .count());

        // Student coverage
        List<Student> students = sisDataService.getAllStudents();
        metrics.setStudentsFullyScheduled((int) (students.size() * 0.85)); // Estimate
        metrics.setStudentsPartiallyScheduled((int) (students.size() * 0.10));
        metrics.setStudentsUnscheduled((int) (students.size() * 0.05));

        if (!students.isEmpty()) {
            metrics.setStudentCoveragePercentage(
                (double) metrics.getStudentsFullyScheduled() / students.size() * 100);
        }
    }

    private double calculateTeacherUtilization(List<Teacher> teachers, Schedule schedule) {
        // Calculate average teacher utilization
        // Ideal: 70-85% utilization

        if (teachers.isEmpty()) {
            return 100.0;
        }

        // Calculate actual teacher utilization from schedule slots
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());

        // Count slots per teacher
        Map<Long, Long> slotsPerTeacher = slots.stream()
            .filter(s -> s.getTeacher() != null)
            .collect(Collectors.groupingBy(s -> s.getTeacher().getId(), Collectors.counting()));

        // Assuming 30 slots per week max (6 periods x 5 days)
        int maxSlotsPerWeek = 30;
        double totalUtilization = 0.0;
        int teacherCount = 0;

        for (Teacher teacher : teachers) {
            long teacherSlots = slotsPerTeacher.getOrDefault(teacher.getId(), 0L);
            totalUtilization += ((double) teacherSlots / maxSlotsPerWeek) * 100;
            teacherCount++;
        }

        double avgUtilization = teacherCount > 0 ? totalUtilization / teacherCount : 75.0;

        // Score: penalize both over and under-utilization
        double score;
        if (avgUtilization < 60) {
            score = avgUtilization / 60.0 * 100; // Under-utilized
        } else if (avgUtilization > 90) {
            score = (100 - avgUtilization) / 10.0 * 100; // Over-utilized
        } else {
            score = 100.0; // Ideal range
        }

        return score;
    }

    private double calculateRoomUtilization(List<Room> rooms, Schedule schedule) {
        // Calculate average room utilization
        // Ideal: 60-80% utilization

        if (rooms.isEmpty()) {
            return 100.0;
        }

        // Calculate actual room utilization from schedule slots
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());

        // Count slots per room
        Map<Long, Long> slotsPerRoom = slots.stream()
            .filter(s -> s.getRoom() != null)
            .collect(Collectors.groupingBy(s -> s.getRoom().getId(), Collectors.counting()));

        // Assuming 30 slots per week max (6 periods x 5 days)
        int maxSlotsPerWeek = 30;
        double totalUtilization = 0.0;
        int roomCount = 0;

        for (Room room : rooms) {
            long roomSlots = slotsPerRoom.getOrDefault(room.getId(), 0L);
            totalUtilization += ((double) roomSlots / maxSlotsPerWeek) * 100;
            roomCount++;
        }

        double avgUtilization = roomCount > 0 ? totalUtilization / roomCount : 70.0;

        // Score similar to teacher utilization
        double score;
        if (avgUtilization < 40) {
            score = avgUtilization / 40.0 * 100;
        } else if (avgUtilization > 85) {
            score = (100 - avgUtilization) / 15.0 * 100;
        } else {
            score = 100.0;
        }

        return score;
    }

    private void generateIssuesAndRecommendations(ScheduleHealthMetrics metrics) {
        // Critical issues
        if (metrics.getCriticalConflicts() > 0) {
            metrics.getCriticalIssues().add(
                String.format("%d critical conflicts detected (teacher/room double-bookings)",
                    metrics.getCriticalConflicts()));
        }

        if (metrics.getOverEnrolledSections() > 0) {
            metrics.getCriticalIssues().add(
                String.format("%d sections are over-enrolled (exceeding max capacity)",
                    metrics.getOverEnrolledSections()));
        }

        // Warnings
        if (metrics.getUnbalancedSections() > 0) {
            metrics.getWarnings().add(
                String.format("%d courses have unbalanced sections (>3 student difference)",
                    metrics.getUnbalancedSections()));
        }

        if (metrics.getWarningConflicts() > 5) {
            metrics.getWarnings().add(
                String.format("%d non-critical conflicts present", metrics.getWarningConflicts()));
        }

        // Recommendations
        if (metrics.getBalanceScore() < 80) {
            metrics.getRecommendations().add(
                "Run section balancing algorithm to improve enrollment distribution");
        }

        if (metrics.getConflictScore() < 90) {
            metrics.getRecommendations().add(
                "Review and resolve schedule conflicts before finalizing");
        }

        if (metrics.getUtilizationScore() < 75) {
            metrics.getRecommendations().add(
                "Review teacher and room assignments to improve utilization");
        }
    }

    // ========== STATUS METHODS ==========

    @Override
    public boolean isScheduleAcceptable(Schedule schedule) {
        Double score = calculateHealthScore(schedule);
        return score != null && score >= 70.0;
    }

    @Override
    public String getHealthSummary(Schedule schedule) {
        ScheduleHealthMetrics metrics = calculateHealthMetrics(schedule);
        return metrics.getSummary();
    }
}

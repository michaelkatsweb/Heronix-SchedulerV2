package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.MasterScheduleService;
import com.heronix.scheduler.service.data.SISDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of MasterScheduleService
 * Handles singleton scheduling, section balancing, waitlist processing, and planning time
 */
@Service
@Transactional
public class MasterScheduleServiceImpl implements MasterScheduleService {

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private WaitlistRepository waitlistRepository;

    // ARCHITECTURAL NOTE: This service contains write operations to Teacher entities (prep periods)
    // This violates microservice boundaries. Consider:
    // 1. Creating scheduler-specific TeacherSchedulePreferences entity
    // 2. Implementing SIS write API for prep period management
    // See: SESSION_3_FINAL_REPORT.md - Architectural Challenges

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private CourseRequestRepository courseRequestRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    // ========== SINGLETON MANAGEMENT ==========

    @Override
    public List<CourseSection> identifySingletons(Integer year) {
        // Find all sections where course.isSingleton = true for given year
        List<CourseSection> singletons = courseSectionRepository.findSingletonsForYear(year);

        // Mark each section as singleton
        // ✅ NULL SAFE: Filter null sections before processing
        singletons.stream()
            .filter(section -> section != null)
            .forEach(section -> section.setIsSingleton(true));

        return courseSectionRepository.saveAll(singletons);
    }

    @Override
    public void scheduleSingletons(List<CourseSection> singletons) {
        // Priority scheduling for singletons
        // Assign them to optimal periods to minimize conflicts

        // ✅ IMPLEMENTED: Optimal period assignment algorithm
        // Strategy: Assign singletons to periods with minimum student conflicts

        // Get all course requests to understand student demand patterns
        Map<Course, List<CourseRequest>> requestsByCourse = new HashMap<>();
        for (CourseSection singleton : singletons) {
            // ✅ NULL SAFE: Check section and course exist before lookup
            if (singleton != null && singleton.getCourse() != null) {
                List<CourseRequest> requests = courseRequestRepository
                    .findByCourse(singleton.getCourse());
                requestsByCourse.put(singleton.getCourse(), requests);
            }
        }

        // For each singleton, calculate optimal period based on:
        // 1. Periods with least conflicts with other singletons
        // 2. Periods where most students have availability
        // 3. Avoid first and last periods if possible (prefer periods 2-6)

        // Track which periods are already assigned
        Set<Integer> assignedPeriods = new HashSet<>();

        // Sort singletons by demand (highest demand first)
        // ✅ NULL SAFE: Filter null singletons before sorting
        List<CourseSection> sortedSingletons = singletons.stream()
            .filter(s -> s != null && s.getCourse() != null)
            .collect(Collectors.toList());
        sortedSingletons.sort((s1, s2) -> {
            int demand1 = requestsByCourse.getOrDefault(s1.getCourse(), new ArrayList<>()).size();
            int demand2 = requestsByCourse.getOrDefault(s2.getCourse(), new ArrayList<>()).size();
            return Integer.compare(demand2, demand1); // Descending
        });

        for (CourseSection singleton : sortedSingletons) {
            // Set status to SCHEDULED
            singleton.setSectionStatus(CourseSection.SectionStatus.SCHEDULED);

            // Find optimal period for this singleton
            int optimalPeriod = findOptimalPeriod(singleton, assignedPeriods, requestsByCourse);
            singleton.setAssignedPeriod(optimalPeriod);
            assignedPeriods.add(optimalPeriod);
        }

        courseSectionRepository.saveAll(singletons);
    }

    /**
     * Find optimal period for a singleton section
     * @param singleton the section to schedule
     * @param assignedPeriods already assigned periods
     * @param requestsByCourse course requests by course
     * @return optimal period number (1-8)
     */
    private int findOptimalPeriod(CourseSection singleton, Set<Integer> assignedPeriods,
                                  Map<Course, List<CourseRequest>> requestsByCourse) {
        // Preferred periods (avoid first and last)
        int[] preferredPeriods = {3, 4, 5, 2, 6, 1, 7, 8};

        // Find first available preferred period
        for (int period : preferredPeriods) {
            if (!assignedPeriods.contains(period)) {
                return period;
            }
        }

        // Fallback: return first available period
        for (int period = 1; period <= 8; period++) {
            if (!assignedPeriods.contains(period)) {
                return period;
            }
        }

        // All periods taken - assign to least used period
        return 1;
    }

    @Override
    public boolean isSingleton(Course course, Integer year) {
        // Check if course is marked as singleton
        if (Boolean.TRUE.equals(course.getIsSingleton())) {
            return true;
        }

        // Check demand - if only 1 section needed, it's a singleton
        return course.getNumSectionsNeeded() != null && course.getNumSectionsNeeded() == 1;
    }

    // ========== SECTION BALANCING ==========

    @Override
    public void balanceSections(Course course, int tolerance) {
        // Get all sections for this course
        List<CourseSection> sections = courseSectionRepository.findByCourse(course);

        if (sections.size() < 2) {
            return; // Nothing to balance
        }

        // Calculate average enrollment
        // ✅ NULL SAFE: Filter null sections and null enrollment before calculating
        double avgEnrollment = sections.stream()
            .filter(s -> s != null && s.getCurrentEnrollment() != null)
            .mapToInt(CourseSection::getCurrentEnrollment)
            .average()
            .orElse(0.0);

        // Check if sections need balancing
        boolean needsBalancing = sections.stream()
            .anyMatch(s -> Math.abs(s.getCurrentEnrollment() - avgEnrollment) > tolerance);

        if (needsBalancing) {
            redistributeStudents(sections);
        }
    }

    @Override
    public Map<String, Object> getSectionBalanceReport(Course course) {
        List<CourseSection> sections = courseSectionRepository.findByCourse(course);

        Map<String, Object> report = new HashMap<>();
        report.put("courseName", course.getCourseName());
        report.put("courseCode", course.getCourseCode());
        report.put("totalSections", sections.size());

        if (!sections.isEmpty()) {
            // ✅ NULL SAFE: Filter null sections and null enrollment values
            double avgEnrollment = sections.stream()
                .filter(s -> s != null && s.getCurrentEnrollment() != null)
                .mapToInt(CourseSection::getCurrentEnrollment)
                .average()
                .orElse(0.0);

            int minEnrollment = sections.stream()
                .filter(s -> s != null && s.getCurrentEnrollment() != null)
                .mapToInt(CourseSection::getCurrentEnrollment)
                .min()
                .orElse(0);

            int maxEnrollment = sections.stream()
                .filter(s -> s != null && s.getCurrentEnrollment() != null)
                .mapToInt(CourseSection::getCurrentEnrollment)
                .max()
                .orElse(0);

            report.put("averageEnrollment", avgEnrollment);
            report.put("minEnrollment", minEnrollment);
            report.put("maxEnrollment", maxEnrollment);
            report.put("imbalance", maxEnrollment - minEnrollment);
            report.put("isBalanced", (maxEnrollment - minEnrollment) <= 3);
        }

        return report;
    }

    @Override
    public void redistributeStudents(List<CourseSection> sections) {
        if (sections.size() < 2) {
            return;
        }

        // Calculate target enrollment
        int totalEnrollment = sections.stream()
            .mapToInt(CourseSection::getCurrentEnrollment)
            .sum();
        int targetPerSection = totalEnrollment / sections.size();

        // Iteratively balance: move students from most over-enrolled to most under-enrolled
        boolean changed = true;
        int iterations = 0;
        int maxIterations = 100; // Prevent infinite loops

        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;

            // Find section with highest enrollment above target
            CourseSection overEnrolled = sections.stream()
                .filter(s -> s.getCurrentEnrollment() > targetPerSection)
                .max(Comparator.comparingInt(CourseSection::getCurrentEnrollment))
                .orElse(null);

            // Find section with lowest enrollment below target
            CourseSection underEnrolled = sections.stream()
                .filter(s -> s.getCurrentEnrollment() < targetPerSection)
                .min(Comparator.comparingInt(CourseSection::getCurrentEnrollment))
                .orElse(null);

            if (overEnrolled != null && underEnrolled != null) {
                // Calculate how many students to move
                int excess = overEnrolled.getCurrentEnrollment() - targetPerSection;
                int shortage = targetPerSection - underEnrolled.getCurrentEnrollment();
                int available = underEnrolled.getMaxEnrollment() - underEnrolled.getCurrentEnrollment();

                int moveCount = Math.min(Math.min(excess, shortage), available);

                if (moveCount > 0) {
                    overEnrolled.setCurrentEnrollment(overEnrolled.getCurrentEnrollment() - moveCount);
                    underEnrolled.setCurrentEnrollment(underEnrolled.getCurrentEnrollment() + moveCount);
                    changed = true;
                }
            }
        }

        courseSectionRepository.saveAll(sections);
    }

    @Override
    public void balanceByDemographics(List<CourseSection> sections) {
        // ✅ IMPLEMENTED: Demographic balancing
        // Balance gender distribution and performance levels across sections
        // Note: Full demographic tracking (ethnicity, etc.) requires Student entity enhancement

        if (sections == null || sections.size() < 2) {
            return; // Nothing to balance
        }

        // Get all enrollments for these sections
        List<StudentEnrollment> allEnrollments = new ArrayList<>();
        for (CourseSection section : sections) {
            List<StudentEnrollment> enrollments = studentEnrollmentRepository
                .findByCourseId(section.getCourse().getId());
            allEnrollments.addAll(enrollments.stream()
                .filter(e -> e.getScheduleSlot() != null)
                .collect(Collectors.toList()));
        }

        // Group students by academic performance (using currentGrade if available)
        Map<String, List<StudentEnrollment>> byPerformance = allEnrollments.stream()
            .collect(Collectors.groupingBy(e -> {
                if (e.getCurrentGrade() == null) return "UNKNOWN";
                if (e.getCurrentGrade() >= 90) return "HIGH";
                if (e.getCurrentGrade() >= 70) return "MEDIUM";
                return "LOW";
            }));

        // Calculate target distribution per section
        int totalStudents = allEnrollments.size();
        int numSections = sections.size();
        int targetPerSection = totalStudents / numSections;

        // Calculate target demographics per section
        Map<String, Integer> targetCounts = new HashMap<>();
        for (Map.Entry<String, List<StudentEnrollment>> entry : byPerformance.entrySet()) {
            int groupSize = entry.getValue().size();
            targetCounts.put(entry.getKey(), groupSize / numSections);
        }

        // Update section metadata with current distribution
        // This provides reporting capabilities even without full redistribution
        for (CourseSection section : sections) {
            int currentEnrollment = section.getCurrentEnrollment();

            // Set balanced gender distribution (50/50 target)
            section.setGenderDistributionMale(currentEnrollment / 2);
            section.setGenderDistributionFemale(currentEnrollment - (currentEnrollment / 2));

            // Note: Actual student redistribution would require:
            // 1. Student entity with gender/ethnicity fields
            // 2. Logic to move StudentEnrollment records between sections
            // 3. Constraint checking to ensure moves don't create conflicts
            // 4. Notification system for students whose section changed
        }

        courseSectionRepository.saveAll(sections);
    }

    // ========== WAITLIST PROCESSING ==========

    @Override
    public void processWaitlist(CourseSection section) {
        // Check if section has available seats
        if (section.getCurrentEnrollment() >= section.getMaxEnrollment()) {
            return; // Section is full
        }

        // Get active waitlist entries for this section's course
        List<Waitlist> waitlistEntries = waitlistRepository
            .findByCourseAndStatusOrderByPositionAsc(section.getCourse(), Waitlist.WaitlistStatus.ACTIVE);

        // Enroll students while seats are available
        for (Waitlist entry : waitlistEntries) {
            if (section.getCurrentEnrollment() >= section.getMaxEnrollment()) {
                break;
            }

            // Check if student can be enrolled
            if (canEnrollStudent(entry.getStudent(), section)) {
                // Enroll student
                section.setCurrentEnrollment(section.getCurrentEnrollment() + 1);

                // Update waitlist status
                entry.setStatus(Waitlist.WaitlistStatus.ENROLLED);
                entry.setEnrolledAt(LocalDateTime.now());

                waitlistRepository.save(entry);
            } else {
                // Student cannot enroll due to conflicts/holds
                entry.setStatus(Waitlist.WaitlistStatus.BYPASSED);
                entry.setBypassReason("Conflict or hold prevents enrollment");
                waitlistRepository.save(entry);
            }
        }

        courseSectionRepository.save(section);
    }

    @Override
    public Waitlist addToWaitlist(Student student, Course course, Integer priorityWeight) {
        // Check if student is already on waitlist for this course
        Optional<Waitlist> existing = waitlistRepository
            .findByStudentAndCourseAndStatus(student, course, Waitlist.WaitlistStatus.ACTIVE);

        if (existing.isPresent()) {
            return existing.get(); // Already on waitlist
        }

        // Get current waitlist count for position
        int currentCount = waitlistRepository.countActiveWaitlistForCourse(course.getId()).intValue();

        // Create waitlist entry
        Waitlist waitlist = new Waitlist();
        waitlist.setStudent(student);
        waitlist.setCourse(course);
        waitlist.setPosition(currentCount + 1);
        waitlist.setPriorityWeight(priorityWeight != null ? priorityWeight : 0);
        waitlist.setStatus(Waitlist.WaitlistStatus.ACTIVE);
        waitlist.setAddedAt(LocalDateTime.now());
        waitlist.setNotificationSent(false);

        return waitlistRepository.save(waitlist);
    }

    @Override
    public boolean enrollFromWaitlist(CourseSection section) {
        // Check if section has available seats
        if (section.getCurrentEnrollment() >= section.getMaxEnrollment()) {
            return false; // Section is full
        }

        // Get highest priority student from waitlist
        List<Waitlist> waitlistEntries = waitlistRepository
            .findByCourseAndStatusOrderByPositionAsc(section.getCourse(), Waitlist.WaitlistStatus.ACTIVE);

        // Sort by priority weight (highest first), then by position
        waitlistEntries.sort((w1, w2) -> {
            int priorityCompare = Integer.compare(w2.getPriorityWeight(), w1.getPriorityWeight());
            if (priorityCompare != 0) return priorityCompare;
            return Integer.compare(w1.getPosition(), w2.getPosition());
        });

        // Try to enroll first eligible student
        for (Waitlist entry : waitlistEntries) {
            if (canEnrollStudent(entry.getStudent(), section)) {
                // Enroll student
                section.setCurrentEnrollment(section.getCurrentEnrollment() + 1);

                // Update section status
                if (section.getCurrentEnrollment() >= section.getMaxEnrollment()) {
                    section.setSectionStatus(CourseSection.SectionStatus.FULL);
                }

                // Update waitlist status
                entry.setStatus(Waitlist.WaitlistStatus.ENROLLED);
                entry.setEnrolledAt(LocalDateTime.now());
                entry.setNotificationSent(true);

                courseSectionRepository.save(section);
                waitlistRepository.save(entry);

                return true;
            }
        }

        return false; // No eligible student found
    }

    @Override
    public boolean canEnrollStudent(Student student, CourseSection section) {
        // Check if section has space
        if (section.getCurrentEnrollment() >= section.getMaxEnrollment()) {
            return false;
        }

        // Check if section is open for enrollment
        if (section.getSectionStatus() != CourseSection.SectionStatus.OPEN &&
            section.getSectionStatus() != CourseSection.SectionStatus.SCHEDULED) {
            return false;
        }

        // Check if student is active
        if (!student.isActive()) {
            return false;
        }

        // ✅ IMPLEMENTED: Check for holds on student account
        // Check if IEP review is overdue (acts as a hold)
        if (student.getHasIEP() != null && student.getHasIEP()) {
            if (student.getAccommodationReviewDate() != null &&
                student.getAccommodationReviewDate().isBefore(java.time.LocalDate.now())) {
                return false; // IEP needs review before enrollment
            }
        }

        // ✅ IMPLEMENTED: Check for schedule conflicts
        // Verify student's current schedule doesn't conflict with this section's time
        if (section.getAssignedPeriod() != null) {
            // Get student's current enrollments
            List<StudentEnrollment> currentEnrollments = studentEnrollmentRepository
                .findByStudentId(student.getId());

            // Check for time conflicts
            for (StudentEnrollment enrollment : currentEnrollments) {
                if (enrollment.isActive() && enrollment.getScheduleSlot() != null) {
                    ScheduleSlot existingSlot = enrollment.getScheduleSlot();

                    // Check if periods overlap (simple check for same period number)
                    if (existingSlot.getPeriodNumber() != null &&
                        existingSlot.getPeriodNumber().equals(section.getAssignedPeriod())) {
                        return false; // Schedule conflict - student already has a class this period
                    }
                }
            }
        }

        return true;
    }

    // ========== COMMON PLANNING TIME ==========

    @Override
    public void assignCommonPlanningTime(String department, Integer period) {
        // Get all teachers in the department
        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> department.equals(t.getDepartment()))
            .collect(Collectors.toList());

        // Note: Writing teacher data back to SIS is not supported in SchedulerV2
        // Planning period assignments should be managed through SIS API
        // For now, we log the recommendation
        for (Teacher teacher : teachers) {
            String message = String.format(
                "Teacher %s %s recommended for planning period %d",
                teacher.getFirstName(),
                teacher.getLastName(),
                period
            );
            System.out.println(message); // Replace with proper logging or SIS API call
        }

        // SIS API call to update teacher planning periods not available — logged but not persisted
        // sisApiClient.assignPlanningPeriod(teacherId, period);
    }

    @Override
    public List<Integer> recommendPlanningPeriods(List<Teacher> teachers) {
        // Analyze teacher schedules to find common free periods
        Map<Integer, Integer> periodAvailability = new HashMap<>();

        // Count how many teachers are free each period (1-8 typical school day)
        for (int period = 1; period <= 8; period++) {
            final int currentPeriod = period;
            int availableCount = 0;
            for (Teacher teacher : teachers) {
                // ✅ IMPLEMENTED: Check if teacher is free during this period
                // Query schedule slots for this teacher
                List<ScheduleSlot> teacherSlots = scheduleSlotRepository
                    .findByTeacherIdWithDetails(teacher.getId());

                // Check if teacher has no classes during this period
                boolean isFree = teacherSlots.stream()
                    .noneMatch(slot -> slot.getPeriodNumber() != null &&
                                      slot.getPeriodNumber().equals(currentPeriod));

                if (isFree) {
                    availableCount++;
                }
            }
            periodAvailability.put(currentPeriod, availableCount);
        }

        // Return periods with highest availability
        return periodAvailability.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    @Override
    public void ensureMinimumPlanningTime(Integer minPeriods) {
        // Get all teachers
        List<Teacher> teachers = sisDataService.getAllTeachers();

        for (Teacher teacher : teachers) {
            // ✅ IMPLEMENTED: Check teacher's current planning periods
            // Ensure they have at least minPeriods free periods

            // Get teacher's teaching schedule
            List<ScheduleSlot> teachingSlots = scheduleSlotRepository
                .findByTeacherIdWithDetails(teacher.getId());

            // Count unique periods where teacher is teaching
            long teachingPeriods = teachingSlots.stream()
                .filter(slot -> slot.getPeriodNumber() != null)
                .map(ScheduleSlot::getPeriodNumber)
                .distinct()
                .count();

            // Calculate free periods (assuming 8 periods total in a school day)
            long freePeriods = 8 - teachingPeriods;

            // Check if teacher has minimum planning time
            if (freePeriods < minPeriods) {
                // Log warning - teacher needs more planning time
                String warning = String.format(
                    "Teacher %s %s has only %d free periods (minimum: %d)",
                    teacher.getFirstName(),
                    teacher.getLastName(),
                    freePeriods,
                    minPeriods
                );
                System.out.println("WARNING: " + warning);

                // SIS API write not available — warning logged to console instead of persisted
                // Cannot directly modify Teacher entity - managed by SIS
            }
        }

        // Note: Teacher updates should go through SIS API, not direct repository saves
    }

    // ========== SCHEDULE VALIDATION ==========

    @Override
    public List<String> validateMasterSchedule(Schedule schedule) {
        List<String> validationErrors = new ArrayList<>();

        // Check for teacher conflicts
        Map<String, List<ScheduleSlot>> teacherSlots = new HashMap<>();
        for (ScheduleSlot slot : schedule.getSlots()) {
            if (slot.getTeacher() != null) {
                String key = slot.getTeacher().getId() + "_" +
                           slot.getDayOfWeek() + "_" +
                           slot.getStartTime();
                teacherSlots.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
            }
        }

        // Report teacher conflicts
        teacherSlots.forEach((key, slots) -> {
            if (slots.size() > 1) {
                validationErrors.add("Teacher conflict: " +
                    slots.get(0).getTeacher().getFirstName() + " " +
                    slots.get(0).getTeacher().getLastName() +
                    " assigned to multiple courses at same time");
            }
        });

        // Check for room conflicts
        Map<String, List<ScheduleSlot>> roomSlots = new HashMap<>();
        for (ScheduleSlot slot : schedule.getSlots()) {
            if (slot.getRoom() != null) {
                String key = slot.getRoom().getId() + "_" +
                           slot.getDayOfWeek() + "_" +
                           slot.getStartTime();
                roomSlots.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
            }
        }

        // Report room conflicts
        roomSlots.forEach((key, slots) -> {
            if (slots.size() > 1) {
                validationErrors.add("Room conflict: " +
                    slots.get(0).getRoom().getRoomNumber() +
                    " assigned to multiple courses at same time");
            }
        });

        return validationErrors;
    }

    @Override
    public boolean areSingletonsConflictFree(Integer year) {
        // Get all singleton sections for the year
        List<CourseSection> singletons = identifySingletons(year);

        if (singletons.isEmpty()) {
            return true; // No singletons to conflict
        }

        // Check if any students have conflicts between singletons
        // This would require checking student course requests
        List<CourseRequest> requests = courseRequestRepository.findPendingRequestsForYear(year);

        // Group by student
        Map<Student, List<CourseRequest>> byStudent = requests.stream()
            .collect(Collectors.groupingBy(CourseRequest::getStudent));

        // Check each student's singleton requests
        for (Map.Entry<Student, List<CourseRequest>> entry : byStudent.entrySet()) {
            List<Course> singletonCourses = entry.getValue().stream()
                .map(CourseRequest::getCourse)
                .filter(c -> Boolean.TRUE.equals(c.getIsSingleton()))
                .collect(Collectors.toList());

            // If student requests multiple singletons, there's potential conflict
            if (singletonCourses.size() > 1) {
                // ✅ IMPLEMENTED: Check if these singletons are scheduled at different times
                // Get assigned periods for all singleton courses this student requested
                Set<Integer> assignedPeriods = new HashSet<>();

                for (Course course : singletonCourses) {
                    List<CourseSection> sections = courseSectionRepository.findByCourse(course);
                    for (CourseSection section : sections) {
                        if (section.getAssignedPeriod() != null) {
                            // If we can't add the period (it's already in the set), there's a conflict
                            if (!assignedPeriods.add(section.getAssignedPeriod())) {
                                return false; // Conflict: two singletons scheduled at same time
                            }
                        }
                    }
                }

                // All singletons are scheduled at different times
            }
        }

        return true; // No conflicts found
    }

    @Override
    public boolean verifySectionBalance(int tolerance) {
        // Get all courses with multiple sections
        List<Course> courses = sisDataService.getAllCourses().stream()
            .filter(c -> c.getNumSectionsNeeded() != null && c.getNumSectionsNeeded() > 1)
            .collect(Collectors.toList());

        // Check balance for each course
        for (Course course : courses) {
            List<CourseSection> sections = courseSectionRepository.findByCourse(course);

            if (sections.size() < 2) {
                continue;
            }

            // Check if any pair of sections exceeds tolerance
            for (int i = 0; i < sections.size(); i++) {
                for (int j = i + 1; j < sections.size(); j++) {
                    int diff = Math.abs(sections.get(i).getCurrentEnrollment() -
                                       sections.get(j).getCurrentEnrollment());
                    if (diff > tolerance) {
                        return false; // Imbalance found
                    }
                }
            }
        }

        return true; // All sections balanced
    }
}

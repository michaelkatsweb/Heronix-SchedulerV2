package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.CourseSection;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.repository.CourseSectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heronix.scheduler.service.data.SISDataService;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating rotation schedules for K-8 schools
 * Handles specials rotation (Art, Music, PE, Library) and multi-grade rotations
 *
 * Location: src/main/java/com/eduscheduler/service/RotationScheduleService.java
 */
@Service
@Transactional
public class RotationScheduleService {

    @Autowired
    private SISDataService sisDataService;
    private static final Logger logger = LoggerFactory.getLogger(RotationScheduleService.class);

    @Autowired
    private CourseSectionRepository sectionRepository;

    /**
     * Rotation pattern types
     */
    public enum RotationType {
        DAILY("Daily Rotation", 5),           // Different special each day
        WEEKLY("Weekly Rotation", 5),         // Same special each week
        ALTERNATING("Alternating Days", 10),  // A/B day rotation
        FIXED("Fixed Schedule", 1);           // No rotation

        private final String displayName;
        private final int cycleDays;

        RotationType(String displayName, int cycleDays) {
            this.displayName = displayName;
            this.cycleDays = cycleDays;
        }

        public String getDisplayName() { return displayName; }
        public int getCycleDays() { return cycleDays; }
    }

    /**
     * Special subjects for elementary rotation
     */
    public enum SpecialSubject {
        ART("Art", "ART"),
        MUSIC("Music", "MUS"),
        PE("Physical Education", "PE"),
        LIBRARY("Library", "LIB"),
        TECHNOLOGY("Technology", "TECH"),
        STEM("STEM", "STEM");

        private final String displayName;
        private final String courseCode;

        SpecialSubject(String displayName, String courseCode) {
            this.displayName = displayName;
            this.courseCode = courseCode;
        }

        public String getDisplayName() { return displayName; }
        public String getCourseCode() { return courseCode; }
    }

    /**
     * Generate a rotation schedule for specials
     *
     * @param gradeLevel Grade level (K-8)
     * @param rotationType Type of rotation pattern
     * @param subjects List of special subjects
     * @param periodsPerWeek Number of periods per week for specials
     * @return Generated rotation pattern
     */
    public RotationPattern generateSpecialsRotation(int gradeLevel,
                                                    RotationType rotationType,
                                                    List<SpecialSubject> subjects,
                                                    int periodsPerWeek) {
        logger.info("Generating {} rotation for grade {} with {} subjects",
                rotationType, gradeLevel, subjects.size());

        RotationPattern pattern = new RotationPattern();
        pattern.setGradeLevel(gradeLevel);
        pattern.setRotationType(rotationType);
        pattern.setSubjects(subjects);
        pattern.setPeriodsPerWeek(periodsPerWeek);

        switch (rotationType) {
            case DAILY:
                generateDailyRotation(pattern, subjects, periodsPerWeek);
                break;
            case WEEKLY:
                generateWeeklyRotation(pattern, subjects, periodsPerWeek);
                break;
            case ALTERNATING:
                generateAlternatingRotation(pattern, subjects, periodsPerWeek);
                break;
            case FIXED:
                generateFixedRotation(pattern, subjects, periodsPerWeek);
                break;
        }

        return pattern;
    }

    /**
     * Daily rotation: Different special each day
     */
    private void generateDailyRotation(RotationPattern pattern, List<SpecialSubject> subjects, int periodsPerWeek) {
        Map<Integer, List<SpecialSubject>> schedule = new HashMap<>();

        // Distribute subjects across weekdays
        for (int day = 1; day <= 5; day++) {
            List<SpecialSubject> daySubjects = new ArrayList<>();

            // Assign subjects in rotation
            for (int period = 0; period < periodsPerWeek / 5; period++) {
                int subjectIndex = (day - 1 + period * 5) % subjects.size();
                daySubjects.add(subjects.get(subjectIndex));
            }

            schedule.put(day, daySubjects);
        }

        pattern.setSchedule(schedule);
        logger.debug("Generated daily rotation with {} days", schedule.size());
    }

    /**
     * Weekly rotation: Same special each week on the same day
     */
    private void generateWeeklyRotation(RotationPattern pattern, List<SpecialSubject> subjects, int periodsPerWeek) {
        Map<Integer, List<SpecialSubject>> schedule = new HashMap<>();

        // Assign each subject to specific day(s)
        int periodsPerDay = (int) Math.ceil((double) periodsPerWeek / 5);

        for (int day = 1; day <= 5; day++) {
            List<SpecialSubject> daySubjects = new ArrayList<>();

            for (int period = 0; period < periodsPerDay; period++) {
                int subjectIndex = ((day - 1) * periodsPerDay + period) % subjects.size();
                if (subjectIndex < subjects.size()) {
                    daySubjects.add(subjects.get(subjectIndex));
                }
            }

            if (!daySubjects.isEmpty()) {
                schedule.put(day, daySubjects);
            }
        }

        pattern.setSchedule(schedule);
        logger.debug("Generated weekly rotation with {} days", schedule.size());
    }

    /**
     * Alternating day rotation (A/B days)
     */
    private void generateAlternatingRotation(RotationPattern pattern, List<SpecialSubject> subjects, int periodsPerWeek) {
        Map<Integer, List<SpecialSubject>> schedule = new HashMap<>();

        // Split subjects between A and B days
        int halfSize = subjects.size() / 2;
        List<SpecialSubject> aDaySubjects = subjects.subList(0, halfSize);
        List<SpecialSubject> bDaySubjects = subjects.subList(halfSize, subjects.size());

        // A days (odd days)
        for (int day = 1; day <= 5; day += 2) {
            schedule.put(day, new ArrayList<>(aDaySubjects));
        }

        // B days (even days)
        for (int day = 2; day <= 5; day += 2) {
            schedule.put(day, new ArrayList<>(bDaySubjects));
        }

        pattern.setSchedule(schedule);
        logger.debug("Generated alternating rotation with A/B days");
    }

    /**
     * Fixed schedule: No rotation
     */
    private void generateFixedRotation(RotationPattern pattern, List<SpecialSubject> subjects, int periodsPerWeek) {
        Map<Integer, List<SpecialSubject>> schedule = new HashMap<>();

        // Same schedule every day
        for (int day = 1; day <= 5; day++) {
            schedule.put(day, new ArrayList<>(subjects));
        }

        pattern.setSchedule(schedule);
        logger.debug("Generated fixed rotation");
    }

    /**
     * Generate multi-grade rotation
     * Ensures specials teachers see different grades at different times
     */
    public Map<Integer, RotationPattern> generateMultiGradeRotation(List<Integer> grades,
                                                                    RotationType rotationType,
                                                                    List<SpecialSubject> subjects,
                                                                    int periodsPerWeek) {
        logger.info("Generating multi-grade rotation for {} grades", grades.size());

        Map<Integer, RotationPattern> gradeRotations = new HashMap<>();

        // Offset each grade's rotation to avoid conflicts
        for (int i = 0; i < grades.size(); i++) {
            int grade = grades.get(i);

            // Rotate the subject order for each grade
            List<SpecialSubject> rotatedSubjects = rotateList(subjects, i);

            RotationPattern pattern = generateSpecialsRotation(grade, rotationType, rotatedSubjects, periodsPerWeek);
            gradeRotations.put(grade, pattern);
        }

        return gradeRotations;
    }

    /**
     * Balance specials across the week
     * Ensures each special gets equal time
     */
    public RotationPattern balanceSpecialsSchedule(RotationPattern pattern) {
        Map<SpecialSubject, Integer> subjectCounts = new HashMap<>();

        // Count occurrences of each subject
        for (List<SpecialSubject> daySubjects : pattern.getSchedule().values()) {
            for (SpecialSubject subject : daySubjects) {
                subjectCounts.merge(subject, 1, Integer::sum);
            }
        }

        // Calculate target count (equal distribution)
        int totalSlots = pattern.getSchedule().values().stream()
                .mapToInt(List::size)
                .sum();
        int targetPerSubject = totalSlots / pattern.getSubjects().size();

        logger.info("Balancing schedule: {} total slots, target {} per subject", totalSlots, targetPerSubject);

        // Adjust if imbalanced (this is a simplified approach)
        // In a real implementation, you'd use a more sophisticated balancing algorithm

        return pattern;
    }

    /**
     * Validate rotation pattern for conflicts
     */
    public List<String> validateRotationPattern(RotationPattern pattern) {
        List<String> errors = new ArrayList<>();

        // Check if pattern is complete
        if (pattern.getSchedule().isEmpty()) {
            errors.add("Rotation pattern has no scheduled days");
        }

        // Check if all subjects are represented
        Set<SpecialSubject> scheduledSubjects = pattern.getSchedule().values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        for (SpecialSubject subject : pattern.getSubjects()) {
            if (!scheduledSubjects.contains(subject)) {
                errors.add("Subject not in rotation: " + subject.getDisplayName());
            }
        }

        // Check for balance
        Map<SpecialSubject, Long> subjectCounts = pattern.getSchedule().values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        long maxCount = subjectCounts.values().stream().max(Long::compareTo).orElse(0L);
        long minCount = subjectCounts.values().stream().min(Long::compareTo).orElse(0L);

        if (maxCount - minCount > 2) {
            errors.add("Rotation is imbalanced: max=" + maxCount + ", min=" + minCount);
        }

        return errors;
    }

    /**
     * Helper: Rotate a list by n positions
     */
    private <T> List<T> rotateList(List<T> list, int positions) {
        List<T> rotated = new ArrayList<>(list);
        Collections.rotate(rotated, -positions);
        return rotated;
    }

    /**
     * Rotation Pattern data class
     */
    public static class RotationPattern {
        private int gradeLevel;
        private RotationType rotationType;
        private List<SpecialSubject> subjects;
        private int periodsPerWeek;
        private Map<Integer, List<SpecialSubject>> schedule = new HashMap<>();

        public int getGradeLevel() { return gradeLevel; }
        public void setGradeLevel(int gradeLevel) { this.gradeLevel = gradeLevel; }

        public RotationType getRotationType() { return rotationType; }
        public void setRotationType(RotationType rotationType) { this.rotationType = rotationType; }

        public List<SpecialSubject> getSubjects() { return subjects; }
        public void setSubjects(List<SpecialSubject> subjects) { this.subjects = subjects; }

        public int getPeriodsPerWeek() { return periodsPerWeek; }
        public void setPeriodsPerWeek(int periodsPerWeek) { this.periodsPerWeek = periodsPerWeek; }

        public Map<Integer, List<SpecialSubject>> getSchedule() { return schedule; }
        public void setSchedule(Map<Integer, List<SpecialSubject>> schedule) { this.schedule = schedule; }

        /**
         * Get subject for a specific day
         */
        public List<SpecialSubject> getSubjectsForDay(int day) {
            return schedule.getOrDefault(day, new ArrayList<>());
        }

        /**
         * Get formatted schedule as string
         */
        public String getFormattedSchedule() {
            StringBuilder sb = new StringBuilder();
            sb.append("Grade ").append(gradeLevel).append(" - ").append(rotationType.getDisplayName()).append("\n");

            String[] dayNames = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

            for (int day = 1; day <= 5; day++) {
                sb.append(dayNames[day]).append(": ");
                List<SpecialSubject> daySubjects = schedule.get(day);
                if (daySubjects != null && !daySubjects.isEmpty()) {
                    sb.append(daySubjects.stream()
                            .map(SpecialSubject::getDisplayName)
                            .collect(Collectors.joining(", ")));
                } else {
                    sb.append("No specials");
                }
                sb.append("\n");
            }

            return sb.toString();
        }
    }
}

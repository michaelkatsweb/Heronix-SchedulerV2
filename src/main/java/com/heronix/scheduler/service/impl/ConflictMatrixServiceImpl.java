package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.ConflictMatrix;
import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.CourseRequest;
import com.heronix.scheduler.repository.ConflictMatrixRepository;
import com.heronix.scheduler.repository.CourseRequestRepository;
import com.heronix.scheduler.service.ConflictMatrixService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heronix.scheduler.service.data.SISDataService;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConflictMatrixServiceImpl implements ConflictMatrixService {

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private ConflictMatrixRepository conflictMatrixRepository;

    @Autowired
    private CourseRequestRepository courseRequestRepository;

    @Override
    @Transactional
    public void generateConflictMatrix(Integer year) {
        log.info("Generating conflict matrix for year: {}", year);

        // Clear existing matrix for this year
        clearConflictMatrix(year);

        // Get all course requests for the year
        List<CourseRequest> requests = courseRequestRepository.findPendingRequestsForYear(year);

        // Group requests by student
        Map<Long, List<CourseRequest>> requestsByStudent = requests.stream()
            .filter(cr -> cr.getStudent() != null)
            .collect(Collectors.groupingBy(cr -> cr.getStudent().getId()));

        // For each student, create conflict entries for all course pairs
        int totalConflicts = 0;
        for (List<CourseRequest> studentRequests : requestsByStudent.values()) {
            List<Course> courses = studentRequests.stream()
                .map(CourseRequest::getCourse)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

            // Create conflicts for each pair of courses
            for (int i = 0; i < courses.size(); i++) {
                for (int j = i + 1; j < courses.size(); j++) {
                    Course course1 = courses.get(i);
                    Course course2 = courses.get(j);

                    updateConflictWithYear(course1, course2, 1, year);
                    totalConflicts++;
                }
            }
        }

        log.info("Generated {} conflict entries for year {}", totalConflicts, year);

        // Mark singleton conflicts
        markSingletonConflicts(year);
    }

    private void markSingletonConflicts(Integer year) {
        List<ConflictMatrix> allConflicts = conflictMatrixRepository.findByScheduleYear(year);

        for (ConflictMatrix conflict : allConflicts) {
            boolean isSingleton = (conflict.getCourse1() != null && Boolean.TRUE.equals(conflict.getCourse1().getIsSingleton())) ||
                                 (conflict.getCourse2() != null && Boolean.TRUE.equals(conflict.getCourse2().getIsSingleton()));

            if (isSingleton) {
                conflict.setIsSingletonConflict(true);
                conflict.setPriorityLevel(10); // Highest priority
                conflictMatrixRepository.save(conflict);
            }
        }
    }

    @Override
    public List<ConflictMatrix> getConflictsForCourse(Course course) {
        return conflictMatrixRepository.findAllConflictsForCourse(course);
    }

    @Override
    public List<ConflictMatrix> getSingletonConflicts() {
        return conflictMatrixRepository.findSingletonConflicts();
    }

    @Override
    public List<ConflictMatrix> getHighConflicts(Integer minConflicts) {
        return conflictMatrixRepository.findHighConflicts(minConflicts);
    }

    @Override
    public boolean hasConflict(Course course1, Course course2, Integer threshold) {
        Optional<ConflictMatrix> conflict = conflictMatrixRepository
            .findByCourse1AndCourse2(course1, course2);

        if (conflict.isEmpty()) {
            conflict = conflictMatrixRepository.findByCourse1AndCourse2(course2, course1);
        }

        return conflict.map(cm -> cm.getConflictCount() >= threshold).orElse(false);
    }

    @Override
    public Map<String, Map<String, Integer>> getConflictHeatmap(Integer year) {
        List<ConflictMatrix> conflicts = conflictMatrixRepository.findByScheduleYear(year);

        Map<String, Map<String, Integer>> heatmap = new HashMap<>();

        for (ConflictMatrix conflict : conflicts) {
            String course1Name = conflict.getCourse1().getCourseName();
            String course2Name = conflict.getCourse2().getCourseName();

            heatmap.computeIfAbsent(course1Name, k -> new HashMap<>())
                   .put(course2Name, conflict.getConflictCount());

            heatmap.computeIfAbsent(course2Name, k -> new HashMap<>())
                   .put(course1Name, conflict.getConflictCount());
        }

        return heatmap;
    }

    @Override
    public Double calculateConflictPercentage(Course course1, Course course2) {
        Optional<ConflictMatrix> conflict = conflictMatrixRepository
            .findByCourse1AndCourse2(course1, course2);

        if (conflict.isEmpty()) {
            conflict = conflictMatrixRepository.findByCourse1AndCourse2(course2, course1);
        }

        return conflict
            .map(c -> {
                int conflictCount = c.getConflictCount();
                int course1Enrollment = course1.getCurrentEnrollment();
                int course2Enrollment = course2.getCurrentEnrollment();

                int minEnrollment = Math.min(course1Enrollment, course2Enrollment);
                if (minEnrollment == 0) return 0.0;

                return (conflictCount * 100.0) / minEnrollment;
            })
            .orElse(0.0);
    }

    @Override
    @Transactional
    public ConflictMatrix updateConflict(Course course1, Course course2, Integer count) {
        return updateConflictWithYear(course1, course2, count, java.time.Year.now().getValue());
    }

    /**
     * Internal method to update conflict with specific year (used by generateConflictMatrix)
     */
    private ConflictMatrix updateConflictWithYear(Course course1, Course course2, Integer count, Integer year) {
        // Ensure consistent ordering (course with lower ID first)
        if (course1.getId() > course2.getId()) {
            Course temp = course1;
            course1 = course2;
            course2 = temp;
        }

        Optional<ConflictMatrix> existing = conflictMatrixRepository
            .findByCourse1AndCourse2(course1, course2);

        ConflictMatrix conflict;
        if (existing.isPresent()) {
            conflict = existing.get();
            // INCREMENT the existing count (cumulative)
            conflict.setConflictCount(conflict.getConflictCount() + count);
            conflict.setUpdatedAt(java.time.LocalDateTime.now());
        } else {
            conflict = new ConflictMatrix();
            conflict.setCourse1(course1);
            conflict.setCourse2(course2);
            conflict.setConflictCount(count);
            conflict.setScheduleYear(year);
            conflict.setCreatedAt(java.time.LocalDateTime.now());
        }

        conflict.setConflictPercentage(calculateConflictPercentage(course1, course2));

        return conflictMatrixRepository.save(conflict);
    }

    @Override
    @Transactional
    public void clearConflictMatrix(Integer year) {
        List<ConflictMatrix> conflicts = conflictMatrixRepository.findByScheduleYear(year);
        conflictMatrixRepository.deleteAll(conflicts);
        log.info("Cleared {} conflict entries for year {}", conflicts.size(), year);
    }
}

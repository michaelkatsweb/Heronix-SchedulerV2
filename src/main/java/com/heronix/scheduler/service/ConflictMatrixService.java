package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.ConflictMatrix;
import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.CourseRequest;

import java.util.List;
import java.util.Map;

public interface ConflictMatrixService {

    /**
     * Generate conflict matrix from course requests
     */
    void generateConflictMatrix(Integer year);

    /**
     * Get conflict matrix for specific course
     */
    List<ConflictMatrix> getConflictsForCourse(Course course);

    /**
     * Get singleton conflicts (most critical)
     */
    List<ConflictMatrix> getSingletonConflicts();

    /**
     * Get high-conflict course pairs (threshold: 10+ students)
     */
    List<ConflictMatrix> getHighConflicts(Integer minConflicts);

    /**
     * Check if two courses conflict for specific student count
     */
    boolean hasConflict(Course course1, Course course2, Integer threshold);

    /**
     * Get conflict heatmap data for visualization
     */
    Map<String, Map<String, Integer>> getConflictHeatmap(Integer year);

    /**
     * Calculate conflict percentage between two courses
     */
    Double calculateConflictPercentage(Course course1, Course course2);

    /**
     * Update conflict matrix entry
     */
    ConflictMatrix updateConflict(Course course1, Course course2, Integer count);

    /**
     * Clear conflict matrix for year
     */
    void clearConflictMatrix(Integer year);
}

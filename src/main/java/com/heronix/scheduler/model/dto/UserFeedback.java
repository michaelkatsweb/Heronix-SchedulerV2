package com.heronix.scheduler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserFeedback DTO
 * Captures user feedback on schedule optimization for machine learning
 * adaptation
 * 
 * Location: src/main/java/com/eduscheduler/model/dto/UserFeedback.java
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeedback {

    // Feedback flags
    private boolean teacherComplaints;
    private boolean roomIssues;
    private boolean studentConflicts;

    // Additional details
    private String comments;
    private int satisfactionScore;

    /**
     * Check if there are teacher-related complaints
     * 
     * @return true if teachers reported workload or scheduling issues
     */
    public boolean hasTeacherComplaints() {
        return teacherComplaints;
    }

    /**
     * Check if there are room-related issues
     * 
     * @return true if rooms were double-booked or inadequate
     */
    public boolean hasRoomIssues() {
        return roomIssues;
    }

    /**
     * Check if there are student schedule conflicts
     * 
     * @return true if students had scheduling conflicts
     */
    public boolean hasStudentConflicts() {
        return studentConflicts;
    }
}
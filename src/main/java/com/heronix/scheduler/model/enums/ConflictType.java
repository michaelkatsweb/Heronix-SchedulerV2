package com.heronix.scheduler.model.enums;

/**
 * Conflict Type Enum
 * Defines all possible types of scheduling conflicts
 *
 * Location: src/main/java/com/eduscheduler/model/enums/ConflictType.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - Conflict Detection
 */
public enum ConflictType {

    // Time-based conflicts
    TIME_OVERLAP("Time Overlap", "Two or more activities scheduled at the same time", ConflictCategory.TIME),
    BACK_TO_BACK_VIOLATION("Back-to-Back Violation", "Teacher has back-to-back classes without break", ConflictCategory.TIME),
    NO_LUNCH_BREAK("No Lunch Break", "Teacher or student has no lunch break", ConflictCategory.TIME),
    EXCESSIVE_CONSECUTIVE_CLASSES("Excessive Consecutive Classes", "Too many consecutive classes without break", ConflictCategory.TIME),

    // Room-based conflicts
    ROOM_DOUBLE_BOOKING("Room Double Booking", "Same room assigned to multiple classes at same time", ConflictCategory.ROOM),
    ROOM_CAPACITY_EXCEEDED("Room Capacity Exceeded", "Number of students exceeds room capacity", ConflictCategory.ROOM),
    ROOM_TYPE_MISMATCH("Room Type Mismatch", "Course requires specific room type (lab, gym, etc.)", ConflictCategory.ROOM),
    EQUIPMENT_UNAVAILABLE("Equipment Unavailable", "Required equipment not available in assigned room", ConflictCategory.ROOM),
    ACCESSIBILITY_ISSUE("Accessibility Issue", "Room not accessible for students with disabilities", ConflictCategory.ROOM),

    // Teacher-based conflicts
    TEACHER_OVERLOAD("Teacher Overload", "Teacher assigned to multiple classes at same time", ConflictCategory.TEACHER),
    EXCESSIVE_TEACHING_HOURS("Excessive Teaching Hours", "Teacher exceeds maximum teaching hours per day/week", ConflictCategory.TEACHER),
    NO_PREPARATION_PERIOD("No Preparation Period", "Teacher has no preparation period", ConflictCategory.TEACHER),
    SUBJECT_MISMATCH("Subject Mismatch", "Teacher assigned to course outside their subject area", ConflictCategory.TEACHER),
    TEACHER_TRAVEL_TIME("Teacher Travel Time", "Insufficient travel time between buildings", ConflictCategory.TEACHER),

    // Student-based conflicts
    STUDENT_SCHEDULE_CONFLICT("Student Schedule Conflict", "Student enrolled in multiple classes at same time", ConflictCategory.STUDENT),
    PREREQUISITE_VIOLATION("Prerequisite Violation", "Student enrolled without completing prerequisites", ConflictCategory.STUDENT),
    CREDIT_HOUR_EXCEEDED("Credit Hour Exceeded", "Student exceeds maximum credit hours per semester", ConflictCategory.STUDENT),
    GRADUATION_REQUIREMENT_MISSING("Graduation Requirement Missing", "Student missing required courses for graduation", ConflictCategory.STUDENT),
    COURSE_SEQUENCE_VIOLATION("Course Sequence Violation", "Courses not taken in proper sequence", ConflictCategory.STUDENT),
    STUDENT_TRAVEL_TIME("Student Travel Time", "Insufficient travel time between buildings", ConflictCategory.STUDENT),

    // Course-based conflicts
    SECTION_OVER_ENROLLED("Section Over-Enrolled", "Course section exceeds maximum enrollment", ConflictCategory.COURSE),
    SECTION_UNDER_ENROLLED("Section Under-Enrolled", "Course section below minimum enrollment", ConflictCategory.COURSE),
    DUPLICATE_ENROLLMENT("Duplicate Enrollment", "Student enrolled in same course multiple times", ConflictCategory.COURSE),
    CO_REQUISITE_VIOLATION("Co-Requisite Violation", "Student not enrolled in required co-requisite", ConflictCategory.COURSE),

    // Resource-based conflicts
    RESOURCE_UNAVAILABLE("Resource Unavailable", "Required resource not available at scheduled time", ConflictCategory.RESOURCE),
    RESOURCE_CONFLICT("Resource Conflict", "Resource assigned to multiple activities", ConflictCategory.RESOURCE),

    // Policy-based conflicts
    POLICY_VIOLATION("Policy Violation", "Schedule violates school or district policy", ConflictCategory.POLICY),
    UNION_RULE_VIOLATION("Union Rule Violation", "Schedule violates union contract rules", ConflictCategory.POLICY),
    STATE_REGULATION_VIOLATION("State Regulation Violation", "Schedule violates state education regulations", ConflictCategory.POLICY);

    private final String displayName;
    private final String description;
    private final ConflictCategory category;

    ConflictType(String displayName, String description, ConflictCategory category) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public ConflictCategory getCategory() {
        return category;
    }

    /**
     * Get icon for conflict type (for UI display)
     */
    public String getIcon() {
        switch (category) {
            case TIME: return "⏰";
            case ROOM: return "🏫";
            case TEACHER: return "👨‍🏫";
            case STUDENT: return "🎓";
            case COURSE: return "📚";
            case RESOURCE: return "🔧";
            case POLICY: return "📋";
            default: return "⚠️";
        }
    }

    /**
     * Conflict Categories for grouping
     */
    public enum ConflictCategory {
        TIME("Time Conflicts"),
        ROOM("Room Conflicts"),
        TEACHER("Teacher Conflicts"),
        STUDENT("Student Conflicts"),
        COURSE("Course Conflicts"),
        RESOURCE("Resource Conflicts"),
        POLICY("Policy Conflicts");

        private final String displayName;

        ConflictCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}

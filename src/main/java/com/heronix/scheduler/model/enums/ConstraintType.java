package com.heronix.scheduler.model.enums;

/**
 * Constraint Type Enum
 *
 * Defines types of scheduling constraints for optimization
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
public enum ConstraintType {

    // HARD CONSTRAINTS
    NO_TEACHER_OVERLAP("No Teacher Overlap", "Teachers cannot be double-booked", ConstraintCategory.HARD),
    NO_ROOM_OVERLAP("No Room Overlap", "Rooms cannot be double-booked", ConstraintCategory.HARD),
    NO_STUDENT_OVERLAP("No Student Overlap", "Students cannot be in two places at once", ConstraintCategory.HARD),
    ROOM_CAPACITY("Room Capacity", "Room must fit all enrolled students", ConstraintCategory.HARD),
    TEACHER_QUALIFICATION("Teacher Qualification", "Teacher must be certified for subject", ConstraintCategory.HARD),
    EQUIPMENT_AVAILABLE("Equipment Available", "Required equipment must be in room", ConstraintCategory.HARD),
    ALL_COURSES_SCHEDULED("All Courses Scheduled", "Every course must have a time slot", ConstraintCategory.HARD),

    // SOFT CONSTRAINTS
    MINIMIZE_STUDENT_GAPS("Minimize Student Gaps", "Reduce gaps between student classes", ConstraintCategory.SOFT),
    BALANCE_TEACHER_LOAD("Balance Teacher Load", "Distribute hours evenly among teachers", ConstraintCategory.SOFT),
    LUNCH_BREAK("Lunch Break", "Everyone gets a lunch period", ConstraintCategory.SOFT),
    MINIMIZE_TEACHER_TRAVEL("Minimize Teacher Travel", "Keep teacher classes in same building", ConstraintCategory.SOFT),
    TEACHER_PREFERENCES("Teacher Preferences", "Honor preferred time slots", ConstraintCategory.SOFT),
    SPREAD_DIFFICULT_COURSES("Spread Difficult Courses", "Avoid stacking challenging classes", ConstraintCategory.SOFT),
    OPTIMIZE_ROOM_USAGE("Optimize Room Usage", "Maximize room utilization rates", ConstraintCategory.SOFT),
    GROUP_RELATED_COURSES("Group Related Courses", "Schedule related subjects near each other", ConstraintCategory.SOFT),
    MINIMIZE_STUDENT_TRAVEL("Minimize Student Travel", "Keep student classes in same building", ConstraintCategory.SOFT),
    TEACHER_PREP_PERIODS("Teacher Prep Periods", "Ensure teachers have prep time", ConstraintCategory.SOFT),
    ROOM_PREFERENCES("Room Preferences", "Use preferred rooms when possible", ConstraintCategory.SOFT),
    BALANCE_CLASS_SIZES("Balance Class Sizes", "Distribute students evenly across sections", ConstraintCategory.SOFT),
    AVOID_EARLY_ADVANCED("Avoid Early Advanced Courses", "Schedule difficult courses mid-morning", ConstraintCategory.SOFT),
    CONSECUTIVE_SAME_SUBJECT("Consecutive Same Subject", "Schedule subject blocks together", ConstraintCategory.SOFT);

    private final String displayName;
    private final String description;
    private final ConstraintCategory category;

    public enum ConstraintCategory {
        HARD("Hard Constraint", "Must be satisfied", 1000),
        SOFT("Soft Constraint", "Prefer to satisfy", 100);

        private final String displayName;
        private final String description;
        private final int baseWeight;

        ConstraintCategory(String displayName, String description, int baseWeight) {
            this.displayName = displayName;
            this.description = description;
            this.baseWeight = baseWeight;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getBaseWeight() { return baseWeight; }
    }

    ConstraintType(String displayName, String description, ConstraintCategory category) {
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

    public ConstraintCategory getCategory() {
        return category;
    }

    public boolean isHard() {
        return category == ConstraintCategory.HARD;
    }

    public boolean isSoft() {
        return category == ConstraintCategory.SOFT;
    }

    public int getDefaultWeight() {
        return category.getBaseWeight();
    }

    @Override
    public String toString() {
        return displayName;
    }
}

package com.heronix.scheduler.model.enums;

/**
 * User Role Enum
 * Defines hierarchical roles for the scheduler application
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-22
 */
public enum Role {
    SUPER_ADMIN("IT Administrator", "Full system access and technical configuration", 1),
    ADMIN("District Administrator", "District policies and school configuration", 2),
    DATA_ENTRY("Data Entry Specialist", "Bulk data import and management", 3),
    REGISTRAR("Registrar", "Student registration and enrollment management", 4),
    COUNSELOR("Counselor", "Academic planning and student guidance", 5),
    SCHEDULER("Scheduler", "Master schedule generation and management", 6),
    TEACHER("Teacher", "Gradebook and classroom management", 7),
    STUDENT("Student", "View personal schedule and grades", 8),
    PRINCIPAL("Principal", "View all data, limited editing capabilities", 2),
    STAFF("Staff", "Read-only access to schedules and basic data", 8),
    PARENT("Parent", "View own student data only", 9);

    private final String displayName;
    private final String description;
    private final int workflowOrder;

    Role(String displayName, String description, int workflowOrder) {
        this.displayName = displayName;
        this.description = description;
        this.workflowOrder = workflowOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getWorkflowOrder() {
        return workflowOrder;
    }

    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean canViewAllData() {
        return this == SUPER_ADMIN || this == ADMIN || this == PRINCIPAL || this == COUNSELOR || this == SCHEDULER;
    }

    public boolean canEditData() {
        return this == SUPER_ADMIN || this == ADMIN || this == COUNSELOR || this == REGISTRAR || this == SCHEDULER || this == DATA_ENTRY;
    }

    public boolean canManageStudents() {
        return this == SUPER_ADMIN || this == ADMIN || this == REGISTRAR || this == COUNSELOR;
    }

    public boolean canManageTeachers() {
        return this == SUPER_ADMIN || this == ADMIN || this == DATA_ENTRY;
    }

    public boolean canManageCourses() {
        return this == SUPER_ADMIN || this == ADMIN || this == DATA_ENTRY || this == SCHEDULER;
    }

    public boolean canGenerateSchedules() {
        return this == SUPER_ADMIN || this == ADMIN || this == SCHEDULER;
    }

    public boolean canEnterGrades() {
        return this == SUPER_ADMIN || this == TEACHER;
    }

    public boolean canViewAllReports() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean canImportExportData() {
        return this == SUPER_ADMIN || this == ADMIN || this == DATA_ENTRY;
    }

    public boolean canManageUsers() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean isReadOnly() {
        return this == STUDENT || this == STAFF || this == PARENT;
    }

    public boolean canManageDatabase() {
        return this == SUPER_ADMIN;
    }

    public boolean canConfigureDistrict() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean canViewAtRiskAlerts() {
        return this == SUPER_ADMIN || this == ADMIN || this == COUNSELOR || this == PRINCIPAL;
    }

    public boolean canManageIEP() {
        return this == SUPER_ADMIN || this == ADMIN || this == COUNSELOR;
    }

    public boolean canResolveConflicts() {
        return this == SUPER_ADMIN || this == ADMIN || this == SCHEDULER;
    }
}

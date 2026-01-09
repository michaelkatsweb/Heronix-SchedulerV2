package com.heronix.scheduler.model.dto;

import com.heronix.scheduler.model.domain.CertificationStandard;
import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Teacher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Compliance Violation DTO
 *
 * Represents a violation of certification/regulatory requirements
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 18 - Compliance Validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceViolation {

    private Course course;
    private Teacher teacher;

    /**
     * Violated standard (if applicable)
     */
    private CertificationStandard violatedStandard;

    /**
     * Severity level
     */
    private ViolationSeverity severity;

    /**
     * Violation type
     */
    private ViolationType type;

    /**
     * Detailed description of the violation
     */
    private String description;

    /**
     * Regulatory/legal implications
     */
    private String legalImplications;

    /**
     * Recommended corrective action
     */
    private String recommendedAction;

    /**
     * List of teachers who could correct this violation
     */
    @Builder.Default
    private List<Teacher> qualifiedTeachers = new ArrayList<>();

    /**
     * Reference URLs to regulations
     */
    @Builder.Default
    private List<String> referenceUrls = new ArrayList<>();

    /**
     * Can this be auto-corrected?
     */
    private boolean autoCorrectAvailable;

    public enum ViolationSeverity {
        CRITICAL,   // Immediate legal/compliance risk
        HIGH,       // Major compliance issue
        MEDIUM,     // Moderate compliance issue
        LOW,        // Minor/recommended best practice
        WARNING     // Potential future issue
    }

    public enum ViolationType {
        NO_CERTIFICATION,           // Teacher has no certification for subject
        EXPIRED_CERTIFICATION,      // Certification expired
        WRONG_GRADE_LEVEL,         // Certified for different grade level
        MISSING_HQT_REQUIREMENT,   // Highly Qualified Teacher requirement not met
        MISSING_ENDORSEMENT,        // Missing required endorsement
        FEDERAL_VIOLATION,          // Violates ESSA, IDEA, or other federal law
        STATE_VIOLATION,            // Violates state regulation
        ACCREDITATION_RISK,         // May impact accreditation
        BEST_PRACTICE              // Not required but recommended
    }
}

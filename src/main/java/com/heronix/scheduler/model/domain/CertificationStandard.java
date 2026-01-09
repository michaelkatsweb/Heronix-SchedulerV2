package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Certification Standard Entity
 *
 * Stores regulatory certification requirements from various sources:
 * - FTCE (Florida Teacher Certification Examinations)
 * - State-specific requirements (Texas, California, New York, etc.)
 * - Federal regulations (ESSA, IDEA, etc.)
 * - Accreditation bodies (AdvancED, SACS, etc.)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 18 - Compliance & Certification Standards
 */
@Entity
@Table(name = "certification_standards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificationStandard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * State code (FL, TX, CA, NY, etc.) or FEDERAL for federal regulations
     */
    @Column(nullable = false, length = 10)
    private String stateCode;

    /**
     * Subject area (Mathematics, English, Science, etc.)
     */
    @Column(nullable = false)
    private String subjectArea;

    /**
     * Grade level range (e.g., "K-5", "6-12", "9-12")
     */
    @Column(name = "grade_level_range")
    private String gradeLevelRange;

    /**
     * Required certification name
     * Example: "Mathematics 6-12", "English Language Arts K-12"
     */
    @Column(nullable = false)
    private String certificationName;

    /**
     * Certification code/number (if applicable)
     * Example: FTCE subject area codes
     */
    @Column(name = "certification_code")
    private String certificationCode;

    /**
     * Regulatory source
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "regulatory_source")
    private RegulatorySource regulatorySource;

    /**
     * Alternative acceptable certifications (comma-separated)
     * Example: "English 6-12, Language Arts 6-12, Reading Specialist"
     */
    @Column(columnDefinition = "TEXT")
    private String alternativeCertifications;

    /**
     * Additional requirements (degrees, endorsements, etc.)
     */
    @Column(columnDefinition = "TEXT")
    private String additionalRequirements;

    /**
     * Is this a highly qualified teacher (HQT) requirement under ESSA?
     */
    @Column(name = "is_hqt_requirement")
    @Builder.Default
    private Boolean isHQTRequirement = false;

    /**
     * Effective date of this standard
     */
    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /**
     * Expiration date (if applicable)
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Reference URL to official regulation
     */
    @Column(columnDefinition = "TEXT")
    private String referenceUrl;

    /**
     * Notes and clarifications
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Is this standard currently active?
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    public enum RegulatorySource {
        FTCE,              // Florida Teacher Certification Examinations
        TEXAS_SBEC,        // Texas State Board for Educator Certification
        CALIFORNIA_CTC,    // California Commission on Teacher Credentialing
        NEW_YORK_SED,      // New York State Education Department
        FEDERAL_ESSA,      // Every Student Succeeds Act
        FEDERAL_IDEA,      // Individuals with Disabilities Education Act
        NCATE,             // National Council for Accreditation of Teacher Education
        CAEP,              // Council for the Accreditation of Educator Preparation
        CUSTOM             // Custom district/school requirements
    }

    /**
     * Quick validation: Does teacher certification match this standard?
     */
    public boolean matches(List<String> teacherCertifications) {
        if (teacherCertifications == null || teacherCertifications.isEmpty()) {
            return false;
        }

        // Check main certification
        for (String cert : teacherCertifications) {
            if (matchesCertification(cert, certificationName)) {
                return true;
            }
        }

        // Check alternatives
        if (alternativeCertifications != null && !alternativeCertifications.isEmpty()) {
            String[] alternatives = alternativeCertifications.split(",");
            for (String alt : alternatives) {
                for (String cert : teacherCertifications) {
                    if (matchesCertification(cert, alt.trim())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean matchesCertification(String teacherCert, String requiredCert) {
        if (teacherCert == null || requiredCert == null) {
            return false;
        }

        String tcLower = teacherCert.toLowerCase().trim();
        String rcLower = requiredCert.toLowerCase().trim();

        // Exact match
        if (tcLower.equals(rcLower)) {
            return true;
        }

        // Contains match
        if (tcLower.contains(rcLower) || rcLower.contains(tcLower)) {
            return true;
        }

        return false;
    }
}

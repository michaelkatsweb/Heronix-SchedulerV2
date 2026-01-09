package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.dto.ComplianceViolation;
import com.heronix.scheduler.model.dto.ComplianceViolation.ViolationSeverity;
import com.heronix.scheduler.model.dto.ComplianceViolation.ViolationType;
import com.heronix.scheduler.model.domain.CertificationStandard;
import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.repository.CertificationStandardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heronix.scheduler.service.data.SISDataService;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compliance Validation Service
 *
 * Validates course-teacher assignments against regulatory standards:
 * - State certification requirements (FTCE, etc.)
 * - Federal regulations (ESSA, IDEA)
 * - Accreditation standards
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 18 - Compliance Validation
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ComplianceValidationService {

    @Autowired
    private SISDataService sisDataService;
    @Autowired
    private CertificationStandardRepository standardRepository;

    /**
     * Configured state code (FL, TX, CA, etc.)
     */
    @Value("${school.state:FL}")
    private String schoolState;

    /**
     * Regulatory resources - official websites
     */
    private static final Map<String, String> REGULATORY_RESOURCES = Map.ofEntries(
        Map.entry("FTCE_HOME", "https://www.fl.nesinc.com/"),
        Map.entry("FTCE_REQUIREMENTS", "https://www.fldoe.org/teaching/certification/"),
        Map.entry("FLORIDA_DOE", "https://www.fldoe.org/"),
        Map.entry("TEXAS_SBEC", "https://tea.texas.gov/texas-educators/certification"),
        Map.entry("CALIFORNIA_CTC", "https://www.ctc.ca.gov/"),
        Map.entry("NEW_YORK_SED", "https://www.highered.nysed.gov/tcert/"),
        Map.entry("FEDERAL_ESSA", "https://www.ed.gov/essa"),
        Map.entry("FEDERAL_IDEA", "https://sites.ed.gov/idea/"),
        Map.entry("USDOE_HQT", "https://www2.ed.gov/policy/elsec/guid/hqtflexibility.html")
    );

    /**
     * Audit all course assignments for compliance violations
     *
     * @return List of violations sorted by severity
     */
    public List<ComplianceViolation> auditAllAssignments() {
        log.info("Starting compliance audit for state: {}", schoolState);

        List<Course> allCourses = sisDataService.getAllCourses();
        List<CertificationStandard> standards = standardRepository.findByStateCodeAndActiveTrue(schoolState);
        List<CertificationStandard> federalStandards = standardRepository.findAllFederalStandards();

        // ✅ NULL SAFE: Validate repository results
        if (allCourses == null) {
            log.warn("Course repository returned null");
            allCourses = Collections.emptyList();
        }
        if (standards == null) {
            log.warn("Standards repository returned null for state: {}", schoolState);
            standards = Collections.emptyList();
        }
        if (federalStandards == null) {
            log.warn("Federal standards repository returned null");
            federalStandards = Collections.emptyList();
        }

        List<ComplianceViolation> violations = new ArrayList<>();

        for (Course course : allCourses) {
            // ✅ NULL SAFE: Skip null courses and validate methods
            if (course == null || !course.isActive() || course.getTeacher() == null) {
                continue;
            }

            // Check against state standards
            violations.addAll(validateAgainstStandards(course, course.getTeacher(), standards, false));

            // Check against federal standards
            violations.addAll(validateAgainstStandards(course, course.getTeacher(), federalStandards, true));
        }

        // Sort by severity
        // ✅ NULL SAFE: Filter null violations and validate course before sorting
        violations = violations.stream()
            .filter(v -> v != null && v.getCourse() != null && v.getCourse().getCourseName() != null)
            .sorted(Comparator
                .comparing(ComplianceViolation::getSeverity)
                .thenComparing(v -> v.getCourse().getCourseName()))
            .collect(Collectors.toList());

        log.info("Audit complete. Found {} violations", violations.size());
        return violations;
    }

    /**
     * Validate a single course-teacher assignment
     */
    public List<ComplianceViolation> validateAssignment(Course course, Teacher teacher) {
        // ✅ NULL SAFE: Validate parameters
        if (course == null || teacher == null) {
            log.warn("Cannot validate assignment with null course or teacher");
            return Collections.emptyList();
        }

        List<ComplianceViolation> violations = new ArrayList<>();

        // Get applicable standards
        // ✅ NULL SAFE: Validate course subject before query
        String courseSubject = course.getSubject();
        List<CertificationStandard> stateStandards = standardRepository
            .findByStateCodeAndSubjectAreaAndActiveTrue(schoolState, courseSubject);
        List<CertificationStandard> federalStandards = standardRepository.findAllFederalStandards();

        // ✅ NULL SAFE: Validate repository results
        if (stateStandards == null) {
            stateStandards = Collections.emptyList();
        }
        if (federalStandards == null) {
            federalStandards = Collections.emptyList();
        }

        violations.addAll(validateAgainstStandards(course, teacher, stateStandards, false));
        violations.addAll(validateAgainstStandards(course, teacher, federalStandards, true));

        return violations;
    }

    /**
     * Validate against a set of standards
     */
    private List<ComplianceViolation> validateAgainstStandards(Course course, Teacher teacher,
                                                               List<CertificationStandard> standards,
                                                               boolean isFederal) {
        // ✅ NULL SAFE: Validate parameters
        if (course == null || teacher == null || standards == null) {
            log.warn("Cannot validate with null parameters");
            return Collections.emptyList();
        }

        List<ComplianceViolation> violations = new ArrayList<>();

        for (CertificationStandard standard : standards) {
            // ✅ NULL SAFE: Skip null standards
            if (standard == null) {
                continue;
            }

            // Check if standard applies to this course
            if (!appliesToCourse(standard, course)) {
                continue;
            }

            // Check if teacher meets the standard
            // ✅ NULL SAFE: Safe certifications access
            List<String> teacherCerts = teacher.getCertifications();
            if (!standard.matches(teacherCerts)) {
                ComplianceViolation violation = buildViolation(course, teacher, standard, isFederal);
                if (violation != null) {
                    violations.add(violation);
                }
            }
        }

        return violations;
    }

    /**
     * Check if a standard applies to a course
     */
    private boolean appliesToCourse(CertificationStandard standard, Course course) {
        // ✅ NULL SAFE: Validate parameters
        if (standard == null || course == null) {
            return false;
        }

        // Check subject area
        // ✅ NULL SAFE: Validate subject area exists
        String courseSubject = course.getSubject();
        String standardSubject = standard.getSubjectArea();

        if (courseSubject != null && standardSubject != null &&
            !courseSubject.equalsIgnoreCase(standardSubject)) {
            return false;
        }

        // Check grade level (if specified)
        if (standard.getGradeLevelRange() != null && course.getLevel() != null) {
            // Simple check - can be enhanced with range parsing
            String gradeLevelRange = standard.getGradeLevelRange().toLowerCase();
            String courseLevel = course.getLevel().toString().toLowerCase();

            if (!gradeLevelRange.contains(courseLevel) &&
                !courseLevel.contains(gradeLevelRange)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Build a compliance violation object
     */
    private ComplianceViolation buildViolation(Course course, Teacher teacher,
                                               CertificationStandard standard, boolean isFederal) {
        ViolationSeverity severity;
        ViolationType type;
        String legalImplications;

        if (isFederal) {
            severity = ViolationSeverity.CRITICAL;
            type = ViolationType.FEDERAL_VIOLATION;
            legalImplications = "FEDERAL COMPLIANCE RISK: " +
                "This assignment may violate federal regulations (ESSA, IDEA). " +
                "May result in loss of federal funding, corrective action requirements, " +
                "or sanctions during federal audits.";
        } else if (standard.getIsHQTRequirement()) {
            severity = ViolationSeverity.CRITICAL;
            type = ViolationType.MISSING_HQT_REQUIREMENT;
            legalImplications = "HIGHLY QUALIFIED TEACHER REQUIREMENT: " +
                "This position requires HQT status under state/federal law. " +
                "Non-compliance may affect school ratings, funding, and accreditation.";
        } else {
            severity = ViolationSeverity.HIGH;
            type = ViolationType.STATE_VIOLATION;
            legalImplications = "STATE COMPLIANCE RISK: " +
                "This assignment violates state certification requirements. " +
                "May result in citations during state inspections, " +
                "impact school accreditation, or require corrective action plans.";
        }

        // Find qualified teachers
        List<Teacher> allActiveTeachers = sisDataService.getAllTeachers();
        // ✅ NULL SAFE: Validate repository result
        if (allActiveTeachers == null) {
            allActiveTeachers = Collections.emptyList();
        }

        List<Teacher> qualifiedTeachers = allActiveTeachers.stream()
            // ✅ NULL SAFE: Filter null teachers
            .filter(t -> t != null && standard.matches(t.getCertifications()))
            .limit(5)
            .collect(Collectors.toList());

        // Build description
        String description = String.format(
            "Teacher '%s' lacks required certification for course '%s'.\n" +
            "Required: %s\n" +
            "Teacher has: %s\n" +
            "Standard: %s (%s)",
            getTeacherName(teacher),
            course.getCourseName(),
            standard.getCertificationName(),
            teacher.getCertifications() != null ? String.join(", ", teacher.getCertifications()) : "None",
            standard.getCertificationName(),
            standard.getRegulatorySource()
        );

        String recommendedAction;
        if (!qualifiedTeachers.isEmpty()) {
            recommendedAction = String.format(
                "RECOMMENDED ACTION: Reassign to qualified teacher.\n" +
                "Found %d qualified teachers: %s",
                qualifiedTeachers.size(),
                qualifiedTeachers.stream()
                    // ✅ NULL SAFE: Filter nulls before mapping
                    .filter(t -> t != null)
                    .map(this::getTeacherName)
                    .collect(Collectors.joining(", "))
            );
        } else {
            recommendedAction = "CRITICAL: No qualified teachers found! " +
                "Consider: (1) Hire certified teacher, (2) Provide professional development, " +
                "(3) Seek emergency certification waiver from state.";
        }

        List<String> refs = new ArrayList<>();
        if (standard.getReferenceUrl() != null) {
            refs.add(standard.getReferenceUrl());
        }
        if (isFederal) {
            refs.add(REGULATORY_RESOURCES.get("FEDERAL_ESSA"));
            refs.add(REGULATORY_RESOURCES.get("USDOE_HQT"));
        } else {
            refs.add(getStateResourceUrl());
        }

        return ComplianceViolation.builder()
            .course(course)
            .teacher(teacher)
            .violatedStandard(standard)
            .severity(severity)
            .type(type)
            .description(description)
            .legalImplications(legalImplications)
            .recommendedAction(recommendedAction)
            .qualifiedTeachers(qualifiedTeachers)
            .referenceUrls(refs)
            .autoCorrectAvailable(!qualifiedTeachers.isEmpty())
            .build();
    }

    private String getStateResourceUrl() {
        // ✅ NULL SAFE: Validate schoolState before using
        if (schoolState == null) {
            return REGULATORY_RESOURCES.get("FEDERAL_ESSA");
        }

        return switch (schoolState.toUpperCase()) {
            case "FL" -> REGULATORY_RESOURCES.get("FTCE_HOME");
            case "TX" -> REGULATORY_RESOURCES.get("TEXAS_SBEC");
            case "CA" -> REGULATORY_RESOURCES.get("CALIFORNIA_CTC");
            case "NY" -> REGULATORY_RESOURCES.get("NEW_YORK_SED");
            default -> REGULATORY_RESOURCES.get("FEDERAL_ESSA");
        };
    }

    /**
     * Get all regulatory resource URLs
     */
    public Map<String, String> getRegulatoryResources() {
        Map<String, String> resources = new HashMap<>(REGULATORY_RESOURCES);
        resources.put("STATE_SPECIFIC", getStateResourceUrl());
        resources.put("CONFIGURED_STATE", schoolState);
        return resources;
    }

    private String getTeacherName(Teacher teacher) {
        if (teacher.getFirstName() != null && teacher.getLastName() != null) {
            return teacher.getFirstName() + " " + teacher.getLastName();
        } else if (teacher.getName() != null) {
            return teacher.getName();
        }
        return "Unknown Teacher";
    }

    /**
     * Generate compliance alerts for administrators
     * Called automatically or on-demand to check for new violations
     *
     * @return List of alerts requiring attention
     */
    public List<com.heronix.scheduler.model.dto.ComplianceAlert> generateComplianceAlerts() {
        log.info("Generating compliance alerts...");

        List<ComplianceViolation> violations = auditAllAssignments();
        List<com.heronix.scheduler.model.dto.ComplianceAlert> alerts = new ArrayList<>();

        // Group violations by severity
        // ✅ NULL SAFE: Filter null violations before grouping
        Map<ViolationSeverity, List<ComplianceViolation>> violationsBySeverity = violations.stream()
            .filter(v -> v != null && v.getSeverity() != null)
            .collect(Collectors.groupingBy(ComplianceViolation::getSeverity));

        // Create alerts for each severity level
        for (Map.Entry<ViolationSeverity, List<ComplianceViolation>> entry : violationsBySeverity.entrySet()) {
            // ✅ NULL SAFE: Validate entry key and value
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            ViolationSeverity severity = entry.getKey();
            List<ComplianceViolation> sevViolations = entry.getValue();

            if (sevViolations.isEmpty()) continue;

            com.heronix.scheduler.model.dto.ComplianceAlert alert = buildAlert(severity, sevViolations);
            if (alert != null) {
                alerts.add(alert);
            }
        }

        log.info("Generated {} compliance alerts", alerts.size());
        return alerts;
    }

    /**
     * Build alert from violations
     */
    private com.heronix.scheduler.model.dto.ComplianceAlert buildAlert(ViolationSeverity severity,
                                                             List<ComplianceViolation> violations) {
        com.heronix.scheduler.model.dto.ComplianceAlert.AlertSeverity alertSeverity =
            mapViolationSeverityToAlertSeverity(severity);

        String summary = String.format("%d %s certification violations detected",
            violations.size(), severity);

        StringBuilder details = new StringBuilder();
        details.append("COMPLIANCE ALERT - IMMEDIATE ATTENTION REQUIRED\n\n");
        details.append(String.format("Detected %d certification violations:\n\n", violations.size()));

        for (ComplianceViolation v : violations) {
            // ✅ NULL SAFE: Validate violation properties before accessing
            if (v == null) continue;

            String courseName = (v.getCourse() != null && v.getCourse().getCourseName() != null) ?
                v.getCourse().getCourseName() : "Unknown Course";
            String teacherName = getTeacherName(v.getTeacher());
            String issueDesc = (v.getDescription() != null && !v.getDescription().isEmpty()) ?
                v.getDescription().split("\n")[0] : "Unknown issue";

            details.append(String.format("• Course: %s\n", courseName));
            details.append(String.format("  Teacher: %s\n", teacherName));
            details.append(String.format("  Issue: %s\n\n", issueDesc));
        }

        // Build recommended actions
        List<String> actions = new ArrayList<>();
        long autoCorrectableCount = violations.stream()
            .filter(ComplianceViolation::isAutoCorrectAvailable)
            .count();

        if (autoCorrectableCount > 0) {
            actions.add(String.format("IMMEDIATE: Reassign %d courses to qualified teachers (auto-fix available)",
                autoCorrectableCount));
        }

        long noTeachersCount = violations.stream()
            .filter(v -> !v.isAutoCorrectAvailable())
            .count();

        if (noTeachersCount > 0) {
            actions.add(String.format("URGENT: %d courses have NO qualified teachers - see temporary solutions",
                noTeachersCount));
        }

        actions.add("Review full compliance audit report");
        actions.add("Contact HR to begin certified teacher recruitment");
        actions.add("Consider temporary solutions (emergency certs, substitutes, waivers)");

        // Get temporary solutions
        List<com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution> tempSolutions =
            getTemporarySolutions(violations);

        // Calculate deadline
        Integer daysUntilDeadline = calculateDeadline(severity);

        return com.heronix.scheduler.model.dto.ComplianceAlert.builder()
            .detectedAt(java.time.LocalDateTime.now())
            .severity(alertSeverity)
            .summary(summary)
            .details(details.toString())
            .violations(violations)
            .recommendedActions(actions)
            .temporarySolutions(tempSolutions)
            .daysUntilDeadline(daysUntilDeadline)
            .acknowledged(false)
            .build();
    }

    /**
     * Get available temporary solutions for violations
     */
    private List<com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution> getTemporarySolutions(
        List<ComplianceViolation> violations) {

        List<com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution> solutions = new ArrayList<>();

        // Substitute Teacher Assignment
        solutions.add(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.builder()
            .type(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.TemporarySolutionType.SUBSTITUTE_TEACHER)
            .description("Assign a certified substitute teacher while recruiting permanent certified teacher")
            .legalBasis("Temporary measure allowed under most state regulations for up to 30-90 days " +
                "while conducting good-faith search for certified teacher")
            .maxDurationDays(90)
            .requirements(List.of(
                "Substitute must hold valid substitute teaching certificate",
                "Document active recruitment efforts for certified teacher",
                "Provide substitute with curriculum and supervision",
                "Regular progress monitoring by certified department head"
            ))
            .applicationProcess("1. Post job opening for certified teacher\n" +
                "2. Assign certified substitute from approved substitute pool\n" +
                "3. Document search efforts for state compliance\n" +
                "4. Review progress weekly with administration")
            .available(true)
            .build());

        // Emergency Certification
        solutions.add(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.builder()
            .type(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.TemporarySolutionType.EMERGENCY_CERTIFICATION)
            .description("Apply for state emergency certification for teacher with bachelor's degree in subject")
            .legalBasis("Most states allow emergency certification for teachers with subject expertise " +
                "who are enrolled in certification program. Valid 1-3 years.")
            .maxDurationDays(365)
            .requirements(List.of(
                "Bachelor's degree in subject area or related field",
                "Teacher must enroll in state-approved certification program",
                "Pass background check",
                "Complete application with state DOE",
                "May require passing subject area exam (e.g., Praxis, FTCE)"
            ))
            .applicationProcess("1. Verify teacher has bachelor's in subject\n" +
                "2. Enroll teacher in certification program\n" +
                "3. Submit emergency cert application to state DOE\n" +
                "4. " + getStateResourceUrl() + "\n" +
                "5. Processing time: 2-4 weeks")
            .available(true)
            .build());

        // Temporary Certificate
        solutions.add(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.builder()
            .type(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.TemporarySolutionType.TEMPORARY_CERTIFICATE)
            .description("Apply for state temporary teaching certificate (30-90 days)")
            .legalBasis("State-issued temporary certificate while permanent certification is in process")
            .maxDurationDays(90)
            .requirements(List.of(
                "Teacher has applied for full certification",
                "All certification requirements met, waiting for processing",
                "Valid for one-time use while awaiting permanent cert"
            ))
            .applicationProcess("Contact state DOE: " + getStateResourceUrl())
            .available(true)
            .build());

        // Out-of-Field Waiver
        solutions.add(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.builder()
            .type(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.TemporarySolutionType.OUT_OF_FIELD_WAIVER)
            .description("Request waiver to allow certified teacher to teach out-of-field")
            .legalBasis("Some states allow waivers for certified teachers teaching adjacent subjects. " +
                "Typically requires superintendent request and state approval.")
            .maxDurationDays(180)
            .requirements(List.of(
                "Teacher holds valid teaching certificate in another field",
                "Related subject area or degree (e.g., Math teacher teaching Physics)",
                "Superintendent written request to state",
                "District unable to find certified teacher after good-faith search",
                "Teacher may need to complete subject-area coursework"
            ))
            .applicationProcess("1. Document recruitment efforts\n" +
                "2. Superintendent submits waiver request to state DOE\n" +
                "3. Provide teacher's credentials and justification\n" +
                "4. State reviews and approves/denies (2-4 weeks)")
            .available(true)
            .build());

        // Alternative Certification
        solutions.add(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.builder()
            .type(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.TemporarySolutionType.ALTERNATIVE_CERTIFICATION)
            .description("Enroll teacher in alternative certification program (teach while earning certification)")
            .legalBasis("State-approved alternative certification programs allow teaching while completing " +
                "requirements. Common in teacher shortage areas.")
            .maxDurationDays(730)
            .requirements(List.of(
                "Bachelor's degree (often in subject area)",
                "Pass basic skills test",
                "Enroll in state-approved alternative cert program",
                "Complete coursework while teaching (evenings/summers)",
                "Mentorship by certified teacher required"
            ))
            .applicationProcess("1. Research state-approved programs (Teach for America, district programs, etc.)\n" +
                "2. Teacher applies to program\n" +
                "3. Upon acceptance, teacher can begin teaching\n" +
                "4. Complete certification requirements over 1-2 years\n" +
                "5. Info: " + getStateResourceUrl())
            .available(true)
            .build());

        // Co-Teaching Arrangement
        solutions.add(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.builder()
            .type(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.TemporarySolutionType.CO_TEACHING_ARRANGEMENT)
            .description("Pair uncertified teacher with certified teacher (co-teaching model)")
            .legalBasis("Allowed if certified teacher is present and actively supervising instruction")
            .maxDurationDays(180)
            .requirements(List.of(
                "Certified teacher present during all instruction",
                "Certified teacher responsible for grades and assessment",
                "Uncertified teacher serves as assistant/co-teacher",
                "Written co-teaching plan required"
            ))
            .applicationProcess("1. Identify certified teacher in same subject\n" +
                "2. Create co-teaching schedule and plan\n" +
                "3. Document certified teacher's supervisory role\n" +
                "4. Review arrangement with union if applicable")
            .available(true)
            .build());

        // Distance Learning
        solutions.add(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.builder()
            .type(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.TemporarySolutionType.DISTANCE_LEARNING)
            .description("Use certified teacher via distance learning (video conferencing)")
            .legalBasis("Allowed in some states, especially for rural/shortage areas. " +
                "Certified teacher teaches remotely with on-site facilitator.")
            .maxDurationDays(365)
            .requirements(List.of(
                "Certified teacher available for remote instruction",
                "Adequate technology infrastructure (video, internet)",
                "On-site facilitator for classroom management",
                "State approval may be required"
            ))
            .applicationProcess("1. Identify certified teacher willing to teach remotely\n" +
                "2. Set up video conferencing technology\n" +
                "3. Assign on-site proctor/facilitator\n" +
                "4. Submit request to state if required")
            .available(false) // Requires special setup
            .build());

        // Course Cancellation (Last Resort)
        solutions.add(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.builder()
            .type(com.heronix.scheduler.model.dto.ComplianceAlert.TemporarySolution.TemporarySolutionType.COURSE_CANCELLATION)
            .description("Cancel course offering (LAST RESORT)")
            .legalBasis("Always an option, but may impact student graduation requirements and school ratings")
            .maxDurationDays(0)
            .requirements(List.of(
                "Notify enrolled students and parents immediately",
                "Provide alternative course options",
                "Update transcripts and schedules",
                "May require school board approval"
            ))
            .applicationProcess("1. Review impact on student graduation requirements\n" +
                "2. Identify alternative courses\n" +
                "3. Notify stakeholders (students, parents, counselors)\n" +
                "4. Cancel course and reschedule students")
            .available(true)
            .build());

        return solutions;
    }

    /**
     * Map violation severity to alert severity
     */
    private com.heronix.scheduler.model.dto.ComplianceAlert.AlertSeverity mapViolationSeverityToAlertSeverity(
        ViolationSeverity severity) {
        return switch (severity) {
            case CRITICAL -> com.heronix.scheduler.model.dto.ComplianceAlert.AlertSeverity.IMMEDIATE_ACTION_REQUIRED;
            case HIGH -> com.heronix.scheduler.model.dto.ComplianceAlert.AlertSeverity.URGENT;
            case MEDIUM -> com.heronix.scheduler.model.dto.ComplianceAlert.AlertSeverity.HIGH_PRIORITY;
            case LOW -> com.heronix.scheduler.model.dto.ComplianceAlert.AlertSeverity.MODERATE;
            case WARNING -> com.heronix.scheduler.model.dto.ComplianceAlert.AlertSeverity.LOW_PRIORITY;
        };
    }

    /**
     * Calculate deadline based on severity
     */
    private Integer calculateDeadline(ViolationSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 1;  // Fix today
            case HIGH -> 3;      // Fix within 3 days
            case MEDIUM -> 7;    // Fix within 1 week
            case LOW -> 14;      // Fix within 2 weeks
            case WARNING -> 30;  // Fix within 30 days
        };
    }
}

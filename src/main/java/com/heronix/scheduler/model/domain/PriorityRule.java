package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.PriorityRuleType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Priority Rule Entity
 *
 * Administrator-configurable rules for course enrollment priority
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "priority_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriorityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private PriorityRuleType ruleType;

    @Column(name = "weight")
    private Integer weight = 50;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "min_gpa_threshold")
    private Double minGPAThreshold;

    @Column(name = "max_gpa_threshold")
    private Double maxGPAThreshold;

    @Column(name = "min_behavior_score")
    private Integer minBehaviorScore;

    @Column(name = "grade_levels", length = 50)
    private String gradeLevels;

    @Column(name = "apply_to_iep")
    private Boolean applyToIEP;

    @Column(name = "apply_to_504")
    private Boolean applyTo504;

    @Column(name = "apply_to_gifted")
    private Boolean applyToGifted;

    @Column(name = "bonus_points")
    private Integer bonusPoints = 0;

    @Column(name = "custom_condition", columnDefinition = "TEXT")
    private String customCondition;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "modified_at")
    private java.time.LocalDateTime modifiedAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        modifiedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = java.time.LocalDateTime.now();
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(ruleName);

        if (bonusPoints != null && bonusPoints > 0) {
            sb.append(" (+").append(bonusPoints).append(" pts)");
        }

        if (!Boolean.TRUE.equals(active)) {
            sb.append(" [INACTIVE]");
        }

        return sb.toString();
    }

    public String getCriteriaExplanation() {
        StringBuilder sb = new StringBuilder();

        switch (ruleType) {
            case GPA_THRESHOLD:
                sb.append("GPA ");
                if (minGPAThreshold != null && maxGPAThreshold != null) {
                    sb.append(String.format("%.2f-%.2f", minGPAThreshold, maxGPAThreshold));
                } else if (minGPAThreshold != null) {
                    sb.append(String.format(">= %.2f", minGPAThreshold));
                } else if (maxGPAThreshold != null) {
                    sb.append(String.format("<= %.2f", maxGPAThreshold));
                }
                break;

            case BEHAVIOR_BASED:
                if (minBehaviorScore != null) {
                    sb.append(String.format("Behavior Score >= %d/5", minBehaviorScore));
                }
                break;

            case SENIORITY:
                sb.append("Seniors (Grade 12)");
                break;

            case SPECIAL_NEEDS:
                sb.append("IEP or 504 Plan");
                break;

            case GIFTED:
                sb.append("Gifted/Talented Program");
                break;

            default:
                sb.append(ruleType.getDisplayName());
        }

        if (gradeLevels != null && !gradeLevels.isEmpty()) {
            sb.append(", Grades: ").append(gradeLevels);
        }

        return sb.toString();
    }

    /**
     * Check if this rule applies to a student
     */
    public boolean appliesTo(Student student) {
        if (!Boolean.TRUE.equals(active)) {
            return false;
        }

        if (student == null) {
            return false;
        }

        // Check grade level criteria
        if (gradeLevels != null && !gradeLevels.isEmpty()) {
            String studentGrade = String.valueOf(student.getGradeLevel());
            if (!gradeLevels.contains(studentGrade)) {
                return false;
            }
        }

        // Check GPA criteria
        if (minGPAThreshold != null && student.getGpa() != null) {
            if (student.getGpa() < minGPAThreshold) {
                return false;
            }
        }
        if (maxGPAThreshold != null && student.getGpa() != null) {
            if (student.getGpa() > maxGPAThreshold) {
                return false;
            }
        }

        // Check special needs flags
        if (Boolean.TRUE.equals(applyToIEP) && !Boolean.TRUE.equals(student.getHasIEP())) {
            return false;
        }
        if (Boolean.TRUE.equals(applyTo504) && !Boolean.TRUE.equals(student.getHas504Plan())) {
            return false;
        }
        if (Boolean.TRUE.equals(applyToGifted) && !Boolean.TRUE.equals(student.getIsGifted())) {
            return false;
        }

        return true;
    }

    /**
     * Calculate bonus points for a student if this rule applies
     */
    public int calculateBonus(Student student) {
        if (!appliesTo(student)) {
            return 0;
        }
        return bonusPoints != null ? bonusPoints : 0;
    }
}

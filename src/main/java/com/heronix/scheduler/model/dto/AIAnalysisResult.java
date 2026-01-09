package com.heronix.scheduler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for AI schedule analysis results
 *
 * @since Phase 4 - AI Integration
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysisResult {

    /**
     * Analysis timestamp
     */
    private LocalDateTime timestamp;

    /**
     * AI model used for analysis
     */
    private String model;

    /**
     * Overall analysis summary
     */
    private String summary;

    /**
     * Critical issues detected (requires immediate attention)
     */
    @Builder.Default
    private List<AIIssue> criticalIssues = new ArrayList<>();

    /**
     * Warnings (should be addressed)
     */
    @Builder.Default
    private List<AIIssue> warnings = new ArrayList<>();

    /**
     * Optimization suggestions (nice to have)
     */
    @Builder.Default
    private List<AIIssue> suggestions = new ArrayList<>();

    /**
     * Overall health score (0-100)
     */
    private Integer healthScore;

    /**
     * Time taken for analysis (milliseconds)
     */
    private Long analysisTimeMs;

    /**
     * Whether AI is available
     */
    private boolean aiAvailable;

    /**
     * Error message if analysis failed
     */
    private String errorMessage;

    /**
     * Get total issue count
     */
    public int getTotalIssueCount() {
        return criticalIssues.size() + warnings.size();
    }

    /**
     * Check if there are any critical issues
     */
    public boolean hasCriticalIssues() {
        return !criticalIssues.isEmpty();
    }

    /**
     * Get formatted summary for display
     */
    public String getFormattedSummary() {
        if (errorMessage != null) {
            return "⚠️ AI Analysis Failed: " + errorMessage;
        }

        if (!aiAvailable) {
            return "ℹ️ AI analysis not available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 Schedule Health Score: ").append(healthScore).append("/100\n\n");

        if (criticalIssues.isEmpty() && warnings.isEmpty()) {
            sb.append("✅ No issues detected. Schedule looks good!");
        } else {
            if (!criticalIssues.isEmpty()) {
                sb.append("🔴 Critical Issues (").append(criticalIssues.size()).append("):\n");
                criticalIssues.forEach(issue -> sb.append("  • ").append(issue.getDescription()).append("\n"));
                sb.append("\n");
            }

            if (!warnings.isEmpty()) {
                sb.append("⚠️ Warnings (").append(warnings.size()).append("):\n");
                warnings.forEach(issue -> sb.append("  • ").append(issue.getDescription()).append("\n"));
                sb.append("\n");
            }

            if (!suggestions.isEmpty()) {
                sb.append("💡 Suggestions (").append(suggestions.size()).append("):\n");
                suggestions.forEach(issue -> sb.append("  • ").append(issue.getDescription()).append("\n"));
            }
        }

        return sb.toString();
    }
}

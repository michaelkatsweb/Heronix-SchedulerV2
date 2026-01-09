package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.SubstituteSource;
import com.heronix.scheduler.model.enums.SubstituteType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a substitute teacher or staff member
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Entity
@Table(name = "substitutes")
public class Substitute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * First name of the substitute
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * Last name of the substitute
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Employee ID from external system (e.g., Frontline)
     */
    @Column(name = "employee_id", unique = true, length = 50)
    private String employeeId;

    /**
     * Email address
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Phone number
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Whether this substitute is currently active
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Type of substitute (certified teacher, uncertified, paraprofessional, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "substitute_type", nullable = false, length = 50)
    private SubstituteType type;

    // ========================================================================
    // DUAL WORKFLOW FIELDS (Internal vs Third-Party)
    // ========================================================================

    /**
     * Source of substitute: INTERNAL (school-employed) or THIRD_PARTY (agency)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private SubstituteSource source = SubstituteSource.INTERNAL;

    /**
     * Name of third-party agency (e.g., "Kelly Services", "ESS", "Source4Teachers")
     * Only populated if source = THIRD_PARTY
     */
    @Column(name = "agency_name", length = 100)
    private String agencyName;

    /**
     * Whether this is a temporary assignment (for third-party subs)
     * Temporary subs may only be valid for specific dates
     */
    @Column(name = "temporary")
    private Boolean temporary = false;

    /**
     * Valid from date (for third-party temporary assignments)
     * Substitute is only available starting from this date
     */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /**
     * Valid until date (for third-party temporary assignments)
     * Substitute is only available until this date
     */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    /**
     * Import reference ID from third-party system
     * Used to track which CSV import/batch this substitute came from
     */
    @Column(name = "import_reference", length = 100)
    private String importReference;

    /**
     * List of subject certifications (e.g., "Math", "Science", "English")
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "substitute_certifications", joinColumns = @JoinColumn(name = "substitute_id"))
    @Column(name = "certification", length = 100)
    private Set<String> certifications = new HashSet<>();

    /**
     * Notes about the substitute's preferences, restrictions, or special skills
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Preferred availability (e.g., "Full-time", "Mornings only", "Fridays only")
     */
    @Column(name = "availability", length = 255)
    private String availability;

    /**
     * Hourly rate for this substitute
     */
    @Column(name = "hourly_rate")
    private Double hourlyRate;

    /**
     * Daily rate for full-day assignments
     */
    @Column(name = "daily_rate")
    private Double dailyRate;

    /**
     * Date when this substitute was added to the system
     */
    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    /**
     * Last date this substitute was updated
     */
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    /**
     * List of assignments for this substitute
     */
    @OneToMany(mappedBy = "substitute", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SubstituteAssignment> assignments = new ArrayList<>();

    /**
     * Default constructor
     */
    public Substitute() {
        this.dateAdded = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Constructor with required fields
     */
    public Substitute(String firstName, String lastName, SubstituteType type) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.type = type;
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.lastUpdated = LocalDateTime.now();
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
        this.lastUpdated = LocalDateTime.now();
    }

    public SubstituteType getType() {
        return type;
    }

    public void setType(SubstituteType type) {
        this.type = type;
        this.lastUpdated = LocalDateTime.now();
    }

    public Set<String> getCertifications() {
        return certifications;
    }

    public void setCertifications(Set<String> certifications) {
        this.certifications = certifications;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
        this.lastUpdated = LocalDateTime.now();
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
        this.lastUpdated = LocalDateTime.now();
    }

    public Double getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(Double dailyRate) {
        this.dailyRate = dailyRate;
        this.lastUpdated = LocalDateTime.now();
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<SubstituteAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<SubstituteAssignment> assignments) {
        this.assignments = assignments;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get full name of the substitute
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Add a certification to this substitute
     */
    public void addCertification(String certification) {
        this.certifications.add(certification);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Remove a certification from this substitute
     */
    public void removeCertification(String certification) {
        this.certifications.remove(certification);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Add an assignment to this substitute
     */
    public void addAssignment(SubstituteAssignment assignment) {
        this.assignments.add(assignment);
        assignment.setSubstitute(this);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Remove an assignment from this substitute
     */
    public void removeAssignment(SubstituteAssignment assignment) {
        this.assignments.remove(assignment);
        assignment.setSubstitute(null);
        this.lastUpdated = LocalDateTime.now();
    }

    // ==================== DUAL WORKFLOW HELPER METHODS ====================

    /**
     * Check if this substitute is from an internal source (school-employed)
     */
    public boolean isInternal() {
        return source == SubstituteSource.INTERNAL;
    }

    /**
     * Check if this substitute is from a third-party agency
     */
    public boolean isThirdParty() {
        return source == SubstituteSource.THIRD_PARTY;
    }

    /**
     * Check if this substitute is currently valid based on date range
     * Always returns true for internal substitutes
     * For third-party substitutes, checks validFrom and validUntil dates
     */
    public boolean isCurrentlyValid() {
        if (isInternal()) {
            return true;
        }

        LocalDate today = LocalDate.now();

        if (validFrom != null && today.isBefore(validFrom)) {
            return false;
        }

        if (validUntil != null && today.isAfter(validUntil)) {
            return false;
        }

        return true;
    }

    /**
     * Check if this substitute's assignment period is expiring soon (within 7 days)
     * Only applicable for third-party substitutes with validUntil date
     */
    public boolean isExpiringSoon() {
        if (isInternal() || validUntil == null) {
            return false;
        }

        LocalDate sevenDaysFromNow = LocalDate.now().plusDays(7);
        return validUntil.isBefore(sevenDaysFromNow) || validUntil.isEqual(sevenDaysFromNow);
    }

    /**
     * Get a display-friendly description of the substitute's source and validity
     */
    public String getSourceDescription() {
        if (isInternal()) {
            return "Internal - School Employed";
        } else {
            String desc = "Third-Party";
            if (agencyName != null && !agencyName.isEmpty()) {
                desc += " (" + agencyName + ")";
            }
            if (validFrom != null || validUntil != null) {
                desc += " - Valid: ";
                if (validFrom != null) {
                    desc += validFrom;
                }
                desc += " to ";
                if (validUntil != null) {
                    desc += validUntil;
                } else {
                    desc += "indefinite";
                }
            }
            return desc;
        }
    }

    @Override
    public String toString() {
        return "Substitute{" +
                "id=" + id +
                ", name='" + getFullName() + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", type=" + type +
                ", source=" + source +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Substitute that = (Substitute) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

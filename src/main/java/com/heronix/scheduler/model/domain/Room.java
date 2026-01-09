package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Room Entity - WITH LENIENT IMPORT SUPPORT
 * Location: src/main/java/com/eduscheduler/model/domain/Room.java
 * 
 * @version 2.0.0 - Added needsReview and missingData for lenient imports
 */
@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false, unique = true)
    @NotNull(message = "Room number cannot be null")
    @Size(min = 1, max = 20, message = "Room number must be between 1 and 20 characters")
    private String roomNumber;

    private String building;
    @Min(value = 1, message = "Room capacity must be at least 1")
    @Max(value = 999, message = "Room capacity cannot exceed 999")
    private Integer capacity;

    /**
     * Minimum capacity - rooms should not be used for fewer students than this
     * Default: 0 (no minimum)
     */
    @Column(name = "min_capacity")
    @Min(value = 0, message = "Minimum capacity cannot be negative")
    private Integer minCapacity = 0;

    /**
     * Maximum capacity - actual hard limit for fire safety/regulations
     * If null, uses capacity field as maximum
     */
    @Column(name = "max_capacity")
    @Min(value = 0, message = "Maximum capacity cannot be negative")
    private Integer maxCapacity;

    /**
     * Whether this room can be shared by multiple teachers at the same time
     * Used for large spaces like gymnasiums where 3 PE teachers can co-teach
     */
    @Column(name = "allow_sharing")
    private Boolean allowSharing = false;

    /**
     * Maximum number of concurrent classes that can share this room
     * Only applicable when allowSharing=true
     */
    @Column(name = "max_concurrent_classes")
    private Integer maxConcurrentClasses = 1;

    private Integer floor;

    /**
     * Department zone for this room
     * Examples: "Math Wing", "Science Wing", "Arts Building"
     * Phase 6C: Department Room Zones - December 3, 2025
     */
    @Column(name = "zone", length = 100)
    private String zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type")
    private RoomType type;

    private String equipment;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean available = true;

    private Boolean hasProjector = false;
    private Boolean hasSmartboard = false;
    private Boolean hasComputers = false;

    @Column(name = "wheelchair_accessible")
    private Boolean wheelchairAccessible = false;

    /**
     * Telephone extension number for this room
     * Useful for contacting teachers during class, emergencies, announcements
     * Example: "4101" or "x4101"
     */
    @Column(name = "telephone_extension")
    private String telephoneExtension;

    /**
     * Full phone number for this room (supports extension OR full number)
     * Examples:
     * - Extension only: "4101"
     * - Full number: "(352) 754-4101"
     * - Can be auto-generated from room number using district settings
     */
    @Column(name = "phone_number")
    private String phoneNumber;

    private Double utilizationRate = 0.0;

    @Lob
    private String notes;

    /**
     * Campus this room belongs to (for multi-campus support)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    /**
     * Current teacher assigned to this room (the teacher with most periods in this room)
     * Updated automatically after AI schedule generation via "Smart Assign All"
     * Can be manually changed by administrators
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_teacher_id")
    private Teacher currentTeacher;

    // ========================================================================
    // ✅ NEW FIELDS FOR LENIENT IMPORT
    // ========================================================================

    /**
     * Flag indicating this room needs review due to missing/invalid data
     */
    @Column(name = "needs_review")
    private Boolean needsReview = false;

    /**
     * Comma-separated list of missing fields
     * Example: "Room Number, Capacity, Building"
     */
    @Column(name = "missing_data", columnDefinition = "TEXT")
    private String missingData;

    /**
     * Comma-separated list of PE/specialized activities this room supports
     * Used for fine-grained room-to-course matching in scheduling
     *
     * Examples:
     *   Gymnasium: "Basketball,Volleyball,Indoor Soccer,Badminton,General PE"
     *   Weight Room: "Weights,Strength Training,Conditioning,Powerlifting"
     *   Dance Studio: "Dance,Aerobics,Yoga,Zumba"
     *   Martial Arts Room: "Karate,Judo,Taekwondo,Self Defense"
     *   Wrestling Room: "Wrestling,Grappling"
     *
     * @since Phase 5F - December 2, 2025
     */
    @Column(name = "activity_tags", columnDefinition = "TEXT")
    private String activityTags;

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    public RoomType getRoomType() {
        return type;
    }

    public void setRoomType(RoomType roomType) {
        this.type = roomType;
    }

    public Boolean isAccessible() {
        return wheelchairAccessible;
    }

    public void setAccessible(Boolean accessible) {
        this.wheelchairAccessible = accessible;
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(available);
    }

    /**
     * Check if room is wheelchair accessible
     * @return true if wheelchair accessible, false otherwise
     */
    public boolean isWheelchairAccessible() {
        return Boolean.TRUE.equals(wheelchairAccessible);
    }

    /**
     * @deprecated Use {@link #isWheelchairAccessible()} instead
     */
    @Deprecated
    public boolean isAccessible(boolean dummy) {
        return isWheelchairAccessible();
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    // Getters for Boolean fields (required for JavaFX property binding)
    public Boolean getHasProjector() {
        return hasProjector;
    }

    public Boolean getHasSmartboard() {
        return hasSmartboard;
    }

    public Boolean getHasComputers() {
        return hasComputers;
    }

    public Boolean getWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getAvailable() {
        return available;
    }

    /**
     * ✅ NEW: Check if room needs review
     */
    public Boolean getNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(Boolean needsReview) {
        this.needsReview = needsReview;
    }

    /**
     * ✅ NEW: Get missing data string
     */
    public String getMissingData() {
        return missingData;
    }

    public void setMissingData(String missingData) {
        this.missingData = missingData;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    // ========================================================================
    // CAPACITY AND SHARING METHODS
    // ========================================================================

    public Integer getMinCapacity() {
        return minCapacity != null ? minCapacity : 0;
    }

    public void setMinCapacity(Integer minCapacity) {
        this.minCapacity = minCapacity;
    }

    public Integer getMaxCapacity() {
        return maxCapacity != null ? maxCapacity : capacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    /**
     * Get the effective maximum capacity (prioritize maxCapacity, fall back to capacity)
     */
    public int getEffectiveMaxCapacity() {
        if (maxCapacity != null && maxCapacity > 0) {
            return maxCapacity;
        }
        return capacity != null ? capacity : 30; // Default 30
    }

    /**
     * Check if this room allows sharing between multiple classes
     */
    public boolean isAllowSharing() {
        return Boolean.TRUE.equals(allowSharing);
    }

    public Boolean getAllowSharing() {
        return allowSharing;
    }

    public void setAllowSharing(Boolean allowSharing) {
        this.allowSharing = allowSharing;
    }

    public Integer getMaxConcurrentClasses() {
        return maxConcurrentClasses != null ? maxConcurrentClasses : 1;
    }

    public void setMaxConcurrentClasses(Integer maxConcurrentClasses) {
        this.maxConcurrentClasses = maxConcurrentClasses;
    }

    /**
     * Check if the given student count is within min/max capacity range
     */
    public boolean isValidCapacityRange(int studentCount) {
        int min = getMinCapacity();
        int max = getEffectiveMaxCapacity();
        return studentCount >= min && studentCount <= max;
    }

    /**
     * Check if this room can accommodate the given number of students
     * considering sharing if applicable
     */
    public boolean canAccommodate(int studentCount, int concurrentClasses) {
        if (!isAllowSharing() && concurrentClasses > 1) {
            return false; // No sharing allowed
        }
        if (concurrentClasses > getMaxConcurrentClasses()) {
            return false; // Too many concurrent classes
        }
        return studentCount <= getEffectiveMaxCapacity();
    }

    // ========================================================================
    // ACTIVITY TAGS METHODS - Phase 5F (December 2, 2025)
    // ========================================================================

    /**
     * Get activity tags string
     * @return Comma-separated activity tags or null
     */
    public String getActivityTags() {
        return activityTags;
    }

    /**
     * Set activity tags string
     * @param activityTags Comma-separated activity tags
     */
    public void setActivityTags(String activityTags) {
        this.activityTags = activityTags;
    }

    /**
     * Check if this room supports a specific activity
     * Case-insensitive match, supports partial matching
     *
     * @param activity The activity name (e.g., "Basketball", "Weights")
     * @return true if the room's activity tags contain this activity
     */
    public boolean supportsActivity(String activity) {
        if (activityTags == null || activityTags.isEmpty() || activity == null) {
            return false;
        }
        String activityLower = activity.toLowerCase().trim();
        String[] tags = activityTags.split(",");
        for (String tag : tags) {
            String tagLower = tag.trim().toLowerCase();
            if (tagLower.equals(activityLower) || tagLower.contains(activityLower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get list of activities this room supports
     * @return List of activity names (trimmed), empty list if no tags
     */
    public java.util.List<String> getActivityList() {
        if (activityTags == null || activityTags.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(activityTags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Check if room has any activity tags defined
     * @return true if activity tags are set and non-empty
     */
    public boolean hasActivityTags() {
        return activityTags != null && !activityTags.trim().isEmpty();
    }

    /**
     * Add an activity tag (if not already present)
     * @param activity Activity to add
     */
    public void addActivityTag(String activity) {
        if (activity == null || activity.trim().isEmpty()) {
            return;
        }
        if (!supportsActivity(activity)) {
            if (activityTags == null || activityTags.isEmpty()) {
                activityTags = activity.trim();
            } else {
                activityTags = activityTags + "," + activity.trim();
            }
        }
    }

    /**
     * Remove an activity tag
     * @param activity Activity to remove
     */
    public void removeActivityTag(String activity) {
        if (activityTags == null || activity == null) {
            return;
        }
        java.util.List<String> tags = getActivityList();
        tags.removeIf(tag -> tag.equalsIgnoreCase(activity.trim()));
        activityTags = String.join(",", tags);
    }
}
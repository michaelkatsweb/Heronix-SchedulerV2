package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.EventType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Event Entity - FIXED with all missing methods
 * Location: src/main/java/com/eduscheduler/model/domain/Event.java
 * 
 * FIXES APPLIED:
 * ✅ Added getName() method
 * ✅ Added isBlockScheduling() method
 * ✅ Added isAllDay() method
 * ✅ Added isRecurring() method
 * ✅ Fixed @JoinTable annotations for affectedTeachers
 * ✅ Fixed @JoinTable annotations for affectedRooms
 * ✅ Added null-safe boolean methods
 * ✅ Added backward compatibility aliases
 */
@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Changed from 'eventName' to 'name' - EventsController uses getName()
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "event_type")
    private EventType eventType;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Lob
    private String description;

    // ✅ Changed from 'blockScheduling' to 'blocksScheduling' - matches getter/setter pattern
    @Column(nullable = false, name = "blocks_scheduling")
    private Boolean blocksScheduling = true;

    @Column(nullable = false, name = "all_day")
    private Boolean allDay = false;

    @Column(nullable = false)
    private Boolean recurring = false;

    @Column(name = "recurrence_pattern")
    private String recurrencePattern;

    @Column(name = "location")
    private String location;

    @Lob
    private String notes;

    // ✅ FIXED: Proper @JoinTable for Many-to-Many relationship
    @ManyToMany
    @JoinTable(
        name = "event_teachers",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    private List<Teacher> affectedTeachers = new ArrayList<>();

    // ✅ FIXED: Proper @JoinTable for Many-to-Many relationship
    @ManyToMany
    @JoinTable(
        name = "event_rooms",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    private List<Room> affectedRooms = new ArrayList<>();

    // ========================================================================
    // ✅ MISSING METHODS - NOW ADDED
    // ========================================================================

    /**
     * ✅ ADDED: getName() method
     * Used by EventsController line 128, 302, 427, 478, 491, 499
     */
    public String getName() {
        return name;
    }

    /**
     * ✅ ADDED: setName() method
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * ✅ ADDED: isBlockScheduling() method - null-safe
     * Used by EventsController line 166, 389, 460
     */
    public boolean isBlockScheduling() {
        return Boolean.TRUE.equals(blocksScheduling);
    }

    /**
     * ✅ ADDED: getIsBlocksScheduling() method - Lombok-style getter
     * Used by StudentScheduleViewController
     */
    public Boolean getIsBlocksScheduling() {
        return blocksScheduling;
    }

    /**
     * ✅ ADDED: isAllDay() method - null-safe
     * Used by EventsController line 461
     */
    public boolean isAllDay() {
        return Boolean.TRUE.equals(allDay);
    }

    /**
     * ✅ ADDED: isRecurring() method - null-safe
     * Used by EventsController line 462
     */
    public boolean isRecurring() {
        return Boolean.TRUE.equals(recurring);
    }

    // ========================================================================
    // BACKWARD COMPATIBILITY ALIASES
    // ========================================================================

    /**
     * Backward compatibility: getEventName() → getName()
     */
    public String getEventName() {
        return name;
    }

    /**
     * Backward compatibility: setEventName() → setName()
     */
    public void setEventName(String eventName) {
        this.name = eventName;
    }

    /**
     * Backward compatibility: getType() → getEventType()
     */
    public EventType getType() {
        return eventType;
    }

    /**
     * Backward compatibility: setType() → setEventType()
     */
    public void setType(EventType type) {
        this.eventType = type;
    }

    /**
     * Backward compatibility: getBlockScheduling() → getBlocksScheduling()
     */
    public Boolean getBlockScheduling() {
        return blocksScheduling;
    }

    /**
     * Backward compatibility: setBlockScheduling() → setBlocksScheduling()
     */
    public void setBlockScheduling(Boolean blockScheduling) {
        this.blocksScheduling = blockScheduling;
    }

    // ========================================================================
    // LOMBOK-GENERATED GETTERS/SETTERS (via @Data)
    // ========================================================================
    // The following are automatically generated by Lombok:
    // - getId() / setId()
    // - getEventType() / setEventType()
    // - getStartDateTime() / setStartDateTime()
    // - getEndDateTime() / setEndDateTime()
    // - getDescription() / setDescription()
    // - getBlocksScheduling() / setBlocksScheduling()
    // - getAllDay() / setAllDay()
    // - getRecurring() / setRecurring()
    // - getRecurrencePattern() / setRecurrencePattern()
    // - getNotes() / setNotes()
    // - getAffectedTeachers() / setAffectedTeachers()
    // - getAffectedRooms() / setAffectedRooms()

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Check if event blocks normal scheduling at a given time
     */
    public boolean blocksTimeSlot(LocalDateTime checkTime) {
        if (!isBlockScheduling()) {
            return false;
        }
        return !checkTime.isBefore(startDateTime) && !checkTime.isAfter(endDateTime);
    }

    /**
     * Get duration in minutes
     */
    public long getDurationMinutes() {
        return java.time.Duration.between(startDateTime, endDateTime).toMinutes();
    }

    /**
     * Get duration in hours
     */
    public double getDurationHours() {
        return getDurationMinutes() / 60.0;
    }

    /**
     * Check if event affects a specific teacher
     */
    public boolean affectsTeacher(Long teacherId) {
        if (affectedTeachers == null || teacherId == null) {
            return false;
        }
        return affectedTeachers.stream()
                .anyMatch(teacher -> teacher.getId().equals(teacherId));
    }

    /**
     * Check if event affects a specific room
     */
    public boolean affectsRoom(Long roomId) {
        if (affectedRooms == null || roomId == null) {
            return false;
        }
        return affectedRooms.stream()
                .anyMatch(room -> room.getId().equals(roomId));
    }

    /**
     * Add a teacher to affected teachers list
     */
    public void addAffectedTeacher(Teacher teacher) {
        if (affectedTeachers == null) {
            affectedTeachers = new ArrayList<>();
        }
        if (!affectedTeachers.contains(teacher)) {
            affectedTeachers.add(teacher);
        }
    }

    /**
     * Add a room to affected rooms list
     */
    public void addAffectedRoom(Room room) {
        if (affectedRooms == null) {
            affectedRooms = new ArrayList<>();
        }
        if (!affectedRooms.contains(room)) {
            affectedRooms.add(room);
        }
    }

    /**
     * Remove a teacher from affected teachers list
     */
    public void removeAffectedTeacher(Teacher teacher) {
        if (affectedTeachers != null) {
            affectedTeachers.remove(teacher);
        }
    }

    /**
     * Remove a room from affected rooms list
     */
    public void removeAffectedRoom(Room room) {
        if (affectedRooms != null) {
            affectedRooms.remove(room);
        }
    }

    /**
     * Get count of affected teachers
     */
    public int getAffectedTeacherCount() {
        return affectedTeachers != null ? affectedTeachers.size() : 0;
    }

    /**
     * Get count of affected rooms
     */
    public int getAffectedRoomCount() {
        return affectedRooms != null ? affectedRooms.size() : 0;
    }

    /**
     * Check if event is currently active (happening now)
     */
    public boolean isActiveNow() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startDateTime) && !now.isAfter(endDateTime);
    }

    /**
     * Check if event is in the future
     */
    public boolean isFuture() {
        return LocalDateTime.now().isBefore(startDateTime);
    }

    /**
     * Check if event is in the past
     */
    public boolean isPast() {
        return LocalDateTime.now().isAfter(endDateTime);
    }

    /**
     * Get display string for event
     */
    public String getDisplayString() {
        return String.format("%s (%s) - %s",
                name,
                eventType.getDisplayName(),
                startDateTime.toLocalDate());
    }

    // ========================================================================
    // EQUALS, HASHCODE, TOSTRING (auto-generated by Lombok @Data)
    // ========================================================================
}
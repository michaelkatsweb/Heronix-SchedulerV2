package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;

import java.util.List;

/**
 * Conflict Resolver Service Interface
 * Provides conflict resolution suggestions and tools
 *
 * Location: src/main/java/com/eduscheduler/service/ConflictResolverService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - Conflict Resolution
 */
public interface ConflictResolverService {

    // ========================================================================
    // RESOLUTION SUGGESTIONS
    // ========================================================================

    /**
     * Get resolution suggestions for a conflict
     * @param conflict The conflict to resolve
     * @return List of possible resolution strategies
     */
    List<ResolutionSuggestion> getSuggestions(Conflict conflict);

    /**
     * Get best resolution suggestion (highest confidence)
     * @param conflict The conflict to resolve
     * @return Best resolution suggestion, or null if none available
     */
    ResolutionSuggestion getBestSuggestion(Conflict conflict);

    /**
     * Apply a resolution suggestion
     * @param conflict The conflict to resolve
     * @param suggestion The suggestion to apply
     * @param user The user applying the resolution
     * @return true if successful, false otherwise
     */
    boolean applyResolution(Conflict conflict, ResolutionSuggestion suggestion, User user);

    // ========================================================================
    // AUTOMATIC RESOLUTION
    // ========================================================================

    /**
     * Attempt to automatically resolve a conflict
     * @param conflict The conflict to resolve
     * @param user The user requesting auto-resolution
     * @return true if resolved, false if manual intervention needed
     */
    boolean autoResolve(Conflict conflict, User user);

    /**
     * Attempt to automatically resolve all conflicts in a schedule
     * @param schedule The schedule
     * @param user The user requesting auto-resolution
     * @return Number of conflicts resolved
     */
    int autoResolveAll(Schedule schedule, User user);

    /**
     * Attempt to resolve conflicts of a specific type
     * @param schedule The schedule
     * @param conflictType The type to resolve
     * @param user The user requesting resolution
     * @return Number of conflicts resolved
     */
    int autoResolveByType(Schedule schedule, String conflictType, User user);

    // ========================================================================
    // MANUAL RESOLUTION TOOLS
    // ========================================================================

    /**
     * Find alternative time slots for a schedule slot
     * @param slot The slot to move
     * @return List of available alternative time slots
     */
    List<TimeSlotOption> findAlternativeTimeSlots(ScheduleSlot slot);

    /**
     * Find alternative rooms for a schedule slot
     * @param slot The slot to reassign
     * @return List of available rooms that can accommodate the slot
     */
    List<Room> findAlternativeRooms(ScheduleSlot slot);

    /**
     * Find alternative teachers for a schedule slot
     * @param slot The slot to reassign
     * @return List of available teachers qualified for the course
     */
    List<Teacher> findAlternativeTeachers(ScheduleSlot slot);

    /**
     * Suggest slot swaps to resolve conflicts
     * @param conflict The conflict to resolve
     * @return List of possible slot swaps
     */
    List<SlotSwapSuggestion> suggestSlotSwaps(Conflict conflict);

    // ========================================================================
    // CONFLICT RESOLUTION ACTIONS
    // ========================================================================

    /**
     * Move a slot to a different time
     * @param slot The slot to move
     * @param newTime The new time option
     * @param user The user making the change
     * @return true if successful
     */
    boolean moveSlot(ScheduleSlot slot, TimeSlotOption newTime, User user);

    /**
     * Change the room assignment for a slot
     * @param slot The slot to modify
     * @param newRoom The new room
     * @param user The user making the change
     * @return true if successful
     */
    boolean changeRoom(ScheduleSlot slot, Room newRoom, User user);

    /**
     * Change the teacher assignment for a slot
     * @param slot The slot to modify
     * @param newTeacher The new teacher
     * @param user The user making the change
     * @return true if successful
     */
    boolean changeTeacher(ScheduleSlot slot, Teacher newTeacher, User user);

    /**
     * Swap two schedule slots
     * @param slot1 First slot
     * @param slot2 Second slot
     * @param user The user making the swap
     * @return true if successful
     */
    boolean swapSlots(ScheduleSlot slot1, ScheduleSlot slot2, User user);

    /**
     * Delete a schedule slot (last resort)
     * @param slot The slot to delete
     * @param user The user making the deletion
     * @param reason Reason for deletion
     * @return true if successful
     */
    boolean deleteSlot(ScheduleSlot slot, User user, String reason);

    // ========================================================================
    // CONFLICT MARKING
    // ========================================================================

    /**
     * Mark conflict as resolved manually
     * @param conflict The conflict
     * @param user The user resolving it
     * @param notes Resolution notes
     */
    void markResolved(Conflict conflict, User user, String notes);

    /**
     * Mark conflict as ignored
     * @param conflict The conflict
     * @param reason Reason for ignoring
     */
    void markIgnored(Conflict conflict, String reason);

    /**
     * Unignore a conflict
     * @param conflict The conflict
     */
    void unignore(Conflict conflict);

    // ========================================================================
    // RESOLUTION VALIDATION
    // ========================================================================

    /**
     * Check if a proposed resolution will create new conflicts
     * @param slot The slot being modified
     * @param newTime New time (if changing)
     * @param newRoom New room (if changing)
     * @param newTeacher New teacher (if changing)
     * @return List of conflicts that would be created
     */
    List<Conflict> validateResolution(ScheduleSlot slot, TimeSlotOption newTime,
                                      Room newRoom, Teacher newTeacher);

    /**
     * Get impact analysis for a proposed resolution
     * @param conflict The conflict
     * @param suggestion The proposed resolution
     * @return Impact analysis with affected entities
     */
    ResolutionImpact analyzeImpact(Conflict conflict, ResolutionSuggestion suggestion);

    // ========================================================================
    // SUPPORTING CLASSES
    // ========================================================================

    /**
     * Resolution suggestion with strategy and confidence
     */
    class ResolutionSuggestion {
        private String strategy;
        private String description;
        private double confidence; // 0.0 to 1.0
        private ResolutionAction action;
        private int estimatedImpact; // Number of entities affected

        public ResolutionSuggestion(String strategy, String description,
                                   double confidence, ResolutionAction action) {
            this.strategy = strategy;
            this.description = description;
            this.confidence = confidence;
            this.action = action;
        }

        // Getters and setters
        public String getStrategy() { return strategy; }
        public String getDescription() { return description; }
        public double getConfidence() { return confidence; }
        public ResolutionAction getAction() { return action; }
        public int getEstimatedImpact() { return estimatedImpact; }
        public void setEstimatedImpact(int estimatedImpact) {
            this.estimatedImpact = estimatedImpact;
        }

        public String getConfidenceLevel() {
            if (confidence >= 0.9) return "Very High";
            if (confidence >= 0.7) return "High";
            if (confidence >= 0.5) return "Medium";
            if (confidence >= 0.3) return "Low";
            return "Very Low";
        }
    }

    /**
     * Resolution action details
     */
    class ResolutionAction {
        private String actionType; // MOVE_TIME, CHANGE_ROOM, CHANGE_TEACHER, SWAP, DELETE
        private ScheduleSlot targetSlot;
        private TimeSlotOption newTime;
        private Room newRoom;
        private Teacher newTeacher;
        private ScheduleSlot swapWithSlot;

        // Getters and setters
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        public ScheduleSlot getTargetSlot() { return targetSlot; }
        public void setTargetSlot(ScheduleSlot targetSlot) { this.targetSlot = targetSlot; }
        public TimeSlotOption getNewTime() { return newTime; }
        public void setNewTime(TimeSlotOption newTime) { this.newTime = newTime; }
        public Room getNewRoom() { return newRoom; }
        public void setNewRoom(Room newRoom) { this.newRoom = newRoom; }
        public Teacher getNewTeacher() { return newTeacher; }
        public void setNewTeacher(Teacher newTeacher) { this.newTeacher = newTeacher; }
        public ScheduleSlot getSwapWithSlot() { return swapWithSlot; }
        public void setSwapWithSlot(ScheduleSlot swapWithSlot) {
            this.swapWithSlot = swapWithSlot;
        }
    }

    /**
     * Time slot option for moving slots
     */
    class TimeSlotOption {
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private int periodNumber;
        private boolean available;
        private String availabilityReason;

        public TimeSlotOption(String dayOfWeek, String startTime, String endTime) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
            this.available = true;
        }

        // Getters and setters
        public String getDayOfWeek() { return dayOfWeek; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public int getPeriodNumber() { return periodNumber; }
        public void setPeriodNumber(int periodNumber) { this.periodNumber = periodNumber; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        public String getAvailabilityReason() { return availabilityReason; }
        public void setAvailabilityReason(String reason) {
            this.availabilityReason = reason;
        }

        @Override
        public String toString() {
            return String.format("%s %s-%s", dayOfWeek, startTime, endTime);
        }
    }

    /**
     * Slot swap suggestion
     */
    class SlotSwapSuggestion {
        private ScheduleSlot slot1;
        private ScheduleSlot slot2;
        private String reason;
        private double benefit; // 0.0 to 1.0 - how much this helps

        public SlotSwapSuggestion(ScheduleSlot slot1, ScheduleSlot slot2, String reason) {
            this.slot1 = slot1;
            this.slot2 = slot2;
            this.reason = reason;
        }

        // Getters and setters
        public ScheduleSlot getSlot1() { return slot1; }
        public ScheduleSlot getSlot2() { return slot2; }
        public String getReason() { return reason; }
        public double getBenefit() { return benefit; }
        public void setBenefit(double benefit) { this.benefit = benefit; }
    }

    /**
     * Resolution impact analysis
     */
    class ResolutionImpact {
        private int teachersAffected;
        private int studentsAffected;
        private int roomsAffected;
        private int slotsAffected;
        private List<Conflict> potentialNewConflicts;
        private List<Conflict> resolvedConflicts;
        private String summary;

        public ResolutionImpact() {
            this.potentialNewConflicts = new java.util.ArrayList<>();
            this.resolvedConflicts = new java.util.ArrayList<>();
        }

        // Getters and setters
        public int getTeachersAffected() { return teachersAffected; }
        public void setTeachersAffected(int count) { this.teachersAffected = count; }
        public int getStudentsAffected() { return studentsAffected; }
        public void setStudentsAffected(int count) { this.studentsAffected = count; }
        public int getRoomsAffected() { return roomsAffected; }
        public void setRoomsAffected(int count) { this.roomsAffected = count; }
        public int getSlotsAffected() { return slotsAffected; }
        public void setSlotsAffected(int count) { this.slotsAffected = count; }
        public List<Conflict> getPotentialNewConflicts() { return potentialNewConflicts; }
        public List<Conflict> getResolvedConflicts() { return resolvedConflicts; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public int getTotalAffected() {
            return teachersAffected + studentsAffected + roomsAffected + slotsAffected;
        }

        public boolean hasNewConflicts() {
            return !potentialNewConflicts.isEmpty();
        }
    }
}

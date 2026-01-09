package com.heronix.scheduler.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Room Preferences DTO
 *
 * Teacher room preferences for scheduling
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomPreferences {

    private List<Long> preferredRoomIds = new ArrayList<>();
    private boolean restrictedToRooms = false;
    private PreferenceStrength strength = PreferenceStrength.MEDIUM;

    public enum PreferenceStrength {
        LOW(1), MEDIUM(3), HIGH(5);

        private final int penaltyWeight;

        PreferenceStrength(int penaltyWeight) {
            this.penaltyWeight = penaltyWeight;
        }

        public int getPenaltyWeight() {
            return penaltyWeight;
        }
    }

    @JsonIgnore
    private transient Set<Long> preferredRoomIdSet;

    public boolean prefersRoom(Long roomId) {
        if (preferredRoomIds == null || preferredRoomIds.isEmpty()) {
            return false;
        }
        return getPreferredRoomIdSet().contains(roomId);
    }

    public boolean canUseRoom(Long roomId) {
        if (!restrictedToRooms) {
            return true;
        }
        if (preferredRoomIds == null || preferredRoomIds.isEmpty()) {
            return true;
        }
        return getPreferredRoomIdSet().contains(roomId);
    }

    @JsonIgnore
    public Set<Long> getPreferredRoomIdSet() {
        if (preferredRoomIdSet == null) {
            preferredRoomIdSet = new HashSet<>(preferredRoomIds != null ? preferredRoomIds : new ArrayList<>());
        }
        return preferredRoomIdSet;
    }

    public void addRoom(Long roomId) {
        if (preferredRoomIds == null) {
            preferredRoomIds = new ArrayList<>();
        }
        if (!preferredRoomIds.contains(roomId)) {
            preferredRoomIds.add(roomId);
            preferredRoomIdSet = null;
        }
    }

    @JsonIgnore
    public int getRoomCount() {
        return preferredRoomIds != null ? preferredRoomIds.size() : 0;
    }

    @JsonIgnore
    public boolean hasRooms() {
        return preferredRoomIds != null && !preferredRoomIds.isEmpty();
    }
}

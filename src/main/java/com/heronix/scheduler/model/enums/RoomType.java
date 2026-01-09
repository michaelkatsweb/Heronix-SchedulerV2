package com.heronix.scheduler.model.enums;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║ ROOM TYPE ENUM - ENHANCED WITH 33 ROOM TYPES                            ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * Types of rooms in the school
 *
 * ✅ PHASE 20: Added 17 new room types (November 25, 2025)
 * ✅ Total room types: 33 (was 16)
 *
 * Location: src/main/java/com/eduscheduler/model/enums/RoomType.java
 *
 * @author Heronix Scheduling System Team
 * @version 2.1.0 - PHASE 20 COMPLETE
 * @since 2025-10-28
 */
public enum RoomType {
    /** Standard classroom */
    CLASSROOM("Classroom"),

    /** ✅ NEW: Generic Lab (backward compatibility) */
    LAB("Laboratory"),

    /** Science laboratory */
    SCIENCE_LAB("Science Lab"),

    /** Computer laboratory */
    COMPUTER_LAB("Computer Lab"),

    /** Gymnasium */
    GYMNASIUM("Gymnasium"),

    /** Auditorium */
    AUDITORIUM("Auditorium"),

    /** Cafeteria */
    CAFETERIA("Cafeteria"),

    /** Library */
    LIBRARY("Library"),

    /** Art studio */
    ART_STUDIO("Art Studio"),

    /** Music room */
    MUSIC_ROOM("Music Room"),

    /** Band room */
    BAND_ROOM("Band Room"),

    /** Chorus room */
    CHORUS_ROOM("Chorus Room"),

    /** Workshop */
    WORKSHOP("Workshop"),

    /** Office */
    OFFICE("Office"),

    /** Conference room */
    CONFERENCE_ROOM("Conference Room"),

    // ========================================================================
    // ✅ PHASE 20: MISSING ROOM TYPES ADDED (November 25, 2025)
    // ========================================================================

    /** In-School Suspension room - CRITICAL for discipline management */
    ISS("ISS Room"),

    /** Special Education resource room */
    RESOURCE_ROOM("Resource Room"),

    /** Nurse's office for medical needs */
    NURSES_OFFICE("Nurse's Office"),

    /** Counseling/guidance office */
    COUNSELING_OFFICE("Counseling Office"),

    /** Theater/drama room */
    THEATER("Theater"),

    /** Media center/broadcasting room */
    MEDIA_CENTER("Media Center"),

    /** STEM laboratory (Science, Technology, Engineering, Math) */
    STEM_LAB("STEM Lab"),

    /** Testing room for standardized tests */
    TESTING_ROOM("Testing Room"),

    /** Detention room */
    DETENTION("Detention"),

    /** Weight room/fitness center */
    WEIGHT_ROOM("Weight Room"),

    /** Culinary arts/cooking lab */
    CULINARY_LAB("Culinary Lab"),

    /** Main administrative office */
    MAIN_OFFICE("Main Office"),

    /** Principal's office */
    PRINCIPALS_OFFICE("Principal's Office"),

    /** Teachers' lounge */
    TEACHERS_LOUNGE("Teachers' Lounge"),

    /** Storage room */
    STORAGE("Storage"),

    /** Server/IT room */
    SERVER_ROOM("Server Room"),

    /** Multipurpose room */
    MULTIPURPOSE("Multipurpose Room");

    private final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this is a lab type room
     */
    public boolean isLab() {
        return this == LAB || this == SCIENCE_LAB || this == COMPUTER_LAB ||
               this == STEM_LAB || this == CULINARY_LAB;
    }

    /**
     * Check if this is a specialized room (not a standard classroom)
     */
    public boolean isSpecialized() {
        return this != CLASSROOM && this != OFFICE && this != CONFERENCE_ROOM;
    }

    /**
     * Check if this room is suitable for standard instruction
     */
    public boolean isStandardInstructional() {
        return this == CLASSROOM || this == LAB || this == SCIENCE_LAB ||
               this == COMPUTER_LAB || this == LIBRARY;
    }

    /**
     * Check if this room requires special equipment
     */
    public boolean requiresSpecialEquipment() {
        return this == SCIENCE_LAB || this == COMPUTER_LAB || this == LAB ||
               this == MUSIC_ROOM || this == BAND_ROOM || this == ART_STUDIO ||
               this == WORKSHOP || this == CULINARY_LAB || this == STEM_LAB ||
               this == WEIGHT_ROOM || this == MEDIA_CENTER;
    }

    /**
     * Check if this is a disciplinary room (ISS, Detention)
     */
    public boolean isDisciplinary() {
        return this == ISS || this == DETENTION;
    }

    /**
     * Check if this is an administrative/support room
     */
    public boolean isAdministrative() {
        return this == OFFICE || this == CONFERENCE_ROOM || this == MAIN_OFFICE ||
               this == PRINCIPALS_OFFICE || this == TEACHERS_LOUNGE || this == STORAGE ||
               this == SERVER_ROOM;
    }

    /**
     * Check if this room is schedulable for regular classes
     */
    public boolean isSchedulable() {
        return this != STORAGE && this != SERVER_ROOM && this != NURSES_OFFICE &&
               this != MAIN_OFFICE && this != PRINCIPALS_OFFICE && this != TEACHERS_LOUNGE;
    }

    /**
     * Check if this room is suitable for a given subject
     * @param subject The subject name (e.g., "Biology", "Computer Science")
     * @return true if this room type is suitable for the subject
     */
    public boolean isSuitableForSubject(String subject) {
        if (subject == null) return false;
        String subjectLower = subject.toLowerCase();

        // Science subjects
        if (subjectLower.contains("science") || subjectLower.contains("biology") ||
            subjectLower.contains("chemistry") || subjectLower.contains("physics")) {
            return this == SCIENCE_LAB || this == STEM_LAB || this == LAB;
        }

        // Computer subjects
        if (subjectLower.contains("computer") || subjectLower.contains("programming") ||
            subjectLower.contains("technology")) {
            return this == COMPUTER_LAB || this == STEM_LAB;
        }

        // PE/Athletics
        if (subjectLower.contains("physical") || subjectLower.contains("pe") ||
            subjectLower.contains("athletics") || subjectLower.contains("fitness")) {
            return this == GYMNASIUM || this == WEIGHT_ROOM;
        }

        // Arts
        if (subjectLower.contains("art")) {
            return this == ART_STUDIO || this == CLASSROOM;
        }

        // Music
        if (subjectLower.contains("music") || subjectLower.contains("band") ||
            subjectLower.contains("chorus") || subjectLower.contains("choir")) {
            return this == MUSIC_ROOM || this == BAND_ROOM || this == CHORUS_ROOM;
        }

        // Drama/Theater
        if (subjectLower.contains("drama") || subjectLower.contains("theater")) {
            return this == THEATER || this == AUDITORIUM;
        }

        // Culinary
        if (subjectLower.contains("culinary") || subjectLower.contains("cooking") ||
            subjectLower.contains("food")) {
            return this == CULINARY_LAB;
        }

        // Shop/Workshop
        if (subjectLower.contains("shop") || subjectLower.contains("industrial") ||
            subjectLower.contains("tech ed")) {
            return this == WORKSHOP;
        }

        // Default: standard classroom subjects
        return this == CLASSROOM || this == LIBRARY;
    }
}

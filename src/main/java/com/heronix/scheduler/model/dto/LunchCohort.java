package com.heronix.scheduler.model.dto;

import com.heronix.scheduler.model.domain.Student;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a group of students who should eat lunch together
 * Based on their scheduled class during the lunch period
 *
 * SIMPLIFIED VERSION for SchedulerV2 - Uses shadow Student entities
 *
 * @author Heronix Scheduling System Team
 * @version 5.0.0
 * @since 2025-12-06
 */
@Data
public class LunchCohort {

    private String name;
    private List<Student> students;
    private String roomNumber;
    private String roomZone;
    private String courseName;
    private String gradeLevel;

    public LunchCohort(String name, List<Student> students) {
        this.name = name;
        this.students = new ArrayList<>(students);
        this.roomNumber = extractRoomNumber(name);
        this.roomZone = determineRoomZone(roomNumber);
        this.courseName = extractCourseName(name);
        this.gradeLevel = extractGradeLevel(name);
    }

    public int getSize() {
        return students != null ? students.size() : 0;
    }

    private String extractRoomNumber(String cohortName) {
        if (cohortName == null || !cohortName.startsWith("Room ")) {
            return null;
        }
        try {
            int dashIndex = cohortName.indexOf(" - ");
            if (dashIndex > 5) {
                return cohortName.substring(5, dashIndex).trim();
            }
        } catch (Exception e) {
            // Invalid format
        }
        return null;
    }

    private String extractCourseName(String cohortName) {
        if (cohortName == null || !cohortName.contains(" - ")) {
            return null;
        }
        try {
            int dashIndex = cohortName.indexOf(" - ");
            return cohortName.substring(dashIndex + 3).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractGradeLevel(String cohortName) {
        if (cohortName == null || !cohortName.startsWith("Grade ")) {
            return null;
        }
        try {
            String gradeStr = cohortName.substring(6).trim();
            int spaceIndex = gradeStr.indexOf(" ");
            if (spaceIndex > 0) {
                gradeStr = gradeStr.substring(0, spaceIndex);
            }
            return gradeStr;
        } catch (Exception e) {
            return null;
        }
    }

    private String determineRoomZone(String roomNum) {
        if (roomNum == null) {
            return null;
        }
        try {
            int num = Integer.parseInt(roomNum);
            if (num >= 100 && num < 150) return "1st Floor East";
            if (num >= 150 && num < 200) return "1st Floor West";
            if (num >= 200 && num < 250) return "2nd Floor East";
            if (num >= 250 && num < 300) return "2nd Floor West";
            if (num >= 300 && num < 350) return "3rd Floor East";
            if (num >= 350 && num < 400) return "3rd Floor West";
            if (num >= 400) return "Upper Floors";
            if (num < 100) return "Ground Floor";
        } catch (NumberFormatException e) {
            // Invalid room number
        }
        return "Other";
    }

    public boolean isRoomBased() {
        return roomNumber != null;
    }

    public boolean isGradeBased() {
        return gradeLevel != null;
    }

    /**
     * Get list of student IDs in this cohort
     */
    public List<Long> getStudentIds() {
        if (students == null || students.isEmpty()) {
            return new ArrayList<>();
        }
        return students.stream()
            .map(Student::getId)
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("%s (%d students%s)",
            name,
            getSize(),
            roomZone != null ? ", " + roomZone : "");
    }
}

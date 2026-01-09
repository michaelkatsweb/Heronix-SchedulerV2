package com.heronix.scheduler.util;

import com.heronix.scheduler.model.domain.*;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validation utility for domain objects
 * Location: src/main/java/com/eduscheduler/util/ValidationUtil.java
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[1-9]\\d{1,14}$|^\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}$");

    private ValidationUtil() {
        // Private constructor
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validate teacher
     */
    public static List<String> validateTeacher(Teacher teacher) {
        List<String> errors = new ArrayList<>();

        if (teacher == null) {
            errors.add("Teacher cannot be null");
            return errors;
        }

        if (!StringUtils.hasText(teacher.getName())) {
            errors.add("Teacher name is required");
        }

        if (teacher.getEmail() != null && !isValidEmail(teacher.getEmail())) {
            errors.add("Invalid email format");
        }

        // TODO: getPhoneNumber() doesn't exist on Teacher entity (from SIS)
        // Phone number validation would need to be done via SIS API

        if (teacher.getMaxHoursPerWeek() <= 0) {
            errors.add("Max hours per week must be positive");
        }

        // TODO: getMaxConsecutiveHours() and getPreferredBreakMinutes() don't exist on Teacher entity (from SIS)
        // These are scheduler-specific preferences that would need to be stored separately

        return errors;
    }

    /**
     * Validate course
     */
    public static List<String> validateCourse(Course course) {
        List<String> errors = new ArrayList<>();

        if (course == null) {
            errors.add("Course cannot be null");
            return errors;
        }

        if (!StringUtils.hasText(course.getCourseCode())) {
            errors.add("Course code is required");
        }

        if (!StringUtils.hasText(course.getCourseName())) {
            errors.add("Course name is required");
        }

        if (course.getDurationMinutes() <= 0) {
            errors.add("Duration must be positive");
        }

        if (course.getSessionsPerWeek() <= 0) {
            errors.add("Sessions per week must be positive");
        }

        if (course.getMaxStudents() <= 0) {
            errors.add("Max students must be positive");
        }

        if (course.getCurrentEnrollment() < 0) {
            errors.add("Current enrollment cannot be negative");
        }

        if (course.getCurrentEnrollment() > course.getMaxStudents()) {
            errors.add("Current enrollment exceeds maximum students");
        }

        return errors;
    }

    /**
     * Validate room
     */
    public static List<String> validateRoom(Room room) {
        List<String> errors = new ArrayList<>();

        if (room == null) {
            errors.add("Room cannot be null");
            return errors;
        }

        if (!StringUtils.hasText(room.getRoomNumber())) {
            errors.add("Room number is required");
        }

        if (room.getCapacity() <= 0) {
            errors.add("Room capacity must be positive");
        }

        return errors;
    }

    /**
     * Validate student
     */
    public static List<String> validateStudent(Student student) {
        List<String> errors = new ArrayList<>();

        if (student == null) {
            errors.add("Student cannot be null");
            return errors;
        }

        if (!StringUtils.hasText(student.getStudentId())) {
            errors.add("Student ID is required");
        }

        if (!StringUtils.hasText(student.getFirstName())) {
            errors.add("First name is required");
        }

        if (!StringUtils.hasText(student.getLastName())) {
            errors.add("Last name is required");
        }

        if (!StringUtils.hasText(student.getGradeLevel())) {
            errors.add("Grade level is required");
        }

        // TODO: getEmail() doesn't exist on Student entity (from SIS)
        // Email validation would need to be done via SIS API

        return errors;
    }

    /**
     * Validate time slot
     */
    public static List<String> validateTimeSlot(TimeSlot timeSlot) {
        List<String> errors = new ArrayList<>();

        if (timeSlot == null) {
            errors.add("Time slot cannot be null");
            return errors;
        }

        if (timeSlot.getDayOfWeek() == null) {
            errors.add("Day of week is required");
        }

        if (timeSlot.getStartTime() == null) {
            errors.add("Start time is required");
        }

        if (timeSlot.getEndTime() == null) {
            errors.add("End time is required");
        }

        if (timeSlot.getStartTime() != null && timeSlot.getEndTime() != null) {
            if (!timeSlot.getStartTime().isBefore(timeSlot.getEndTime())) {
                errors.add("Start time must be before end time");
            }
        }

        return errors;
    }

    /**
     * Check if string is empty or whitespace
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if time is within school hours
     */
    public static boolean isWithinSchoolHours(LocalTime time) {
        LocalTime schoolStart = LocalTime.of(7, 0);
        LocalTime schoolEnd = LocalTime.of(18, 0);
        return !time.isBefore(schoolStart) && !time.isAfter(schoolEnd);
    }
}
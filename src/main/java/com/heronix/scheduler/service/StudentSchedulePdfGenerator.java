package com.heronix.scheduler.service.export;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.CourseType;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Student Schedule PDF Generator
 * Creates professional PDF layouts for student class schedules
 *
 * Inspired by Skyward and PowerSchool schedule printouts
 *
 * Location: src/main/java/com/eduscheduler/service/export/StudentSchedulePdfGenerator.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Component
@Slf4j
public class StudentSchedulePdfGenerator {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    private static final String SCHOOL_NAME = "Heronix Scheduling System School District";
    private static final String SCHOOL_YEAR = "2025-2026";

    // Font definitions
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
    private static final Font ALERT_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

    // Color definitions (matching UI color scheme)
    private static final BaseColor HEADER_BG = new BaseColor(70, 130, 180);  // Steel Blue
    private static final BaseColor ALERT_COLOR = new BaseColor(211, 47, 47);  // Red
    private static final BaseColor IEP_COLOR = new BaseColor(255, 152, 0);    // Orange
    private static final BaseColor BORDER_COLOR = new BaseColor(200, 200, 200);

    // Subject colors (matching ScheduleColorScheme)
    private static final BaseColor MATH_COLOR = new BaseColor(33, 150, 243);      // Blue
    private static final BaseColor SCIENCE_COLOR = new BaseColor(76, 175, 80);    // Green
    private static final BaseColor ENGLISH_COLOR = new BaseColor(255, 193, 7);    // Gold
    private static final BaseColor HISTORY_COLOR = new BaseColor(244, 67, 54);    // Red
    private static final BaseColor LANGUAGE_COLOR = new BaseColor(156, 39, 176);  // Purple
    private static final BaseColor ARTS_COLOR = new BaseColor(255, 152, 0);       // Orange
    private static final BaseColor PE_COLOR = new BaseColor(0, 188, 212);         // Teal
    private static final BaseColor CS_COLOR = new BaseColor(63, 81, 181);         // Indigo

    // ========================================================================
    // MAIN GENERATION METHOD
    // ========================================================================

    /**
     * Generate student schedule content in PDF document
     *
     * @param document The PDF document
     * @param student The student
     * @param scheduleSlots List of schedule slots
     * @throws DocumentException if generation fails
     */
    public void generateContent(Document document, Student student, List<ScheduleSlot> scheduleSlots)
            throws DocumentException {

        log.info("Generating PDF content for student: {}", student.getStudentId());

        // Header
        addHeader(document);

        // Student Information Card
        addStudentInfoCard(document, student);

        // Medical Alerts (if any)
        if (student.getMedicalConditions() != null && !student.getMedicalConditions().trim().isEmpty()) {
            addMedicalAlert(document, student.getMedicalConditions());
        }

        // IEP/504 Indicators
        if (Boolean.TRUE.equals(student.getHasIEP()) || Boolean.TRUE.equals(student.getHas504Plan())) {
            addAccommodationInfo(document, student);
        }

        // Schedule Table
        addScheduleTable(document, scheduleSlots);

        // Summary Section
        addSummary(document, student, scheduleSlots);

        // Footer
        addFooter(document);
    }

    // ========================================================================
    // DOCUMENT SECTIONS
    // ========================================================================

    /**
     * Add document header
     */
    private void addHeader(Document document) throws DocumentException {
        // School name
        Paragraph schoolName = new Paragraph(SCHOOL_NAME, TITLE_FONT);
        schoolName.setAlignment(Element.ALIGN_CENTER);
        document.add(schoolName);

        // Document title
        Paragraph title = new Paragraph("Student Class Schedule", SUBTITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        // School year
        Paragraph year = new Paragraph("School Year: " + SCHOOL_YEAR, NORMAL_FONT);
        year.setAlignment(Element.ALIGN_CENTER);
        year.setSpacingAfter(15);
        document.add(year);

        // Divider line
        com.itextpdf.text.pdf.draw.LineSeparator line = new com.itextpdf.text.pdf.draw.LineSeparator();
        line.setLineColor(BORDER_COLOR);
        document.add(new Chunk(line));
        document.add(new Paragraph(" ")); // Spacer
    }

    /**
     * Add student information card
     */
    private void addStudentInfoCard(Document document, Student student) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1f, 1f});
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(10);

        // Left column
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.BOX);
        leftCell.setPadding(10);
        leftCell.addElement(new Paragraph("Student Name: " + student.getFirstName() + " " + student.getLastName(), HEADER_FONT));
        leftCell.addElement(new Paragraph("Student ID: " + student.getStudentId(), NORMAL_FONT));

        // Right column
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.BOX);
        rightCell.setPadding(10);
        rightCell.addElement(new Paragraph("Grade: " + student.getGradeLevel(), HEADER_FONT));

        if (student.getEmergencyContact() != null) {
            rightCell.addElement(new Paragraph("Emergency Contact: " + student.getEmergencyContact(), SMALL_FONT));
        }
        if (student.getEmergencyPhone() != null) {
            rightCell.addElement(new Paragraph("Emergency Phone: " + student.getEmergencyPhone(), SMALL_FONT));
        }

        infoTable.addCell(leftCell);
        infoTable.addCell(rightCell);

        document.add(infoTable);
    }

    /**
     * Add medical alert banner
     */
    private void addMedicalAlert(Document document, String medicalConditions) throws DocumentException {
        PdfPTable alertTable = new PdfPTable(1);
        alertTable.setWidthPercentage(100);
        alertTable.setSpacingAfter(10);

        PdfPCell alertCell = new PdfPCell();
        alertCell.setBackgroundColor(new BaseColor(255, 235, 238)); // Light red
        alertCell.setBorderColor(ALERT_COLOR);
        alertCell.setBorderWidth(2);
        alertCell.setPadding(8);

        Paragraph alertTitle = new Paragraph("⚠ MEDICAL ALERT", ALERT_FONT);
        alertTitle.getFont().setColor(ALERT_COLOR);
        alertCell.addElement(alertTitle);

        Paragraph alertText = new Paragraph(medicalConditions, NORMAL_FONT);
        alertText.setSpacingBefore(5);
        alertCell.addElement(alertText);

        alertTable.addCell(alertCell);
        document.add(alertTable);
    }

    /**
     * Add accommodation information
     */
    private void addAccommodationInfo(Document document, Student student) throws DocumentException {
        PdfPTable accomTable = new PdfPTable(1);
        accomTable.setWidthPercentage(100);
        accomTable.setSpacingAfter(10);

        PdfPCell accomCell = new PdfPCell();
        accomCell.setBackgroundColor(new BaseColor(255, 243, 224)); // Light orange
        accomCell.setBorderColor(IEP_COLOR);
        accomCell.setBorderWidth(1);
        accomCell.setPadding(8);

        StringBuilder accomText = new StringBuilder("📋 ");
        if (Boolean.TRUE.equals(student.getHasIEP())) {
            accomText.append("IEP ");
        }
        if (Boolean.TRUE.equals(student.getHas504Plan())) {
            accomText.append("504 Plan ");
        }
        accomText.append("- Accommodations Required");

        Paragraph accomPara = new Paragraph(accomText.toString(), HEADER_FONT);
        accomPara.getFont().setColor(IEP_COLOR);
        accomCell.addElement(accomPara);

        if (student.getAccommodationNotes() != null && !student.getAccommodationNotes().trim().isEmpty()) {
            Paragraph notes = new Paragraph(student.getAccommodationNotes(), NORMAL_FONT);
            notes.setSpacingBefore(5);
            accomCell.addElement(notes);
        }

        accomTable.addCell(accomCell);
        document.add(accomTable);
    }

    /**
     * Add schedule table with color coding
     */
    private void addScheduleTable(Document document, List<ScheduleSlot> scheduleSlots) throws DocumentException {
        document.add(new Paragraph("Class Schedule", SUBTITLE_FONT));

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1.5f, 2.5f, 2f, 1.5f, 1f, 2f});
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        // Headers
        addScheduleTableHeader(table, "Period");
        addScheduleTableHeader(table, "Time");
        addScheduleTableHeader(table, "Course");
        addScheduleTableHeader(table, "Teacher");
        addScheduleTableHeader(table, "Room");
        addScheduleTableHeader(table, "Ext.");
        addScheduleTableHeader(table, "Notes");

        // Rows
        for (ScheduleSlot slot : scheduleSlots) {
            addScheduleRow(table, slot);
        }

        document.add(table);
    }

    /**
     * Add schedule table header
     */
    private void addScheduleTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Font whiteFont = new Font(HEADER_FONT);
        whiteFont.setColor(BaseColor.WHITE);
        cell.setPhrase(new Phrase(text, whiteFont));
        table.addCell(cell);
    }

    /**
     * Add schedule row with color coding
     */
    private void addScheduleRow(PdfPTable table, ScheduleSlot slot) {
        Course course = slot.getCourse();
        BaseColor rowColor = getSubjectColor(course != null ? course.getSubject() : null);

        // Period
        addColoredCell(table, getPeriodDisplay(slot), rowColor);

        // Time
        addColoredCell(table, getTimeDisplay(slot), rowColor);

        // Course (with type)
        addColoredCell(table, getCourseDisplay(course), rowColor);

        // Teacher
        addColoredCell(table, slot.getTeacher() != null ? slot.getTeacher().getName() : "", rowColor);

        // Room
        addColoredCell(table, slot.getRoom() != null ? slot.getRoom().getRoomNumber() : "", rowColor);

        // Extension
        addColoredCell(table, getExtensionDisplay(slot.getRoom()), rowColor);

        // Notes
        addColoredCell(table, getNotesDisplay(slot), rowColor);
    }

    /**
     * Add colored table cell
     */
    private void addColoredCell(PdfPTable table, String text, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        if (bgColor != null) {
            cell.setBackgroundColor(lightenColor(bgColor));
        }
        cell.setPadding(5);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    /**
     * Add summary section
     */
    private void addSummary(Document document, Student student, List<ScheduleSlot> scheduleSlots)
            throws DocumentException {

        document.add(new Paragraph("Schedule Summary", SUBTITLE_FONT));

        PdfPTable summaryTable = new PdfPTable(3);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10);

        // Total classes
        addSummaryItem(summaryTable, "Total Classes:", String.valueOf(scheduleSlots.size()));

        // Total credits (example calculation)
        double totalCredits = scheduleSlots.size() * 0.5;
        addSummaryItem(summaryTable, "Total Credits:", String.format("%.1f", totalCredits));

        // Print date
        addSummaryItem(summaryTable, "Print Date:",
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy")));

        document.add(summaryTable);
    }

    /**
     * Add summary item
     */
    private void addSummaryItem(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);

        table.addCell(new PdfPCell()); // Empty cell for spacing
    }

    /**
     * Add footer
     */
    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" ")); // Spacer

        com.itextpdf.text.pdf.draw.LineSeparator line = new com.itextpdf.text.pdf.draw.LineSeparator();
        line.setLineColor(BORDER_COLOR);
        document.add(new Chunk(line));

        Paragraph footer = new Paragraph(
            "Generated by Heronix Scheduling System | " +
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            SMALL_FONT
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(5);
        document.add(footer);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get period display string
     */
    private String getPeriodDisplay(ScheduleSlot slot) {
        if (slot.getPeriodNumber() != null) {
            return String.valueOf(slot.getPeriodNumber());
        }
        if (slot.getDayOfWeek() != null) {
            return slot.getDayOfWeek().toString().substring(0, 3);
        }
        return "";
    }

    /**
     * Get time display string
     */
    private String getTimeDisplay(ScheduleSlot slot) {
        if (slot.getStartTime() != null && slot.getEndTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
            return slot.getStartTime().format(formatter) + "\n" +
                   slot.getEndTime().format(formatter);
        }
        return "";
    }

    /**
     * Get course display string with type
     */
    private String getCourseDisplay(Course course) {
        if (course == null) return "";

        String display = course.getCourseName();

        if (course.getCourseType() != null && !CourseType.REGULAR.name().equals(course.getCourseType())) {
            // Try to get display name from enum, fallback to raw value
            try {
                CourseType type = CourseType.valueOf(course.getCourseType());
                display += "\n(" + type.getDisplayName() + ")";
            } catch (IllegalArgumentException e) {
                display += "\n(" + course.getCourseType() + ")";
            }
        }

        return display;
    }

    /**
     * Get extension display string
     */
    private String getExtensionDisplay(Room room) {
        if (room != null && room.getTelephoneExtension() != null) {
            return room.getTelephoneExtension();
        }
        return "";
    }

    /**
     * Get notes display string
     */
    private String getNotesDisplay(ScheduleSlot slot) {
        StringBuilder notes = new StringBuilder();

        if (Boolean.TRUE.equals(slot.getIsSpecialEvent())) {
            notes.append("⭐ ");
            if (slot.getSpecialEventType() != null) {
                notes.append(slot.getSpecialEventType().getDisplayName());
            }
        }

        if (slot.getNotes() != null && !slot.getNotes().trim().isEmpty()) {
            if (notes.length() > 0) notes.append("\n");
            notes.append(slot.getNotes());
        }

        return notes.toString();
    }

    /**
     * Get subject color matching UI color scheme
     */
    private BaseColor getSubjectColor(String subject) {
        if (subject == null) return null;

        String subjectLower = subject.toLowerCase();

        if (subjectLower.contains("math") || subjectLower.contains("algebra") ||
            subjectLower.contains("geometry") || subjectLower.contains("calculus")) {
            return MATH_COLOR;
        }
        if (subjectLower.contains("science") || subjectLower.contains("biology") ||
            subjectLower.contains("chemistry") || subjectLower.contains("physics")) {
            return SCIENCE_COLOR;
        }
        if (subjectLower.contains("english") || subjectLower.contains("language arts") ||
            subjectLower.contains("literature")) {
            return ENGLISH_COLOR;
        }
        if (subjectLower.contains("history") || subjectLower.contains("social studies") ||
            subjectLower.contains("government")) {
            return HISTORY_COLOR;
        }
        if (subjectLower.contains("spanish") || subjectLower.contains("french") ||
            subjectLower.contains("foreign language")) {
            return LANGUAGE_COLOR;
        }
        if (subjectLower.contains("art") || subjectLower.contains("music") ||
            subjectLower.contains("band")) {
            return ARTS_COLOR;
        }
        if (subjectLower.contains("physical education") || subjectLower.contains("pe") ||
            subjectLower.contains("health")) {
            return PE_COLOR;
        }
        if (subjectLower.contains("computer") || subjectLower.contains("technology")) {
            return CS_COLOR;
        }

        return null; // No color for unknown subjects
    }

    /**
     * Lighten color for table cell background
     */
    private BaseColor lightenColor(BaseColor color) {
        if (color == null) return null;

        float factor = 0.7f; // 70% lighter
        int r = Math.min(255, color.getRed() + (int)((255 - color.getRed()) * factor));
        int g = Math.min(255, color.getGreen() + (int)((255 - color.getGreen()) * factor));
        int b = Math.min(255, color.getBlue() + (int)((255 - color.getBlue()) * factor));

        return new BaseColor(r, g, b);
    }
}

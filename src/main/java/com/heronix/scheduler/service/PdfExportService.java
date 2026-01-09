package com.heronix.scheduler.service.export;

import com.heronix.scheduler.model.domain.ScheduleSlot;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.domain.Teacher;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF Export Service
 * Handles PDF generation for schedules, rosters, and reports
 *
 * Location: src/main/java/com/eduscheduler/service/export/PdfExportService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Service
@Slf4j
public class PdfExportService {

    @Autowired
    private StudentSchedulePdfGenerator studentSchedulePdfGenerator;

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    private static final String SCHOOL_NAME = "Heronix Scheduling System";
    private static final String SCHOOL_ADDRESS = "School District";

    // Font definitions
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

    // ========================================================================
    // STUDENT SCHEDULE PDF
    // ========================================================================

    /**
     * Generate student schedule PDF
     *
     * @param student The student
     * @param scheduleSlots List of schedule slots for the student
     * @return ByteArrayOutputStream containing PDF data
     * @throws DocumentException if PDF generation fails
     */
    public ByteArrayOutputStream generateStudentSchedulePdf(
            Student student,
            List<ScheduleSlot> scheduleSlots) throws DocumentException {

        log.info("Generating PDF for student: {} {}", student.getFirstName(), student.getLastName());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.LETTER);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Use the student schedule generator
            studentSchedulePdfGenerator.generateContent(document, student, scheduleSlots);

            log.info("PDF generated successfully for student: {}", student.getStudentId());

        } catch (Exception e) {
            log.error("Error generating student schedule PDF", e);
            throw new DocumentException("Failed to generate PDF: " + e.getMessage());
        } finally {
            document.close();
        }

        return outputStream;
    }

    // ========================================================================
    // TEACHER SCHEDULE PDF
    // ========================================================================

    /**
     * Generate teacher schedule PDF
     *
     * @param teacher The teacher
     * @param scheduleSlots List of schedule slots for the teacher
     * @return ByteArrayOutputStream containing PDF data
     * @throws DocumentException if PDF generation fails
     */
    public ByteArrayOutputStream generateTeacherSchedulePdf(
            Teacher teacher,
            List<ScheduleSlot> scheduleSlots) throws DocumentException {

        log.info("Generating PDF for teacher: {}", teacher.getName());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add title
            Paragraph title = new Paragraph("Teacher Schedule", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Teacher info
            document.add(new Paragraph("Teacher: " + teacher.getName(), HEADER_FONT));
            document.add(new Paragraph("Department: " + (teacher.getDepartment() != null ? teacher.getDepartment() : "N/A"), NORMAL_FONT));

            if (teacher.getSpecialAssignments() != null && !teacher.getSpecialAssignments().isEmpty()) {
                document.add(new Paragraph("Special Assignments: " +
                    String.join(", ", teacher.getSpecialAssignments()), NORMAL_FONT));
            }

            document.add(new Paragraph(" ")); // Spacer

            // Schedule table
            PdfPTable table = createTeacherScheduleTable(scheduleSlots);
            document.add(table);

            // Footer
            addFooter(document);

            log.info("Teacher PDF generated successfully");

        } catch (Exception e) {
            log.error("Error generating teacher schedule PDF", e);
            throw new DocumentException("Failed to generate teacher PDF: " + e.getMessage());
        } finally {
            document.close();
        }

        return outputStream;
    }

    // ========================================================================
    // FILE OPERATIONS
    // ========================================================================

    /**
     * Export PDF to file
     *
     * @param pdfStream The PDF stream
     * @param outputFile The output file
     * @throws IOException if file write fails
     */
    public void exportToFile(ByteArrayOutputStream pdfStream, File outputFile) throws IOException {
        log.info("Exporting PDF to file: {}", outputFile.getAbsolutePath());

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            pdfStream.writeTo(fos);
            log.info("PDF exported successfully to: {}", outputFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to export PDF to file", e);
            throw e;
        }
    }

    /**
     * Print PDF using default printer
     *
     * @param pdfStream The PDF stream
     * @throws Exception if printing fails
     */
    public void printPdf(ByteArrayOutputStream pdfStream) throws Exception {
        log.info("Printing PDF document");

        // Note: This is a simplified implementation
        // In production, you might want to use Java's PrintService API
        // or integrate with platform-specific printing utilities

        try {
            // Write to temp file
            File tempFile = File.createTempFile("schedule_", ".pdf");
            tempFile.deleteOnExit();

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                pdfStream.writeTo(fos);
            }

            // Open with default PDF viewer for printing
            // This will vary by platform
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(tempFile);
                log.info("PDF opened for printing");
            } else {
                throw new Exception("Desktop printing not supported on this platform");
            }

        } catch (Exception e) {
            log.error("Failed to print PDF", e);
            throw e;
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Create teacher schedule table
     */
    private PdfPTable createTeacherScheduleTable(List<ScheduleSlot> scheduleSlots) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 2f, 3f, 2f, 1.5f, 2f});
        table.setSpacingBefore(10);

        // Headers
        addTableHeader(table, "Period");
        addTableHeader(table, "Time");
        addTableHeader(table, "Course");
        addTableHeader(table, "Room");
        addTableHeader(table, "Students");
        addTableHeader(table, "Notes");

        // Rows
        for (ScheduleSlot slot : scheduleSlots) {
            addTableCell(table, getPeriodDisplay(slot));
            addTableCell(table, getTimeDisplay(slot));
            addTableCell(table, slot.getCourse() != null ? slot.getCourse().getCourseName() : "");
            addTableCell(table, slot.getRoom() != null ? slot.getRoom().getRoomNumber() : "");
            addTableCell(table, String.valueOf(slot.getStudents() != null ? slot.getStudents().size() : 0));
            addTableCell(table, slot.getNotes() != null ? slot.getNotes() : "");
        }

        return table;
    }

    /**
     * Add table header cell
     */
    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new BaseColor(200, 200, 200));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    /**
     * Add table cell
     */
    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(5);
        table.addCell(cell);
    }

    /**
     * Get period display string
     */
    private String getPeriodDisplay(ScheduleSlot slot) {
        if (slot.getPeriodNumber() != null) {
            return "Period " + slot.getPeriodNumber();
        }
        if (slot.getDayOfWeek() != null) {
            return slot.getDayOfWeek().toString();
        }
        return "";
    }

    /**
     * Get time display string
     */
    private String getTimeDisplay(ScheduleSlot slot) {
        if (slot.getStartTime() != null && slot.getEndTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
            return slot.getStartTime().format(formatter) + " - " +
                   slot.getEndTime().format(formatter);
        }
        return "";
    }

    /**
     * Add footer to document
     */
    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
            "Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) +
            " | " + SCHOOL_NAME,
            SMALL_FONT
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
    }
}

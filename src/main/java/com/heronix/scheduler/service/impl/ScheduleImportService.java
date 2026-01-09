package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for importing schedules from PDF files using OCR
 *
 * Features:
 * - PDF text extraction (native PDF text)
 * - OCR for scanned/image-based PDFs
 * - Pattern recognition for schedule data
 * - Automatic entity matching/creation
 *
 * TODO: EXTERNAL SIS FILE IMPORT SUPPORT
 * ======================================
 *
 * Currently supports PDF imports only. Expand to support file-based imports from
 * external SIS systems (Skyward, PowerSchool, Infinite Campus, etc.):
 *
 * 1. CSV File Import Service:
 *    ```java
 *    @Service
 *    public class CSVScheduleImportService {
 *        public ScheduleImportResult importFromCSV(File csvFile, CSVFieldMapping mapping) {
 *            // Parse CSV using Apache Commons CSV or OpenCSV
 *            // Map columns using field mapping configuration
 *            // Validate data types and required fields
 *            // Create schedule slots from parsed data
 *        }
 *
 *        // Example CSV formats to support:
 *        // Skyward: StudentID,StudentName,CourseCode,CourseName,Teacher,Room,Period,Semester
 *        // PowerSchool: DCID,Student_Number,Course_Number,Course_Name,TeacherName,Room,Expression
 *        // Infinite Campus: PersonID,LastName,FirstName,CourseNumber,CourseName,StaffName,RoomNumber,Period
 *    }
 *    ```
 *
 * 2. Excel (XLSX) File Import Service:
 *    ```java
 *    @Service
 *    public class ExcelScheduleImportService {
 *        public ScheduleImportResult importFromExcel(File excelFile, ExcelFieldMapping mapping) {
 *            // Use Apache POI to read Excel workbook
 *            // Support multiple sheets (students, courses, teachers, schedules)
 *            // Handle merged cells and complex formatting
 *            // Validate data across multiple sheets (referential integrity)
 *        }
 *
 *        // Example sheet structures:
 *        // Sheet 1 "Students": ID, LastName, FirstName, Grade, Counselor
 *        // Sheet 2 "Courses": CourseID, CourseName, Credits, Department
 *        // Sheet 3 "Teachers": TeacherID, LastName, FirstName, Department
 *        // Sheet 4 "Schedule": StudentID, CourseID, TeacherID, Room, Period, Semester
 *    }
 *    ```
 *
 * 3. JSON File Import Service:
 *    ```java
 *    @Service
 *    public class JSONScheduleImportService {
 *        public ScheduleImportResult importFromJSON(File jsonFile) {
 *            // Parse JSON using Jackson or Gson
 *            // Support standardized Heronix JSON schema
 *            // Support custom schema with field mapping
 *            // Validate against JSON schema
 *        }
 *
 *        // Example Heronix JSON schema:
 *        // {
 *        //   "students": [{ "id": 12345, "firstName": "John", "lastName": "Doe", ... }],
 *        //   "teachers": [{ "id": 100, "firstName": "Jane", "lastName": "Smith", ... }],
 *        //   "courses": [{ "id": 200, "name": "Algebra 1", "credits": 1.0, ... }],
 *        //   "schedules": [{ "studentId": 12345, "courseId": 200, "teacherId": 100, "room": "A101", "period": 1 }]
 *        // }
 *    }
 *    ```
 *
 * 4. XML File Import Service:
 *    ```java
 *    @Service
 *    public class XMLScheduleImportService {
 *        public ScheduleImportResult importFromXML(File xmlFile, String schemaType) {
 *            // Parse XML using JAXB or DOM parser
 *            // Support SIF (Schools Interoperability Framework) XML format
 *            // Support Infinite Campus XML export format
 *            // Validate against XSD schema
 *        }
 *
 *        // SIF XML example:
 *        // <SIF_Message>
 *        //   <StudentPersonal RefId="...">
 *        //     <PersonInfo><Name><LastName>Doe</LastName>...</Name></PersonInfo>
 *        //   </StudentPersonal>
 *        //   <ScheduleInfo RefId="...">
 *        //     <StudentPersonalRefId>...</StudentPersonalRefId>
 *        //     <CourseCode>ALG1</CourseCode>
 *        //   </ScheduleInfo>
 *        // </SIF_Message>
 *    }
 *    ```
 *
 * 5. Field Mapping Configuration:
 *    ```java
 *    @Entity
 *    public class ImportFieldMapping {
 *        private Long id;
 *        private String sisType; // SKYWARD, POWERSCHOOL, INFINITE_CAMPUS, CUSTOM
 *        private String fileFormat; // CSV, EXCEL, JSON, XML
 *        private Map<String, String> fieldMappings; // External field -> Heronix field
 *        private Map<String, String> transformations; // Field -> transformation rule
 *        private Map<String, String> defaultValues; // Field -> default value
 *    }
 *
 *    // Example field mapping:
 *    // {
 *    //   "StudentNumber": "studentId",
 *    //   "StudentName": "SPLIT_NAME", // transformation: split into firstName/lastName
 *    //   "GradeLevel": "gradeLevel",
 *    //   "Course_Number": "courseCode",
 *    //   "Teacher": "PARSE_TEACHER_NAME" // transformation: parse "Last, First" format
 *    // }
 *    ```
 *
 * 6. Data Validation Pipeline:
 *    ```java
 *    @Service
 *    public class ImportDataValidator {
 *        public ValidationResult validate(List<ImportedEntity> entities, ImportRules rules) {
 *            // Validate required fields present
 *            // Validate data types (numbers, dates, booleans)
 *            // Validate referential integrity (student exists, course exists, etc.)
 *            // Validate business rules (no duplicate enrollments, valid grade levels, etc.)
 *            // Collect validation errors with line numbers
 *        }
 *
 *        // Example validation rules:
 *        // - Student ID must be numeric
 *        // - Grade level must be K-12
 *        // - Course must exist in catalog
 *        // - Teacher must be certified for course subject
 *        // - Room capacity not exceeded
 *    }
 *    ```
 *
 * 7. Import Preview UI:
 *    - Show preview of first 100 rows before import
 *    - Highlight validation errors in red
 *    - Allow user to fix mappings or data before final import
 *    - Show statistics: total rows, valid rows, errors, warnings
 *
 * 8. Batch Import Processing:
 *    - Support large file imports (10,000+ students)
 *    - Process in batches to avoid memory issues
 *    - Show progress bar with estimated time remaining
 *    - Support resume after failure (checkpoint processing)
 *
 * 9. Import History Tracking:
 *    ```java
 *    @Entity
 *    public class ImportHistory {
 *        private Long id;
 *        private LocalDateTime importDate;
 *        private String fileName;
 *        private String sisType;
 *        private String fileFormat;
 *        private Integer totalRecords;
 *        private Integer successfulRecords;
 *        private Integer failedRecords;
 *        private String importedBy; // username
 *        private String errorLog; // JSON array of errors
 *    }
 *    ```
 *
 * 10. Template Generation:
 *     - Provide downloadable CSV/Excel templates for each SIS type
 *     - Include sample data in templates
 *     - Include field descriptions and requirements
 *     - Generate templates from current Heronix schema
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 14 - PDF/OCR Import
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleImportService {

    private final SISDataService sisDataService;
    private final RoomRepository roomRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;

    // Common time patterns
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(\\d{1,2}:\\d{2})\\s*(?:AM|PM|am|pm)?\\s*[-–]\\s*(\\d{1,2}:\\d{2})\\s*(?:AM|PM|am|pm)?");

    // Day patterns
    private static final Pattern DAY_PATTERN = Pattern.compile(
        "(?i)(Monday|Tuesday|Wednesday|Thursday|Friday|Mon|Tue|Wed|Thu|Fri|M|T|W|R|F)");

    // Room patterns (e.g., "Room 101", "Rm 205", "A-101")
    private static final Pattern ROOM_PATTERN = Pattern.compile(
        "(?i)(?:Room|Rm\\.?|#)?\\s*([A-Z]?-?\\d{2,4}[A-Z]?)");

    // Period patterns (e.g., "Period 1", "P1", "1st Period")
    private static final Pattern PERIOD_PATTERN = Pattern.compile(
        "(?i)(?:Period|P|Per\\.?)\\s*(\\d+)|(?:(\\d+)(?:st|nd|rd|th)?\\s*Period)");

    /**
     * Import schedule from PDF file
     */
    @Transactional
    public ScheduleImportResult importFromPdf(File pdfFile) throws IOException {
        // ✅ NULL SAFE: Validate pdfFile parameter
        if (pdfFile == null) {
            throw new IllegalArgumentException("PDF file cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of file name
        String fileName = (pdfFile.getName() != null) ? pdfFile.getName() : "Unknown";
        log.info("Starting PDF import from: {}", fileName);

        ScheduleImportResult result = new ScheduleImportResult();
        result.setFileName(fileName);

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            // First try native text extraction
            String text = extractTextFromPdf(document);

            // ✅ NULL SAFE: Validate extracted text
            if (text == null) {
                text = "";
            }

            if (text.trim().length() < 100) {
                // If little text found, try OCR
                log.info("PDF appears to be image-based, attempting OCR...");
                String ocrText = extractTextWithOcr(document);
                text = (ocrText != null) ? ocrText : text;
                result.setOcrUsed(true);
            }

            result.setRawText(text);

            // Parse the extracted text
            List<ParsedScheduleEntry> entries = parseScheduleText(text);
            // ✅ NULL SAFE: Validate entries list
            if (entries == null) {
                entries = new ArrayList<>();
            }
            result.setParsedEntries(entries.size());

            // Process and create schedule slots
            List<ScheduleSlotImport> imports = processEntries(entries);
            // ✅ NULL SAFE: Validate imports list
            if (imports == null) {
                imports = new ArrayList<>();
            }
            result.setImportedSlots(imports);
            // ✅ NULL SAFE: Filter null imports before counting
            result.setSuccessCount((int) imports.stream()
                .filter(i -> i != null)
                .filter(ScheduleSlotImport::isSuccess).count());
            result.setFailureCount((int) imports.stream()
                .filter(i -> i != null)
                .filter(i -> !i.isSuccess()).count());

            log.info("PDF import complete: {} entries parsed, {} successful, {} failed",
                entries.size(), result.getSuccessCount(), result.getFailureCount());

        } catch (Exception e) {
            log.error("Error importing PDF: {}", e.getMessage(), e);
            result.setError(e.getMessage());
        }

        return result;
    }

    /**
     * Import schedule from InputStream
     */
    @Transactional
    public ScheduleImportResult importFromPdf(InputStream inputStream, String fileName) throws IOException {
        // ✅ NULL SAFE: Validate inputStream parameter
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        File tempFile = File.createTempFile("schedule_import_", ".pdf");
        try {
            java.nio.file.Files.copy(inputStream, tempFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            ScheduleImportResult result = importFromPdf(tempFile);
            // ✅ NULL SAFE: Safe extraction of fileName
            result.setFileName((fileName != null) ? fileName : "Uploaded PDF");
            return result;
        } finally {
            tempFile.delete();
        }
    }

    /**
     * Extract text from PDF using PDFBox
     */
    private String extractTextFromPdf(PDDocument document) throws IOException {
        // ✅ NULL SAFE: Validate document parameter
        if (document == null) {
            log.warn("PDF document is null, cannot extract text");
            return "";
        }

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        String text = stripper.getText(document);
        // ✅ NULL SAFE: Ensure non-null return value
        return (text != null) ? text : "";
    }

    /**
     * Extract text from PDF using OCR (Tesseract)
     */
    private String extractTextWithOcr(PDDocument document) throws IOException {
        // ✅ NULL SAFE: Validate document parameter
        if (document == null) {
            log.warn("PDF document is null, cannot perform OCR");
            return "";
        }

        StringBuilder text = new StringBuilder();
        PDFRenderer renderer = new PDFRenderer(document);

        Tesseract tesseract = new Tesseract();
        // Set tessdata path - can be configured via properties
        // ✅ NULL SAFE: Check tessDataPath before using
        String tessDataPath = System.getenv("TESSDATA_PREFIX");
        if (tessDataPath != null) {
            tesseract.setDatapath(tessDataPath);
        }
        tesseract.setLanguage("eng");

        for (int page = 0; page < document.getNumberOfPages(); page++) {
            try {
                BufferedImage image = renderer.renderImageWithDPI(page, 300);
                // ✅ NULL SAFE: Check image is not null
                if (image == null) {
                    log.warn("Failed to render page {}", page);
                    continue;
                }
                String pageText = tesseract.doOCR(image);
                // ✅ NULL SAFE: Check pageText before appending
                if (pageText != null) {
                    text.append(pageText).append("\n");
                }
            } catch (TesseractException e) {
                log.warn("OCR failed for page {}: {}", page, e.getMessage());
            }
        }

        return text.toString();
    }

    /**
     * Parse schedule text into structured entries
     */
    private List<ParsedScheduleEntry> parseScheduleText(String text) {
        List<ParsedScheduleEntry> entries = new ArrayList<>();

        // ✅ NULL SAFE: Validate text parameter
        if (text == null || text.isEmpty()) {
            log.warn("Cannot parse null or empty text");
            return entries;
        }

        // Split by lines
        String[] lines = text.split("\\r?\\n");
        // ✅ NULL SAFE: Validate lines array
        if (lines == null) {
            return entries;
        }

        ParsedScheduleEntry currentEntry = null;
        DayOfWeek currentDay = null;

        for (String line : lines) {
            // ✅ NULL SAFE: Skip null lines
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;

            // Check for day header
            Matcher dayMatcher = DAY_PATTERN.matcher(line);
            if (dayMatcher.find()) {
                currentDay = parseDayOfWeek(dayMatcher.group(1));
            }

            // Check for time range
            Matcher timeMatcher = TIME_PATTERN.matcher(line);
            if (timeMatcher.find()) {
                if (currentEntry != null) {
                    entries.add(currentEntry);
                }

                currentEntry = new ParsedScheduleEntry();
                currentEntry.setDayOfWeek(currentDay);
                currentEntry.setStartTime(parseTime(timeMatcher.group(1)));
                currentEntry.setEndTime(parseTime(timeMatcher.group(2)));
                currentEntry.setRawLine(line);

                // Try to extract room
                Matcher roomMatcher = ROOM_PATTERN.matcher(line);
                if (roomMatcher.find()) {
                    currentEntry.setRoomNumber(roomMatcher.group(1));
                }

                // Try to extract period
                Matcher periodMatcher = PERIOD_PATTERN.matcher(line);
                if (periodMatcher.find()) {
                    String period = periodMatcher.group(1) != null ?
                        periodMatcher.group(1) : periodMatcher.group(2);
                    currentEntry.setPeriodNumber(Integer.parseInt(period));
                }

                // Remaining text might be course/teacher name
                String remaining = line
                    .replaceAll(TIME_PATTERN.pattern(), "")
                    .replaceAll(ROOM_PATTERN.pattern(), "")
                    .replaceAll(PERIOD_PATTERN.pattern(), "")
                    .trim();

                if (!remaining.isEmpty()) {
                    // Try to identify if it's a course or teacher
                    if (remaining.contains(" - ")) {
                        String[] parts = remaining.split(" - ", 2);
                        currentEntry.setCourseName(parts[0].trim());
                        currentEntry.setTeacherName(parts[1].trim());
                    } else {
                        currentEntry.setCourseName(remaining);
                    }
                }
            } else if (currentEntry != null && !line.isEmpty()) {
                // Additional info for current entry
                if (currentEntry.getTeacherName() == null &&
                    (line.contains("Teacher") || line.matches(".*[A-Z][a-z]+,?\\s+[A-Z][a-z]+.*"))) {
                    currentEntry.setTeacherName(line.replaceAll("(?i)teacher:?\\s*", "").trim());
                } else if (currentEntry.getCourseName() == null) {
                    currentEntry.setCourseName(line);
                }
            }
        }

        if (currentEntry != null) {
            entries.add(currentEntry);
        }

        return entries;
    }

    /**
     * Process parsed entries and create schedule slots
     */
    private List<ScheduleSlotImport> processEntries(List<ParsedScheduleEntry> entries) {
        List<ScheduleSlotImport> imports = new ArrayList<>();

        // ✅ NULL SAFE: Validate entries list
        if (entries == null) {
            return imports;
        }

        for (ParsedScheduleEntry entry : entries) {
            // ✅ NULL SAFE: Skip null entries
            if (entry == null) continue;

            ScheduleSlotImport slotImport = new ScheduleSlotImport();
            slotImport.setEntry(entry);

            try {
                // Find or create course
                Course course = findOrSuggestCourse(entry.getCourseName());
                if (course == null) {
                    slotImport.setSuccess(false);
                    // ✅ NULL SAFE: Safe extraction of course name
                    String courseName = (entry.getCourseName() != null) ? entry.getCourseName() : "Unknown";
                    slotImport.setError("Course not found: " + courseName);
                    slotImport.setSuggestedAction("Create course '" + courseName + "'");
                    imports.add(slotImport);
                    continue;
                }
                slotImport.setMatchedCourse(course);

                // Find teacher
                Teacher teacher = findTeacher(entry.getTeacherName());
                slotImport.setMatchedTeacher(teacher);

                // Find room
                Room room = findRoom(entry.getRoomNumber());
                slotImport.setMatchedRoom(room);

                // Validate schedule slot data
                if (entry.getDayOfWeek() == null || entry.getStartTime() == null) {
                    slotImport.setSuccess(false);
                    slotImport.setError("Missing day or time information");
                    imports.add(slotImport);
                    continue;
                }

                slotImport.setSuccess(true);
                slotImport.setReadyToImport(true);

            } catch (Exception e) {
                slotImport.setSuccess(false);
                // ✅ NULL SAFE: Safe extraction of error message
                slotImport.setError((e.getMessage() != null) ? e.getMessage() : "Unknown error");
            }

            imports.add(slotImport);
        }

        return imports;
    }

    /**
     * Find matching course by name (fuzzy matching)
     */
    private Course findOrSuggestCourse(String courseName) {
        if (courseName == null || courseName.isEmpty()) return null;

        String normalized = courseName.toLowerCase().trim();

        // Try exact match first
        List<Course> allCourses = sisDataService.getAllCourses();
        // ✅ NULL SAFE: Validate repository result
        if (allCourses == null) {
            return null;
        }

        for (Course course : allCourses) {
            // ✅ NULL SAFE: Skip null courses and check course name exists
            if (course == null || course.getCourseName() == null) continue;
            if (course.getCourseName().equalsIgnoreCase(courseName)) {
                return course;
            }
        }

        // Try partial match
        for (Course course : allCourses) {
            // ✅ NULL SAFE: Skip null courses and check course name exists
            if (course == null || course.getCourseName() == null) continue;
            String courseNormalized = course.getCourseName().toLowerCase();
            if (courseNormalized.contains(normalized) || normalized.contains(courseNormalized)) {
                return course;
            }
        }

        // Try word-based matching
        String[] words = normalized.split("\\s+");
        for (Course course : allCourses) {
            // ✅ NULL SAFE: Skip null courses and check course name exists
            if (course == null || course.getCourseName() == null) continue;
            String courseNormalized = course.getCourseName().toLowerCase();
            int matchCount = 0;
            for (String word : words) {
                // ✅ NULL SAFE: Skip null words
                if (word == null) continue;
                if (word.length() > 2 && courseNormalized.contains(word)) {
                    matchCount++;
                }
            }
            if (matchCount >= words.length / 2.0) {
                return course;
            }
        }

        return null;
    }

    /**
     * Find teacher by name
     */
    private Teacher findTeacher(String teacherName) {
        if (teacherName == null || teacherName.isEmpty()) return null;

        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActive()))
                .collect(Collectors.toList());
        // ✅ NULL SAFE: Validate repository result
        if (teachers == null) {
            return null;
        }

        String normalized = teacherName.toLowerCase().trim();

        for (Teacher teacher : teachers) {
            // ✅ NULL SAFE: Skip null teachers and check name fields exist
            if (teacher == null || teacher.getFirstName() == null || teacher.getLastName() == null) continue;

            String fullName = (teacher.getFirstName() + " " + teacher.getLastName()).toLowerCase();
            String reverseName = (teacher.getLastName() + ", " + teacher.getFirstName()).toLowerCase();

            if (fullName.equals(normalized) || reverseName.equals(normalized) ||
                fullName.contains(normalized) || normalized.contains(teacher.getLastName().toLowerCase())) {
                return teacher;
            }
        }

        return null;
    }

    /**
     * Find room by number
     */
    private Room findRoom(String roomNumber) {
        if (roomNumber == null || roomNumber.isEmpty()) return null;

        List<Room> rooms = roomRepository.findAll();
        // ✅ NULL SAFE: Validate repository result
        if (rooms == null) {
            return null;
        }

        String normalized = roomNumber.toLowerCase().trim();

        for (Room room : rooms) {
            // ✅ NULL SAFE: Skip null rooms and check room number exists
            if (room == null || room.getRoomNumber() == null) continue;
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber) ||
                room.getRoomNumber().toLowerCase().contains(normalized)) {
                return room;
            }
        }

        return null;
    }

    /**
     * Parse day of week from string
     */
    private DayOfWeek parseDayOfWeek(String day) {
        if (day == null) return null;

        String d = day.toUpperCase().trim();
        return switch (d) {
            case "MONDAY", "MON", "M" -> DayOfWeek.MONDAY;
            case "TUESDAY", "TUE", "T" -> DayOfWeek.TUESDAY;
            case "WEDNESDAY", "WED", "W" -> DayOfWeek.WEDNESDAY;
            case "THURSDAY", "THU", "R" -> DayOfWeek.THURSDAY;
            case "FRIDAY", "FRI", "F" -> DayOfWeek.FRIDAY;
            default -> null;
        };
    }

    /**
     * Parse time from string
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null) return null;

        try {
            // Try various formats
            String normalized = timeStr.trim().toUpperCase();

            // Handle AM/PM
            boolean isPM = normalized.contains("PM");
            normalized = normalized.replaceAll("\\s*(AM|PM)\\s*", "");

            String[] parts = normalized.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            if (isPM && hour < 12) hour += 12;
            if (!isPM && hour == 12) hour = 0;

            return LocalTime.of(hour, minute);
        } catch (Exception e) {
            log.warn("Could not parse time: {}", timeStr);
            return null;
        }
    }

    /**
     * Confirm and execute import of validated slots
     */
    @Transactional
    public int executeImport(List<ScheduleSlotImport> slots) {
        int imported = 0;

        // ✅ NULL SAFE: Validate slots list
        if (slots == null) {
            log.warn("Cannot execute import with null slots list");
            return imported;
        }

        for (ScheduleSlotImport slot : slots) {
            // ✅ NULL SAFE: Skip null slots
            if (slot == null) continue;
            if (!slot.isReadyToImport()) continue;

            try {
                // ✅ NULL SAFE: Validate slot entry
                if (slot.getEntry() == null) {
                    log.warn("Slot has null entry, skipping import");
                    continue;
                }

                ScheduleSlot scheduleSlot = new ScheduleSlot();
                scheduleSlot.setCourse(slot.getMatchedCourse());
                scheduleSlot.setTeacher(slot.getMatchedTeacher());
                scheduleSlot.setRoom(slot.getMatchedRoom());
                scheduleSlot.setDayOfWeek(slot.getEntry().getDayOfWeek());
                scheduleSlot.setStartTime(slot.getEntry().getStartTime());
                scheduleSlot.setEndTime(slot.getEntry().getEndTime());
                scheduleSlot.setPeriodNumber(slot.getEntry().getPeriodNumber());

                scheduleSlotRepository.save(scheduleSlot);
                imported++;
            } catch (Exception e) {
                // ✅ NULL SAFE: Safe extraction of error message
                String errorMsg = (e.getMessage() != null) ? e.getMessage() : "Unknown error";
                log.error("Failed to import slot: {}", errorMsg);
            }
        }

        log.info("Imported {} schedule slots", imported);
        return imported;
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleImportResult {
        private String fileName;
        private boolean ocrUsed;
        private String rawText;
        private int parsedEntries;
        private List<ScheduleSlotImport> importedSlots;
        private int successCount;
        private int failureCount;
        private String error;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedScheduleEntry {
        private String rawLine;
        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer periodNumber;
        private String courseName;
        private String teacherName;
        private String roomNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleSlotImport {
        private ParsedScheduleEntry entry;
        private boolean success;
        private boolean readyToImport;
        private String error;
        private String suggestedAction;
        private Course matchedCourse;
        private Teacher matchedTeacher;
        private Room matchedRoom;
    }
}

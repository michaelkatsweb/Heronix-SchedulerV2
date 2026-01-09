package com.heronix.scheduler.client;

import com.heronix.scheduler.model.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * SIS API Client - Communication Layer with Heronix SIS
 *
 * Purpose: Fetch student, teacher, course, and enrollment data from SIS
 *
 * SIS API Base URL: http://localhost:8080/api (default)
 *
 * Endpoints:
 * - GET /api/students - All students
 * - GET /api/students/{id} - Student details
 * - GET /api/teachers - All teachers
 * - GET /api/teachers/{id} - Teacher details
 * - GET /api/courses - All courses
 * - GET /api/courses/{id} - Course details
 * - GET /api/enrollments - All enrollments
 * - GET /api/lunch-assignments - Lunch assignments
 *
 * TODO: EXTERNAL SIS INTEGRATION - ADAPTER PATTERN IMPLEMENTATION
 * ================================================================
 *
 * To support external SIS systems (Skyward, PowerSchool, Infinite Campus, etc.),
 * implement the following adapter pattern:
 *
 * 1. Create SIS Adapter Interface:
 *    ```java
 *    public interface ExternalSISAdapter {
 *        List<StudentDTO> fetchStudents();
 *        List<TeacherDTO> fetchTeachers();
 *        List<CourseDTO> fetchCourses();
 *        List<EnrollmentDTO> fetchEnrollments();
 *        boolean testConnection();
 *        String getSISSystemName();
 *    }
 *    ```
 *
 * 2. Implement Skyward Adapter:
 *    ```java
 *    @Service("skywardAdapter")
 *    public class SkywardApiAdapter implements ExternalSISAdapter {
 *        private final RestTemplate restTemplate;
 *        private final String skywardApiUrl; // from application.yml
 *        private final String skywardApiKey;
 *
 *        @Override
 *        public List<StudentDTO> fetchStudents() {
 *            // Call Skyward REST API endpoints
 *            // Map Skyward student model to StudentDTO
 *            // Handle Skyward-specific authentication (OAuth, API key)
 *        }
 *
 *        // Data transformation example:
 *        private StudentDTO mapSkywardStudentToDTO(SkywardStudent skywardStudent) {
 *            return StudentDTO.builder()
 *                .externalId(skywardStudent.getStudentNumber())
 *                .firstName(skywardStudent.getFirstName())
 *                .lastName(skywardStudent.getLastName())
 *                .gradeLevel(skywardStudent.getGradeLevel())
 *                .source("SKYWARD")
 *                .build();
 *        }
 *    }
 *    ```
 *
 * 3. Implement PowerSchool Adapter:
 *    ```java
 *    @Service("powerschoolAdapter")
 *    public class PowerSchoolApiAdapter implements ExternalSISAdapter {
 *        // PowerSchool uses plugin-based API access
 *        // Requires PowerSchool plugin installed on their server
 *        // Authentication: OAuth 2.0
 *        // Endpoints: /ws/v1/student, /ws/v1/teacher, etc.
 *    }
 *    ```
 *
 * 4. Implement Infinite Campus Adapter:
 *    ```java
 *    @Service("infiniteCampusAdapter")
 *    public class InfiniteCampusApiAdapter implements ExternalSISAdapter {
 *        // Infinite Campus Messenger API
 *        // Authentication: District-specific credentials
 *        // Data export formats: XML, CSV
 *    }
 *    ```
 *
 * 5. Create Adapter Factory/Manager:
 *    ```java
 *    @Service
 *    public class SISAdapterManager {
 *        private final Map<String, ExternalSISAdapter> adapters;
 *
 *        public SISAdapterManager(
 *            @Qualifier("skywardAdapter") ExternalSISAdapter skywardAdapter,
 *            @Qualifier("powerschoolAdapter") ExternalSISAdapter powerschoolAdapter,
 *            @Qualifier("infiniteCampusAdapter") ExternalSISAdapter infiniteCampusAdapter) {
 *            this.adapters = Map.of(
 *                "SKYWARD", skywardAdapter,
 *                "POWERSCHOOL", powerschoolAdapter,
 *                "INFINITE_CAMPUS", infiniteCampusAdapter
 *            );
 *        }
 *
 *        public ExternalSISAdapter getAdapter(String sisType) {
 *            return adapters.get(sisType.toUpperCase());
 *        }
 *    }
 *    ```
 *
 * 6. Configuration (application.yml):
 *    ```yaml
 *    heronix:
 *      scheduler:
 *        external-sis:
 *          enabled: true
 *          type: SKYWARD  # or POWERSCHOOL, INFINITE_CAMPUS, HERONIX
 *          skyward:
 *            api-url: https://skyward.district.edu/api
 *            api-key: ${SKYWARD_API_KEY}
 *            district-id: ${SKYWARD_DISTRICT_ID}
 *          powerschool:
 *            api-url: https://powerschool.district.edu/ws/v1
 *            client-id: ${POWERSCHOOL_CLIENT_ID}
 *            client-secret: ${POWERSCHOOL_CLIENT_SECRET}
 *          infinite-campus:
 *            api-url: https://infinitecampus.district.edu/api
 *            username: ${IC_USERNAME}
 *            password: ${IC_PASSWORD}
 *    ```
 *
 * 7. Usage in Scheduler:
 *    ```java
 *    @Service
 *    public class DataSyncService {
 *        private final SISAdapterManager adapterManager;
 *        private final String configuredSISType; // from application.yml
 *
 *        public void syncStudents() {
 *            ExternalSISAdapter adapter = adapterManager.getAdapter(configuredSISType);
 *            if (adapter != null && adapter.testConnection()) {
 *                List<StudentDTO> students = adapter.fetchStudents();
 *                // Process and store in local database
 *            }
 *        }
 *    }
 *    ```
 *
 * 8. Field Mapping Configuration:
 *    - Create mapping configuration UI for custom field mappings
 *    - Allow districts to map their SIS fields to Heronix fields
 *    - Store mappings in FieldMappingConfiguration entity
 *    - Support custom field transformations (date formats, name parsing, etc.)
 *
 * 9. Data Validation Pipeline:
 *    - Validate imported data against Heronix schema
 *    - Handle missing required fields with defaults or errors
 *    - Log validation errors for district review
 *    - Support partial imports with error reporting
 *
 * 10. Sync Scheduling:
 *     - Implement scheduled sync jobs (@Scheduled)
 *     - Support manual sync triggers via UI
 *     - Track last sync timestamp per entity type
 *     - Implement incremental sync (only changed records)
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Slf4j
@Service
public class SISApiClient {

    private final RestTemplate restTemplate;
    private final String sisBaseUrl;

    public SISApiClient(
            RestTemplate restTemplate,
            @Value("${heronix.scheduler.sis.api-url:http://localhost:8080/api}") String sisBaseUrl) {
        this.restTemplate = restTemplate;
        this.sisBaseUrl = sisBaseUrl;
        log.info("SIS API Client initialized with base URL: {}", sisBaseUrl);
    }

    // ========================================================================
    // Student API
    // ========================================================================

    /**
     * Fetch all students from SIS
     */
    public List<StudentDTO> getAllStudents() {
        try {
            log.debug("Fetching all students from SIS...");
            ResponseEntity<List<StudentDTO>> response = restTemplate.exchange(
                    sisBaseUrl + "/students",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<StudentDTO>>() {}
            );

            List<StudentDTO> students = response.getBody();
            log.info("Fetched {} students from SIS", students != null ? students.size() : 0);
            return students != null ? students : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Failed to fetch students from SIS: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch single student by ID
     */
    public StudentDTO getStudentById(Long id) {
        try {
            log.debug("Fetching student {} from SIS...", id);
            ResponseEntity<StudentDTO> response = restTemplate.getForEntity(
                    sisBaseUrl + "/students/" + id,
                    StudentDTO.class
            );

            StudentDTO student = response.getBody();
            log.debug("Fetched student: {}", student);
            return student;

        } catch (RestClientException e) {
            log.error("Failed to fetch student {}: {}", id, e.getMessage());
            return null;
        }
    }

    // ========================================================================
    // Teacher API
    // ========================================================================

    /**
     * Fetch all teachers from SIS
     */
    public List<TeacherDTO> getAllTeachers() {
        try {
            log.debug("Fetching all teachers from SIS...");
            ResponseEntity<List<TeacherDTO>> response = restTemplate.exchange(
                    sisBaseUrl + "/teachers",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<TeacherDTO>>() {}
            );

            List<TeacherDTO> teachers = response.getBody();
            log.info("Fetched {} teachers from SIS", teachers != null ? teachers.size() : 0);
            return teachers != null ? teachers : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Failed to fetch teachers from SIS: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch single teacher by ID
     */
    public TeacherDTO getTeacherById(Long id) {
        try {
            log.debug("Fetching teacher {} from SIS...", id);
            ResponseEntity<TeacherDTO> response = restTemplate.getForEntity(
                    sisBaseUrl + "/teachers/" + id,
                    TeacherDTO.class
            );

            TeacherDTO teacher = response.getBody();
            log.debug("Fetched teacher: {}", teacher);
            return teacher;

        } catch (RestClientException e) {
            log.error("Failed to fetch teacher {}: {}", id, e.getMessage());
            return null;
        }
    }

    // ========================================================================
    // Course API
    // ========================================================================

    /**
     * Fetch all courses from SIS
     */
    public List<CourseDTO> getAllCourses() {
        try {
            log.debug("Fetching all courses from SIS...");
            ResponseEntity<List<CourseDTO>> response = restTemplate.exchange(
                    sisBaseUrl + "/courses",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<CourseDTO>>() {}
            );

            List<CourseDTO> courses = response.getBody();
            log.info("Fetched {} courses from SIS", courses != null ? courses.size() : 0);
            return courses != null ? courses : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Failed to fetch courses from SIS: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch single course by ID
     */
    public CourseDTO getCourseById(Long id) {
        try {
            log.debug("Fetching course {} from SIS...", id);
            ResponseEntity<CourseDTO> response = restTemplate.getForEntity(
                    sisBaseUrl + "/courses/" + id,
                    CourseDTO.class
            );

            CourseDTO course = response.getBody();
            log.debug("Fetched course: {}", course);
            return course;

        } catch (RestClientException e) {
            log.error("Failed to fetch course {}: {}", id, e.getMessage());
            return null;
        }
    }

    // ========================================================================
    // Enrollment API
    // ========================================================================

    /**
     * Fetch all enrollments from SIS
     */
    public List<EnrollmentDTO> getAllEnrollments() {
        try {
            log.debug("Fetching all enrollments from SIS...");
            ResponseEntity<List<EnrollmentDTO>> response = restTemplate.exchange(
                    sisBaseUrl + "/enrollments",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<EnrollmentDTO>>() {}
            );

            List<EnrollmentDTO> enrollments = response.getBody();
            log.info("Fetched {} enrollments from SIS", enrollments != null ? enrollments.size() : 0);
            return enrollments != null ? enrollments : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Failed to fetch enrollments from SIS: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ========================================================================
    // Lunch Assignment API
    // ========================================================================

    /**
     * Fetch all lunch assignments from SIS
     */
    public List<LunchAssignmentDTO> getAllLunchAssignments() {
        try {
            log.debug("Fetching all lunch assignments from SIS...");
            ResponseEntity<List<LunchAssignmentDTO>> response = restTemplate.exchange(
                    sisBaseUrl + "/lunch-assignments",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<LunchAssignmentDTO>>() {}
            );

            List<LunchAssignmentDTO> assignments = response.getBody();
            log.info("Fetched {} lunch assignments from SIS", assignments != null ? assignments.size() : 0);
            return assignments != null ? assignments : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Failed to fetch lunch assignments from SIS: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ========================================================================
    // Teacher Availability API
    // ========================================================================

    /**
     * Fetch teacher availability from SIS
     */
    public List<TeacherAvailabilityDTO> getTeacherAvailability(Long teacherId) {
        try {
            log.debug("Fetching availability for teacher {} from SIS...", teacherId);
            ResponseEntity<List<TeacherAvailabilityDTO>> response = restTemplate.exchange(
                    sisBaseUrl + "/teachers/" + teacherId + "/availability",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<TeacherAvailabilityDTO>>() {}
            );

            List<TeacherAvailabilityDTO> availability = response.getBody();
            log.debug("Fetched {} availability windows for teacher {}",
                    availability != null ? availability.size() : 0, teacherId);
            return availability != null ? availability : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Failed to fetch availability for teacher {}: {}", teacherId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ========================================================================
    // Health Check
    // ========================================================================

    /**
     * Check if SIS API is available
     */
    public boolean isSISAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    sisBaseUrl.replace("/api", "") + "/actuator/health",
                    String.class
            );
            boolean available = response.getStatusCode().is2xxSuccessful();
            log.info("SIS API health check: {}", available ? "UP" : "DOWN");
            return available;

        } catch (RestClientException e) {
            log.warn("SIS API is not available: {}", e.getMessage());
            return false;
        }
    }
}

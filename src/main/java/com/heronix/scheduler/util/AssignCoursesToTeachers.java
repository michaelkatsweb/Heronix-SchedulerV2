package com.heronix.scheduler.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║ ASSIGN COURSES TO EXISTING TEACHERS                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * This utility assigns courses to existing teachers based on their departments.
 * No UI required - runs as a command-line tool.
 *
 * Usage:
 * cd h:\Heronix Scheduler\eduscheduler-pro
 * mvn exec:java -Dexec.mainClass=com.heronix.util.AssignCoursesToTeachers
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-18
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "com.heronix")
@EntityScan(basePackages = "com.heronix.model")
@EnableJpaRepositories(basePackages = "com.heronix.repository")
public class AssignCoursesToTeachers implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        // Disable GUI
        System.setProperty("java.awt.headless", "true");
        SpringApplication app = new SpringApplication(AssignCoursesToTeachers.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("╔══════════════════════════════════════════════════════════════════════════╗");
        log.info("║ ASSIGN COURSES TO TEACHERS - Starting...                                 ║");
        log.info("╚══════════════════════════════════════════════════════════════════════════╝");

        try {
            // Step 1: Check current state
            log.info("\n=== STEP 1: CHECKING CURRENT STATE ===\n");
            checkCurrentState();

            // Step 2: Assign courses
            log.info("\n=== STEP 2: ASSIGNING COURSES TO TEACHERS ===\n");
            assignCourses();

            // Step 3: Verify results
            log.info("\n=== STEP 3: VERIFYING RESULTS ===\n");
            verifyResults();

            log.info("\n╔══════════════════════════════════════════════════════════════════════════╗");
            log.info("║ ✅ ASSIGNMENT COMPLETE!                                                   ║");
            log.info("╚══════════════════════════════════════════════════════════════════════════╝\n");

        } catch (Exception e) {
            log.error("❌ Error assigning courses", e);
            throw e;
        }
    }

    private void checkCurrentState() {
        log.info("Checking teachers and their current assignments...\n");

        List<Map<String, Object>> teachers = jdbcTemplate.queryForList(
            "SELECT t.id, t.first_name, t.last_name, t.department, " +
            "COUNT(c.id) as assigned_courses " +
            "FROM teachers t " +
            "LEFT JOIN courses c ON c.teacher_id = t.id " +
            "WHERE t.active = TRUE " +
            "GROUP BY t.id, t.first_name, t.last_name, t.department " +
            "ORDER BY t.department, t.last_name"
        );

        log.info("Current Teachers:");
        for (Map<String, Object> teacher : teachers) {
            log.info("  {} {} ({}) - {} courses",
                teacher.get("first_name"),
                teacher.get("last_name"),
                teacher.get("department"),
                teacher.get("assigned_courses")
            );
        }

        Integer unassignedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM courses WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0)",
            Integer.class
        );

        log.info("\nUnassigned courses: {}\n", unassignedCount);
    }

    private void assignCourses() {
        int totalAssigned = 0;

        // MATHEMATICS
        log.info("Assigning Mathematics courses...");
        int mathAssigned = jdbcTemplate.update(
            "UPDATE courses SET teacher_id = (" +
            "  SELECT t.id FROM teachers t " +
            "  WHERE t.active = TRUE AND UPPER(t.department) LIKE '%MATH%' " +
            "  ORDER BY (SELECT COUNT(*) FROM courses c2 WHERE c2.teacher_id = t.id) ASC " +
            "  LIMIT 1" +
            ") WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0) " +
            "AND (UPPER(subject) LIKE '%MATH%' OR UPPER(subject) LIKE '%ALGEBRA%' " +
            "OR UPPER(subject) LIKE '%GEOMETRY%' OR UPPER(subject) LIKE '%CALCULUS%' " +
            "OR UPPER(course_code) LIKE 'MATH%' OR UPPER(course_code) LIKE 'ALG%')"
        );
        log.info("  ✅ Assigned {} mathematics courses", mathAssigned);
        totalAssigned += mathAssigned;

        // SCIENCE
        log.info("Assigning Science courses...");
        int scienceAssigned = jdbcTemplate.update(
            "UPDATE courses SET teacher_id = (" +
            "  SELECT t.id FROM teachers t " +
            "  WHERE t.active = TRUE AND UPPER(t.department) LIKE '%SCIENCE%' " +
            "  ORDER BY (SELECT COUNT(*) FROM courses c2 WHERE c2.teacher_id = t.id) ASC " +
            "  LIMIT 1" +
            ") WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0) " +
            "AND (UPPER(subject) LIKE '%SCIENCE%' OR UPPER(subject) LIKE '%BIOLOGY%' " +
            "OR UPPER(subject) LIKE '%CHEMISTRY%' OR UPPER(subject) LIKE '%PHYSICS%' " +
            "OR UPPER(course_code) LIKE 'SCI%' OR UPPER(course_code) LIKE 'BIO%')"
        );
        log.info("  ✅ Assigned {} science courses", scienceAssigned);
        totalAssigned += scienceAssigned;

        // ENGLISH
        log.info("Assigning English courses...");
        int englishAssigned = jdbcTemplate.update(
            "UPDATE courses SET teacher_id = (" +
            "  SELECT t.id FROM teachers t " +
            "  WHERE t.active = TRUE AND UPPER(t.department) LIKE '%ENGLISH%' " +
            "  ORDER BY (SELECT COUNT(*) FROM courses c2 WHERE c2.teacher_id = t.id) ASC " +
            "  LIMIT 1" +
            ") WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0) " +
            "AND (UPPER(subject) LIKE '%ENGLISH%' OR UPPER(subject) LIKE '%LITERATURE%' " +
            "OR UPPER(course_code) LIKE 'ENG%' OR UPPER(course_code) LIKE 'LIT%')"
        );
        log.info("  ✅ Assigned {} english courses", englishAssigned);
        totalAssigned += englishAssigned;

        // SOCIAL STUDIES
        log.info("Assigning Social Studies courses...");
        int socialAssigned = jdbcTemplate.update(
            "UPDATE courses SET teacher_id = (" +
            "  SELECT t.id FROM teachers t " +
            "  WHERE t.active = TRUE AND (UPPER(t.department) LIKE '%SOCIAL%' OR UPPER(t.department) LIKE '%HISTORY%') " +
            "  ORDER BY (SELECT COUNT(*) FROM courses c2 WHERE c2.teacher_id = t.id) ASC " +
            "  LIMIT 1" +
            ") WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0) " +
            "AND (UPPER(subject) LIKE '%HISTORY%' OR UPPER(subject) LIKE '%SOCIAL%' " +
            "OR UPPER(course_code) LIKE 'SS%' OR UPPER(course_code) LIKE 'HIST%')"
        );
        log.info("  ✅ Assigned {} social studies courses", socialAssigned);
        totalAssigned += socialAssigned;

        // WORLD LANGUAGES
        log.info("Assigning World Languages courses...");
        int languageAssigned = jdbcTemplate.update(
            "UPDATE courses SET teacher_id = (" +
            "  SELECT t.id FROM teachers t " +
            "  WHERE t.active = TRUE AND (UPPER(t.department) LIKE '%LANGUAGE%' OR UPPER(t.department) LIKE '%WORLD%') " +
            "  ORDER BY (SELECT COUNT(*) FROM courses c2 WHERE c2.teacher_id = t.id) ASC " +
            "  LIMIT 1" +
            ") WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0) " +
            "AND (UPPER(subject) LIKE '%SPANISH%' OR UPPER(subject) LIKE '%FRENCH%' " +
            "OR UPPER(course_code) LIKE 'SPAN%' OR UPPER(course_code) LIKE 'FREN%')"
        );
        log.info("  ✅ Assigned {} language courses", languageAssigned);
        totalAssigned += languageAssigned;

        // ARTS
        log.info("Assigning Arts courses...");
        int artsAssigned = jdbcTemplate.update(
            "UPDATE courses SET teacher_id = (" +
            "  SELECT t.id FROM teachers t " +
            "  WHERE t.active = TRUE AND (UPPER(t.department) LIKE '%ART%' OR UPPER(t.department) LIKE '%MUSIC%') " +
            "  ORDER BY (SELECT COUNT(*) FROM courses c2 WHERE c2.teacher_id = t.id) ASC " +
            "  LIMIT 1" +
            ") WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0) " +
            "AND (UPPER(subject) LIKE '%ART%' OR UPPER(subject) LIKE '%MUSIC%' " +
            "OR UPPER(course_code) LIKE 'ART%' OR UPPER(course_code) LIKE 'MUS%')"
        );
        log.info("  ✅ Assigned {} arts courses", artsAssigned);
        totalAssigned += artsAssigned;

        // PHYSICAL EDUCATION
        log.info("Assigning Physical Education courses...");
        int peAssigned = jdbcTemplate.update(
            "UPDATE courses SET teacher_id = (" +
            "  SELECT t.id FROM teachers t " +
            "  WHERE t.active = TRUE AND (UPPER(t.department) LIKE '%PE%' OR UPPER(t.department) LIKE '%PHYSICAL%') " +
            "  ORDER BY (SELECT COUNT(*) FROM courses c2 WHERE c2.teacher_id = t.id) ASC " +
            "  LIMIT 1" +
            ") WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0) " +
            "AND (UPPER(subject) LIKE '%PHYSICAL%' OR UPPER(subject) LIKE '%PE%' " +
            "OR UPPER(course_code) LIKE 'PE%' OR UPPER(course_code) LIKE 'HEALTH%')"
        );
        log.info("  ✅ Assigned {} PE courses", peAssigned);
        totalAssigned += peAssigned;

        // TECHNOLOGY
        log.info("Assigning Technology courses...");
        int techAssigned = jdbcTemplate.update(
            "UPDATE courses SET teacher_id = (" +
            "  SELECT t.id FROM teachers t " +
            "  WHERE t.active = TRUE AND (UPPER(t.department) LIKE '%TECH%' OR UPPER(t.department) LIKE '%COMPUTER%') " +
            "  ORDER BY (SELECT COUNT(*) FROM courses c2 WHERE c2.teacher_id = t.id) ASC " +
            "  LIMIT 1" +
            ") WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0) " +
            "AND (UPPER(subject) LIKE '%COMPUTER%' OR UPPER(subject) LIKE '%TECHNOLOGY%' " +
            "OR UPPER(course_code) LIKE 'CS%' OR UPPER(course_code) LIKE 'TECH%')"
        );
        log.info("  ✅ Assigned {} technology courses", techAssigned);
        totalAssigned += techAssigned;

        log.info("\n✅ TOTAL COURSES ASSIGNED: {}\n", totalAssigned);
    }

    private void verifyResults() {
        log.info("Final course distribution by teacher:\n");

        List<Map<String, Object>> distribution = jdbcTemplate.queryForList(
            "SELECT t.first_name, t.last_name, t.department, COUNT(c.id) as assigned_courses " +
            "FROM teachers t " +
            "LEFT JOIN courses c ON c.teacher_id = t.id AND c.active = TRUE " +
            "WHERE t.active = TRUE " +
            "GROUP BY t.id, t.first_name, t.last_name, t.department " +
            "HAVING COUNT(c.id) > 0 " +
            "ORDER BY t.department, assigned_courses DESC"
        );

        for (Map<String, Object> teacher : distribution) {
            log.info("  {} {} ({}) - {} courses",
                teacher.get("first_name"),
                teacher.get("last_name"),
                teacher.get("department"),
                teacher.get("assigned_courses")
            );
        }

        Integer remainingUnassigned = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM courses WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0)",
            Integer.class
        );

        log.info("\n✅ Remaining unassigned courses: {}", remainingUnassigned);

        if (remainingUnassigned > 0) {
            log.warn("\n⚠️  Some courses remain unassigned. This may be because:");
            log.warn("   - No teachers exist for those departments");
            log.warn("   - Course subjects don't match any teacher departments");
            log.warn("   - These courses may need manual assignment");
        }
    }
}

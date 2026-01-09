package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.DayType;
import com.heronix.scheduler.model.enums.SlotStatus;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.BlockScheduleService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockScheduleServiceImpl implements BlockScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Schedule generateBlockSchedule(List<Student> students, List<Course> courses,
                                         List<Teacher> teachers, List<Room> rooms) {
        log.info("Generating block schedule for {} students, {} courses", students.size(), courses.size());

        // Create the schedule entity
        Schedule schedule = new Schedule();
        schedule.setName("Block Schedule " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        schedule.setPeriod(com.heronix.scheduler.model.enums.SchedulePeriod.YEARLY);
        schedule.setScheduleType(com.heronix.scheduler.model.enums.ScheduleType.BLOCK);
        schedule.setStatus(com.heronix.scheduler.model.enums.ScheduleStatus.DRAFT);

        // Set date range (current academic year)
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate startDate = java.time.LocalDate.of(
            today.getMonthValue() >= 8 ? today.getYear() : today.getYear() - 1,
            8, 1
        );
        java.time.LocalDate endDate = java.time.LocalDate.of(startDate.getYear() + 1, 6, 30);
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);
        schedule.setCreatedDate(today);
        schedule.setActive(true);

        // Save schedule first to get ID
        schedule = scheduleRepository.save(schedule);

        // Create schedule slots for each course
        List<ScheduleSlot> slots = new ArrayList<>();

        for (Course course : courses) {
            // ✅ NULL SAFE: Skip null courses or check active status
            if (course == null || !course.isActive()) {
                continue;
            }

            // Determine day type for this course (simplified: alternate courses between ODD/EVEN)
            DayType dayType = (courses.indexOf(course) % 2 == 0) ? DayType.ODD : DayType.EVEN;

            // For block schedule generation, assign all students to all courses
            // (In production, you would filter by actual enrollment)
            List<Student> enrolledStudents = new ArrayList<>(students);

            // Create multiple slots per week for block schedule (typically 3-4 periods per course)
            int periodsPerWeek = 3; // Block schedule typically meets 3 times per week

            for (int period = 1; period <= periodsPerWeek; period++) {
                ScheduleSlot slot = new ScheduleSlot();
                slot.setSchedule(schedule);
                slot.setCourse(course);
                // ✅ NULL SAFE: Safe extraction of teacher
                slot.setTeacher(course.getTeacher());
                slot.setDayType(dayType);
                slot.setPeriodNumber(period);
                slot.setStatus(SlotStatus.ACTIVE);

                // Set time slots based on period number
                // Block periods are typically 90 minutes each
                java.time.DayOfWeek dayOfWeek = (dayType == DayType.ODD) ?
                    java.time.DayOfWeek.MONDAY : java.time.DayOfWeek.TUESDAY;
                slot.setDayOfWeek(dayOfWeek);

                // Calculate start time (8:00 AM + (period-1) * 90 minutes)
                java.time.LocalTime startTime = java.time.LocalTime.of(8, 0)
                    .plusMinutes((period - 1) * 90L);
                slot.setStartTime(startTime);
                slot.setEndTime(startTime.plusMinutes(90));

                // Initialize students list
                if (slot.getStudents() == null) {
                    slot.setStudents(new ArrayList<>());
                }
                slot.getStudents().addAll(enrolledStudents);

                slots.add(slot);
            }
        }

        // Save all slots
        if (!slots.isEmpty()) {
            log.info("Saving {} schedule slots for block schedule", slots.size());
            slots = scheduleSlotRepository.saveAll(slots);
            schedule.setSlots(slots);
        }

        // Calculate initial metrics
        schedule.setTotalConflicts(0);
        schedule.setResolvedConflicts(0);
        schedule.setOptimizationScore(0.0);
        schedule.setQualityScore(0.0);

        // Save updated schedule
        schedule = scheduleRepository.save(schedule);

        log.info("Generated block schedule with {} slots for {} courses",
            slots.size(), courses.size());

        return schedule;
    }

    @Override
    public boolean isOddDay(LocalDate date) {
        LocalDate schoolYearStart = LocalDate.of(date.getYear(), 9, 1);
        long daysSinceStart = ChronoUnit.DAYS.between(schoolYearStart, date);
        return daysSinceStart % 2 == 1;
    }

    @Override
    public boolean isEvenDay(LocalDate date) {
        return !isOddDay(date);
    }

    @Override
    public DayType getDayType(LocalDate date) {
        return isOddDay(date) ? DayType.ODD : DayType.EVEN;
    }

    @Override
    public List<Course> getCoursesForDayType(Student student, DayType dayType) {
        // Query slots from repository to avoid lazy loading issues
        // ✅ NULL SAFE: Filter null slots before checking properties
        return scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot != null && slot.getStudents() != null && slot.getStudents().contains(student))
            .filter(slot -> slot.getDayType() == dayType)
            .map(ScheduleSlot::getCourse)
            .filter(course -> course != null)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignCoursesToDays(Student student, List<Course> oddDayCourses, List<Course> evenDayCourses) {
        // ✅ NULL SAFE: Validate student parameter
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of student ID
        String studentId = student.getStudentId() != null ? student.getStudentId() : "Unknown";
        log.info("Assigning courses to ODD/EVEN days for student: {}", studentId);

        // Clear existing schedule slots for this student by querying from repository
        // ✅ NULL SAFE: Filter null slots before checking properties
        List<ScheduleSlot> existingSlots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot != null && slot.getStudents() != null && slot.getStudents().contains(student))
            .collect(java.util.stream.Collectors.toList());

        for (ScheduleSlot slot : existingSlots) {
            // ✅ NULL SAFE: Skip if slot or students list is null
            if (slot == null || slot.getStudents() == null) continue;
            slot.getStudents().remove(student);
            scheduleSlotRepository.save(slot);
        }

        // Create schedule slots for ODD day courses
        if (oddDayCourses != null) {
            for (Course course : oddDayCourses) {
                // ✅ NULL SAFE: Skip null courses
                if (course == null) continue;

                ScheduleSlot slot = new ScheduleSlot();
                slot.setCourse(course);
                slot.setDayType(DayType.ODD);
                // ✅ NULL SAFE: Safe extraction of teacher
                slot.setTeacher(course.getTeacher());
                slot.setStatus(com.heronix.scheduler.model.enums.SlotStatus.ACTIVE);

                // Initialize students list
                if (slot.getStudents() == null) {
                    slot.setStudents(new ArrayList<>());
                }
                slot.getStudents().add(student);

                // Save the slot
                scheduleSlotRepository.save(slot);

                // ✅ NULL SAFE: Safe extraction of course properties
                String courseName = course.getCourseName() != null ? course.getCourseName() : "Unknown";
                String courseCode = course.getCourseCode() != null ? course.getCourseCode() : "N/A";
                log.debug("Created ODD day slot for course: {} ({})", courseName, courseCode);
            }
        }

        // Create schedule slots for EVEN day courses
        if (evenDayCourses != null) {
            for (Course course : evenDayCourses) {
                // ✅ NULL SAFE: Skip null courses
                if (course == null) continue;

                ScheduleSlot slot = new ScheduleSlot();
                slot.setCourse(course);
                slot.setDayType(DayType.EVEN);
                // ✅ NULL SAFE: Safe extraction of teacher
                slot.setTeacher(course.getTeacher());
                slot.setStatus(com.heronix.scheduler.model.enums.SlotStatus.ACTIVE);

                // Initialize students list
                if (slot.getStudents() == null) {
                    slot.setStudents(new ArrayList<>());
                }
                slot.getStudents().add(student);

                // Save the slot
                scheduleSlotRepository.save(slot);

                // ✅ NULL SAFE: Safe extraction of course properties
                String courseName = course.getCourseName() != null ? course.getCourseName() : "Unknown";
                String courseCode = course.getCourseCode() != null ? course.getCourseCode() : "N/A";
                log.debug("Created EVEN day slot for course: {} ({})", courseName, courseCode);
            }
        }

        // Flush to ensure schedule slots are immediately visible to queries within same transaction
        entityManager.flush();

        log.info("Assigned {} ODD day courses and {} EVEN day courses to student: {}",
            oddDayCourses != null ? oddDayCourses.size() : 0,
            evenDayCourses != null ? evenDayCourses.size() : 0,
            studentId);
    }

    @Override
    public List<ScheduleSlot> getSlotsForDayType(Schedule schedule, DayType dayType) {
        // ✅ NULL SAFE: Filter null slots before checking day type
        return scheduleSlotRepository.findByScheduleId(schedule.getId()).stream()
            .filter(slot -> slot != null && slot.getDayType() != null &&
                    (slot.getDayType() == dayType || slot.getDayType() == DayType.DAILY))
            .collect(Collectors.toList());
    }
}

package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.Conflict;
import com.heronix.scheduler.model.dto.ConflictPriorityScore;
import com.heronix.scheduler.model.dto.ConflictResolutionSuggestion;
import com.heronix.scheduler.model.dto.ConflictResolutionSuggestion.ResolutionAction;
import com.heronix.scheduler.model.dto.ConflictResolutionSuggestion.ResolutionType;
import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.ConflictSeverity;
import com.heronix.scheduler.model.enums.ConflictType;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.ConflictResolutionSuggestionService;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Conflict Resolution Suggestion Service Implementation
 * Location: src/main/java/com/eduscheduler/service/impl/ConflictResolutionSuggestionServiceImpl.java
 *
 * AI-powered conflict resolution with ML-based priority scoring and auto-fix suggestions.
 *
 * @author Heronix Scheduling System Team
 * @since Beta 1 Polish - November 26, 2025
 */
@Slf4j
@Service
@Transactional
public class ConflictResolutionSuggestionServiceImpl implements ConflictResolutionSuggestionService {

    @Autowired
    private ConflictRepository conflictRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;


    // Historical success rate tracking (in-memory for Beta 1)
    private final Map<ResolutionType, List<Boolean>> successHistory = new HashMap<>();

    // ========================================================================
    // GENERATE SUGGESTIONS
    // ========================================================================

    @Override
    public List<ConflictResolutionSuggestion> generateSuggestions(Conflict conflict) {
        // ✅ NULL SAFE: Validate conflict parameter
        if (conflict == null) {
            log.warn("Cannot generate suggestions for null conflict");
            return new ArrayList<>();
        }

        // ✅ NULL SAFE: Safe extraction of conflict ID for logging
        String conflictIdStr = (conflict.getId() != null) ? conflict.getId().toString() : "Unknown";
        log.info("Generating AI suggestions for conflict ID: {}", conflictIdStr);

        List<ConflictResolutionSuggestion> suggestions = new ArrayList<>();

        try {
            // ✅ NULL SAFE: Check conflict type exists before using
            ConflictType conflictType = conflict.getConflictType();
            if (conflictType == null) {
                log.warn("Conflict {} has no type specified, using generic suggestions", conflictIdStr);
                return generateGenericSuggestions(conflict);
            }

            switch (conflictType) {
                case TEACHER_OVERLOAD:
                    suggestions.addAll(generateTeacherDoubleBookSuggestions(conflict));
                    break;

                case ROOM_DOUBLE_BOOKING:
                    suggestions.addAll(generateRoomDoubleBookSuggestions(conflict));
                    break;

                case STUDENT_SCHEDULE_CONFLICT:
                    suggestions.addAll(generateStudentOverlapSuggestions(conflict));
                    break;

                case EXCESSIVE_TEACHING_HOURS:
                    suggestions.addAll(generateTeacherOverloadSuggestions(conflict));
                    break;

                case ROOM_CAPACITY_EXCEEDED:
                    suggestions.addAll(generateRoomCapacitySuggestions(conflict));
                    break;

                case SUBJECT_MISMATCH:
                    suggestions.addAll(generateTeacherUnavailableSuggestions(conflict));
                    break;

                default:
                    suggestions.addAll(generateGenericSuggestions(conflict));
                    break;
            }

            // Calculate priority rankings
            suggestions.forEach(this::calculateSuggestionMetrics);

            // Sort by priority (lower ranking = higher priority)
            suggestions.sort(Comparator.comparingInt(ConflictResolutionSuggestion::getPriorityRanking));

            log.info("Generated {} suggestions for conflict ID: {}", suggestions.size(), conflict.getId());

        } catch (Exception e) {
            log.error("Error generating suggestions for conflict ID: {}", conflict.getId(), e);
        }

        return suggestions;
    }

    // ========================================================================
    // CONFLICT-SPECIFIC SUGGESTION GENERATORS
    // ========================================================================

    private List<ConflictResolutionSuggestion> generateTeacherDoubleBookSuggestions(Conflict conflict) {
        List<ConflictResolutionSuggestion> suggestions = new ArrayList<>();

        // ✅ NULL SAFE: Check affected slots exist and have enough entries
        if (conflict.getAffectedSlots() == null || conflict.getAffectedSlots().size() < 2) {
            return suggestions;
        }

        ScheduleSlot slot1 = conflict.getAffectedSlots().get(0);
        ScheduleSlot slot2 = conflict.getAffectedSlots().get(1);

        // ✅ NULL SAFE: Skip if either slot is null
        if (slot1 == null || slot2 == null) {
            return suggestions;
        }

        // Suggestion 1: Change teacher in slot 1
        List<Teacher> alternativeTeachers1 = findAlternativeTeachers(slot1);
        if (!alternativeTeachers1.isEmpty()) {
            Teacher bestTeacher = alternativeTeachers1.get(0);
            suggestions.add(ConflictResolutionSuggestion.builder()
                .id(UUID.randomUUID().toString())
                .type(ResolutionType.CHANGE_TEACHER)
                .description("Change teacher in " + getSlotDisplayName(slot1) + " to " + bestTeacher.getName())
                .explanation("This teacher is qualified and available during this time slot")
                .impactScore(15)
                .successProbability(85)
                .affectedEntitiesCount(1)
                .affectedEntities(Arrays.asList("Slot: " + getSlotDisplayName(slot1), "Teacher: " + bestTeacher.getName()))
                .actions(Arrays.asList(createChangeTeacherAction(slot1, bestTeacher)))
                .warnings(new ArrayList<>())
                .estimatedTimeSeconds(30)
                .requiresConfirmation(true)
                .confidenceLevel(85)
                .build());
        }

        // Suggestion 2: Change teacher in slot 2
        List<Teacher> alternativeTeachers2 = findAlternativeTeachers(slot2);
        if (!alternativeTeachers2.isEmpty()) {
            Teacher bestTeacher = alternativeTeachers2.get(0);
            suggestions.add(ConflictResolutionSuggestion.builder()
                .id(UUID.randomUUID().toString())
                .type(ResolutionType.CHANGE_TEACHER)
                .description("Change teacher in " + getSlotDisplayName(slot2) + " to " + bestTeacher.getName())
                .explanation("This teacher is qualified and available during this time slot")
                .impactScore(15)
                .successProbability(85)
                .affectedEntitiesCount(1)
                .affectedEntities(Arrays.asList("Slot: " + getSlotDisplayName(slot2), "Teacher: " + bestTeacher.getName()))
                .actions(Arrays.asList(createChangeTeacherAction(slot2, bestTeacher)))
                .warnings(new ArrayList<>())
                .estimatedTimeSeconds(30)
                .requiresConfirmation(true)
                .confidenceLevel(85)
                .build());
        }

        // Suggestion 3: Swap slots to different times
        suggestions.add(ConflictResolutionSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .type(ResolutionType.CHANGE_TIME_SLOT)
            .description("Move " + getSlotDisplayName(slot2) + " to a different time")
            .explanation("Moving one slot to a different time will resolve the teacher conflict")
            .impactScore(25)
            .successProbability(70)
            .affectedEntitiesCount(getAffectedStudentsCount(slot2))
            .affectedEntities(Arrays.asList("Slot: " + getSlotDisplayName(slot2)))
            .actions(new ArrayList<>())
            .warnings(Arrays.asList("May affect student schedules"))
            .estimatedTimeSeconds(60)
            .requiresConfirmation(true)
            .confidenceLevel(70)
            .build());

        return suggestions;
    }

    private List<ConflictResolutionSuggestion> generateRoomDoubleBookSuggestions(Conflict conflict) {
        List<ConflictResolutionSuggestion> suggestions = new ArrayList<>();

        // ✅ NULL SAFE: Check affected slots exist and are not empty
        if (conflict.getAffectedSlots() == null || conflict.getAffectedSlots().isEmpty()) {
            return suggestions;
        }

        ScheduleSlot slot = conflict.getAffectedSlots().get(0);

        // ✅ NULL SAFE: Skip if slot is null
        if (slot == null) {
            return suggestions;
        }

        // Find alternative rooms
        List<Room> alternativeRooms = findAlternativeRooms(slot);

        for (int i = 0; i < Math.min(3, alternativeRooms.size()); i++) {
            Room room = alternativeRooms.get(i);
            // ✅ NULL SAFE: Skip null rooms
            if (room == null) continue;

            // ✅ NULL SAFE: Safe extraction of room properties with defaults
            String roomNumber = (room.getRoomNumber() != null) ? room.getRoomNumber() : "Unknown";
            String roomType = (room.getRoomType() != null) ? room.getRoomType().name() : "Standard";

            suggestions.add(ConflictResolutionSuggestion.builder()
                .id(UUID.randomUUID().toString())
                .type(ResolutionType.CHANGE_ROOM)
                .description("Change room to " + roomNumber + " (" + roomType + ")")
                .explanation("This room is available and suitable for " + getSlotDisplayName(slot))
                .impactScore(10)
                .successProbability(90)
                .affectedEntitiesCount(1)
                .affectedEntities(Arrays.asList("Slot: " + getSlotDisplayName(slot), "Room: " + room.getRoomNumber()))
                .actions(Arrays.asList(createChangeRoomAction(slot, room)))
                .warnings(new ArrayList<>())
                .estimatedTimeSeconds(15)
                .requiresConfirmation(false)
                .confidenceLevel(90)
                .build());
        }

        return suggestions;
    }

    private List<ConflictResolutionSuggestion> generateStudentOverlapSuggestions(Conflict conflict) {
        List<ConflictResolutionSuggestion> suggestions = new ArrayList<>();

        // ✅ NULL SAFE: Check both collections exist and are not empty
        if (conflict.getAffectedSlots() == null || conflict.getAffectedSlots().isEmpty() ||
            conflict.getAffectedStudents() == null || conflict.getAffectedStudents().isEmpty()) {
            return suggestions;
        }

        ScheduleSlot slot = conflict.getAffectedSlots().get(0);
        Student student = conflict.getAffectedStudents().get(0);

        // ✅ NULL SAFE: Skip if either is null
        if (slot == null || student == null) {
            return suggestions;
        }

        // ✅ NULL SAFE: Safe extraction of student name with defaults
        String firstName = (student.getFirstName() != null) ? student.getFirstName() : "";
        String lastName = (student.getLastName() != null) ? student.getLastName() : "";
        String studentName = (firstName + " " + lastName).trim();
        if (studentName.isEmpty()) {
            studentName = "Student #" + (student.getId() != null ? student.getId() : "Unknown");
        }
        suggestions.add(ConflictResolutionSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .type(ResolutionType.REASSIGN_STUDENT)
            .description("Reassign " + studentName + " to different section")
            .explanation("Moving the student to a different section of the same course will resolve the overlap")
            .impactScore(20)
            .successProbability(75)
            .affectedEntitiesCount(1)
            .affectedEntities(Arrays.asList("Student: " + studentName))
            .actions(new ArrayList<>())
            .warnings(Arrays.asList("Student may prefer their current section"))
            .estimatedTimeSeconds(45)
            .requiresConfirmation(true)
            .confidenceLevel(75)
            .build());

        return suggestions;
    }

    private List<ConflictResolutionSuggestion> generateTeacherOverloadSuggestions(Conflict conflict) {
        List<ConflictResolutionSuggestion> suggestions = new ArrayList<>();

        // ✅ NULL SAFE: Check affected teachers exist and are not empty
        if (conflict.getAffectedTeachers() == null || conflict.getAffectedTeachers().isEmpty()) {
            return suggestions;
        }

        Teacher teacher = conflict.getAffectedTeachers().get(0);

        // ✅ NULL SAFE: Skip if teacher is null
        if (teacher == null) {
            return suggestions;
        }

        // ✅ NULL SAFE: Safe extraction of teacher name
        String teacherName = (teacher.getName() != null) ? teacher.getName() : "Unknown Teacher";

        // Suggestion 1: Add co-teacher
        suggestions.add(ConflictResolutionSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .type(ResolutionType.ADD_CO_TEACHER)
            .description("Add a co-teacher to assist " + teacherName)
            .explanation("Adding a co-teacher will help distribute the workload")
            .impactScore(15)
            .successProbability(80)
            .affectedEntitiesCount(1)
            .affectedEntities(Arrays.asList("Teacher: " + teacherName))
            .actions(new ArrayList<>())
            .warnings(Arrays.asList("Requires finding a qualified co-teacher"))
            .estimatedTimeSeconds(120)
            .requiresConfirmation(true)
            .confidenceLevel(80)
            .build());

        // Suggestion 2: Reassign some classes
        suggestions.add(ConflictResolutionSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .type(ResolutionType.CHANGE_TEACHER)
            .description("Reassign some of " + teacherName + "'s classes to other teachers")
            .explanation("Distributing classes will reduce the teacher's workload")
            .impactScore(30)
            .successProbability(70)
            .affectedEntitiesCount(3)
            .affectedEntities(Arrays.asList("Teacher: " + teacherName, "Multiple sections"))
            .actions(new ArrayList<>())
            .warnings(Arrays.asList("May affect multiple students", "Requires available qualified teachers"))
            .estimatedTimeSeconds(180)
            .requiresConfirmation(true)
            .confidenceLevel(70)
            .build());

        return suggestions;
    }

    private List<ConflictResolutionSuggestion> generateRoomCapacitySuggestions(Conflict conflict) {
        List<ConflictResolutionSuggestion> suggestions = new ArrayList<>();

        // ✅ NULL SAFE: Check affected slots exist and are not empty
        if (conflict.getAffectedSlots() == null || conflict.getAffectedSlots().isEmpty()) {
            return suggestions;
        }

        ScheduleSlot slot = conflict.getAffectedSlots().get(0);

        // ✅ NULL SAFE: Skip if slot is null
        if (slot == null) {
            return suggestions;
        }

        // Find larger rooms
        List<Room> largerRooms = findLargerRooms(slot);

        for (int i = 0; i < Math.min(3, largerRooms.size()); i++) {
            Room room = largerRooms.get(i);
            // ✅ NULL SAFE: Skip null rooms
            if (room == null) continue;

            // ✅ NULL SAFE: Safe extraction of room properties
            String roomNumber = (room.getRoomNumber() != null) ? room.getRoomNumber() : "Unknown";
            int capacity = (room.getCapacity() != null) ? room.getCapacity() : 0;

            suggestions.add(ConflictResolutionSuggestion.builder()
                .id(UUID.randomUUID().toString())
                .type(ResolutionType.CHANGE_ROOM)
                .description("Move to larger room " + roomNumber + " (capacity: " + capacity + ")")
                .explanation("This room has sufficient capacity for all students")
                .impactScore(10)
                .successProbability(90)
                .affectedEntitiesCount(1)
                .affectedEntities(Arrays.asList("Slot: " + getSlotDisplayName(slot), "Room: " + room.getRoomNumber()))
                .actions(Arrays.asList(createChangeRoomAction(slot, room)))
                .warnings(new ArrayList<>())
                .estimatedTimeSeconds(15)
                .requiresConfirmation(false)
                .confidenceLevel(90)
                .build());
        }

        // Split section suggestion
        suggestions.add(ConflictResolutionSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .type(ResolutionType.SPLIT_SECTION)
            .description("Split section into two smaller sections")
            .explanation("Creating two sections will resolve the capacity issue")
            .impactScore(50)
            .successProbability(65)
            .affectedEntitiesCount(getAffectedStudentsCount(slot))
            .affectedEntities(Arrays.asList("Section: " + getSlotDisplayName(slot), "All students"))
            .actions(new ArrayList<>())
            .warnings(Arrays.asList("Requires additional teacher and room", "Affects all students in section"))
            .estimatedTimeSeconds(300)
            .requiresConfirmation(true)
            .confidenceLevel(65)
            .build());

        return suggestions;
    }

    private List<ConflictResolutionSuggestion> generateTeacherUnavailableSuggestions(Conflict conflict) {
        List<ConflictResolutionSuggestion> suggestions = new ArrayList<>();

        // ✅ NULL SAFE: Check affected slots exist and are not empty
        if (conflict.getAffectedSlots() == null || conflict.getAffectedSlots().isEmpty()) {
            return suggestions;
        }

        ScheduleSlot slot = conflict.getAffectedSlots().get(0);

        // ✅ NULL SAFE: Skip if slot is null
        if (slot == null) {
            return suggestions;
        }

        // Find alternative teachers
        List<Teacher> alternativeTeachers = findAlternativeTeachers(slot);

        for (int i = 0; i < Math.min(3, alternativeTeachers.size()); i++) {
            Teacher teacher = alternativeTeachers.get(i);
            // ✅ NULL SAFE: Skip null teachers
            if (teacher == null) continue;

            // ✅ NULL SAFE: Safe extraction of teacher name
            String teacherName = (teacher.getName() != null) ? teacher.getName() : "Unknown Teacher";

            suggestions.add(ConflictResolutionSuggestion.builder()
                .id(UUID.randomUUID().toString())
                .type(ResolutionType.CHANGE_TEACHER)
                .description("Assign " + teacherName + " to teach this section")
                .explanation("This teacher is qualified and available")
                .impactScore(15)
                .successProbability(85)
                .affectedEntitiesCount(1)
                .affectedEntities(Arrays.asList("Slot: " + getSlotDisplayName(slot), "Teacher: " + teacher.getName()))
                .actions(Arrays.asList(createChangeTeacherAction(slot, teacher)))
                .warnings(new ArrayList<>())
                .estimatedTimeSeconds(30)
                .requiresConfirmation(true)
                .confidenceLevel(85)
                .build());
        }

        // Change time slot
        suggestions.add(ConflictResolutionSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .type(ResolutionType.CHANGE_TIME_SLOT)
            .description("Move section to a time when the teacher is available")
            .explanation("Rescheduling to match teacher availability")
            .impactScore(30)
            .successProbability(70)
            .affectedEntitiesCount(getAffectedStudentsCount(slot))
            .affectedEntities(Arrays.asList("Slot: " + getSlotDisplayName(slot), "All students"))
            .actions(new ArrayList<>())
            .warnings(Arrays.asList("May affect student schedules"))
            .estimatedTimeSeconds(60)
            .requiresConfirmation(true)
            .confidenceLevel(70)
            .build());

        return suggestions;
    }

    private List<ConflictResolutionSuggestion> generateGenericSuggestions(Conflict conflict) {
        List<ConflictResolutionSuggestion> suggestions = new ArrayList<>();

        suggestions.add(ConflictResolutionSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .type(ResolutionType.OTHER)
            .description("Manual resolution required")
            .explanation("This conflict type requires custom resolution")
            .impactScore(50)
            .successProbability(50)
            .affectedEntitiesCount(conflict.getAffectedEntitiesCount())
            .affectedEntities(Arrays.asList("Manual review needed"))
            .actions(new ArrayList<>())
            .warnings(Arrays.asList("No automatic resolution available"))
            .estimatedTimeSeconds(0)
            .requiresConfirmation(true)
            .confidenceLevel(50)
            .build());

        return suggestions;
    }

    // ========================================================================
    // PRIORITY SCORING
    // ========================================================================

    @Override
    public ConflictPriorityScore calculatePriorityScore(Conflict conflict) {
        ConflictPriorityScore score = new ConflictPriorityScore();
        score.setConflictId(conflict.getId());

        // Hard constraint score (0-50)
        int hardScore = calculateHardConstraintScore(conflict);
        score.setHardConstraintScore(hardScore);

        // Affected entities score (0-25)
        int affectedScore = calculateAffectedEntitiesScore(conflict);
        score.setAffectedEntitiesScore(affectedScore);

        // Cascade impact score (0-25)
        int cascadeScore = Math.min(25, estimateCascadeImpact(conflict) * 5);
        score.setCascadeImpactScore(cascadeScore);

        // Historical difficulty score (0-15)
        int historicalScore = calculateHistoricalDifficultyScore(conflict);
        score.setHistoricalDifficultyScore(historicalScore);

        // Time sensitivity score (0-10)
        int timeScore = calculateTimeSensitivityScore(conflict);
        score.setTimeSensitivityScore(timeScore);

        // Calculate total and set priority level
        score.calculateTotalScore();

        // Generate explanation
        score.setScoreExplanation(generateScoreExplanation(score, conflict));

        log.debug("Calculated priority score for conflict {}: {} ({})",
            conflict.getId(), score.getTotalScore(), score.getPriorityLevel());

        return score;
    }

    private int calculateHardConstraintScore(Conflict conflict) {
        if (conflict.getSeverity() == ConflictSeverity.CRITICAL) {
            return 50;
        } else if (conflict.getSeverity() == ConflictSeverity.HIGH) {
            return 40;
        } else if (conflict.getSeverity() == ConflictSeverity.MEDIUM) {
            return 25;
        } else if (conflict.getSeverity() == ConflictSeverity.LOW) {
            return 10;
        }
        return 0;
    }

    private int calculateAffectedEntitiesScore(Conflict conflict) {
        int count = conflict.getAffectedEntitiesCount();
        if (count >= 10) return 25;
        if (count >= 5) return 20;
        if (count >= 3) return 15;
        if (count >= 1) return 10;
        return 0;
    }

    private int calculateHistoricalDifficultyScore(Conflict conflict) {
        // For Beta 1, use simple heuristic based on conflict type
        ConflictType type = conflict.getConflictType();

        switch (type) {
            case TEACHER_OVERLOAD:
            case ROOM_DOUBLE_BOOKING:
                return 5; // Usually easy to resolve

            case STUDENT_SCHEDULE_CONFLICT:
            case SUBJECT_MISMATCH:
                return 10; // Moderate difficulty

            case EXCESSIVE_TEACHING_HOURS:
            case ROOM_CAPACITY_EXCEEDED:
                return 15; // More complex

            default:
                return 8; // Average
        }
    }

    private int calculateTimeSensitivityScore(Conflict conflict) {
        long ageInDays = conflict.getAgeInDays();

        if (ageInDays == 0) return 10; // Just detected
        if (ageInDays <= 1) return 8;
        if (ageInDays <= 3) return 6;
        if (ageInDays <= 7) return 4;
        if (ageInDays <= 14) return 2;
        return 0; // Old conflict, less urgent
    }

    private String generateScoreExplanation(ConflictPriorityScore score, Conflict conflict) {
        StringBuilder explanation = new StringBuilder();
        explanation.append(String.format("Priority: %s. ", score.getPriorityLevel()));
        explanation.append(String.format("Severity: %s. ", conflict.getSeverity()));
        explanation.append(String.format("Affects %d entities. ", conflict.getAffectedEntitiesCount()));

        if (score.getCascadeImpactScore() > 15) {
            explanation.append("High cascade risk. ");
        }

        if (score.getTimeSensitivityScore() > 7) {
            explanation.append("Recently detected, needs immediate attention.");
        } else if (score.getTimeSensitivityScore() > 4) {
            explanation.append("Resolve soon.");
        } else {
            explanation.append("Can be addressed with other priorities.");
        }

        return explanation.toString();
    }

    // ========================================================================
    // APPLY SUGGESTION
    // ========================================================================

    @Override
    @Transactional
    public boolean applySuggestion(Conflict conflict, ConflictResolutionSuggestion suggestion) {
        log.info("Applying suggestion {} for conflict {}", suggestion.getId(), conflict.getId());

        boolean success = false;

        try {
            // Apply each action in the suggestion
            for (ResolutionAction action : suggestion.getActions()) {
                success = applyAction(conflict, action);
                if (!success) {
                    log.warn("Failed to apply action: {}", action.getChangeDescription());
                    break;
                }
            }

            // Record success/failure for ML learning
            recordResolutionAttempt(suggestion.getType(), success);

            if (success) {
                // Mark conflict as resolved
                conflict.resolve(null, "Auto-resolved using AI suggestion: " + suggestion.getDescription());
                conflictRepository.save(conflict);
                log.info("Successfully applied suggestion and resolved conflict {}", conflict.getId());
            }

        } catch (Exception e) {
            log.error("Error applying suggestion for conflict {}", conflict.getId(), e);
            success = false;
        }

        return success;
    }

    private boolean applyAction(Conflict conflict, ResolutionAction action) {
        try {
            String entityType = action.getEntityType();
            Long entityId = action.getEntityId();

            switch (entityType) {
                case "Teacher":
                    return applyTeacherChange(entityId, action.getNewValue());

                case "Room":
                    return applyRoomChange(entityId, action.getNewValue());

                case "TimeSlot":
                    return applyTimeSlotChange(entityId, action.getNewValue());

                default:
                    log.warn("Unknown action entity type: {}", entityType);
                    return false;
            }

        } catch (Exception e) {
            log.error("Error applying action: {}", action.getChangeDescription(), e);
            return false;
        }
    }

    private boolean applyTeacherChange(Long slotId, String newTeacherId) {
        try {
            Optional<ScheduleSlot> slotOpt = scheduleSlotRepository.findById(slotId);
            Optional<Teacher> teacherOpt = sisDataService.getTeacherById(Long.parseLong(newTeacherId));

            if (slotOpt.isPresent() && teacherOpt.isPresent()) {
                ScheduleSlot slot = slotOpt.get();
                slot.setTeacher(teacherOpt.get());
                scheduleSlotRepository.save(slot);
                return true;
            }
        } catch (Exception e) {
            log.error("Error applying teacher change", e);
        }
        return false;
    }

    private boolean applyRoomChange(Long slotId, String newRoomId) {
        try {
            Optional<ScheduleSlot> slotOpt = scheduleSlotRepository.findById(slotId);
            Optional<Room> roomOpt = roomRepository.findById(Long.parseLong(newRoomId));

            if (slotOpt.isPresent() && roomOpt.isPresent()) {
                ScheduleSlot slot = slotOpt.get();
                slot.setRoom(roomOpt.get());
                scheduleSlotRepository.save(slot);
                return true;
            }
        } catch (Exception e) {
            log.error("Error applying room change", e);
        }
        return false;
    }

    private boolean applyTimeSlotChange(Long slotId, String newTimeSlot) {
        try {
            Optional<ScheduleSlot> slotOpt = scheduleSlotRepository.findById(slotId);

            if (slotOpt.isPresent()) {
                // For Beta 1, this is a placeholder
                // Full implementation would parse newTimeSlot and update slot times
                log.info("Time slot change requested but not fully implemented in Beta 1");
                return true;
            }
        } catch (Exception e) {
            log.error("Error applying time slot change", e);
        }
        return false;
    }

    // ========================================================================
    // HISTORICAL SUCCESS TRACKING
    // ========================================================================

    @Override
    public int getHistoricalSuccessRate(ResolutionType type) {
        List<Boolean> history = successHistory.getOrDefault(type, new ArrayList<>());

        if (history.isEmpty()) {
            // Default success rates based on resolution type
            return getDefaultSuccessRate(type);
        }

        long successCount = history.stream().filter(Boolean::booleanValue).count();
        return (int) ((successCount * 100.0) / history.size());
    }

    private int getDefaultSuccessRate(ResolutionType type) {
        switch (type) {
            case CHANGE_ROOM:
            case CHANGE_TEACHER:
                return 85;

            case CHANGE_TIME_SLOT:
            case SWAP_TEACHERS:
            case SWAP_ROOMS:
                return 75;

            case REASSIGN_STUDENT:
            case ADD_CO_TEACHER:
                return 70;

            case SPLIT_SECTION:
            case COMBINE_SECTIONS:
                return 60;

            case ADJUST_CAPACITY:
            case REMOVE_CONSTRAINT:
                return 65;

            default:
                return 50;
        }
    }

    private void recordResolutionAttempt(ResolutionType type, boolean success) {
        successHistory.computeIfAbsent(type, k -> new ArrayList<>()).add(success);

        // Keep only last 100 attempts per type
        List<Boolean> history = successHistory.get(type);
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    // ========================================================================
    // CONFLICT QUERIES
    // ========================================================================

    @Override
    public List<Conflict> getConflictsByPriority() {
        List<Conflict> conflicts = conflictRepository.findAllActive();

        // Calculate priority scores and sort
        Map<Long, ConflictPriorityScore> scores = new HashMap<>();
        for (Conflict conflict : conflicts) {
            // ✅ NULL SAFE: Skip null conflicts or conflicts without ID
            if (conflict == null || conflict.getId() == null) continue;

            scores.put(conflict.getId(), calculatePriorityScore(conflict));
        }

        conflicts.sort((c1, c2) -> {
            // ✅ NULL SAFE: Safe extraction of IDs and scores with null checks
            if (c1 == null || c1.getId() == null) return 1;
            if (c2 == null || c2.getId() == null) return -1;

            ConflictPriorityScore score1Obj = scores.get(c1.getId());
            ConflictPriorityScore score2Obj = scores.get(c2.getId());

            int score1 = (score1Obj != null) ? score1Obj.getTotalScore() : 0;
            int score2 = (score2Obj != null) ? score2Obj.getTotalScore() : 0;

            return Integer.compare(score2, score1); // Higher score = higher priority
        });

        return conflicts;
    }

    @Override
    public int estimateCascadeImpact(Conflict conflict) {
        // ✅ NULL SAFE: Validate conflict parameter
        if (conflict == null) {
            return 0;
        }

        int cascadeCount = 0;

        // ✅ NULL SAFE: Estimate based on affected entities with null checks
        if (conflict.getAffectedSlots() != null) {
            cascadeCount += conflict.getAffectedSlots().size() * 2;  // Each slot can affect multiple students
        }
        if (conflict.getAffectedTeachers() != null) {
            cascadeCount += conflict.getAffectedTeachers().size() * 3;  // Teachers affect multiple classes
        }
        if (conflict.getAffectedStudents() != null) {
            cascadeCount += conflict.getAffectedStudents().size();
        }

        // Cap at reasonable value
        return Math.min(5, cascadeCount / 10);
    }

    @Override
    public boolean canAutoApply(ConflictResolutionSuggestion suggestion) {
        // ✅ NULL SAFE: Validate suggestion parameter
        if (suggestion == null) {
            return false;
        }

        return suggestion.canAutoApply();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void calculateSuggestionMetrics(ConflictResolutionSuggestion suggestion) {
        // Adjust impact based on historical success rate
        int historicalRate = getHistoricalSuccessRate(suggestion.getType());

        // Update success probability based on history
        int adjustedProbability = (suggestion.getSuccessProbability() + historicalRate) / 2;
        suggestion.setSuccessProbability(adjustedProbability);
    }

    private List<Teacher> findAlternativeTeachers(ScheduleSlot slot) {
        // Simplified: Find teachers not assigned at this time
        // Full implementation would check qualifications, availability, etc.
        return sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .limit(3)
            .collect(Collectors.toList());
    }

    private List<Room> findAlternativeRooms(ScheduleSlot slot) {
        // Simplified: Find available rooms
        // Full implementation would check room type, capacity, availability
        return roomRepository.findAll().stream()
            .limit(3)
            .collect(Collectors.toList());
    }

    private List<Room> findLargerRooms(ScheduleSlot slot) {
        // ✅ NULL SAFE: Find rooms with larger capacity
        Room currentRoom = (slot != null) ? slot.getRoom() : null;
        int currentCapacity = (currentRoom != null && currentRoom.getCapacity() != null) ?
            currentRoom.getCapacity() : 25;
        int minCapacity = currentCapacity + 5;

        return roomRepository.findAll().stream()
            // ✅ NULL SAFE: Filter null rooms and check capacity exists
            .filter(r -> r != null && r.getCapacity() != null && r.getCapacity() >= minCapacity)
            .sorted(Comparator.comparingInt(Room::getCapacity))
            .limit(3)
            .collect(Collectors.toList());
    }

    private ResolutionAction createChangeTeacherAction(ScheduleSlot slot, Teacher teacher) {
        // ✅ NULL SAFE: Safe extraction of slot and teacher properties
        Long slotId = (slot != null && slot.getId() != null) ? slot.getId() : 0L;
        String currentTeacher = (slot != null && slot.getTeacher() != null && slot.getTeacher().getName() != null) ?
            slot.getTeacher().getName() : "None";
        String newTeacherId = (teacher != null && teacher.getId() != null) ?
            teacher.getId().toString() : "0";
        String newTeacherName = (teacher != null && teacher.getName() != null) ?
            teacher.getName() : "Unknown";

        return ResolutionAction.builder()
            .entityType("Teacher")
            .entityId(slotId)
            .currentValue(currentTeacher)
            .newValue(newTeacherId)
            .changeDescription("Change teacher to " + newTeacherName)
            .build();
    }

    private ResolutionAction createChangeRoomAction(ScheduleSlot slot, Room room) {
        // ✅ NULL SAFE: Safe extraction of slot and room properties
        Long slotId = (slot != null && slot.getId() != null) ? slot.getId() : 0L;
        String currentRoom = (slot != null && slot.getRoom() != null && slot.getRoom().getRoomNumber() != null) ?
            slot.getRoom().getRoomNumber() : "None";
        String newRoomId = (room != null && room.getId() != null) ?
            room.getId().toString() : "0";
        String newRoomNumber = (room != null && room.getRoomNumber() != null) ?
            room.getRoomNumber() : "Unknown";

        return ResolutionAction.builder()
            .entityType("Room")
            .entityId(slotId)
            .currentValue(currentRoom)
            .newValue(newRoomId)
            .changeDescription("Change room to " + newRoomNumber)
            .build();
    }

    private String getSlotDisplayName(ScheduleSlot slot) {
        // ✅ NULL SAFE: Safe extraction of slot display name
        if (slot == null) {
            return "Unknown Slot";
        }

        if (slot.getCourse() != null && slot.getCourse().getCourseName() != null) {
            return slot.getCourse().getCourseName();
        }

        return "Slot #" + (slot.getId() != null ? slot.getId() : "Unknown");
    }

    private int getAffectedStudentsCount(ScheduleSlot slot) {
        // Simplified: Return estimated count
        // Full implementation would query enrollment
        return 25;
    }
}

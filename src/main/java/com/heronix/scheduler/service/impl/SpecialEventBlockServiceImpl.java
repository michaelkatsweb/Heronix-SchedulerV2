package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.SpecialEventBlock;

import com.heronix.scheduler.repository.SpecialEventBlockRepository;
import com.heronix.scheduler.service.SpecialEventBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.heronix.scheduler.model.domain.SpecialEventBlock.EventBlockType;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Special Event Block Service Implementation
 * Location:
 * src/main/java/com/eduscheduler/service/impl/SpecialEventBlockServiceImpl.java
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecialEventBlockServiceImpl implements SpecialEventBlockService {

    private final SpecialEventBlockRepository eventBlockRepository;

    @Override
    @Transactional
    public SpecialEventBlock createEventBlock(SpecialEventBlock eventBlock) {
        // ✅ NULL SAFE: Validate eventBlock parameter
        if (eventBlock == null) {
            throw new IllegalArgumentException("Event block cannot be null");
        }

        // Calculate duration if not set
        if (eventBlock.getDurationMinutes() == null &&
                eventBlock.getStartTime() != null &&
                eventBlock.getEndTime() != null) {

            Duration duration = Duration.between(
                    eventBlock.getStartTime(),
                    eventBlock.getEndTime());
            eventBlock.setDurationMinutes((int) duration.toMinutes());
        }

        SpecialEventBlock saved = eventBlockRepository.save(eventBlock);

        // ✅ NULL SAFE: Safe extraction of event properties with defaults
        String blockType = (saved.getBlockType() != null) ? saved.getBlockType().toString() : "Unknown";
        String dayOfWeek = (saved.getDayOfWeek() != null) ? saved.getDayOfWeek().toString() : "Unknown";
        String startTime = (saved.getStartTime() != null) ? saved.getStartTime().toString() : "Unknown";
        String endTime = (saved.getEndTime() != null) ? saved.getEndTime().toString() : "Unknown";

        log.info("Created event block: {} on {} ({}-{})",
                blockType, dayOfWeek, startTime, endTime);

        return saved;
    }

    @Override
    @Transactional
    public SpecialEventBlock updateEventBlock(Long id, SpecialEventBlock eventBlock) {
        // ✅ NULL SAFE: Validate id and eventBlock parameters
        if (id == null) {
            throw new IllegalArgumentException("Event block ID cannot be null");
        }
        if (eventBlock == null) {
            throw new IllegalArgumentException("Event block cannot be null");
        }

        SpecialEventBlock existing = eventBlockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event block not found: " + id));

        existing.setBlockType(eventBlock.getBlockType());
        existing.setDayOfWeek(eventBlock.getDayOfWeek());
        existing.setStartTime(eventBlock.getStartTime());
        existing.setEndTime(eventBlock.getEndTime());
        existing.setDescription(eventBlock.getDescription());
        existing.setBlocksTeaching(eventBlock.isBlocksTeaching());
        existing.setActive(eventBlock.isActive());

        // Recalculate duration
        if (existing.getStartTime() != null && existing.getEndTime() != null) {
            Duration duration = Duration.between(existing.getStartTime(), existing.getEndTime());
            existing.setDurationMinutes((int) duration.toMinutes());
        }

        SpecialEventBlock updated = eventBlockRepository.save(existing);
        log.info("Updated event block: {}", id);

        return updated;
    }

    @Override
    @Transactional
    public void deleteEventBlock(Long id) {
        // ✅ NULL SAFE: Validate id parameter
        if (id == null) {
            throw new IllegalArgumentException("Event block ID cannot be null");
        }

        eventBlockRepository.deleteById(id);
        log.info("Deleted event block: {}", id);
    }

    @Override
    public List<SpecialEventBlock> getAllActiveBlocks() {
        return eventBlockRepository.findByActiveTrue();
    }

    @Override
    public List<SpecialEventBlock> getBlocksByType(EventBlockType type) {
        // ✅ NULL SAFE: Validate type parameter
        if (type == null) {
            throw new IllegalArgumentException("Event block type cannot be null");
        }

        return eventBlockRepository.findByBlockType(type);
    }

    @Override
    public List<SpecialEventBlock> getBlocksByDay(DayOfWeek day) {
        // ✅ NULL SAFE: Validate day parameter
        if (day == null) {
            throw new IllegalArgumentException("Day of week cannot be null");
        }

        return eventBlockRepository.findByDayOfWeek(day);
    }
}

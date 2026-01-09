package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.SpecialEventBlock;
import com.heronix.scheduler.model.domain.SpecialEventBlock.EventBlockType;
import java.time.DayOfWeek;
import java.util.List;

/**
 * Special Event Block Service
 * Location:
 * src/main/java/com/eduscheduler/service/SpecialEventBlockService.java
 */
public interface SpecialEventBlockService {

    SpecialEventBlock createEventBlock(SpecialEventBlock eventBlock);

    SpecialEventBlock updateEventBlock(Long id, SpecialEventBlock eventBlock);

    void deleteEventBlock(Long id);

    List<SpecialEventBlock> getAllActiveBlocks();

    List<SpecialEventBlock> getBlocksByType(EventBlockType type);

    List<SpecialEventBlock> getBlocksByDay(DayOfWeek day);
}

package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.ScheduleOverride;
import com.heronix.scheduler.model.domain.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleOverrideRepository extends JpaRepository<ScheduleOverride, Long> {

    List<ScheduleOverride> findByScheduleSlot(ScheduleSlot scheduleSlot);

    List<ScheduleOverride> findByChangedBy(String changedBy);

    List<ScheduleOverride> findByChangedAtBetween(LocalDateTime start, LocalDateTime end);

    List<ScheduleOverride> findByOverrideType(String overrideType);

    List<ScheduleOverride> findByScheduleSlotOrderByChangedAtDesc(ScheduleSlot scheduleSlot);
}

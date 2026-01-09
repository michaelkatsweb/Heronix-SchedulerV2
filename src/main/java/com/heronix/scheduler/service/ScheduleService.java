package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Schedule;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for Schedule operations
 * Location: src/main/java/com/eduscheduler/service/ScheduleService.java
 */
public interface ScheduleService {
    
    List<Schedule> getAllSchedules();
    
    Optional<Schedule> getScheduleById(Long id);
    
    Schedule saveSchedule(Schedule schedule);
    
    void deleteSchedule(Long id);
    
    List<Schedule> getActiveSchedules();
}
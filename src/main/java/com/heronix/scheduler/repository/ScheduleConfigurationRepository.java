package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.ScheduleConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ScheduleConfigurationRepository extends JpaRepository<ScheduleConfiguration, Long> {
    Optional<ScheduleConfiguration> findByActiveTrue();
    Optional<ScheduleConfiguration> findByName(String name);
}

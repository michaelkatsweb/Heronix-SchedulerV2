package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.SchedulerConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchedulerConfigurationRepository extends JpaRepository<SchedulerConfiguration, Long> {

    Optional<SchedulerConfiguration> findByActiveTrue();

    Optional<SchedulerConfiguration> findByName(String name);

    List<SchedulerConfiguration> findByActiveTrueOrderByNameAsc();
}

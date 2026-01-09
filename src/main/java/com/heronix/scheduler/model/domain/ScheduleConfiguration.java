package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType = ScheduleType.STANDARD;

    @Column(name = "periods_per_day")
    private Integer periodsPerDay = 7;

    @Column(name = "uses_alternating_days")
    private Boolean usesAlternatingDays = false;

    @Column(name = "lunch_duration_minutes")
    private Integer lunchDurationMinutes = 30;

    @Column(name = "number_of_lunch_periods")
    private Integer numberOfLunchPeriods = 3;

    @Column(name = "default_period_duration")
    private Integer defaultPeriodDuration = 50;

    @Column(name = "block_period_duration")
    private Integer blockPeriodDuration = 90;

    @Column(name = "default_max_students")
    private Integer defaultMaxStudents = 30;

    @Column(name = "school_start_time")
    private LocalTime schoolStartTime = LocalTime.of(8, 0);

    @Column(name = "school_end_time")
    private LocalTime schoolEndTime = LocalTime.of(15, 30);

    @Column(nullable = false)
    private Boolean active = true;

    public enum ScheduleType {
        STANDARD,
        BLOCK,
        HYBRID
    }
}

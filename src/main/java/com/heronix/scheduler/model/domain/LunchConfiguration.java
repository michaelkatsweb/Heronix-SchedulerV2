package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalTime;

@Entity
@Table(name = "lunch_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LunchConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 30;

    @Column(name = "number_of_lunch_periods", nullable = false)
    private Integer numberOfLunchPeriods = 3;

    @Column(name = "earliest_lunch_time")
    private LocalTime earliestLunchTime = LocalTime.of(11, 0);

    @Column(name = "latest_lunch_time")
    private LocalTime latestLunchTime = LocalTime.of(13, 30);

    @Column(name = "max_students_per_period")
    private Integer maxStudentsPerPeriod = 300;

    @Column(name = "stagger_by_grade", nullable = false)
    private Boolean staggerByGrade = false;

    @Column(name = "allow_teacher_preference", nullable = false)
    private Boolean allowTeacherPreference = true;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @ManyToOne
    @JoinColumn(name = "schedule_configuration_id")
    private ScheduleConfiguration scheduleConfiguration;
}

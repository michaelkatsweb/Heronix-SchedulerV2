package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Co-Teacher Entity (Scheduler Shadow Copy)
 *
 * Minimal representation for scheduling purposes only.
 * Authoritative data lives in SIS microservice.
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "co_teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 100)
    private String role;  // SPECIAL_ED, INCLUSION, TEAM_TEACHER, etc.
}

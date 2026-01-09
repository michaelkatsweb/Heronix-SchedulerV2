package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * User Entity (Scheduler Shadow Copy)
 *
 * Minimal representation for scheduling purposes only.
 * Authoritative data lives in SIS microservice.
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(length = 100)
    private String email;

    @Column(name = "full_name", length = 150)
    private String fullName;
}

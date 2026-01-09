package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Academic Year Entity
 *
 * Represents an academic year in the school system
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "academic_years")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String yearName;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column
    private LocalDate graduationDate;

    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false)
    private boolean graduated = false;

    @Column
    private LocalDate progressionDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public AcademicYear(String yearName, LocalDate startDate, LocalDate endDate) {
        this.yearName = yearName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = false;
        this.graduated = false;
    }
}

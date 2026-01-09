package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "conflict_matrix",
       uniqueConstraints = @UniqueConstraint(columnNames = {"course1_id", "course2_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConflictMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course1_id", nullable = false)
    private Course course1;

    @ManyToOne
    @JoinColumn(name = "course2_id", nullable = false)
    private Course course2;

    @Column(name = "conflict_count", nullable = false)
    private Integer conflictCount = 0;

    @Column(name = "conflict_percentage")
    private Double conflictPercentage;

    @Column(name = "is_singleton_conflict")
    private Boolean isSingletonConflict = false;

    @Column(name = "priority_level")
    private Integer priorityLevel = 1;

    @Column(name = "schedule_year")
    private Integer scheduleYear;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}

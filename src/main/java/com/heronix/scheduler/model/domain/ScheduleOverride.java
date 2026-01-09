package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_overrides")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "schedule_slot_id", nullable = false)
    private ScheduleSlot scheduleSlot;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "old_teacher_id")
    private Teacher oldTeacher;

    @ManyToOne
    @JoinColumn(name = "new_teacher_id")
    private Teacher newTeacher;

    @ManyToOne
    @JoinColumn(name = "old_room_id")
    private Room oldRoom;

    @ManyToOne
    @JoinColumn(name = "new_room_id")
    private Room newRoom;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "override_type")
    private String overrideType; // TEACHER, ROOM, TIME, PIN, UNPIN
}

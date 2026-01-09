package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.EventBlockType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Special Event Time Blocks (IEP, 504, Planning, Meetings)
 * Location: src/main/java/com/eduscheduler/model/domain/SpecialEventBlock.java
 */
@Entity
@Table(name = "special_event_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecialEventBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "block_type")
    private EventBlockType blockType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(nullable = false, name = "start_time")
    private LocalTime startTime;

    @Column(nullable = false, name = "end_time")
    private LocalTime endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(length = 1000)
    private String description;

    @Column(name = "blocks_teaching", nullable = false)
    private boolean blocksTeaching = true;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Inner Enum for Event Block Types
     */
    public enum EventBlockType {
        IEP_MEETING("IEP Meeting"),
        SECTION_504_MEETING("504 Meeting"),
        TEACHER_PLANNING("Teacher Planning"),
        DEPARTMENT_MEETING("Department Meeting"),
        PARENT_CONFERENCE("Parent Conference"),
        STAFF_DEVELOPMENT("Staff Development"),
        RECURRING_WEEKLY("Recurring Weekly"),
        RECURRING_DAILY("Recurring Daily"),
        ONE_TIME("One-Time Event");

        private final String displayName;

        EventBlockType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
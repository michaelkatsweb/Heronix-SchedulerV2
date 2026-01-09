package com.heronix.scheduler.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Unavailable Time Block DTO
 *
 * Represents a time block when a teacher is unavailable
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnavailableTimeBlock {

    private DayOfWeek dayOfWeek;

    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    private String reason;
    private boolean recurring = true;

    public boolean contains(DayOfWeek day, LocalTime time) {
        if (this.dayOfWeek != day) {
            return false;
        }
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    public boolean overlaps(UnavailableTimeBlock other) {
        if (this.dayOfWeek != other.dayOfWeek) {
            return false;
        }
        return this.startTime.isBefore(other.endTime) &&
               this.endTime.isAfter(other.startTime);
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(dayOfWeek.toString().substring(0, 1))
          .append(dayOfWeek.toString().substring(1).toLowerCase())
          .append(" ")
          .append(startTime.toString())
          .append("-")
          .append(endTime.toString());

        if (reason != null && !reason.isEmpty()) {
            sb.append(" (").append(reason).append(")");
        }

        return sb.toString();
    }

    public boolean validate() {
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Day of week cannot be null");
        }

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                String.format("Start time (%s) must be before end time (%s)",
                    startTime, endTime));
        }

        return true;
    }

    @Override
    public String toString() {
        return getDisplayString();
    }
}

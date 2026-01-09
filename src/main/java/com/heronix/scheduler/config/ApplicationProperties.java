package com.heronix.scheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application Configuration Properties
 * Location: src/main/java/com/heronix/config/ApplicationProperties.java
 *
 * Maps all custom heronix.* properties from application.properties
 * to type-safe Java configuration objects.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-02
 */
@Data
@Component
@ConfigurationProperties(prefix = "heronix")
public class ApplicationProperties {

    private Schedule schedule = new Schedule();
    private Import importSettings = new Import();
    private Conflict conflict = new Conflict();
    private Export export = new Export();
    private Duty duty = new Duty();
    private Print print = new Print();

    @Data
    public static class Schedule {
        private int defaultClassDuration = 50;
        private int defaultBreakDuration = 5;
        private int defaultLunchDuration = 30;
        private String defaultStartTime = "08:00";
        private String defaultEndTime = "15:00";
    }

    @Data
    public static class Import {
        private int maxBatchSize = 1000;
        private boolean lenientMode = true;
        private boolean autoGenerateIds = true;
    }

    @Data
    public static class Conflict {
        private boolean checkTeacherConflicts = true;
        private boolean checkRoomConflicts = true;
        private boolean checkStudentConflicts = true;
        private int maxTeacherConsecutiveHours = 5;
    }

    @Data
    public static class Export {
        private String directory;
    }

    @Data
    public static class Duty {
        private TimeWindow am = new TimeWindow();
        private TimeWindow pm = new TimeWindow();

        @Data
        public static class TimeWindow {
            private String start;
            private String end;
        }
    }

    @Data
    public static class Print {
        private DefaultSettings defaultSettings = new DefaultSettings();

        @Data
        public static class DefaultSettings {
            private String orientation = "LANDSCAPE";
            private String paperSize = "LETTER";

            // Alias for 'default' which is a reserved keyword
            public String getOrientation() {
                return orientation;
            }

            public String getPaperSize() {
                return paperSize;
            }
        }
    }
}

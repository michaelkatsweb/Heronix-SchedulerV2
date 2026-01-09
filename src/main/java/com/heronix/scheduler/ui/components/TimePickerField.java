package com.heronix.scheduler.ui.components;

import javafx.scene.control.TextField;
import java.time.LocalTime;

/**
 * Time Picker Field Component
 * Custom text field for time input
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-22
 */
public class TimePickerField extends TextField {

    private LocalTime time;

    public TimePickerField() {
        super();
        setPromptText("HH:MM");
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
        if (time != null) {
            setText(time.toString());
        }
    }
}

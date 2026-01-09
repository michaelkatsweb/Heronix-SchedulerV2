package com.heronix.scheduler.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Color Legend Component
 * Displays a legend showing subject color coding
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-22
 */
public class ColorLegend extends VBox {

    public ColorLegend() {
        setupLegend();
    }

    private void setupLegend() {
        setPadding(new Insets(10));
        setSpacing(8);
        setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1px;");

        Label title = new Label("Subject Colors");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        getChildren().add(title);

        // Basic color legend
        Map<String, String> mainColors = new LinkedHashMap<>();
        mainColors.put("Mathematics", "#4285F4");
        mainColors.put("Science", "#34A853");
        mainColors.put("English", "#FBBC05");
        mainColors.put("History", "#EA4335");
        mainColors.put("Physical Education", "#009688");
        mainColors.put("Arts", "#FF7043");
        mainColors.put("Technology", "#3F51B5");
        mainColors.put("Business", "#795548");
        mainColors.put("Lunch", "#8BC34A");
        mainColors.put("Other", "#9E9E9E");

        for (Map.Entry<String, String> entry : mainColors.entrySet()) {
            getChildren().add(createLegendItem(entry.getKey(), entry.getValue()));
        }
    }

    private HBox createLegendItem(String label, String colorHex) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);

        Rectangle colorBox = new Rectangle(16, 16);
        colorBox.setFill(Color.web(colorHex));
        colorBox.setStroke(Color.web("#cbd5e1"));

        Label labelText = new Label(label);
        labelText.setFont(Font.font("Segoe UI", 10));

        item.getChildren().addAll(colorBox, labelText);
        return item;
    }

    public static ColorLegend create() {
        return new ColorLegend();
    }
}

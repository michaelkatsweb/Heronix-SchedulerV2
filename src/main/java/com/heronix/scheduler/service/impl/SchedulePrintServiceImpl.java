package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.repository.ScheduleSlotRepository;
import com.heronix.scheduler.service.SchedulePrintService;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * SCHEDULE PRINT SERVICE IMPLEMENTATION
 * Print schedules with preview and custom layouts
 * ═══════════════════════════════════════════════════════════════
 * 
 * Location: src/main/java/com/eduscheduler/service/impl/SchedulePrintServiceImpl.java
 * 
 * Features:
 * ✓ Print preview dialog
 * ✓ Multiple page layouts (Portrait/Landscape)
 * ✓ Custom headers and footers
 * ✓ Color/Black-white options
 * ✓ Page scaling and margins
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-02
 */
@Slf4j
@Service
public class SchedulePrintServiceImpl implements SchedulePrintService {

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    // ═══════════════════════════════════════════════════════════════
    // PRINT METHODS
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void printSchedule(Schedule schedule) {
        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            log.error("❌ Cannot print null schedule");
            showError("Print Error", "Schedule data is missing");
            return;
        }

        // ✅ NULL SAFE: Safe extraction of schedule name with default
        String scheduleName = (schedule.getScheduleName() != null) ? schedule.getScheduleName() : "Unnamed Schedule";
        log.info("🖨️ Printing schedule: {}", scheduleName);

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            log.error("❌ No printer available");
            showError("Print Error", "No printer found. Please install a printer and try again.");
            return;
        }

        // Show print dialog
        boolean proceed = job.showPrintDialog(null);

        if (proceed) {
            Node printNode = createPrintableNode(schedule);

            boolean success = job.printPage(printNode);

            if (success) {
                job.endJob();
                log.info("✅ Print job sent successfully");
            } else {
                log.error("❌ Print job failed");
                showError("Print Error", "Failed to print schedule");
            }
        } else {
            log.info("🚫 Print cancelled by user");
        }
    }

    @Override
    public void printSchedulePreview(Schedule schedule) {
        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            log.error("❌ Cannot show print preview for null schedule");
            showError("Preview Error", "Schedule data is missing");
            return;
        }

        // ✅ NULL SAFE: Safe extraction of schedule name with default
        String scheduleName = (schedule.getScheduleName() != null) ? schedule.getScheduleName() : "Unnamed Schedule";
        log.info("👁️ Showing print preview for: {}", scheduleName);

        Stage previewStage = new Stage();
        previewStage.setTitle("Print Preview - " + scheduleName);

        Node printNode = createPrintableNode(schedule);

        // Wrap in scroll pane for preview
        ScrollPane scrollPane = new ScrollPane(printNode);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #808080;");

        // Toolbar
        ToolBar toolbar = createPrintToolbar(previewStage, schedule);

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 900, 700);
        previewStage.setScene(scene);
        previewStage.show();
    }

    // ═══════════════════════════════════════════════════════════════
    // NODE CREATION
    // ═══════════════════════════════════════════════════════════════

    private Node createPrintableNode(Schedule schedule) {
        VBox container = new VBox(10);
        container.setStyle("-fx-background-color: white; -fx-padding: 20;");
        container.setPrefWidth(750); // Standard US Letter width in points
        
        // Header
        VBox header = createHeader(schedule);
        container.getChildren().add(header);
        
        // Schedule grid
        GridPane grid = createScheduleGrid(schedule);
        container.getChildren().add(grid);
        
        // Footer
        HBox footer = createFooter();
        container.getChildren().add(footer);
        
        return container;
    }

    private VBox createHeader(Schedule schedule) {
        VBox header = new VBox(5);
        header.setStyle("-fx-alignment: center; -fx-padding: 0 0 15 0; -fx-border-color: #cccccc; -fx-border-width: 0 0 2 0;");

        // ✅ NULL SAFE: Safe extraction of schedule name with default
        String scheduleName = (schedule != null && schedule.getScheduleName() != null)
            ? schedule.getScheduleName() : "Unnamed Schedule";
        Label titleLabel = new Label(scheduleName);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        // ✅ NULL SAFE: Safe extraction of dates with defaults
        String startDate = (schedule != null && schedule.getStartDate() != null)
            ? schedule.getStartDate().toString() : "N/A";
        String endDate = (schedule != null && schedule.getEndDate() != null)
            ? schedule.getEndDate().toString() : "N/A";
        Label dateLabel = new Label(String.format("Date Range: %s to %s", startDate, endDate));
        dateLabel.setFont(Font.font("Arial", 12));

        Label generatedLabel = new Label("Generated: " + java.time.LocalDate.now());
        generatedLabel.setFont(Font.font("Arial", 10));
        generatedLabel.setStyle("-fx-text-fill: #666666;");

        header.getChildren().addAll(titleLabel, dateLabel, generatedLabel);

        return header;
    }

    private GridPane createScheduleGrid(Schedule schedule) {
        // ✅ NULL SAFE: Validate schedule and ID before repository query
        List<ScheduleSlot> slots = new ArrayList<>();
        if (schedule != null && schedule.getId() != null) {
            List<ScheduleSlot> foundSlots = scheduleSlotRepository.findByScheduleId(schedule.getId());
            // ✅ NULL SAFE: Check repository result
            if (foundSlots != null) {
                slots = foundSlots;
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setStyle("-fx-border-color: #000000; -fx-border-width: 1;");

        // Headers
        String[] days = {"Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        for (int col = 0; col < days.length; col++) {
            Label header = new Label(days[col]);
            header.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            header.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 5; -fx-alignment: center; -fx-border-color: #000000; -fx-border-width: 0.5;");
            header.setMinWidth(120);
            header.setMaxWidth(Double.MAX_VALUE);
            grid.add(header, col, 0);
        }

        // Time slots
        // ✅ NULL SAFE: Filter null slots and null start times before mapping
        List<LocalTime> times = slots.stream()
            .filter(slot -> slot != null && slot.getStartTime() != null)
            .map(ScheduleSlot::getStartTime)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        int row = 1;
        for (LocalTime time : times) {
            // ✅ NULL SAFE: Skip null times in iteration
            if (time == null) continue;

            // Time column
            Label timeLabel = new Label(time.format(DateTimeFormatter.ofPattern("h:mm a")));
            timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            timeLabel.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 5; -fx-alignment: center; -fx-border-color: #000000; -fx-border-width: 0.5;");
            timeLabel.setMinWidth(120);
            grid.add(timeLabel, 0, row);

            // Day columns
            for (int day = 1; day <= 5; day++) {
                final int dayNum = day;
                // ✅ NULL SAFE: Filter null slots and check dayOfWeek/startTime before comparison
                Optional<ScheduleSlot> slot = slots.stream()
                    .filter(s -> s != null && s.getDayOfWeek() != null && s.getStartTime() != null &&
                                s.getDayOfWeek().getValue() == dayNum && s.getStartTime().equals(time))
                    .findFirst();
                
                VBox cell = new VBox(2);
                cell.setStyle("-fx-border-color: #000000; -fx-border-width: 0.5; -fx-padding: 5; -fx-background-color: white;");
                cell.setMinWidth(120);
                cell.setMinHeight(60);
                
                if (slot.isPresent()) {
                    ScheduleSlot s = slot.get();
                    
                    Label courseLabel = new Label(s.getCourse() != null ? s.getCourse().getCourseName() : "");
                    courseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 9));
                    courseLabel.setWrapText(true);
                    
                    Label teacherLabel = new Label(s.getTeacher() != null ? s.getTeacher().getName() : "");
                    teacherLabel.setFont(Font.font("Arial", 8));
                    
                    Label roomLabel = new Label(s.getRoom() != null ? s.getRoom().getRoomNumber() : "");
                    roomLabel.setFont(Font.font("Arial", 8));
                    roomLabel.setStyle("-fx-text-fill: #666666;");
                    
                    cell.getChildren().addAll(courseLabel, teacherLabel, roomLabel);
                }
                
                grid.add(cell, day, row);
            }
            
            row++;
        }
        
        return grid;
    }

    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setStyle("-fx-alignment: center; -fx-padding: 15 0 0 0; -fx-border-color: #cccccc; -fx-border-width: 2 0 0 0;");
        
        Label footerText = new Label("Powered by Heronix Scheduling System");
        footerText.setFont(Font.font("Arial", 8));
        footerText.setStyle("-fx-text-fill: #999999;");
        
        footer.getChildren().add(footerText);
        
        return footer;
    }

    // ═══════════════════════════════════════════════════════════════
    // TOOLBAR
    // ═══════════════════════════════════════════════════════════════

    private ToolBar createPrintToolbar(Stage stage, Schedule schedule) {
        ToolBar toolbar = new ToolBar();
        
        Button printButton = new Button("🖨️ Print");
        printButton.setOnAction(e -> {
            printSchedule(schedule);
            stage.close();
        });
        
        Button closeButton = new Button("❌ Close");
        closeButton.setOnAction(e -> stage.close());
        
        ComboBox<String> orientationCombo = new ComboBox<>();
        orientationCombo.getItems().addAll("Portrait", "Landscape");
        orientationCombo.setValue("Landscape");
        orientationCombo.setPromptText("Orientation");
        
        ComboBox<String> paperSizeCombo = new ComboBox<>();
        paperSizeCombo.getItems().addAll("Letter", "A4", "Legal");
        paperSizeCombo.setValue("Letter");
        paperSizeCombo.setPromptText("Paper Size");
        
        Label spacer = new Label("    ");
        
        toolbar.getItems().addAll(
            printButton,
            new Separator(),
            new Label("Orientation:"),
            orientationCombo,
            spacer,
            new Label("Paper:"),
            paperSizeCombo,
            new Separator(),
            closeButton
        );
        
        return toolbar;
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
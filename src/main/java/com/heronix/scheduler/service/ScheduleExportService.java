package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Schedule;
import java.io.File;

/**
 * Schedule Export Service Interface
 * 
 * Provides methods to export schedules to various formats:
 * - PDF (with color coding and formatting)
 * - Excel (multiple sheets with statistics)
 * - CSV (simple data export)
 * - iCalendar (for calendar apps)
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-11
 */
public interface ScheduleExportService {
    
    /**
     * Export schedule to PDF format
     * 
     * @param schedule The schedule to export
     * @return File object pointing to the generated PDF
     * @throws Exception if export fails
     */
    File exportToPDF(Schedule schedule) throws Exception;
    
    /**
     * Export schedule to Excel format (XLSX)
     * 
     * @param schedule The schedule to export
     * @return File object pointing to the generated Excel file
     * @throws Exception if export fails
     */
    File exportToExcel(Schedule schedule) throws Exception;
    
    /**
     * Export schedule to CSV format
     * 
     * @param schedule The schedule to export
     * @return File object pointing to the generated CSV file
     * @throws Exception if export fails
     */
    File exportToCSV(Schedule schedule) throws Exception;
    
    /**
     * Export schedule to iCalendar format (ICS)
     * 
     * @param schedule The schedule to export
     * @return File object pointing to the generated ICS file
     * @throws Exception if export fails
     */
    File exportToICalendar(Schedule schedule) throws Exception;
    
    /**
     * Get the exports directory
     * Creates it if it doesn't exist
     * 
     * @return File object representing the exports directory
     */
    File getExportsDirectory();
}
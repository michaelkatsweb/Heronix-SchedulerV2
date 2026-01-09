// Location: src/main/java/com/eduscheduler/util/FileUtil.java
package com.heronix.scheduler.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * File Utility Methods
 * Location: src/main/java/com/eduscheduler/util/FileUtil.java
 */
public class FileUtil {

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            "pdf", "xlsx", "xls", "docx", "doc", "jpg", "jpeg", "png", "csv");

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    /**
     * Get file extension
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * Check if file type is supported
     */
    public static boolean isSupportedFileType(String filename) {
        String extension = getFileExtension(filename);
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    /**
     * Validate file size
     */
    public static boolean isValidFileSize(long fileSize) {
        return fileSize > 0 && fileSize <= MAX_FILE_SIZE;
    }

    /**
     * Generate unique filename with timestamp
     */
    public static String generateUniqueFilename(String originalFilename) {
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = getFileExtension(originalFilename);
        String baseName = originalFilename.substring(0,
                originalFilename.lastIndexOf('.'));

        return sanitizeFilename(baseName) + "_" + timestamp + "." + extension;
    }

    /**
     * Sanitize filename (remove unsafe characters)
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "file";
        }

        return filename
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_{2,}", "_")
                .substring(0, Math.min(filename.length(), 100));
    }

    /**
     * Create directory if not exists
     */
    public static void ensureDirectoryExists(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Get file MIME type
     */
    public static String getMimeType(String filename) {
        String extension = getFileExtension(filename);

        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "csv" -> "text/csv";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };
    }

    /**
     * Format file size for display
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Delete file safely
     */
    public static boolean deleteFile(String filepath) {
        try {
            File file = new File(filepath);
            return file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if file exists
     */
    public static boolean fileExists(String filepath) {
        return new File(filepath).exists();
    }

    /**
     * Get file name without extension
     */
    public static String getFileNameWithoutExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return filename;
        }

        return filename.substring(0, lastDotIndex);
    }

    /**
     * Validate file for import
     */
    public static List<String> validateFileForImport(String filename, long fileSize) {
        List<String> errors = new java.util.ArrayList<>();

        if (filename == null || filename.isEmpty()) {
            errors.add("Filename is required");
            return errors;
        }

        if (!isSupportedFileType(filename)) {
            errors.add("Unsupported file type. Supported: " +
                    String.join(", ", SUPPORTED_EXTENSIONS));
        }

        if (!isValidFileSize(fileSize)) {
            errors.add("File size must be between 1 byte and 50MB");
        }

        return errors;
    }
}
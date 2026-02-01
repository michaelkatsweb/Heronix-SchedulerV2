package com.heronix.scheduler.util;

/**
 * Authentication Context Utility
 * Provides current user information for the application.
 * Uses thread-local storage so login screens can set the current user.
 */
public class AuthenticationContext {

    private static final ThreadLocal<String> currentUsername = ThreadLocal.withInitial(() -> System.getProperty("user.name", "system"));
    private static final ThreadLocal<Long> currentUserId = ThreadLocal.withInitial(() -> 1L);

    public static String getCurrentUsername() {
        return currentUsername.get();
    }

    public static Long getCurrentUserId() {
        return currentUserId.get();
    }

    public static void setCurrentUser(String username, Long userId) {
        currentUsername.set(username);
        currentUserId.set(userId);
    }

    public static void clear() {
        currentUsername.remove();
        currentUserId.remove();
    }
}

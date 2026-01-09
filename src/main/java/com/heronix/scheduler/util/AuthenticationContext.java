package com.heronix.scheduler.util;

/**
 * Authentication Context Utility
 * Provides current user information for the application
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 */
public class AuthenticationContext {

    /**
     * Get the current username
     * TODO: Integrate with actual authentication system
     *
     * @return current username or "system" as default
     */
    public static String getCurrentUsername() {
        // TODO: Integrate with actual security/authentication system
        return "system";
    }

    /**
     * Get the current user ID
     * TODO: Integrate with actual authentication system
     *
     * @return current user ID or 1L as default
     */
    public static Long getCurrentUserId() {
        // TODO: Integrate with actual security/authentication system
        return 1L;
    }
}

package com.heronix.scheduler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.prefs.Preferences;

/**
 * District Settings Service
 * Manages district-level configuration using Java Preferences API for persistence.
 */
@Slf4j
@Service
public class DistrictSettingsService {

    private final Preferences prefs = Preferences.userRoot().node("heronix/scheduler/district");

    /**
     * Get district setting by key.
     */
    public String getSetting(String key) {
        return prefs.get(key, null);
    }

    /**
     * Get district setting by key with default value.
     */
    public String getSetting(String key, String defaultValue) {
        return prefs.get(key, defaultValue);
    }

    /**
     * Save district setting.
     */
    public void saveSetting(String key, String value) {
        prefs.put(key, value);
        log.debug("Saved district setting: {} = {}", key, value);
    }
}

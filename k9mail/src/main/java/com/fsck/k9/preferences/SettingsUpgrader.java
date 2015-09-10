package com.fsck.k9.preferences;

import java.util.Map;
import java.util.Set;

/**
 * Used for a nontrivial settings upgrade.
 *
 * @see Settings#upgrade(int, Map, Map, Map)
 */
public interface SettingsUpgrader {
    /**
     * Upgrade the provided settings.
     *
     * @param settings
     *         The settings to upgrade.  This map is modified and contains the upgraded
     *         settings when this method returns.
     *
     * @return A set of setting names that were removed during the upgrade process or
     *         {@code null} if none were removed.
     */
    public Set<String> upgrade(Map<String, Object> settings);
}

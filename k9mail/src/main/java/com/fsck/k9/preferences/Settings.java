package com.fsck.k9.preferences;

import android.util.Log;

import com.fsck.k9.K9;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/*
 * TODO:
 * - use the default values defined in GlobalSettings and AccountSettings when creating new
 *   accounts
 * - think of a better way to validate enums than to use the resource arrays (i.e. get rid of
 *   ResourceArrayValidator); maybe even use the settings description for the settings UI
 * - add unit test that validates the default values are actually valid according to the validator
 */

public class Settings {
    /**
     * Version number of global and account settings.
     *
     * <p>
     * This value is used as "version" attribute in the export file. It needs to be incremented
     * when a global or account setting is added or removed, or when the format of a setting
     * is changed (e.g. add a value to an enum).
     * </p>
     *
     * @see SettingsExporter
     */
    public static final int VERSION = 41;

    public static Map<String, Object> validate(int version, Map<String,
            TreeMap<Integer, SettingsDescription>> settings,
            Map<String, String> importedSettings, boolean useDefaultValues) {

        Map<String, Object> validatedSettings = new HashMap<String, Object>();
        for (Map.Entry<String, TreeMap<Integer, SettingsDescription>> versionedSetting :
                settings.entrySet()) {

            // Get the setting description with the highest version lower than or equal to the
            // supplied content version.
            TreeMap<Integer, SettingsDescription> versions = versionedSetting.getValue();
            SortedMap<Integer, SettingsDescription> headMap = versions.headMap(version + 1);

            // Skip this setting if it was introduced after 'version'
            if (headMap.isEmpty()) {
                continue;
            }

            Integer settingVersion = headMap.lastKey();
            SettingsDescription desc = versions.get(settingVersion);

            // Skip this setting if it is no longer used in 'version'
            if (desc == null) {
                continue;
            }

            String key = versionedSetting.getKey();

            boolean useDefaultValue;
            if (!importedSettings.containsKey(key)) {
                Log.v(K9.LOG_TAG, "Key \"" + key + "\" wasn't found in the imported file." +
                        ((useDefaultValues) ? " Using default value." : ""));
                useDefaultValue = useDefaultValues;
            } else {
                String prettyValue = importedSettings.get(key);
                try {
                    Object internalValue = desc.fromPrettyString(prettyValue);
                    validatedSettings.put(key, internalValue);
                    useDefaultValue = false;
                } catch (InvalidSettingValueException e) {
                    Log.v(K9.LOG_TAG, "Key \"" + key + "\" has invalid value \"" + prettyValue +
                            "\" in imported file. " +
                            ((useDefaultValues) ? "Using default value." : "Skipping."));
                    useDefaultValue = useDefaultValues;
                }
            }

            if (useDefaultValue) {
                Object defaultValue = desc.getDefaultValue();
                validatedSettings.put(key, defaultValue);
            }
        }

        return validatedSettings;
    }

    /**
     * Upgrade settings using the settings structure and/or special upgrade code.
     *
     * @param version
     *         The content version of the settings in {@code validatedSettings}.
     * @param upgraders
     *         A map of {@link SettingsUpgrader}s for nontrivial settings upgrades.
     * @param settings
     *         The structure describing the different settings, possibly containing multiple
     *         versions.
     * @param validatedSettings
     *         The settings as returned by {@link Settings#validate(int, Map, Map, boolean)}.
     *         This map is modified and contains the upgraded settings when this method returns.
     *
     * @return A set of setting names that were removed during the upgrade process or {@code null}
     *         if none were removed.
     */
    public static Set<String> upgrade(int version, Map<Integer, SettingsUpgrader> upgraders,
            Map<String, TreeMap<Integer, SettingsDescription>> settings,
            Map<String, Object> validatedSettings) {

        Map<String, Object> upgradedSettings = validatedSettings;
        Set<String> deletedSettings = null;

        for (int toVersion = version + 1; toVersion <= VERSION; toVersion++) {

            // Check if there's an SettingsUpgrader for that version
            SettingsUpgrader upgrader = upgraders.get(toVersion);
            if (upgrader != null) {
                deletedSettings = upgrader.upgrade(upgradedSettings);
            }

            // Deal with settings that don't need special upgrade code
            for (Entry<String, TreeMap<Integer, SettingsDescription>> versions :
                settings.entrySet()) {

                String settingName = versions.getKey();
                TreeMap<Integer, SettingsDescription> versionedSettings = versions.getValue();

                // Handle newly added settings
                if (versionedSettings.firstKey().intValue() == toVersion) {

                    // Check if it was already added to upgradedSettings by the SettingsUpgrader
                    if (!upgradedSettings.containsKey(settingName)) {
                        // Insert default value to upgradedSettings
                        SettingsDescription setting = versionedSettings.get(toVersion);
                        Object defaultValue = setting.getDefaultValue();
                        upgradedSettings.put(settingName, defaultValue);

                        if (K9.DEBUG) {
                            String prettyValue = setting.toPrettyString(defaultValue);
                            Log.v(K9.LOG_TAG, "Added new setting \"" + settingName +
                                    "\" with default value \"" + prettyValue + "\"");
                        }
                    }
                }

                // Handle removed settings
                Integer highestVersion = versionedSettings.lastKey();
                if (highestVersion.intValue() == toVersion &&
                        versionedSettings.get(highestVersion) == null) {
                    upgradedSettings.remove(settingName);
                    if (deletedSettings == null) {
                        deletedSettings = new HashSet<String>();
                    }
                    deletedSettings.add(settingName);

                    if (K9.DEBUG) {
                        Log.v(K9.LOG_TAG, "Removed setting \"" + settingName + "\"");
                    }
                }
            }
        }

        return deletedSettings;
    }

    /**
     * Convert settings from the internal representation to the string representation used in the
     * preference storage.
     *
     * @param settings
     *         The map of settings to convert.
     * @param settingDescriptions
     *         The structure containing the {@link SettingsDescription} objects that will be used
     *         to convert the setting values.
     *
     * @return The settings converted to the string representation used in the preference storage.
     */
    public static Map<String, String> convert(Map<String, Object> settings,
            Map<String, TreeMap<Integer, SettingsDescription>> settingDescriptions) {

        Map<String, String> serializedSettings = new HashMap<String, String>();

        for (Entry<String, Object> setting : settings.entrySet()) {
            String settingName = setting.getKey();
            Object internalValue = setting.getValue();

            TreeMap<Integer, SettingsDescription> versionedSetting =
                settingDescriptions.get(settingName);
            Integer highestVersion = versionedSetting.lastKey();
            SettingsDescription settingDesc = versionedSetting.get(highestVersion);

            if (settingDesc != null) {
                String stringValue = settingDesc.toString(internalValue);

                serializedSettings.put(settingName, stringValue);
            } else {
                if (K9.DEBUG) {
                    Log.w(K9.LOG_TAG, "Settings.serialize() called with a setting that should " +
                            "have been removed: " + settingName);
                }
            }
        }

        return serializedSettings;
    }

    /**
     * Creates a {@link TreeMap} linking version numbers to {@link SettingsDescription} instances.
     *
     * <p>
     * This {@code TreeMap} is used to quickly find the {@code SettingsDescription} belonging to a
     * content version as read by {@link SettingsImporter}. See e.g.
     * {@link Settings#validate(int, Map, Map, boolean)}.
     * </p>
     *
     * @param versionDescriptions
     *         A list of descriptions for a specific setting mapped to version numbers. Never
     *         {@code null}.
     *
     * @return A {@code TreeMap} using the version number as key, the {@code SettingsDescription}
     *         as value.
     */
    public static TreeMap<Integer, SettingsDescription> versions(
            V... versionDescriptions) {
        TreeMap<Integer, SettingsDescription> map = new TreeMap<Integer, SettingsDescription>();
        for (V v : versionDescriptions) {
            map.put(v.version, v.description);
        }
        return map;
    }
}

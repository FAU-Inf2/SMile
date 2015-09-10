package com.fsck.k9.preferences;

/**
 * Container to hold a {@link SettingsDescription} instance and a version number.
 *
 * @see Settings#versions(V...)
 */
public class V {
    public final Integer version;
    public final SettingsDescription description;

    public V(Integer version, SettingsDescription description) {
        this.version = version;
        this.description = description;
    }
}

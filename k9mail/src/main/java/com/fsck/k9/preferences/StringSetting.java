package com.fsck.k9.preferences;

/**
 * A string setting.
 */
public class StringSetting extends SettingsDescription {
    public StringSetting(String defaultValue) {
        super(defaultValue);
    }

    @Override
    public Object fromString(String value) {
        return value;
    }
}

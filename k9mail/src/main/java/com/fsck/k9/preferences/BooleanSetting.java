package com.fsck.k9.preferences;

/**
 * A boolean setting.
 */
public class BooleanSetting extends SettingsDescription {
    public BooleanSetting(boolean defaultValue) {
        super(defaultValue);
    }

    @Override
    public Object fromString(String value) throws InvalidSettingValueException {
        if (Boolean.TRUE.toString().equals(value)) {
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            return false;
        }
        throw new InvalidSettingValueException();
    }
}

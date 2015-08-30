package com.fsck.k9.preferences;

/**
 * A color setting.
 */
public class ColorSetting extends SettingsDescription {
    public ColorSetting(int defaultValue) {
        super(defaultValue);
    }

    @Override
    public Object fromString(String value) throws InvalidSettingValueException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidSettingValueException();
        }
    }

    @Override
    public String toPrettyString(Object value) {
        int color = ((Integer) value) & 0x00FFFFFF;
        return String.format("#%06x", color);
    }

    @Override
    public Object fromPrettyString(String value) throws InvalidSettingValueException {
        try {
            if (value.length() == 7) {
                return Integer.parseInt(value.substring(1), 16) | 0xFF000000;
            }
        } catch (NumberFormatException e) { /* do nothing */ }

        throw new InvalidSettingValueException();
    }
}

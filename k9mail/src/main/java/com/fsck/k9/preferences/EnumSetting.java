package com.fsck.k9.preferences;

/**
 * An {@code Enum} setting.
 *
 * <p>
 * {@link Enum#toString()} is used to obtain the "pretty" string representation.
 * </p>
 */
public class EnumSetting<T extends Enum<T>> extends SettingsDescription {
    private Class<T> mEnumClass;

    public EnumSetting(Class<T> enumClass, Object defaultValue) {
        super(defaultValue);
        mEnumClass = enumClass;
    }

    @Override
    public Object fromString(String value) throws InvalidSettingValueException {
        try {
            return Enum.valueOf(mEnumClass, value);
        } catch (Exception e) {
            throw new InvalidSettingValueException();
        }
    }
}

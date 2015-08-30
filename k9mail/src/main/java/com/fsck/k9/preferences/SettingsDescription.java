package com.fsck.k9.preferences;

import com.fsck.k9.K9;

/**
 * Describes a setting.
 *
 * <p>
 * Instances of this class are used to convert the string representations of setting values to
 * an internal representation (e.g. an integer) and back.
 * </p><p>
 * Currently we use two different string representations:
 * </p>
 * <ol>
 *   <li>
 *   The one that is used by the internal preference {@link Storage}. It is usually obtained by
 *   calling {@code toString()} on the internal representation of the setting value (see e.g.
 *   {@link K9#save(android.content.SharedPreferences.Editor)}).
 *   </li>
 *   <li>
 *   The "pretty" version that is used by the import/export settings file (e.g. colors are
 *   exported in #rrggbb format instead of a integer string like "-8734021").
 *   </li>
 * </ol>
 * <p>
 * <strong>Note:</strong>
 * For the future we should aim to get rid of the "internal" string representation. The
 * "pretty" version makes reading a database dump easier and the performance impact should be
 * negligible.
 * </p>
 */
public abstract class SettingsDescription {
    /**
     * The setting's default value (internal representation).
     */
    protected Object mDefaultValue;

    public SettingsDescription(Object defaultValue) {
        mDefaultValue = defaultValue;
    }

    /**
     * Get the default value.
     *
     * @return The internal representation of the default value.
     */
    public Object getDefaultValue() {
        return mDefaultValue;
    }

    /**
     * Convert a setting's value to the string representation.
     *
     * @param value
     *         The internal representation of a setting's value.
     *
     * @return The string representation of {@code value}.
     */
    public String toString(Object value) {
        return value.toString();
    }

    /**
     * Parse the string representation of a setting's value .
     *
     * @param value
     *         The string representation of a setting's value.
     *
     * @return The internal representation of the setting's value.
     *
     * @throws InvalidSettingValueException
     *         If {@code value} contains an invalid value.
     */
    public abstract Object fromString(String value) throws InvalidSettingValueException;

    /**
     * Convert a setting value to the "pretty" string representation.
     *
     * @param value
     *         The setting's value.
     *
     * @return A pretty-printed version of the setting's value.
     */
    public String toPrettyString(Object value) {
        return toString(value);
    }

    /**
     * Convert the pretty-printed version of a setting's value to the internal representation.
     *
     * @param value
     *         The pretty-printed version of the setting's value. See
     *         {@link #toPrettyString(Object)}.
     *
     * @return The internal representation of the setting's value.
     *
     * @throws InvalidSettingValueException
     *         If {@code value} contains an invalid value.
     */
    public Object fromPrettyString(String value) throws InvalidSettingValueException {
        return fromString(value);
    }
}

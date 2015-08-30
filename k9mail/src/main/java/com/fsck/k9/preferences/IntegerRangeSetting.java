package com.fsck.k9.preferences;

/**
 * An integer settings whose values a limited to a certain range.
 */
public class IntegerRangeSetting extends SettingsDescription {
    private int mStart;
    private int mEnd;

    public IntegerRangeSetting(int start, int end, int defaultValue) {
        super(defaultValue);
        mStart = start;
        mEnd = end;
    }

    @Override
    public Object fromString(String value) throws InvalidSettingValueException {
        try {
            int intValue = Integer.parseInt(value);
            if (mStart <= intValue && intValue <= mEnd) {
                return intValue;
            }
        } catch (NumberFormatException e) { /* do nothing */ }

        throw new InvalidSettingValueException();
    }
}

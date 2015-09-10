package com.fsck.k9.preferences;

import java.util.Map;

/**
 * A setting that has multiple valid values but doesn't use an {@link Enum} internally.
 *
 * @param <A>
 *         The type of the internal representation (e.g. {@code Integer}).
 */
public abstract class PseudoEnumSetting<A> extends SettingsDescription {
    public PseudoEnumSetting(Object defaultValue) {
        super(defaultValue);
    }

    protected abstract Map<A, String> getMapping();

    @Override
    public String toPrettyString(Object value) {
        return getMapping().get(value);
    }

    @Override
    public Object fromPrettyString(String value) throws InvalidSettingValueException {
        for (Map.Entry<A, String> entry : getMapping().entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }

        throw new InvalidSettingValueException();
    }
}

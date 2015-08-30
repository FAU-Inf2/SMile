package com.fsck.k9.preferences;

import com.fsck.k9.FontSizes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A font size setting.
 */
public class FontSizeSetting extends PseudoEnumSetting<Integer> {
    private final Map<Integer, String> mMapping;

    public FontSizeSetting(int defaultValue) {
        super(defaultValue);

        Map<Integer, String> mapping = new HashMap<Integer, String>();
        mapping.put(FontSizes.FONT_10SP, "tiniest");
        mapping.put(FontSizes.FONT_12SP, "tiny");
        mapping.put(FontSizes.SMALL, "smaller");
        mapping.put(FontSizes.FONT_16SP, "small");
        mapping.put(FontSizes.MEDIUM, "medium");
        mapping.put(FontSizes.FONT_20SP, "large");
        mapping.put(FontSizes.LARGE, "larger");
        mMapping = Collections.unmodifiableMap(mapping);
    }

    @Override
    protected Map<Integer, String> getMapping() {
        return mMapping;
    }

    @Override
    public Object fromString(String value) throws InvalidSettingValueException {
        try {
            Integer fontSize = Integer.parseInt(value);
            if (mMapping.containsKey(fontSize)) {
                return fontSize;
            }
        } catch (NumberFormatException e) { /* do nothing */ }

        throw new InvalidSettingValueException();
    }
}

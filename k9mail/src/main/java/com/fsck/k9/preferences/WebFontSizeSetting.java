package com.fsck.k9.preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link android.webkit.WebView} font size setting.
 */
public class WebFontSizeSetting extends PseudoEnumSetting<Integer> {
    private final Map<Integer, String> mMapping;

    public WebFontSizeSetting(int defaultValue) {
        super(defaultValue);

        Map<Integer, String> mapping = new HashMap<Integer, String>();
        mapping.put(1, "smallest");
        mapping.put(2, "smaller");
        mapping.put(3, "normal");
        mapping.put(4, "larger");
        mapping.put(5, "largest");
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

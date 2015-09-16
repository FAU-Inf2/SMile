package com.fsck.k9.preferences;

import android.content.Intent;
import android.graphics.drawable.Drawable;

/*
Taken from https://github.com/open-keychain/openpgp-api/blob/master/openpgp-api/src/main/java/org/openintents/openpgp/util/OpenPgpAppPreference.java
 */

public class AppEntry {
    private String packageName;
    private String simpleName;
    private Drawable icon;
    private Intent intent;

    public AppEntry(String packageName, String simpleName, Drawable icon, Intent intent) {
        this.packageName = packageName;
        this.simpleName = simpleName;
        this.icon = icon;
        this.intent = intent;
    }

    public AppEntry(String packageName, String simpleName, Drawable icon) {
        this(packageName, simpleName, icon, null);
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public Intent getIntent() {
        return intent;
    }

    @Override
    public String toString() {
        return simpleName;
    }
}

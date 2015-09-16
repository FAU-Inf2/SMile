package com.fsck.k9.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.util.AttributeSet;

import com.fsck.k9.fragment.SmileDialogPreference;

import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

/*

 */
public class AppPreference extends DialogPreference implements SmileDialogPreference {
    private static final String MARKET_INTENT_URI_BASE = "market://details?id=%s";

    private static final ArrayList<String> PROVIDER_BLACKLIST = new ArrayList<String>();

    static {
        // Unfortunately, the current released version of APG includes a broken version of the API
        PROVIDER_BLACKLIST.add("org.thialfihar.android.apg");
    }

    private List<AppEntry> entries;
    private String serviceIntentName;
    private String packageName;
    private String simpleName;
    private String selectedPackage;
    private Intent marketIntent;
    private Drawable noneIcon;

    public AppPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.AppPreference,
                0, 0);

        try {
            final String packageName = a.getString(R.styleable.AppPreference_packageName);
            final String serviceIntentName = a.getString(R.styleable.AppPreference_serviceIntentName);
            noneIcon = a.getDrawable(R.styleable.AppPreference_noneIcon);
            simpleName = a.getString(R.styleable.AppPreference_simpleName);
            setPackageName(packageName);
            setServiceIntentName(serviceIntentName);
        } finally {
            a.recycle();
        }

        final String marketUrl = String.format(MARKET_INTENT_URI_BASE, getPackageName());
        marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl));
        populateAppList();
    }

    @Override
    public CharSequence getSummary() {
        return getEntryByValue(selectedPackage);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            // Restore state
            selectedPackage = getPersistedString(selectedPackage);
            updateSummary(selectedPackage);
        } else {
            String value = (String) defaultValue;
            setAndPersist(value);
            updateSummary(value);
        }
    }

    void setAndPersist(String packageName) {
        selectedPackage = packageName;

        // Save to persistent storage (this method will make sure this
        // preference should be persistent, along with other useful checks)
        persistString(selectedPackage);

        // Data has changed, notify so UI can be refreshed!
        notifyChanged();

        // also update summary with selected provider
        updateSummary(selectedPackage);
    }

    private void updateSummary(String packageName) {
        String summary = getEntryByValue(packageName);
        setSummary(summary);
    }

    public String getEntryByValue(String packageName) {
        for (AppEntry app : entries) {
            if (app.getPackageName().equals(packageName) && app.getIntent() == null) {
                return app.getSimpleName();
            }
        }

        return getContext().getString(R.string.app_preference_none);
    }

    void populateAppList() {
        getEntries().clear();

        // add "none"-entry
        getEntries().add(0, new AppEntry("",
                getContext().getString(R.string.app_preference_none),
                noneIcon));

        // search for OpenPGP providers...
        ArrayList<AppEntry> providerList = new ArrayList<>();
        Intent intent = new Intent(getServiceIntentName());
        PackageManager packageManager = getContext().getPackageManager();
        List<ResolveInfo> resInfo = packageManager.queryIntentServices(intent, 0);
        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                if (resolveInfo.serviceInfo == null)
                    continue;

                String packageName = resolveInfo.serviceInfo.packageName;
                String simpleName = String.valueOf(resolveInfo.serviceInfo.loadLabel(packageManager));
                Drawable icon = resolveInfo.serviceInfo.loadIcon(packageManager);

                if (!PROVIDER_BLACKLIST.contains(packageName)) {
                    providerList.add(new AppEntry(packageName, simpleName, icon));
                }
            }
        }

        if (providerList.isEmpty()) {
            // add install links if provider list is empty
            resInfo = packageManager.queryIntentActivities(getMarketIntent(), 0);

            for (ResolveInfo resolveInfo : resInfo) {
                Intent marketIntent = new Intent(getMarketIntent());
                marketIntent.setPackage(resolveInfo.activityInfo.packageName);
                Drawable icon = resolveInfo.activityInfo.loadIcon(packageManager);

                String marketName = String.valueOf(resolveInfo.activityInfo.applicationInfo
                        .loadLabel(packageManager));
                String simpleName = String.format(getContext().getString(R.string
                        .app_preference_install_via), this.simpleName, marketName);
                getEntries().add(new AppEntry(getPackageName(), simpleName, icon, marketIntent));
            }
        } else {
            // add provider
            getEntries().addAll(providerList);
        }
    }

    public List<AppEntry> getEntries() {
        if(entries == null) {
            entries = new ArrayList<>();
        }

        return entries;
    }

    public void setEntries(List<AppEntry> entries) {
        this.entries = entries;
    }

    public String getServiceIntentName() {
        return serviceIntentName;
    }

    public void setServiceIntentName(String serviceIntentName) {
        this.serviceIntentName = serviceIntentName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Intent getMarketIntent() {
        return marketIntent;
    }

    public String getSelectedPackage() {
        return selectedPackage;
    }

    public void setSelectedPackage(String selectedPackage) {
        this.selectedPackage = selectedPackage;
    }

    @Override
    public PreferenceDialogFragmentCompat getDialogInstance() {
        return AppPreferenceDialog.newInstance(getKey());
    }
}

package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.ColorPickerDialog;
import com.fsck.k9.fragment.SmilePreferenceFragment;
import com.fsck.k9.helper.FileBrowserHelper;
import com.fsck.k9.helper.FileBrowserHelper.FileBrowserFailOverCallback;
import com.fsck.k9.helper.NotificationHelper;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mail.RemindMe.RemindMeInterval;
import com.fsck.k9.preferences.AccountPreference;
import com.fsck.k9.preferences.BACKGROUND_OPS;
import com.fsck.k9.preferences.CheckBoxListPreference;
import com.fsck.k9.preferences.LockScreenNotificationVisibility;
import com.fsck.k9.preferences.NotificationHideSubject;
import com.fsck.k9.preferences.NotificationQuickDelete;
import com.fsck.k9.preferences.SplitViewMode;
import com.fsck.k9.preferences.TimePickerPreference;
import com.fsck.k9.preferences.TimeSpanPreference;
import com.fsck.k9.service.MailService;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.fau.cs.mad.smile.android.R;


public class GlobalPreferences extends SmilePreferenceFragment {

    /**
     * Immutable empty {@link CharSequence} array
     */
    private static final CharSequence[] EMPTY_CHAR_SEQUENCE_ARRAY = new CharSequence[0];
    /*
     * Keys of the preferences defined in res/xml/global_preferences.xml
     */
    private static final String PREFERENCE_LANGUAGE = "language";
    private static final String PREFERENCE_THEME = "theme";
    private static final String PREFERENCE_FONT_SIZE = "font_size";
    private static final String PREFERENCE_VOLUME_NAVIGATION = "volumeNavigation";
    private static final String PREFERENCE_START_INTEGRATED_INBOX = "start_integrated_inbox";
    private static final String PREFERENCE_NOTIFICATION_HIDE_SUBJECT = "notification_hide_subject";
    private static final String PREFERENCE_MESSAGELIST_PREVIEW_LINES = "messagelist_preview_lines";
    private static final String PREFERENCE_MESSAGELIST_SHOW_CORRESPONDENT_NAMES = "messagelist_show_correspondent_names";
    private static final String PREFERENCE_MESSAGELIST_CONTACT_NAME_COLOR = "messagelist_contact_name_color";
    private static final String PREFERENCE_MESSAGEVIEW_FIXEDWIDTH = "messageview_fixedwidth_font";
    private static final String PREFERENCE_MESSAGEVIEW_RETURN_TO_LIST = "messageview_return_to_list";
    private static final String PREFERENCE_QUIET_TIME_ENABLED = "quiet_time_enabled";
    private static final String PREFERENCE_DISABLE_NOTIFICATION_DURING_QUIET_TIME =
            "disable_notifications_during_quiet_time";
    private static final String PREFERENCE_QUIET_TIME_STARTS = "quiet_time_starts";
    private static final String PREFERENCE_QUIET_TIME_ENDS = "quiet_time_ends";
    private static final String PREFERENCE_NOTIF_QUICK_DELETE = "notification_quick_delete";
    private static final String PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY = "lock_screen_notification_visibility";
    private static final String PREFERENCE_DEBUG_LOGGING = "debug_logging";
    private static final String PREFERENCE_ATTACHMENT_DEF_PATH = "attachment_default_path";
    private static final String PREFERENCE_THREADED_VIEW = "threaded_view";
    private static final String PREFERENCE_SPLITVIEW_MODE = "splitview_mode";
    private static final int ACTIVITY_CHOOSE_FOLDER = 1;
    private ListPreference mLanguage;
    private ListPreference mTheme;
    private CheckBoxListPreference mVolumeNavigation;
    private SwitchPreferenceCompat mStartIntegratedInbox;
    private ListPreference mNotificationHideSubject;
    private ListPreference mPreviewLines;
    private SwitchPreferenceCompat mShowCorrespondentNames;
    private SwitchPreferenceCompat mChangeContactNameColor;
    private SwitchPreferenceCompat mFixedWidth;
    private SwitchPreferenceCompat mReturnToList;
    private SwitchPreferenceCompat mDebugLogging;
    private SwitchPreferenceCompat mQuietTimeEnabled;
    private SwitchPreferenceCompat mDisableNotificationDuringQuietTime;
    private TimePickerPreference mQuietTimeStarts;
    private TimePickerPreference mQuietTimeEnds;
    private ListPreference mNotificationQuickDelete;
    private ListPreference mLockScreenNotificationVisibility;
    private Preference mAttachmentPathPreference;
    private SwitchPreferenceCompat mThreadedView;
    private ListPreference mSplitViewMode;
    private GlobalPreferencesCallback callback;
    private TimeSpanPreference remindme_later;
    private TimePickerPreference remindme_evening;
    private TimePickerPreference remindme_tomorrow;
    private TimePickerPreference remindme_next_week;
    private TimePickerPreference remindme_next_month;
    private Context mContext;

    public static GlobalPreferences newInstance(GlobalPreferencesCallback callback) {
        GlobalPreferences preferences = new GlobalPreferences();
        preferences.setCallback(callback);
        return preferences;
    }

    private static String themeIdToName(K9.Theme theme) {
        switch (theme) {
            case DARK:
                return "dark";
            case USE_GLOBAL:
                return "global";
            default:
                return "light";
        }
    }

    private static K9.Theme themeNameToId(String theme) {
        if (TextUtils.equals(theme, "dark")) {
            return K9.Theme.DARK;
        } else if (TextUtils.equals(theme, "global")) {
            return K9.Theme.USE_GLOBAL;
        } else {
            return K9.Theme.LIGHT;
        }
    }

    @Override
    public SmilePreferenceFragment openPreferenceScreen() {
        return newInstance(callback);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        super.onCreatePreferences(bundle, s);
        setPreferencesFromResource(R.xml.global_preferences, s);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        JodaTimeAndroid.init(mContext);

        addAccounts();
        setupLanguage();
        setupFontSize();
        setupVolumeNavigation();

        mStartIntegratedInbox = setupSwitchPreference(PREFERENCE_START_INTEGRATED_INBOX, K9.startIntegratedInbox());
        mTheme = setupListPreference(PREFERENCE_THEME, themeIdToName(K9.getK9Theme()));
        mNotificationHideSubject = setupListPreference(PREFERENCE_NOTIFICATION_HIDE_SUBJECT, K9.getNotificationHideSubject().toString());
        mPreviewLines = setupListPreference(PREFERENCE_MESSAGELIST_PREVIEW_LINES, Integer.toString(K9.messageListPreviewLines()));
        mShowCorrespondentNames = setupSwitchPreference(PREFERENCE_MESSAGELIST_SHOW_CORRESPONDENT_NAMES, K9.showCorrespondentNames());
        mThreadedView = setupSwitchPreference(PREFERENCE_THREADED_VIEW, K9.isThreadedViewEnabled());

        mChangeContactNameColor = setupSwitchPreference(PREFERENCE_MESSAGELIST_CONTACT_NAME_COLOR, K9.changeContactNameColor());
        if (mChangeContactNameColor != null) {
            if (K9.changeContactNameColor()) {
                mChangeContactNameColor.setSummary(R.string.global_settings_registered_name_color_changed);
            } else {
                mChangeContactNameColor.setSummary(R.string.global_settings_registered_name_color_default);
            }

            mChangeContactNameColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final Boolean checked = (Boolean) newValue;
                    if (checked) {
                        onChooseContactNameColor();
                        mChangeContactNameColor.setSummary(R.string.global_settings_registered_name_color_changed);
                    } else {
                        mChangeContactNameColor.setSummary(R.string.global_settings_registered_name_color_default);
                    }
                    mChangeContactNameColor.setChecked(checked);
                    return false;
                }
            });
        }

        remindme_later = (TimeSpanPreference) findPreference("remindme_time_later");
        remindme_evening = setupTimePickerPreference("remindme_time_evening", K9.getRemindMeTime(RemindMeInterval.EVENING));
        remindme_tomorrow = setupTimePickerPreference("remindme_time_tomorrow", K9.getRemindMeTime(RemindMeInterval.TOMORROW));
        remindme_next_week = setupTimePickerPreference("remindme_time_next_week", K9.getRemindMeTime(RemindMeInterval.NEXT_WEEK));
        remindme_next_month = setupTimePickerPreference("remindme_time_next_month", K9.getRemindMeTime(RemindMeInterval.NEXT_MONTH));

        mFixedWidth = setupSwitchPreference(PREFERENCE_MESSAGEVIEW_FIXEDWIDTH, K9.messageViewFixedWidthFont());
        mReturnToList = setupSwitchPreference(PREFERENCE_MESSAGEVIEW_RETURN_TO_LIST, K9.messageViewReturnToList());
        mQuietTimeEnabled = setupSwitchPreference(PREFERENCE_QUIET_TIME_ENABLED, K9.getQuietTimeEnabled());
        mDisableNotificationDuringQuietTime = setupSwitchPreference(PREFERENCE_DISABLE_NOTIFICATION_DURING_QUIET_TIME, !K9.isNotificationDuringQuietTimeEnabled());

        setupQuietTime();

        mNotificationQuickDelete = setupListPreference(PREFERENCE_NOTIF_QUICK_DELETE,
                K9.getNotificationQuickDeleteBehaviour().toString());
        if (!NotificationHelper.platformSupportsExtendedNotifications()) {
            PreferenceScreen prefs = (PreferenceScreen) findPreference("notification_preferences");
            if (prefs != null) {
                prefs.removePreference(mNotificationQuickDelete);
            }

            mNotificationQuickDelete = null;
        }

        mLockScreenNotificationVisibility = setupListPreference(PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY,
                K9.getLockScreenNotificationVisibility().toString());
        if (!NotificationHelper.platformSupportsLockScreenNotifications()) {
            PreferenceScreen prefs = (PreferenceScreen) findPreference("notification_preferences");
            if (prefs != null) {
                prefs.removePreference(mLockScreenNotificationVisibility);
            }
            mLockScreenNotificationVisibility = null;
        }

        mDebugLogging = setupSwitchPreference(PREFERENCE_DEBUG_LOGGING, K9.DEBUG);

        setupAttachmentPath();

        mSplitViewMode = (ListPreference) findPreference(PREFERENCE_SPLITVIEW_MODE);
        if (mSplitViewMode != null) {
            initListPreference(mSplitViewMode, K9.getSplitViewMode().name(),
                    mSplitViewMode.getEntries(), mSplitViewMode.getEntryValues());
        }
    }

    private TimePickerPreference setupTimePickerPreference(String key, Period defaultValue) {
        TimePickerPreference preference = (TimePickerPreference) findPreference(key);
        if (preference == null) {
            return null;
        }

        preference.setDefaultValue(defaultValue);
        return preference;
    }

    private void setupQuietTime() {
        final DateTimeFormatter formatter = DateTimeFormat.shortTime();
        mQuietTimeStarts = (TimePickerPreference) findPreference(PREFERENCE_QUIET_TIME_STARTS);
        if (mQuietTimeStarts != null) {
            mQuietTimeStarts.setDefaultValue(K9.getQuietTimeStarts());
            mQuietTimeStarts.setSummary(K9.getQuietTimeStarts().toString(formatter));
            mQuietTimeStarts.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final DateTime time = (DateTime) newValue;
                    mQuietTimeStarts.setSummary(formatter.print(time));
                    return false;
                }
            });
        }

        mQuietTimeEnds = (TimePickerPreference) findPreference(PREFERENCE_QUIET_TIME_ENDS);
        if (mQuietTimeEnds != null) {
            mQuietTimeEnds.setSummary(K9.getQuietTimeEnds().toString(formatter));
            mQuietTimeEnds.setDefaultValue(K9.getQuietTimeEnds());
            mQuietTimeEnds.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final DateTime time = (DateTime) newValue;
                    mQuietTimeEnds.setSummary(formatter.print(time));
                    return false;
                }
            });
        }
    }

    private void setupAttachmentPath() {
        mAttachmentPathPreference = findPreference(PREFERENCE_ATTACHMENT_DEF_PATH);
        if (mAttachmentPathPreference == null) {
            return;
        }

        mAttachmentPathPreference.setSummary(K9.getAttachmentDefaultPath());
        mAttachmentPathPreference
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    FileBrowserFailOverCallback callback = new FileBrowserFailOverCallback() {

                        @Override
                        public void onPathEntered(String path) {
                            mAttachmentPathPreference.setSummary(path);
                            K9.setAttachmentDefaultPath(path);
                        }

                        @Override
                        public void onCancel() {
                            // canceled, do nothing
                        }
                    };

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        FileBrowserHelper
                                .getInstance()
                                .showFileBrowserActivity(getActivity(),
                                        new File(K9.getAttachmentDefaultPath()),
                                        ACTIVITY_CHOOSE_FOLDER, callback);

                        return true;
                    }
                });
    }

    private void setupVolumeNavigation() {
        mVolumeNavigation = (CheckBoxListPreference) findPreference(PREFERENCE_VOLUME_NAVIGATION);
        if (mVolumeNavigation == null) {
            return;
        }

        mVolumeNavigation.setItems(new CharSequence[]{getString(R.string.volume_navigation_message), getString(R.string.volume_navigation_list)});
        mVolumeNavigation.setCheckedItems(new boolean[]{K9.useVolumeKeysForNavigationEnabled(), K9.useVolumeKeysForListNavigationEnabled()});
    }

    private void setupFontSize() {
        Preference fontPreference = findPreference(PREFERENCE_FONT_SIZE);
        if (fontPreference == null) {
            return;
        }

        fontPreference.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        callback.onFontSizeSettings();
                        return true;
                    }
                });
    }

    private void setupLanguage() {
        mLanguage = (ListPreference) findPreference(PREFERENCE_LANGUAGE);
        if (mLanguage == null) {
            return;
        }

        List<CharSequence> entryVector = new ArrayList<>(Arrays.asList(mLanguage.getEntries()));
        List<CharSequence> entryValueVector = new ArrayList<>(Arrays.asList(mLanguage.getEntryValues()));
        String supportedLanguages[] = getResources().getStringArray(R.array.supported_languages);
        Set<String> supportedLanguageSet = new HashSet<>(Arrays.asList(supportedLanguages));

        for (int i = entryVector.size() - 1; i > -1; --i) {
            if (!supportedLanguageSet.contains(entryValueVector.get(i))) {
                entryVector.remove(i);
                entryValueVector.remove(i);
            }
        }

        initListPreference(mLanguage, K9.getK9Language(),
                entryVector.toArray(EMPTY_CHAR_SEQUENCE_ARRAY),
                entryValueVector.toArray(EMPTY_CHAR_SEQUENCE_ARRAY));
    }

    private void addAccounts() {
        PreferenceCategory category = (PreferenceCategory) findPreference("accounts");
        if (category == null) {
            return;
        }

        List<Account> accounts = Preferences.getPreferences(mContext).getAccounts();
        for (Account account : accounts) {
            category.addPreference(new AccountPreference(mContext, account, new OnAccountPreferenceClickListener()));
        }
    }

    @Override
    public void onPause() {
        saveSettings();
        super.onPause();
    }

    private void saveSettings() {
        SharedPreferences preferences = Preferences.getPreferences(mContext).getPreferences();

        handleSaveRemindMeScreen();
        boolean needsRefresh = handleSave();

        Editor editor = preferences.edit();
        K9.save(editor);
        editor.apply();

        if (needsRefresh) {
            MailService.actionReset(mContext, null);
        }
    }

    private boolean handleSave() {
        if(mLanguage == null) {
            return false;
        }

        K9.setK9Language(mLanguage.getValue());
        K9.setK9Theme(themeNameToId(mTheme.getValue()));
        K9.setUseVolumeKeysForNavigation(mVolumeNavigation.getCheckedItems()[0]);
        K9.setUseVolumeKeysForListNavigation(mVolumeNavigation.getCheckedItems()[1]);
        K9.setStartIntegratedInbox(mStartIntegratedInbox.isChecked());
        K9.setNotificationHideSubject(NotificationHideSubject.valueOf(mNotificationHideSubject.getValue()));

        if (NotificationHelper.platformSupportsExtendedNotifications()) {
            K9.setConfirmDeleteFromNotification(true);
        }

        K9.setMessageListPreviewLines(Integer.parseInt(mPreviewLines.getValue()));
        K9.setShowCorrespondentNames(mShowCorrespondentNames.isChecked());
        K9.setThreadedViewEnabled(mThreadedView.isChecked());
        K9.setChangeContactNameColor(mChangeContactNameColor.isChecked());
        K9.setMessageViewFixedWidthFont(mFixedWidth.isChecked());
        K9.setMessageViewReturnToList(mReturnToList.isChecked());

        K9.setQuietTimeEnabled(mQuietTimeEnabled.isChecked());
        K9.setNotificationDuringQuietTimeEnabled(!mDisableNotificationDuringQuietTime.isChecked());
        K9.setQuietTimeStarts(mQuietTimeStarts.getTime());
        K9.setQuietTimeEnds(mQuietTimeEnds.getTime());

        K9.setNotificationQuickDeleteBehaviour(
                NotificationQuickDelete.valueOf(mNotificationQuickDelete.getValue()));

        K9.setLockScreenNotificationVisibility(
                LockScreenNotificationVisibility.valueOf(mLockScreenNotificationVisibility.getValue()));


        K9.setSplitViewMode(SplitViewMode.valueOf(mSplitViewMode.getValue()));
        K9.setAttachmentDefaultPath(mAttachmentPathPreference.getSummary().toString());
        boolean needsRefresh = K9.setBackgroundOps(BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC);

        if (!K9.DEBUG && mDebugLogging.isChecked()) {
            Toast.makeText(mContext, R.string.debug_logging_enabled, Toast.LENGTH_LONG).show();
        }

        K9.DEBUG = mDebugLogging.isChecked();

        K9.setAnimations(true);
        K9.setGesturesEnabled(true);
        K9.setConfirmDelete(false);
        K9.setConfirmDeleteStarred(true);
        K9.setConfirmSpam(false);
        K9.setConfirmDiscardMessage(true);
        K9.setMeasureAccounts(true);
        K9.setCountSearchMessages(true);
        K9.setHideSpecialAccounts(false);
        K9.setMessageListCheckboxes(false);
        K9.setMessageListStars(true);
        K9.setMessageListSenderAboveSubject(false);
        K9.setShowContactName(true);
        K9.setShowContactPicture(true);
        K9.setColorizeMissingContactPictures(true);
        K9.setUseBackgroundAsUnreadIndicator(true);
        K9.setMessageViewShowNext(true);
        K9.setAutofitWidth(true);
        K9.setWrapFolderNames(true);
        K9.DEBUG_SENSITIVE = false;
        K9.setHideUserAgent(false);
        K9.setHideTimeZone(true);
        return needsRefresh;
    }

    private void handleSaveRemindMeScreen() {
        if(remindme_later == null) {
            return;
        }

        K9.setRemindMeTime(RemindMeInterval.LATER, remindme_later.getPeriod());
        K9.setRemindMeTime(RemindMeInterval.EVENING, remindme_evening.getPeriod());
        K9.setRemindMeTime(RemindMeInterval.TOMORROW, remindme_tomorrow.getPeriod());
        K9.setRemindMeTime(RemindMeInterval.NEXT_WEEK, remindme_next_week.getPeriod());
        K9.setRemindMeTime(RemindMeInterval.NEXT_MONTH, remindme_next_month.getPeriod());
    }

    private void onChooseContactNameColor() {
        ColorPickerDialog dialog = ColorPickerDialog.newInstance(
                new ColorPickerDialog.OnColorChangedListener() {
                    public void colorChanged(int color) {
                        K9.setContactNameColor(color);
                    }
                }, K9.getContactNameColor());

        dialog.show(getFragmentManager(), "colorPicker");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_CHOOSE_FOLDER:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // obtain the filename
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        String filePath = fileUri.getPath();
                        if (filePath != null) {
                            mAttachmentPathPreference.setSummary(filePath);
                            K9.setAttachmentDefaultPath(filePath);
                        }
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setCallback(GlobalPreferencesCallback callback) {
        this.callback = callback;
    }

    public interface GlobalPreferencesCallback {
        void onAccountClick(Account account);

        void onFontSizeSettings();
    }

    private class OnAccountPreferenceClickListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            AccountPreference accountPreference = (AccountPreference) preference;
            K9.logDebug("found acc pref: " + accountPreference.getAccount());
            callback.onAccountClick(accountPreference.getAccount());
            return false;
        }
    }
}

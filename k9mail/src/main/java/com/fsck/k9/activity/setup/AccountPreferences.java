package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.SwitchPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.Account.MessageFormat;
import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.Account.Searchable;
import com.fsck.k9.Account.ShowPictures;
import com.fsck.k9.K9;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.ColorPickerDialog;
import com.fsck.k9.crypto.OpenPgpApiHelper;
import com.fsck.k9.fragment.SmilePreferenceFragment;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.service.MailService;

import org.openintents.openpgp.util.OpenPgpAppPreference;
import org.openintents.openpgp.util.OpenPgpKeyPreference;
import org.openintents.openpgp.util.OpenPgpUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.fau.cs.mad.smile.android.R;
import de.fau.cs.mad.smime_api.SMimeApi;


public class AccountPreferences extends SmilePreferenceFragment {
    private static final String EXTRA_ACCOUNT = "account";

    private static final int SELECT_AUTO_EXPAND_FOLDER = 100;

    private static final String PREFERENCE_SCREEN_MAIN = "main";
    private static final String PREFERENCE_SCREEN_COMPOSING = "composing";
    private static final String PREFERENCE_SCREEN_INCOMING = "incoming_prefs";
    private static final String PREFERENCE_SCREEN_PUSH_ADVANCED = "push_advanced";
    private static final String PREFERENCE_SCREEN_SEARCH = "search";

    private static final String PREFERENCE_DESCRIPTION = "account_description";
    private static final String PREFERENCE_COMPOSITION = "composition";
    private static final String PREFERENCE_FREQUENCY = "account_check_frequency";
    private static final String PREFERENCE_DISPLAY_COUNT = "account_display_count";
    private static final String PREFERENCE_DEFAULT = "account_default";
    private static final String PREFERENCE_SHOW_PICTURES = "show_pictures_enum";
    private static final String PREFERENCE_NOTIFY = "account_notify";
    private static final String PREFERENCE_NOTIFY_NEW_MAIL_MODE = "folder_notify_new_mail_mode";
    private static final String PREFERENCE_NOTIFY_SELF = "account_notify_self";
    private static final String PREFERENCE_NOTIFY_SYNC = "account_notify_sync";
    private static final String PREFERENCE_VIBRATE = "account_vibrate";
    private static final String PREFERENCE_VIBRATE_PATTERN = "account_vibrate_pattern";
    private static final String PREFERENCE_VIBRATE_TIMES = "account_vibrate_times";
    private static final String PREFERENCE_RINGTONE = "account_ringtone";
    private static final String PREFERENCE_NOTIFICATION_LED = "account_led";
    private static final String PREFERENCE_INCOMING = "incoming";
    private static final String PREFERENCE_OUTGOING = "outgoing";
    private static final String PREFERENCE_DISPLAY_MODE = "folder_display_mode";
    private static final String PREFERENCE_SYNC_MODE = "folder_sync_mode";
    private static final String PREFERENCE_PUSH_MODE = "folder_push_mode";
    private static final String PREFERENCE_TARGET_MODE = "folder_target_mode";
    private static final String PREFERENCE_DELETE_POLICY = "delete_policy";
    private static final String PREFERENCE_EXPUNGE_POLICY = "expunge_policy";
    private static final String PREFERENCE_AUTO_EXPAND_FOLDER = "account_setup_auto_expand_folder";
    private static final String PREFERENCE_SEARCHABLE_FOLDERS = "searchable_folders";
    private static final String PREFERENCE_CHIP_COLOR = "chip_color";
    private static final String PREFERENCE_LED_COLOR = "led_color";
    private static final String PREFERENCE_NOTIFICATION_OPENS_UNREAD = "notification_opens_unread";
    private static final String PREFERENCE_MESSAGE_AGE = "account_message_age";
    private static final String PREFERENCE_MESSAGE_SIZE = "account_autodownload_size";
    private static final String PREFERENCE_MESSAGE_FORMAT = "message_format";
    private static final String PREFERENCE_MESSAGE_READ_RECEIPT = "message_read_receipt";
    private static final String PREFERENCE_QUOTE_PREFIX = "account_quote_prefix";
    private static final String PREFERENCE_QUOTE_STYLE = "quote_style";
    private static final String PREFERENCE_DEFAULT_QUOTED_TEXT_SHOWN = "default_quoted_text_shown";
    private static final String PREFERENCE_SYNC_REMOTE_DELETIONS = "account_sync_remote_deletetions";
    private static final String PREFERENCE_CRYPTO = "crypto";
    private static final String PREFERENCE_CRYPTO_APP = "crypto_app";
    private static final String PREFERENCE_CRYPTO_KEY = "crypto_key";
    private static final String PREFERENCE_CLOUD_SEARCH_ENABLED = "remote_search_enabled";
    private static final String PREFERENCE_REMOTE_SEARCH_NUM_RESULTS = "account_remote_search_num_results";
    private static final String PREFERENCE_REMOTE_SEARCH_FULL_TEXT = "account_remote_search_full_text";

    private static final String PREFERENCE_LOCAL_STORAGE_PROVIDER = "local_storage_provider";
    private static final String PREFERENCE_CATEGORY_FOLDERS = "folders";
    private static final String PREFERENCE_ARCHIVE_FOLDER = "archive_folder";
    private static final String PREFERENCE_DRAFTS_FOLDER = "drafts_folder";
    private static final String PREFERENCE_SENT_FOLDER = "sent_folder";
    private static final String PREFERENCE_SPAM_FOLDER = "spam_folder";
    private static final String PREFERENCE_TRASH_FOLDER = "trash_folder";

    private static final String PREFERENCE_CRYPTO_SMIME_APP = "smime_app";

    private Account mAccount;
    private boolean mIsMoveCapable = false;
    private boolean mIsPushCapable = false;
    private boolean mIsExpungeCapable = false;
    private boolean mIsSeenFlagSupported = false;

    private PreferenceScreen mMainScreen;
    private PreferenceScreen mComposingScreen;

    private EditTextPreference mAccountDescription;
    private ListPreference mCheckFrequency;
    private ListPreference mDisplayCount;
    private ListPreference mMessageAge;
    private ListPreference mMessageSize;
    private SwitchPreference mAccountDefault;
    private SwitchPreference mAccountNotify;
    private ListPreference mAccountNotifyNewMailMode;
    private SwitchPreference mAccountNotifySelf;
    private ListPreference mAccountShowPictures;
    private SwitchPreference mAccountNotifySync;
    private SwitchPreference mAccountVibrate;
    private SwitchPreference mAccountLed;
    private ListPreference mAccountVibratePattern;
    private ListPreference mAccountVibrateTimes;
    private RingtonePreference mAccountRingtone;
    private ListPreference mDisplayMode;
    private ListPreference mSyncMode;
    private ListPreference mPushMode;
    private ListPreference mTargetMode;
    private ListPreference mDeletePolicy;
    private ListPreference mExpungePolicy;
    private ListPreference mSearchableFolders;
    private ListPreference mAutoExpandFolder;
    private Preference mChipColor;
    private Preference mLedColor;
    private boolean mIncomingChanged = false;
    private SwitchPreference mNotificationOpensUnread;
    private ListPreference mMessageFormat;
    private SwitchPreference mMessageReadReceipt;
    private ListPreference mQuoteStyle;
    private EditTextPreference mAccountQuotePrefix;
    private SwitchPreference mAccountDefaultQuotedTextShown;
    private SwitchPreference mSyncRemoteDeletions;
    private boolean mHasCrypto = false;
    private OpenPgpAppPreference mCryptoApp;
    private OpenPgpKeyPreference mCryptoKey;

    private PreferenceCategory mSearchScreen;
    private SwitchPreference mCloudSearchEnabled;
    private ListPreference mRemoteSearchNumResults;
    /*
     * Temporarily removed because search results aren't displayed to the user.
     * So this feature is useless.
     */
    //private SwitchPreference mRemoteSearchFullText;

    private ListPreference mLocalStorageProvider;
    private ListPreference mArchiveFolder;
    private ListPreference mDraftsFolder;
    private ListPreference mSentFolder;
    private ListPreference mSpamFolder;
    private ListPreference mTrashFolder;

    private ListPreference mSmimeApp;
    private ListPreference defaultCryptoProvider;

    private Context mContext;

    public static AccountPreferences newInstance(Account account) {
        Bundle args = new Bundle();
        args.putString(EXTRA_ACCOUNT, account.getUuid());
        AccountPreferences settings = new AccountPreferences();
        settings.setArguments(args);
        return settings;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        Bundle args = getArguments();
        String accountUuid = args.getString(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(mContext).getAccount(accountUuid);

        try {
            final Store store = mAccount.getRemoteStore();
            mIsMoveCapable = store.isMoveCapable();
            mIsPushCapable = store.isPushCapable();
            mIsExpungeCapable = store.isExpungeCapable();
            mIsSeenFlagSupported = store.isSeenFlagSupported();
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Could not get remote store", e);
        }

        addPreferencesFromResource(R.xml.account_preferences);

        mMainScreen = (PreferenceScreen) findPreference(PREFERENCE_SCREEN_MAIN);

        mAccountDescription = (EditTextPreference) findPreference(PREFERENCE_DESCRIPTION);
        mAccountDescription.setSummary(mAccount.getDescription());
        mAccountDescription.setText(mAccount.getDescription());
        mAccountDescription.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                mAccountDescription.setSummary(summary);
                mAccountDescription.setText(summary);
                return false;
            }
        });

        mMessageFormat = (ListPreference) findPreference(PREFERENCE_MESSAGE_FORMAT);
        mMessageFormat.setValue(mAccount.getMessageFormat().name());
        mMessageFormat.setSummary(mMessageFormat.getEntry());
        mMessageFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mMessageFormat.findIndexOfValue(summary);
                mMessageFormat.setSummary(mMessageFormat.getEntries()[index]);
                mMessageFormat.setValue(summary);
                return false;
            }
        });

        mMessageReadReceipt = (SwitchPreference) findPreference(PREFERENCE_MESSAGE_READ_RECEIPT);
        mMessageReadReceipt.setChecked(mAccount.isMessageReadReceiptAlways());

        mAccountQuotePrefix = (EditTextPreference) findPreference(PREFERENCE_QUOTE_PREFIX);
        mAccountQuotePrefix.setSummary(mAccount.getQuotePrefix());
        mAccountQuotePrefix.setText(mAccount.getQuotePrefix());
        mAccountQuotePrefix.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String value = newValue.toString();
                mAccountQuotePrefix.setSummary(value);
                mAccountQuotePrefix.setText(value);
                return false;
            }
        });

        mAccountDefaultQuotedTextShown = (SwitchPreference) findPreference(PREFERENCE_DEFAULT_QUOTED_TEXT_SHOWN);
        mAccountDefaultQuotedTextShown.setChecked(mAccount.isDefaultQuotedTextShown());

        mComposingScreen = (PreferenceScreen) findPreference(PREFERENCE_SCREEN_COMPOSING);

        Preference.OnPreferenceChangeListener quoteStyleListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final QuoteStyle style = QuoteStyle.valueOf(newValue.toString());
                int index = mQuoteStyle.findIndexOfValue(newValue.toString());
                mQuoteStyle.setSummary(mQuoteStyle.getEntries()[index]);

                if (style == QuoteStyle.PREFIX) {
                    mComposingScreen.addPreference(mAccountQuotePrefix);
                } else if (style == QuoteStyle.HEADER) {
                    mComposingScreen.removePreference(mAccountQuotePrefix);
                }
                return true;
            }
        };

        mQuoteStyle = (ListPreference) findPreference(PREFERENCE_QUOTE_STYLE);
        mQuoteStyle.setValue(mAccount.getQuoteStyle().name());
        mQuoteStyle.setSummary(mQuoteStyle.getEntry());
        mQuoteStyle.setOnPreferenceChangeListener(quoteStyleListener);
        // Call the onPreferenceChange() handler on startup to update the Preference dialogue based
        // upon the existing quote style setting.
        quoteStyleListener.onPreferenceChange(mQuoteStyle, mAccount.getQuoteStyle().name());

        mCheckFrequency = (ListPreference) findPreference(PREFERENCE_FREQUENCY);
        mCheckFrequency.setValue(String.valueOf(mAccount.getAutomaticCheckIntervalMinutes()));
        mCheckFrequency.setSummary(mCheckFrequency.getEntry());
        mCheckFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mCheckFrequency.findIndexOfValue(summary);
                mCheckFrequency.setSummary(mCheckFrequency.getEntries()[index]);
                mCheckFrequency.setValue(summary);
                return false;
            }
        });

        mDisplayMode = (ListPreference) findPreference(PREFERENCE_DISPLAY_MODE);
        mDisplayMode.setValue(mAccount.getFolderDisplayMode().name());
        mDisplayMode.setSummary(mDisplayMode.getEntry());
        mDisplayMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mDisplayMode.findIndexOfValue(summary);
                mDisplayMode.setSummary(mDisplayMode.getEntries()[index]);
                mDisplayMode.setValue(summary);
                return false;
            }
        });

        mSyncMode = (ListPreference) findPreference(PREFERENCE_SYNC_MODE);
        mSyncMode.setValue(mAccount.getFolderSyncMode().name());
        mSyncMode.setSummary(mSyncMode.getEntry());
        mSyncMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mSyncMode.findIndexOfValue(summary);
                mSyncMode.setSummary(mSyncMode.getEntries()[index]);
                mSyncMode.setValue(summary);
                return false;
            }
        });


        mTargetMode = (ListPreference) findPreference(PREFERENCE_TARGET_MODE);
        mTargetMode.setValue(mAccount.getFolderTargetMode().name());
        mTargetMode.setSummary(mTargetMode.getEntry());
        mTargetMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mTargetMode.findIndexOfValue(summary);
                mTargetMode.setSummary(mTargetMode.getEntries()[index]);
                mTargetMode.setValue(summary);
                return false;
            }
        });

        mDeletePolicy = (ListPreference) findPreference(PREFERENCE_DELETE_POLICY);
        if (!mIsSeenFlagSupported) {
            removeListEntry(mDeletePolicy, DeletePolicy.MARK_AS_READ.preferenceString());
        }
        mDeletePolicy.setValue(mAccount.getDeletePolicy().preferenceString());
        mDeletePolicy.setSummary(mDeletePolicy.getEntry());
        mDeletePolicy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mDeletePolicy.findIndexOfValue(summary);
                mDeletePolicy.setSummary(mDeletePolicy.getEntries()[index]);
                mDeletePolicy.setValue(summary);
                return false;
            }
        });


        mExpungePolicy = (ListPreference) findPreference(PREFERENCE_EXPUNGE_POLICY);
        if (mIsExpungeCapable) {
            mExpungePolicy.setValue(mAccount.getExpungePolicy().name());
            mExpungePolicy.setSummary(mExpungePolicy.getEntry());
            mExpungePolicy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = mExpungePolicy.findIndexOfValue(summary);
                    mExpungePolicy.setSummary(mExpungePolicy.getEntries()[index]);
                    mExpungePolicy.setValue(summary);
                    return false;
                }
            });
        } else {
            ((PreferenceScreen) findPreference(PREFERENCE_SCREEN_INCOMING)).removePreference(mExpungePolicy);
        }


        mSyncRemoteDeletions = (SwitchPreference) findPreference(PREFERENCE_SYNC_REMOTE_DELETIONS);
        mSyncRemoteDeletions.setChecked(mAccount.syncRemoteDeletions());

        mSearchableFolders = (ListPreference) findPreference(PREFERENCE_SEARCHABLE_FOLDERS);
        mSearchableFolders.setValue(mAccount.getSearchableFolders().name());
        mSearchableFolders.setSummary(mSearchableFolders.getEntry());
        mSearchableFolders.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mSearchableFolders.findIndexOfValue(summary);
                mSearchableFolders.setSummary(mSearchableFolders.getEntries()[index]);
                mSearchableFolders.setValue(summary);
                return false;
            }
        });

        mDisplayCount = (ListPreference) findPreference(PREFERENCE_DISPLAY_COUNT);
        mDisplayCount.setValue(String.valueOf(mAccount.getDisplayCount()));
        mDisplayCount.setSummary(mDisplayCount.getEntry());
        mDisplayCount.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mDisplayCount.findIndexOfValue(summary);
                mDisplayCount.setSummary(mDisplayCount.getEntries()[index]);
                mDisplayCount.setValue(summary);
                return false;
            }
        });


        mMessageAge = (ListPreference) findPreference(PREFERENCE_MESSAGE_AGE);

        if (!mAccount.isSearchByDateCapable()) {
            ((PreferenceScreen) findPreference(PREFERENCE_SCREEN_INCOMING)).removePreference(mMessageAge);
        } else {
            mMessageAge.setValue(String.valueOf(mAccount.getMaximumPolledMessageAge()));
            mMessageAge.setSummary(mMessageAge.getEntry());
            mMessageAge.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = mMessageAge.findIndexOfValue(summary);
                    mMessageAge.setSummary(mMessageAge.getEntries()[index]);
                    mMessageAge.setValue(summary);
                    return false;
                }
            });

        }

        mMessageSize = (ListPreference) findPreference(PREFERENCE_MESSAGE_SIZE);
        mMessageSize.setValue(String.valueOf(mAccount.getMaximumAutoDownloadMessageSize()));
        mMessageSize.setSummary(mMessageSize.getEntry());
        mMessageSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mMessageSize.findIndexOfValue(summary);
                mMessageSize.setSummary(mMessageSize.getEntries()[index]);
                mMessageSize.setValue(summary);
                return false;
            }
        });

        mAccountDefault = (SwitchPreference) findPreference(PREFERENCE_DEFAULT);
        mAccountDefault.setChecked(
                mAccount.equals(Preferences.getPreferences(mContext).getDefaultAccount()));

        mAccountShowPictures = (ListPreference) findPreference(PREFERENCE_SHOW_PICTURES);
        mAccountShowPictures.setValue("" + mAccount.getShowPictures());
        mAccountShowPictures.setSummary(mAccountShowPictures.getEntry());
        mAccountShowPictures.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mAccountShowPictures.findIndexOfValue(summary);
                mAccountShowPictures.setSummary(mAccountShowPictures.getEntries()[index]);
                mAccountShowPictures.setValue(summary);
                return false;
            }
        });


        mLocalStorageProvider = (ListPreference) findPreference(PREFERENCE_LOCAL_STORAGE_PROVIDER);
        {
            final Map<String, String> providers;
            providers = StorageManager.getInstance(mContext).getAvailableProviders();
            int i = 0;
            final String[] providerLabels = new String[providers.size()];
            final String[] providerIds = new String[providers.size()];
            for (final Map.Entry<String, String> entry : providers.entrySet()) {
                providerIds[i] = entry.getKey();
                providerLabels[i] = entry.getValue();
                i++;
            }
            mLocalStorageProvider.setEntryValues(providerIds);
            mLocalStorageProvider.setEntries(providerLabels);
            mLocalStorageProvider.setValue(mAccount.getLocalStorageProviderId());
            mLocalStorageProvider.setSummary(providers.get(mAccount.getLocalStorageProviderId()));

            mLocalStorageProvider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mLocalStorageProvider.setSummary(providers.get(newValue));
                    return true;
                }
            });
        }

        // IMAP-specific preferences

        mSearchScreen = (PreferenceCategory) findPreference(PREFERENCE_SCREEN_SEARCH);

        mCloudSearchEnabled = (SwitchPreference) findPreference(PREFERENCE_CLOUD_SEARCH_ENABLED);
        mRemoteSearchNumResults = (ListPreference) findPreference(PREFERENCE_REMOTE_SEARCH_NUM_RESULTS);
        mRemoteSearchNumResults.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference pref, Object newVal) {
                        updateRemoteSearchLimit((String) newVal);
                        return true;
                    }
                }
        );
        //mRemoteSearchFullText = (SwitchPreference) findPreference(PREFERENCE_REMOTE_SEARCH_FULL_TEXT);

        if (mIsPushCapable) {
            mCloudSearchEnabled.setChecked(mAccount.allowRemoteSearch());
            String searchNumResults = Integer.toString(mAccount.getRemoteSearchNumResults());
            mRemoteSearchNumResults.setValue(searchNumResults);
            updateRemoteSearchLimit(searchNumResults);
            //mRemoteSearchFullText.setChecked(mAccount.isRemoteSearchFullText());

            mPushMode = (ListPreference) findPreference(PREFERENCE_PUSH_MODE);
            mPushMode.setValue(mAccount.getFolderPushMode().name());
            mPushMode.setSummary(mPushMode.getEntry());
            mPushMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = mPushMode.findIndexOfValue(summary);
                    mPushMode.setSummary(mPushMode.getEntries()[index]);
                    mPushMode.setValue(summary);
                    return false;
                }
            });
        } else {
            PreferenceScreen incomingPrefs = (PreferenceScreen) findPreference(PREFERENCE_SCREEN_INCOMING);
            incomingPrefs.removePreference((PreferenceScreen) findPreference(PREFERENCE_SCREEN_PUSH_ADVANCED));
            incomingPrefs.removePreference((ListPreference) findPreference(PREFERENCE_PUSH_MODE));
            mMainScreen.removePreference(mSearchScreen);
        }

        mAccountNotify = (SwitchPreference) findPreference(PREFERENCE_NOTIFY);
        mAccountNotify.setChecked(mAccount.isNotifyNewMail());

        mAccountNotifyNewMailMode = (ListPreference) findPreference(PREFERENCE_NOTIFY_NEW_MAIL_MODE);
        mAccountNotifyNewMailMode.setValue(mAccount.getFolderNotifyNewMailMode().name());
        mAccountNotifyNewMailMode.setSummary(mAccountNotifyNewMailMode.getEntry());
        mAccountNotifyNewMailMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mAccountNotifyNewMailMode.findIndexOfValue(summary);
                mAccountNotifyNewMailMode.setSummary(mAccountNotifyNewMailMode.getEntries()[index]);
                mAccountNotifyNewMailMode.setValue(summary);
                return false;
            }
        });

        mAccountNotifySelf = (SwitchPreference) findPreference(PREFERENCE_NOTIFY_SELF);
        mAccountNotifySelf.setChecked(mAccount.isNotifySelfNewMail());

        mAccountNotifySync = (SwitchPreference) findPreference(PREFERENCE_NOTIFY_SYNC);
        mAccountNotifySync.setChecked(mAccount.isShowOngoing());

        mAccountRingtone = (RingtonePreference) findPreference(PREFERENCE_RINGTONE);

        // XXX: The following two lines act as a workaround for the RingtonePreference
        //      which does not let us set/get the value programmatically
        SharedPreferences prefs = mAccountRingtone.getPreferenceManager().getSharedPreferences();
        String currentRingtone = (!mAccount.getNotificationSetting().shouldRing() ? null : mAccount.getNotificationSetting().getRingtone());
        prefs.edit().putString(PREFERENCE_RINGTONE, currentRingtone).commit();

        mAccountVibrate = (SwitchPreference) findPreference(PREFERENCE_VIBRATE);
        mAccountVibrate.setChecked(mAccount.getNotificationSetting().shouldVibrate());

        mAccountVibratePattern = (ListPreference) findPreference(PREFERENCE_VIBRATE_PATTERN);
        mAccountVibratePattern.setValue(String.valueOf(mAccount.getNotificationSetting().getVibratePattern()));
        mAccountVibratePattern.setSummary(mAccountVibratePattern.getEntry());
        mAccountVibratePattern.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mAccountVibratePattern.findIndexOfValue(summary);
                mAccountVibratePattern.setSummary(mAccountVibratePattern.getEntries()[index]);
                mAccountVibratePattern.setValue(summary);
                doVibrateTest(preference);
                return false;
            }
        });

        mAccountVibrateTimes = (ListPreference) findPreference(PREFERENCE_VIBRATE_TIMES);
        mAccountVibrateTimes.setValue(String.valueOf(mAccount.getNotificationSetting().getVibrateTimes()));
        mAccountVibrateTimes.setSummary(String.valueOf(mAccount.getNotificationSetting().getVibrateTimes()));
        mAccountVibrateTimes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String value = newValue.toString();
                mAccountVibrateTimes.setSummary(value);
                mAccountVibrateTimes.setValue(value);
                doVibrateTest(preference);
                return false;
            }
        });

        mAccountLed = (SwitchPreference) findPreference(PREFERENCE_NOTIFICATION_LED);
        mAccountLed.setChecked(mAccount.getNotificationSetting().isLed());

        mNotificationOpensUnread = (SwitchPreference) findPreference(PREFERENCE_NOTIFICATION_OPENS_UNREAD);
        mNotificationOpensUnread.setChecked(mAccount.goToUnreadMessageSearch());

        new PopulateFolderPrefsTask().execute();

        mChipColor = findPreference(PREFERENCE_CHIP_COLOR);
        mChipColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onChooseChipColor();
                return false;
            }
        });

        mLedColor = findPreference(PREFERENCE_LED_COLOR);
        mLedColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onChooseLedColor();
                return false;
            }
        });

        findPreference(PREFERENCE_COMPOSITION).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        onCompositionSettings();
                        return true;
                    }
                });

        findPreference(PREFERENCE_INCOMING).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        mIncomingChanged = true;
                        onIncomingSettings();
                        return true;
                    }
                });

        findPreference(PREFERENCE_OUTGOING).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        onOutgoingSettings();
                        return true;
                    }
                });

        mHasCrypto = OpenPgpUtils.isAvailable(mContext);
        if (mHasCrypto) {
            mCryptoApp = (OpenPgpAppPreference) findPreference(PREFERENCE_CRYPTO_APP);
            mCryptoKey = (OpenPgpKeyPreference) findPreference(PREFERENCE_CRYPTO_KEY);

            mCryptoApp.setValue(String.valueOf(mAccount.getPgpApp()));
            mCryptoApp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String value = newValue.toString();
                    mCryptoApp.setValue(value);

                    mCryptoKey.setOpenPgpProvider(value);
                    return false;
                }
            });

            mCryptoKey.setValue(mAccount.getPgpKey());
            mCryptoKey.setOpenPgpProvider(mCryptoApp.getValue());
            // TODO: other identities?
            mCryptoKey.setDefaultUserId(OpenPgpApiHelper.buildUserId(mAccount.getIdentity(0)));
            mCryptoKey.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    long value = (Long) newValue;
                    mCryptoKey.setValue(value);
                    return false;
                }
            });
        } else {
            final Preference mCryptoApp = findPreference(PREFERENCE_CRYPTO_APP);
            mCryptoApp.setEnabled(false);
            mCryptoApp.setSummary(R.string.account_settings_no_openpgp_provider_installed);
            final Preference mCryptoKey = findPreference(PREFERENCE_CRYPTO_KEY);
            mCryptoKey.setEnabled(false);
            mCryptoKey.setSummary(R.string.account_settings_no_openpgp_provider_installed);
        }

        PackageManager packageManager = mContext.getPackageManager();
        Intent smime = new Intent(SMimeApi.SERVICE_INTENT);
        List<ResolveInfo> activities = packageManager.queryIntentServices(smime, 0);
        mSmimeApp = (ListPreference) findPreference(PREFERENCE_CRYPTO_SMIME_APP);

        if (activities.size() > 0) {
            final ArrayList<String> names = new ArrayList<>();
            final ArrayList<String> values = new ArrayList<>();
            final String none = "None";
            names.add(none);
            values.add(none);

            for (ResolveInfo ri : activities) {
                if (ri.serviceInfo != null) {
                    names.add(ri.serviceInfo.loadLabel(packageManager).toString());
                    values.add(ri.serviceInfo.packageName);
                }
            }

            mSmimeApp.setEntries(names.toArray(new String[names.size()]));
            mSmimeApp.setEntryValues(values.toArray(new String[values.size()]));
            mSmimeApp.setDefaultValue(none);
            mSmimeApp.setSummary(none);

            final String smimeApp = mAccount.getSmimeProvider();
            if (smimeApp != null && !smimeApp.equals("")) {
                mSmimeApp.setValue(smimeApp);
                int pos = values.indexOf(smimeApp);
                if (pos >= 0) {
                    String name = names.get(pos);
                    mSmimeApp.setSummary(name);
                }
            }

            mSmimeApp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String value = newValue.toString();
                    Log.d(K9.LOG_TAG, "Selected " + value);
                    mSmimeApp.setValue(value);
                    int pos = values.indexOf(value);
                    String name = names.get(pos);
                    mSmimeApp.setSummary(name);
                    return false;
                }
            });
        } else {
            mSmimeApp.setSummary(getString(R.string.account_settings_smime_app_not_found));
            mSmimeApp.setEnabled(false);
        }

        defaultCryptoProvider = (ListPreference) findPreference("default_crypto");
        if (defaultCryptoProvider != null) {
            defaultCryptoProvider.setValue(mAccount.getDefaultCryptoProvider());
            defaultCryptoProvider.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String value = newValue.toString();
                    defaultCryptoProvider.setValue(value);

                    return false;
                }
            });
        }
    }

    @Override
    public void onPause() {
        saveSettings();
        super.onPause();
    }

    private void removeListEntry(ListPreference listPreference, String remove) {
        CharSequence[] entryValues = listPreference.getEntryValues();
        CharSequence[] entries = listPreference.getEntries();

        CharSequence[] newEntryValues = new String[entryValues.length - 1];
        CharSequence[] newEntries = new String[entryValues.length - 1];

        for (int i = 0, out = 0; i < entryValues.length; i++) {
            CharSequence value = entryValues[i];
            if (!value.equals(remove)) {
                newEntryValues[out] = value;
                newEntries[out] = entries[i];
                out++;
            }
        }

        listPreference.setEntryValues(newEntryValues);
        listPreference.setEntries(newEntries);
    }

    private void saveSettings() {
        if (mAccountDefault.isChecked()) {
            Preferences.getPreferences(mContext).setDefaultAccount(mAccount);
        }

        mAccount.setDescription(mAccountDescription.getText());
        mAccount.setNotifyNewMail(mAccountNotify.isChecked());
        mAccount.setFolderNotifyNewMailMode(FolderMode.valueOf(mAccountNotifyNewMailMode.getValue()));
        mAccount.setNotifySelfNewMail(mAccountNotifySelf.isChecked());
        mAccount.setShowOngoing(mAccountNotifySync.isChecked());
        mAccount.setDisplayCount(Integer.parseInt(mDisplayCount.getValue()));
        mAccount.setMaximumAutoDownloadMessageSize(Integer.parseInt(mMessageSize.getValue()));
        if (mAccount.isSearchByDateCapable()) {
            mAccount.setMaximumPolledMessageAge(Integer.parseInt(mMessageAge.getValue()));
        }

        mAccount.getNotificationSetting().setVibrate(mAccountVibrate.isChecked());
        mAccount.getNotificationSetting().setVibratePattern(Integer.parseInt(mAccountVibratePattern.getValue()));
        mAccount.getNotificationSetting().setVibrateTimes(Integer.parseInt(mAccountVibrateTimes.getValue()));
        mAccount.getNotificationSetting().setLed(mAccountLed.isChecked());
        mAccount.setGoToUnreadMessageSearch(mNotificationOpensUnread.isChecked());
        mAccount.setFolderTargetMode(FolderMode.valueOf(mTargetMode.getValue()));
        mAccount.setDeletePolicy(DeletePolicy.fromInt(Integer.parseInt(mDeletePolicy.getValue())));
        if (mIsExpungeCapable) {
            mAccount.setExpungePolicy(Expunge.valueOf(mExpungePolicy.getValue()));
        }

        mAccount.setSyncRemoteDeletions(mSyncRemoteDeletions.isChecked());
        mAccount.setSearchableFolders(Searchable.valueOf(mSearchableFolders.getValue()));
        mAccount.setMessageFormat(MessageFormat.valueOf(mMessageFormat.getValue()));
        mAccount.setAlwaysShowCcBcc(false);
        mAccount.setMessageReadReceipt(mMessageReadReceipt.isChecked());
        mAccount.setQuoteStyle(QuoteStyle.valueOf(mQuoteStyle.getValue()));
        mAccount.setQuotePrefix(mAccountQuotePrefix.getText());
        mAccount.setDefaultQuotedTextShown(mAccountDefaultQuotedTextShown.isChecked());
        mAccount.setLocalStorageProviderId(mLocalStorageProvider.getValue());

        if (mHasCrypto) {
            mAccount.setPgpApp(mCryptoApp.getValue());
            mAccount.setPgpKey(mCryptoKey.getValue());
        }

        // In webdav account we use the exact folder name also for inbox,
        // since it varies because of internationalization
        if (mAccount.getStoreUri().startsWith("webdav")) {
            mAccount.setAutoExpandFolderName(mAutoExpandFolder.getValue());
        } else {
            mAccount.setAutoExpandFolderName(reverseTranslateFolder(mAutoExpandFolder.getValue()));
        }

        if (mIsMoveCapable) {
            mAccount.setArchiveFolderName(mArchiveFolder.getValue());
            mAccount.setDraftsFolderName(mDraftsFolder.getValue());
            mAccount.setSentFolderName(mSentFolder.getValue());
            mAccount.setSpamFolderName(mSpamFolder.getValue());
            mAccount.setTrashFolderName(mTrashFolder.getValue());
        }

        //IMAP stuff
        if (mIsPushCapable) {
            mAccount.setPushPollOnConnect(true);
            mAccount.setIdleRefreshMinutes(24); // TODO: move to constants, strings or something like that
            mAccount.setMaxPushFolders(10);
            mAccount.setAllowRemoteSearch(mCloudSearchEnabled.isChecked());
            mAccount.setRemoteSearchNumResults(Integer.parseInt(mRemoteSearchNumResults.getValue()));
            //mAccount.setRemoteSearchFullText(mRemoteSearchFullText.isChecked());
        }

        boolean needsRefresh = mAccount.setAutomaticCheckIntervalMinutes(Integer.parseInt(mCheckFrequency.getValue()));
        needsRefresh |= mAccount.setFolderSyncMode(FolderMode.valueOf(mSyncMode.getValue()));

        boolean displayModeChanged = mAccount.setFolderDisplayMode(FolderMode.valueOf(mDisplayMode.getValue()));

        SharedPreferences prefs = mAccountRingtone.getPreferenceManager().getSharedPreferences();
        String newRingtone = prefs.getString(PREFERENCE_RINGTONE, null);
        if (newRingtone != null) {
            mAccount.getNotificationSetting().setRing(true);
            mAccount.getNotificationSetting().setRingtone(newRingtone);
        } else {
            if (mAccount.getNotificationSetting().shouldRing()) {
                mAccount.getNotificationSetting().setRingtone(null);
            }
        }

        mAccount.setShowPictures(ShowPictures.valueOf(mAccountShowPictures.getValue()));

        //IMAP specific stuff
        if (mIsPushCapable) {
            boolean needsPushRestart = mAccount.setFolderPushMode(FolderMode.valueOf(mPushMode.getValue()));
            if (mAccount.getFolderPushMode() != FolderMode.NONE) {
                needsPushRestart |= displayModeChanged;
                needsPushRestart |= mIncomingChanged;
            }

            if (needsRefresh && needsPushRestart) {
                MailService.actionReset(mContext, null);
            } else if (needsRefresh) {
                MailService.actionReschedulePoll(mContext, null);
            } else if (needsPushRestart) {
                MailService.actionRestartPushers(mContext, null);
            }
        }

        mAccount.setSmimeProvider(mSmimeApp.getValue());
        mAccount.setDefaultCryptoProvider(defaultCryptoProvider.getValue());

        // TODO: refresh folder list here
        mAccount.save(Preferences.getPreferences(mContext));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mCryptoKey != null && mCryptoKey.handleOnActivityResult(requestCode, resultCode, data)) {
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SELECT_AUTO_EXPAND_FOLDER:
                    mAutoExpandFolder.setSummary(translateFolder(data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER)));
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onCompositionSettings() {
        AccountSetupComposition.actionEditCompositionSettings(mContext, mAccount);
    }

    private void onIncomingSettings() {
        AccountSetupIncoming.actionEditIncomingSettings(mContext, mAccount);
    }

    private void onOutgoingSettings() {
        AccountSetupOutgoing.actionEditOutgoingSettings(mContext, mAccount);
    }

    public void onChooseChipColor() {
        ColorPickerDialog dialog = ColorPickerDialog.newInstance(
                new ColorPickerDialog.OnColorChangedListener() {
                    public void colorChanged(int color) {
                        mAccount.setChipColor(color);
                    }
                },
                mAccount.getChipColor());

        dialog.show(getFragmentManager(), "colorPicker");
    }

    public void onChooseLedColor() {
        ColorPickerDialog dialog = ColorPickerDialog.newInstance(
                new ColorPickerDialog.OnColorChangedListener() {
                    public void colorChanged(int color) {
                        mAccount.getNotificationSetting().setLedColor(color);
                    }
                },
                mAccount.getNotificationSetting().getLedColor());

        dialog.show(getFragmentManager(), "colorPicker");
    }

    public void onChooseAutoExpandFolder() {
        Intent selectIntent = new Intent(mContext, ChooseFolder.class);
        selectIntent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());

        selectIntent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mAutoExpandFolder.getSummary());
        selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_CURRENT, "yes");
        selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_FOLDER_NONE, "yes");
        selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_DISPLAYABLE_ONLY, "yes");
        startActivityForResult(selectIntent, SELECT_AUTO_EXPAND_FOLDER);
    }

    private String translateFolder(String in) {
        if (mAccount.getInboxFolderName().equalsIgnoreCase(in)) {
            return getString(R.string.special_mailbox_name_inbox);
        } else {
            return in;
        }
    }

    private String reverseTranslateFolder(String in) {
        if (getString(R.string.special_mailbox_name_inbox).equals(in)) {
            return mAccount.getInboxFolderName();
        } else {
            return in;
        }
    }

    private void doVibrateTest(Preference preference) {
        // Do the vibration to show the user what it's like.
        Vibrator vibrate = (Vibrator) preference.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrate.vibrate(NotificationSetting.getVibration(
                Integer.parseInt(mAccountVibratePattern.getValue()),
                Integer.parseInt(mAccountVibrateTimes.getValue())), -1);
    }

    /**
     * Remote search result limit summary contains the current limit.  On load or change, update this value.
     *
     * @param maxResults Search limit to update the summary with.
     */
    private void updateRemoteSearchLimit(String maxResults) {
        if (maxResults != null) {
            if (maxResults.equals("0")) {
                maxResults = getString(R.string.account_settings_remote_search_num_results_entries_all);
            }

            mRemoteSearchNumResults.setSummary(String.format(getString(R.string.account_settings_remote_search_num_summary), maxResults));
        }
    }

    private class PopulateFolderPrefsTask extends AsyncTask<Void, Void, Void> {
        List<? extends Folder> folders = new LinkedList<LocalFolder>();
        String[] allFolderValues;
        String[] allFolderLabels;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                folders = mAccount.getLocalStore().getPersonalNamespaces(false);
            } catch (Exception e) {
                /// this can't be checked in
            }

            // TODO: In the future the call above should be changed to only return remote folders.
            // For now we just remove the Outbox folder if present.
            Iterator<? extends Folder> iter = folders.iterator();
            while (iter.hasNext()) {
                Folder folder = iter.next();
                if (mAccount.getOutboxFolderName().equals(folder.getName())) {
                    iter.remove();
                }
            }

            allFolderValues = new String[folders.size() + 1];
            allFolderLabels = new String[folders.size() + 1];

            allFolderValues[0] = K9.FOLDER_NONE;
            allFolderLabels[0] = K9.FOLDER_NONE;

            int i = 1;
            for (Folder folder : folders) {
                allFolderLabels[i] = folder.getName();
                allFolderValues[i] = folder.getName();
                i++;
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            mAutoExpandFolder = (ListPreference) findPreference(PREFERENCE_AUTO_EXPAND_FOLDER);
            mAutoExpandFolder.setEnabled(false);
            mArchiveFolder = (ListPreference) findPreference(PREFERENCE_ARCHIVE_FOLDER);
            mArchiveFolder.setEnabled(false);
            mDraftsFolder = (ListPreference) findPreference(PREFERENCE_DRAFTS_FOLDER);
            mDraftsFolder.setEnabled(false);
            mSentFolder = (ListPreference) findPreference(PREFERENCE_SENT_FOLDER);
            mSentFolder.setEnabled(false);
            mSpamFolder = (ListPreference) findPreference(PREFERENCE_SPAM_FOLDER);
            mSpamFolder.setEnabled(false);
            mTrashFolder = (ListPreference) findPreference(PREFERENCE_TRASH_FOLDER);
            mTrashFolder.setEnabled(false);

            if (!mIsMoveCapable) {
                PreferenceScreen foldersCategory =
                        (PreferenceScreen) findPreference(PREFERENCE_CATEGORY_FOLDERS);
                foldersCategory.removePreference(mArchiveFolder);
                foldersCategory.removePreference(mSpamFolder);
                foldersCategory.removePreference(mDraftsFolder);
                foldersCategory.removePreference(mSentFolder);
                foldersCategory.removePreference(mTrashFolder);
            }
        }

        @Override
        protected void onPostExecute(Void res) {
            initListPreference(mAutoExpandFolder, mAccount.getAutoExpandFolderName(), allFolderLabels, allFolderValues);
            mAutoExpandFolder.setEnabled(true);
            if (mIsMoveCapable) {
                initListPreference(mArchiveFolder, mAccount.getArchiveFolderName(), allFolderLabels, allFolderValues);
                initListPreference(mDraftsFolder, mAccount.getDraftsFolderName(), allFolderLabels, allFolderValues);
                initListPreference(mSentFolder, mAccount.getSentFolderName(), allFolderLabels, allFolderValues);
                initListPreference(mSpamFolder, mAccount.getSpamFolderName(), allFolderLabels, allFolderValues);
                initListPreference(mTrashFolder, mAccount.getTrashFolderName(), allFolderLabels, allFolderValues);
                mArchiveFolder.setEnabled(true);
                mSpamFolder.setEnabled(true);
                mDraftsFolder.setEnabled(true);
                mSentFolder.setEnabled(true);
                mTrashFolder.setEnabled(true);
            }
        }
    }
}

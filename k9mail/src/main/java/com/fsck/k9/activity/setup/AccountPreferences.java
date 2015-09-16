package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.RingtonePreference;
import android.support.annotation.StringRes;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.SwitchPreferenceCompat;
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
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.preferences.AppPreference;
import com.fsck.k9.preferences.OpenPgpKeyPreferenceCompat;
import com.fsck.k9.service.MailService;

import org.openintents.openpgp.util.OpenPgpUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.fau.cs.mad.smile.android.R;

public class AccountPreferences extends SmilePreferenceFragment {
    public interface AccountPreferenceFragmentCallback {
        /*
        workaround for startIntentSenderFromChild in OpenPgpKeyPreferenceCompat
         */
        void registerFragment(AccountPreferences fragment);
    }

    private static final String EXTRA_ACCOUNT = "account";

    private static final int SELECT_AUTO_EXPAND_FOLDER = 100;

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
    private static final String PREFERENCE_SYNC_REMOTE_DELETIONS = "account_sync_remote_deletions";
    private static final String PREFERENCE_CRYPTO = "crypto";
    private static final String PREFERENCE_CRYPTO_APP = "crypto_app";
    private static final String PREFERENCE_CRYPTO_KEY = "crypto_key";
    private static final String PREFERENCE_REMOTE_SEARCH_ENABLED = "remote_search_enabled";
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
    private ListPreference incoming_checkFrequency;
    private ListPreference incoming_displayCount;
    private ListPreference incoming_messageAge;
    private ListPreference incoming_messageSize;
    private SwitchPreferenceCompat mAccountDefault;
    private SwitchPreferenceCompat mAccountNotify;
    private ListPreference mAccountNotifyNewMailMode;
    private SwitchPreferenceCompat mAccountNotifySelf;
    private ListPreference mAccountShowPictures;
    private SwitchPreferenceCompat mAccountNotifySync;
    private SwitchPreferenceCompat mAccountVibrate;
    private SwitchPreferenceCompat mAccountLed;
    private ListPreference mAccountVibratePattern;
    private ListPreference mAccountVibrateTimes;
    private RingtonePreference mAccountRingtone;
    private ListPreference folder_displayMode;
    private ListPreference incoming_SyncMode;
    private ListPreference incoming_pushMode;
    private ListPreference folder_targetMode;
    private ListPreference incoming_deletePolicy;
    private ListPreference incoming_expungePolicy;
    private ListPreference folder_searchableFolders;
    private ListPreference mAutoExpandFolder;
    private Preference mChipColor;
    private Preference mLedColor;
    private boolean mIncomingChanged = false;
    private SwitchPreferenceCompat mNotificationOpensUnread;
    private ListPreference composing_messageFormat;
    private SwitchPreferenceCompat composing_messageReadReceipt;
    private ListPreference composing_quoteStyle;
    private EditTextPreference composing_accountQuotePrefix;
    private SwitchPreferenceCompat composing_accountDefaultQuotedTextShown;
    private SwitchPreferenceCompat incoming_syncRemoteDeletions;
    private boolean mHasCrypto = false;
    private AppPreference mCryptoApp;
    private OpenPgpKeyPreferenceCompat mCryptoKey;

    private PreferenceCategory mSearchScreen;
    private SwitchPreferenceCompat mCloudSearchEnabled;
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

    private AppPreference mSmimeApp;
    private ListPreference defaultCryptoProvider;

    private Context mContext;
    private AccountPreferenceFragmentCallback callback;

    public static AccountPreferences newInstance(Account account) {
        Bundle args = new Bundle();
        args.putString(EXTRA_ACCOUNT, account.getUuid());
        AccountPreferences settings = new AccountPreferences();
        settings.setArguments(args);
        return settings;
    }

    @Override
    public SmilePreferenceFragment openPreferenceScreen() {
        return newInstance(mAccount);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        super.onCreatePreferences(bundle, s);
        setPreferencesFromResource(R.xml.account_preferences, s);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        callback.registerFragment(this);

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

        //addPreferencesFromResource(R.xml.account_preferences);

        setupMainScreen();
        setupIncomingScreen();
        setupComposingScreen();
        setupFolderScreen();
        setupStorageProviderScreen();
        setupNotificationScreen();
    }

    private void setupNotificationScreen() {
        if (!isNotificationScreen()) {
            return;
        }

        mAccountNotify = (SwitchPreferenceCompat) findPreference(PREFERENCE_NOTIFY);
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

        mAccountNotifySelf = (SwitchPreferenceCompat) findPreference(PREFERENCE_NOTIFY_SELF);
        mAccountNotifySelf.setChecked(mAccount.isNotifySelfNewMail());

        mAccountNotifySync = (SwitchPreferenceCompat) findPreference(PREFERENCE_NOTIFY_SYNC);
        mAccountNotifySync.setChecked(mAccount.isShowOngoing());

        /*mAccountRingtone = (RingtonePreference) findPreference(PREFERENCE_RINGTONE);

        // XXX: The following two lines act as a workaround for the RingtonePreference
        //      which does not let us set/get the value programmatically
        SharedPreferences prefs = mAccountRingtone.getPreferenceManager().getSharedPreferences();
        String currentRingtone = (!mAccount.getNotificationSetting().shouldRing() ? null : mAccount.getNotificationSetting().getRingtone());
        prefs.edit().putString(PREFERENCE_RINGTONE, currentRingtone).commit();*/

        mAccountVibrate = (SwitchPreferenceCompat) findPreference(PREFERENCE_VIBRATE);
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

        mAccountLed = (SwitchPreferenceCompat) findPreference(PREFERENCE_NOTIFICATION_LED);
        mAccountLed.setChecked(mAccount.getNotificationSetting().isLed());

        mNotificationOpensUnread = (SwitchPreferenceCompat) findPreference(PREFERENCE_NOTIFICATION_OPENS_UNREAD);
        mNotificationOpensUnread.setChecked(mAccount.goToUnreadMessageSearch());

        mLedColor = findPreference(PREFERENCE_LED_COLOR);
        mLedColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onChooseLedColor();
                return false;
            }
        });
    }

    private void setupMainScreen() {
        if (!isMainScreen()) {
            return;
        }

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

        mChipColor = findPreference(PREFERENCE_CHIP_COLOR);
        mChipColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onChooseChipColor();
                return false;
            }
        });

        mAccountDefault = (SwitchPreferenceCompat) findPreference(PREFERENCE_DEFAULT);
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

        setupCryptoCategory();

        // IMAP-specific preferences

        mCloudSearchEnabled = (SwitchPreferenceCompat) findPreference(PREFERENCE_REMOTE_SEARCH_ENABLED);
        mRemoteSearchNumResults = (ListPreference) findPreference(PREFERENCE_REMOTE_SEARCH_NUM_RESULTS);
        mRemoteSearchNumResults.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
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
        } else {
            final String searchCategoryKey = mContext.getString(R.string.account_preferences_search_key);
            Preference searchCategory = findPreference(searchCategoryKey);
            getPreferenceScreen().removePreference(searchCategory);
        }
    }

    private void setupCryptoCategory() {
        mHasCrypto = OpenPgpUtils.isAvailable(mContext);
        if (mHasCrypto) {

            mCryptoApp = (AppPreference) findPreference(PREFERENCE_CRYPTO_APP);
            mCryptoKey = (OpenPgpKeyPreferenceCompat) findPreference(PREFERENCE_CRYPTO_KEY);
            mCryptoKey.setActivity(getActivity());
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

        mSmimeApp = (AppPreference) findPreference(PREFERENCE_CRYPTO_SMIME_APP);
        mSmimeApp.setValue(String.valueOf(mAccount.getSmimeProvider()));
        mSmimeApp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = newValue.toString();
                mSmimeApp.setValue(value);
                return false;
            }
        });

        defaultCryptoProvider = (ListPreference) findPreference("default_crypto");
        if (defaultCryptoProvider != null) {
            defaultCryptoProvider.setValue(mAccount.getDefaultCryptoProvider());
            defaultCryptoProvider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String value = newValue.toString();
                    defaultCryptoProvider.setValue(value);

                    return false;
                }
            });
        }
    }

    private void setupIncomingScreen() {
        if (!isIncomingScreen()) {
            return;
        }

        incoming_displayCount = (ListPreference) findPreference(PREFERENCE_DISPLAY_COUNT);
        incoming_displayCount.setValue(String.valueOf(mAccount.getDisplayCount()));
        incoming_displayCount.setSummary(incoming_displayCount.getEntry());
        incoming_displayCount.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = incoming_displayCount.findIndexOfValue(summary);
                incoming_displayCount.setSummary(incoming_displayCount.getEntries()[index]);
                incoming_displayCount.setValue(summary);
                return false;
            }
        });

        incoming_messageAge = (ListPreference) findPreference(PREFERENCE_MESSAGE_AGE);

        if (!mAccount.isSearchByDateCapable()) {
            ((PreferenceScreen) findPreference(PREFERENCE_SCREEN_INCOMING)).removePreference(incoming_messageAge);
        } else {
            incoming_messageAge.setValue(String.valueOf(mAccount.getMaximumPolledMessageAge()));
            incoming_messageAge.setSummary(incoming_messageAge.getEntry());
            incoming_messageAge.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = incoming_messageAge.findIndexOfValue(summary);
                    incoming_messageAge.setSummary(incoming_messageAge.getEntries()[index]);
                    incoming_messageAge.setValue(summary);
                    return false;
                }
            });

        }

        incoming_messageSize = (ListPreference) findPreference(PREFERENCE_MESSAGE_SIZE);
        incoming_messageSize.setValue(String.valueOf(mAccount.getMaximumAutoDownloadMessageSize()));
        incoming_messageSize.setSummary(incoming_messageSize.getEntry());
        incoming_messageSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = incoming_messageSize.findIndexOfValue(summary);
                incoming_messageSize.setSummary(incoming_messageSize.getEntries()[index]);
                incoming_messageSize.setValue(summary);
                return false;
            }
        });

        incoming_checkFrequency = (ListPreference) findPreference(PREFERENCE_FREQUENCY);
        incoming_checkFrequency.setValue(String.valueOf(mAccount.getAutomaticCheckIntervalMinutes()));
        incoming_checkFrequency.setSummary(incoming_checkFrequency.getEntry());
        incoming_checkFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = incoming_checkFrequency.findIndexOfValue(summary);
                incoming_checkFrequency.setSummary(incoming_checkFrequency.getEntries()[index]);
                incoming_checkFrequency.setValue(summary);
                return false;
            }
        });

        incoming_SyncMode = (ListPreference) findPreference(PREFERENCE_SYNC_MODE);
        incoming_SyncMode.setValue(mAccount.getFolderSyncMode().name());
        incoming_SyncMode.setSummary(incoming_SyncMode.getEntry());
        incoming_SyncMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = incoming_SyncMode.findIndexOfValue(summary);
                incoming_SyncMode.setSummary(incoming_SyncMode.getEntries()[index]);
                incoming_SyncMode.setValue(summary);
                return false;
            }
        });

        if (mIsPushCapable) {
            incoming_pushMode = (ListPreference) findPreference(PREFERENCE_PUSH_MODE);
            incoming_pushMode.setValue(mAccount.getFolderPushMode().name());
            incoming_pushMode.setSummary(incoming_pushMode.getEntry());
            incoming_pushMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = incoming_pushMode.findIndexOfValue(summary);
                    incoming_pushMode.setSummary(incoming_pushMode.getEntries()[index]);
                    incoming_pushMode.setValue(summary);
                    return false;
                }
            });
        } else {
            getPreferenceScreen().removePreference(findPreference(PREFERENCE_PUSH_MODE));
        }

        incoming_syncRemoteDeletions = setupSwitchPreference(PREFERENCE_SYNC_REMOTE_DELETIONS, mAccount.syncRemoteDeletions());

        incoming_deletePolicy = (ListPreference) findPreference(PREFERENCE_DELETE_POLICY);
        if (!mIsSeenFlagSupported) {
            removeListEntry(incoming_deletePolicy, DeletePolicy.MARK_AS_READ.preferenceString());
        }

        incoming_deletePolicy.setValue(mAccount.getDeletePolicy().preferenceString());
        incoming_deletePolicy.setSummary(incoming_deletePolicy.getEntry());
        incoming_deletePolicy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = incoming_deletePolicy.findIndexOfValue(summary);
                incoming_deletePolicy.setSummary(incoming_deletePolicy.getEntries()[index]);
                incoming_deletePolicy.setValue(summary);
                return false;
            }
        });


        incoming_expungePolicy = (ListPreference) findPreference(PREFERENCE_EXPUNGE_POLICY);
        if (mIsExpungeCapable) {
            incoming_expungePolicy.setValue(mAccount.getExpungePolicy().name());
            incoming_expungePolicy.setSummary(incoming_expungePolicy.getEntry());
            incoming_expungePolicy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = incoming_expungePolicy.findIndexOfValue(summary);
                    incoming_expungePolicy.setSummary(incoming_expungePolicy.getEntries()[index]);
                    incoming_expungePolicy.setValue(summary);
                    return false;
                }
            });
        } else {
            getPreferenceScreen().removePreference(incoming_expungePolicy);
        }

        findPreference(PREFERENCE_INCOMING).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        mIncomingChanged = true;
                        onIncomingSettings();
                        return true;
                    }
                });
    }

    private void setupComposingScreen() {
        if (!isComposingScreen()) {
            return;
        }

        Preference composition = findPreference(PREFERENCE_COMPOSITION);
        composition.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        onCompositionSettings();
                        return true;
                    }
                });

        composing_messageFormat = (ListPreference) findPreference(PREFERENCE_MESSAGE_FORMAT);
        composing_messageFormat.setValue(mAccount.getMessageFormat().name());
        composing_messageFormat.setSummary(composing_messageFormat.getEntry());
        composing_messageFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = composing_messageFormat.findIndexOfValue(summary);
                composing_messageFormat.setSummary(composing_messageFormat.getEntries()[index]);
                composing_messageFormat.setValue(summary);
                return false;
            }
        });

        composing_messageReadReceipt = (SwitchPreferenceCompat) findPreference(PREFERENCE_MESSAGE_READ_RECEIPT);
        composing_messageReadReceipt.setChecked(mAccount.isMessageReadReceiptAlways());

        composing_accountQuotePrefix = (EditTextPreference) findPreference(PREFERENCE_QUOTE_PREFIX);
        composing_accountQuotePrefix.setSummary(mAccount.getQuotePrefix());
        composing_accountQuotePrefix.setText(mAccount.getQuotePrefix());
        composing_accountQuotePrefix.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String value = newValue.toString();
                composing_accountQuotePrefix.setSummary(value);
                composing_accountQuotePrefix.setText(value);
                return false;
            }
        });

        composing_accountDefaultQuotedTextShown = setupSwitchPreference(PREFERENCE_DEFAULT_QUOTED_TEXT_SHOWN, mAccount.isDefaultQuotedTextShown());

        mComposingScreen = (PreferenceScreen) findPreference(PREFERENCE_SCREEN_COMPOSING);

        Preference.OnPreferenceChangeListener quoteStyleListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final QuoteStyle style = QuoteStyle.valueOf(newValue.toString());
                int index = composing_quoteStyle.findIndexOfValue(newValue.toString());
                composing_quoteStyle.setSummary(composing_quoteStyle.getEntries()[index]);

                if (style == QuoteStyle.PREFIX) {
                    mComposingScreen.addPreference(composing_accountQuotePrefix);
                } else if (style == QuoteStyle.HEADER) {
                    mComposingScreen.removePreference(composing_accountQuotePrefix);
                }
                return true;
            }
        };

        composing_quoteStyle = (ListPreference) findPreference(PREFERENCE_QUOTE_STYLE);
        composing_quoteStyle.setValue(mAccount.getQuoteStyle().name());
        composing_quoteStyle.setSummary(composing_quoteStyle.getEntry());
        composing_quoteStyle.setOnPreferenceChangeListener(quoteStyleListener);
        // Call the onPreferenceChange() handler on startup to update the Preference dialogue based
        // upon the existing quote style setting.
        quoteStyleListener.onPreferenceChange(composing_quoteStyle, mAccount.getQuoteStyle().name());

        Preference outgoing = findPreference(PREFERENCE_OUTGOING);
        outgoing.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        onOutgoingSettings();
                        return true;
                    }
                });
    }

    private void setupFolderScreen() {
        if (!isFolderScreen()) {
            return;
        }

        new PopulateFolderPrefsTask().execute();
        folder_displayMode = (ListPreference) findPreference(PREFERENCE_DISPLAY_MODE);
        folder_displayMode.setValue(mAccount.getFolderDisplayMode().name());
        folder_displayMode.setSummary(folder_displayMode.getEntry());
        folder_displayMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = folder_displayMode.findIndexOfValue(summary);
                folder_displayMode.setSummary(folder_displayMode.getEntries()[index]);
                folder_displayMode.setValue(summary);
                return false;
            }
        });

        folder_targetMode = (ListPreference) findPreference(PREFERENCE_TARGET_MODE);
        folder_targetMode.setValue(mAccount.getFolderTargetMode().name());
        folder_targetMode.setSummary(folder_targetMode.getEntry());
        folder_targetMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = folder_targetMode.findIndexOfValue(summary);
                folder_targetMode.setSummary(folder_targetMode.getEntries()[index]);
                folder_targetMode.setValue(summary);
                return false;
            }
        });

        folder_searchableFolders = (ListPreference) findPreference(PREFERENCE_SEARCHABLE_FOLDERS);
        folder_searchableFolders.setValue(mAccount.getSearchableFolders().name());
        folder_searchableFolders.setSummary(folder_searchableFolders.getEntry());
        folder_searchableFolders.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = folder_searchableFolders.findIndexOfValue(summary);
                folder_searchableFolders.setSummary(folder_searchableFolders.getEntries()[index]);
                folder_searchableFolders.setValue(summary);
                return false;
            }
        });
    }

    private void setupStorageProviderScreen() {
        if (!isStorageProviderScreen()) {
            return;
        }

        mLocalStorageProvider = (ListPreference) findPreference(PREFERENCE_LOCAL_STORAGE_PROVIDER);

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

    private boolean isMainScreen() {
        return isScreen(R.string.account_preferences_main_key);
    }

    private boolean isIncomingScreen() {
        return isScreen(R.string.account_preferences_incoming_key);
    }

    private boolean isComposingScreen() {
        return isScreen(R.string.account_preferences_composing_key);
    }

    private boolean isFolderScreen() {
        return isScreen(R.string.account_preferences_folders_key);
    }

    private boolean isStorageProviderScreen() {
        return isScreen(R.string.account_preferences_storage_provider_key);
    }

    private boolean isNotificationScreen() {
        return isScreen(R.string.account_preferences_notifications_key);
    }

    private boolean isScreen(@StringRes int resId) {
        String key = getPreferenceScreen().getKey();
        String screen = mContext.getString(resId);

        return screen.equals(key);
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
        saveMainScreen();
        saveIncomingScreen();
        saveComposingScreen();
        saveFolderScreen();
        saveStorageProviderScreen();
        saveNotificationScreen();

        mAccount.setAlwaysShowCcBcc(false);

        boolean needsRefresh = false;
        boolean displayModeChanged = false;

        if(isFolderScreen()) {
            FolderMode mode = FolderMode.valueOf(folder_displayMode.getValue());
            displayModeChanged = mAccount.setFolderDisplayMode(mode);
        }

        if(isIncomingScreen()) {
            final int checkFrequency = Integer.parseInt(incoming_checkFrequency.getValue());
            needsRefresh = mAccount.setAutomaticCheckIntervalMinutes(checkFrequency);
            FolderMode mode = FolderMode.valueOf(incoming_SyncMode.getValue());
            needsRefresh |= mAccount.setFolderSyncMode(mode);

            //IMAP specific stuff
            if (mIsPushCapable) {

                boolean needsPushRestart = mAccount.setFolderPushMode(FolderMode.valueOf(incoming_pushMode.getValue()));
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
        }

        // TODO: refresh folder list here
        mAccount.save(Preferences.getPreferences(mContext));
    }

    private void saveMainScreen() {
        if (!isMainScreen()) {
            return;
        }

        if (mAccountDefault.isChecked()) {
            Preferences.getPreferences(mContext).setDefaultAccount(mAccount);
        }

        mAccount.setShowPictures(ShowPictures.valueOf(mAccountShowPictures.getValue()));

        mAccount.setDescription(mAccountDescription.getText());
        if (mHasCrypto) {
            mAccount.setPgpApp(mCryptoApp.getValue());
            mAccount.setPgpKey(mCryptoKey.getValue());
        }

        mAccount.setSmimeProvider(mSmimeApp.getValue());
        mAccount.setDefaultCryptoProvider(defaultCryptoProvider.getValue());

        //IMAP stuff
        if (mIsPushCapable) {
            mAccount.setPushPollOnConnect(true);
            mAccount.setIdleRefreshMinutes(24); // TODO: move to constants, strings or something like that
            mAccount.setMaxPushFolders(10);
            mAccount.setAllowRemoteSearch(mCloudSearchEnabled.isChecked());
            mAccount.setRemoteSearchNumResults(Integer.parseInt(mRemoteSearchNumResults.getValue()));
            //mAccount.setRemoteSearchFullText(mRemoteSearchFullText.isChecked());
        }
    }

    private void saveIncomingScreen() {
        if(!isIncomingScreen()) {
            return;
        }

        mAccount.setDisplayCount(Integer.parseInt(incoming_displayCount.getValue()));
        mAccount.setMaximumAutoDownloadMessageSize(Integer.parseInt(incoming_messageSize.getValue()));
        if (mAccount.isSearchByDateCapable()) {
            mAccount.setMaximumPolledMessageAge(Integer.parseInt(incoming_messageAge.getValue()));
        }

        mAccount.setDeletePolicy(DeletePolicy.fromInt(Integer.parseInt(incoming_deletePolicy.getValue())));

        if (mIsExpungeCapable) {
            mAccount.setExpungePolicy(Expunge.valueOf(incoming_expungePolicy.getValue()));
        }

        mAccount.setSyncRemoteDeletions(incoming_syncRemoteDeletions.isChecked());
    }

    private void saveComposingScreen() {
        if(!isComposingScreen()) {
            return;
        }

        mAccount.setMessageFormat(MessageFormat.valueOf(composing_messageFormat.getValue()));
        mAccount.setMessageReadReceipt(composing_messageReadReceipt.isChecked());
        mAccount.setQuoteStyle(QuoteStyle.valueOf(composing_quoteStyle.getValue()));
        mAccount.setQuotePrefix(composing_accountQuotePrefix.getText());
        mAccount.setDefaultQuotedTextShown(composing_accountDefaultQuotedTextShown.isChecked());
    }

    private void saveFolderScreen() {
        if(!isFolderScreen()) {
            return;
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

        mAccount.setFolderTargetMode(FolderMode.valueOf(folder_targetMode.getValue()));
        mAccount.setSearchableFolders(Searchable.valueOf(folder_searchableFolders.getValue()));
    }

    private void saveStorageProviderScreen() {
        if(!isStorageProviderScreen()) {
            return;
        }

        mAccount.setLocalStorageProviderId(mLocalStorageProvider.getValue());
    }

    private void saveNotificationScreen() {
        if (!isNotificationScreen()) {
            return;
        }

        //Notification
        mAccount.setNotifyNewMail(mAccountNotify.isChecked());
        mAccount.setFolderNotifyNewMailMode(FolderMode.valueOf(mAccountNotifyNewMailMode.getValue()));
        mAccount.setNotifySelfNewMail(mAccountNotifySelf.isChecked());
        mAccount.setShowOngoing(mAccountNotifySync.isChecked());
        mAccount.getNotificationSetting().setVibrate(mAccountVibrate.isChecked());
        mAccount.getNotificationSetting().setVibratePattern(Integer.parseInt(mAccountVibratePattern.getValue()));
        mAccount.getNotificationSetting().setVibrateTimes(Integer.parseInt(mAccountVibrateTimes.getValue()));
        mAccount.getNotificationSetting().setLed(mAccountLed.isChecked());
        mAccount.setGoToUnreadMessageSearch(mNotificationOpensUnread.isChecked());

        /*SharedPreferences prefs = mAccountRingtone.getPreferenceManager().getSharedPreferences();
        String newRingtone = prefs.getString(PREFERENCE_RINGTONE, null);
        if (newRingtone != null) {
            mAccount.getNotificationSetting().setRing(true);
            mAccount.getNotificationSetting().setRingtone(newRingtone);
        } else {
            if (mAccount.getNotificationSetting().shouldRing()) {
                mAccount.getNotificationSetting().setRingtone(null);
            }
        }*/
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

    @Override
    public void onAttach(Activity activity) {
        try {
            callback = (AccountPreferenceFragmentCallback)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("you need to implement AccountPreferenceFragmentCallback");
        }

        super.onAttach(activity);
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
        List<? extends Folder> folders = new LinkedList<>();
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

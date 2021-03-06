package com.fsck.k9.activity;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Account.MessageFormat;
import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.FontSizes;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.misc.AttachmentContentLoaderCallback;
import com.fsck.k9.activity.misc.AttachmentInfoLoaderCallback;
import com.fsck.k9.activity.misc.Attachment;
import com.fsck.k9.activity.misc.AttachmentMessageComposeHelper;
import com.fsck.k9.adapter.IdentityAdapter;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.OpenPgpMessageCompose;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.crypto.SmimeMessageCompose;
import com.fsck.k9.fragment.ProgressDialogFragment;
import com.fsck.k9.helper.ContactItem;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.helper.IdentityHelper;
import com.fsck.k9.helper.SimpleTextWatcher;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.message.IdentityField;
import com.fsck.k9.message.IdentityHeaderParser;
import com.fsck.k9.message.InsertableHtmlContent;
import com.fsck.k9.message.MessageBuilder;
import com.fsck.k9.message.QuotedTextMode;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.ui.EolConvertingEditText;
import com.fsck.k9.view.MessageComposeRecipient;
import com.fsck.k9.view.MessageWebView;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fau.cs.mad.smile.android.R;
import de.fau.cs.mad.smime_api.ISMimeService;
import de.fau.cs.mad.smime_api.SMimeApi;
import de.fau.cs.mad.smime_api.SMimeServiceConnection;

public class MessageCompose extends K9Activity implements View.OnClickListener,
        ProgressDialogFragment.CancelListener {

    private static final long INVALID_DRAFT_ID = MessagingController.INVALID_MESSAGE_ID;

    private static final String ACTION_COMPOSE = "com.fsck.k9.intent.action.COMPOSE";
    private static final String ACTION_REPLY = "com.fsck.k9.intent.action.REPLY";
    private static final String ACTION_REPLY_ALL = "com.fsck.k9.intent.action.REPLY_ALL";
    private static final String ACTION_FORWARD = "com.fsck.k9.intent.action.FORWARD";
    private static final String ACTION_EDIT_DRAFT = "com.fsck.k9.intent.action.EDIT_DRAFT";

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MESSAGE_BODY  = "messageBody";
    private static final String EXTRA_MESSAGE_REFERENCE = "message_reference";

    private static final String STATE_KEY_ATTACHMENTS =
        "com.fsck.k9.activity.MessageCompose.attachments";
    private static final String STATE_KEY_CC_SHOWN =
        "com.fsck.k9.activity.MessageCompose.ccShown";
    private static final String STATE_KEY_BCC_SHOWN =
        "com.fsck.k9.activity.MessageCompose.bccShown";
    private static final String STATE_KEY_QUOTED_TEXT_MODE =
        "com.fsck.k9.activity.MessageCompose.QuotedTextShown";
    private static final String STATE_KEY_SOURCE_MESSAGE_PROCED =
        "com.fsck.k9.activity.MessageCompose.stateKeySourceMessageProced";
    private static final String STATE_KEY_DRAFT_ID = "com.fsck.k9.activity.MessageCompose.draftId";
    private static final String STATE_KEY_HTML_QUOTE = "com.fsck.k9.activity.MessageCompose.HTMLQuote";
    private static final String STATE_IDENTITY_CHANGED =
        "com.fsck.k9.activity.MessageCompose.identityChanged";
    private static final String STATE_IDENTITY =
        "com.fsck.k9.activity.MessageCompose.identity";
    private static final String STATE_PGP_DATA = "pgpData";
    private static final String STATE_IN_REPLY_TO = "com.fsck.k9.activity.MessageCompose.inReplyTo";
    private static final String STATE_REFERENCES = "com.fsck.k9.activity.MessageCompose.references";
    private static final String STATE_KEY_READ_RECEIPT = "com.fsck.k9.activity.MessageCompose.messageReadReceipt";
    private static final String STATE_KEY_DRAFT_NEEDS_SAVING = "com.fsck.k9.activity.MessageCompose.mDraftNeedsSaving";
    private static final String STATE_KEY_FORCE_PLAIN_TEXT =
            "com.fsck.k9.activity.MessageCompose.forcePlainText";
    private static final String STATE_KEY_QUOTED_TEXT_FORMAT =
            "com.fsck.k9.activity.MessageCompose.quotedTextFormat";
    private static final String STATE_KEY_NUM_ATTACHMENTS_LOADING = "numAttachmentsLoading";
    private static final String STATE_KEY_WAITING_FOR_ATTACHMENTS = "waitingForAttachments";

    public static final String LOADER_ARG_ATTACHMENT = "attachment";

    private static final String FRAGMENT_WAITING_FOR_ATTACHMENT = "waitingForAttachment";

    /**
     * package accessible so handler can use the following constants
     */
    static final int MSG_PROGRESS_ON = 1;
    static final int MSG_PROGRESS_OFF = 2;
    static final int MSG_SKIPPED_ATTACHMENTS = 3;
    static final int MSG_SAVED_DRAFT = 4;
    static final int MSG_DISCARDED_DRAFT = 5;
    static final int MSG_PERFORM_STALLED_ACTION = 6;

    private static final int ACTIVITY_REQUEST_PICK_ATTACHMENT = 1;
    private static final int CONTACT_PICKER_TO = 4;
    private static final int CONTACT_PICKER_CC = 5;
    private static final int CONTACT_PICKER_BCC = 6;
    private static final int CONTACT_PICKER_TO2 = 7;
    private static final int CONTACT_PICKER_CC2 = 8;
    private static final int CONTACT_PICKER_BCC2 = 9;

    public static final int REQUEST_CODE_SIGN_ENCRYPT_OPENPGP = 12;

    /**
     * Regular expression to remove the first localized "Re:" prefix in subjects.
     *
     * Currently:
     * - "Aw:" (german: abbreviation for "Antwort")
     */
    private static final Pattern PREFIX = Pattern.compile("^AW[:\\s]\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * The account used for message composition.
     */
    private Account mAccount;
    private Contacts mContacts;

    /**
     * This identity's settings are used for message composition.
     * Note: This has to be an identity of the account {@link #mAccount}.
     */
    private Identity mIdentity;

    private boolean mIdentityChanged = false;
    private boolean mSignatureChanged = false;

    /**
     * Reference to the source message (in case of reply, forward, or edit
     * draft actions).
     */
    private MessageReference mMessageReference;

    private Message mSourceMessage;
    private MimeMessage currentMessage;

    /**
     * "Original" message body
     *
     * <p>
     * The contents of this string will be used instead of the body of a referenced message when
     * replying to or forwarding a message.<br>
     * Right now this is only used when replying to a signed or encrypted message. It then contains
     * the stripped/decrypted body of that message.
     * </p>
     * <p><strong>Note:</strong>
     * When this field is not {@code null} we assume that the message we are composing right now
     * should be encrypted.
     * </p>
     */
    private String mSourceMessageBody;

    /**
     * Indicates that the source message has been processed at least once and should not
     * be processed on any subsequent loads. This protects us from adding attachments that
     * have already been added from the restore of the view state.
     */
    private boolean mSourceMessageProcessed = false;
    private int mMaxLoaderId = 0;
    private MessagingController controller;
    private MenuItem sendMenuButton;

    enum Action {
        COMPOSE,
        REPLY,
        REPLY_ALL,
        FORWARD,
        EDIT_DRAFT
    }

    /**
     * Contains the action we're currently performing (e.g. replying to a message)
     */
    private Action mAction;

    private boolean mReadReceipt = false;
    private QuotedTextMode mQuotedTextMode = QuotedTextMode.NONE;

    /**
     * Contains the format of the quoted text (text vs. HTML).
     */
    private SimpleMessageFormat mQuotedTextFormat;

    /**
     * When this it {@code true} the message format setting is ignored and we're always sending
     * a text/plain message.
     */
    private boolean mForcePlainText = false;

    private Button mChooseIdentityButton;
    private MessageComposeRecipient mToView;
    private MessageComposeRecipient mCcView;
    private MessageComposeRecipient mBccView;
    private EditText mSubjectView;
    private EolConvertingEditText mSignatureView;
    private EolConvertingEditText mMessageContentView;
    private LinearLayout mAttachments;
    private Button mQuotedTextShow;
    private View mQuotedTextBar;
    private ImageButton mQuotedTextEdit;
    private EolConvertingEditText mQuotedText;
    private MessageWebView mQuotedHTML;
    private InsertableHtmlContent mQuotedHtmlContent;   // Container for HTML reply as it's being built.
    private CheckBox mCryptoSignatureCheckbox;
    private CheckBox mEncryptCheckbox;
    private TextView mCryptoSignatureUserId;
    private TextView mCryptoSignatureUserIdRest;
    private Spinner mSpinner;

    private PgpData mPgpData = null;
    private String mOpenPgpProvider;
    private String mSmimeProvider;
    private OpenPgpServiceConnection mOpenPgpServiceConnection;
    private SMimeServiceConnection mSmimeServiceConnection;
    private SMimeApi sMimeApi;
    private OpenPgpMessageCompose openPgpMessageCompose;

    private String mReferences;
    private String mInReplyTo;
    private Menu mMenu;

    private boolean mSourceProcessed = false;

    /**
     * The currently used message format.
     *
     * <p>
     * <strong>Note:</strong>
     * Don't modify this field directly. Use {@link #updateMessageFormat()}.
     * </p>
     */
    private SimpleMessageFormat mMessageFormat;

    private QuoteStyle mQuoteStyle;

    private boolean mDraftNeedsSaving = false;
    private boolean mPreventDraftSaving = false;

    /**
     * If this is {@code true} we don't save the message as a draft in {@link #onPause()}.
     */
    private boolean mIgnoreOnPause = false;

    /**
     * The database ID of this message's draft. This is used when saving drafts so the message in
     * the database is updated instead of being created anew. This property is INVALID_DRAFT_ID
     * until the first save.
     */
    private long mDraftId = INVALID_DRAFT_ID;

    /**
     * Number of attachments currently being fetched.
     */
    private int mNumAttachmentsLoading = 0;

    private enum WaitingAction {
        NONE,
        SEND,
        SAVE
    }

    private enum CryptoProvider {
        NONE("None"),
        PGP("OpenPGP"),
        SMIME("S/MIME");

        private final String name;
        private CryptoProvider(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * Specifies what action to perform once attachments have been fetched.
     */
    private WaitingAction mWaitingForAttachments = WaitingAction.NONE;

    private MessageComposeHandler mHandler = new MessageComposeHandler(this);
    private Listener mListener = new Listener();

    private FontSizes mFontSizes = K9.getFontSizes();
    private ContextThemeWrapper mThemeContext;
    private CryptoProvider selectedCryptoProvider = CryptoProvider.NONE;


    /**
     * Compose a new message using the given account. If account is null the default account
     * will be used.
     * @param context
     * @param account
     */
    public static void actionCompose(Context context, Account account) {
        String accountUuid = (account == null) ?
                Preferences.getPreferences(context).getDefaultAccount().getUuid() :
                account.getUuid();

        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_ACCOUNT, accountUuid);
        i.setAction(ACTION_COMPOSE);
        context.startActivity(i);
    }

    /**
     * Get intent for composing a new message as a reply to the given message. If replyAll is true
     * the function is reply all instead of simply reply.
     * @param context
     * @param message
     * @param replyAll
     * @param messageBody optional, for decrypted messages, null if it should be grabbed from the given message
     */
    public static Intent getActionReplyIntent(
            Context context,
            LocalMessage message,
            boolean replyAll,
            String messageBody) {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_MESSAGE_BODY, messageBody);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, message.makeMessageReference());
        if (replyAll) {
            i.setAction(ACTION_REPLY_ALL);
        } else {
            i.setAction(ACTION_REPLY);
        }
        return i;
    }

    public static Intent getActionReplyIntent(Context context, MessageReference messageReference) {
        Intent intent = new Intent(context, MessageCompose.class);
        intent.setAction(ACTION_REPLY);
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    /**
     * Compose a new message as a reply to the given message. If replyAll is true the function
     * is reply all instead of simply reply.
     * @param context
     * @param message
     * @param replyAll
     * @param messageBody optional, for decrypted messages, null if it should be grabbed from the given message
     */
    public static void actionReply(
        Context context,
        LocalMessage message,
        boolean replyAll,
        String messageBody) {
        context.startActivity(getActionReplyIntent(context, message, replyAll, messageBody));
    }

    /**
     * Compose a new message as a forward of the given message.
     * @param context
     * @param message
     * @param messageBody optional, for decrypted messages, null if it should be grabbed from the given message
     */
    public static void actionForward(
            Context context,
            LocalMessage message,
            String messageBody) {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_MESSAGE_BODY, messageBody);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, message.makeMessageReference());
        i.setAction(ACTION_FORWARD);
        context.startActivity(i);
    }

    /**
     * Continue composition of the given message. This action modifies the way this Activity
     * handles certain actions.
     * Save will attempt to replace the message in the given folder with the updated version.
     * Discard will delete the message from the given folder.
     * @param context
     * @param messageReference
     */
    public static void actionEditDraft(Context context, MessageReference messageReference) {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
        i.setAction(ACTION_EDIT_DRAFT);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        setupTheme();

        final Intent intent = getIntent();
        final String accountUuid = acquireExtras(intent);

        if (!ensureAccount(accountUuid)) {
            return;
        }

        if (mIdentity == null) {
            mIdentity = mAccount.getIdentity(0);
        }

        mReadReceipt = mAccount.isMessageReadReceiptAlways();
        mQuoteStyle = mAccount.getQuoteStyle();
        mOpenPgpProvider = mAccount.getOpenPgpProvider();
        mSmimeProvider = mAccount.getSmimeProvider();
        controller = MessagingController.getInstance(getApplication());

        acquireLayout();
        setupLayout();

        if (savedInstanceState != null) {
            /*
             * This data gets used in onCreate, so grab it here instead of onRestoreInstanceState
             */
            mSourceMessageProcessed = savedInstanceState.getBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED, false);
        }

        if (initFromIntent(intent)) {
            mAction = Action.COMPOSE;
            mDraftNeedsSaving = true;
        } else {
            String action = intent.getAction();
            if (ACTION_COMPOSE.equals(action)) {
                mAction = Action.COMPOSE;
            } else if (ACTION_REPLY.equals(action)) {
                mAction = Action.REPLY;
            } else if (ACTION_REPLY_ALL.equals(action)) {
                mAction = Action.REPLY_ALL;
            } else if (ACTION_FORWARD.equals(action)) {
                mAction = Action.FORWARD;
            } else if (ACTION_EDIT_DRAFT.equals(action)) {
                mAction = Action.EDIT_DRAFT;
            } else {
                // This shouldn't happen
                Log.w(K9.LOG_TAG, "MessageCompose was started with an unsupported action");
                mAction = Action.COMPOSE;
            }
        }

        updateFrom();

        if (!mSourceMessageProcessed) {
            updateSignature();

            if (mAction == Action.REPLY || mAction == Action.REPLY_ALL ||
                    mAction == Action.FORWARD || mAction == Action.EDIT_DRAFT) {
                /*
                 * If we need to load the message we add ourself as a message listener here
                 * so we can kick it off. Normally we add in onResume but we don't
                 * want to reload the message every time the activity is resumed.
                 * There is no harm in adding twice.
                 */
                controller.addListener(mListener);

                final Account account = Preferences.getPreferences(this).getAccount(mMessageReference.getAccountUuid());
                final String folderName = mMessageReference.getFolderName();
                final String sourceMessageUid = mMessageReference.getUid();
                controller.loadMessageForView(account, folderName, sourceMessageUid, null);
            }

            if (mAction != Action.EDIT_DRAFT) {
                mBccView.addRecipients(Address.parseUnencoded(mAccount.getAlwaysBcc()));
            }
        }

        if (mAction == Action.REPLY || mAction == Action.REPLY_ALL) {
            mMessageReference = mMessageReference.withModifiedFlag(Flag.ANSWERED);
        }

        if (mAction == Action.REPLY || mAction == Action.REPLY_ALL ||
                mAction == Action.EDIT_DRAFT) {
            //change focus to message body.
            mMessageContentView.requestFocus();
        } else {
            // TODO: check if recipient box receives focus
            // Explicitly set focus to "To:" input field (see issue 2998)
            mToView.requestFocus();
        }

        if (mAction == Action.FORWARD) {
            mMessageReference = mMessageReference.withModifiedFlag(Flag.FORWARDED);
        }

        final View mEncryptLayout = findViewById(R.id.layout_encrypt);

        initializeCrypto();

        if (isCryptoProviderEnabled()) {
            mCryptoSignatureCheckbox = (CheckBox)findViewById(R.id.cb_crypto_signature);
            final CompoundButton.OnCheckedChangeListener updateListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    updateMessageFormat();
                }
            };
            mCryptoSignatureCheckbox.setOnCheckedChangeListener(updateListener);
            mCryptoSignatureUserId = (TextView)findViewById(R.id.userId);
            mCryptoSignatureUserIdRest = (TextView)findViewById(R.id.userIdRest);
            mEncryptCheckbox = (CheckBox)findViewById(R.id.cb_encrypt);
            mEncryptCheckbox.setOnCheckedChangeListener(updateListener);

            if (mSourceMessageBody != null) {
                // mSourceMessageBody is set to something when replying to and forwarding decrypted
                // messages, so the sender probably wants the message to be encrypted.
                mEncryptCheckbox.setChecked(true);
            }

            // New OpenPGP Provider API

            // bind to service
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(mOpenPgpProvider != null && !mOpenPgpProvider.equals("")) {
                        mOpenPgpServiceConnection = new OpenPgpServiceConnection(getApplicationContext(), mOpenPgpProvider);
                        mOpenPgpServiceConnection.bindToService();
                    }

                    if(mSmimeProvider != null && !mSmimeProvider.equals("")) {
                        mSmimeServiceConnection = new SMimeServiceConnection(getApplicationContext(), mSmimeProvider, new SMimeServiceConnection.OnBound() {
                            @Override
                            public void onBound(ISMimeService service) {
                                sMimeApi = new SMimeApi(getApplicationContext(), service);
                            }

                            @Override
                            public void onError(Exception e) {
                                K9.logDebug( "smime api failed: ", e);
                            }
                        });
                        mSmimeServiceConnection.bindToService();
                    }
                }
            }).start();

            mEncryptLayout.setVisibility(View.VISIBLE);
            mCryptoSignatureCheckbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox checkBox = (CheckBox) v;
                    if (checkBox.isChecked()) {
                        mPreventDraftSaving = true;
                    }
                }
            });

            updateMessageFormat();
        } else {
            mEncryptLayout.setVisibility(View.GONE);
        }

        updateMessageFormat();
        setTitle();
    }

    private void setupLayout() {
        ImageButton mQuotedTextDelete = (ImageButton) findViewById(R.id.quoted_text_delete);

        TextWatcher draftNeedsChangingTextWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mDraftNeedsSaving = true;
            }
        };

        mToView.addTextChangedListener(draftNeedsChangingTextWatcher);
        mCcView.addTextChangedListener(draftNeedsChangingTextWatcher);
        mBccView.addTextChangedListener(draftNeedsChangingTextWatcher);
        mSubjectView.addTextChangedListener(draftNeedsChangingTextWatcher);

        mMessageContentView.addTextChangedListener(draftNeedsChangingTextWatcher);
        mQuotedText.addTextChangedListener(draftNeedsChangingTextWatcher);

        /* Yes, there really are people who ship versions of android without a contact picker */
        if (mContacts.hasContactPicker()) {
            mToView.setAddContactListener(new DoLaunchOnClickListener(CONTACT_PICKER_TO));
            mCcView.setAddContactListener(new DoLaunchOnClickListener(CONTACT_PICKER_CC));
            mBccView.setAddContactListener(new DoLaunchOnClickListener(CONTACT_PICKER_BCC));
        }
        /*
         * We set this to invisible by default. Other methods will turn it back on if it's
         * needed.
         */

        showOrHideQuotedText(QuotedTextMode.NONE);

        mQuotedTextShow.setOnClickListener(this);
        mQuotedTextEdit.setOnClickListener(this);
        mQuotedTextDelete.setOnClickListener(this);

        EolConvertingEditText upperSignature = (EolConvertingEditText)findViewById(R.id.upper_signature);
        EolConvertingEditText lowerSignature = (EolConvertingEditText)findViewById(R.id.lower_signature);

        TextWatcher signTextWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mDraftNeedsSaving = true;
                mSignatureChanged = true;
            }
        };

        if (mAccount.isSignatureBeforeQuotedText()) {
            mSignatureView = upperSignature;
            lowerSignature.setVisibility(View.GONE);
        } else {
            mSignatureView = lowerSignature;
            upperSignature.setVisibility(View.GONE);
        }

        mSignatureView.addTextChangedListener(signTextWatcher);

        if (!mIdentity.getSignatureUse()) {
            mSignatureView.setVisibility(View.GONE);
        }

        // Set font size of input controls
        int fontSize = mFontSizes.getMessageComposeInput();
        mFontSizes.setViewTextSize(mSubjectView, fontSize);
        mFontSizes.setViewTextSize(mMessageContentView, fontSize);
        mFontSizes.setViewTextSize(mQuotedText, fontSize);
        mFontSizes.setViewTextSize(mSignatureView, fontSize);

        List<CryptoProvider> cryptoProviders = new ArrayList<>();
        cryptoProviders.add(CryptoProvider.NONE);

        if(mOpenPgpProvider != null && mOpenPgpProvider.trim().length() > 0) {
            cryptoProviders.add(CryptoProvider.PGP);
        }

        if(mSmimeProvider != null && mSmimeProvider.trim().length() > 0) {
            cryptoProviders.add(CryptoProvider.SMIME);
        }

        ArrayAdapter<CryptoProvider> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cryptoProviders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCryptoProvider = (CryptoProvider) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCryptoProvider = CryptoProvider.NONE;
            }
        });

        for(int i = 0; i < mSpinner.getCount(); i++) {
            if(mSpinner.getItemAtPosition(i).toString().equals(mAccount.getDefaultCryptoProvider())) {
                mSpinner.setSelection(i);
                break;
            }
        }

        if(cryptoProviders.size() == 1) {
            mSpinner.setVisibility(View.GONE);
        }
    }

    private void acquireLayout() {
        mContacts = Contacts.getInstance(MessageCompose.this);

        mChooseIdentityButton = (Button) findViewById(R.id.identity);
        mChooseIdentityButton.setOnClickListener(this);

        if (mAccount.getIdentities().size() == 1 &&
                Preferences.getPreferences(this).getAvailableAccounts().size() == 1) {
            mChooseIdentityButton.setVisibility(View.GONE);
        }

        mToView = (MessageComposeRecipient) findViewById(R.id.to_wrapper);
        mCcView = (MessageComposeRecipient) findViewById(R.id.cc_wrapper);
        mBccView = (MessageComposeRecipient) findViewById(R.id.bcc_wrapper);
        mSubjectView = (EditText) findViewById(R.id.subject);
        mSubjectView.getInputExtras(true).putBoolean("allowEmoji", true);

        if (mAccount.isAlwaysShowCcBcc()) {
            onAddCcBcc();
        }

        mMessageContentView = (EolConvertingEditText)findViewById(R.id.message_content);
        mMessageContentView.getInputExtras(true).putBoolean("allowEmoji", true);

        mAttachments = (LinearLayout)findViewById(R.id.attachments);
        mQuotedTextShow = (Button)findViewById(R.id.quoted_text_show);
        mQuotedTextBar = findViewById(R.id.quoted_text_bar);
        mQuotedTextEdit = (ImageButton)findViewById(R.id.quoted_text_edit);

        mQuotedText = (EolConvertingEditText)findViewById(R.id.quoted_text);
        mQuotedText.getInputExtras(true).putBoolean("allowEmoji", true);

        mQuotedHTML = (MessageWebView) findViewById(R.id.quoted_html);
        mQuotedHTML.configure();
        // Disable the ability to click links in the quoted HTML page. I think this is a nice feature, but if someone
        // feels this should be a preference (or should go away all together), I'm ok with that too. -achen 20101130
        mQuotedHTML.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        });

        mSpinner = (Spinner)findViewById(R.id.crypto_provider);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    private boolean ensureAccount(final String accountUuid) {
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        if (mAccount == null) {
            mAccount = Preferences.getPreferences(this).getDefaultAccount();
        }

        if (mAccount == null) {
            /*
             * There are no accounts set up. This should not have happened. Prompt the
             * user to set up an account as an acceptable bailout.
             */
            startActivity(new Intent(this, Accounts.class));
            mDraftNeedsSaving = false;
            finish();
            return false;
        }

        return true;
    }

    private String acquireExtras(Intent intent) {
        mMessageReference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
        mSourceMessageBody = intent.getStringExtra(EXTRA_MESSAGE_BODY);

        if (K9.DEBUG && mSourceMessageBody != null) {
            Log.d(K9.LOG_TAG, "Composing message with explicitly specified message body.");
        }

        return (mMessageReference != null) ?
                                   mMessageReference.getAccountUuid() :
                                   intent.getStringExtra(EXTRA_ACCOUNT);
    }

    private void setupTheme() {
        setContentView(R.layout.message_compose);
        mThemeContext = new ContextThemeWrapper(this, K9.getK9ThemeResourceId(K9.getK9Theme()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mOpenPgpServiceConnection != null) {
            if(mOpenPgpServiceConnection.isBound()) {
                mOpenPgpServiceConnection.unbindFromService();
            }
        }

        if (mSmimeServiceConnection != null) {
            if(mSmimeServiceConnection.isBound()) {
                mSmimeServiceConnection.unbindFromService();
            }
        }
    }

    /**
     * Handle external intents that trigger the message compose activity.
     *
     * <p>
     * Supported external intents:
     * <ul>
     *   <li>{@link Intent#ACTION_VIEW}</li>
     *   <li>{@link Intent#ACTION_SENDTO}</li>
     *   <li>{@link Intent#ACTION_SEND}</li>
     *   <li>{@link Intent#ACTION_SEND_MULTIPLE}</li>
     * </ul>
     * </p>
     *
     * @param intent
     *         The (external) intent that started the activity.
     *
     * @return {@code true}, if this activity was started by an external intent. {@code false},
     *         otherwise.
     */
    private boolean initFromIntent(final Intent intent) {
        boolean startedByExternalIntent = false;
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SENDTO.equals(action)) {
            /*
             * Someone has clicked a mailto: link. The address is in the URI.
             */
            if (intent.getData() != null) {
                Uri uri = intent.getData();
                if ("mailto".equals(uri.getScheme())) {
                    initializeFromMailto(uri);
                }
            }

            /*
             * Note: According to the documentation ACTION_VIEW and ACTION_SENDTO don't accept
             * EXTRA_* parameters.
             * And previously we didn't process these EXTRAs. But it looks like nobody bothers to
             * read the official documentation and just copies wrong sample code that happens to
             * work with the AOSP Email application. And because even big players get this wrong,
             * we're now finally giving in and read the EXTRAs for those actions (below).
             */
        }

        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action) ||
                Intent.ACTION_SENDTO.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            startedByExternalIntent = true;

            /*
             * Note: Here we allow a slight deviation from the documented behavior.
             * EXTRA_TEXT is used as message body (if available) regardless of the MIME
             * type of the intent. In addition one or multiple attachments can be added
             * using EXTRA_STREAM.
             */
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            // Only use EXTRA_TEXT if the body hasn't already been set by the mailto URI
            if (text != null && mMessageContentView.getText().length() == 0) {
                mMessageContentView.setCharacters(text);
            }

            String type = intent.getType();
            if (Intent.ACTION_SEND.equals(action)) {
                Uri stream = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (stream != null) {
                    AttachmentMessageComposeHelper.addAttachment(this, stream, type, mAttachmentInfoLoaderCallback);
                }
            } else {
                List<Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (list != null) {
                    for (Parcelable parcelable : list) {
                        Uri stream = (Uri) parcelable;
                        if (stream != null) {
                            AttachmentMessageComposeHelper.addAttachment(this, stream, type, mAttachmentInfoLoaderCallback);
                        }
                    }
                }
            }

            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            // Only use EXTRA_SUBJECT if the subject hasn't already been set by the mailto URI
            if (subject != null && mSubjectView.getText().length() == 0) {
                mSubjectView.setText(subject);
            }

            String[] extraEmail = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
            String[] extraCc = intent.getStringArrayExtra(Intent.EXTRA_CC);
            String[] extraBcc = intent.getStringArrayExtra(Intent.EXTRA_BCC);

            if (extraEmail != null) {
                mToView.addRecipients(Arrays.asList(extraEmail));
            }

            boolean ccOrBcc = false;
            if (extraCc != null) {
                ccOrBcc |= mCcView.addRecipients(Arrays.asList(extraCc));
            }

            if (extraBcc != null) {
                ccOrBcc |= mBccView.addRecipients(Arrays.asList(extraBcc));
            }

            if (ccOrBcc) {
                // Display CC and BCC text fields if CC or BCC recipients were set by the intent.
                onAddCcBcc();
            }
        }

        return startedByExternalIntent;
    }

    private void initializeCrypto() {
        if (mPgpData != null) {
            return;
        }
        mPgpData = new PgpData();
    }

    /**
     * Fill the encrypt layout with the latest data about signature key and encryption keys.
     */
    public void updateEncryptLayout() {
        if (!isCryptoProviderEnabled()) {
            return;
        }

        if (!mPgpData.hasSignatureKey()) {
            mCryptoSignatureCheckbox.setText(R.string.btn_crypto_sign);
            mCryptoSignatureCheckbox.setChecked(false);
            mCryptoSignatureUserId.setVisibility(View.INVISIBLE);
            mCryptoSignatureUserIdRest.setVisibility(View.INVISIBLE);
        } else {
            // if a signature key is selected, then the checkbox itself has no text
            mCryptoSignatureCheckbox.setText("");
            mCryptoSignatureCheckbox.setChecked(true);
            mCryptoSignatureUserId.setVisibility(View.VISIBLE);
            mCryptoSignatureUserIdRest.setVisibility(View.VISIBLE);
            mCryptoSignatureUserId.setText(R.string.unknown_crypto_signature_user_id);
            mCryptoSignatureUserIdRest.setText("");

            String userId = mPgpData.getSignatureUserId();
            if (userId != null) {
                String chunks[] = mPgpData.getSignatureUserId().split(" <", 2);
                mCryptoSignatureUserId.setText(chunks[0]);
                if (chunks.length > 1) {
                    mCryptoSignatureUserIdRest.setText("<" + chunks[1]);
                }
            }
        }

        updateMessageFormat();
    }

    @Override
    public void onResume() {
        super.onResume();
        mIgnoreOnPause = false;
        controller.addListener(mListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        controller.removeListener(mListener);
        // Save email as draft when activity is changed (go to home screen, call received) or screen locked
        // don't do this if only changing orientations
        if (!mIgnoreOnPause && (getChangingConfigurations() & ActivityInfo.CONFIG_ORIENTATION) == 0) {
            saveIfNeeded();
        }
    }

    /**
     * The framework handles most of the fields, but we need to handle stuff that we
     * dynamically show and hide:
     * Attachment list,
     * Cc field,
     * Bcc field,
     * Quoted text,
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_KEY_NUM_ATTACHMENTS_LOADING, mNumAttachmentsLoading);
        outState.putString(STATE_KEY_WAITING_FOR_ATTACHMENTS, mWaitingForAttachments.name());
        outState.putParcelableArrayList(STATE_KEY_ATTACHMENTS, AttachmentMessageComposeHelper.createAttachmentList(mAttachments));
        outState.putBoolean(STATE_KEY_CC_SHOWN, mCcView.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_KEY_BCC_SHOWN, mBccView.getVisibility() == View.VISIBLE);
        outState.putSerializable(STATE_KEY_QUOTED_TEXT_MODE, mQuotedTextMode);
        outState.putBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED, mSourceMessageProcessed);
        outState.putLong(STATE_KEY_DRAFT_ID, mDraftId);
        outState.putSerializable(STATE_IDENTITY, mIdentity);
        outState.putBoolean(STATE_IDENTITY_CHANGED, mIdentityChanged);
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
        outState.putString(STATE_IN_REPLY_TO, mInReplyTo);
        outState.putString(STATE_REFERENCES, mReferences);
        outState.putSerializable(STATE_KEY_HTML_QUOTE, mQuotedHtmlContent);
        outState.putBoolean(STATE_KEY_READ_RECEIPT, mReadReceipt);
        outState.putBoolean(STATE_KEY_DRAFT_NEEDS_SAVING, mDraftNeedsSaving);
        outState.putBoolean(STATE_KEY_FORCE_PLAIN_TEXT, mForcePlainText);
        outState.putSerializable(STATE_KEY_QUOTED_TEXT_FORMAT, mQuotedTextFormat);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mAttachments.removeAllViews();
        mMaxLoaderId = 0;

        mNumAttachmentsLoading = savedInstanceState.getInt(STATE_KEY_NUM_ATTACHMENTS_LOADING);
        mWaitingForAttachments = WaitingAction.NONE;
        try {
            String waitingFor = savedInstanceState.getString(STATE_KEY_WAITING_FOR_ATTACHMENTS);
            mWaitingForAttachments = WaitingAction.valueOf(waitingFor);
        } catch (Exception e) {
            Log.w(K9.LOG_TAG, "Couldn't read value \" + STATE_KEY_WAITING_FOR_ATTACHMENTS +" +
                    "\" from saved instance state", e);
        }

        List<Attachment> attachments = savedInstanceState.getParcelableArrayList(STATE_KEY_ATTACHMENTS);
        for (Attachment attachment : attachments) {
            AttachmentMessageComposeHelper.addAttachmentView(this, mAttachments, attachment);
            if (attachment.loaderId > mMaxLoaderId) {
                mMaxLoaderId = attachment.loaderId;
            }

            if (attachment.state == Attachment.LoadingState.URI_ONLY) {
                AttachmentMessageComposeHelper.initAttachmentInfoLoader(this, mAttachmentInfoLoaderCallback, attachment);
            } else if (attachment.state == Attachment.LoadingState.METADATA) {
                AttachmentMessageComposeHelper.initAttachmentContentLoader(this, mAttachmentContentLoaderCallback, attachment);
            }
        }

        mReadReceipt = savedInstanceState
                       .getBoolean(STATE_KEY_READ_RECEIPT);
        mCcView.setVisibility(savedInstanceState.getBoolean(STATE_KEY_CC_SHOWN) ? View.VISIBLE
                                 : View.GONE);
        mBccView.setVisibility(savedInstanceState
                                  .getBoolean(STATE_KEY_BCC_SHOWN) ? View.VISIBLE : View.GONE);

        // This method is called after the action bar menu has already been created and prepared.
        // So compute the visibility of the "Add Cc/Bcc" menu item again.
        computeAddCcBccVisibility();

        mQuotedHtmlContent =
                (InsertableHtmlContent) savedInstanceState.getSerializable(STATE_KEY_HTML_QUOTE);
        if (mQuotedHtmlContent != null && mQuotedHtmlContent.getQuotedContent() != null) {
            mQuotedHTML.setText(mQuotedHtmlContent.getQuotedContent());
        }

        mDraftId = savedInstanceState.getLong(STATE_KEY_DRAFT_ID);
        mIdentity = (Identity)savedInstanceState.getSerializable(STATE_IDENTITY);
        mIdentityChanged = savedInstanceState.getBoolean(STATE_IDENTITY_CHANGED);
        mPgpData = (PgpData) savedInstanceState.getSerializable(STATE_PGP_DATA);
        mInReplyTo = savedInstanceState.getString(STATE_IN_REPLY_TO);
        mReferences = savedInstanceState.getString(STATE_REFERENCES);
        mDraftNeedsSaving = savedInstanceState.getBoolean(STATE_KEY_DRAFT_NEEDS_SAVING);
        mForcePlainText = savedInstanceState.getBoolean(STATE_KEY_FORCE_PLAIN_TEXT);
        mQuotedTextFormat = (SimpleMessageFormat) savedInstanceState.getSerializable(
                STATE_KEY_QUOTED_TEXT_FORMAT);

        showOrHideQuotedText(
                (QuotedTextMode) savedInstanceState.getSerializable(STATE_KEY_QUOTED_TEXT_MODE));

        initializeCrypto();
        updateFrom();
        updateSignature();
        updateEncryptLayout();

        updateMessageFormat();
    }

    private void setTitle() {
        switch (mAction) {
            case REPLY: {
                setTitle(R.string.compose_title_reply);
                break;
            }
            case REPLY_ALL: {
                setTitle(R.string.compose_title_reply_all);
                break;
            }
            case FORWARD: {
                setTitle(R.string.compose_title_forward);
                break;
            }
            case COMPOSE:
            default: {
                setTitle(R.string.compose_title_compose);
                break;
            }
        }
    }

    /*
     * Returns an Address array of recipients this email will be sent to.
     * @return Address array of recipients this email will be sent to.
     */
    private List<Address> getRecipientAddresses() {
        ArrayList<Address> recipients = new ArrayList<Address>();
        recipients.addAll(mToView.getRecipients());
        recipients.addAll(mCcView.getRecipients());
        recipients.addAll(mBccView.getRecipients());
        return recipients;
    }

    protected TextBody buildText(boolean isDraft) {
        return createMessageBuilder(isDraft).buildText();
    }

    private MimeMessage createDraftMessage() throws MessagingException {
        return createMessageBuilder(true).build();
    }

    protected MimeMessage createMessage() throws MessagingException {
        return createMessageBuilder(false).build();
    }

    private MessageBuilder createMessageBuilder(final boolean isDraft) {
        return new MessageBuilder(getApplicationContext())
                .setSubject(mSubjectView.getText().toString())
                .setTo(mToView.getRecipients())
                .setCc(mCcView.getRecipients())
                .setBcc(mBccView.getRecipients())
                .setInReplyTo(mInReplyTo)
                .setReferences(mReferences)
                .setRequestReadReceipt(mReadReceipt)
                .setIdentity(mIdentity)
                .setMessageFormat(mMessageFormat)
                .setText(mMessageContentView.getCharacters())
                .setPgpData(mPgpData)
                .setAttachments(AttachmentMessageComposeHelper.createAttachmentList(mAttachments))
                .setSignature(mSignatureView.getCharacters())
                .setQuoteStyle(mQuoteStyle)
                .setQuotedTextMode(mQuotedTextMode)
                .setQuotedText(mQuotedText.getCharacters())
                .setQuotedHtmlContent(mQuotedHtmlContent)
                .setReplyAfterQuote(mAccount.isReplyAfterQuote())
                .setSignatureBeforeQuotedText(mAccount.isSignatureBeforeQuotedText())
                .setIdentityChanged(mIdentityChanged)
                .setSignatureChanged(mSignatureChanged)
                .setCursorPosition(mMessageContentView.getSelectionStart())
                .setMessageReference(mMessageReference)
                .setDraft(isDraft);
    }

    private void sendMessage() {
        new SendMessageTask().execute();
    }

    private void saveMessage() {
        new SaveMessageTask().execute();
    }

    private void saveIfNeeded() {
        if (!mDraftNeedsSaving || mPreventDraftSaving || mPgpData.hasEncryptionKeys() ||
                shouldEncrypt() || !mAccount.hasDraftsFolder()) {
            return;
        }

        mDraftNeedsSaving = false;
        saveMessage();
    }

    public void onEncryptionKeySelectionDone() {
        if (mPgpData.hasEncryptionKeys()) {
            onSend();
        } else {
            Toast.makeText(this, R.string.send_aborted, Toast.LENGTH_SHORT).show();
        }
    }

    public void onEncryptDone() {
        if (mPgpData.getEncryptedData() != null) {
            onSend();
        } else {
            Toast.makeText(this, R.string.send_aborted, Toast.LENGTH_SHORT).show();
        }
    }

    protected void onSend() {
        if (mToView.getRecipients().size() == 0 && mCcView.getRecipients().size() == 0 && mBccView.getRecipients().size() == 0) {
            mToView.setError(getString(R.string.message_compose_error_no_recipients));
            Toast.makeText(this, getString(R.string.message_compose_error_no_recipients), Toast.LENGTH_LONG).show();
            sendMenuButton.setEnabled(true);
            return;
        }

        if (mWaitingForAttachments != WaitingAction.NONE) {
            sendMenuButton.setEnabled(true);
            return;
        }

        if (mNumAttachmentsLoading > 0) {
            mWaitingForAttachments = WaitingAction.SEND;
            sendMenuButton.setEnabled(true);
            showWaitingForAttachmentDialog();
        } else {
            performSend();
        }
    }

    private void performSend() {
        if (isCryptoProviderEnabled() && handleEncrypt()) {
            return;
        }

        sendMessage();
        updateMessageFlags();

        mDraftNeedsSaving = false;
        finish();
    }

    private void updateMessageFlags() {
        if (mMessageReference != null && mMessageReference.getFlag() != null) {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "Setting referenced message (" + mMessageReference.getFolderName() + ", " + mMessageReference.getUid() + ") flag to " + mMessageReference.getFlag());
            }

            final Account account = Preferences.getPreferences(this).getAccount(mMessageReference.getAccountUuid());
            final String folderName = mMessageReference.getFolderName();
            final String sourceMessageUid = mMessageReference.getUid();
            controller.setFlag(account, folderName, sourceMessageUid, mMessageReference.getFlag(), true);
        }
    }

    private boolean handleEncrypt() {
        // user does not want to encrypt...
        if ((!mEncryptCheckbox.isChecked() && !mCryptoSignatureCheckbox.isChecked())) {
            return false;
        }

        // SMIME already encrypted
        if(currentMessage != null) {
            return false;
        }

        // PGP already encrypted
        if(mPgpData.getEncryptedData() != null) {
            return false;
        }

        switch (selectedCryptoProvider) {
            case PGP:
                openPgpMessageCompose = new OpenPgpMessageCompose(buildText(false), mOpenPgpServiceConnection, getRecipientAddresses(), mPgpData, mHandler, this, mAccount);
                openPgpMessageCompose.handlePgp(mEncryptCheckbox.isChecked(), mCryptoSignatureCheckbox.isChecked());
                break;
            case SMIME:
                SmimeMessageCompose smimeMessageCompose = new SmimeMessageCompose(mCryptoSignatureCheckbox.isChecked(), mEncryptCheckbox.isChecked(), getRecipientAddresses(), mIdentity, sMimeApi, mHandler);
                smimeMessageCompose.handleSmime();
                break;
            case NONE:
                return false;
        }

        // onSend() is called again in OpenPgpSignEncryptCallback and with
        // encryptedData set in pgpData!
        return true;
    }

    private void onDiscard() {
        if (mDraftId != INVALID_DRAFT_ID) {
            controller.deleteDraft(mAccount, mDraftId);
            mDraftId = INVALID_DRAFT_ID;
        }

        mHandler.sendEmptyMessage(MSG_DISCARDED_DRAFT);
        mDraftNeedsSaving = false;
        finish();
    }

    private void onSave() {
        if (mWaitingForAttachments != WaitingAction.NONE) {
            return;
        }

        if (mNumAttachmentsLoading > 0) {
            mWaitingForAttachments = WaitingAction.SAVE;
            showWaitingForAttachmentDialog();
        } else {
            performSave();
        }
    }

    private void performSave() {
        saveIfNeeded();
        finish();
    }

    private void onAddCcBcc() {
        mCcView.setVisibility(View.VISIBLE);
        mBccView.setVisibility(View.VISIBLE);
        computeAddCcBccVisibility();
    }

    /**
     * Hide the 'Add Cc/Bcc' menu item when both fields are visible.
     */
    private void computeAddCcBccVisibility() {
        if (mMenu != null && mCcView.getVisibility() == View.VISIBLE &&
                mBccView.getVisibility() == View.VISIBLE) {
            mMenu.findItem(R.id.add_cc_bcc).setVisible(false);
        }
    }

    private void onReadReceipt() {
        CharSequence txt;
        if (mReadReceipt == false) {
            txt = getString(R.string.read_receipt_enabled);
            mReadReceipt = true;
        } else {
            txt = getString(R.string.read_receipt_disabled);
            mReadReceipt = false;
        }

        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, txt, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Kick off a picker for the specified MIME type and let Android take over.
     *
     * @param mime_type
     *         The MIME type we want our attachment to have.
     */
    @SuppressLint("InlinedApi")
    private void onAddAttachment(final String mime_type) {
        if (isCryptoProviderEnabled()) {
            Toast.makeText(this, R.string.attachment_encryption_unsupported, Toast.LENGTH_LONG).show();
        }

        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime_type);
        mIgnoreOnPause = true;
        startActivityForResult(Intent.createChooser(i, null), ACTIVITY_REQUEST_PICK_ATTACHMENT);
    }

    public int increaseAndReturnMaxLoadId() {
        return ++mMaxLoaderId;
    }

    private LoaderManager.LoaderCallbacks<Attachment> mAttachmentContentLoaderCallback =
            new AttachmentContentLoaderCallback(this);

    private LoaderManager.LoaderCallbacks<Attachment> mAttachmentInfoLoaderCallback =
            new AttachmentInfoLoaderCallback(this, mAttachmentContentLoaderCallback);

    public void onFetchAttachmentStarted() {
        mNumAttachmentsLoading += 1;
    }

    public void onFetchAttachmentFinished() {
        // We're not allowed to perform fragment transactions when called from onLoadFinished().
        // So we use the Handler to call performStalledAction().
        mHandler.sendEmptyMessage(MSG_PERFORM_STALLED_ACTION);
    }

    void performStalledAction() {
        mNumAttachmentsLoading -= 1;

        WaitingAction waitingFor = mWaitingForAttachments;
        mWaitingForAttachments = WaitingAction.NONE;

        if (waitingFor != WaitingAction.NONE) {
            dismissWaitingForAttachmentDialog();
        }

        switch (waitingFor) {
            case SEND: {
                performSend();
                break;
            }
            case SAVE: {
                performSave();
                break;
            }
            case NONE:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a CryptoSystem activity is returning, then mPreventDraftSaving was set to true
        mPreventDraftSaving = false;

        // OpenPGP: try again after user interaction
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SIGN_ENCRYPT_OPENPGP) {
            /*
             * The data originally given to the pgp method are are again
             * returned here to be used when calling again after user
             * interaction. They also contain results from the user interaction
             * which happened, for example selected key ids.
             */
            openPgpMessageCompose.executeOpenPgpMethod(data);

            return;
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        if (data == null) {
            return;
        }

        switch (requestCode) {
            case ACTIVITY_REQUEST_PICK_ATTACHMENT:
                AttachmentMessageComposeHelper.addAttachmentsFromResultIntent(this, data, mAttachmentInfoLoaderCallback);
                mDraftNeedsSaving = true;
                break;
            case CONTACT_PICKER_TO:
            case CONTACT_PICKER_CC:
            case CONTACT_PICKER_BCC:
                ContactItem contact = mContacts.extractInfoFromContactPickerIntent(data);
                if (contact == null) {
                    Toast.makeText(this, getString(R.string.error_contact_address_not_found), Toast.LENGTH_LONG).show();
                    return;
                }
                if (contact.emailAddresses.size() > 1) {
                    Intent i = new Intent(this, EmailAddressList.class);
                    i.putExtra(EmailAddressList.EXTRA_CONTACT_ITEM, contact);

                    if (requestCode == CONTACT_PICKER_TO) {
                        startActivityForResult(i, CONTACT_PICKER_TO2);
                    } else if (requestCode == CONTACT_PICKER_CC) {
                        startActivityForResult(i, CONTACT_PICKER_CC2);
                    } else if (requestCode == CONTACT_PICKER_BCC) {
                        startActivityForResult(i, CONTACT_PICKER_BCC2);
                    }
                    return;
                }
                if (K9.DEBUG) {
                    List<String> emails = contact.emailAddresses;
                    for (int i = 0; i < emails.size(); i++) {
                        Log.v(K9.LOG_TAG, "email[" + i + "]: " + emails.get(i));
                    }
                }


                String email = contact.emailAddresses.get(0);
                if (requestCode == CONTACT_PICKER_TO) {
                    mToView.addRecipient(new Address(email, ""));
                } else if (requestCode == CONTACT_PICKER_CC) {
                    mCcView.addRecipient(new Address(email, ""));
                } else if (requestCode == CONTACT_PICKER_BCC) {
                    mBccView.addRecipient(new Address(email, ""));
                } else {
                    return;
                }



                break;
            case CONTACT_PICKER_TO2:
            case CONTACT_PICKER_CC2:
            case CONTACT_PICKER_BCC2:
                String emailAddr = data.getStringExtra(EmailAddressList.EXTRA_EMAIL_ADDRESS);
                if (requestCode == CONTACT_PICKER_TO2) {
                    mToView.addRecipient(new Address(emailAddr, ""));
                } else if (requestCode == CONTACT_PICKER_CC2) {
                    mCcView.addRecipient(new Address(emailAddr, ""));
                } else if (requestCode == CONTACT_PICKER_BCC2) {
                    mBccView.addRecipient(new Address(emailAddr, ""));
                }
                break;
        }
    }

    private void onAccountChosen(Account account, Identity identity) {
        if (!mAccount.equals(account)) {
            if (K9.DEBUG) {
                Log.v(K9.LOG_TAG, "Switching account from " + mAccount + " to " + account);
            }

            // on draft edit, make sure we don't keep previous message UID
            if (mAction == Action.EDIT_DRAFT) {
                mMessageReference = null;
            }

            // test whether there is something to save
            if (mDraftNeedsSaving || (mDraftId != INVALID_DRAFT_ID)) {
                final long previousDraftId = mDraftId;
                final Account previousAccount = mAccount;

                // make current message appear as new
                mDraftId = INVALID_DRAFT_ID;

                // actual account switch
                mAccount = account;

                if (K9.DEBUG) {
                    Log.v(K9.LOG_TAG, "Account switch, saving new draft in new account");
                }
                saveMessage();

                if (previousDraftId != INVALID_DRAFT_ID) {
                    if (K9.DEBUG) {
                        Log.v(K9.LOG_TAG, "Account switch, deleting draft from previous account: "
                              + previousDraftId);
                    }

                    controller.deleteDraft(previousAccount, previousDraftId);
                }
            } else {
                mAccount = account;
            }

            // Show CC/BCC text input field when switching to an account that always wants them
            // displayed.
            // Please note that we're not hiding the fields if the user switches back to an account
            // that doesn't have this setting checked.
            if (mAccount.isAlwaysShowCcBcc()) {
                onAddCcBcc();
            }

            // not sure how to handle mFolder, mSourceMessage?
        }

        switchToIdentity(identity);
    }

    private void switchToIdentity(Identity identity) {
        mIdentity = identity;
        mIdentityChanged = true;
        mDraftNeedsSaving = true;
        updateFrom();
        updateBcc();
        updateSignature();
        updateMessageFormat();
    }

    private void updateFrom() {
        mChooseIdentityButton.setText(mIdentity.getEmail());
    }

    private void updateBcc() {
        if (mIdentityChanged) {
            mBccView.setVisibility(View.VISIBLE);
        }
        mBccView.clearRecipients();
        mBccView.addRecipients(Address.parseUnencoded(mAccount.getAlwaysBcc()));
    }

    private void updateSignature() {
        if (mIdentity.getSignatureUse()) {
            mSignatureView.setCharacters(mIdentity.getSignature());
            mSignatureView.setVisibility(View.VISIBLE);
        } else {
            mSignatureView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.attachment_delete:
                /*
                 * The view is the delete button, and we have previously set the tag of
                 * the delete button to the view that owns it. We don't use parent because the
                 * view is very complex and could change in the future.
                 */
                mAttachments.removeView((View) view.getTag());
                mDraftNeedsSaving = true;
                break;
            case R.id.quoted_text_show:
                showOrHideQuotedText(QuotedTextMode.SHOW);
                updateMessageFormat();
                mDraftNeedsSaving = true;
                break;
            case R.id.quoted_text_delete:
                showOrHideQuotedText(QuotedTextMode.HIDE);
                updateMessageFormat();
                mDraftNeedsSaving = true;
                break;
            case R.id.quoted_text_edit:
                mForcePlainText = true;
                if (mMessageReference != null) { // shouldn't happen...
                    // TODO - Should we check if mSourceMessageBody is already present and bypass the MessagingController call?
                    controller.addListener(mListener);
                    final Account account = Preferences.getPreferences(this).getAccount(mMessageReference.getAccountUuid());
                    final String folderName = mMessageReference.getFolderName();
                    final String sourceMessageUid = mMessageReference.getUid();
                    controller.loadMessageForView(account, folderName, sourceMessageUid, null);
                }
                break;
            case R.id.identity:
                createChooseIdentityDialog().show();
                break;
        }
    }

    /**
     * Show or hide the quoted text.
     *
     * @param mode
     *         The value to set {@link #mQuotedTextMode} to.
     */
    private void showOrHideQuotedText(QuotedTextMode mode) {
        mQuotedTextMode = mode;
        switch (mode) {
            case NONE:
            case HIDE: {
                if (mode == QuotedTextMode.NONE) {
                    mQuotedTextShow.setVisibility(View.GONE);
                } else {
                    mQuotedTextShow.setVisibility(View.VISIBLE);
                }
                mQuotedTextBar.setVisibility(View.GONE);
                mQuotedText.setVisibility(View.GONE);
                mQuotedHTML.setVisibility(View.GONE);
                mQuotedTextEdit.setVisibility(View.GONE);
                break;
            }
            case SHOW: {
                mQuotedTextShow.setVisibility(View.GONE);
                mQuotedTextBar.setVisibility(View.VISIBLE);

                if (mQuotedTextFormat == SimpleMessageFormat.HTML) {
                    mQuotedText.setVisibility(View.GONE);
                    mQuotedHTML.setVisibility(View.VISIBLE);
                    mQuotedTextEdit.setVisibility(View.VISIBLE);
                } else {
                    mQuotedText.setVisibility(View.VISIBLE);
                    mQuotedHTML.setVisibility(View.GONE);
                    mQuotedTextEdit.setVisibility(View.GONE);
                }
                break;
            }
        }
    }

    private void askBeforeDiscard(){
        if (K9.confirmDiscardMessage()) {
            createConfirmDiscardDialog2().show();
        } else {
            onDiscard();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.send:
                sendMenuButton.setEnabled(false);
                mPgpData.setEncryptionKeys(null);
                onSend();
                break;
            case R.id.save:
                if (shouldEncrypt()) {
                    createCanNotSaveEncryptedDialog().show();
                } else {
                    onSave();
                }
                break;
            case R.id.discard:
                askBeforeDiscard();
                break;
            case R.id.add_cc_bcc:
                onAddCcBcc();
                break;
            case R.id.add_attachment:
                onAddAttachment("*/*");
                break;
            case R.id.read_receipt:
                onReadReceipt();
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_compose_option, menu);

        mMenu = menu;

        // Disable the 'Save' menu option if Drafts folder is set to -NONE-
        if (!mAccount.hasDraftsFolder()) {
            menu.findItem(R.id.save).setEnabled(false);
        }

        sendMenuButton = menu.findItem(R.id.send);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        computeAddCcBccVisibility();

        return true;
    }

    @Override
    public void onBackPressed() {
        if (mDraftNeedsSaving) {
            if (shouldEncrypt()) {
                createCanNotSaveEncryptedDialog().show();
            } else if (!mAccount.hasDraftsFolder()) {
                createConfirmDiscardDialog().show();
            } else {
                createSaveOrDiscardDialog().show();
            }
        } else {
            // Check if editing an existing draft.
            if (mDraftId == INVALID_DRAFT_ID) {
                onDiscard();
            } else {
                super.onBackPressed();
            }
        }
    }

    private void showWaitingForAttachmentDialog() {
        String title;

        switch (mWaitingForAttachments) {
            case SEND: {
                title = getString(R.string.fetching_attachment_dialog_title_send);
                break;
            }
            case SAVE: {
                title = getString(R.string.fetching_attachment_dialog_title_save);
                break;
            }
            default: {
                return;
            }
        }

        ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(title,
                getString(R.string.fetching_attachment_dialog_message));
        fragment.show(getSupportFragmentManager(), FRAGMENT_WAITING_FOR_ATTACHMENT);
    }

    public void onCancel(ProgressDialogFragment fragment) {
        attachmentProgressDialogCancelled();
    }

    void attachmentProgressDialogCancelled() {
        mWaitingForAttachments = WaitingAction.NONE;
    }

    private void dismissWaitingForAttachmentDialog() {
        ProgressDialogFragment fragment = (ProgressDialogFragment)
                getSupportFragmentManager().findFragmentByTag(FRAGMENT_WAITING_FOR_ATTACHMENT);

        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private AlertDialog createConfirmDiscardDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_discard_draft_message_title)
                .setMessage(R.string.confirm_discard_draft_message)
                .setPositiveButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.discard_action, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        Toast.makeText(MessageCompose.this,
                                getString(R.string.message_discarded_toast),
                                Toast.LENGTH_LONG).show();
                        onDiscard();
                    }
                })
                .create();
    }

    private AlertDialog createConfirmDiscardDialog2() {
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_confirm_delete_title)
                .setMessage(R.string.dialog_confirm_delete_message)
                .setPositiveButton(R.string.dialog_confirm_delete_confirm_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                onDiscard();
                            }
                        })
                .setNegativeButton(R.string.dialog_confirm_delete_cancel_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .create();
    }

    private AlertDialog createChooseIdentityDialog() {
        Context context = new ContextThemeWrapper(this,
                (K9.getK9Theme() == K9.Theme.LIGHT) ?
                R.style.Theme_K9_Dialog_Light :
                R.style.Theme_K9_Dialog_Dark);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.send_as);
        final IdentityAdapter adapter = new IdentityAdapter(context);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                IdentityContainer container = (IdentityContainer) adapter.getItem(which);
                onAccountChosen(container.account, container.identity);
            }
        });

        return builder.create();
    }

    private AlertDialog createCanNotSaveEncryptedDialog() {
        return new AlertDialog.Builder(this)
               .setTitle(R.string.refuse_to_save_draft_marked_encrypted_dlg_title)
               .setMessage(R.string.refuse_to_save_draft_marked_encrypted_instructions_fmt)
        .setNeutralButton(R.string.okay_action, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        })
                .create();
    }

    private AlertDialog createSaveOrDiscardDialog() {
        return new AlertDialog.Builder(this)
               .setTitle(R.string.save_or_discard_draft_message_dlg_title)
               .setMessage(R.string.save_or_discard_draft_message_instructions_fmt)
        .setPositiveButton(R.string.save_draft_action, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                onSave();
            }
        })
        .setNegativeButton(R.string.discard_action, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                onDiscard();
            }
        })
        .create();
    }

    /**
     * Add all attachments of an existing message as if they were added by hand.
     *
     * @param part
     *         The message part to check for being an attachment. This method will recurse if it's
     *         a multipart part.
     * @param depth
     *         The recursion depth. Currently unused.
     *
     * @return {@code true} if all attachments were able to be attached, {@code false} otherwise.
     *
     * @throws MessagingException
     *          In case of an error
     */
    private boolean loadAttachments(Part part, int depth) throws MessagingException {
        if (part.getBody() instanceof Multipart) {
            Multipart mp = (Multipart) part.getBody();
            boolean ret = true;
            for (int i = 0, count = mp.getCount(); i < count; i++) {
                if (!loadAttachments(mp.getBodyPart(i), depth + 1)) {
                    ret = false;
                }
            }
            return ret;
        }

        String contentType = MimeUtility.unfoldAndDecode(part.getContentType());
        String name = MimeUtility.getHeaderParameter(contentType, "name");
        if (name != null) {
            Body body = part.getBody();
            //FIXME
//            if (body instanceof LocalAttachmentBody) {
//                final Uri uri = ((LocalAttachmentBody) body).getContentUri();
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        addAttachment(uri);
//                    }
//                });
//            } else {
//                return false;
//            }
            return false;
        }
        return true;
    }

    /**
     * Pull out the parts of the now loaded source message and apply them to the new message
     * depending on the type of message being composed.
     *
     * @param message
     *         The source message used to populate the various text fields.
     */
    private void processSourceMessage(LocalMessage message) {
        try {
            switch (mAction) {
                case REPLY:
                case REPLY_ALL: {
                    processMessageToReplyTo(message);
                    break;
                }
                case FORWARD: {
                    processMessageToForward(message);
                    break;
                }
                case EDIT_DRAFT: {
                    processDraftMessage(message);
                    break;
                }
                default: {
                    Log.w(K9.LOG_TAG, "processSourceMessage() called with unsupported action");
                    break;
                }
            }
        } catch (MessagingException me) {
            /**
             * Let the user continue composing their message even if we have a problem processing
             * the source message. Log it as an error, though.
             */
            Log.e(K9.LOG_TAG, "Error while processing source message: ", me);
        } finally {
            mSourceMessageProcessed = true;
            mDraftNeedsSaving = false;
        }

        updateMessageFormat();
    }

    private void processMessageToReplyTo(Message message) throws MessagingException {
        if (message.getSubject() != null) {
            final String subject = PREFIX.matcher(message.getSubject()).replaceFirst("");

            if (!subject.toLowerCase(Locale.US).startsWith("re:")) {
                mSubjectView.setText("Re: " + subject);
            } else {
                mSubjectView.setText(subject);
            }
        } else {
            mSubjectView.setText("");
        }

        /*
         * If a reply-to was included with the message use that, otherwise use the from
         * or sender address.
         */
        Address[] replyToAddresses;
        if (message.getReplyTo().length > 0) {
            replyToAddresses = message.getReplyTo();
        } else {
            replyToAddresses = message.getFrom();
        }

        // if we're replying to a message we sent, we probably meant
        // to reply to the recipient of that message
        if (mAccount.isAnIdentity(replyToAddresses)) {
            replyToAddresses = message.getRecipients(RecipientType.TO);
        }

        mToView.addRecipients(replyToAddresses);

        if (message.getMessageId() != null && message.getMessageId().length() > 0) {
            mInReplyTo = message.getMessageId();

            String[] refs = message.getReferences();
            if (refs != null && refs.length > 0) {
                mReferences = TextUtils.join("", refs) + " " + mInReplyTo;
            } else {
                mReferences = mInReplyTo;
            }

        } else {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "could not get Message-ID.");
            }
        }

        // Quote the message and setup the UI.
        populateUIWithQuotedMessage(mAccount.isDefaultQuotedTextShown());

        if (mAction == Action.REPLY || mAction == Action.REPLY_ALL) {
            Identity useIdentity = IdentityHelper.getRecipientIdentityFromMessage(mAccount, message);
            Identity defaultIdentity = mAccount.getIdentity(0);
            if (useIdentity != defaultIdentity) {
                switchToIdentity(useIdentity);
            }
        }

        if (mAction == Action.REPLY_ALL) {
            if (message.getReplyTo().length > 0) {
                for (Address address : message.getFrom()) {
                    if (!mAccount.isAnIdentity(address) && !Utility.arrayContains(replyToAddresses, address)) {
                        mToView.addRecipient(address);
                    }
                }
            }

            for (Address address : message.getRecipients(RecipientType.TO)) {
                if (!mAccount.isAnIdentity(address) && !Utility.arrayContains(replyToAddresses, address)) {
                    mToView.addRecipient(address);
                }

            }

            if (message.getRecipients(RecipientType.CC).length > 0) {
                for (Address address : message.getRecipients(RecipientType.CC)) {
                    if (!mAccount.isAnIdentity(address) && !Utility.arrayContains(replyToAddresses, address)) {
                        mCcView.addRecipient(address);
                    }

                }
                mCcView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void processMessageToForward(Message message) throws MessagingException {
        String subject = message.getSubject();
        if (subject != null && !subject.toLowerCase(Locale.US).startsWith("fwd:")) {
            mSubjectView.setText("Fwd: " + subject);
        } else {
            mSubjectView.setText(subject);
        }
        mQuoteStyle = QuoteStyle.HEADER;

        // "Be Like Thunderbird" - on forwarded messages, set the message ID
        // of the forwarded message in the references and the reply to.  TB
        // only includes ID of the message being forwarded in the reference,
        // even if there are multiple references.
        if (!TextUtils.isEmpty(message.getMessageId())) {
            mInReplyTo = message.getMessageId();
            mReferences = mInReplyTo;
        } else {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "could not get Message-ID.");
            }
        }

        // Quote the message and setup the UI.
        populateUIWithQuotedMessage(true);

        if (!mSourceMessageProcessed) {
            if (!loadAttachments(message, 0)) {
                mHandler.sendEmptyMessage(MSG_SKIPPED_ATTACHMENTS);
            }
        }
    }

    private void processDraftMessage(LocalMessage message) throws MessagingException {
        String showQuotedTextMode = "NONE";

        mDraftId = controller.getId(message);
        mSubjectView.setText(message.getSubject());
        mToView.addRecipients(message.getRecipients(RecipientType.TO));
        if (message.getRecipients(RecipientType.CC).length > 0) {
            mCcView.addRecipients(message.getRecipients(RecipientType.CC));
            mCcView.setVisibility(View.VISIBLE);
        }

        Address[] bccRecipients = message.getRecipients(RecipientType.BCC);
        if (bccRecipients.length > 0) {
            mBccView.addRecipients(bccRecipients);
            String bccAddress = mAccount.getAlwaysBcc();
            if (bccRecipients.length == 1 && bccAddress != null && bccAddress.equals(bccRecipients[0].toString())) {
                // If the auto-bcc is the only entry in the BCC list, don't show the Bcc fields.
                mBccView.setVisibility(View.GONE);
            } else {
                mBccView.setVisibility(View.VISIBLE);
            }
        }

        // Read In-Reply-To header from draft
        final String[] inReplyTo = message.getHeader("In-Reply-To");
        if (inReplyTo.length >= 1) {
            mInReplyTo = inReplyTo[0];
        }

        // Read References header from draft
        final String[] references = message.getHeader("References");
        if (references.length >= 1) {
            mReferences = references[0];
        }

        if (!mSourceMessageProcessed) {
            loadAttachments(message, 0);
        }

        // Decode the identity header when loading a draft.
        // See buildIdentityHeader(TextBody) for a detailed description of the composition of this blob.
        Map<IdentityField, String> k9identity = new HashMap<IdentityField, String>();
        String[] identityHeaders = message.getHeader(K9.IDENTITY_HEADER);

        if (identityHeaders.length > 0 && identityHeaders[0] != null) {
            k9identity = IdentityHeaderParser.parse(identityHeaders[0]);
        }

        Identity newIdentity = new Identity();
        if (k9identity.containsKey(IdentityField.SIGNATURE)) {
            newIdentity.setSignatureUse(true);
            newIdentity.setSignature(k9identity.get(IdentityField.SIGNATURE));
            mSignatureChanged = true;
        } else {
            newIdentity.setSignatureUse(message.getFolder().getSignatureUse());
            newIdentity.setSignature(mIdentity.getSignature());
        }

        if (k9identity.containsKey(IdentityField.NAME)) {
            newIdentity.setName(k9identity.get(IdentityField.NAME));
            mIdentityChanged = true;
        } else {
            newIdentity.setName(mIdentity.getName());
        }

        if (k9identity.containsKey(IdentityField.EMAIL)) {
            newIdentity.setEmail(k9identity.get(IdentityField.EMAIL));
            mIdentityChanged = true;
        } else {
            newIdentity.setEmail(mIdentity.getEmail());
        }

        if (k9identity.containsKey(IdentityField.ORIGINAL_MESSAGE)) {
            mMessageReference = null;
            try {
                String originalMessage = k9identity.get(IdentityField.ORIGINAL_MESSAGE);
                MessageReference messageReference = new MessageReference(originalMessage);

                // Check if this is a valid account in our database
                Preferences prefs = Preferences.getPreferences(getApplicationContext());
                Account account = prefs.getAccount(messageReference.getAccountUuid());
                if (account != null) {
                    mMessageReference = messageReference;
                }
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Could not decode message reference in identity.", e);
            }
        }

        int cursorPosition = 0;
        if (k9identity.containsKey(IdentityField.CURSOR_POSITION)) {
            try {
                cursorPosition = Integer.parseInt(k9identity.get(IdentityField.CURSOR_POSITION));
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Could not parse cursor position for MessageCompose; continuing.", e);
            }
        }

        if (k9identity.containsKey(IdentityField.QUOTED_TEXT_MODE)) {
            showQuotedTextMode = k9identity.get(IdentityField.QUOTED_TEXT_MODE);
        }

        mIdentity = newIdentity;

        updateSignature();
        updateFrom();

        Integer bodyLength = k9identity.get(IdentityField.LENGTH) != null
                             ? Integer.valueOf(k9identity.get(IdentityField.LENGTH))
                             : 0;
        Integer bodyOffset = k9identity.get(IdentityField.OFFSET) != null
                             ? Integer.valueOf(k9identity.get(IdentityField.OFFSET))
                             : 0;
        Integer bodyFooterOffset = k9identity.get(IdentityField.FOOTER_OFFSET) != null
                ? Integer.valueOf(k9identity.get(IdentityField.FOOTER_OFFSET))
                : null;
        Integer bodyPlainLength = k9identity.get(IdentityField.PLAIN_LENGTH) != null
                ? Integer.valueOf(k9identity.get(IdentityField.PLAIN_LENGTH))
                : null;
        Integer bodyPlainOffset = k9identity.get(IdentityField.PLAIN_OFFSET) != null
                ? Integer.valueOf(k9identity.get(IdentityField.PLAIN_OFFSET))
                : null;
        mQuoteStyle = k9identity.get(IdentityField.QUOTE_STYLE) != null
                ? QuoteStyle.valueOf(k9identity.get(IdentityField.QUOTE_STYLE))
                : mAccount.getQuoteStyle();


        QuotedTextMode quotedMode;
        try {
            quotedMode = QuotedTextMode.valueOf(showQuotedTextMode);
        } catch (Exception e) {
            quotedMode = QuotedTextMode.NONE;
        }

        // Always respect the user's current composition format preference, even if the
        // draft was saved in a different format.
        // TODO - The current implementation doesn't allow a user in HTML mode to edit a draft that wasn't saved with K9mail.
        String messageFormatString = k9identity.get(IdentityField.MESSAGE_FORMAT);

        MessageFormat messageFormat = null;
        if (messageFormatString != null) {
            try {
                messageFormat = MessageFormat.valueOf(messageFormatString);
            } catch (Exception e) { /* do nothing */ }
        }

        if (messageFormat == null) {
            // This message probably wasn't created by us. The exception is legacy
            // drafts created before the advent of HTML composition. In those cases,
            // we'll display the whole message (including the quoted part) in the
            // composition window. If that's the case, try and convert it to text to
            // match the behavior in text mode.
            mMessageContentView.setCharacters(getBodyTextFromMessage(message, SimpleMessageFormat.TEXT));
            mForcePlainText = true;

            showOrHideQuotedText(quotedMode);
            return;
        }

        if (messageFormat == MessageFormat.HTML) {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            if (part != null) { // Shouldn't happen if we were the one who saved it.
                mQuotedTextFormat = SimpleMessageFormat.HTML;
                String text = MessageExtractor.getTextFromPart(part);
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "Loading message with offset " + bodyOffset + ", length " + bodyLength + ". Text length is " + text.length() + ".");
                }

                if (bodyOffset + bodyLength > text.length()) {
                    // The draft was edited outside of K-9 Mail?
                    K9.logDebug( "The identity field from the draft contains an invalid LENGTH/OFFSET");
                    bodyOffset = 0;
                    bodyLength = 0;
                }
                // Grab our reply text.
                String bodyText = text.substring(bodyOffset, bodyOffset + bodyLength);
                mMessageContentView.setCharacters(HtmlConverter.htmlToText(bodyText));

                // Regenerate the quoted html without our user content in it.
                StringBuilder quotedHTML = new StringBuilder();
                quotedHTML.append(text.substring(0, bodyOffset));   // stuff before the reply
                quotedHTML.append(text.substring(bodyOffset + bodyLength));
                if (quotedHTML.length() > 0) {
                    mQuotedHtmlContent = new InsertableHtmlContent();
                    mQuotedHtmlContent.setQuotedContent(quotedHTML);
                    // We don't know if bodyOffset refers to the header or to the footer
                    mQuotedHtmlContent.setHeaderInsertionPoint(bodyOffset);
                    if (bodyFooterOffset != null) {
                        mQuotedHtmlContent.setFooterInsertionPoint(bodyFooterOffset);
                    } else {
                        mQuotedHtmlContent.setFooterInsertionPoint(bodyOffset);
                    }
                    mQuotedHTML.setText(mQuotedHtmlContent.getQuotedContent());
                }
            }
            if (bodyPlainOffset != null && bodyPlainLength != null) {
                processSourceMessageText(message, bodyPlainOffset, bodyPlainLength, false);
            }
        } else if (messageFormat == MessageFormat.TEXT) {
            mQuotedTextFormat = SimpleMessageFormat.TEXT;
            processSourceMessageText(message, bodyOffset, bodyLength, true);
        } else {
            Log.e(K9.LOG_TAG, "Unhandled message format.");
        }

        // Set the cursor position if we have it.
        try {
            mMessageContentView.setSelection(cursorPosition);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Could not set cursor position in MessageCompose; ignoring.", e);
        }

        showOrHideQuotedText(quotedMode);
    }

    /**
     * Pull out the parts of the now loaded source message and apply them to the new message
     * depending on the type of message being composed.
     * @param message Source message
     * @param bodyOffset Insertion point for reply.
     * @param bodyLength Length of reply.
     * @param viewMessageContent Update mMessageContentView or not.
     * @throws MessagingException
     */
    private void processSourceMessageText(Message message, Integer bodyOffset, Integer bodyLength,
            boolean viewMessageContent) throws MessagingException {
        Part textPart = MimeUtility.findFirstPartByMimeType(message, "text/plain");
        if (textPart != null) {
            String text = MessageExtractor.getTextFromPart(textPart);
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "Loading message with offset " + bodyOffset + ", length " + bodyLength + ". Text length is " + text.length() + ".");
            }

            // If we had a body length (and it was valid), separate the composition from the quoted text
            // and put them in their respective places in the UI.
            if (bodyLength > 0) {
                try {
                    String bodyText = text.substring(bodyOffset, bodyOffset + bodyLength);

                    // Regenerate the quoted text without our user content in it nor added newlines.
                    StringBuilder quotedText = new StringBuilder();
                    if (bodyOffset == 0 && text.substring(bodyLength, bodyLength + 4).equals("\r\n\r\n")) {
                        // top-posting: ignore two newlines at start of quote
                        quotedText.append(text.substring(bodyLength + 4));
                    } else if (bodyOffset + bodyLength == text.length() &&
                            text.substring(bodyOffset - 2, bodyOffset).equals("\r\n")) {
                        // bottom-posting: ignore newline at end of quote
                        quotedText.append(text.substring(0, bodyOffset - 2));
                    } else {
                        quotedText.append(text.substring(0, bodyOffset));   // stuff before the reply
                        quotedText.append(text.substring(bodyOffset + bodyLength));
                    }

                    if (viewMessageContent) {
                        mMessageContentView.setCharacters(bodyText);
                    }

                    mQuotedText.setCharacters(quotedText);
                } catch (IndexOutOfBoundsException e) {
                    // Invalid bodyOffset or bodyLength.  The draft was edited outside of K-9 Mail?
                    K9.logDebug( "The identity field from the draft contains an invalid bodyOffset/bodyLength");
                    if (viewMessageContent) {
                        mMessageContentView.setCharacters(text);
                    }
                }
            } else {
                if (viewMessageContent) {
                    mMessageContentView.setCharacters(text);
                }
            }
        }
    }

    // Regexes to check for signature.
    private static final Pattern DASH_SIGNATURE_PLAIN = Pattern.compile("\r\n-- \r\n.*", Pattern.DOTALL);
    private static final Pattern DASH_SIGNATURE_HTML = Pattern.compile("(<br( /)?>|\r?\n)-- <br( /)?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCKQUOTE_START = Pattern.compile("<blockquote", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCKQUOTE_END = Pattern.compile("</blockquote>", Pattern.CASE_INSENSITIVE);

    /**
     * Build and populate the UI with the quoted message.
     *
     * @param showQuotedText
     *         {@code true} if the quoted text should be shown, {@code false} otherwise.
     *
     * @throws MessagingException
     */
    private void populateUIWithQuotedMessage(boolean showQuotedText) throws MessagingException {
        MessageFormat origMessageFormat = mAccount.getMessageFormat();
        if (mForcePlainText || origMessageFormat == MessageFormat.TEXT) {
            // Use plain text for the quoted message
            mQuotedTextFormat = SimpleMessageFormat.TEXT;
        } else if (origMessageFormat == MessageFormat.AUTO) {
            // Figure out which message format to use for the quoted text by looking if the source
            // message contains a text/html part. If it does, we use that.
            mQuotedTextFormat =
                    (MimeUtility.findFirstPartByMimeType(mSourceMessage, "text/html") == null) ?
                            SimpleMessageFormat.TEXT : SimpleMessageFormat.HTML;
        } else {
            mQuotedTextFormat = SimpleMessageFormat.HTML;
        }

        // TODO -- I am assuming that mSourceMessageBody will always be a text part.  Is this a safe assumption?

        // Handle the original message in the reply
        // If we already have mSourceMessageBody, use that.  It's pre-populated if we've got crypto going on.
        String content = (mSourceMessageBody != null) ?
                mSourceMessageBody :
                getBodyTextFromMessage(mSourceMessage, mQuotedTextFormat);

        if (mQuotedTextFormat == SimpleMessageFormat.HTML) {
            // Strip signature.
            // closing tags such as </div>, </span>, </table>, </pre> will be cut off.
            if (mAccount.isStripSignature() &&
                    (mAction == Action.REPLY || mAction == Action.REPLY_ALL)) {
                Matcher dashSignatureHtml = DASH_SIGNATURE_HTML.matcher(content);
                if (dashSignatureHtml.find()) {
                    Matcher blockquoteStart = BLOCKQUOTE_START.matcher(content);
                    Matcher blockquoteEnd = BLOCKQUOTE_END.matcher(content);
                    List<Integer> start = new ArrayList<Integer>();
                    List<Integer> end = new ArrayList<Integer>();

                    while (blockquoteStart.find()) {
                        start.add(blockquoteStart.start());
                    }
                    while (blockquoteEnd.find()) {
                        end.add(blockquoteEnd.start());
                    }
                    if (start.size() != end.size()) {
                        Log.d(K9.LOG_TAG, "There are " + start.size() + " <blockquote> tags, but " +
                                end.size() + " </blockquote> tags. Refusing to strip.");
                    } else if (start.size() > 0) {
                        // Ignore quoted signatures in blockquotes.
                        dashSignatureHtml.region(0, start.get(0));
                        if (dashSignatureHtml.find()) {
                            // before first <blockquote>.
                            content = content.substring(0, dashSignatureHtml.start());
                        } else {
                            for (int i = 0; i < start.size() - 1; i++) {
                                // within blockquotes.
                                if (end.get(i) < start.get(i + 1)) {
                                    dashSignatureHtml.region(end.get(i), start.get(i + 1));
                                    if (dashSignatureHtml.find()) {
                                        content = content.substring(0, dashSignatureHtml.start());
                                        break;
                                    }
                                }
                            }
                            if (end.get(end.size() - 1) < content.length()) {
                                // after last </blockquote>.
                                dashSignatureHtml.region(end.get(end.size() - 1), content.length());
                                if (dashSignatureHtml.find()) {
                                    content = content.substring(0, dashSignatureHtml.start());
                                }
                            }
                        }
                    } else {
                        // No blockquotes found.
                        content = content.substring(0, dashSignatureHtml.start());
                    }
                }

                // Fix the stripping off of closing tags if a signature was stripped,
                // as well as clean up the HTML of the quoted message.
                HtmlCleaner cleaner = new HtmlCleaner();
                CleanerProperties properties = cleaner.getProperties();

                // see http://htmlcleaner.sourceforge.net/parameters.php for descriptions
                properties.setNamespacesAware(false);
                properties.setAdvancedXmlEscape(false);
                properties.setOmitXmlDeclaration(true);
                properties.setOmitDoctypeDeclaration(false);
                properties.setTranslateSpecialEntities(false);
                properties.setRecognizeUnicodeChars(false);

                TagNode node = cleaner.clean(content);
                SimpleHtmlSerializer htmlSerialized = new SimpleHtmlSerializer(properties);
                content = htmlSerialized.getAsString(node, "UTF8");
            }

            // Add the HTML reply header to the top of the content.
            mQuotedHtmlContent = quoteOriginalHtmlMessage(mSourceMessage, content, mQuoteStyle);

            // Load the message with the reply header.
            mQuotedHTML.setText(mQuotedHtmlContent.getQuotedContent());

            // TODO: Also strip the signature from the text/plain part
            mQuotedText.setCharacters(quoteOriginalTextMessage(mSourceMessage,
                    getBodyTextFromMessage(mSourceMessage, SimpleMessageFormat.TEXT), mQuoteStyle));

        } else if (mQuotedTextFormat == SimpleMessageFormat.TEXT) {
            if (mAccount.isStripSignature() &&
                    (mAction == Action.REPLY || mAction == Action.REPLY_ALL)) {
                if (DASH_SIGNATURE_PLAIN.matcher(content).find()) {
                    content = DASH_SIGNATURE_PLAIN.matcher(content).replaceFirst("\r\n");
                }
            }

            mQuotedText.setCharacters(quoteOriginalTextMessage(mSourceMessage, content, mQuoteStyle));
        }

        if (showQuotedText) {
            showOrHideQuotedText(QuotedTextMode.SHOW);
        } else {
            showOrHideQuotedText(QuotedTextMode.HIDE);
        }
    }

    /**
     * Fetch the body text from a message in the desired message format. This method handles
     * conversions between formats (html to text and vice versa) if necessary.
     * @param message Message to analyze for body part.
     * @param format Desired format.
     * @return Text in desired format.
     * @throws MessagingException
     */
    private String getBodyTextFromMessage(final Message message, final SimpleMessageFormat format)
            throws MessagingException {
        Part part;
        if (format == SimpleMessageFormat.HTML) {
            // HTML takes precedence, then text.
            part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            if (part != null) {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: HTML requested, HTML found.");
                }
                return MessageExtractor.getTextFromPart(part);
            }

            part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part != null) {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: HTML requested, text found.");
                }
                String text = MessageExtractor.getTextFromPart(part);
                return HtmlConverter.textToHtml(text);
            }
        } else if (format == SimpleMessageFormat.TEXT) {
            // Text takes precedence, then html.
            part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part != null) {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: Text requested, text found.");
                }
                return MessageExtractor.getTextFromPart(part);
            }

            part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            if (part != null) {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "getBodyTextFromMessage: Text requested, HTML found.");
                }
                String text = MessageExtractor.getTextFromPart(part);
                return HtmlConverter.htmlToText(text);
            }
        }

        // If we had nothing interesting, return an empty string.
        return "";
    }

    // Regular expressions to look for various HTML tags. This is no HTML::Parser, but hopefully it's good enough for
    // our purposes.
    private static final Pattern FIND_INSERTION_POINT_HTML = Pattern.compile("(?si:.*?(<html(?:>|\\s+[^>]*>)).*)");
    private static final Pattern FIND_INSERTION_POINT_HEAD = Pattern.compile("(?si:.*?(<head(?:>|\\s+[^>]*>)).*)");
    private static final Pattern FIND_INSERTION_POINT_BODY = Pattern.compile("(?si:.*?(<body(?:>|\\s+[^>]*>)).*)");
    private static final Pattern FIND_INSERTION_POINT_HTML_END = Pattern.compile("(?si:.*(</html>).*?)");
    private static final Pattern FIND_INSERTION_POINT_BODY_END = Pattern.compile("(?si:.*(</body>).*?)");
    // The first group in a Matcher contains the first capture group. We capture the tag found in the above REs so that
    // we can locate the *end* of that tag.
    private static final int FIND_INSERTION_POINT_FIRST_GROUP = 1;
    // HTML bits to insert as appropriate
    // TODO is it safe to assume utf-8 here?
    private static final String FIND_INSERTION_POINT_HTML_CONTENT = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\r\n<html>";
    private static final String FIND_INSERTION_POINT_HTML_END_CONTENT = "</html>";
    private static final String FIND_INSERTION_POINT_HEAD_CONTENT = "<head><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\"></head>";
    // Index of the start of the beginning of a String.
    private static final int FIND_INSERTION_POINT_START_OF_STRING = 0;

    /**
     * <p>Find the start and end positions of the HTML in the string. This should be the very top
     * and bottom of the displayable message. It returns a {@link InsertableHtmlContent}, which
     * contains both the insertion points and potentially modified HTML. The modified HTML should be
     * used in place of the HTML in the original message.</p>
     *
     * <p>This method loosely mimics the HTML forward/reply behavior of BlackBerry OS 4.5/BIS 2.5, which in turn mimics
     * Outlook 2003 (as best I can tell).</p>
     *
     * @param content Content to examine for HTML insertion points
     * @return Insertion points and HTML to use for insertion.
     */
    private InsertableHtmlContent findInsertionPoints(final String content) {
        InsertableHtmlContent insertable = new InsertableHtmlContent();

        // If there is no content, don't bother doing any of the regex dancing.
        if (content == null || content.equals("")) {
            return insertable;
        }

        // Search for opening tags.
        boolean hasHtmlTag = false;
        boolean hasHeadTag = false;
        boolean hasBodyTag = false;
        // First see if we have an opening HTML tag.  If we don't find one, we'll add one later.
        Matcher htmlMatcher = FIND_INSERTION_POINT_HTML.matcher(content);
        if (htmlMatcher.matches()) {
            hasHtmlTag = true;
        }
        // Look for a HEAD tag.  If we're missing a BODY tag, we'll use the close of the HEAD to start our content.
        Matcher headMatcher = FIND_INSERTION_POINT_HEAD.matcher(content);
        if (headMatcher.matches()) {
            hasHeadTag = true;
        }
        // Look for a BODY tag.  This is the ideal place for us to start our content.
        Matcher bodyMatcher = FIND_INSERTION_POINT_BODY.matcher(content);
        if (bodyMatcher.matches()) {
            hasBodyTag = true;
        }

        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "Open: hasHtmlTag:" + hasHtmlTag + " hasHeadTag:" + hasHeadTag + " hasBodyTag:" + hasBodyTag);
        }

        // Given our inspections, let's figure out where to start our content.
        // This is the ideal case -- there's a BODY tag and we insert ourselves just after it.
        if (hasBodyTag) {
            insertable.setQuotedContent(new StringBuilder(content));
            insertable.setHeaderInsertionPoint(bodyMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP));
        } else if (hasHeadTag) {
            // Now search for a HEAD tag.  We can insert after there.

            // If BlackBerry sees a HEAD tag, it inserts right after that, so long as there is no BODY tag. It doesn't
            // try to add BODY, either.  Right or wrong, it seems to work fine.
            insertable.setQuotedContent(new StringBuilder(content));
            insertable.setHeaderInsertionPoint(headMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP));
        } else if (hasHtmlTag) {
            // Lastly, check for an HTML tag.
            // In this case, it will add a HEAD, but no BODY.
            StringBuilder newContent = new StringBuilder(content);
            // Insert the HEAD content just after the HTML tag.
            newContent.insert(htmlMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP), FIND_INSERTION_POINT_HEAD_CONTENT);
            insertable.setQuotedContent(newContent);
            // The new insertion point is the end of the HTML tag, plus the length of the HEAD content.
            insertable.setHeaderInsertionPoint(htmlMatcher.end(FIND_INSERTION_POINT_FIRST_GROUP) + FIND_INSERTION_POINT_HEAD_CONTENT.length());
        } else {
            // If we have none of the above, we probably have a fragment of HTML.  Yahoo! and Gmail both do this.
            // Again, we add a HEAD, but not BODY.
            StringBuilder newContent = new StringBuilder(content);
            // Add the HTML and HEAD tags.
            newContent.insert(FIND_INSERTION_POINT_START_OF_STRING, FIND_INSERTION_POINT_HEAD_CONTENT);
            newContent.insert(FIND_INSERTION_POINT_START_OF_STRING, FIND_INSERTION_POINT_HTML_CONTENT);
            // Append the </HTML> tag.
            newContent.append(FIND_INSERTION_POINT_HTML_END_CONTENT);
            insertable.setQuotedContent(newContent);
            insertable.setHeaderInsertionPoint(FIND_INSERTION_POINT_HTML_CONTENT.length() + FIND_INSERTION_POINT_HEAD_CONTENT.length());
        }

        // Search for closing tags. We have to do this after we deal with opening tags since it may
        // have modified the message.
        boolean hasHtmlEndTag = false;
        boolean hasBodyEndTag = false;
        // First see if we have an opening HTML tag.  If we don't find one, we'll add one later.
        Matcher htmlEndMatcher = FIND_INSERTION_POINT_HTML_END.matcher(insertable.getQuotedContent());
        if (htmlEndMatcher.matches()) {
            hasHtmlEndTag = true;
        }
        // Look for a BODY tag.  This is the ideal place for us to place our footer.
        Matcher bodyEndMatcher = FIND_INSERTION_POINT_BODY_END.matcher(insertable.getQuotedContent());
        if (bodyEndMatcher.matches()) {
            hasBodyEndTag = true;
        }

        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "Close: hasHtmlEndTag:" + hasHtmlEndTag + " hasBodyEndTag:" + hasBodyEndTag);
        }

        // Now figure out where to put our footer.
        // This is the ideal case -- there's a BODY tag and we insert ourselves just before it.
        if (hasBodyEndTag) {
            insertable.setFooterInsertionPoint(bodyEndMatcher.start(FIND_INSERTION_POINT_FIRST_GROUP));
        } else if (hasHtmlEndTag) {
            // Check for an HTML tag.  Add ourselves just before it.
            insertable.setFooterInsertionPoint(htmlEndMatcher.start(FIND_INSERTION_POINT_FIRST_GROUP));
        } else {
            // If we have none of the above, we probably have a fragment of HTML.
            // Set our footer insertion point as the end of the string.
            insertable.setFooterInsertionPoint(insertable.getQuotedContent().length());
        }

        return insertable;
    }

    class Listener extends MessagingListener {
        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid) {
            if ((mMessageReference == null) || !mMessageReference.getUid().equals(uid)) {
                return;
            }

            mHandler.sendEmptyMessage(MSG_PROGRESS_ON);
        }

        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid, LocalMessage message) {
            if ((mMessageReference == null) || !mMessageReference.getUid().equals(uid)) {
                return;
            }

            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
        }

        @Override
        public void loadMessageForViewBodyAvailable(Account account, String folder, String uid, final Message message) {
            if ((mMessageReference == null) || !mMessageReference.getUid().equals(uid)) {
                return;
            }

            mSourceMessage = message;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // We check to see if we've previously processed the source message since this
                    // could be called when switching from HTML to text replies. If that happens, we
                    // only want to update the UI with quoted text (which picks the appropriate
                    // part).
                    if (mSourceProcessed) {
                        try {
                            populateUIWithQuotedMessage(true);
                        } catch (MessagingException e) {
                            // Hm, if we couldn't populate the UI after source reprocessing, let's just delete it?
                            showOrHideQuotedText(QuotedTextMode.HIDE);
                            Log.e(K9.LOG_TAG, "Could not re-process source message; deleting quoted text to be safe.", e);
                        }
                        updateMessageFormat();
                    } else {
                        processSourceMessage((LocalMessage) message);
                        mSourceProcessed = true;
                    }
                }
            });
        }

        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid, Throwable t) {
            if ((mMessageReference == null) || !mMessageReference.getUid().equals(uid)) {
                return;
            }
            mHandler.sendEmptyMessage(MSG_PROGRESS_OFF);
            // TODO show network error
        }

        @Override
        public void messageUidChanged(Account account, String folder, String oldUid, String newUid) {
            // Track UID changes of the source message
            if (mMessageReference != null) {
                final Account sourceAccount = Preferences.getPreferences(MessageCompose.this).getAccount(mMessageReference.getAccountUuid());
                final String sourceFolder = mMessageReference.getFolderName();
                final String sourceMessageUid = mMessageReference.getUid();

                if (account.equals(sourceAccount) && (folder.equals(sourceFolder))) {
                    if (oldUid.equals(sourceMessageUid)) {
                        mMessageReference = mMessageReference.withModifiedUid(newUid);
                    }
                    if ((mSourceMessage != null) && (oldUid.equals(mSourceMessage.getUid()))) {
                        mSourceMessage.setUid(newUid);
                    }
                }
            }
        }
    }

    /**
     * When we are launched with an intent that includes a mailto: URI, we can actually
     * gather quite a few of our message fields from it.
     *
     * @param mailtoUri
     *         The mailto: URI we use to initialize the message fields.
     */
    private void initializeFromMailto(Uri mailtoUri) {
        String schemaSpecific = mailtoUri.getSchemeSpecificPart();
        int end = schemaSpecific.indexOf('?');
        if (end == -1) {
            end = schemaSpecific.length();
        }

        // Extract the recipient's email address from the mailto URI if there's one.
        String recipient = Uri.decode(schemaSpecific.substring(0, end));

        /*
         * mailto URIs are not hierarchical. So calling getQueryParameters()
         * will throw an UnsupportedOperationException. We avoid this by
         * creating a new hierarchical dummy Uri object with the query
         * parameters of the original URI.
         */
        CaseInsensitiveParamWrapper uri = new CaseInsensitiveParamWrapper(
                Uri.parse("foo://bar?" + mailtoUri.getEncodedQuery()));

        // Read additional recipients from the "to" parameter.
        List<String> to = uri.getQueryParameters("to");
        if (recipient.length() != 0) {
            to = new ArrayList<String>(to);
            to.add(0, recipient);
        }
        mToView.addRecipients(to);

        // Read carbon copy recipients from the "cc" parameter.
        boolean ccOrBcc = mCcView.addRecipients(uri.getQueryParameters("cc"));

        // Read blind carbon copy recipients from the "bcc" parameter.
        ccOrBcc |= mBccView.addRecipients(uri.getQueryParameters("bcc"));

        if (ccOrBcc) {
            // Display CC and BCC text fields if CC or BCC recipients were set by the intent.
            onAddCcBcc();
        }

        // Read subject from the "subject" parameter.
        List<String> subject = uri.getQueryParameters("subject");
        if (!subject.isEmpty()) {
            mSubjectView.setText(subject.get(0));
        }

        // Read message body from the "body" parameter.
        List<String> body = uri.getQueryParameters("body");
        if (!body.isEmpty()) {
            mMessageContentView.setCharacters(body.get(0));
        }
    }

    private static class CaseInsensitiveParamWrapper {
        private final Uri uri;
        private Set<String> mParamNames;

        public CaseInsensitiveParamWrapper(Uri uri) {
            this.uri = uri;
        }

        public List<String> getQueryParameters(String key) {
            final List<String> params = new ArrayList<String>();
            for (String paramName : uri.getQueryParameterNames()) {
                if (paramName.equalsIgnoreCase(key)) {
                    params.addAll(uri.getQueryParameters(paramName));
                }
            }
            return params;
        }

    }

    private class SendMessageTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            /*
             * Create the message from all the data the user has entered.
             */
            MimeMessage message;
            try {
                if(currentMessage == null) {
                    message = createMessage();
                } else {
                    message = currentMessage;
                }
            } catch (MessagingException me) {
                Log.e(K9.LOG_TAG, "Failed to create new message for send or save.", me);
                throw new RuntimeException("Failed to create a new message for send or save.", me);
            }

            try {
                mContacts.markAsContacted(message.getRecipients(RecipientType.TO));
                mContacts.markAsContacted(message.getRecipients(RecipientType.CC));
                mContacts.markAsContacted(message.getRecipients(RecipientType.BCC));
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Failed to mark contact as contacted.", e);
            }

            MessagingController.getInstance(getApplication()).sendMessage(mAccount, message, null);
            long draftId = mDraftId;
            if (draftId != INVALID_DRAFT_ID) {
                mDraftId = INVALID_DRAFT_ID;
                MessagingController.getInstance(getApplication()).deleteDraft(mAccount, draftId);
            }

            return null;
        }
    }

    private class SaveMessageTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            /*
             * Create the message from all the data the user has entered.
             */
            MimeMessage message;
            try {
                message = createDraftMessage();
            } catch (MessagingException me) {
                Log.e(K9.LOG_TAG, "Failed to create new message for send or save.", me);
                throw new RuntimeException("Failed to create a new message for send or save.", me);
            }

            /*
             * Save a draft
             */
            if (mAction == Action.EDIT_DRAFT) {
                /*
                 * We're saving a previously saved draft, so update the new message's uid
                 * to the old message's uid.
                 */
                if (mMessageReference != null) {
                    message.setUid(mMessageReference.getUid());
                }
            }

            final MessagingController messagingController = MessagingController.getInstance(getApplication());
            Message draftMessage = messagingController.saveDraft(mAccount, message, mDraftId);
            mDraftId = messagingController.getId(draftMessage);

            mHandler.sendEmptyMessage(MSG_SAVED_DRAFT);
            return null;
        }
    }

    private static final int REPLY_WRAP_LINE_WIDTH = 72;
    private static final int QUOTE_BUFFER_LENGTH = 512; // amount of extra buffer to allocate to accommodate quoting headers or prefixes

    /**
     * Add quoting markup to a text message.
     * @param originalMessage Metadata for message being quoted.
     * @param messageBody Text of the message to be quoted.
     * @param quoteStyle Style of quoting.
     * @return Quoted text.
     * @throws MessagingException
     */
    private String quoteOriginalTextMessage(final Message originalMessage, final String messageBody, final QuoteStyle quoteStyle) throws MessagingException {
        String body = messageBody == null ? "" : messageBody;
        String sentDate = getSentDateText(originalMessage);
        if (quoteStyle == QuoteStyle.PREFIX) {
            StringBuilder quotedText = new StringBuilder(body.length() + QUOTE_BUFFER_LENGTH);
            if (sentDate.length() != 0) {
                quotedText.append(String.format(
                        getString(R.string.message_compose_reply_header_fmt_with_date) + "\r\n",
                        sentDate,
                        Address.toString(originalMessage.getFrom())));
            } else {
                quotedText.append(String.format(
                                      getString(R.string.message_compose_reply_header_fmt) + "\r\n",
                                      Address.toString(originalMessage.getFrom()))
                                 );
            }

            final String prefix = mAccount.getQuotePrefix();
            final String wrappedText = Utility.wrap(body, REPLY_WRAP_LINE_WIDTH - prefix.length());

            // "$" and "\" in the quote prefix have to be escaped for
            // the replaceAll() invocation.
            final String escapedPrefix = prefix.replaceAll("(\\\\|\\$)", "\\\\$1");
            quotedText.append(wrappedText.replaceAll("(?m)^", escapedPrefix));

            return quotedText.toString().replaceAll("\\\r", "");
        } else if (quoteStyle == QuoteStyle.HEADER) {
            StringBuilder quotedText = new StringBuilder(body.length() + QUOTE_BUFFER_LENGTH);
            quotedText.append("\r\n");
            quotedText.append(getString(R.string.message_compose_quote_header_separator)).append("\r\n");

            if (originalMessage.getFrom() != null && Address.toString(originalMessage.getFrom()).length() != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_from)).append(" ").append(Address.toString(originalMessage.getFrom())).append("\r\n");
            }

            if (sentDate.length() != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_send_date)).append(" ").append(sentDate).append("\r\n");
            }

            if (originalMessage.getRecipients(RecipientType.TO) != null && originalMessage.getRecipients(RecipientType.TO).length != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_to)).append(" ").append(Address.toString(originalMessage.getRecipients(RecipientType.TO))).append("\r\n");
            }

            if (originalMessage.getRecipients(RecipientType.CC) != null && originalMessage.getRecipients(RecipientType.CC).length != 0) {
                quotedText.append(getString(R.string.message_compose_quote_header_cc)).append(" ").append(Address.toString(originalMessage.getRecipients(RecipientType.CC))).append("\r\n");
            }

            if (originalMessage.getSubject() != null) {
                quotedText.append(getString(R.string.message_compose_quote_header_subject)).append(" ").append(originalMessage.getSubject()).append("\r\n");
            }

            quotedText.append("\r\n");
            quotedText.append(body);

            return quotedText.toString();
        } else {
            // Shouldn't ever happen.
            return body;
        }
    }

    /**
     * Add quoting markup to a HTML message.
     * @param originalMessage Metadata for message being quoted.
     * @param messageBody Text of the message to be quoted.
     * @param quoteStyle Style of quoting.
     * @return Modified insertable message.
     * @throws MessagingException
     */
    private InsertableHtmlContent quoteOriginalHtmlMessage(final Message originalMessage, final String messageBody, final QuoteStyle quoteStyle) throws MessagingException {
        InsertableHtmlContent insertable = findInsertionPoints(messageBody);

        String sentDate = getSentDateText(originalMessage);
        if (quoteStyle == QuoteStyle.PREFIX) {
            StringBuilder header = new StringBuilder(QUOTE_BUFFER_LENGTH);
            header.append("<div class=\"gmail_quote\">");
            if (sentDate.length() != 0) {
                header.append(HtmlConverter.textToHtmlFragment(String.format(
                        getString(R.string.message_compose_reply_header_fmt_with_date),
                        sentDate,
                        Address.toString(originalMessage.getFrom()))
                                                    ));
            } else {
                header.append(HtmlConverter.textToHtmlFragment(String.format(
                                  getString(R.string.message_compose_reply_header_fmt),
                                  Address.toString(originalMessage.getFrom()))
                                                              ));
            }
            header.append("<blockquote class=\"gmail_quote\" " +
                          "style=\"margin: 0pt 0pt 0pt 0.8ex; border-left: 1px solid rgb(204, 204, 204); padding-left: 1ex;\">\r\n");

            String footer = "</blockquote></div>";

            insertable.insertIntoQuotedHeader(header.toString());
            insertable.insertIntoQuotedFooter(footer);
        } else if (quoteStyle == QuoteStyle.HEADER) {

            StringBuilder header = new StringBuilder();
            header.append("<div style='font-size:10.0pt;font-family:\"Tahoma\",\"sans-serif\";padding:3.0pt 0in 0in 0in'>\r\n");
            header.append("<hr style='border:none;border-top:solid #E1E1E1 1.0pt'>\r\n"); // This gets converted into a horizontal line during html to text conversion.
            if (originalMessage.getFrom() != null && Address.toString(originalMessage.getFrom()).length() != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_from)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(Address.toString(originalMessage.getFrom())))
                    .append("<br>\r\n");
            }

            if (sentDate.length() != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_send_date)).append("</b> ")
                    .append(sentDate)
                    .append("<br>\r\n");
            }

            if (originalMessage.getRecipients(RecipientType.TO) != null && originalMessage.getRecipients(RecipientType.TO).length != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_to)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(Address.toString(originalMessage.getRecipients(RecipientType.TO))))
                    .append("<br>\r\n");
            }

            if (originalMessage.getRecipients(RecipientType.CC) != null && originalMessage.getRecipients(RecipientType.CC).length != 0) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_cc)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(Address.toString(originalMessage.getRecipients(RecipientType.CC))))
                    .append("<br>\r\n");
            }

            if (originalMessage.getSubject() != null) {
                header.append("<b>").append(getString(R.string.message_compose_quote_header_subject)).append("</b> ")
                    .append(HtmlConverter.textToHtmlFragment(originalMessage.getSubject()))
                    .append("<br>\r\n");
            }

            header.append("</div>\r\n");
            header.append("<br>\r\n");

            insertable.insertIntoQuotedHeader(header.toString());
        }

        return insertable;
    }

    private void setMessageFormat(SimpleMessageFormat format) {
        // This method will later be used to enable/disable the rich text editing mode.

        mMessageFormat = format;
    }

    private void updateMessageFormat() {
        MessageFormat origMessageFormat = mAccount.getMessageFormat();
        SimpleMessageFormat messageFormat;

        if (origMessageFormat == MessageFormat.TEXT) {
            // The user wants to send text/plain messages. We don't override that choice under
            // any circumstances.
            messageFormat = SimpleMessageFormat.TEXT;
        } else if (mForcePlainText && includeQuotedText()) {
            // Right now we send a text/plain-only message when the quoted text was edited, no
            // matter what the user selected for the message format.
            messageFormat = SimpleMessageFormat.TEXT;
        } else if (shouldEncrypt() || shouldSign()) {
            // Right now we only support PGP inline which doesn't play well with HTML. So force
            // plain text in those cases.
            messageFormat = SimpleMessageFormat.TEXT;
        } else if (origMessageFormat == MessageFormat.AUTO) {
            if (mAction == Action.COMPOSE || mQuotedTextFormat == SimpleMessageFormat.TEXT ||
                    !includeQuotedText()) {
                // If the message format is set to "AUTO" we use text/plain whenever possible. That
                // is, when composing new messages and replying to or forwarding text/plain
                // messages.
                messageFormat = SimpleMessageFormat.TEXT;
            } else {
                messageFormat = SimpleMessageFormat.HTML;
            }
        } else {
            // In all other cases use HTML
            messageFormat = SimpleMessageFormat.HTML;
        }

        setMessageFormat(messageFormat);
    }

    private boolean includeQuotedText() {
        return (mQuotedTextMode == QuotedTextMode.SHOW);
    }

    /**
     * Extract the date from a message and convert it into a locale-specific
     * date string suitable for use in a header for a quoted message.
     *
     * @param message
     * @return A string with the formatted date/time
     */
    private String getSentDateText(Message message) {
        try {
            final int dateStyle = DateFormat.LONG;
            final int timeStyle = DateFormat.LONG;
            Date date = message.getSentDate();
            Locale locale = getResources().getConfiguration().locale;
            return DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale)
                    .format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isCryptoProviderEnabled() {
        return mOpenPgpProvider != null || mSmimeProvider != null;
    }

    private boolean shouldEncrypt() {
        return isCryptoProviderEnabled() && mEncryptCheckbox.isChecked();
    }

    private boolean shouldSign() {
        return isCryptoProviderEnabled() && mCryptoSignatureCheckbox.isChecked();
    }

    class DoLaunchOnClickListener implements View.OnClickListener {

        private final int resultId;

        DoLaunchOnClickListener(int resultId) {
            this.resultId = resultId;
        }

        @Override
        public void onClick(View v) {
            mIgnoreOnPause = true;
            startActivityForResult(mContacts.contactPickerIntent(), resultId);
        }
    }

    protected void setMessage(MimeMessage message) {
        if(message != null) {
            currentMessage = message;
        }
    }

    public LinearLayout getAttachments() {
        return mAttachments;
    }
}

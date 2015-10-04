package com.fsck.k9.ui.messageview;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.RemindMeList;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.fragment.ProgressDialogFragment;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;
import com.fsck.k9.ui.crypto.MessageCryptoCallback;
import com.fsck.k9.ui.crypto.MessageCryptoHelper;
import com.fsck.k9.ui.crypto.PgpMessageCryptoHelper;
import com.fsck.k9.ui.crypto.SmimeMessageCryptoHelper;
import com.github.clans.fab.FloatingActionButton;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;

public final class MessageViewFragment extends Fragment
        implements ConfirmationDialogFragmentListener,
        CryptoHeaderViewCallback,
        MessageCryptoCallback {

    public static final String ARG_REFERENCE = "reference";
    public static final String ARG_MESSAGE = "message";
    public static final String ARG_ANNOTATIONS = "annotations";
    static final int ACTIVITY_CHOOSE_DIRECTORY = 3;
    private static final String STATE_MESSAGE_REFERENCE = "reference";
    private static final String STATE_PGP_DATA = "pgpData";
    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    private static final int LOCAL_MESSAGE_LOADER_ID = 1;
    private static final int DECODE_MESSAGE_LOADER_ID = 2;
    private final MessageViewHandler handler = new MessageViewHandler(this);
    private final DownloadMessageListener downloadMessageListener = new DownloadMessageListener(handler);
    private MessageTopView mMessageView;
    private PgpData mPgpData;
    private Account mAccount;
    private MessageReference mMessageReference;
    private LocalMessage mMessage;
    private MessagingController mController;
    private MessageCryptoHelper messageCryptoHelper;
    /**
     * Used to temporarily store the destination folder for refile operations if a confirmation
     * dialog is shown.
     */
    private String mDstFolder;
    private MessageViewFragmentListener mFragmentListener;
    /**
     * {@code true} after {@link #onCreate(Bundle)} has been executed. This is used by
     * {@code MessageList.configureMenu()} to make sure the fragment has been initialized before
     * it is used.
     */
    private boolean mInitialized = false;
    private Context mContext;
    private LoaderManager.LoaderCallbacks<LocalMessage> localMessageLoaderCallback;
    private LoaderManager.LoaderCallbacks<MessageViewInfo> decodeMessageLoaderCallback;

    public static MessageViewFragment newInstance(MessageReference reference) {
        MessageViewFragment fragment = new MessageViewFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_REFERENCE, reference);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContext = activity;
        decodeMessageLoaderCallback = new DecodeMessageLoaderCallback(mContext, handler);
        mController = MessagingController.getInstance(mContext);

        try {
            mFragmentListener = (MessageViewFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() +
                    " must implement MessageViewFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This fragments adds options to the action bar
        setHasOptionsMenu(true);
        mInitialized = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View view = layoutInflater.inflate(R.layout.message, container, false);

        mMessageView = findById(view, R.id.message_view);
        mMessageView.setAttachmentCallback(new AttachmentCallback(mContext, mController, handler, this));
        mMessageView.setCryptoHeaderViewCallback(this);
        mMessageView.setOnToggleFlagClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleFlagged();
            }
        });

        mMessageView.setOnDownloadButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onDownloadRemainder();
            }
        });

        mFragmentListener.messageHeaderViewAvailable(mMessageView.getMessageHeaderView());

        final FloatingActionButton fabReply = findById(view, R.id.fab_reply);
        fabReply.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onReply();
            }
        });

        final FloatingActionButton fabReplyAll = findById(view, R.id.fab_reply_all);
        fabReplyAll.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                onReplyAll();
            }
        });

        final FloatingActionButton fabForward = findById(view, R.id.fab_forward);
        fabForward.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                onForward();
            }
        });

        return view;
    }

    // restore state
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MessageReference messageReference;
        if (savedInstanceState != null) {
            mPgpData = (PgpData) savedInstanceState.get(STATE_PGP_DATA);
            messageReference = (MessageReference) savedInstanceState.get(STATE_MESSAGE_REFERENCE);
        } else {
            Bundle args = getArguments();
            messageReference = args.getParcelable(ARG_REFERENCE);
        }

        displayMessage(messageReference, (mPgpData == null));
    }

    // save state
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_MESSAGE_REFERENCE, mMessageReference);
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(messageCryptoHelper != null) {
            try {
                messageCryptoHelper.close();
            } catch (IOException e) {
                Log.e(K9.LOG_TAG, "error on close crypto helper", e);
            }
        }
    }

    private void displayMessage(final MessageReference ref, final boolean resetPgpData) {
        mMessageReference = ref;
        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "MessageView displaying message " + mMessageReference);
        }

        mAccount = Preferences.getPreferences(mContext).getAccount(mMessageReference.getAccountUuid());
        localMessageLoaderCallback = new LocalMessageLoaderCallback(handler, mContext, mAccount, mMessageReference);
        if (resetPgpData) {
            // start with fresh, empty PGP data
            mPgpData = new PgpData();
        }

        // Clear previous message
        mMessageView.resetView();
        mMessageView.resetHeaderView();

        startLoadingMessageFromDatabase();

        mFragmentListener.updateMenu();
    }

    private void startLoadingMessageFromDatabase() {
        getLoaderManager().initLoader(LOCAL_MESSAGE_LOADER_ID, null, localMessageLoaderCallback);
    }

    void onLoadMessageFromDatabaseFinished(LocalMessage message) {
        mMessage = message;
        displayMessageHeader(message);

        // TODO: download message body if it is missing
        if (!message.isBodyMissing()) {
            try {
                if (MessageDecryptVerifier.isMimeSignedPart(message)) {
                    startExtractingTextAndAttachments(new MessageCryptoAnnotations());
                }

                if (MessageDecryptVerifier.isSmimePart(message)) {
                    messageCryptoHelper = new SmimeMessageCryptoHelper(getActivity(), this, mAccount.getSmimeProvider(), mAccount.getOpenPgpProvider());
                } else {
                    messageCryptoHelper = new PgpMessageCryptoHelper(getActivity(), this, mAccount.getSmimeProvider(), mAccount.getOpenPgpProvider());
                }
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "error loading message", e);
            }

            messageCryptoHelper.decryptOrVerifyMessagePartsIfNecessary(message);
        }
    }

    void onLoadMessageFromDatabaseFailed() {
        // mMessageView.showStatusMessage(mContext.getString(R.string.status_invalid_id_error));
    }

    public void handleCryptoResult(int requestCode, int resultCode, Intent data) {
        if (messageCryptoHelper != null) {
            messageCryptoHelper.handleCryptoResult(requestCode, resultCode, data);
        }
    }

    public void onMessageDownloadFinished(LocalMessage message) {
        mMessage = message;

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.destroyLoader(LOCAL_MESSAGE_LOADER_ID);
        loaderManager.destroyLoader(DECODE_MESSAGE_LOADER_ID);

        onLoadMessageFromDatabaseFinished(mMessage);
    }

    void onDownloadMessageFailed(Throwable t) {
        mMessageView.enableDownloadButton();
        String errorMessage;
        if (t instanceof IllegalArgumentException) {
            errorMessage = mContext.getString(R.string.status_invalid_id_error);
        } else {
            errorMessage = mContext.getString(R.string.status_network_error);
        }
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCryptoOperationsFinished(final MessageCryptoAnnotations annotations) {
        startExtractingTextAndAttachments(annotations);
    }

    private void startExtractingTextAndAttachments(final MessageCryptoAnnotations annotations) {
        final Bundle args = new Bundle();
        args.putSerializable(ARG_MESSAGE, mMessage);
        args.putSerializable(ARG_ANNOTATIONS, annotations);

        if (getActivity() != null) {
            getLoaderManager().restartLoader(DECODE_MESSAGE_LOADER_ID, args, decodeMessageLoaderCallback);
        }
    }

    void showMessage(final MessageViewInfo messageContainer) {
        try {
            if (messageContainer != null) {
                mMessageView.setMessage(mAccount, messageContainer);
                mMessageView.setShowDownloadButton(mMessage);
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Error while trying to display message", e);
        }
    }

    private void displayMessageHeader(final LocalMessage message) {
        mMessageView.setHeaders(message, mAccount);
        displayMessageSubject(getSubjectForMessage(message));
        mFragmentListener.updateMenu();
    }

    /**
     * Called from UI thread when user select Delete
     */
    public void onDelete() {
        if (K9.confirmDelete() || (K9.confirmDeleteStarred() && mMessage.isSet(Flag.FLAGGED))) {
            showDialog(R.id.dialog_confirm_delete);
        } else {
            delete();
        }
    }

    public void onToggleAllHeadersView() {
        mMessageView.getMessageHeaderView().onShowAdditionalHeaders();
    }

    public boolean allHeadersVisible() {
        return mMessageView.getMessageHeaderView().additionalHeadersVisible();
    }

    private void delete() {
        if (mMessage != null) {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            mFragmentListener.disableDeleteAction();
            LocalMessage messageToDelete = mMessage;
            mFragmentListener.showNextMessageOrReturn();
            mController.deleteMessages(Collections.singletonList(messageToDelete), null);
        }
    }

    public void onRefile(final String dstFolder) {
        if (!mController.isMoveCapable(mAccount)) {
            return;
        }

        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder)) {
            return;
        }

        if (mAccount.getSpamFolderName().equals(dstFolder) && K9.confirmSpam()) {
            mDstFolder = dstFolder;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            refileMessage(dstFolder);
        }
    }

    private void refileMessage(final String dstFolder) {
        String srcFolder = mMessageReference.getFolderName();
        LocalMessage messageToMove = mMessage;
        mFragmentListener.showNextMessageOrReturn();
        mController.moveMessage(mAccount, srcFolder, messageToMove, dstFolder, null);
    }

    public void onReply() {
        if (mMessage != null) {
            mFragmentListener.onReply(mMessage, mPgpData);
        }
    }

    public void onReplyAll() {
        if (mMessage != null) {
            mFragmentListener.onReplyAll(mMessage, mPgpData);
        }
    }

    public void onForward() {
        if (mMessage != null) {
            mFragmentListener.onForward(mMessage, mPgpData);
        }
    }

    public void onToggleFlagged() {
        if (mMessage != null) {
            boolean newState = !mMessage.isSet(Flag.FLAGGED);
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    Collections.singletonList(mMessage), Flag.FLAGGED, newState);
            mMessageView.setHeaders(mMessage, mAccount);
        }
    }

    public void onMove() {
        if ((!mController.isMoveCapable(mAccount)) || (mMessage == null)) {
            return;
        }

        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_MOVE);
    }

    public void onRemindMe() {
        startActivity(RemindMeList.createRemindMe(this.getActivity(), mMessage));
    }

    public void onCopy() {
        if ((!mController.isCopyCapable(mAccount)) || (mMessage == null)) {
            return;
        }

        if (!mController.isCopyCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    public void onArchive() {
        onRefile(mAccount.getArchiveFolderName());
    }

    public void onSpam() {
        onRefile(mAccount.getSpamFolderName());
    }

    public void onSelectText() {
        // FIXME
        // mMessageView.beginSelectingText();
    }

    private void startRefileActivity(final int activity) {
        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.getFolderName());
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        startActivityForResult(intent, activity);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case ACTIVITY_CHOOSE_DIRECTORY: {
                if (data != null) {
                    // obtain the filename
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        String filePath = fileUri.getPath();
                        AttachmentViewInfo attachmentViewInfo = (AttachmentViewInfo) data.getSerializableExtra("attachmentInfo");
                        if (filePath != null) {
                            getAttachmentController(attachmentViewInfo).saveAttachmentTo(filePath);
                        }
                    }
                }
                break;
            }
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY: {
                if (data == null) {
                    return;
                }

                String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
                MessageReference ref = data.getParcelableExtra(ChooseFolder.EXTRA_MESSAGE);
                if (mMessageReference.equals(ref)) {
                    mAccount.setLastSelectedFolderName(destFolderName);
                    switch (requestCode) {
                        case ACTIVITY_CHOOSE_FOLDER_MOVE: {
                            mFragmentListener.showNextMessageOrReturn();
                            moveMessage(ref, destFolderName);
                            break;
                        }
                        case ACTIVITY_CHOOSE_FOLDER_COPY: {
                            copyMessage(ref, destFolderName);
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    public void onSendAlternate() {
        if (mMessage != null) {
            mController.sendAlternate(getActivity(), mAccount, mMessage);
        }
    }

    public void onToggleRead() {
        if (mMessage != null) {
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    Collections.singletonList(mMessage), Flag.SEEN, !mMessage.isSet(Flag.SEEN));
            mMessageView.setHeaders(mMessage, mAccount);
            String subject = mMessage.getSubject();
            displayMessageSubject(subject);
            mFragmentListener.updateMenu();
        }
    }

    private void onDownloadRemainder() {
        if (mMessage.isSet(Flag.X_DOWNLOADED_FULL)) {
            return;
        }

        mMessageView.disableDownloadButton();
        mController.loadMessageForViewRemote(mAccount, mMessageReference.getFolderName(), mMessageReference.getUid(),
                downloadMessageListener);
    }

    @Override
    public void setProgress(boolean enable) {
        if (mFragmentListener != null) {
            mFragmentListener.setProgress(enable);
        }
    }

    private void displayMessageSubject(String subject) {
        if (mFragmentListener != null) {
            mFragmentListener.displayMessageSubject(subject);
        }
    }

    private String getSubjectForMessage(LocalMessage message) {
        String subject = message.getSubject();
        if (TextUtils.isEmpty(subject)) {
            return mContext.getString(R.string.general_no_subject);
        }

        return subject;
    }

    public void moveMessage(MessageReference reference, String destFolderName) {
        mController.moveMessage(mAccount, mMessageReference.getFolderName(), mMessage,
                destFolderName, null);
    }

    public void copyMessage(MessageReference reference, String destFolderName) {
        mController.copyMessage(mAccount, mMessageReference.getFolderName(), mMessage,
                destFolderName, null);
    }

    void showDialog(int dialogId) {
        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                String title = getString(R.string.dialog_confirm_delete_title);
                String message = getString(R.string.dialog_confirm_delete_message);
                String confirmText = getString(R.string.dialog_confirm_delete_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_delete_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_confirm_spam: {
                String title = getString(R.string.dialog_confirm_spam_title);
                String message = getResources().getQuantityString(R.plurals.dialog_confirm_spam_message, 1);
                String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_attachment_progress: {
                String message = getString(R.string.dialog_attachment_progress_title);
                fragment = ProgressDialogFragment.newInstance(null, message);
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        fragment.setTargetFragment(this, dialogId);
        fragment.show(getFragmentManager(), getDialogTag(dialogId));
    }

    void removeDialog(int dialogId) {
        FragmentManager fragmentManager = getFragmentManager();

        if (fragmentManager == null || isRemoving() || isDetached()) {
            return;
        }

        // Make sure the "show dialog" transaction has been processed when we call
        // findFragmentByTag() below. Otherwise the fragment won't be found and the dialog will
        // never be dismissed.
        fragmentManager.executePendingTransactions();
        DialogFragment fragment = (DialogFragment) fragmentManager.findFragmentByTag(getDialogTag(dialogId));

        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private String getDialogTag(int dialogId) {
        return String.format(Locale.US, "dialog-%d", dialogId);
    }

    public void zoom(KeyEvent event) {
        // mMessageView.zoom(event);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                delete();
                break;
            }
            case R.id.dialog_confirm_spam: {
                refileMessage(mDstFolder);
                mDstFolder = null;
                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        /* do nothing */
    }

    @Override
    public void dialogCancelled(int dialogId) {
        /* do nothing */
    }

    /**
     * Get the {@link MessageReference} of the currently displayed message.
     */
    public MessageReference getMessageReference() {
        return mMessageReference;
    }

    public boolean isMessageRead() {
        return (mMessage != null) && mMessage.isSet(Flag.SEEN);
    }

    public boolean isCopyCapable() {
        return mController.isCopyCapable(mAccount);
    }

    public boolean isMoveCapable() {
        return mController.isMoveCapable(mAccount);
    }

    public boolean canMessageBeArchived() {
        return (!mMessageReference.getFolderName().equals(mAccount.getArchiveFolderName())
                && mAccount.hasArchiveFolder());
    }

    public boolean canMessageBeMovedToSpam() {
        return (!mMessageReference.getFolderName().equals(mAccount.getSpamFolderName())
                && mAccount.hasSpamFolder());
    }

    public void updateTitle() {
        if (mMessage != null) {
            displayMessageSubject(mMessage.getSubject());
        }
    }

    @Override
    public void onSignatureButtonClick(PendingIntent pendingIntent) {
        try {
            getActivity().startIntentSenderForResult(
                    pendingIntent.getIntentSender(),
                    42, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.e(K9.LOG_TAG, "SendIntentException", e);
        }
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    private AttachmentController getAttachmentController(AttachmentViewInfo attachment) {
        return new AttachmentController(mContext, mController, attachment, handler);
    }
}

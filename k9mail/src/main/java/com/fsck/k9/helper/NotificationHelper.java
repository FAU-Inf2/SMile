package com.fsck.k9.helper;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.K9;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.NotificationDeleteConfirmation;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.AccountSetupIncoming;
import com.fsck.k9.activity.setup.AccountSetupOutgoing;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.service.NotificationActionService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.fau.cs.mad.smile.android.R;

public class NotificationHelper {
    // Maximum number of senders to display in a lock screen notification.
    private static final int NUM_SENDERS_IN_LOCK_SCREEN_NOTIFICATION = 5;
    private static NotificationHelper instance = null;

    private final Context context;
    private final ConcurrentMap<Integer, NotificationData> notificationData;
    private TextAppearanceSpan sEmphasizedSpan;

    public static synchronized NotificationHelper getInstance(Context context) {
        if(instance == null) {
            instance = new NotificationHelper(context);
        }

        return instance;
    }

    private NotificationHelper(Context context) {
        this.context = context;
        notificationData = new ConcurrentHashMap<>();
    }

    public void cancelNotification(int id) {
        NotificationManager notifMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notifMgr.cancel(id);
    }

    public void notifyWhileSendingDone(Account account) {
        if (account.isShowOngoing()) {
            cancelNotification(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber());
        }
    }

    /**
     * Display an ongoing notification while a message is being sent.
     *
     * @param account The account the message is sent from. Never {@code null}.
     */
    public void notifyWhileSending(Account account) {
        if (!account.isShowOngoing()) {
            return;
        }

        NotificationManager notifMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notify_check_mail);
        builder.setWhen(System.currentTimeMillis());
        builder.setOngoing(true);

        String accountDescription = account.getDescription();
        String accountName = (TextUtils.isEmpty(accountDescription)) ?
                account.getEmail() : accountDescription;

        builder.setTicker(context.getString(R.string.notification_bg_send_ticker,
                accountName));

        builder.setContentTitle(context.getString(R.string.notification_bg_send_title));
        builder.setContentText(account.getDescription());

        TaskStackBuilder stack = buildMessageListBackStack(context, account,
                account.getInboxFolderName());
        builder.setContentIntent(stack.getPendingIntent(0, 0));
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (K9.NOTIFICATION_LED_WHILE_SYNCING) {
            configureNotification(builder, null, null,
                    account.getNotificationSetting().getLedColor(),
                    K9.NOTIFICATION_LED_BLINK_FAST, true);
        }

        notifMgr.notify(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber(),
                builder.build());
    }

    /**
     * Cancel a notification of new email messages
     */
    public void notifyAccountCancel(Context context, Account account) {
        NotificationManager notifMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifMgr.cancel(account.getAccountNumber());
        notifMgr.cancel(-1000 - account.getAccountNumber());
        notificationData.remove(account.getAccountNumber());
    }

    /**
     * Creates a notification of a newly received message.
     */
    public void notifyAccount(Context context, Account account, LocalMessage message, int previousUnreadMessageCount, boolean removeMessage) {
        if (K9.isQuietTime() && !K9.isNotificationDuringQuietTimeEnabled()) {
            return;
        }

        final NotificationData data = getNotificationData(account, previousUnreadMessageCount);
        synchronized (data) {
            if(removeMessage) {
                if(!data.removeMatchingMessage(context, message.makeMessageReference())) {
                    return;
                }
            }

            notifyAccountWithDataLocked(context, account, message, data);
        }
    }

    public void clearCertificateErrorNotifications(Context context,
                                                   final Account account, AccountSetupCheckSettings.CheckDirection direction) {
        final NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (direction == AccountSetupCheckSettings.CheckDirection.INCOMING) {
            nm.cancel(null, K9.CERTIFICATE_EXCEPTION_NOTIFICATION_INCOMING + account.getAccountNumber());
        } else {
            nm.cancel(null, K9.CERTIFICATE_EXCEPTION_NOTIFICATION_OUTGOING + account.getAccountNumber());
        }
    }

    public void notifyUserIfCertificateProblem(Context context, Exception e,
                                        Account account, boolean incoming) {
        if (!(e instanceof CertificateValidationException)) {
            return;
        }

        CertificateValidationException cve = (CertificateValidationException) e;
        if (!cve.needsUserAttention()) {
            return;
        }

        final int id = incoming
                ? K9.CERTIFICATE_EXCEPTION_NOTIFICATION_INCOMING + account.getAccountNumber()
                : K9.CERTIFICATE_EXCEPTION_NOTIFICATION_OUTGOING + account.getAccountNumber();
        final Intent i = incoming
                ? AccountSetupIncoming.intentActionEditIncomingSettings(context, account)
                : AccountSetupOutgoing.intentActionEditOutgoingSettings(context, account);
        final PendingIntent pi = PendingIntent.getActivity(context,
                account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
        final String title = context.getString(
                R.string.notification_certificate_error_title, account.getDescription());

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(platformSupportsLockScreenNotifications()
                ? R.drawable.ic_notify_new_mail_vector
                : R.drawable.ic_notify_new_mail);
        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(true);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(context.getString(R.string.notification_certificate_error_text));
        builder.setContentIntent(pi);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        configureNotification(builder, null, null,
                K9.NOTIFICATION_LED_FAILURE_COLOR,
                K9.NOTIFICATION_LED_BLINK_FAST, true);

        final NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(null, id, builder.build());
    }

    private void notifyAccountWithDataLocked(Context context, Account account,
                                             LocalMessage message, NotificationData data) {
        boolean updateSilently = false;

        if (message == null) {
            /* this can happen if a message we previously notified for is read or deleted remotely */
            message = findNewestMessageForNotificationLocked(context, data);
            updateSilently = true;
            if (message == null) {
                // seemingly both the message list as well as the overflow list is empty;
                // it probably is a good idea to cancel the notification in that case
                notifyAccountCancel(context, account);
                return;
            }
        } else {
            data.addMessage(message);
        }

        final KeyguardManager keyguardService = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        final CharSequence sender = getMessageSender(context, account, message);
        final CharSequence subject = getMessageSubject(context, message);
        CharSequence summary = buildMessageSummary(context, sender, subject);

        boolean privacyModeEnabled =
                (K9.getNotificationHideSubject() == K9.NotificationHideSubject.ALWAYS) ||
                        (K9.getNotificationHideSubject() == K9.NotificationHideSubject.WHEN_LOCKED &&
                                keyguardService.inKeyguardRestrictedInputMode());

        if (privacyModeEnabled || summary.length() == 0) {
            summary = context.getString(R.string.notification_new_title);
        }

        NotificationManager notifMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notify_new_mail);
        builder.setWhen(System.currentTimeMillis());

        if (!updateSilently) {
            builder.setTicker(summary);
        }

        final int newMessages = data.getNewMessageCount();
        final int unreadCount = data.getUnreadBeforeNotification() + newMessages;

        builder.setNumber(unreadCount);

        String accountDescr = (account.getDescription() != null) ?
                account.getDescription() : account.getEmail();
        final ArrayList<MessageReference> allRefs = new ArrayList<>();
        data.supplyAllMessageRefs(allRefs);

        if (platformSupportsExtendedNotifications() && !privacyModeEnabled) {
            if (newMessages > 1) {
                // multiple messages pending, show inbox style
                NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle(builder);
                for (Message m : data.getMessages()) {
                    style.addLine(buildMessageSummary(context,
                            getMessageSender(context, account, m),
                            getMessageSubject(context, m)));
                }

                if (!data.getDroppedMessages().isEmpty()) {
                    style.setSummaryText(context.getString(R.string.notification_additional_messages,
                            data.getDroppedMessages().size(), accountDescr));
                }

                final String title = context.getResources().getQuantityString(
                        R.plurals.notification_new_messages_title, newMessages, newMessages);
                style.setBigContentTitle(title);
                builder.setContentTitle(title);
                builder.setSubText(accountDescr);
                builder.setStyle(style);
            } else {
                // single message pending, show big text
                NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle(builder);
                CharSequence preview = getMessagePreview(context, message);
                if (preview != null) {
                    style.bigText(preview);
                }

                builder.setContentText(subject);
                builder.setSubText(accountDescr);
                builder.setContentTitle(sender);
                builder.setStyle(style);

                builder.addAction(
                        platformSupportsLockScreenNotifications()
                                ? R.drawable.ic_action_single_message_options_dark_vector
                                : R.drawable.ic_action_single_message_options_dark,
                        context.getString(R.string.notification_action_reply),
                        NotificationActionService.getReplyIntent(context, account, message.makeMessageReference()));
            }

            // Mark Read on phone
            builder.addAction(
                    platformSupportsLockScreenNotifications()
                            ? R.drawable.ic_action_mark_as_read_dark_vector
                            : R.drawable.ic_action_mark_as_read_dark,
                    context.getString(R.string.notification_action_mark_as_read),
                    NotificationActionService.getReadAllMessagesIntent(context, account, allRefs));

            K9.NotificationQuickDelete deleteOption = K9.getNotificationQuickDeleteBehaviour();
            boolean showDeleteAction = deleteOption == K9.NotificationQuickDelete.ALWAYS ||
                    (deleteOption == K9.NotificationQuickDelete.FOR_SINGLE_MSG && newMessages == 1);

            NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

            if (showDeleteAction) {
                // we need to pass the action directly to the activity, otherwise the
                // status bar won't be pulled up and we won't see the confirmation (if used)

                // Delete on phone
                builder.addAction(
                        platformSupportsLockScreenNotifications()
                                ? R.drawable.ic_action_delete_dark_vector
                                : R.drawable.ic_action_delete_dark,
                        context.getString(R.string.notification_action_delete),
                        NotificationDeleteConfirmation.getIntent(context, account, allRefs));

                // Delete on wear only if no confirmation is required
                if (!K9.confirmDeleteFromNotification()) {
                    NotificationCompat.Action wearActionDelete =
                            new NotificationCompat.Action.Builder(
                                    R.drawable.ic_action_delete_dark,
                                    context.getString(R.string.notification_action_delete),
                                    NotificationDeleteConfirmation.getIntent(context, account, allRefs))
                                    .build();
                    builder.extend(wearableExtender.addAction(wearActionDelete));
                }
            }

            if (NotificationActionService.isArchiveAllMessagesWearAvaliable(context, account, data.getMessages())) {

                // Archive on wear
                NotificationCompat.Action wearActionArchive =
                        new NotificationCompat.Action.Builder(
                                R.drawable.ic_action_delete_dark,
                                context.getString(R.string.notification_action_archive),
                                NotificationActionService.getArchiveAllMessagesIntent(context, account, allRefs))
                                .build();
                builder.extend(wearableExtender.addAction(wearActionArchive));
            }

            if (NotificationActionService.isSpamAllMessagesWearAvaliable(context, account, data.getMessages())) {

                // Archive on wear
                NotificationCompat.Action wearActionSpam =
                        new NotificationCompat.Action.Builder(
                                R.drawable.ic_action_delete_dark,
                                context.getString(R.string.notification_action_spam),
                                NotificationActionService.getSpamAllMessagesIntent(context, account, allRefs))
                                .build();
                builder.extend(wearableExtender.addAction(wearActionSpam));
            }
        } else {
            String accountNotice = context.getString(R.string.notification_new_one_account_fmt,
                    unreadCount, accountDescr);
            builder.setContentTitle(accountNotice);
            builder.setContentText(summary);
        }

        for (Message m : data.getMessages()) {
            if (m.isSet(Flag.FLAGGED)) {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                break;
            }
        }

        TaskStackBuilder stack;
        boolean treatAsSingleMessageNotification;

        if (platformSupportsExtendedNotifications()) {
            // in the new-style notifications, we focus on the new messages, not the unread ones
            treatAsSingleMessageNotification = newMessages == 1;
        } else {
            // in the old-style notifications, we focus on unread messages, as we don't have a
            // good way to express the new message count
            treatAsSingleMessageNotification = unreadCount == 1;
        }

        if (treatAsSingleMessageNotification) {
            stack = buildMessageViewBackStack(context, message.makeMessageReference());
        } else if (account.goToUnreadMessageSearch()) {
            stack = buildUnreadBackStack(context, account);
        } else {
            String initialFolder = message.getFolder().getName();
            /* only go to folder if all messages are in the same folder, else go to folder list */
            for (MessageReference ref : allRefs) {
                if (!TextUtils.equals(initialFolder, ref.getFolderName())) {
                    initialFolder = null;
                    break;
                }
            }

            stack = buildMessageListBackStack(context, account, initialFolder);
        }

        builder.setContentIntent(stack.getPendingIntent(
                account.getAccountNumber(),
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT));
        builder.setDeleteIntent(NotificationActionService.getAcknowledgeIntent(context, account));

        // Only ring or vibrate if we have not done so already on this account and fetch
        boolean ringAndVibrate = false;
        if (!updateSilently && !account.isRingNotified()) {
            account.setRingNotified(true);
            ringAndVibrate = true;
        }

        NotificationSetting n = account.getNotificationSetting();
        configureLockScreenNotification(builder, context, account, newMessages, unreadCount, accountDescr, sender, data.getMessages());
        configureNotification(
                builder,
                (n.shouldRing()) ? n.getRingtone() : null,
                (n.shouldVibrate()) ? n.getVibration() : null,
                (n.isLed()) ? Integer.valueOf(n.getLedColor()) : null,
                K9.NOTIFICATION_LED_BLINK_SLOW,
                ringAndVibrate);

        notifMgr.notify(account.getAccountNumber(), builder.build());
    }

    public void notifySendTempFailed(Account account, Exception lastFailure) {
        notifySendFailed(account, lastFailure, account.getOutboxFolderName());
    }

    public void notifySendPermFailed(Account account, Exception lastFailure) {
        notifySendFailed(account, lastFailure, account.getDraftsFolderName());
    }

    /**
     * Display a notification when sending a message has failed.
     *
     * @param account     The account that was used to sent the message.
     * @param lastFailure The {@link Exception} instance that indicated sending the message has failed.
     * @param openFolder  The name of the folder to open when the notification is clicked.
     */
    private void notifySendFailed(Account account, Exception lastFailure, String openFolder) {
        NotificationManager notifMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(platformSupportsLockScreenNotifications()
                ? R.drawable.ic_notify_new_mail_vector
                : R.drawable.ic_notify_new_mail);
        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(true);
        builder.setTicker(context.getString(R.string.send_failure_subject));
        builder.setContentTitle(context.getString(R.string.send_failure_subject));
        builder.setContentText(getRootCauseMessage(lastFailure));

        TaskStackBuilder stack = buildFolderListBackStack(context, account);
        builder.setContentIntent(stack.getPendingIntent(0, 0));
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        configureNotification(builder, null, null, K9.NOTIFICATION_LED_FAILURE_COLOR,
                K9.NOTIFICATION_LED_BLINK_FAST, true);

        notifMgr.notify(K9.SEND_FAILED_NOTIFICATION - account.getAccountNumber(),
                builder.build());
    }

    /**
     * Display an ongoing notification while checking for new messages on the server.
     *
     * @param account The account that is checked for new messages. Never {@code null}.
     * @param folder  The folder that is being checked for new messages. Never {@code null}.
     */
    public void notifyFetchingMail(final Account account, final Folder folder) {
        if (!account.isShowOngoing()) {
            return;
        }

        final NotificationManager notifMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notify_check_mail);
        builder.setWhen(System.currentTimeMillis());
        builder.setOngoing(true);
        builder.setTicker(context.getString(
                R.string.notification_bg_sync_ticker, account.getDescription(), folder.getName()));
        builder.setContentTitle(context.getString(R.string.notification_bg_sync_title));
        builder.setContentText(account.getDescription() +
                context.getString(R.string.notification_bg_title_separator) +
                folder.getName());

        TaskStackBuilder stack = buildMessageListBackStack(context, account,
                account.getInboxFolderName());
        builder.setContentIntent(stack.getPendingIntent(0, 0));
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (K9.NOTIFICATION_LED_WHILE_SYNCING) {
            configureNotification(builder, null, null,
                    account.getNotificationSetting().getLedColor(),
                    K9.NOTIFICATION_LED_BLINK_FAST, true);
        }

        notifMgr.notify(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber(),
                builder.build());
    }

    public void notifyFetchingMailCancel(final Account account) {
        if (account.isShowOngoing()) {
            cancelNotification(K9.FETCHING_EMAIL_NOTIFICATION - account.getAccountNumber());
        }
    }

    private CharSequence getMessageSubject(Context context, Message message) {
        String subject = message.getSubject();
        if (!TextUtils.isEmpty(subject)) {
            return subject;
        }

        return context.getString(R.string.general_no_subject);
    }

    private CharSequence getMessageSender(Context context, Account account, Message message) {
        try {
            boolean isSelf = false;
            final Contacts contacts = K9.showContactName() ? Contacts.getInstance(context) : null;
            final Address[] fromAddrs = message.getFrom();

            if (fromAddrs != null) {
                isSelf = account.isAnIdentity(fromAddrs);
                if (!isSelf && fromAddrs.length > 0) {
                    return MessageHelper.toFriendly(fromAddrs[0], contacts).toString();
                }
            }

            if (isSelf) {
                // show To: if the message was sent from me
                Address[] rcpts = message.getRecipients(Message.RecipientType.TO);

                if (rcpts != null && rcpts.length > 0) {
                    return context.getString(R.string.message_to_fmt,
                            MessageHelper.toFriendly(rcpts[0], contacts).toString());
                }

                return context.getString(R.string.general_no_sender);
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to get sender information for notification.", e);
        }

        return null;
    }

    /**
     * Get the pending notification data for an account.
     * See {@link NotificationData}.
     *
     * @param account                    The account to retrieve the pending data for
     * @param previousUnreadMessageCount The number of currently pending messages, which will be used
     *                                   if there's no pending data yet. If passed as null, a new instance
     *                                   won't be created if currently not existent.
     * @return A pending data instance, or null if one doesn't exist and
     * previousUnreadMessageCount was passed as null.
     */
    private NotificationData getNotificationData(Account account, Integer previousUnreadMessageCount) {
        NotificationData data;

        synchronized (notificationData) {
            data = notificationData.get(account.getAccountNumber());
            if (data == null && previousUnreadMessageCount != null) {
                data = new NotificationData(previousUnreadMessageCount);
                notificationData.put(account.getAccountNumber(), data);
            }
        }

        return data;
    }

    private TaskStackBuilder buildMessageListBackStack(Context context, Account account, String folder) {
        TaskStackBuilder stack = skipFolderListInBackStack(context, account, folder)
                ? buildAccountsBackStack(context)
                : buildFolderListBackStack(context, account);

        if (folder != null) {
            LocalSearch search = new LocalSearch(folder);
            search.addAllowedFolder(folder);
            search.addAccountUuid(account.getUuid());
            stack.addNextIntent(MessageList.intentDisplaySearch(context, search, false, true, true));
        }
        return stack;
    }

    private TaskStackBuilder buildFolderListBackStack(Context context, Account account) {
        TaskStackBuilder stack = buildAccountsBackStack(context);
        stack.addNextIntent(FolderList.actionHandleAccountIntent(context, account, false));
        return stack;
    }

    /**
     * Configure the notification sound and LED
     *
     * @param builder          {@link NotificationCompat.Builder} instance used to configure the notification.
     *                         Never {@code null}.
     * @param ringtone         String name of ringtone. {@code null}, if no ringtone should be played.
     * @param vibrationPattern {@code long[]} vibration pattern. {@code null}, if no vibration should be played.
     * @param ledColor         Color to flash LED. {@code null}, if no LED flash should happen.
     * @param ledSpeed         Either {@link K9#NOTIFICATION_LED_BLINK_SLOW} or
     *                         {@link K9#NOTIFICATION_LED_BLINK_FAST}.
     * @param ringAndVibrate   {@code true}, if ringtone/vibration are allowed. {@code false}, otherwise.
     */
    private void configureNotification(NotificationCompat.Builder builder, String ringtone,
                                       long[] vibrationPattern, Integer ledColor, int ledSpeed, boolean ringAndVibrate) {

        // if it's quiet time, then we shouldn't be ringing, buzzing or flashing
        if (K9.isQuietTime()) {
            return;
        }

        if (ringAndVibrate) {
            if (ringtone != null && !TextUtils.isEmpty(ringtone)) {
                builder.setSound(Uri.parse(ringtone));
            }

            if (vibrationPattern != null) {
                builder.setVibrate(vibrationPattern);
            }
        }

        if (ledColor != null) {
            int ledOnMS;
            int ledOffMS;
            if (ledSpeed == K9.NOTIFICATION_LED_BLINK_SLOW) {
                ledOnMS = K9.NOTIFICATION_LED_ON_TIME;
                ledOffMS = K9.NOTIFICATION_LED_OFF_TIME;
            } else {
                ledOnMS = K9.NOTIFICATION_LED_FAST_ON_TIME;
                ledOffMS = K9.NOTIFICATION_LED_FAST_OFF_TIME;
            }

            builder.setLights(ledColor, ledOnMS, ledOffMS);
        }
    }

    private TaskStackBuilder buildAccountsBackStack(Context context) {
        TaskStackBuilder stack = TaskStackBuilder.create(context);
        if (!skipAccountsInBackStack(context)) {
            stack.addNextIntent(new Intent(context, Accounts.class).putExtra(Accounts.EXTRA_STARTUP, false));
        }
        return stack;
    }

    private boolean skipAccountsInBackStack(Context context) {
        return Preferences.getPreferences(context).getAccounts().size() == 1;
    }

    private boolean skipFolderListInBackStack(Context context, Account account, String folder) {
        return folder != null && folder.equals(account.getAutoExpandFolderName());
    }

    private TaskStackBuilder buildUnreadBackStack(Context context, final Account account) {
        TaskStackBuilder stack = buildAccountsBackStack(context);
        LocalSearch search = createUnreadSearch(context, account);
        stack.addNextIntent(MessageList.intentDisplaySearch(context, search, true, false, false));
        return stack;
    }

    private TaskStackBuilder buildMessageViewBackStack(Context context, MessageReference message) {
        Account account = Preferences.getPreferences(context).getAccount(message.getAccountUuid());
        TaskStackBuilder stack = buildMessageListBackStack(context, account, message.getFolderName());
        stack.addNextIntent(MessageList.actionDisplayMessageIntent(context, message));
        return stack;
    }

    private LocalMessage findNewestMessageForNotificationLocked(Context context, NotificationData data) {
        if (!data.getMessages().isEmpty()) {
            return data.getMessages().getFirst();
        }

        if (!data.getDroppedMessages().isEmpty()) {
            return data.getDroppedMessages().getFirst().restoreToLocalMessage(context);
        }

        return null;
    }

    /**
     * Configure lock screen notifications on platforms that support it
     *
     * @param builder            Unlocked notification
     * @param context            Context
     * @param account            Account being notified
     * @param newMessages        Number of new messages being notified for
     * @param unreadCount        Total number of unread messages in this account
     * @param accountDescription Formatted account name for display
     * @param formattedSender    Formatted sender name for display
     * @param messages           List of messages if notifying for multiple messages. Null otherwise.
     */
    private void configureLockScreenNotification(NotificationCompat.Builder builder,
                                                 Context context,
                                                 Account account,
                                                 int newMessages,
                                                 int unreadCount,
                                                 CharSequence accountDescription,
                                                 CharSequence formattedSender,
                                                 List<? extends Message> messages) {
        if (!platformSupportsLockScreenNotifications()) {
            return;
        }

        builder.setSmallIcon(R.drawable.ic_notify_new_mail_vector);
        builder.setColor(account.getChipColor());

        NotificationCompat.Builder publicNotification = new NotificationCompat.Builder(context);
        publicNotification.setSmallIcon(R.drawable.ic_notify_new_mail_vector);
        publicNotification.setColor(account.getChipColor());
        publicNotification.setNumber(unreadCount);
        final String title = context.getResources().getQuantityString(
                R.plurals.notification_new_messages_title, newMessages, newMessages);
        publicNotification.setContentTitle(title);

        switch (K9.getLockScreenNotificationVisibility()) {
            case NOTHING:
                builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
                break;
            case APP_NAME:
                // This is the Android default, but we should be explicit in case that changes in the future.
                builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                break;
            case SENDERS:
                if (newMessages == 1) {
                    publicNotification.setContentText(formattedSender);
                } else {
                    // Use a LinkedHashSet so that we preserve ordering (newest to oldest), but still remove duplicates
                    Set<CharSequence> senders = new LinkedHashSet<CharSequence>(NUM_SENDERS_IN_LOCK_SCREEN_NOTIFICATION);
                    for (Message message : messages) {
                        senders.add(getMessageSender(context, account, message));
                        if (senders.size() == NUM_SENDERS_IN_LOCK_SCREEN_NOTIFICATION) {
                            break;
                        }
                    }
                    publicNotification.setContentText(TextUtils.join(", ", senders));
                }

                builder.setPublicVersion(publicNotification.build());
                break;
            case EVERYTHING:
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                break;
            case MESSAGE_COUNT:
            default:
                publicNotification.setContentText(accountDescription);

                builder.setPublicVersion(publicNotification.build());
                break;
        }
    }

    private CharSequence buildMessageSummary(Context context, CharSequence sender, CharSequence subject) {
        if (sender == null) {
            return subject;
        }

        SpannableStringBuilder summary = new SpannableStringBuilder();
        summary.append(sender);
        summary.append(" ");
        summary.append(subject);

        summary.setSpan(getEmphasizedSpan(context), 0, sender.length(), 0);

        return summary;
    }

    private TextAppearanceSpan getEmphasizedSpan(Context context) {
        if (sEmphasizedSpan == null) {
            sEmphasizedSpan = new TextAppearanceSpan(context,
                    R.style.TextAppearance_StatusBar_EventContent_Emphasized);
        }
        return sEmphasizedSpan;
    }

    private CharSequence getMessagePreview(Context context, Message message) {
        CharSequence subject = getMessageSubject(context, message);
        String snippet = message.getPreview();

        if (TextUtils.isEmpty(subject)) {
            return snippet;
        } else if (TextUtils.isEmpty(snippet)) {
            return subject;
        }

        SpannableStringBuilder preview = new SpannableStringBuilder();
        preview.append(subject);
        preview.append('\n');
        preview.append(snippet);

        preview.setSpan(getEmphasizedSpan(context), 0, subject.length(), 0);

        return preview;
    }

    private String getRootCauseMessage(Throwable t) {
        Throwable rootCause = t;
        Throwable nextCause = rootCause;

        do {
            nextCause = rootCause.getCause();
            if (nextCause != null) {
                rootCause = nextCause;
            }
        } while (nextCause != null);

        if (rootCause instanceof MessagingException) {
            return rootCause.getMessage();
        } else {
            // Remove the namespace on the exception so we have a fighting chance of seeing more of the error in the
            // notification.
            return (rootCause.getLocalizedMessage() != null)
                    ? (rootCause.getClass().getSimpleName() + ": " + rootCause.getLocalizedMessage())
                    : rootCause.getClass().getSimpleName();
        }
    }

    public static final boolean platformSupportsExtendedNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static final boolean platformSupportsLockScreenNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static LocalSearch createUnreadSearch(Context context, BaseAccount account) {
        String searchTitle = context.getString(R.string.search_title, account.getDescription(),
                context.getString(R.string.unread_modifier));

        LocalSearch search;
        if (account instanceof SearchAccount) {
            search = ((SearchAccount) account).getRelatedSearch().clone();
            search.setName(searchTitle);
        } else {
            search = new LocalSearch(searchTitle);
            search.addAccountUuid(account.getUuid());

            Account realAccount = (Account) account;
            realAccount.excludeSpecialFolders(search);
            realAccount.limitToDisplayableFolders(search);
        }

        search.and(SearchSpecification.SearchField.READ, "1", SearchSpecification.Attribute.NOT_EQUALS);

        return search;
    }
}
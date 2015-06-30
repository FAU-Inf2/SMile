package com.fsck.k9.service;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.Account;
import com.fsck.k9.activity.ActivityListener;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalRemindMe;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RemindMeService extends CoreService {
    private static final String START_SERVICE = "com.fsck.k9.intent.action.startService";
    private static final String ACTION_CHECK_REMINDME = "com.fsck.k9.intent.action.REMINDME_SERVICE_CHECK";

    public static void startService(Context context) {
        Intent i = new Intent();
        i.setClass(context, RemindMeService.class);
        i.setAction(RemindMeService.START_SERVICE);
        addWakeLock(context, i);
        context.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(K9.LOG_TAG, "***** RemindMeService *****: onCreate");
    }

    @Override
    public int startService(Intent intent, int startId) {
        Log.i(K9.LOG_TAG, "RemindMeService.startService(" + intent + ", " + startId + ") alive and kicking");

        Preferences prefs = Preferences.getPreferences(RemindMeService.this);

        // TODO: get interval from preferences
        long delay = (10 * (60 * 1000)); // wait 10 min
        long minNextTime = System.currentTimeMillis() + delay;

        for(Account acc : prefs.getAccounts()) {
            Date ret = handleAccount(acc);
            if(ret != null && ret.getTime() < minNextTime) {
                minNextTime = ret.getTime();
            }
        }

        minNextTime += 60 * 1000;
        Intent i = new Intent(this, RemindMeService.class);
        i.setAction(ACTION_CHECK_REMINDME);
        BootReceiver.scheduleIntent(RemindMeService.this, minNextTime, i);
        return 0;
    }

    private Date handleAccount(final Account acc) {
        Context context = getApplication();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        NotificationManager notifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        List<RemindMe> remindMes = getRemindMeItems(acc);
        Date now = new Date();
        Date minDate = null;

        final MessagingController messagingController = MessagingController.getInstance(getApplication());
        final List<LocalMessage> messages = new ArrayList<LocalMessage>();
        final List<Long> messageIds = new ArrayList<Long>();
        LocalRemindMe localRemindMe = null;

        try {
            localRemindMe = new LocalRemindMe(acc.getLocalStore());
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Exception thrown while calling acquiring LocalRemindMe", e);
        }

        for(RemindMe item : remindMes) {
            if(item.getRemindTime().after(now) && item.getSeen() == null) {
                if(minDate == null || item.getRemindTime().compareTo(minDate) == -1) {
                    minDate = item.getRemindTime();
                }

                continue;
            }

            LocalMessage message = (LocalMessage) item.getReference();
            messages.add(message);
            messageIds.add(message.getId());
            item.setSeen(new Date(System.currentTimeMillis()));

            if(localRemindMe != null) {
                try {
                    localRemindMe.update(item);
                    localRemindMe.delete(item);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }

/*
            builder.setSmallIcon(R.drawable.ic_notify_new_mail);
            builder.setContentTitle(item.getTitle());
            builder.setContentText(item.getTitle());
            builder.setWhen(System.currentTimeMillis());
            builder.setAutoCancel(true);

            builder.addAction(
                    R.drawable.ic_action_mark_as_read_dark,
                    context.getString(R.string.notification_action_mark_as_read),
                    NotificationActionService.getReadAllMessagesIntent(context, acc, allRefs));

            Intent resultIntent = new Intent(context, RemindMeList.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(RemindMeList.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultPendingIntent);
            notifyMgr.notify(item.getId(), builder.build());
            */
        }

        if(messages.size() == 0) {
            Log.e(K9.LOG_TAG, "No messages to handle until " + minDate);
            return minDate;
        }

        // move mail back to inbox
        Log.d(K9.LOG_TAG, "Will move mails from RemindMe back to inbox.");
        messagingController.moveMessages(acc,
                acc.getRemindMeFolderName(),
                messages,
                acc.getInboxFolderName(),
                new ActivityListener() {
                    @Override
                    public void folderStatusChanged(Account account, String folder, int unreadMessageCount) {
                        Log.d(K9.LOG_TAG, "folderStatusChanged");
                        super.folderStatusChanged(account, folder, unreadMessageCount);
                    }
                });

        //TODO: check whether move-to-inbox has finished!

        Log.d(K9.LOG_TAG, "Will synchronize inbox.");
        messagingController.synchronizeMailbox(acc, acc.getInboxFolderName(),
                new ActivityListener() {
                    @Override
                    public void folderStatusChanged(Account account, String folder, int unreadMessageCount) {
                        Log.d(K9.LOG_TAG, "folderStatusChanged in synchronize Mailbox");
                        super.folderStatusChanged(account, folder, unreadMessageCount);
                    }

                    @Override
                    public void synchronizeMailboxFinished(Account account, String folder,
                                                           int totalMessagesInMailbox, int numNewMessages) {
                        Log.e(K9.LOG_TAG, "synchronization finished successfully.");
                        Log.d(K9.LOG_TAG, "Mark mails from RemindMe as unread.");
                        messagingController.setFlag(acc, messageIds, Flag.SEEN, false);

                        FetchProfile fp = new FetchProfile();
                        fp.add(FetchProfile.Item.ENVELOPE);
                        try {
                            Log.d(K9.LOG_TAG, "Fetch flags.");
                            LocalFolder localFolder = acc.getLocalStore().getFolder(acc.getInboxFolderName());
                            localFolder.fetch(messages, fp, null);
                            localFolder.close();
                        } catch (Exception e) {
                            Log.e(K9.LOG_TAG, "Error while fetching flags for mail (remindMe): " +
                                    e.getMessage());
                        }
                    }
                }, null);

        return minDate;
    }

    private List<RemindMe> getRemindMeItems(Account acc) {
        ArrayList<RemindMe> remindMes = new ArrayList<RemindMe>();
        try {
            LocalRemindMe localRemindMe = new LocalRemindMe(acc.getLocalStore());
            remindMes.addAll(localRemindMe.getAllRemindMes());
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Exception thrown while calling getAllRemindMes()", e);
        }

        return remindMes;
    }
}

package com.fsck.k9.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.Account;
import com.fsck.k9.activity.RemindMeList;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalRemindMe;
import com.fsck.k9.mailstore.LocalMessage;

import de.fau.cs.mad.smile.android.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class RemindMeService extends CoreService {
    private static final String START_SERVICE = "com.fsck.k9.intent.action.startService";
    private static final String ACTION_CHECK_FOLLOWUP = "com.fsck.k9.intent.action.REMINDME_SERVICE_CHECK";

    private Account mAccount;

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

        for(Account acc : prefs.getAccounts()) {
            handleAccount(acc);
        }

        Intent i = new Intent(this, RemindMeService.class);
        i.setAction(ACTION_CHECK_FOLLOWUP);
        // TODO: get interval from preferences
        long delay = (10 * (60 * 1000)); // wait 10 min
        long nextTime  = System.currentTimeMillis() + delay;
        BootReceiver.scheduleIntent(RemindMeService.this, nextTime, i);
        return 0;
    }

    private void handleAccount(Account acc) {
        Context context = getApplication();
        mAccount = acc;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        NotificationManager notifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        List<RemindMe> remindMes = getFollowUpItems(acc);
        Date now = new Date();

        for(RemindMe item : remindMes) {
            if(item.getRemindTime().after(now)) {
                continue;
            }

            builder.setSmallIcon(R.drawable.ic_notify_new_mail);
            builder.setContentTitle(item.getTitle());
            builder.setContentText(item.getTitle());
            builder.setWhen(System.currentTimeMillis());

            // move mail back to inbox
            new MoveFollowUpBack().execute(item);

            builder.addAction(
                    R.drawable.ic_action_mark_as_read_dark,
                    context.getString(R.string.notification_action_mark_as_read),
                    NotificationActionService.getFollowUpIntent(context, acc));

            Intent resultIntent = new Intent(context, RemindMeList.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(RemindMeList.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultPendingIntent);
            notifyMgr.notify(item.getId(), builder.build());
        }
    }

    private List<RemindMe> getFollowUpItems(Account acc) {
        ArrayList<RemindMe> remindMes = new ArrayList<RemindMe>();
        try {
            LocalRemindMe localRemindMe = new LocalRemindMe(acc.getLocalStore());
            remindMes.addAll(localRemindMe.getAllFollowUps());
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Exception thrown while calling getAllRemindMes()", e);
        }

        return remindMes;
    }

    class MoveFollowUpBack extends AsyncTask<RemindMe, Void, Void> {

        @Override
        protected Void doInBackground(RemindMe... params) {
            for(RemindMe remindMe : params) {
                MessagingController messagingController = MessagingController.getInstance(getApplication());
                try {
                    messagingController.moveMessages(mAccount,
                            mAccount.getFollowUpFolderName(),
                            new ArrayList<LocalMessage>(Arrays.asList((LocalMessage) remindMe.getReference())),
                            //mAccount.getLocalStore().getFolderById(remindMe.getFolderId()).getName(), null);
                            mAccount.getInboxFolderName(), null);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Error while moving message back to inbox: " + e.getMessage());
                }
                //TODO: Mark as unread?
            }
            return null;
        }
    }
}

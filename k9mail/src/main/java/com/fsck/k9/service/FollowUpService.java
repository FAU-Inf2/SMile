package com.fsck.k9.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.Account;
import com.fsck.k9.activity.FollowUpList;
import com.fsck.k9.mail.FollowUp;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFollowUp;
import com.fsck.k9.mailstore.LocalStore;

import de.fau.cs.mad.smile.android.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FollowUpService extends CoreService {
    private static final String ACTION_RESET = "com.fsck.k9.intent.action.FOLLOWUP_SERVICE_RESET";
    private static final String ACTION_CHECK_FOLLOWUP = "com.fsck.k9.intent.action.FOLLOWUP_SERVICE_CHECK";

    public static void actionReset(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, FollowUpService.class);
        i.setAction(FollowUpService.ACTION_RESET);
        addWakeLockId(context, i, wakeLockId, true);
        context.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (K9.DEBUG) {
            Log.v(K9.LOG_TAG, "***** WiedervorlageService *****: onCreate");
        }
    }

    @Override
    public int startService(Intent intent, int startId) {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "WiedervorlageService.startService(" + intent + ", " + startId + ") alive and kicking");
        }

        Preferences prefs = Preferences.getPreferences(FollowUpService.this);

        for(Account acc : prefs.getAccounts()) {
            handleAccount(acc);
        }
        Intent i = new Intent(this, FollowUpService.class);
        i.setAction(ACTION_CHECK_FOLLOWUP);
        long delay = (10 * (60 * 1000)); // wait 10 min
        long nextTime  = System.currentTimeMillis() + delay;
        BootReceiver.scheduleIntent(FollowUpService.this, nextTime, i);
        return 0;
    }

    private void handleAccount(Account acc) {
        Log.i(K9.LOG_TAG, "Working account " + acc);
        Context context = getApplication();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        NotificationManager notifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        List<FollowUp> followUps = getFollowUpItems(acc);
        Date now = new Date();

        for(FollowUp item : followUps) {
            if(item.getRemindTime().after(now)) {
                continue;
            }

            builder.setSmallIcon(R.drawable.ic_notify_new_mail);
            builder.setContentTitle(item.getTitle());
            builder.setContentText(item.getTitle());
            builder.setWhen(item.getRemindTime().getTime());

            builder.addAction(
                    R.drawable.ic_action_mark_as_read_dark,
                    context.getString(R.string.notification_action_mark_as_read),
                    NotificationActionService.getFollowUpIntent(context, acc));

            Intent resultIntent = new Intent(context, FollowUpList.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(FollowUpList.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultPendingIntent);
            notifyMgr.notify(item.getId(), builder.build());
        }
    }

    private List<FollowUp> getFollowUpItems(Account acc) {
        ArrayList<FollowUp> followUps = new ArrayList<FollowUp>();
        try {
            LocalFollowUp localFollowUp = new LocalFollowUp(acc.getLocalStore());
            followUps.addAll(localFollowUp.getAllFollowUps());
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return followUps;
    }
}

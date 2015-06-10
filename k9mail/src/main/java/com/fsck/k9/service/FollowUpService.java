package com.fsck.k9.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.Account;
import com.fsck.k9.mail.FollowUp;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFollowUp;

import de.fau.cs.mad.smile.android.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FollowUpService extends CoreService {

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

        return 0;
    }

    private void handleAccount(Account acc) {
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

            Notification notification = builder.build();
            notifyMgr.notify(item.getId(), notification);
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

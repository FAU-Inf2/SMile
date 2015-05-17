package com.fsck.k9.service;

import android.content.Intent;
import android.util.Log;
import com.fsck.k9.K9;

public class WiedervorlageService extends CoreService {

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

        return 0;
    }
}

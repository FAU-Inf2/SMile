package com.fsck.k9.ui.messageview;

import android.content.Intent;

class IntentAndResolvedActivitiesCount {
    private final Intent intent;
    private final int activitiesCount;

    IntentAndResolvedActivitiesCount(final Intent intent, final int activitiesCount) {
        this.intent = intent;
        this.activitiesCount = activitiesCount;
    }

    public Intent getIntent() {
        return intent;
    }

    public boolean hasResolvedActivities() {
        return activitiesCount > 0;
    }

    public String getMimeType() {
        return intent.getType();
    }

    public boolean containsFileUri() {
        return "file".equals(intent.getData().getScheme());
    }
}

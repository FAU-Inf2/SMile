package com.fsck.k9.activity.listener;

import android.content.Context;
import android.view.View;

import com.fsck.k9.activity.MessageList;
import com.fsck.k9.search.LocalSearch;

public class FolderClickListener implements View.OnClickListener {
    final LocalSearch search;
    final Context context;

    public FolderClickListener(final Context context, final LocalSearch search) {
        this.search = search;
        this.context = context;
    }

    @Override
    public void onClick(final View v) {
        MessageList.actionDisplaySearch(context, search, true, false);
    }
}

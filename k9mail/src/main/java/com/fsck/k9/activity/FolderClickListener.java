package com.fsck.k9.activity;

import android.content.Context;
import android.view.View;

import com.fsck.k9.search.LocalSearch;

class FolderClickListener implements View.OnClickListener {
    final LocalSearch search;
    final Context context;

    FolderClickListener(Context context, LocalSearch search) {
        this.search = search;
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        MessageList.actionDisplaySearch(context, search, true, false);
    }
}

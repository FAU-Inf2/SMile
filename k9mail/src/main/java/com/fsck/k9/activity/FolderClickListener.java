package com.fsck.k9.activity;

import android.content.Context;
import android.view.View;

import com.fsck.k9.search.LocalSearch;

public class FolderClickListener implements View.OnClickListener {
    final LocalSearch search;
    final Context context;

    public FolderClickListener(Context context, LocalSearch search) {
        this.search = search;
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        Messages.actionDisplaySearch(context, search);
    }
}

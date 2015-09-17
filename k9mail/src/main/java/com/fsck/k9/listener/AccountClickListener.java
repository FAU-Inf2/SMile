package com.fsck.k9.listener;

import android.content.Context;
import android.view.View;

import com.fsck.k9.activity.MessageList;
import com.fsck.k9.search.LocalSearch;

public class AccountClickListener implements View.OnClickListener {

    private Context context;
    final LocalSearch search;

    public AccountClickListener(Context context, LocalSearch search) {
        this.context = context;
        this.search = search;
    }

    @Override
    public void onClick(View v) {
        MessageList.actionDisplaySearch(context, search, true, false);
    }

}

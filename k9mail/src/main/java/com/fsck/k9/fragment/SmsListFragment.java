package com.fsck.k9.fragment;

import android.os.Bundle;

import com.fsck.k9.search.LocalSearch;

public class SmsListFragment extends MessageListFragment {

    public static SmsListFragment newInstance(LocalSearch search, boolean isThreadDisplay, boolean threadedList) {
        SmsListFragment fragment = new SmsListFragment();
        Bundle args = new Bundle();
        args.putParcelable(getArgSearch(), search);
        args.putBoolean(getArgIsThreadDisplay(), isThreadDisplay);
        args.putBoolean(getArgThreadedList(), threadedList);
        fragment.setArguments(args);
        return fragment;
    }

}

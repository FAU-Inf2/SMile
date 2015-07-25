package com.fsck.k9.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.misc.ContactPictureLoader;
import com.fsck.k9.adapter.MessageAdapter;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.search.LocalSearch;

import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class MessageFragment extends Fragment {

    private static final String ARG_SEARCH = "searchObject";

    private List<LocalMessage> messages;
    private MessageAdapter adapter;
    private SwipeRefreshLayout mPullToRefreshView;
    private RecyclerView mRecyclerView;
    private MessageListHandler mHandler;
    private LocalSearch search;
    private String mFolderName;
    private Account mAccount;

    public static final MessageFragment newInstance(final LocalSearch search) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SEARCH, search);
        fragment.setArguments(args);
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleArguments(getArguments());

        //mHandler = new MessageListHandler(this);
        messages = new ArrayList<LocalMessage>();
        adapter = new MessageAdapter(getActivity(), messages);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.message_list_fragment, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.message_list);
        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(adapter);
        mPullToRefreshView = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);

        mPullToRefreshView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                messages.clear();
                messages.addAll(loadMessages(mAccount, mAccount.getInboxFolderName()));
                mPullToRefreshView.setRefreshing(false);
            }
        });

        messages.addAll(loadMessages(mAccount, mFolderName));
        return rootView;
    }

    private final void handleArguments(final Bundle arguments) {
        search = arguments.getParcelable(ARG_SEARCH);
        mFolderName = search.getFolderNames().get(0);
        String title = search.getName();
        String[] accountUuids = search.getAccountUuids();
        Preferences preferences = Preferences.getPreferences(getActivity().getApplication());

        if(accountUuids.length > 0) {
            mAccount = preferences.getAccount(accountUuids[0]);
        }
    }

    private final List<LocalMessage> loadMessages(final Account account, final String folderName) {
        final List<LocalMessage> localMessages = new ArrayList<LocalMessage>();
        LocalFolder folder = null;

        try {
            folder = account.getLocalStore().getFolder(folderName);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        try {
            folder.getMessages(new MessageRetrievalListener() {
                @Override
                public void messageStarted(String uid, int number, int ofTotal) {

                }

                @Override
                public void messageFinished(Message message, int number, int ofTotal) {
                    localMessages.add((LocalMessage) message);
                }

                @Override
                public void messagesFinished(int total) {

                }
            });
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return localMessages;
    }
}

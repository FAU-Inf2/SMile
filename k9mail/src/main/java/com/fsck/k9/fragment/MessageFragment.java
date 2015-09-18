package com.fsck.k9.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.adapter.MessageAdapter;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.search.LocalSearch;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class MessageFragment extends Fragment {

    private static final String ARG_SEARCH = "searchObject";

    private Context context;
    private List<LocalMessage> messages;
    private List<RemindMe> remindMeList;
    private MessageAdapter adapter;
    private SwipeRefreshLayout mPullToRefreshView;
    private RecyclerView mRecyclerView;
    private MessageFragmentHandler handler;
    private LocalSearch search;
    private String mFolderName;
    private Account mAccount;
    private IMessageListPresenter mCallback;

    public static MessageFragment newInstance(final LocalSearch search) {
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

        handler = new MessageFragmentHandler(this);
        context = getActivity();
        messages = new ArrayList<>();
        remindMeList = new ArrayList<>();
        adapter = new MessageAdapter(messages, mCallback);

        new LoadFollowUp(context, mAccount, handler).execute();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (IMessageListPresenter) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.messages_fragment, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.message_list);
        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        LocalMessage message = messages.get(position);
                        Log.d(K9.LOG_TAG, message.toString());
                        mCallback.openMessage(message.makeMessageReference());
                    }
                })
        );

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

        if (accountUuids.length > 0) {
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

        if (folder == null) {
            return null;
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
            }, false);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return localMessages;
    }

    public final void add(final RemindMe remindMe) {
        remindMeList.add(remindMe);
        new InsertFollowUp(context, mAccount).execute(remindMe);
    }

    public final RemindMe isRemindMe(LocalMessage localMessage) {
        for (RemindMe remindMe : remindMeList) {
            if (remindMe.getUid().equals(localMessage.getUid())) {
                return remindMe;
            }
        }

        return null;
    }

    public final void delete(RemindMe remindMe) {
        new DeleteFollowUp(context, mAccount).execute(remindMe);
    }

    static class MessageFragmentHandler extends Handler {
        WeakReference<MessageFragment> messageFragment;

        public MessageFragmentHandler(MessageFragment fragment) {
            this.messageFragment = new WeakReference<>(fragment);
        }

        public void addMessage(List<LocalMessage> message) {

        }

        public void addRemindMe(List<RemindMe> remindMe) {
            MessageFragment frag = messageFragment.get();
            if(frag != null) {
                for(RemindMe item : remindMe) {
                    frag.add(item);
                }
            }
        }
    }

}

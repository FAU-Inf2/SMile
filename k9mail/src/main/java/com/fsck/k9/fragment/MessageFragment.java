package com.fsck.k9.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.adapter.MessageAdapter;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalRemindMe;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.search.LocalSearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class MessageFragment extends Fragment {

    private static final String ARG_SEARCH = "searchObject";

    private LocalRemindMe mLocalRemindMe;
    private List<LocalMessage> messages;
    private List<RemindMe> remindMeList;
    private MessageAdapter adapter;
    private SwipeRefreshLayout mPullToRefreshView;
    private RecyclerView mRecyclerView;
    private MessageListHandler mHandler;
    private LocalSearch search;
    private String mFolderName;
    private Account mAccount;
    private MessageActions mCallback;

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
        remindMeList = new ArrayList<RemindMe>();
        adapter = new MessageAdapter(getActivity(), messages, mCallback);

        try {
            LocalStore localStore = LocalStore.getInstance(mAccount, getActivity());
            mLocalRemindMe = new LocalRemindMe(localStore);
        } catch (MessagingException e) {
            // TODO: handle exception
        }

        new LoadFollowUp().execute();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (MessageActions) activity;
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

        if(folder == null) {
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
        new InsertFollowUp().execute(remindMe);
    }

    public final RemindMe isRemindMe(LocalMessage localMessage) {
        for (RemindMe remindMe : remindMeList) {
            if(remindMe.getUid().equals(localMessage.getUid())) {
                return remindMe;
            }
        }

        return null;
    }

    public final void delete(RemindMe remindMe) {
        new DeleteFollowUp().execute(remindMe);
    }

    public interface MessageActions {
        void move(LocalMessage message, String destFolder);
        void delete(LocalMessage message);
        void archive(LocalMessage message);
        void remindMe(LocalMessage message);
        void reply(LocalMessage message);
        void replyAll(LocalMessage message);
        void openMessage(MessageReference messageReference);
    }

    public static class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
        private final OnItemClickListener mListener;
        private final GestureDetector mGestureDetector;

        public interface OnItemClickListener {
            void onItemClick(View view, int position);
        }

        public RecyclerItemClickListener(Context context, OnItemClickListener listener) {
            mListener = listener;
            mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }
            });
        }

        @Override public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
            View childView = view.findChildViewUnder(e.getX(), e.getY());

            if (childView != null && mListener != null && mGestureDetector.onTouchEvent(e)) {
                mListener.onItemClick(childView, view.getChildPosition(childView));
                return true;
            }

            return false;
        }

        @Override public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) { }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    class LoadFollowUp extends AsyncTask<Void, Void, List<RemindMe>> {

        @Override
        protected List<RemindMe> doInBackground(Void... params) {
            try {
                return mLocalRemindMe.getAllRemindMes();
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Unable to retrieve FollowUps", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RemindMe> remindMes) {
            super.onPostExecute(remindMes);
            remindMeList.clear();
            remindMeList.addAll(remindMes);
        }
    }

    class InsertFollowUp extends AsyncTask<RemindMe, Void, Void> {

        @Override
        protected Void doInBackground(RemindMe... params) {
            for(RemindMe remindMe : params) {
                try {
                    LocalStore store = LocalStore.getInstance(mAccount, getActivity());
                    LocalFolder folder = new LocalFolder(store, mAccount.getRemindMeFolderName());
                    folder.open(Folder.OPEN_MODE_RW);

                    remindMe.setFolderId(folder.getId());

                    Log.d(K9.LOG_TAG, "Inserting remindMe: " + remindMe);
                    // TODO: remove messagingController
                    MessagingController messagingController = MessagingController.getInstance(getActivity());
                    messagingController.moveMessages(mAccount,
                            remindMe.getReference().getFolder().getName(),
                            new ArrayList<LocalMessage>(Arrays.asList((LocalMessage) remindMe.getReference())),
                            mAccount.getRemindMeFolderName(), null);

                    if(remindMe.getId() > 0) {
                        mLocalRemindMe.update(remindMe);
                    } else {
                        mLocalRemindMe.add(remindMe);
                    }
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Unable to insert followup", e);
                }
            }
            return null;
        }
    }

    class DeleteFollowUp extends AsyncTask<RemindMe, Void, Void> {

        @Override
        protected Void doInBackground(RemindMe... params) {
            for(RemindMe remindMe : params) {
                try {
                    mLocalRemindMe.delete(remindMe);
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "Unable to delete RemindMe", e);
                }
                try {
                    //move back to inbox
                    MessagingController messagingController = MessagingController.getInstance(getActivity());
                    messagingController.moveMessages(mAccount,
                            remindMe.getReference().getFolder().getName(),
                            new ArrayList<LocalMessage>(Arrays.asList((LocalMessage) remindMe.getReference())),
                            mAccount.getInboxFolderName(), null);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Moving back deleted RemindMe failed: " + e.getMessage());
                }
            }
            return null;
        }
    }
}

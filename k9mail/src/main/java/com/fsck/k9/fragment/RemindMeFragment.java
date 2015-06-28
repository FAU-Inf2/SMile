package com.fsck.k9.fragment;

import android.app.ListFragment;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.RemindMeAdapter;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalRemindMe;
import com.fsck.k9.mailstore.LocalStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemindMeFragment extends ListFragment {
    private static final String ARG_ACCOUNT = "accountUuid";

    private LocalRemindMe mLocalRemindMe;
    private RemindMeAdapter mAdapter;
    private List<RemindMe> mRemindMeList;
    private Account mAccount;


    public static RemindMeFragment newInstance(String accountUuid) {
        RemindMeFragment remindMeFragment = new RemindMeFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_ACCOUNT, accountUuid);
        remindMeFragment.setArguments(arguments);
        return remindMeFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRemindMeList = new ArrayList<RemindMe>();
        mAdapter = new RemindMeAdapter(getActivity(), mRemindMeList);
        mAdapter.setNotifyOnChange(true);
        String accountUuid = getArguments().getString(ARG_ACCOUNT);
        mAccount = Preferences.getPreferences(getActivity()).getAccount(accountUuid);

        try {
            LocalStore localStore = LocalStore.getInstance(mAccount, getActivity());
            mLocalRemindMe = new LocalRemindMe(localStore);
        } catch (MessagingException e) {
            // TODO: handle exception
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setListAdapter(mAdapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        new LoadFollowUp().execute();
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        Object obj = listView.getItemAtPosition(position);

        if(obj instanceof RemindMe) {
            RemindMe remindMe = (RemindMe)obj;
            // TODO: build intent to view Message
            Intent intent = new Intent(getActivity(), MessageList.class);
        }

        super.onListItemClick(listView, view, position, id);
    }

    public void add(RemindMe remindMe) {
        mRemindMeList.add(remindMe);
        mAdapter.notifyDataSetChanged();
        new InsertFollowUp().execute(remindMe);
    }

    private void populateListView(List<RemindMe> items) {
        mRemindMeList.clear();
        mRemindMeList.addAll(items);
        mAdapter.notifyDataSetChanged();
        ListView listView = getListView();
        listView.invalidate();
    }

    public void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        RemindMe remindMe = getFollowUpFromListSwipe(e1, e2);

        if(remindMe != null) {
            Log.d(K9.LOG_TAG, "LeftToRightSwipe, Object: " + remindMe);
            new DeleteFollowUp().execute(remindMe);
            mAdapter.remove(remindMe);
        }
    }

    public void onSwipeRightToLeft(MotionEvent e1, MotionEvent e2) {
        RemindMe remindMe = getFollowUpFromListSwipe(e1, e2);

        if(remindMe != null) {
            Log.d(K9.LOG_TAG, "RightToLeftSwipe, Object: " + remindMe);
            RemindMeDialog dialog = RemindMeDialog.newInstance(remindMe);
            dialog.show(getFragmentManager(), "mTimeValue");
        }
    }

    private RemindMe getFollowUpFromListSwipe(MotionEvent e1, MotionEvent e2) {
        int x = (int) e1.getRawX();
        int y = (int) e1.getRawY();

        ListView listView = getListView();
        Rect rect = new Rect();
        listView.getGlobalVisibleRect(rect);

        if (rect.contains(x, y)) {
            int[] listPosition = new int[2];
            listView.getLocationOnScreen(listPosition);

            int listX = x - listPosition[0];
            int listY = y - listPosition[1];

            int listViewPosition = listView.pointToPosition(listX, listY);
            return mAdapter.getItem(listViewPosition);
        }

        return  null;
    }

    class LoadFollowUp extends AsyncTask<Void, Void, List<RemindMe>> {

        @Override
        protected List<RemindMe> doInBackground(Void... params) {
            try {
                return mLocalRemindMe.getAllFollowUps();
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Unable to retrieve FollowUps", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RemindMe> remindMes) {
            super.onPostExecute(remindMes);
            populateListView(remindMes);
        }
    }

    class InsertFollowUp extends AsyncTask<RemindMe, Void, Void> {

        @Override
        protected Void doInBackground(RemindMe... params) {
            for(RemindMe remindMe : params) {
                try {
                    LocalStore store = LocalStore.getInstance(mAccount, getActivity());
                    LocalFolder folder = new LocalFolder(store, mAccount.getFollowUpFolderName());
                    folder.open(Folder.OPEN_MODE_RW);

                    remindMe.setFolderId(folder.getId());

                    Log.d(K9.LOG_TAG, "Inserting remindMe: " + remindMe);
                    MessagingController messagingController = MessagingController.getInstance(getActivity());
                    messagingController.moveMessages(mAccount,
                            remindMe.getReference().getFolder().getName(),
                            new ArrayList<LocalMessage>(Arrays.asList((LocalMessage) remindMe.getReference())),
                            mAccount.getFollowUpFolderName(), null);

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

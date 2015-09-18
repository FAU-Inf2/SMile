package com.fsck.k9.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.fsck.k9.Account;
import com.fsck.k9.helper.FolderHelper;
import com.fsck.k9.mail.Message;
import com.fsck.k9.search.LocalSearch;

import java.util.List;

public class SmsListFragment extends MessageListFragment {

    public static SmsListFragment newInstance(LocalSearch search, boolean isThreadDisplay, boolean threadedList) {
        SmsListFragment fragment = new SmsListFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SEARCH, search);
        args.putBoolean(ARG_IS_THREAD_DISPLAY, isThreadDisplay);
        args.putBoolean(ARG_THREADED_LIST, threadedList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view == mFooterView) {
            if (mCurrentFolder != null && !mSearch.isManualSearch()) {

                mController.loadMoreMessages(mAccount, mFolderName, null);

            } else if (mCurrentFolder != null && isRemoteSearch() &&
                    mExtraSearchResults != null && mExtraSearchResults.size() > 0) {

                int numResults = mExtraSearchResults.size();
                int limit = mAccount.getRemoteSearchNumResults();

                List<Message> toProcess = mExtraSearchResults;

                if (limit > 0 && numResults > limit) {
                    toProcess = toProcess.subList(0, limit);
                    mExtraSearchResults = mExtraSearchResults.subList(limit,
                            mExtraSearchResults.size());
                } else {
                    mExtraSearchResults = null;
                    updateFooter("");
                }

                mController.loadSearchResults(mAccount, mCurrentFolder.name, toProcess, mListener);
            }

            return;
        }

        Cursor cursor = (Cursor) parent.getItemAtPosition(position);
        if (cursor == null) {
            return;
        }
/*
        int x = cursor.getCount();

        do{
            String accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
            String folderName = cursor.getString(FOLDER_NAME_COLUMN);
            String messageUid = cursor.getString(UID_COLUMN);
            MessageReference ref = new MessageReference(accountUuid, folderName, messageUid, null);
            LocalMessage msg = ref.restoreToLocalMessage(getActivity());

            String subject = msg.getSubject();
            Address[] addresses = msg.getFrom();
            Date date = msg.getSentDate();
            Body body = msg.getBody();
            InputStream stream = null;

            try {
                if (body != null)
                    stream = body.getInputStream();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            if (stream != null){
                StringWriter writer = new StringWriter();
                try {
                    IOUtils.copy(stream, writer, "UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String theString = writer.toString();
            }


        }while(cursor.moveToNext());

*/

        if (mSelectedCount > 0) {
            toggleMessageSelect(position);
        } else {
            if (mThreadedList && cursor.getInt(THREAD_COUNT_COLUMN) > 1) {
                Account account = getAccountFromCursor(cursor);
                long folderId = cursor.getLong(FOLDER_ID_COLUMN);
                String folderName = FolderHelper.getFolderNameById(account, folderId);

                // If threading is enabled and this item represents a thread, display the thread contents.
                long rootId = cursor.getLong(THREAD_ROOT_COLUMN);
                mFragmentListener.showThread(account, folderName, rootId);
            } else {
                // This item represents a message; just display the message.
                //openMessageAtPosition(listViewToAdapterPosition(position));
            }
        }
    }
}

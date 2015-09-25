package com.fsck.k9.presenter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.fragment.IMessageListPresenter;
import com.fsck.k9.fragment.MessageListHandler;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.view.IMessageListView;
import com.fsck.k9.view.MessageListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MessageListPresenter implements IMessageListPresenter {
    final Map<SortType, Comparator<LocalMessage>> sortMap;
    private IMessageListView messageListView;
    private List<LocalMessage> messages;
    private boolean mThreadedList;
    private final Account account;
    private final LocalFolder folder;
    private final MessageListHandler handler;
    private final Context context;
    private final MessagingController mController;

    public MessageListPresenter(final Context context, final Account account, final LocalFolder folder, final MessageListHandler handler) {
        this.context = context;
        this.messages = new ArrayList<>();
        EnumMap<SortType, Comparator<LocalMessage>> sortMap = new EnumMap<>(SortType.class);
        sortMap.put(SortType.SORT_ATTACHMENT, new AttachmentComparator());
        sortMap.put(SortType.SORT_DATE, new DateComparator());
        sortMap.put(SortType.SORT_ARRIVAL, new ArrivalComparator());
        sortMap.put(SortType.SORT_FLAGGED, new FlaggedComparator());
        sortMap.put(SortType.SORT_SUBJECT, new SubjectComparator());
        sortMap.put(SortType.SORT_SENDER, new SenderComparator());
        sortMap.put(SortType.SORT_UNREAD, new UnreadComparator());

        // make it immutable to prevent accidental alteration (content is immutable already)
        this.sortMap = Collections.unmodifiableMap(sortMap);
        this.account = account;
        this.folder = folder;
        this.handler = handler;
        this.mController = MessagingController.getInstance(context);
        this.refreshList();
    }

    @Override
    public void setView(IMessageListView messageListView) {
        this.messageListView = messageListView;
        this.messageListView.showMessageList(messages);
    }

    public void enableThreadedList(boolean enable) {
        mThreadedList = enable;
    }

    @Override
    public void move(LocalMessage message, String destFolder) {

    }

    @Override
    public void delete(LocalMessage message) {

    }

    @Override
    public void archive(LocalMessage message) {

    }

    @Override
    public void remindMe(LocalMessage message) {

    }

    @Override
    public void reply(LocalMessage message) {

    }

    @Override
    public void replyAll(LocalMessage message) {

    }

    @Override
    public void refreshList() {
        loadMessages(folder);
    }

    @Override
    public void setFlag(LocalMessage message, Flag flag) {
        boolean flagState = message.isSet(flag);
        setFlag(message, flag, !flagState);
    }

    private void setFlag(LocalMessage message, final Flag flag, final boolean newState) {
        if(message == null) {
            return;
        }

        Account account = message.getAccount();
        LocalFolder folder = message.getFolder();
        int threadCount = 0;

        try {
            threadCount = folder.getThreadCount(message.getRootId());
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "error in setFlag ", e);
        }

        if (mThreadedList && threadCount > 1) {
            long threadRootId = message.getRootId();
            mController.setFlagForThreads(account,
                    Collections.singletonList(threadRootId), flag, newState);
        } else {
            long id = message.getId();
            mController.setFlag(account, Collections.singletonList(id), flag,
                    newState);
        }

        // TODO: selected computeBatchDirection();
    }

    @Override
    public void openMessage(MessageReference messageReference) {
        handler.openMessage(messageReference);
    }

    @Override
    public boolean openPreviousMessage(MessageReference messageReference) {
        int position = getPosition(messageReference);
        if (position <= 0) {
            return false;
        }

        openMessageAtPosition(position - 1);
        return true;
    }

    @Override
    public boolean openNextMessage(MessageReference messageReference) {
        int position = getPosition(messageReference);
        if (position < 0 || position == messages.size() - 1) {
            return false;
        }

        openMessageAtPosition(position + 1);
        return false;
    }

    @Override
    public boolean isFirst(MessageReference messageReference) {
        return messages.isEmpty() || messageReference.equals(getReferenceForPosition(0));
    }

    @Override
    public boolean isLast(MessageReference messageReference) {
        return messages.isEmpty() || messageReference.equals(getReferenceForPosition(messages.size() - 1));
    }

    private int getPosition(MessageReference messageReference) {
        LocalMessage message = messageReference.restoreToLocalMessage(context);
        return getPositionForUniqueId(message.getId());
    }

    private int getPositionForUniqueId(long uniqueId) {
        for (int position = 0; position < messages.size(); position++) {
            if (messages.get(position).getId() == uniqueId) {
                return position;
            }
        }

        return -1;
    }

    private void openMessageAtPosition(int position) {
        MessageReference ref = getReferenceForPosition(position);
        handler.openMessage(ref);
    }

    private MessageReference getReferenceForPosition(int position) {
        LocalMessage message = messages.get(position);
        if(message == null) {
            return null;
        }

        return message.makeMessageReference();
    }

    @Override
    public void sort(SortType sortType, Boolean ascending) {
        saveSort(sortType, ascending);
        Collections.sort(messages, getComparator(sortType, ascending));
        messageListView.showMessageList(messages);
    }

    private void saveSort(SortType sortType, Boolean ascending) {
        final Preferences preferences = Preferences.getPreferences(K9.getApplication());
        boolean mSortAscending;
        if (account != null) {
            account.setSortType(sortType);

            if (ascending == null) {
                mSortAscending = account.isSortAscending(sortType);
            } else {
                mSortAscending = ascending;
            }

            account.setSortAscending(sortType, mSortAscending);
            account.save(preferences);
        } else {
            K9.setSortType(sortType);

            if (ascending == null) {
                mSortAscending = K9.isSortAscending(sortType);
            } else {
                mSortAscending = ascending;
            }

            K9.setSortAscending(sortType, mSortAscending);

            SharedPreferences.Editor editor = preferences.getPreferences().edit();
            K9.save(editor);
            editor.apply();
        }
    }

    private void loadMessages(final LocalFolder folder) {
        try {
            folder.getMessages(new MessageRetrievalListener() {
                @Override
                public void messageStarted(String uid, int number, int ofTotal) {

                }

                @Override
                public void messageFinished(Message message, int number, int ofTotal) {
                    messages.add((LocalMessage) message);
                }

                @Override
                public void messagesFinished(int total) {

                }
            }, false);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "failed to retrieve messages");
        }
    }

    private Comparator<LocalMessage> getComparator(SortType sortType, boolean ascending) {
        /* we add 3 comparators at most */
        final List<Comparator<LocalMessage>> chain = new ArrayList<>(3);
        final Comparator<LocalMessage> comparator = sortMap.get(sortType);

        // Add the specified comparator
        if (ascending) {
            chain.add(comparator);
        } else {
            chain.add(new ReverseComparator<>(comparator));
        }

        // Add the date comparator if not already specified
        if (sortType != SortType.SORT_DATE && sortType != SortType.SORT_ARRIVAL) {
            final Comparator<LocalMessage> dateComparator = sortMap.get(SortType.SORT_DATE);
            if (ascending) {
                chain.add(dateComparator);
            } else {
                chain.add(new ReverseComparator<>(dateComparator));
            }
        }
        // TODO: sort by id

        // Build the comparator chain
        return new ComparatorChain<>(chain);
    }

    public static class AttachmentComparator extends BooleanComparator implements Comparator<LocalMessage> {
        @Override
        public int compare(LocalMessage lhs, LocalMessage rhs) {
            return compare(lhs.hasAttachments(), rhs.hasAttachments());
        }
    }

    public static class DateComparator implements Comparator<LocalMessage> {
        @Override
        public int compare(LocalMessage lhs, LocalMessage rhs) {
            return lhs.getSentDate().compareTo(rhs.getSentDate());
        }
    }

    public static class ArrivalComparator implements Comparator<LocalMessage> {
        @Override
        public int compare(LocalMessage lhs, LocalMessage rhs) {
            return lhs.getInternalDate().compareTo(rhs.getInternalDate());
        }
    }

    public static class FlaggedComparator extends BooleanComparator implements Comparator<LocalMessage> {

        @Override
        public int compare(LocalMessage lhs, LocalMessage rhs) {
            return compare(lhs.getFlags().contains(Flag.FLAGGED), rhs.getFlags().contains(Flag.FLAGGED));
        }
    }

    public static class StringComparator {
        public int compare(String lhs, String rhs) {
            if(lhs == null && rhs == null) {
                return 0;
            }

            if(lhs == null) {
                return 1;
            }

            if(rhs == null) {
                return -1;
            }

            return lhs.compareTo(rhs);
        }
    }

    public static class BooleanComparator {
        public int compare(boolean lhs, boolean rhs) {
            return lhs == rhs ? 0 : lhs ? 1 : -1;
        }
    }

    public static class SubjectComparator extends StringComparator implements Comparator<LocalMessage> {

        @Override
        public int compare(LocalMessage lhs, LocalMessage rhs) {
            final String lhsSubject = lhs.getSubject();
            final String rhsSubject = rhs.getSubject();
            return compare(lhsSubject, rhsSubject);
        }
    }

    public static class SenderComparator extends StringComparator implements Comparator<LocalMessage> {
        @Override
        public int compare(LocalMessage lhs, LocalMessage rhs) {
            final String sender1 = lhs.getFrom()[0].getAddress();
            final String sender2 = rhs.getFrom()[0].getAddress();
            return compare(sender1, sender2);
        }
    }

    public static class UnreadComparator extends BooleanComparator implements Comparator<LocalMessage> {
        @Override
        public int compare(LocalMessage lhs, LocalMessage rhs) {
            return compare(lhs.getFlags().contains(Flag.SEEN), rhs.getFlags().contains(Flag.SEEN));
        }
    }

    /**
     * Chains comparator to find a non-0 result.
     *
     * @param <T>
     */
    public static class ComparatorChain<T> implements Comparator<T> {
        private List<Comparator<T>> mChain;

        /**
         * @param chain Comparator chain. Never {@code null}.
         */
        public ComparatorChain(final List<Comparator<T>> chain) {
            mChain = chain;
        }

        @Override
        public int compare(T object1, T object2) {
            int result = 0;
            for (final Comparator<T> comparator : mChain) {
                result = comparator.compare(object1, object2);
                if (result != 0) {
                    break;
                }
            }
            return result;
        }
    }

    /**
     * Reverses the result of a {@link Comparator}.
     *
     * @param <T>
     */
    public static class ReverseComparator<T> implements Comparator<T> {
        private Comparator<T> mDelegate;

        /**
         * @param delegate Never {@code null}.
         */
        public ReverseComparator(final Comparator<T> delegate) {
            mDelegate = delegate;
        }

        @Override
        public int compare(final T object1, final T object2) {
            // arg1 & 2 are mixed up, this is done on purpose
            return mDelegate.compare(object2, object1);
        }
    }
}

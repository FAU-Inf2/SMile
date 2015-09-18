package com.fsck.k9.activity;

import android.app.DatePickerDialog;
import android.app.SearchManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.fragment.IMessageListPresenter;
import com.fsck.k9.fragment.MessageFragment;
import com.fsck.k9.fragment.RemindMeDatePickerDialog;
import com.fsck.k9.fragment.RemindMeDialog;
import com.fsck.k9.fragment.RemindMeTimePickerDialog;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchCondition;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.ui.messageview.MessageViewFragment;
import com.fsck.k9.ui.messageview.MessageViewFragmentListener;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.MessageListView;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class Messages extends SmileActivity
        implements IMessageListPresenter,
        MessageViewFragmentListener,RemindMeDialog.NoticeDialogListener,
        TimePickerDialog.OnTimeSetListener,
        DatePickerDialog.OnDateSetListener {
    private static final String ACTION_SHORTCUT = "shortcut";
    private static final String EXTRA_SEARCH = "search";
    public static final String EXTRA_MESSAGE_REFERENCE = "message_reference";
    private static final String EXTRA_SPECIAL_FOLDER = "special_folder";
    public static final String EXTRA_SEARCH_ACCOUNT = "com.fsck.k9.search_account";
    public static final String EXTRA_SEARCH_FOLDER = "com.fsck.k9.search_folder";

    private LocalSearch mSearch;
    private MessageReference mMessageReference;
    private MessageFragment messageFragment;

    public static void actionDisplaySearch(Context context,
                                           SearchSpecification search) {
        context.startActivity(intentDisplaySearch(context, search));
    }

    public static Intent intentDisplaySearch(Context context, SearchSpecification search) {
        Intent intent = new Intent(context, Messages.class);
        intent.putExtra(EXTRA_SEARCH, search);

        return intent;
    }

    public static Intent shortcutIntent(Context context, String specialFolder) {
        Intent intent = new Intent(context, Messages.class);
        intent.setAction(ACTION_SHORTCUT);
        intent.putExtra(EXTRA_SPECIAL_FOLDER, specialFolder);

        return intent;
    }

    public static Intent actionDisplayMessageIntent(Context context,
                                                         MessageReference messageReference) {
        Intent intent = new Intent(context, Messages.class);
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: according to android documentation long running tasks like db upgrade should not be placed inside an activity
        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        JodaTimeAndroid.init(this);
        handleIntent(getIntent());

        FragmentManager fragmentManager = getSupportFragmentManager();
        messageFragment = (MessageFragment) fragmentManager.findFragmentById(R.layout.messages_fragment);

        if(messageFragment == null) {
            messageFragment = MessageFragment.newInstance(mSearch);
        }

        loadFragment(messageFragment);
    }

    private void handleIntent(final Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            decodeExtraActionView(intent);
        } else if (ACTION_SHORTCUT.equals(action)) {
            decodeExtraActionShortcut(intent);
        } else if (intent.getStringExtra(SearchManager.QUERY) != null) {
            decodeExtraActionSearch(intent);
        } else {
            // regular LocalSearch object was passed
            mSearch = intent.getParcelableExtra(EXTRA_SEARCH);
        }
    }

    private void decodeExtraActionSearch(final Intent intent) {
        // check if this intent comes from the system search ( remote )
        if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
            return;
        }
        //Query was received from Search Dialog
        final String query = intent.getStringExtra(SearchManager.QUERY).trim();

        mSearch = new LocalSearch(getString(R.string.search_results));
        mSearch.setManualSearch(true);

        mSearch.or(new SearchCondition(SearchSpecification.SearchField.SENDER, SearchSpecification.Attribute.CONTAINS, query));
        mSearch.or(new SearchCondition(SearchSpecification.SearchField.SUBJECT, SearchSpecification.Attribute.CONTAINS, query));
        // FIXME: SqlQueryBuilder throws RTE
        //mSearch.or(new SearchCondition(SearchField.MESSAGE_CONTENTS, Attribute.CONTAINS, query));

        Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            mSearch.addAccountUuid(appData.getString(EXTRA_SEARCH_ACCOUNT));
            // searches started from a folder list activity will provide an account, but no folder
            if (appData.getString(EXTRA_SEARCH_FOLDER) != null) {
                mSearch.addAllowedFolder(appData.getString(EXTRA_SEARCH_FOLDER));
            }
        } else {
            mSearch.addAccountUuid(LocalSearch.ALL_ACCOUNTS);
        }
    }

    private void decodeExtraActionShortcut(final Intent intent) {
        // Handle shortcut intents
        final String specialFolder = intent.getStringExtra(EXTRA_SPECIAL_FOLDER);
        if (SearchAccount.UNIFIED_INBOX.equals(specialFolder)) {
            mSearch = SearchAccount.createUnifiedInboxAccount(this).getRelatedSearch();
        } else if (SearchAccount.ALL_MESSAGES.equals(specialFolder)) {
            mSearch = SearchAccount.createAllMessagesAccount(this).getRelatedSearch();
        }
    }

    private void decodeExtraActionView(final Intent intent) {
        final Uri uri = intent.getData();
        List<String> segmentList = uri.getPathSegments();
        String accountId = segmentList.get(0);
        Collection<Account> accounts = Preferences.getPreferences(this).getAvailableAccounts();

        for (Account account : accounts) {
            if (String.valueOf(account.getAccountNumber()).equals(accountId)) {
                String folderName = segmentList.get(1);
                String messageUid = segmentList.get(2);
                mMessageReference = new MessageReference(account.getUuid(), folderName, messageUid, null);
                break;
            }
        }
    }

    @Override
    public void onForward(LocalMessage message, PgpData mPgpData) {
        MessageCompose.actionForward(this, message, null);
    }

    @Override
    public void onReply(LocalMessage message, PgpData mPgpData) {
        MessageCompose.actionReply(this, message, false, null);
    }

    @Override
    public void onReplyAll(LocalMessage message, PgpData mPgpData) {
        MessageCompose.actionReply(this, message, true, null);
    }

    @Override
    public void disableDeleteAction() {

    }

    @Override
    public void displayMessageSubject(String title) {

    }

    @Override
    public void setProgress(boolean b) {

    }

    @Override
    public void showNextMessageOrReturn() {

    }

    @Override
    public void messageHeaderViewAvailable(MessageHeader messageHeaderView) {

    }

    @Override
    public void updateMenu() {

    }

    // implement IMessageListPresenter

    @Override
    public void setView(MessageListView messageListView) {

    }

    @Override
    public void move(LocalMessage message, String destFolder) {
        LocalFolder localFolder = null;

        try {
            localFolder = new LocalFolder(message.getAccount().getLocalStore(), destFolder);
            message.getFolder().moveMessages(Collections.singletonList(message), localFolder);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(LocalMessage message) {
        RemindMe remindMe = messageFragment.isRemindMe(message);

        if(remindMe == null) {
            move(message, message.getAccount().getTrashFolderName());
            try {
                message.setFlag(Flag.DELETED, true);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        } else {
            move(message, message.getAccount().getInboxFolderName());
            messageFragment.delete(remindMe);
        }
    }

    @Override
    public void archive(LocalMessage message) {
        move(message, message.getAccount().getArchiveFolderName());
    }

    @Override
    public void remindMe(LocalMessage message) {
        RemindMeDialog dialog = RemindMeDialog.newInstance(message);
        dialog.show(getFragmentManager(), "mTimeValue");
    }

    @Override
    public void reply(LocalMessage message) {

    }

    @Override
    public void replyAll(LocalMessage message) {

    }

    @Override
    public void openMessage(MessageReference messageReference) {
        loadFragment(MessageViewFragment.newInstance(messageReference));
    }

    @Override
    public void sort(Account.SortType sortType, Boolean ascending) {

    }

    @Override
    public void onDialogClick(RemindMeDialog dialog) {
        Log.i(K9.LOG_TAG, "RemindMeList.onDialogClick");
        currentRemindMe = dialog.getRemindMe();

        if(currentRemindMe.getRemindMeInterval() == RemindMe.RemindMeInterval.CUSTOM) {
            onDateSetCalled = false;
            onTimeSetCalled = false;

            final String datePickerTag = "remindMeDatePicker";
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            Fragment prev = fragmentManager.findFragmentByTag(datePickerTag);

            if (prev != null) {
                ft.remove(prev);
            }

            ft.addToBackStack(datePickerTag);

            RemindMeDatePickerDialog datePickerDialog = RemindMeDatePickerDialog.newInstance(this);
            datePickerDialog.show(ft, datePickerTag);
        } else {
            currentRemindMe.setRemindTime(getDelay(currentRemindMe.getRemindMeInterval()));
            messageFragment.add(currentRemindMe);
        }
    }

    private Date getDelay(RemindMe.RemindMeInterval interval) {
        DateTime delay = DateTime.now();

        switch (interval) {
            case LATER:
                delay.plusMinutes(10);
                break;
            case EVENING:
                delay.plusMinutes(30);
                break;
            case TOMORROW:
                delay.plusDays(1);
                break;
        }

        return delay.toDate();
    }

    private boolean onTimeSetCalled = false;
    private boolean onDateSetCalled = false;
    private RemindMe currentRemindMe;

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        if(onDateSetCalled) {
            return;
        }

        onDateSetCalled = true;

        final String timePickerTag = "remindMeTimePicker";
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        Log.d(K9.LOG_TAG, "Selected date: " + calendar.getTime());
        currentRemindMe.setRemindTime(calendar.getTime());
        RemindMeTimePickerDialog timePickerDialog = RemindMeTimePickerDialog.newInstance(this);
        timePickerDialog.show(getFragmentManager(), timePickerTag);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if(onTimeSetCalled) {
            return;
        }

        onTimeSetCalled = true;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentRemindMe.getRemindTime());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        Log.d(K9.LOG_TAG, "Selected time: " + calendar.getTime());
        Date minDate = new Date(System.currentTimeMillis() + 15 * 1000l);

        // do not accept dates in the past -- earliest is 15 seconds in the future
        if(calendar.getTime().before(minDate)) {
            Log.d(K9.LOG_TAG, "Selected date was before min date -- new date: " + minDate);
            currentRemindMe.setRemindTime(minDate);
        } else {
            currentRemindMe.setRemindTime(calendar.getTime());
        }

        messageFragment.add(currentRemindMe);
    }

    private final Date addMinute(final Date date, final int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minute);
        return calendar.getTime();
    }
}

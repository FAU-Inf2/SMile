package com.fsck.k9.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.fsck.k9.Account;
import de.fau.cs.mad.smile.android.R;

import com.fsck.k9.FeatureStorage;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.MessagingException;

public class IMAPAppendTextWorkaroundActivity extends K9Activity {
/* Workaround-Activity for testing the functionality of IMAPAppendText.*/

    private static final String EXTRA_MESSAGE_REFERENCE = "message_reference";
    private static final String EXTRA_ACCOUNT = "account";
    private MessageReference mMessageReference;
    private Account mAccount;
    private Context mContext;
    private String filesDirectory;

    public IMAPAppendTextWorkaroundActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imapappendtext);


        // get current Account
        final Intent intent = getIntent();
        mMessageReference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
        final String accountUuid = (mMessageReference != null) ?
                mMessageReference.getAccountUuid() :
                intent.getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        if (mAccount == null) {
            mAccount = Preferences.getPreferences(this).getDefaultAccount();
        }
        if (mAccount == null) {
            /*
             * There are no accounts set up. This should not have happened. Prompt the
             * user to set up an account as an acceptable bailout.
             */
            startActivity(new Intent(this, Accounts.class));
            finish();
            return;
        }

        //get current Context
        mContext = this.getBaseContext();

        filesDirectory = this.getFilesDir().getAbsolutePath();

        //Button for testing
        Button execute_button = (Button) this.findViewById(R.id.imapAppendExecuteButton);
        execute_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAppend();
            }
        });
        }
    private String testContent;
    private static int counter = 0;
    private void handleAppend() {
        testContent = "Test-content from Workaround!";
        new AppendAndReceiveSmileStorageMessages().execute();
    }
    private class AppendAndReceiveSmileStorageMessages extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            //testImapAppendDirectly();
            testFeatureStorage();
            return null;
        }

        private void testFeatureStorage() {
            MessagingController messagingController = MessagingController.getInstance(getApplication());
            Log.d(K9.LOG_TAG, "Start testing FeatureStorage.");
            FeatureStorage featureStorage = new FeatureStorage(mAccount, mContext,
                    messagingController, filesDirectory);
            featureStorage.saveNewFollowUpMailInformation("uid-uid-uid-uid-" + counter,
                    "message-id-" + counter, System.currentTimeMillis() + 100000);
            counter++;
        }

        private void testImapAppendDirectly(){
            MessagingController messagingController = MessagingController.getInstance(getApplication());
            IMAPAppendText appendText = new IMAPAppendText(mAccount, mContext, messagingController);
            Log.i(K9.LOG_TAG, "Newest MsgID: " + appendText.getCurrentMessageId());
            Log.i(K9.LOG_TAG, "Newest Content: " + appendText.getCurrentContent(null));

            try {
                appendText.appendNewContent(testContent);

            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, getResources().getString(R.string.imap_append_failed));
                return;
            }
            Log.i(K9.LOG_TAG, getResources().getString(R.string.imap_append_done));
            Log.i(K9.LOG_TAG, "Newest MsgID: " + appendText.getCurrentMessageId());
            Log.i(K9.LOG_TAG, "Newest Content: " + appendText.getCurrentContent(null));
        }
    }
}
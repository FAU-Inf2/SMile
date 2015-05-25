package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.MessagingException;

public class IMAPAppendTextWorkaroundActivity extends K9Activity {
/* Use IMAP-command "append" to upload a text to the server.*/

    private static final String EXTRA_MESSAGE_REFERENCE = "message_reference";
    private static final String EXTRA_ACCOUNT = "account";
    private MessageReference mMessageReference;
    private Account mAccount;

    public IMAPAppendTextWorkaroundActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imapappendtext);

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

        Button execute_button = (Button) this.findViewById(R.id.imapAppendExecuteButton);
        execute_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast toast = Toast.makeText(getApplicationContext(), R.string.
                        //imap_append_notimplemented, Toast.LENGTH_SHORT);
                        imap_append_wait, Toast.LENGTH_SHORT);
                //toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);

                toast.show();

                // use imap command "append"
                IMAPAppendText appendText = new IMAPAppendText(mAccount);
                try {
                    appendText.append_new_content("Test-content from Workaround");
                } catch (MessagingException e) {
                    toast = Toast.makeText(getApplicationContext(), R.string.imap_append_failed,
                            Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                toast = Toast.makeText(getApplicationContext(), R.string.imap_append_done,
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }

}
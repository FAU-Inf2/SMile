package com.fsck.k9.crypto;

import android.content.Context;
import android.content.Intent;

import com.fsck.k9.Account;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageComposeHandler;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.message.MessageBuilder;

import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to compose pgp encrypted and/or signed messages.
 */
public class OpenPgpMessageCompose {

    private TextBody textBody;
    private OpenPgpServiceConnection openPgpServiceConnection;
    private List<Address> addresses;
    private PgpData pgpData;
    private MessageComposeHandler handler;
    private Context context;
    private Account account;

    public OpenPgpMessageCompose(TextBody textBody, OpenPgpServiceConnection openPgpServiceConnection, List<Address> addresses, PgpData pgpData, MessageComposeHandler handler, Context context, Account account) {
        this.textBody = textBody;
        this.openPgpServiceConnection = openPgpServiceConnection;
        this.addresses = addresses;
        this.pgpData = pgpData;
        this.handler = handler;
        this.context = context;
        this.account = account;
    }

    public void handlePgp(boolean encryptionActivated, boolean signingActivated) {
        if(pgpData.getEncryptedData() != null) {
            return;
        }
        // OpenPGP Provider API
        String[] emailsArray = null;
        if (encryptionActivated) {
            // get emails as array
            List<String> emails = new ArrayList<String>();

            for (Address address : addresses) {
                emails.add(address.getAddress());
            }
            emailsArray = emails.toArray(new String[emails.size()]);
        }

        if (encryptionActivated && signingActivated) {
            Intent intent = new Intent(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
            intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, emailsArray);
            intent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, account.getPgpKey());
            executeOpenPgpMethod(intent);
        } else if (signingActivated) {
            Intent intent = new Intent(OpenPgpApi.ACTION_SIGN);
            intent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, account.getPgpKey());
            executeOpenPgpMethod(intent);
        } else if (encryptionActivated) {
            Intent intent = new Intent(OpenPgpApi.ACTION_ENCRYPT);
            intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, emailsArray);
            executeOpenPgpMethod(intent);
        }
    }

    private InputStream getOpenPgpInputStream() {
        String text = textBody.getText();

        return new ByteArrayInputStream(text.getBytes(Charset.forName("UTF-8")));
    }

    public void executeOpenPgpMethod(Intent intent) {
        intent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        final InputStream inputStream = getOpenPgpInputStream();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        OpenPgpSignEncryptCallback callback = new OpenPgpSignEncryptCallback(outputStream, MessageCompose.REQUEST_CODE_SIGN_ENCRYPT_OPENPGP, pgpData, handler);

        OpenPgpApi api = new OpenPgpApi(context, openPgpServiceConnection.getService());
        api.executeApiAsync(intent, inputStream, outputStream, callback);
    }
}

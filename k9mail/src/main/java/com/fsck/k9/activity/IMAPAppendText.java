package com.fsck.k9.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fsck.k9.R;

public class IMAPAppendText extends K9Activity {
/*Use IMAP-command "append" to upload a text to the server.*/

    public IMAPAppendText() {
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imapappendtext);

        Button execute_button = (Button) this.findViewById(R.id.imapAppendExecuteButton);
        execute_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v) {
                Toast toast = Toast.makeText(getApplicationContext(), R.string.
                                imap_append_notimplemented, Toast.LENGTH_SHORT);
                            // imap_append_wait, Toast.LENGTH_SHORT);
                //toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();

                // TODO: use imap command "append"

            }
        });
    }
}
package com.fsck.k9.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;

import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.FollowUp;
import com.fsck.k9.mail.Message;

import de.fau.cs.mad.smile.android.R;

public class FollowUpDialog extends DialogFragment {

    public interface NoticeDialogListener {
        void onDialogClick(DialogFragment dialog);
    }

    public static FollowUpDialog newInstance(Message message) {
        FollowUpDialog dlg = new FollowUpDialog();
        dlg.setMessage(message);
        return dlg;
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;
    private int mTimeValue = 0;
    private MessageReference reference;
    private Message message;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.follow_up_default_time_values, new AlertDialogOnClickListener());
        return builder.create();
    }

    public Message getMessage() {
        return message;
    }

    private void setMessage(Message message) {
        this.message = message;
    }

    public int getTimeValue() {
        return mTimeValue;
    }

    private void setTimeValue(int timeValue) {
        this.mTimeValue = timeValue;
    }

    private class AlertDialogOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch(which) {
                case 1: {
                    setTimeValue(10);
                    break;
                }
                case 2: {
                    setTimeValue(30);
                    break;
                }
                case 3: {
                    setTimeValue(-1);
                    break;
                }
            }

            mListener.onDialogClick(FollowUpDialog.this);
        }
    }

}


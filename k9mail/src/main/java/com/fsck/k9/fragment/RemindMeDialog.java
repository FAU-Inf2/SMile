package com.fsck.k9.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.RemindMe;

import de.fau.cs.mad.smile.android.R;

public class RemindMeDialog extends DialogFragment {

    public interface NoticeDialogListener {
        void onDialogClick(DialogFragment dialog);
    }

    public static RemindMeDialog newInstance(Message message) {
        RemindMeDialog dlg = new RemindMeDialog();
        dlg.setRemindMe(new RemindMe());
        dlg.getRemindMe().setReference(message);
        return dlg;
    }

    public static RemindMeDialog newInstance(RemindMe remindMe) {
        RemindMeDialog dlg = new RemindMeDialog();
        dlg.setRemindMe(remindMe);
        return dlg;
    }

    // Use this instance of the interface to deliver action events
    private NoticeDialogListener mListener;
    private RemindMe remindMe;

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
        builder.setItems(R.array.remindme_default_time_values, new AlertDialogOnClickListener());
        return builder.create();
    }

    private void setRemindMe(RemindMe remindMe) {
        this.remindMe = remindMe;
    }

    public RemindMe getRemindMe() {
        return remindMe;
    }

    private class AlertDialogOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch(which) {
                case 0: {
                    getRemindMe().setRemindInterval(RemindMe.RemindInterval.TEN_MINUTES);
                    break;
                }
                case 1: {
                    getRemindMe().setRemindInterval(RemindMe.RemindInterval.THIRTY_MINUTES);
                    break;
                }
                case 2: {
                    getRemindMe().setRemindInterval(RemindMe.RemindInterval.TOMORROW);
                    break;
                }
                case 3: {
                    getRemindMe().setRemindInterval(RemindMe.RemindInterval.CUSTOM);
                    break;
                }
            }

            mListener.onDialogClick(RemindMeDialog.this);
        }
    }

}


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
import com.fsck.k9.mailstore.LocalFolder;

import de.fau.cs.mad.smile.android.R;

public class FollowUpDialog extends DialogFragment {

    public interface NoticeDialogListener {
        void onDialogClick(DialogFragment dialog);
    }

    public static FollowUpDialog newInstance(Message message) {
        FollowUpDialog dlg = new FollowUpDialog();
        dlg.setFollowUp(new FollowUp());
        dlg.getFollowUp().setReference(message);
        return dlg;
    }

    public static FollowUpDialog newInstance(FollowUp followUp) {
        FollowUpDialog dlg = new FollowUpDialog();
        dlg.setFollowUp(followUp);
        return dlg;
    }

    // Use this instance of the interface to deliver action events
    private NoticeDialogListener mListener;
    private FollowUp followUp;

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

    private void setFollowUp(FollowUp followUp) {
        this.followUp = followUp;
    }

    public FollowUp getFollowUp() {
        return followUp;
    }

    private class AlertDialogOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch(which) {
                case 0: {
                    getFollowUp().setRemindInterval(FollowUp.RemindInterval.TEN_MINUTES);
                    break;
                }
                case 1: {
                    getFollowUp().setRemindInterval(FollowUp.RemindInterval.THIRTY_MINUTES);
                    break;
                }
                case 2: {
                    getFollowUp().setRemindInterval(FollowUp.RemindInterval.TOMORROW);
                    break;
                }
                case 3: {
                    getFollowUp().setRemindInterval(FollowUp.RemindInterval.CUSTOM);
                    break;
                }
            }

            mListener.onDialogClick(FollowUpDialog.this);
        }
    }

}


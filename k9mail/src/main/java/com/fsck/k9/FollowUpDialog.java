package com.fsck.k9;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.fsck.k9.activity.MessageReference;

import de.fau.cs.mad.smile.android.R;

public class FollowUpDialog extends DialogFragment {
    public interface NoticeDialogListener {
        void onDialogClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

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


    private int mTimeValue = 0;
    private MessageReference reference;

    private void setTimeValue(int timeValue) {
        this.mTimeValue = timeValue;
    }

    public int getTimeValue() {
        return this.mTimeValue;
    }

    public MessageReference getReference() {
        return reference;
    }

    public void setReference(MessageReference reference) {
        this.reference = reference;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.follow_up_default_time_values, new DialogInterface.OnClickListener() {
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
                }

                mListener.onDialogClick(FollowUpDialog.this);
            }
        });

        return builder.create();
    }
}
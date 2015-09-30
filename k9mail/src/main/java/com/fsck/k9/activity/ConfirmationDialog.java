package com.fsck.k9.activity;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.fsck.k9.Account;

public class ConfirmationDialog extends DialogFragment {

    @StringRes
    private int title;
    private CharSequence message;
    @StringRes
    private int confirmButton;
    @StringRes
    private int cancelButton;

    private Runnable action;
    private Runnable negativeAction;

    /**
     * Creates a customized confirmation dialog ({@link AlertDialog}).
     *
     * @param activity The activity this dialog is created for.
     * @param title The resource id of the text to display in the dialog title
     * @param message The resource id of text to display in the main dialog area
     * @param confirmButton The resource id of the text to display in the confirm button
     * @param cancelButton The resource id of the text to display in the cancel button
     * @param action The action to perform if the user presses the confirm button
     * @return A confirmation dialog with the supplied arguments
     * @see #create(Activity,int,String,int,int,Runnable, Runnable)
     */
    public static ConfirmationDialog create(@StringRes final int title, final CharSequence message,
                                            @StringRes final int confirmButton, @StringRes final int cancelButton,
                                     final Runnable action) {

        return create(title, message, confirmButton, cancelButton, action, null);
    }

    /**
     * Creates a customized confirmation dialog ({@link AlertDialog}).
     *
     * @param title The resource id of the text to display in the dialog title
     * @param message The text to display in the main dialog area
     * @param confirmButton The resource id of the text to display in the confirm button
     * @param cancelButton The resource id of the text to display in the cancel button
     * @param action The action to perform if the user presses the confirm button
     * @param negativeAction The action to perform if the user presses the cancel button. Can be {@code null}.
     * @return A confirmation dialog with the supplied arguments
     */
    public static ConfirmationDialog create(@StringRes final int title, final CharSequence message,
                                            @StringRes final int confirmButton, @StringRes final int cancelButton,
                                final Runnable action, final Runnable negativeAction) {

        ConfirmationDialog dialog = new ConfirmationDialog();
        dialog.title = title;
        dialog.message = message;
        dialog.confirmButton = confirmButton;
        dialog.cancelButton = cancelButton;
        dialog.action = action;
        dialog.negativeAction = negativeAction;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(confirmButton,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        action.run();
                    }
                });
        builder.setNegativeButton(cancelButton,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (negativeAction != null) {
                            negativeAction.run();
                        }
                    }
                });
        return builder.create();
    }
}

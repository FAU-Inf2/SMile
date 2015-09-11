package com.fsck.k9.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;

import com.larswerkman.colorpicker.ColorPicker;

import de.fau.cs.mad.smile.android.R;


/**
 * Dialog displaying a color picker.
 */
public class ColorPickerDialog extends DialogFragment {
    private final static String ARG_COLOR = "color";

    /**
     * The interface users of {@link ColorPickerDialog} have to implement to learn the selected
     * color.
     */
    public interface OnColorChangedListener {
        /**
         * This is called after the user pressed the "OK" button of the dialog.
         *
         * @param color
         *         The ARGB value of the selected color.
         */
        void colorChanged(int color);
    }

    public static ColorPickerDialog newInstance(OnColorChangedListener listener, int color) {
        Bundle args = new Bundle();
        args.putInt(ARG_COLOR, color);
        ColorPickerDialog dialog = new ColorPickerDialog();
        dialog.setArguments(args);
        dialog.setColorChangedListener(listener);
        return dialog;
    }

    private OnColorChangedListener mColorChangedListener;
    private ColorPicker mColorPicker;

    private void setColorChangedListener(OnColorChangedListener mColorChangedListener) {
        this.mColorChangedListener = mColorChangedListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        final int color = savedInstanceState.getInt(ARG_COLOR);
        View view = LayoutInflater.from(context).inflate(R.layout.color_picker_dialog, null);

        mColorPicker = (ColorPicker) view.findViewById(R.id.color_picker);
        mColorPicker.setColor(color);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        builder.setPositiveButton(R.string.okay_action,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mColorChangedListener != null) {
                            mColorChangedListener.colorChanged(mColorPicker.getColor());
                        }
                    }
                });
        builder.setNegativeButton(R.string.cancel_action, null);

        return builder.create();
    }

    /**
     * Set the color the color picker should highlight as selected color.
     *
     * @param color
     *         The (A)RGB value of a color (the alpha channel will be ignored).
     */
    public void setColor(int color) {
        mColorPicker.setColor(color);
    }
}

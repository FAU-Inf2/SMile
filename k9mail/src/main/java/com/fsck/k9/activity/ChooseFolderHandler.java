package com.fsck.k9.activity;

import android.os.Handler;

import java.lang.ref.WeakReference;

public class ChooseFolderHandler extends Handler {
    private WeakReference<ChooseFolder> chooseFolderWeakReference;

    public ChooseFolderHandler(ChooseFolder chooseFolder) {
        chooseFolderWeakReference = new WeakReference<>(chooseFolder);
    }

    private static final int MSG_PROGRESS = 1;
    private static final int MSG_SET_SELECTED_FOLDER = 2;

    @Override
    public void handleMessage(android.os.Message msg) {
        ChooseFolder reference = chooseFolderWeakReference.get();
        if (reference == null) {
            return;
        }

        switch (msg.what) {
            case MSG_PROGRESS: {
                reference.setProgressBarIndeterminateVisibility(msg.arg1 != 0);
                break;
            }
            case MSG_SET_SELECTED_FOLDER: {
                reference.getListView().setSelection(msg.arg1);
                break;
            }
        }
    }

    public void progress(boolean progress) {
        android.os.Message msg = new android.os.Message();
        msg.what = MSG_PROGRESS;
        msg.arg1 = progress ? 1 : 0;
        sendMessage(msg);
    }

    public void setSelectedFolder(int position) {
        android.os.Message msg = new android.os.Message();
        msg.what = MSG_SET_SELECTED_FOLDER;
        msg.arg1 = position;
        sendMessage(msg);
    }
}

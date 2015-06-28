package com.fsck.k9.activity.holder;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FolderViewHolder {
    public TextView folderName;

    public TextView folderStatus;

    public TextView newMessageCount;
    public TextView flaggedMessageCount;
    public View newMessageCountIcon;
    public View flaggedMessageCountIcon;
    public View newMessageCountWrapper;
    public View flaggedMessageCountWrapper;

    public RelativeLayout activeIcons;
    public String rawFolderName;
    public View chip;
    public LinearLayout folderListItemLayout;
}

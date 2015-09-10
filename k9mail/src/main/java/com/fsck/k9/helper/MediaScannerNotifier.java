package com.fsck.k9.helper;


import android.content.Context;
import android.media.MediaScannerConnection;

import java.io.File;


public class MediaScannerNotifier {
    public static void notify(Context context, File file) {
        String[] paths = { file.getAbsolutePath() };
        MediaScannerConnection.scanFile(context, paths, null, null);
    }
}

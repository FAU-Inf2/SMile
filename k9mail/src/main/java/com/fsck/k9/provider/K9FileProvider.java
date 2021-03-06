package com.fsck.k9.provider;


import android.content.Context;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.File;

import de.fau.cs.mad.smile.android.BuildConfig;


public class K9FileProvider extends FileProvider {
    private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";

    public static Uri getUriForFile(Context context, File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(context, AUTHORITY, file);
        return uri.buildUpon().appendQueryParameter("mime_type", mimeType).build();
    }

    @Override
    public String getType(Uri uri) {
        return uri.getQueryParameter("mime_type");
    }
}

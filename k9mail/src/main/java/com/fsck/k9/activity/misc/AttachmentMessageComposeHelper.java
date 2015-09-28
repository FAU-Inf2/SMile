package com.fsck.k9.activity.misc;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsck.k9.activity.MessageCompose;

import java.util.ArrayList;

import de.fau.cs.mad.smile.android.R;

public class AttachmentMessageComposeHelper {

    public static ArrayList<Attachment> createAttachmentList(LinearLayout mAttachments) {
        ArrayList<Attachment> attachments = new ArrayList<Attachment>();
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            View view = mAttachments.getChildAt(i);
            Attachment attachment = (Attachment) view.getTag();
            attachments.add(attachment);
        }

        return attachments;
    }

    public static void addAttachment(MessageCompose messageCompose, Uri uri, LoaderManager.LoaderCallbacks<Attachment> attachmentLoaderCallbacks) {
        addAttachment(messageCompose, uri, null, attachmentLoaderCallbacks);
    }

    public static void addAttachment(MessageCompose messageCompose, Uri uri, String contentType, LoaderManager.LoaderCallbacks<Attachment> attachmentLoaderCallbacks) {
        Attachment attachment = new Attachment();
        attachment.state = Attachment.LoadingState.URI_ONLY;
        attachment.uri = uri;
        attachment.contentType = contentType;
        attachment.loaderId = messageCompose.increaseAndReturnMaxLoadId();

        addAttachmentView(messageCompose, messageCompose.getAttachments(), attachment);

        initAttachmentInfoLoader(messageCompose, attachmentLoaderCallbacks, attachment);
    }

    public static void initAttachmentInfoLoader(MessageCompose messageCompose, LoaderManager.LoaderCallbacks<Attachment> mAttachmentInfoLoaderCallback, Attachment attachment) {
        LoaderManager loaderManager = messageCompose.getSupportLoaderManager();
        Bundle bundle = new Bundle();
        bundle.putParcelable(MessageCompose.LOADER_ARG_ATTACHMENT, attachment);
        loaderManager.initLoader(attachment.loaderId, bundle, mAttachmentInfoLoaderCallback);
    }

    public static void initAttachmentContentLoader(MessageCompose messageCompose, LoaderManager.LoaderCallbacks<Attachment> mAttachmentContentLoaderCallback, Attachment attachment) {
        LoaderManager loaderManager = messageCompose.getSupportLoaderManager();
        Bundle bundle = new Bundle();
        bundle.putParcelable(MessageCompose.LOADER_ARG_ATTACHMENT, attachment);
        loaderManager.initLoader(attachment.loaderId, bundle, mAttachmentContentLoaderCallback);
    }

    public static void addAttachmentView(MessageCompose messageCompose, LinearLayout mAttachments, Attachment attachment) {
        boolean hasMetadata = (attachment.state != Attachment.LoadingState.URI_ONLY);
        boolean isLoadingComplete = (attachment.state == Attachment.LoadingState.COMPLETE);

        View view = messageCompose.getLayoutInflater().inflate(R.layout.message_compose_attachment, mAttachments, false);
        TextView nameView = (TextView) view.findViewById(R.id.attachment_name);
        View progressBar = view.findViewById(R.id.progressBar);

        if (hasMetadata) {
            nameView.setText(attachment.name);
        } else {
            nameView.setText(R.string.loading_attachment);
        }

        progressBar.setVisibility(isLoadingComplete ? View.GONE : View.VISIBLE);

        ImageButton delete = (ImageButton) view.findViewById(R.id.attachment_delete);
        delete.setOnClickListener(messageCompose);
        delete.setTag(view);

        view.setTag(attachment);
        mAttachments.addView(view);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void addAttachmentsFromResultIntent(MessageCompose messageCompose, Intent data, LoaderManager.LoaderCallbacks<Attachment> attachmentLoaderCallbacks) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0, end = clipData.getItemCount(); i < end; i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (uri != null) {
                        addAttachment(messageCompose, uri, attachmentLoaderCallbacks);
                    }
                }
                return;
            }
        }

        Uri uri = data.getData();
        if (uri != null) {
            addAttachment(messageCompose, uri, attachmentLoaderCallbacks);
        }
    }

    static View getAttachmentView(LinearLayout attachments, int loaderId) {
        for (int i = 0, childCount = attachments.getChildCount(); i < childCount; i++) {
            View view = attachments.getChildAt(i);
            Attachment tag = (Attachment) view.getTag();
            if (tag != null && tag.loaderId == loaderId) {
                return view;
            }
        }

        return null;
    }
}

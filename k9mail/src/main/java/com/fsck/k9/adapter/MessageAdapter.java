package com.fsck.k9.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.view.MessageListItemView;

import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        MessageListItemView itemView;

        public MessageViewHolder(View itemView) {
            super(itemView);
            this.itemView = (MessageListItemView)itemView;
        }

        public MessageListItemView getItemView() {
            return itemView;
        }
    }

    private final List<LocalMessage> mMessages;

    public MessageAdapter(final List<LocalMessage> messages) {
        this.mMessages = messages;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.message_list_item, parent, false);
        MessageViewHolder holder = new MessageViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, final int position) {
        final LocalMessage message = mMessages.get(position);
        holder.getItemView().setMessage(message);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }
}

package com.fsck.k9.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fsck.k9.fragment.IMessageListPresenter;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.view.MessageListItemContextMenu;
import com.fsck.k9.view.MessageListItemView;

import java.util.List;

import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        MessageListItemView itemView;

        public MessageViewHolder(final MessageListItemView itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        public MessageListItemView getItemView() {
            return itemView;
        }
    }

    private final List<LocalMessage> messages;
    private final View.OnClickListener onClickListener;
    private final IMessageListPresenter presenter;

    public MessageAdapter(final List<LocalMessage> messages, View.OnClickListener onClickListener, IMessageListPresenter presenter) {
        this.messages = messages;
        this.onClickListener = onClickListener;
        this.presenter = presenter;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        MessageListItemView view = (MessageListItemView)inflater.inflate(R.layout.message_list_item, parent, false);
        view.setOnCreateContextMenuListener(new MessageListItemContextMenu());
        MessageViewHolder holder = new MessageViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, final int position) {
        final LocalMessage message = messages.get(position);
        MessageListItemView itemView = holder.getItemView();
        itemView.setPresenter(presenter);
        itemView.getMessageContainer().setOnClickListener(onClickListener);
        itemView.getFlagged().setOnClickListener(onClickListener);
        itemView.setMessage(message);
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).getId();
    }

    @Override
    public void onViewRecycled(MessageViewHolder holder) {
        super.onViewRecycled(holder);
        MessageListItemView itemView = holder.getItemView();
        itemView.getFlagged().setOnClickListener(null);
        itemView.getMessageContainer().setOnClickListener(null);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}

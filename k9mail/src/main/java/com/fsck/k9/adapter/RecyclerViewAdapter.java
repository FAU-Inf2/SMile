package com.fsck.k9.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.view.AccountView;

import de.fau.cs.mad.smile.android.R;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    /*TODO: Use a spinner for email-address to switch easily between accounts */

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private final Account account;

    enum ItemType {
        HEADER,
        ITEM
    }

    private String mNavigationTitles[];
    private int mIcons[];

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemType type;
        AccountView header;

        TextView textView;
        ImageView imageView;

        public ViewHolder(View itemView, int ViewType) {
            super(itemView);

            if (ViewType == TYPE_ITEM) {
                textView = (TextView) itemView.findViewById(R.id.rowText);
                imageView = (ImageView) itemView.findViewById(R.id.rowIcon);
                type = ItemType.ITEM;
            } else {
                header = (AccountView)itemView;
                type = ItemType.HEADER;
            }
        }
    }

    public RecyclerViewAdapter(String[] titles, int[] icons, Account account) {
        mNavigationTitles = titles;
        mIcons = icons;
        this.account = account;
    }

    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            view = inflater.inflate(R.layout.header, parent, false);
        } else if (viewType == TYPE_ITEM) {
            view = inflater.inflate(R.layout.item_row, parent, false);
        } else {
            return null;
        }

        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerViewAdapter.ViewHolder holder, int position) {
        if (holder.type == ItemType.ITEM) {
            holder.textView.setText(mNavigationTitles[position - 1]);
            holder.imageView.setImageResource(mIcons[position - 1]);
        } else {
            holder.header.setCurrentAccount(account);
        }
    }

    @Override
    public int getItemCount() {
        return mNavigationTitles.length + 1; //includes header view
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;
        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }
}
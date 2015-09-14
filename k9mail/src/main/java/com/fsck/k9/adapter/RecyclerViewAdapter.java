package com.fsck.k9.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.fau.cs.mad.smile.android.R;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    /*TODO: Use a spinner for email-address to switch easily between accounts */

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private String mHeaderName;
    private String mHeaderEmail;

    private String mNavigationTitles[];
    private int mIcons[];

    public static class ViewHolder extends RecyclerView.ViewHolder {
        int holderId;

        TextView textView;
        ImageView imageView;
        TextView name;
        TextView email;

        public ViewHolder(View itemView, int ViewType) {
            super(itemView);

            if (ViewType == TYPE_ITEM) {
                textView = (TextView) itemView.findViewById(R.id.rowText);
                imageView = (ImageView) itemView.findViewById(R.id.rowIcon);
                holderId = 1;
            } else {
                name = (TextView) itemView.findViewById(R.id.name);
                email = (TextView) itemView.findViewById(R.id.email);
                holderId = 0;
            }
        }
    }

    public RecyclerViewAdapter(String[] titles, int[] icons, String name, String email) {
        mNavigationTitles = titles;
        mIcons = icons;
        this.mHeaderName = name;
        this.mHeaderEmail = email;
    }

    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (viewType == TYPE_HEADER) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.header, parent, false);
        } else if (viewType == TYPE_ITEM) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false);
        } else {
            return null;
        }
        return new ViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerViewAdapter.ViewHolder holder, int position) {
        if (holder.holderId == 1) {
            holder.textView.setText(mNavigationTitles[position - 1]);
            holder.imageView.setImageResource(mIcons[position - 1]);
        } else {
            holder.name.setText(mHeaderName);
            holder.email.setText(mHeaderEmail);
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
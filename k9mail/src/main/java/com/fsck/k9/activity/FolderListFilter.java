package com.fsck.k9.activity;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.fsck.k9.K9;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filter to search for occurences of the search-expression in any place of the
 * folder-name instead of doing jsut a prefix-search.
 *
 * @author Marcus@Wolschon.biz
 */
public class FolderListFilter<T> extends Filter {
    /**
     * ArrayAdapter that contains the list of folders displayed in the
     * ListView.
     * This object is modified by {@link #publishResults} to reflect the
     * changes due to the filtering performed by {@link #performFiltering}.
     * This in turn will change the folders displayed in the ListView.
     */
    private ArrayAdapter<T> mAdapter;

    /**
     * All folders.
     */
    private List<T> mOriginalValues = null;

    /**
     * Create a filter for a list of folders.
     *
     * @param adapter
     */
    public FolderListFilter(final ArrayAdapter<T> adapter) {
        this.mAdapter = adapter;
    }

    /**
     * Do the actual search.
     * {@inheritDoc}
     *
     * @see #publishResults(CharSequence, FilterResults)
     */
    @Override
    protected FilterResults performFiltering(CharSequence searchTerm) {
        FilterResults results = new FilterResults();

        // Copy the values from mAdapter to mOriginalValues if this is the
        // first time this method is called.
        if (mOriginalValues == null) {
            int count = mAdapter.getCount();
            mOriginalValues = new ArrayList<T>(count);
            for (int i = 0; i < count; i++) {
                mOriginalValues.add(mAdapter.getItem(i));
            }
        }

        Locale locale = Locale.getDefault();
        if ((searchTerm == null) || (searchTerm.length() == 0)) {
            List<T> list = new ArrayList<T>(mOriginalValues);
            results.values = list;
            results.count = list.size();
        } else {
            final String searchTermString = searchTerm.toString().toLowerCase(locale);
            final String[] words = searchTermString.split(" ");
            final int wordCount = words.length;

            final List<T> values = mOriginalValues;

            final List<T> newValues = new ArrayList<T>();

            for (final T value : values) {
                final String valueText = value.toString().toLowerCase(locale);

                for (int k = 0; k < wordCount; k++) {
                    if (valueText.contains(words[k])) {
                        newValues.add(value);
                        break;
                    }
                }
            }

            results.values = newValues;
            results.count = newValues.size();
        }

        return results;
    }

    /**
     * Publish the results to the user-interface.
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        // Don't notify for every change
        mAdapter.setNotifyOnChange(false);
        try {

            //noinspection unchecked
            final List<T> folders = (List<T>) results.values;
            mAdapter.clear();
            if (folders != null) {
                for (T folder : folders) {
                    if (folder != null) {
                        mAdapter.add(folder);
                    }
                }
            } else {
                Log.w(K9.LOG_TAG, "FolderListFilter.publishResults - null search-result ");
            }

            // Send notification that the data set changed now
            mAdapter.notifyDataSetChanged();
        } finally {
            // restore notification status
            mAdapter.setNotifyOnChange(true);
        }
    }

    public void invalidate() {
        mOriginalValues = null;
    }
}

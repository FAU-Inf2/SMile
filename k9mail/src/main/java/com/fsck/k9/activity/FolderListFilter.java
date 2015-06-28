package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.widget.Filter;

import com.fsck.k9.activity.holder.FolderInfoHolder;

/**
 * Filter to search for occurences of the search-expression in any place of the
 * folder-name instead of doing jsut a prefix-search.
 *
 * @author Marcus@Wolschon.biz
 */
public class FolderListFilter extends Filter {
    private FolderList.FolderListAdapter mAdapter;

    public FolderListFilter(FolderList.FolderListAdapter adapter) {
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
        Locale locale = Locale.getDefault();

        if ((searchTerm == null) || (searchTerm.length() == 0)) {
            List<FolderInfoHolder> list = new ArrayList<FolderInfoHolder>(mAdapter.getFolders());
            results.values = list;
            results.count = list.size();
        } else {
            final String searchTermString = searchTerm.toString().toLowerCase(locale);
            final String[] words = searchTermString.split(" ");
            final int wordCount = words.length;
            final List<FolderInfoHolder> newValues = new ArrayList<FolderInfoHolder>();

            for (final FolderInfoHolder value : mAdapter.getFolders()) {
                if (value.displayName == null) {
                    continue;
                }

                final String valueText = value.displayName.toLowerCase(locale);

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
        //noinspection unchecked
        mAdapter.setFilterFolders(Collections.unmodifiableList((ArrayList<FolderInfoHolder>) results.values));
        // Send notification that the data set changed now
        mAdapter.notifyDataSetChanged();
    }
}
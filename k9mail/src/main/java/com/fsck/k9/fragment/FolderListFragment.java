package com.fsck.k9.fragment;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.holder.FolderInfoHolder;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.adapter.FolderAdapter;
import com.fsck.k9.search.LocalSearch;

import de.fau.cs.mad.smile.android.R;

public final class FolderListFragment extends ListFragment {
    private static final String ARG_ACCOUNT = "accountUuid";

    private FolderAdapter mFolderAdapter;
    private Account mAccount;

    public static FolderListFragment newInstance(String accountUuid) {
        FolderListFragment folderListFragment = new FolderListFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_ACCOUNT, accountUuid);
        folderListFragment.setArguments(arguments);
        return folderListFragment;
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String accountUuid = getArguments().getString(ARG_ACCOUNT);
        mAccount = Preferences.getPreferences(getActivity()).getAccount(accountUuid);
        mFolderAdapter = new FolderAdapter(getActivity(), mAccount);
        setupListView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setListAdapter(mFolderAdapter);
        getListView().setTextFilterEnabled(mFolderAdapter.getFilter() != null);
        setRetainInstance(true); // TODO: check if data is retained across configuration changes
        return view;
    }

    @Override
    public final void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.folder_list_option, menu);
        configureFolderSearchView(menu);
    }

    @Override
    public final void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        // TODO: use contextual action bar http://developer.android.com/guide/topics/ui/menus.html#CAB
        getActivity().getMenuInflater().inflate(R.menu.folder_context, menu);

        FolderInfoHolder folder = mFolderAdapter.getItem(info.position);
        menu.setHeaderTitle(folder.displayName);
    }

    @Override
    public final boolean onContextItemSelected(final MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        FolderInfoHolder folder = mFolderAdapter.getItem(info.position);

        switch (item.getItemId()) {
            case R.id.folder_settings:
                FolderSettings.actionSettings(getActivity(), mAccount, folder.name);
                break;
        }

        return super.onContextItemSelected(item);
    }

    private final void configureFolderSearchView(final Menu menu) {
        final MenuItem folderMenuItem = menu.findItem(R.id.filter_folders);
        final SearchView folderSearchView = (SearchView) folderMenuItem.getActionView();
        folderSearchView.setQueryHint(getString(R.string.folder_list_filter_hint));
        folderSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                folderMenuItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mFolderAdapter.getFilter().filter(newText);
                return true;
            }
        });

        folderSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return false;
            }
        });
    }

    private final void onOpenFolder(final String folder) {
        LocalSearch search = new LocalSearch(folder);
        search.addAccountUuid(mAccount.getUuid());
        search.addAllowedFolder(folder);
        MessageList.actionDisplaySearch(getActivity(), search, false, false);
    }

    private final void setupListView() {
        final ListView listView = getListView();
        listView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        listView.setLongClickable(true);
        listView.setFastScrollEnabled(true);
        listView.setScrollingCacheEnabled(false);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onOpenFolder((mFolderAdapter.getItem(position)).name);
            }
        });

        registerForContextMenu(listView);
        listView.setSaveEnabled(true);
    }
}

package com.fsck.k9.listener;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;

import com.fsck.k9.Account;
import com.fsck.k9.activity.About;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.Settings;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;

import de.fau.cs.mad.smile.android.R;

public class NavigationMenuItemClickListener implements MenuItem.OnMenuItemClickListener {
    private Account account;
    private final Context context;
    private final DrawerLayout drawerLayout;

    public NavigationMenuItemClickListener(Context context, DrawerLayout drawerLayout, Account account) {
        this.context = context;
        this.drawerLayout = drawerLayout;
        this.account = account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        if(itemId == R.id.drawer_settings_item) {
            Settings.actionPreferences(context);
        }

        if(itemId == R.id.drawer_about_item) {
            Intent i = new Intent(context, About.class);
            context.startActivity(i);
        }

        LocalSearch search = null;
        if(account != null) {
            switch (itemId) {
                case R.id.drawer_inbox:
                    search = getSearch(account.getInboxFolderName());
                    break;
                case R.id.drawer_sent:
                    search = getSearch(account.getSentFolderName());
                    break;
                case R.id.drawer_drafts:
                    search = getSearch(account.getDraftsFolderName());
                    break;
                case R.id.drawer_trash:
                    search = getSearch(account.getTrashFolderName());
                    break;
                case R.id.drawer_all_folders: {
                    FolderList.actionHandleAccount(context, account);
                    break;
                }
            }
        }

        switch(itemId) {
            case R.id.drawer_all_messages: {
                SearchAccount searchAccount = SearchAccount.createAllMessagesAccount(context);
                search = searchAccount.getRelatedSearch();
                break;
            }
            case R.id.drawer_unified_inbox: {
                SearchAccount searchAccount = SearchAccount.createUnifiedInboxAccount(context);
                search = searchAccount.getRelatedSearch();
                break;
            }
        }

        if(search != null) {
            MessageList.actionDisplaySearch(context, search, false, false);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private LocalSearch getSearch(final String folder) {
        LocalSearch search = new LocalSearch(folder);
        search.addAccountUuid(account.getUuid());
        search.addAllowedFolder(folder);
        return search;
    }
}

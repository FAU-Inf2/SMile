package com.fsck.k9.activity;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import de.fau.cs.mad.smile.android.R;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.larswerkman.colorpicker.ColorPicker;
import com.fsck.k9.fragment.MessageListFragment.MessageListFragmentListener;

import java.util.List;


public class SmsLikeViewTabs extends K9Activity implements MessageListFragmentListener {

    ScrollView leftScrollView;
    LinearLayout leftLinearLayout;

    private ActionBar mActionBar;
    private TextView mActionBarTitle;
    private TextView mActionBarSubTitle;

    private Menu mMenu;

    private MessageListFragment mMessageListFragment;

    private LocalSearch mSearch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smslikeview_tabs);

        leftScrollView = (ScrollView) findViewById(R.id.smsLikeView_left1);
        leftLinearLayout = (LinearLayout) findViewById(R.id.smsLikeView_left_linear1);

        initializeActionBar();

        initMessages();
    }

    private void initMessages(){
        FragmentManager fragmentManager = getFragmentManager();
        mMessageListFragment = (MessageListFragment) fragmentManager.findFragmentById(
                R.id.sms_message_list_container);

        mSearch = SearchAccount.createAllMessagesAccount(this).getRelatedSearch();

        FragmentTransaction ft = fragmentManager.beginTransaction();
        mMessageListFragment = MessageListFragment.newInstance(mSearch, false,
                (K9.isThreadedViewEnabled()));
        ft.add(R.id.sms_message_list_container, mMessageListFragment);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_list_option, menu);
        mMenu = menu;
        return true;
    }

    public void initLeftButton(){
        leftLinearLayout.removeAllViews();
        List<MessageReference> list =  mMessageListFragment.getMessageReferences();

         for (MessageReference ref : list){
             LocalMessage msg = ref.restoreToLocalMessage(this);
             Address[] addresses = msg.getFrom();
             for (Address adr : addresses){
                 String from = adr.getAddress();
                 Button button = new Button(this);
                 button.setText(from);
                 Integer color = ColorPicker.getRandomColor();
                 button.setBackgroundColor(color);
                 leftLinearLayout.addView(button);
             }
         }
    }

    private void initializeActionBar() {
        mActionBar = getActionBar();

        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setCustomView(R.layout.actionbar_custom);

        View customView = mActionBar.getCustomView();
        mActionBarTitle = (TextView) customView.findViewById(R.id.actionbar_title_first);
        mActionBarSubTitle = (TextView) customView.findViewById(R.id.actionbar_title_sub);

        setActionBarTitle("SmsLikeViewActionBarTitle");
        setActionBarSubTitle("SmsLikeViewActionBarSubTitle");
        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home: {
                goBack();
                return true;
            }
            case R.id.goto_sms_like_view: {
                initLeftButton();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void goBack(){
        NavUtils.navigateUpFromSameTask(this);
    }

    public void setActionBarTitle(String title) { mActionBarTitle.setText(title); }

    public void setActionBarSubTitle(String subTitle) { mActionBarSubTitle.setText(subTitle); }


    public void enableActionBarProgress(boolean enable){}
    public void setMessageListProgress(int level){}
    public void showThread(Account account, String folderName, long rootId){}
    public void showMoreFromSameSender(String senderAddress){}
    public void onResendMessage(LocalMessage message){}
    public void onForward(LocalMessage message){}
    public void onReply(LocalMessage message){}
    public void onReplyAll(LocalMessage message){}
    public void openMessage(MessageReference messageReference){}
    public void setMessageListTitle(String title){}
    public void setMessageListSubTitle(String subTitle){}
    public void setUnreadCount(int unread){}
    public void onCompose(Account account){}
    public boolean startSearch(Account account, String folderName){return false;}
    public void remoteSearchStarted(){}
    public void updateMenu(){}
}

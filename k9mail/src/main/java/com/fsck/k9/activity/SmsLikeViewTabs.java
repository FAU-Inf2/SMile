package com.fsck.k9.activity;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fsck.k9.R;
import com.larswerkman.colorpicker.ColorPicker;

import java.util.Random;

public class SmsLikeViewTabs extends K9Activity {

    ScrollView leftScrollView;
    LinearLayout leftLinearLayout;
    ScrollView rightScrollView;
    LinearLayout rightLinearLayout;
    int buttonNumber = 0;
    int mailNumber = 0;

    private ActionBar mActionBar;
    private TextView mActionBarTitle;
    private TextView mActionBarSubTitle;

    private Menu mMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smslikeview_tabs);

        leftScrollView = (ScrollView) findViewById(R.id.smsLikeView_left);
        leftLinearLayout = (LinearLayout) findViewById(R.id.smsLikeView_left_linear);

        rightScrollView = (ScrollView) findViewById(R.id.smsLikeView_right);
        rightLinearLayout = (LinearLayout) findViewById(R.id.smsLikeView_right_linear);

        initializeActionBar();

        initLeftButton();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_list_option, menu);
        mMenu = menu;
        return true;
    }

    public void initLeftButton(){
        Button button = new Button(this);
        button.setText(String.valueOf(buttonNumber++));
     //   button.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));
        button.getBackground().setAlpha(64);
        Integer color = ColorPicker.getRandomColor();
        button.setBackgroundColor(color);
        button.setTag(color);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = new Button(v.getContext());
                button.setOnClickListener(this);
                button.setText(String.valueOf(buttonNumber++));
                Integer color = ColorPicker.getRandomColor();
                button.setBackgroundColor(color);
                button.setTag(color);
                addTextViews(v);
                //  button.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));
                leftLinearLayout.addView(button);
            }
        });
        leftLinearLayout.addView(button);
    }

    public void addTextViews(View view){
        Random random = new Random();
        for (int i = 0; i < random.nextInt(10); i++)
        {
            TextView textView = new TextView(this);
            textView.setBackgroundColor((Integer) view.getTag());
            textView.setText("Email " + String.valueOf(mailNumber++));
            rightLinearLayout.addView(textView);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void goBack(){}

    public void setActionBarTitle(String title) { mActionBarTitle.setText(title); }

    public void setActionBarSubTitle(String subTitle) { mActionBarSubTitle.setText(subTitle); }
}

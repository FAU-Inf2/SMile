package com.fsck.k9.activity;

import android.os.Bundle;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smslikeview_tabs);

        leftScrollView = (ScrollView) findViewById(R.id.smsLikeView_left);
        leftLinearLayout = (LinearLayout) findViewById(R.id.smsLikeView_left_linear);

        rightScrollView = (ScrollView) findViewById(R.id.smsLikeView_right);
        rightLinearLayout = (LinearLayout) findViewById(R.id.smsLikeView_right_linear);

        initLeftButton();
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
}

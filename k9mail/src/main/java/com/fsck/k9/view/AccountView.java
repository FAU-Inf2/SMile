package com.fsck.k9.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;

import java.util.List;

import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;

public class AccountView extends RelativeLayout {
    private TextView name;
    private Spinner mail;
    private List<Account> accounts;

    public AccountView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.accounts = Preferences.getPreferences(context).getAccounts();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = findById(this, R.id.name);
        mail = findById(this, R.id.email);
        AccountSpinnerAdapter adapter = new AccountSpinnerAdapter(getContext(), accounts);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        mail.setAdapter(adapter);
    }

    public TextView getName() {
        return name;
    }

    public Spinner getMail() {
        return mail;
    }

    public void setName(String name) {
        this.name.setText(name);
    }

    public void setCurrentAccount(Account currentAccount) {
        mail.setSelection(this.accounts.indexOf(currentAccount));
        if(currentAccount != null) {
            setName(currentAccount.getName());
        }
    }

    public void setAccountSpinnerListener(AdapterView.OnItemSelectedListener listener) {
        mail.setOnItemSelectedListener(listener);
    }

    static class AccountSpinnerAdapter extends ArrayAdapter<Account> {
        public AccountSpinnerAdapter(Context context, List<Account> accounts) {
            super(context, android.R.layout.simple_spinner_item, accounts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView)super.getView(position, convertView, parent);
            view.setText(getItem(position).getEmail());
            return view;
        }
    }
}

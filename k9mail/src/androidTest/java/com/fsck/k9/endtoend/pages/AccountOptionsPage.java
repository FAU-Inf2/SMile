package com.fsck.k9.endtoend.pages;

import de.fau.cs.mad.smile.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


public class AccountOptionsPage extends AbstractPage {

    public AccountSetupNamesPage clickNext() {
        onView(withId(R.id.next)).perform(click());
        return new AccountSetupNamesPage();
    }

}

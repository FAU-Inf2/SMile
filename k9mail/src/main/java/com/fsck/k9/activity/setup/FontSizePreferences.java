package com.fsck.k9.activity.setup;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;

import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.fragment.SmilePreferenceFragment;

import de.fau.cs.mad.smile.android.R;

/**
 * Activity to configure the font size of the information displayed in the
 * account list, folder list, message list and in the message view.
 *
 * @see FontSizes
 */
public class FontSizePreferences extends SmilePreferenceFragment {
    /*
     * Keys of the preferences defined in res/xml/font_preferences.xml
     */
    private static final String PREFERENCE_ACCOUNT_NAME_FONT = "account_name_font";
    private static final String PREFERENCE_ACCOUNT_DESCRIPTION_FONT = "account_description_font";
    private static final String PREFERENCE_FOLDER_NAME_FONT = "folder_name_font";
    private static final String PREFERENCE_FOLDER_STATUS_FONT = "folder_status_font";
    private static final String PREFERENCE_MESSAGE_LIST_SUBJECT_FONT = "message_list_subject_font";
    private static final String PREFERENCE_MESSAGE_LIST_SENDER_FONT = "message_list_sender_font";
    private static final String PREFERENCE_MESSAGE_LIST_DATE_FONT = "message_list_date_font";
    private static final String PREFERENCE_MESSAGE_LIST_PREVIEW_FONT = "message_list_preview_font";
    private static final String PREFERENCE_MESSAGE_VIEW_SENDER_FONT = "message_view_sender_font";
    private static final String PREFERENCE_MESSAGE_VIEW_TO_FONT = "message_view_to_font";
    private static final String PREFERENCE_MESSAGE_VIEW_CC_FONT = "message_view_cc_font";
    private static final String PREFERENCE_MESSAGE_VIEW_ADDITIONAL_HEADERS_FONT = "message_view_additional_headers_font";
    private static final String PREFERENCE_MESSAGE_VIEW_SUBJECT_FONT = "message_view_subject_font";
    private static final String PREFERENCE_MESSAGE_VIEW_DATE_FONT = "message_view_date_font";
    private static final String PREFERENCE_MESSAGE_VIEW_CONTENT_FONT_SLIDER = "message_view_content_font_slider";
    private static final String PREFERENCE_MESSAGE_COMPOSE_INPUT_FONT = "message_compose_input_font";
    private static final int FONT_PERCENT_MIN = 40;
    private static final int FONT_PERCENT_MAX = 250;
    private ListPreference mAccountName;
    private ListPreference mAccountDescription;
    private ListPreference mFolderName;
    private ListPreference mFolderStatus;
    private ListPreference mMessageListSubject;
    private ListPreference mMessageListSender;
    private ListPreference mMessageListDate;
    private ListPreference mMessageListPreview;
    private ListPreference mMessageViewSender;
    private ListPreference mMessageViewTo;
    private ListPreference mMessageViewCC;
    private ListPreference mMessageViewAdditionalHeaders;
    private ListPreference mMessageViewSubject;
    private ListPreference mMessageViewDate;
    private SliderPreference mMessageViewContentSlider;
    private ListPreference mMessageComposeInput;

    public static FontSizePreferences newInstance() {
        FontSizePreferences settings = new FontSizePreferences();
        return settings;
    }

    @Override
    public SmilePreferenceFragment openPreferenceScreen() {
        return newInstance();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        super.onCreatePreferences(bundle, s);
        setPreferencesFromResource(R.xml.font_preferences, s);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FontSizes fontSizes = K9.getFontSizes();

        final String screenKey = getPreferenceScreen().getKey();
        if (screenKey == null) {
            setupAccountList(fontSizes);
            setupFolderList(fontSizes);
        } else if (screenKey.equals("message_list_fonts")) {
            setupMessageList(fontSizes);
        } else if (screenKey.equals("message_view_fonts")) {
            setupMessageView(fontSizes);
            mMessageComposeInput = setupListPreference(
                    PREFERENCE_MESSAGE_COMPOSE_INPUT_FONT,
                    Integer.toString(fontSizes.getMessageComposeInput()));
        }
    }

    private void setupMessageView(FontSizes fontSizes) {
        mMessageViewSender = setupListPreference(
                PREFERENCE_MESSAGE_VIEW_SENDER_FONT,
                Integer.toString(fontSizes.getMessageViewSender()));

        mMessageViewTo = setupListPreference(
                PREFERENCE_MESSAGE_VIEW_TO_FONT,
                Integer.toString(fontSizes.getMessageViewTo()));
        mMessageViewCC = setupListPreference(
                PREFERENCE_MESSAGE_VIEW_CC_FONT,
                Integer.toString(fontSizes.getMessageViewCC()));
        mMessageViewAdditionalHeaders = setupListPreference(
                PREFERENCE_MESSAGE_VIEW_ADDITIONAL_HEADERS_FONT,
                Integer.toString(fontSizes.getMessageViewAdditionalHeaders()));
        mMessageViewSubject = setupListPreference(
                PREFERENCE_MESSAGE_VIEW_SUBJECT_FONT,
                Integer.toString(fontSizes.getMessageViewSubject()));
        mMessageViewDate = setupListPreference(
                PREFERENCE_MESSAGE_VIEW_DATE_FONT,
                Integer.toString(fontSizes.getMessageViewDate()));

        setupSlider(fontSizes);
    }

    private void setupMessageList(FontSizes fontSizes) {
        mMessageListSubject = setupListPreference(
                PREFERENCE_MESSAGE_LIST_SUBJECT_FONT,
                Integer.toString(fontSizes.getMessageListSubject()));
        mMessageListSender = setupListPreference(
                PREFERENCE_MESSAGE_LIST_SENDER_FONT,
                Integer.toString(fontSizes.getMessageListSender()));
        mMessageListDate = setupListPreference(
                PREFERENCE_MESSAGE_LIST_DATE_FONT,
                Integer.toString(fontSizes.getMessageListDate()));
        mMessageListPreview = setupListPreference(
                PREFERENCE_MESSAGE_LIST_PREVIEW_FONT,
                Integer.toString(fontSizes.getMessageListPreview()));
    }

    private void setupFolderList(FontSizes fontSizes) {
        mFolderName = setupListPreference(
                PREFERENCE_FOLDER_NAME_FONT,
                Integer.toString(fontSizes.getFolderName()));

        mFolderStatus = setupListPreference(
                PREFERENCE_FOLDER_STATUS_FONT,
                Integer.toString(fontSizes.getFolderStatus()));
    }

    private void setupAccountList(FontSizes fontSizes) {
        mAccountName = setupListPreference(
                PREFERENCE_ACCOUNT_NAME_FONT,
                Integer.toString(fontSizes.getAccountName()));

        mAccountDescription = setupListPreference(
                PREFERENCE_ACCOUNT_DESCRIPTION_FONT,
                Integer.toString(fontSizes.getAccountDescription()));
    }

    private void setupSlider(FontSizes fontSizes) {
        mMessageViewContentSlider = (SliderPreference) findPreference(
                PREFERENCE_MESSAGE_VIEW_CONTENT_FONT_SLIDER);

        final String summaryFormat = getString(R.string.font_size_message_view_content_summary);
        final String titleFormat = getString(R.string.font_size_message_view_content_dialog_title);
        mMessageViewContentSlider.setValue(scaleFromInt(fontSizes.getMessageViewContentAsPercent()));
        mMessageViewContentSlider.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    // Show the preference value in the preference summary field.
                    @Override
                    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                        final SliderPreference slider = (SliderPreference) preference;
                        final Float value = (Float) newValue;
                        slider.setSummary(String.format(summaryFormat, scaleToInt(value)));
                        slider.setDialogTitle(String.format(titleFormat, slider.getTitle(), slider.getSummary()));
                        return true;
                    }
                }
        );

        mMessageViewContentSlider.getOnPreferenceChangeListener().onPreferenceChange(
                mMessageViewContentSlider, mMessageViewContentSlider.getValue());
    }

    /**
     * Update the global FontSize object and permanently store the (possibly
     * changed) font size settings.
     */
    private void saveSettings() {
        FontSizes fontSizes = K9.getFontSizes();

        final String screenKey = getPreferenceScreen().getKey();
        if (screenKey == null) {
            fontSizes.setAccountName(Integer.parseInt(mAccountName.getValue()));
            fontSizes.setAccountDescription(Integer.parseInt(mAccountDescription.getValue()));

            fontSizes.setFolderName(Integer.parseInt(mFolderName.getValue()));
            fontSizes.setFolderStatus(Integer.parseInt(mFolderStatus.getValue()));
        } else if (screenKey.equals("message_list_fonts")) {
            fontSizes.setMessageListSubject(Integer.parseInt(mMessageListSubject.getValue()));
            fontSizes.setMessageListSender(Integer.parseInt(mMessageListSender.getValue()));
            fontSizes.setMessageListDate(Integer.parseInt(mMessageListDate.getValue()));
            fontSizes.setMessageListPreview(Integer.parseInt(mMessageListPreview.getValue()));
        } else if (screenKey.equals("message_view_fonts")) {
            setupMessageView(fontSizes);
            fontSizes.setMessageViewSender(Integer.parseInt(mMessageViewSender.getValue()));
            fontSizes.setMessageViewTo(Integer.parseInt(mMessageViewTo.getValue()));
            fontSizes.setMessageViewCC(Integer.parseInt(mMessageViewCC.getValue()));
            fontSizes.setMessageViewAdditionalHeaders(Integer.parseInt(mMessageViewAdditionalHeaders.getValue()));
            fontSizes.setMessageViewSubject(Integer.parseInt(mMessageViewSubject.getValue()));
            fontSizes.setMessageViewDate(Integer.parseInt(mMessageViewDate.getValue()));
            fontSizes.setMessageViewContentAsPercent(scaleToInt(mMessageViewContentSlider.getValue()));

            fontSizes.setMessageComposeInput(Integer.parseInt(mMessageComposeInput.getValue()));
        }

        SharedPreferences preferences = Preferences.getPreferences(getActivity()).getPreferences();
        Editor editor = preferences.edit();
        fontSizes.save(editor);
        editor.apply();
    }

    private int scaleToInt(float sliderValue) {
        return (int) (FONT_PERCENT_MIN + sliderValue * (FONT_PERCENT_MAX - FONT_PERCENT_MIN));
    }

    private float scaleFromInt(int value) {
        return (float) (value - FONT_PERCENT_MIN) / (FONT_PERCENT_MAX - FONT_PERCENT_MIN);
    }

    @Override
    public void onPause() {
        saveSettings();
        super.onPause();
    }
}

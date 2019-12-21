package com.example.gmara;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    protected static ListPreference setListPreferenceData(ListPreference lp, Activity mActivity) {
        CharSequence[] entries = { "הרב שמואל נבון", "Two", "Three" };
        CharSequence[] entryValues = { "1", "2", "3" };
        if(lp == null)
            lp = new ListPreference(mActivity);
        lp.setEntries(entries);
        lp.setDefaultValue("1");
        lp.setEntryValues(entryValues);
        // lp.setSummary(lp.getEntry());
        lp.setDialogTitle(R.string.magid_dialog);
        lp.setKey("magid_name");
        return lp;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            final ListPreference lp = setListPreferenceData((ListPreference) findPreference("magid_name"), getActivity());
        }
    }
}
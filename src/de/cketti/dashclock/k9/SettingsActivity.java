/*
 * Copyright 2013 Christian Ketterer (cketti)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cketti.dashclock.k9;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.cketti.dashclock.k9.K9Helper.Account;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On handset devices,
 * settings are presented as a single list. On tablets, settings are split by category, with
 * category headers shown to the left of the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html"> Android Design:
 * Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings API Guide</a> for more
 * information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {

    /**
     * Determines whether to always show the simplified settings UI, where settings are presented in
     * a single list. When false, settings are shown as a master/detail two-pane view on tablets.
     * When true, a single pane is shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }
            case R.id.about: {
                new AboutDialogFragment().show(getFragmentManager(), "about");
                return true;
            }
            case R.id.more_apps: {
                showMoreApps();
                return true;
            }
        }

        return false;
    }

    private void showMoreApps() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(getString(R.string.more_apps_uri)));
        startActivity(intent);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the device configuration
     * dictates that a simplified, single-pane UI should be shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        MultiSelectListPreference accountListPreference =
                (MultiSelectListPreference) findPreference("accounts_list");
        new LoadAccounts(this, accountListPreference).execute();
    }

    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For example, 10" tablets
     * are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is true if this is forced
     * via {@link #ALWAYS_SIMPLE_PREFS}, or the device doesn't have an extra-large screen. In these
     * cases, a single-pane "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS || !isXLargeTablet(context);
    }

    /** {@inheritDoc} */
    @Override
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference instanceof MultiSelectListPreference) {
                MultiSelectListPreference multiListPreference =
                        (MultiSelectListPreference) preference;

                int accountCount = multiListPreference.getEntries().length;
                int selectedCount = ((Set<String>) value).size();

                preference.setSummary(preference.getContext().getString(
                        R.string.pref_summary_accounts, selectedCount, accountCount));

            } else {
                preference.setSummary(value.toString());
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(MultiSelectListPreference preference,
            List<Account> accounts) {

        int len = accounts.size();
        CharSequence[] entries = new CharSequence[len];
        CharSequence[] entryValues = new CharSequence[len];
        Set<String> defaultValue = new HashSet<String>();

        Set<String> savedUuids = PreferenceManager.getDefaultSharedPreferences(
                preference.getContext()).getStringSet(preference.getKey(), null);

        // Populate MultiSelectListPreference with entries for all available accounts
        int i = 0;
        for (Account account : accounts) {
            defaultValue.add(account.uuid);
            entries[i] = account.name;
            entryValues[i] = account.uuid;
            i++;
        }
        preference.setEntries(entries);
        preference.setEntryValues(entryValues);

        // Check currently selected accounts
        Set<String> selectedAccounts;
        if (savedUuids == null) {
            // Select all accounts if there was no saved configuration
            selectedAccounts = defaultValue;
        } else {
            // Clear out accounts that no longer exist
            selectedAccounts = new HashSet<String>(savedUuids);
            for (Iterator<String> iter = selectedAccounts.iterator(); iter.hasNext(); ) {
                String accountUuid = iter.next();
                if (!defaultValue.contains(accountUuid)) {
                    iter.remove();
                }
            }
        }
        preference.setValues(selectedAccounts);


        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, selectedAccounts);

        preference.setEnabled(true);
    }

    /**
     * This fragment shows general preferences only. It is used when the activity is showing a
     * two-pane settings UI.
     */
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            MultiSelectListPreference accountListPreference =
                    (MultiSelectListPreference) findPreference("accounts_list");
            new LoadAccounts(getActivity(), accountListPreference).execute();
        }
    }

    static class LoadAccounts extends AsyncTask<Void, String, List<Account>> {

        private Context mContext;
        private MultiSelectListPreference mPreference;

        public LoadAccounts(Context context, MultiSelectListPreference preference) {
            mContext = context;
            mPreference = preference;
        }

        @Override
        protected List<Account> doInBackground(Void... params) {
            if (!K9Helper.isK9Installed(mContext)) {
                publishProgress(mContext.getString(R.string.error_k9_not_installed));
                return null;
            }

            if (!K9Helper.isK9Enabled(mContext)) {
                publishProgress(mContext.getString(R.string.error_k9_not_enabled));
                return null;
            }

            if (!K9Helper.hasK9ReadPermission(mContext)) {
                publishProgress(mContext.getString(R.string.error_k9_no_permission,
                        mContext.getString(R.string.app_name)));
                return null;
            }

            return K9Helper.getAccounts(mContext);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values.length > 0) {
                mPreference.setTitle(mContext.getString(R.string.status_error));
                mPreference.setSummary(values[0]);
            }
        }

        @Override
        protected void onPostExecute(List<Account> result) {
            if (result != null && result.size() > 0) {
                bindPreferenceSummaryToValue(mPreference, result);
            }
        }
    }
}

package de.cketti.dashclock.k9;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import java.util.Arrays;
import java.util.Collection;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
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
     * via {@link #ALWAYS_SIMPLE_PREFS}, or the device doesn't have newer APIs like
     * {@link PreferenceFragment}, or the device doesn't have an extra-large screen. In these cases,
     * a single-pane "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /** {@inheritDoc} */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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

    static class LoadAccounts extends AsyncTask<Void, Void, List<Account>> {

        private Context mContext;
        private MultiSelectListPreference mPreference;

        public LoadAccounts(Context context, MultiSelectListPreference preference) {
            mContext = context;
            mPreference = preference;
        }

        @Override
        protected List<Account> doInBackground(Void... params) {
            return K9Helper.getAccounts(mContext);
        }

        @Override
        protected void onPostExecute(List<Account> result) {
            if (result.size() > 0) {
                bindPreferenceSummaryToValue(mPreference, result);
            }
        }
    }
}

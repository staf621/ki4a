package com.staf621.ki4a;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */

    private static final String TAG = "ki4a";
    protected static String BASE = "/data/data/com.staf621.ki4a/ki4a";

    protected static Preference proxy_host;
    protected static Preference proxy_port;
    protected static Preference proxy_header;
    protected static Preference ssh_password;
    protected static Preference ssh_key;
    protected static Preference iptables_switch;
    protected static Preference airplane_switch;


    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            }
            else if(preference.getKey().equals("proxy_header"))
            {
                try {
                    String newFileName = BASE + "/header_file";
                    OutputStream out = new FileOutputStream(newFileName);
                    byte[] buffer = value.toString().getBytes();

                    out.write(buffer, 0, value.toString().length());
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    Log.e(TAG, "Exception creating header file", e);
                }
            }
            else if(preference.getKey().equals("key_text"))
            {
                try {
                    String newFileName = BASE + "/id_rsa";
                    OutputStream out = new FileOutputStream(newFileName);
                    byte[] buffer = value.toString().getBytes();

                    out.write(buffer, 0, value.toString().length());
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    Log.e(TAG, "Exception creating ssh key file", e);
                }
            }
            else
            {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };


    private static Preference.OnPreferenceChangeListener sBindPreferenceEnablerListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            if (preference.getKey().equals("proxy_switch")) {
                if((boolean) value) {
                    proxy_host.setEnabled(true);
                    proxy_port.setEnabled(true);
                    proxy_header.setEnabled(true);
                }
                else
                {
                    proxy_host.setEnabled(false);
                    proxy_port.setEnabled(false);
                    proxy_header.setEnabled(false);
                }
            }
            else if(preference.getKey().equals("key_switch"))
            {
                if((boolean) value) {
                    ssh_password.setEnabled(false);
                    ssh_key.setEnabled(true);
                }
                else
                {
                    ssh_password.setEnabled(true);
                    ssh_key.setEnabled(false);
                }
            }
            else if(preference.getKey().equals("cellular_switch"))
            {
                if((boolean) value) {
                    airplane_switch.setEnabled(true);
                }
                else
                {
                    airplane_switch.setEnabled(false);
                }
            }
            return true;
        }
    };


    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private static void bindPreferenceEnabler(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceEnablerListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceEnablerListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getBoolean(preference.getKey(), false));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new GeneralPreferenceFragment ())
                .commit();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //Let's go back to main Activity
            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {

    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            proxy_host = findPreference("proxy_host");
            proxy_port = findPreference("proxy_port");
            proxy_header = findPreference("proxy_header");
            ssh_key = findPreference("key_text");
            ssh_password = findPreference("password_text");
            iptables_switch = findPreference("iptables_switch");
            airplane_switch = findPreference("airplane_switch");

            bindPreferenceSummaryToValue(findPreference("server_text"));
            bindPreferenceSummaryToValue(findPreference("server_port"));
            bindPreferenceSummaryToValue(findPreference("user_text"));
            bindPreferenceSummaryToValue(proxy_host);
            bindPreferenceSummaryToValue(proxy_port);
            bindPreferenceSummaryToValue(proxy_header);
            bindPreferenceSummaryToValue(ssh_key);

            bindPreferenceEnabler(findPreference("key_switch"));
            bindPreferenceEnabler(findPreference("proxy_switch"));
            bindPreferenceEnabler(findPreference("cellular_switch"));

            //Enable/Disable iptables switch when connected/disconnected
            if(ki4aService.current_status==Util.STATUS_DISCONNECT) iptables_switch.setEnabled(true);
            else iptables_switch.setEnabled(false);
        }
    }
}

package net.opendasharchive.openarchive;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;


public class SettingsActivity extends AppCompatActivity {

    SettingsFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        fragment = (SettingsFragment)fm.findFragmentByTag("ftag");
        if (fragment == null) {
            FragmentTransaction ft = fm.beginTransaction();
            fragment = new SettingsFragment();
            ft.add(android.R.id.content, fragment, "ftag");
            ft.commit();
        }



    }



    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            // Load the Preferences from the XML file
            addPreferencesFromResource(R.xml.app_prefs);

            initValues();
        }

        private void initValues ()
        {
            /**
            Account account = new Account(getActivity(), null);
            ((EditTextPreference)findPreference("archive_username")).setText(account.getUserName());

            if (!account.isAuthenticated()) {
                findPreference("archive_username").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getActivity().startActivity(new Intent(getActivity(), FirstStartActivity.class));
                        return true;
                    }
                });
            }**/
        }


    }


}

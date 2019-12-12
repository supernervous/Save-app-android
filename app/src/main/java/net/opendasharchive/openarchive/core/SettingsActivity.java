package net.opendasharchive.openarchive.core;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;

import net.opendasharchive.openarchive.R;


public class SettingsActivity extends AppCompatActivity {

    SettingsFragment fragment;

    public final static String KEY_TYPE = "type";
    public final static String KEY_DATAUSE = "datause";
    public final static String KEY_METADATA = "metadata";
    public final static String KEY_NETWORKING = "networking";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_usage);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager fm = getSupportFragmentManager();
        fragment = (SettingsFragment)fm.findFragmentByTag("ftag");
        if (fragment == null) {
            FragmentTransaction ft = fm.beginTransaction();
            fragment = new SettingsFragment();
            ft.add(R.id.content, fragment, "ftag");
            ft.commit();
        }

    }



    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            // Load the Preferences from the XML file

            String type = getActivity().getIntent().getStringExtra(KEY_TYPE);

            if (TextUtils.isEmpty(type) || type.equals(KEY_DATAUSE))
                addPreferencesFromResource(R.xml.app_prefs_datause);
            else if (type.equals(KEY_METADATA))
                addPreferencesFromResource(R.xml.app_prefs_metadata);
            else if (type.equals(KEY_NETWORKING))
                addPreferencesFromResource(R.xml.app_prefs_networking);

            initValues();
        }

        private void initValues ()
        {

        }


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

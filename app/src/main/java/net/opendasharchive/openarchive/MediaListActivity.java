package net.opendasharchive.openarchive;

import android.os.Bundle;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;

public class MediaListActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_list);

        // Show the Up button in the action bar.
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}

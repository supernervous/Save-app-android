package net.opendasharchive.openarchive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.fragments.MediaListFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_MEDIA_ID;
import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_PROGRESS;
import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_STATUS;
import static net.opendasharchive.openarchive.MainActivity.INTENT_FILTER_NAME;


public class UploadManagerActivity extends AppCompatActivity {

    MediaListFragment mFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_manager);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.title_uploads));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFrag = (MediaListFragment)getSupportFragmentManager().findFragmentById(R.id.fragUploadManager);
    }


    @Override
    protected void onResume() {
        super.onResume();


        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_FILTER_NAME));
    }


    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }


    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Log.d("receiver", "Updating media");

            int status = intent.getIntExtra(MESSAGE_KEY_STATUS,-1);
            if (status == (Media.STATUS_UPLOADED))
                mFrag.refresh();
            else if (status == (Media.STATUS_UPLOADING))
            {
                long mediaId = intent.getLongExtra(MESSAGE_KEY_MEDIA_ID,-1);
                long progress = intent.getLongExtra(MESSAGE_KEY_PROGRESS,-1);

                if (mediaId != -1)
                {
                    mFrag.updateItem(mediaId, progress);
                }

            }

        }
    };

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

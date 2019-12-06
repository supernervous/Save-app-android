package net.opendasharchive.openarchive.media;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.publish.PublishService;

import java.util.List;


public class PreviewMediaListActivity extends AppCompatActivity {

    MediaListFragment mFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_media);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.title_activity_batch_media_review));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFrag = (MediaListFragment)getSupportFragmentManager().findFragmentById(R.id.fragUploadManager);
    }


    @Override
    protected void onResume() {
        super.onResume();

        mFrag.refresh();

        mFrag.stopBatchMode();

    }

    @Override
    public void onPause() {
       
        super.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_batch_review_media, menu);

        return true;
    }

    private void batchUpload ()
    {
        List<Media> listMedia = mFrag.getMediaList();

        for (Media media : listMedia)
        {
            media.status = Media.STATUS_QUEUED;
            media.save();
        }

        startService(new Intent(this, PublishService.class));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_upload:
                batchUpload();
                return true;

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

package net.opendasharchive.openarchive;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.coursion.freakycoder.mediapicker.galleries.Gallery;
import com.google.android.material.snackbar.Snackbar;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.ProjectAdapter;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.onboarding.OAAppIntro;
import net.opendasharchive.openarchive.services.PirateBoxSiteController;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Prefs;
import net.opendasharchive.openarchive.util.Utility;

import org.w3c.dom.Text;
import org.witness.proofmode.ProofMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;
import io.cleaninsights.sdk.piwik.Measurer;

import static net.opendasharchive.openarchive.util.Utility.getOutputMediaFile;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OASAVE:Main";
    private ViewPager mPager;
    private ProjectAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //otherwise go right into this app;

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.avatar_default);

        setTitle(R.string.main_activity_title);

        mPager = findViewById(R.id.pager);
        mPagerAdapter = new ProjectAdapter(getSupportFragmentManager());
        mPagerAdapter.updateData(Project.getAllAsList());
        mPager.setAdapter(mPagerAdapter);

        final FloatingActionButton fabMenu = (FloatingActionButton) findViewById(R.id.floating_menu);
        fabMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mPagerAdapter.getCount() > 0)
                    importMedia();
                else {
                    promptNewProject();
                }
            }
        });

        SharedPreferences sharedPref = this.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE);
        boolean showIntro = sharedPref.getBoolean(Globals.PREF_FIRST_TIME_KEY,true);
        if (showIntro)
        {
            Intent intent = new Intent(this, OAAppIntro.class);
            startActivity(intent);

            sharedPref.edit().putBoolean(Globals.PREF_FIRST_TIME_KEY,false).commit();
        }

        //check for any queued uploads and restart
        ((OpenArchiveApp)getApplication()).uploadQueue();

    }

    private final static int REQUEST_NEW_PROJECT_NAME = 1001;

    private void promptNewProject ()
    {
        startActivityForResult(new Intent(this,NewProjectActivity.class),REQUEST_NEW_PROJECT_NAME);

    }


    
    private void startNewProject (String name)
    {
        createProject(name);
        refreshProjects();
    }

    private void createProject (String description)
    {
        Project project = new Project ();
        project.created = new Date();
        project.description = description;
        project.save();
    }

    private void refreshProjects ()
    {
        mPagerAdapter = new ProjectAdapter(getSupportFragmentManager());
        mPagerAdapter.updateData(Project.getAllAsList());
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(0);
    }

    private void refreshCurrentProject ()
    {
        if (mPagerAdapter.getCount() > 0) {
            MediaListFragment frag = ((MediaListFragment) mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem()));
            if (frag != null)
                frag.refresh();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();


        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_FILTER_NAME));

        if (getIntent() != null && getIntent().getData() != null) {

            final Snackbar bar = Snackbar.make(mPager, getString(R.string.importing_media), Snackbar.LENGTH_INDEFINITE);
            Snackbar.SnackbarLayout snack_view = (Snackbar.SnackbarLayout)bar.getView();
            snack_view.addView(new ProgressBar(this));
            // The Very Basic
            new AsyncTask<Void, Void, Media>() {
                protected void onPreExecute() {
                    bar.show();

                }
                protected Media doInBackground(Void... unused) {
                    return handleOutsideMedia(getIntent());
                }
                protected void onPostExecute(Media media) {
                    // Post Code
                    if (media != null) {
                        Intent reviewMediaIntent = new Intent(MainActivity.this, ReviewMediaActivity.class);
                        reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
                        startActivity(reviewMediaIntent);
                    }

                    bar.dismiss();

                    setIntent(null);
                }
            }.execute();



            // handle if started from outside app
        }

        refreshCurrentProject();

        //when the app pauses do a private, randomized-response based tracking of the number of media files
      //  MeasureHelper.track().privateEvent("OpeNArchive", "media imported", Integer.valueOf(fragmentMediaList.getCount()).floatValue(), getMeasurer())
        //        .with(getMeasurer());

    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    public final static String INTENT_FILTER_NAME = "MEDIA_UPDATED";

    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Log.d("receiver", "Updating media");

           refreshCurrentProject ();

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {

            case android.R.id.home:
                showSpaceSettings();
                return true;
                /**
            case R.id.action_settings:
                Intent firstStartIntent = new Intent(this, SettingsActivity.class);
                startActivity(firstStartIntent);
                return true;
            case R.id.action_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;**/
            case R.id.action_new_project:
                promptNewProject();
                return true;
            case R.id.menu_upload_manager:
                startActivity(new Intent(this,UploadManagerActivity.class));
                return true;


        }

        return super.onOptionsItemSelected(item);
    }

    private void showSpaceSettings ()
    {
        Intent intent = new Intent(MainActivity.this, SpaceSettingsActivity.class);
        startActivity(intent);
    }

    private boolean mediaExists (Uri uri)
    {

        if (uri.getScheme() == null || uri.getScheme().equals("file"))
        {
            return new File(uri.getPath()).exists();
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.d(TAG, "onActivityResult, requestCode:" + requestCode + ", resultCode: " + resultCode);

        // Check which request we're responding to
        if (requestCode == Globals.REQUEST_FILE_IMPORT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK && resultData != null) {
                final ArrayList<String> selectionResult = resultData.getStringArrayListExtra("result");

                final Snackbar bar = Snackbar.make(mPager, R.string.importing_media, Snackbar.LENGTH_INDEFINITE);
                Snackbar.SnackbarLayout snack_view = (Snackbar.SnackbarLayout)bar.getView();
                snack_view.addView(new ProgressBar(this));

                for (String result : selectionResult)
                {

                    String mimeType = Utility.getMediaType(result);

                    new AsyncTask<String, Void, Media>() {
                        protected void onPreExecute() {
                            bar.show();
                        }
                        protected Media doInBackground(String... params) {
                            return  importMedia(new File(params[0]), params[1]);
                        }
                        protected void onPostExecute(Media media) {

                            bar.dismiss();

                            refreshCurrentProject();

                        }
                    }.execute(result,mimeType);

                }
            }
        }
        else if (requestCode == REQUEST_NEW_PROJECT_NAME)
        {
            // Make sure the request was successful
            if (resultCode == RESULT_OK && resultData != null) {
                String newProjectName = resultData.getStringExtra("projectName");
                if (!TextUtils.isEmpty(newProjectName))
                    startNewProject(newProjectName);
            }
        }




    }

    private Media importMedia (File fileSource, String mimeType)
    {
        String title = fileSource.getName();
        File fileImport = getOutputMediaFile(title);
        boolean success = fileImport.getParentFile().mkdirs();
        Log.d(TAG,"create parent folders, success=" + success);

        try {
            boolean imported = Utility.writeStreamToFile(new FileInputStream(fileSource),fileImport);
            if (!imported)
                return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        // create media
        Media media = new Media();
        media.setOriginalFilePath(Uri.fromFile(fileImport).toString());
        media.setMimeType(mimeType);
        media.setCreateDate(new Date(fileSource.lastModified()));
        media.setUpdateDate(media.getCreateDate());
        media.status = Media.STATUS_LOCAL;


        Project project = mPagerAdapter.getProject(mPager.getCurrentItem());

        media.projectId = project.getId();

        if (title != null)
            media.setTitle(title);
        media.save();


        //if not offline, then try to notarize

        //if (!PirateBoxSiteController.isPirateBox(this)) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("autoNotarize", false).commit();
    //}

        /**
        String hash = ProofMode.generateProof(this, Uri.fromFile(fileSource));
        if (!TextUtils.isEmpty(hash))
        {
            media.setMediaHash(hash.getBytes());
            media.save();
        }**/


        return media;
    }

    private Media importMedia (Uri uri, String mimeType)
    {
        String title = Utility.getUriDisplayName(this,uri);
        File fileImport = getOutputMediaFile(title);
        try {
            boolean imported = Utility.writeStreamToFile(getContentResolver().openInputStream(uri),fileImport);
            if (!imported)
                return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        // create media
        Media media = new Media();
        media.setOriginalFilePath(Uri.fromFile(fileImport).toString());
        media.setMimeType(mimeType);
        media.setCreateDate(new Date());
        media.status = Media.STATUS_LOCAL;

        if (title != null)
            media.setTitle(title);
        media.save();

        //if not offline, then try to notarize
        //if (!PirateBoxSiteController.isPirateBox(this)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean("autoNotarize", false).commit();
        //}

        String hash = ProofMode.generateProof(this, uri);
        if (!TextUtils.isEmpty(hash))
        {
            media.setMediaHash(hash.getBytes());
            media.save();
        }


        return media;
    }

    private Media handleOutsideMedia(Intent intent) {

        Media media = null;

        if (intent != null && intent.getAction()!= null
          && intent.getAction().equals(Intent.ACTION_SEND)) {

            String mimeType = intent.getType();

            Uri uri = intent.getData();


            if (uri == null)
            {
                if (Build.VERSION.SDK_INT >= 16 && intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                    uri = intent.getClipData().getItemAt(0).getUri();
                }
                else {
                    return null;
                }
            }


            media = importMedia(uri, mimeType);

        }

        return media;
    }



    private void importMedia ()
    {
        if (!askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,1)) {

            Intent intent = new Intent(this,Gallery.class);

// Set the title for toolbar
            intent.putExtra("title", getString(R.string.menu_import_media));
// Mode 1 for both images and videos selection, 2 for images only and 3 for videos!
            intent.putExtra("mode", 1);
            startActivityForResult(intent, Globals.REQUEST_FILE_IMPORT);

        }
    }



    private boolean askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }

            return true;
        }

        return false;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 2);
                break;
            case 2:
                break;
        }

    }


    private Measurer getMeasurer() {
        return ((OpenArchiveApp) getApplication()).getCleanInsightsApp().getMeasurer();
    }



    private boolean checkNearbyPermissions ()
    {
        boolean allowed = false;

        allowed = !askForPermission("android.permission.ACCESS_FINE_LOCATION", 3);

        if (!allowed)
            return false;

        if (Prefs.getNearbyUseBluetooth()) {
            allowed = !askForPermission("android.permission.BLUETOOTH", 1);
            if (!allowed)
                return false;

            allowed = !askForPermission("android.permission.BLUETOOTH_ADMIN", 2);
            if (!allowed)
                return false;
        }

        if (Prefs.getNearbyUseWifi()) {
            allowed = !askForPermission("android.permission.ACCESS_WIFI_STATE", 4);
            if (!allowed)
                return false;
            allowed = !askForPermission("android.permission.CHANGE_WIFI_STATE", 5);
            if (!allowed)
                return false;
            allowed = !askForPermission("android.permission.ACCESS_NETWORK_STATE", 6);
            if (!allowed)
                return false;
            allowed = !askForPermission("android.permission.CHANGE_NETWORK_STATE", 7);
            if (!allowed)
                return false;
        }

        return allowed;
    }



}

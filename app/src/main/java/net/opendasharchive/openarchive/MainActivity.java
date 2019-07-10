package net.opendasharchive.openarchive;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import android.widget.Gallery;
import android.widget.ProgressBar;

import com.google.android.material.snackbar.Snackbar;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.engine.impl.PicassoEngine;
import com.zhihu.matisse.filter.Filter;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.ProjectAdapter;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.onboarding.OAAppIntro;
import net.opendasharchive.openarchive.services.PirateBoxSiteController;
import net.opendasharchive.openarchive.services.WebDAVSiteController;
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
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;
import io.cleaninsights.sdk.piwik.Measurer;
import io.scal.secureshareui.model.Account;

import static net.opendasharchive.openarchive.util.Globals.REQUEST_FILE_IMPORT;
import static net.opendasharchive.openarchive.util.Utility.getOutputMediaFile;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OASAVE:Main";
    private ViewPager mPager;
    private ProjectAdapter mPagerAdapter;

    private FloatingActionButton mFab;

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
        mPagerAdapter = new ProjectAdapter(this,getSupportFragmentManager());
        List<Project> listProjects = Project.getAllAsList();
        mPagerAdapter.updateData(listProjects);
        mPager.setAdapter(mPagerAdapter);

        mFab = (FloatingActionButton) findViewById(R.id.floating_menu);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mPagerAdapter.getCount() > 0)
                    importMedia();
                else {
                    promptNewProject();
                }
            }
        });

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0)
                    mFab.setVisibility(View.GONE);
                else
                    mFab.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        Account account = new Account(this, WebDAVSiteController.SITE_NAME);

        if (account == null || TextUtils.isEmpty(account.getSite()))
        {
            Intent intent = new Intent(this, OAAppIntro.class);
            startActivity(intent);
        }


        if (listProjects.size() > 0)
            mPager.setCurrentItem(1);
        else
            mPager.setCurrentItem(0);


        //check for any queued uploads and restart
        ((OpenArchiveApp)getApplication()).uploadQueue();


    }

    private final static int REQUEST_NEW_PROJECT_NAME = 1001;

    public void promptNewProject ()
    {
        startActivityForResult(new Intent(this,NewProjectActivity.class),REQUEST_NEW_PROJECT_NAME);

    }

    public void onNewProjectClicked (View view)
    {
        promptNewProject();
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
        List<Project> listProjects = Project.getAllAsList();
        mPagerAdapter = new ProjectAdapter(this,getSupportFragmentManager());
        mPagerAdapter.updateData(listProjects);
        mPager.setAdapter(mPagerAdapter);

        if (listProjects.size() > 0)
            mPager.setCurrentItem(1);
        else
            mPager.setCurrentItem(0);


    }

    private void refreshCurrentProject ()
    {
        if (mPager.getCurrentItem() > 0) {
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
        super.onActivityResult(requestCode, resultCode, resultData);
        Log.d(TAG, "onActivityResult, requestCode:" + requestCode + ", resultCode: " + resultCode);

        // Check which request we're responding to
        if (requestCode == REQUEST_FILE_IMPORT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK && resultData != null) {

                final List<Uri> mSelected = Matisse.obtainResult(resultData);
                Log.d("Matisse", "mSelected: " + mSelected);

                final Snackbar bar = Snackbar.make(mPager, R.string.importing_media, Snackbar.LENGTH_INDEFINITE);
                Snackbar.SnackbarLayout snack_view = (Snackbar.SnackbarLayout) bar.getView();
                snack_view.addView(new ProgressBar(this));

                for (Uri result : mSelected) {

                    new AsyncTask<Uri, Void, Media>() {
                        protected void onPreExecute() {
                            bar.show();
                        }

                        protected Media doInBackground(Uri... params) {
                            return importMedia(params[0]);
                        }

                        protected void onPostExecute(Media media) {

                            bar.dismiss();

                            refreshCurrentProject();

                        }
                    }.execute(result);

                }
            }
        } else if (requestCode == REQUEST_NEW_PROJECT_NAME) {
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


        return media;
    }

    private Media importMedia (Uri uri)
    {
        String title = Utility.getUriDisplayName(this,uri);
        String mimeType = Utility.getMimeType(this,uri);

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

        File fileSource = new File(uri.getPath());
        Date createDate = new Date();
        if (fileSource.exists()) {
            createDate = new Date(fileSource.lastModified());
            media.contentLength = fileSource.length();
        }

        media.setOriginalFilePath(Uri.fromFile(fileImport).toString());
        media.setMimeType(mimeType);
        media.setCreateDate(createDate);
        media.setUpdateDate(media.getCreateDate());
        media.status = Media.STATUS_LOCAL;

        Project project = mPagerAdapter.getProject(mPager.getCurrentItem());

        media.projectId = project.getId();

        if (title != null)
            media.setTitle(title);
        media.save();

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


            media = importMedia(uri);

        }

        return media;
    }



    private void importMedia ()
    {
        if (!askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,1)) {

            /**
            Intent intent = new Intent(this, Gallery.class);

// Set the title for toolbar
            intent.putExtra("title", getString(R.string.menu_import_media));
// Mode 1 for both images and videos selection, 2 for images only and 3 for videos!
            intent.putExtra("mode", 1);
            startActivityForResult(intent, Globals.REQUEST_FILE_IMPORT);
            **/

            Matisse.from(MainActivity.this)
                    .choose(MimeType.ofAll())
                    .countable(false)
                    .maxSelectable(100)
              //      .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
              //      .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                    .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    .thumbnailScale(0.85f)
                    .imageEngine(new PicassoEngine())
                    .forResult(REQUEST_FILE_IMPORT);
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




}

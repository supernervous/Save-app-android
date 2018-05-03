package net.opendasharchive.openarchive;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.fragments.NavigationDrawerFragment;
import net.opendasharchive.openarchive.onboarding.FirstStartActivity;
import net.opendasharchive.openarchive.onboarding.OAAppIntro;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Utility;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.cleaninsights.sdk.piwik.Measurer;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

    private MediaListFragment fragmentMediaList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //otherwise go right into this app;

        setContentView(R.layout.activity_main);
        setTitle(R.string.main_activity_title);

        fragmentMediaList = (MediaListFragment)getSupportFragmentManager().findFragmentById(R.id.media_list);

        // handle if started from outside app
        handleOutsideMedia(getIntent());

        final FloatingActionsMenu fabMenu = (FloatingActionsMenu) findViewById(R.id.floating_menu);
        FloatingActionButton fabAction = (FloatingActionButton) findViewById(R.id.floating_menu_import);
        fabAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabMenu.collapse();
                importMedia();
            }
        });

        fabAction = (FloatingActionButton) findViewById(R.id.floating_menu_camera);
        fabAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabMenu.collapse();
                captureMedia(Media.MEDIA_TYPE.IMAGE);
            }
        });

        fabAction = (FloatingActionButton) findViewById(R.id.floating_menu_video);
        fabAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabMenu.collapse();
                captureMedia(Media.MEDIA_TYPE.VIDEO);
            }
        });

        fabAction = (FloatingActionButton) findViewById(R.id.floating_menu_audio);
        fabAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabMenu.collapse();
                captureMedia(Media.MEDIA_TYPE.AUDIO);
            }
        });

        fabAction = (FloatingActionButton) findViewById(R.id.floating_menu_nearby);
        fabAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabMenu.collapse();
                startNearby();
            }
        });

        SharedPreferences sharedPref = this.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE);
        if (sharedPref.getBoolean(Globals.PREF_FIRST_TIME_KEY,true))
        {
            Intent intent = new Intent(this, OAAppIntro.class);
            startActivity(intent);

            sharedPref.edit().putBoolean(Globals.PREF_FIRST_TIME_KEY,false).commit();
        }

        //check for any queued uploads and restart
        ((OpenArchiveApp)getApplication()).uploadQueue();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (fragmentMediaList != null)
            fragmentMediaList.refresh();

        if (Media.getAllMediaAsList().size() == 0)
        {
            findViewById(R.id.media_list).setVisibility(View.GONE);
            findViewById(R.id.media_hint).setVisibility(View.VISIBLE);


        }
        else
        {
            findViewById(R.id.media_list).setVisibility(View.VISIBLE);
            findViewById(R.id.media_hint).setVisibility(View.GONE);

        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_FILTER_NAME));

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

            if (fragmentMediaList != null)
                fragmentMediaList.refresh();

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (id == R.id.action_settings)
        {
            Intent firstStartIntent = new Intent(this, SettingsActivity.class);
            startActivity(firstStartIntent);

            return true;
        }
        else
         if (id == R.id.action_about)
        {
           // Intent intent = new Intent(this, OAAppIntro.class);
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult, requestCode:" + requestCode + ", resultCode: " + resultCode);

        String mimeType = null;

        Uri uri = null;

        if (intent != null)
            uri = intent.getData();

        if (uri == null &&
                requestCode == Globals.REQUEST_IMAGE_CAPTURE)
            uri = mCameraUri;

        if (uri != null) {
            mimeType = getContentResolver().getType(uri);

            // Will only allow stream-based access to files

            try {
                if (uri.getScheme().equals("content") && Build.VERSION.SDK_INT >= 19) {
                    grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            } catch (SecurityException se) {
                Log.d("OA", "security exception accessing URI", se);
            }

        }

        if (resultCode == RESULT_OK) {

            if (requestCode == Globals.REQUEST_IMAGE_CAPTURE) {
                String path = this.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString(Globals.EXTRA_FILE_LOCATION, null);
                mimeType = "image/jpeg";
                Log.d(TAG, "onActivityResult, image path:" + path);
            }

            if (null == mimeType) {
                Log.d(TAG, "onActivityResult: Invalid Media Type");
                Toast.makeText(getApplicationContext(), R.string.error_invalid_media_type, Toast.LENGTH_SHORT).show();
            } else {
                // create media
                Media media = new Media();
                media.setOriginalFilePath(uri.toString());
                media.setMimeType(mimeType);
                media.setCreateDate(new Date());
                media.status = Media.STATUS_LOCAL;
                media.save();

                Intent reviewMediaIntent = new Intent(this, ReviewMediaActivity.class);
                reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
                reviewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(reviewMediaIntent);

            }
        }


        fragmentMediaList.refresh();
    }


    private void handleOutsideMedia(Intent intent) {

        if (intent != null && intent.getAction()!= null
          && intent.getAction().equals(Intent.ACTION_SEND)) {

            String type = intent.getType();

            Uri uri = intent.getData();

            if (uri == null)
            {
                if (Build.VERSION.SDK_INT >= 16 && intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                    uri = intent.getClipData().getItemAt(0).getUri();
                }
                else {
                    return;
                }
            }

            String path = Utility.getRealPathFromURI(this, uri);
            // create media
            Media media = new Media();

            media.setOriginalFilePath(path);
            media.setMimeType(type);

            File fileMedia = new File(path);
            media.setCreateDate(new Date(fileMedia.lastModified()));
            media.setUpdateDate(new Date(fileMedia.lastModified()));

            media.save();

            Intent reviewMediaIntent = new Intent(this, ReviewMediaActivity.class);
            reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
            startActivity(reviewMediaIntent);
        }
    }


    private void startNearby ()
    {
        Intent intent = new Intent(this, NearbyActivity.class);
        startActivity(intent);
    }

    private void importMedia ()
    {
        if (!askForPermission("android.permission.READ_EXTERNAL_STORAGE",1)) {

            // ACTION_OPEN_DOCUMENT is the new API 19 action for the Android file manager
            Intent intent;
            int requestId = Globals.REQUEST_FILE_IMPORT;
            if (Build.VERSION.SDK_INT >= 19) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            } else {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
            }

            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            //String cardMediaId = mCardModel.getStoryPath().getId() + "::" + mCardModel.getId() + "::" + MEDIA_PATH_KEY;
            // Apply is async and fine for UI thread. commit() is synchronous
            //mContext.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(Constants.PREFS_CALLING_CARD_ID, cardMediaId).apply();
            startActivityForResult(intent, requestId);

        }
    }

    private Uri mCameraUri;

    private void captureMedia (Media.MEDIA_TYPE mediaType)
    {

        if (!askForPermission("android.permission.WRITE_EXTERNAL_STORAGE",1)) {

            Intent intent = null;
            int requestId = -1;

            if (mediaType == Media.MEDIA_TYPE.AUDIO) {

                if (!askForPermission("android.permission.RECORD_AUDIO",1)) {

//                    intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);

                    intent = new Intent();
                    intent.setType("audio/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);

                    Intent intent2 = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);

                    Intent chooser = new Intent(Intent.ACTION_CHOOSER);
                    chooser.putExtra(Intent.EXTRA_INTENT, intent);
                    chooser.putExtra(Intent.EXTRA_TITLE, "title");
                    Intent[] intentarray= {intent2};
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,intentarray);

                    requestId = Globals.REQUEST_AUDIO_CAPTURE;
                }


            } else if (mediaType == Media.MEDIA_TYPE.IMAGE) {

                if (!askForPermission("android.permission.CAMERA",1)) {

                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    File photoFile;
                    try {
                        photoFile = getOutputMediaFile();
                    } catch (Exception ex) {
                        Log.e(TAG, "Unable to make image file");
                        return;
                    }

                    getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(Globals.EXTRA_FILE_LOCATION, photoFile.getAbsolutePath()).apply();

                    mCameraUri = FileProvider.getUriForFile(MainActivity.this,
                            BuildConfig.APPLICATION_ID + ".provider",
                            photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,mCameraUri);
                    requestId = Globals.REQUEST_IMAGE_CAPTURE;
                }

            } else if (mediaType == Media.MEDIA_TYPE.VIDEO) {
                if (!askForPermission("android.permission.CAMERA",1)) {

                    intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    requestId = Globals.REQUEST_VIDEO_CAPTURE;
                }
            }

            if (null != intent && intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, requestId);
            }
        }
    }


    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "OpenArchive");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
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


    private Measurer getMeasurer() {
        return ((OpenArchiveApp) getApplication()).getCleanInsightsApp().getMeasurer();
    }
}

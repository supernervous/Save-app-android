package net.opendasharchive.openarchive;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.fragments.NavigationDrawerFragment;
import net.opendasharchive.openarchive.onboarding.FirstStartActivity;
import net.opendasharchive.openarchive.onboarding.OAAppIntro;
import net.opendasharchive.openarchive.services.PirateBoxSiteController;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Prefs;
import net.opendasharchive.openarchive.util.Utility;

import org.witness.proofmode.ProofMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;
import io.cleaninsights.sdk.piwik.Measurer;
import io.scal.secureshareui.model.Account;

import static net.opendasharchive.openarchive.util.Utility.getOutputMediaFile;


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


        if (getIntent() != null) {

            final Snackbar bar = Snackbar.make(fragmentMediaList.getView(), "...", Snackbar.LENGTH_INDEFINITE);
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

        MenuItem mi = menu.findItem(R.id.action_logout);
        Account account = new Account(this, null);

        // if user doesn't have an account
        if(!account.isAuthenticated()) {
            mi.setVisible(false);
        }

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
        else if (id == R.id.action_about)
        {
           // Intent intent = new Intent(this, OAAppIntro.class);
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_logout)
        {
            logout();
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout ()
    {
        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_lbl_logout)
                .setMessage(R.string.alert_logout)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //nothing
                    }
                })
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Account account = new Account(MainActivity.this, null);
                        account.setAuthenticated(false);
                        account.setCredentials("");
                        account.saveToSharedPrefs(MainActivity.this, null);

                        Intent firstStartIntent = new Intent(MainActivity.this, FirstStartActivity.class);
                        startActivity(firstStartIntent);
                    }
                }).create().show();



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
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult, requestCode:" + requestCode + ", resultCode: " + resultCode);

        String mimeType = null;

        Uri uri = null;

        if (intent != null)
            uri = intent.getData();

        if (uri == null) {
            if (requestCode == Globals.REQUEST_IMAGE_CAPTURE) {
                uri = mCameraUri;
                mimeType = "image/jpeg";
                if (!mediaExists(uri))
                    return;
            } else if (requestCode == Globals.REQUEST_AUDIO_CAPTURE) {
                uri = mAudioUri;
                mimeType = "audio/wav";
                if (!mediaExists(uri))
                    return;
            }


        }


        if (uri != null) {

            if (mimeType == null)
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

                String title = Utility.getUriDisplayName(this, uri);
                if (title != null)
                    media.setTitle(title);
                media.save();

                //if not offline, then try to notarize
                if (!PirateBoxSiteController.isPirateBox(this)) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().putBoolean("autoNotarize", false).commit();
                }

                String hash = ProofMode.generateProof(this, uri);
                if (!TextUtils.isEmpty(hash))
                {
                    media.setMediaHash(hash.getBytes());
                    media.save();
                }

                Intent reviewMediaIntent = new Intent(this, ReviewMediaActivity.class);
                reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
                reviewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(reviewMediaIntent);

            }
        }


        fragmentMediaList.refresh();
    }


    private Media handleOutsideMedia(Intent intent) {

        Media media = null;

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
                    return null;
                }
            }

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
            media = new Media();
            media.status = Media.STATUS_LOCAL;
            media.setOriginalFilePath(Uri.fromFile(fileImport).toString());
            media.setMimeType(type);


            if (title != null)
                media.setTitle(title);

            media.setCreateDate(new Date());
            media.setUpdateDate(new Date());

            media.save();


        }

        return media;
    }


    private void startNearby ()
    {
        if (checkNearbyPermissions()) {
            Intent intent = new Intent(this, NearbyActivity.class);
            startActivity(intent);
        }
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

                    startAudioRecorder();
                }


            } else if (mediaType == Media.MEDIA_TYPE.IMAGE) {

                if (!askForPermission("android.permission.CAMERA",1)) {

                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    File photoFile;
                    try {
                        photoFile = getOutputMediaFile("IMG","jpg");
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

    Uri mAudioUri;

    private void startAudioRecorder ()
    {
        File fileAudioPath = getOutputMediaFile("AUDIO","wav");
        mAudioUri = Uri.fromFile(fileAudioPath);

        int color = getResources().getColor(R.color.bright_blue);
        int requestCode = Globals.REQUEST_AUDIO_CAPTURE;
        AndroidAudioRecorder.with(this)
                // Required
                .setFilePath(fileAudioPath.getAbsolutePath())
                .setColor(color)
                .setRequestCode(requestCode)

                // Optional
                .setSource(AudioSource.MIC)
                .setChannel(AudioChannel.STEREO)
                .setSampleRate(AudioSampleRate.HZ_48000)
                .setAutoStart(false)
                .setKeepDisplayOn(true)


                // Start recording
                .record();
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

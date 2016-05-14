package net.opendasharchive.openarchive;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;
import net.opendasharchive.openarchive.db.Media;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.scal.secureshareui.lib.Util;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if user has not accepted EULA
        if(!EulaActivity.isAccepted(this)) {
            Intent firstStartIntent = new Intent(this, FirstStartActivity.class);
            firstStartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(firstStartIntent);
            finish();
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFirstRun = sp.getBoolean(Globals.PREF_FIRST_RUN, true);
        // if first time running app
        if (isFirstRun) {
            initFirstRun(sp);
        }
        else {

            setContentView(R.layout.activity_main);

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
        }
    }




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

        if (id == R.id.action_logout)
        {
            handleLogout();
            return true;
        }
        else if (id == R.id.action_about)
        {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult, requestCode:" + requestCode + ", resultCode: " + resultCode);

        String path = null;
        String mimeType = null;

        if (intent != null) {
            Uri uri = intent.getData();
            mimeType = getContentResolver().getType(uri);

            // Will only allow stream-based access to files

            try {
                if (uri.getScheme().equals("content") && Build.VERSION.SDK_INT >= 19) {
                    grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            catch (SecurityException se)
            {
                Log.d("OA","security exception accessing URI",se);
            }

            path = Utility.getRealPathFromURI(this, uri);


        }

        if (resultCode == RESULT_OK) {
            if(requestCode == Globals.REQUEST_IMAGE_CAPTURE) {
                path = this.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString(Globals.EXTRA_FILE_LOCATION, null);
                mimeType = "image/jpeg";
                Log.d(TAG, "onActivityResult, image path:" + path);

            }

            if (null == path) {
                Log.d(TAG, "onActivityResult: Invalid file on import or capture");
                Toast.makeText(getApplicationContext(), R.string.error_file_not_found, Toast.LENGTH_SHORT).show();
            } else if (null == mimeType) {
                Log.d(TAG, "onActivityResult: Invalid Media Type");
                Toast.makeText(getApplicationContext(), R.string.error_invalid_media_type, Toast.LENGTH_SHORT).show();
            } else {
                // create media
                Media media = new Media(this, path, mimeType);
                media.save();

                Intent reviewMediaIntent = new Intent(this, ReviewMediaActivity.class);
                reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
                startActivity(reviewMediaIntent);
            }
        }
    }

    private void initFirstRun(SharedPreferences sp) {
        //Do firstRUn things here


        // set first run flag as false
        sp.edit().putBoolean(Globals.PREF_FIRST_RUN, false).apply();
    }


    private void handleOutsideMedia(Intent intent) {

        if (intent != null && intent.getAction()!= null
        && intent.getAction().equals(Intent.ACTION_SEND)) {

            String type = intent.getType();

            Uri uri = intent.getData();

            if (uri == null)
            {
                if (intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                    uri = intent.getClipData().getItemAt(0).getUri();
                }
                else {
                    return;
                }
            }


            String path = Utility.getRealPathFromURI(this, uri);
            // create media
            Media media = new Media(this, path, type);
            media.save();

            Intent reviewMediaIntent = new Intent(this, ReviewMediaActivity.class);
            reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
            startActivity(reviewMediaIntent);
        }
    }

    private void handleLogout() {
        final SharedPreferences sharedPrefs = this.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE);

        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_lbl_logout)
                .setCancelable(true)
                .setMessage(R.string.alert_logout)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //clear all user prefs
                        sharedPrefs.edit().clear().commit();
                        finish();
                        Intent firstStartIntent = new Intent(MainActivity.this, FirstStartActivity.class);
                        firstStartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(firstStartIntent);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(R.drawable.ic_dialog_alert_holo_light)
                .show();
    }

    private void importMedia ()
    {
        // ACTION_OPEN_DOCUMENT is the new API 19 action for the Android file manager
        Intent intent;
        int requestId = Globals.REQUEST_FILE_IMPORT;
        if (Build.VERSION.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        //String cardMediaId = mCardModel.getStoryPath().getId() + "::" + mCardModel.getId() + "::" + MEDIA_PATH_KEY;
        // Apply is async and fine for UI thread. commit() is synchronous
        //mContext.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(Constants.PREFS_CALLING_CARD_ID, cardMediaId).apply();
        startActivityForResult(intent, requestId);
    }

    private void captureMedia (Media.MEDIA_TYPE mediaType)
    {
        Intent intent = null;
        int requestId = -1;

        if (mediaType == Media.MEDIA_TYPE.AUDIO) {
            intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
            requestId = Globals.REQUEST_AUDIO_CAPTURE;

        } else if (mediaType == Media.MEDIA_TYPE.IMAGE) {
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Unable to make image file");
                return;
            }

            getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(Globals.EXTRA_FILE_LOCATION, photoFile.getAbsolutePath()).apply();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            requestId = Globals.REQUEST_IMAGE_CAPTURE;

        } else if (mediaType == Media.MEDIA_TYPE.VIDEO) {
            intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            requestId = Globals.REQUEST_VIDEO_CAPTURE;
        }

        if (null != intent && intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, requestId);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }
}

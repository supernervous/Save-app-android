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
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import net.opendasharchive.openarchive.db.Media;

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, ArchiveSettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        else if (id == R.id.action_uploads)
        {
            Intent mediaListIntent = new Intent(this, MediaListActivity.class);
            startActivity(mediaListIntent);
        }
        else if (id == R.id.action_logout)
        {
            handleLogout();
        }
        else if (id == R.id.action_about)
        {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
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

            if (uri.getScheme().equals("content") && Build.VERSION.SDK_INT >= 19) {
                grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
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

        if (intent != null && intent.getAction().equals(Intent.ACTION_SEND)) {

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
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(R.drawable.ic_dialog_alert_holo_light)
                .show();
    }
}

package net.opendasharchive.openarchive.nearby;


import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.opendasharchive.openarchive.Globals;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaDeserializer;
import net.opendasharchive.openarchive.util.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;


public class NearbyActivity extends FragmentActivity {

    private TextView mTvNearbyLog;
    private DonutProgress mProgress;
    private LinearLayout mViewNearbyDevices;
    private SwitchCompat mSwitchPairedOnly;

    private boolean mIsServer = false;

    private NearbyListener mNearbyListener = null;

    private Media mMedia = null;

    private boolean mPairedDevicesOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);

        mTvNearbyLog = (TextView)findViewById(R.id.tvnearbylog);
        mViewNearbyDevices = (LinearLayout)findViewById(R.id.nearbydevices);
        mSwitchPairedOnly = (SwitchCompat)findViewById(R.id.tbPairedDevices);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mPairedDevicesOnly = prefs.getBoolean("pairedonly",false);
        mSwitchPairedOnly.setChecked(mPairedDevicesOnly);

        mSwitchPairedOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPairedDevicesOnly = isChecked;

                prefs.edit().putBoolean("pairedonly",mPairedDevicesOnly).commit();
                restartNearby();


            }
        });

        mProgress = (DonutProgress)findViewById(R.id.donut_progress);
        mProgress.setMax(100);

        Button btn = (Button)findViewById(R.id.btnCancel);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                cancelNearby();

            }
        });

        mNearbyListener = new NearbyListener() {

            @Override
            public void transferComplete (Neighbor neighbor, NearbyMedia media) {
                addMedia(media);

                /**
                Message message = Message.obtain(mHandler,Constants.MessageType.DATA_PROGRESS_UPDATE);
                message.getData().putInt("progress",100);
                message.sendToTarget();
                 **/
            }

            @Override
            public void foundNeighbor (Neighbor neighbor)
            {
                Snackbar snackbar = Snackbar
                        .make(findViewById(R.id.main_nearby), "Found " + neighbor.mName, Snackbar.LENGTH_SHORT);
                snackbar.show();
            }

            @Override
            public void transferProgress (Neighbor neighbor, File fileMedia, String title, String mimeType, long transferred, long total)
            {
                int perComplete = (int) ((((float) (total-transferred)) / ((float) total)) * 100f);

                /**
                Message message = Message.obtain(mHandler,Constants.MessageType.DATA_PROGRESS_UPDATE);
                message.getData().putInt("progress",perComplete);
                message.sendToTarget();
                 **/
            }

            @Override
            public void noNeighborsFound()
            {

            }
        };

        mIsServer = getIntent().getBooleanExtra("isServer",false);

        if (mIsServer) {
            try {
                startServer();
                mProgress.setInnerBottomText(">>>>>>>>");
            }
            catch (IOException ioe)
            {
                Log.e("Nearby","error starting server",ioe);
            }
        }
        else {
            startClient();
            mProgress.setInnerBottomText("<<<<<<<<");
        }

    }

    private synchronized void addMedia (final NearbyMedia nearbyMedia)
    {

        Media media = null;

        if (nearbyMedia.mMetadataJson == null) {
            media = new Media();

            media.setMimeType(nearbyMedia.mMimeType);
            media.setCreateDate(new Date(nearbyMedia.mFileMedia.lastModified()));
            media.setUpdateDate(new Date(nearbyMedia.mFileMedia.lastModified()));
            media.setTitle(nearbyMedia.mTitle);
        }
        else
        {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Media.class, new MediaDeserializer());
            gsonBuilder.setDateFormat(DateFormat.FULL, DateFormat.FULL);
            Gson gson = gsonBuilder.create();
            media = gson.fromJson(nearbyMedia.mMetadataJson, Media.class);
        }

        if (media.getMediaHash() == null)
            media.setMediaHash(nearbyMedia.mDigest);

        //set the local file path for both
        media.setOriginalFilePath(nearbyMedia.mFileMedia.getAbsolutePath());

        List<Media> results = Media.find(Media.class, "title = ? AND author = ?", media.title,media.author);

        if (results == null || results.isEmpty()) {
            media.save();

            Snackbar snackbar = Snackbar
                    .make(findViewById(R.id.main_nearby), media.getTitle(), Snackbar.LENGTH_LONG);

            snackbar.setAction(getString(R.string.action_open), new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(nearbyMedia.mFileMedia), nearbyMedia.mMimeType);
                    startActivity(intent);
                }
            });

            snackbar.show();
        }
        else
        {
            final Media mediaExisting = results.get(0);

            Snackbar snackbar = Snackbar
                    .make(findViewById(R.id.main_nearby), mediaExisting.getTitle(), Snackbar.LENGTH_LONG);

            snackbar.setAction(getString(R.string.action_open), new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(nearbyMedia.mFileMedia), nearbyMedia.mMimeType);
                    startActivity(intent);
                }
            });

            snackbar.show();
        }
    }



    private void restartNearby ()
    {
        /**

        new Thread ()
        {
            public void run ()
            {

                if (mBluetoothServer != null)
                    mBluetoothServer.stopSharing();

                if (mBluetoothClient != null)
                    mBluetoothClient.cancel();

                //now start again
                if (mIsServer)
                    try {
                        startServer();

                    }
                    catch (IOException ioe)
                    {
                        Log.e("Nearby","error starting server",ioe);
                    }
                else
                    startClient();

            }
        }.start();
         **/
    }

    private void cancelNearby ()
    {
        /**
        new Thread ()
        {
            public void run ()
            {
                if (mBluetoothServer != null)
                    mBluetoothServer.stopSharing();

                if (mBluetoothClient != null)
                    mBluetoothClient.cancel();

                if (mNsdService != null)
                    mNsdService.stopSharing();



            }
        }.start();
         **/

    }

    private void log (String msg)
    {
        if (mTvNearbyLog != null)
            mTvNearbyLog.setText(msg);

        Log.d("Nearby",msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cancelNearby();
    }

    private void startServer () throws IOException {

        NearbyMedia nearbyMedia = new NearbyMedia();

        long currentMediaId = getIntent().getLongExtra(Globals.EXTRA_CURRENT_MEDIA_ID, -1);

        if (currentMediaId >= 0)
            mMedia = Media.findById(Media.class, currentMediaId);


        File fileMedia = new File(mMedia.getOriginalFilePath());

        InputStream is = new FileInputStream(fileMedia);
        byte[] digest = Utility.getDigest(fileMedia);

        if (mMedia.getMediaHash() == null)
            mMedia.setMediaHash(digest);

        String title = mMedia.getTitle();
        if (TextUtils.isEmpty(title))
            title = fileMedia.getName();


        //mBluetoothServer = new BluetoothSender(this);

        Gson gson = new GsonBuilder()
                .setDateFormat(DateFormat.FULL, DateFormat.FULL).create();
        nearbyMedia.mMetadataJson = gson.toJson(mMedia);

        /**
        if (mBluetoothServer.isNetworkEnabled()) {
            mBluetoothServer.setPairedDevicesOnly(mPairedDevicesOnly);
            mBluetoothServer.setNearbyListener(mNearbyListener);

            mBluetoothServer.setShareFile(fileMedia, digest, title, mMedia.getMimeType(),nearbyMedia.mMetadataJson);
            mBluetoothServer.startSharing();
        }
        */

        nearbyMedia.mTitle = mMedia.getTitle();
        nearbyMedia.mFileMedia = fileMedia;
        nearbyMedia.mMimeType = mMedia.getMimeType();

        /**
        mNsdService = new NSDSender(this);
        mNsdService.setShareFile(nearbyMedia);
        mNsdService.startSharing();
        **/
    }

    private void startClient ()
    {

        /**
        mBluetoothClient = new BluetoothReceiver(this);

        if (mBluetoothClient.isNetworkEnabled()) {
            mBluetoothClient.setPairedDevicesOnly(mPairedDevicesOnly);
            mBluetoothClient.setNearbyListener(mNearbyListener);
            mBluetoothClient.start();
        }

        NSDReceiver nsdClient = new NSDReceiver(this);
        nsdClient.setListener(mNearbyListener);
        nsdClient.startDiscovery();
        **/


    }


    private void addDeviceToView (BluetoothDevice device)
    {

        LinearLayout.LayoutParams imParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        LinearLayout.LayoutParams imgvwDimens = new LinearLayout.LayoutParams(60, 60);
        ImageView iv = new ImageView(this);
        iv.setLayoutParams(imgvwDimens);

        TextDrawable drawable = TextDrawable.builder()
                .buildRoundRect(device.getName().substring(0,1), Color.GREEN, 10);

        iv.setImageDrawable(drawable);

        mViewNearbyDevices.addView(iv,imParams);

    }

    private void noPairedDevices ()
    {
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.main_nearby), "You have no paired devices. Add now?", Snackbar.LENGTH_LONG);

        snackbar.setAction("Add", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            openBluetoothSettings();
            }
        });

        snackbar.show();
    }

    private void openBluetoothSettings ()
    {
        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intentOpenBluetoothSettings);
    }

    private Handler mHandler = new Handler ()
    {
        @Override
        public void handleMessage(Message message) {
            /**
            switch (message.what) {

                case Constants.MessageType.DATA_PROGRESS_UPDATE: {
                    log("data progress update");
                    mProgress.setProgress(message.getData().getInt("progress"));
                    break;
                }


            }
             **/
        }
    };

}

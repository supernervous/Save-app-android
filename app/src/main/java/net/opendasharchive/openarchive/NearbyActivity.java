package net.opendasharchive.openarchive;


import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.opendasharchive.openarchive.nearby.AyandaClient;
import net.opendasharchive.openarchive.nearby.AyandaServer;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaDeserializer;
import net.opendasharchive.openarchive.util.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sintulabs.p2p.Ayanda;
import sintulabs.p2p.IWifiDirect;
import sintulabs.p2p.NearbyMedia;
import sintulabs.p2p.Server;


public class NearbyActivity extends FragmentActivity {

    private TextView mTvNearbyLog;
    private DonutProgress mProgress;
    private LinearLayout mViewNearbyDevices;

    private boolean mIsServer = false;

    private Media mMedia = null;

    private Ayanda mAyanda;
    private List mPeers = new ArrayList();
    private List mPeerNames = new ArrayList();
    private ArrayAdapter<String> mPeersAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);

        mAyanda = new Ayanda(this, null, null, nearbyWifiDirect);

        mTvNearbyLog = findViewById(R.id.tvnearbylog);
        mViewNearbyDevices = findViewById(R.id.nearbydevices);

        mProgress = findViewById(R.id.donut_progress);
        mProgress.setMax(100);

        Button btn = findViewById(R.id.btnCancel);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                cancelNearby();
            }
        });

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

    /**
    private synchronized void addMedia (final NearbyMedia nearbyMedia)
    {

        Media media = null;

        if (nearbyMedia.mMetadataJson == null) {
            media = new Media();

            media.setMimeType(nearbyMedia.mMimeType);
            media.setCreateDate(new Date());
            media.setUpdateDate(new Date());

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
        media.setOriginalFilePath(nearbyMedia.mUriMedia.toString());

        List<Media> results = Media.find(Media.class, "title = ? AND author = ?", media.title,media.author);

        if (results == null || results.isEmpty()) {
            media.save();

            Snackbar snackbar = Snackbar
                    .make(findViewById(R.id.main_nearby), media.getTitle(), Snackbar.LENGTH_LONG);

            snackbar.setAction(getString(R.string.action_open), new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(nearbyMedia.mUriMedia, nearbyMedia.mMimeType);
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
                    intent.setDataAndType(nearbyMedia.mUriMedia, nearbyMedia.mMimeType);
                    startActivity(intent);
                }
            });

            snackbar.show();
        }
    }
    **/


    private void restartNearby ()
    {
        mAyanda.wdDiscover();
    }

    private void cancelNearby ()
    {

    }

    private void log (String msg)
    {
        if (mTvNearbyLog != null)
            mTvNearbyLog.setText(msg);

        Log.d("Nearby",msg);
    }

    @Override
    public void onResume () {
        super.onResume();
        mAyanda.wdRegisterReceivers();
        restartNearby();
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        mAyanda.wdUnregisterReceivers();
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

        Uri uriMedia =Uri.parse(mMedia.getOriginalFilePath());
        InputStream is = getContentResolver().openInputStream(uriMedia);
        byte[] digest = Utility.getDigest(is);

        if (mMedia.getMediaHash() == null)
            mMedia.setMediaHash(digest);

        String title = mMedia.getTitle();
        if (TextUtils.isEmpty(title))
            title = uriMedia.getLastPathSegment();

        Gson gson = new GsonBuilder()
                .setDateFormat(DateFormat.FULL, DateFormat.FULL).create();
        nearbyMedia.mMetadataJson = gson.toJson(mMedia);

        nearbyMedia.mTitle = title;

        //nearbyMedia.mUriMedia = uriMedia;

        nearbyMedia.mMimeType = mMedia.getMimeType();
        mAyanda.wdShareFile(nearbyMedia);

        try {
            int defaultPort = 8080;
            mAyanda.setServer(new AyandaServer(this, defaultPort));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startClient ()
    {

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

    /**
    private void openBluetoothSettings ()
    {
        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intentOpenBluetoothSettings);
    }
     **/

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

    IWifiDirect nearbyWifiDirect = new IWifiDirect () {

        @Override
        public void onConnectedAsClient(final InetAddress groupOwnerAddress) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    AyandaClient client = new AyandaClient(NearbyActivity.this);
                    try {

                        final String response = client
                                .get(groupOwnerAddress.getHostAddress() + ":" + Integer.toString(8080));
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(NearbyActivity.this, response, Toast.LENGTH_LONG).show();
                            }
                        });


                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        final File file = client.getFile(groupOwnerAddress.getHostAddress() + ":" + Integer.toString(8080));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    if (mMedia != null) {

                        NearbyMedia nearbyMedia = new NearbyMedia();
                        client.uploadFile(groupOwnerAddress.getHostAddress() + ":" + Integer.toString(8080), nearbyMedia);
                    }
                }
            }).start();
        }

        @Override
        public void wifiP2pStateChangedAction(Intent intent) {

        }

        @Override
        public void wifiP2pPeersChangedAction() {
            mPeers.clear();
            // TODO fix error when WiFi off
            mPeers.addAll(mAyanda.wdGetDevicesDiscovered() );
            mPeerNames.clear();
            for (int i = 0; i < mPeers.size(); i++) {
                WifiP2pDevice device = (WifiP2pDevice) mPeers.get(i);
                mPeersAdapter.add(device.deviceName);
            }
        }

        @Override
        public void wifiP2pConnectionChangedAction(Intent intent) {

        }

        @Override
        public void wifiP2pThisDeviceChangedAction(Intent intent) {

        }

        @Override
        public void onConnectedAsServer(Server server) {



        }


    };

}

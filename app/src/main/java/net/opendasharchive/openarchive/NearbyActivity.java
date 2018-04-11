package net.opendasharchive.openarchive;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaDeserializer;
import net.opendasharchive.openarchive.nearby.AyandaClient;
import net.opendasharchive.openarchive.nearby.AyandaServer;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import sintulabs.p2p.Ayanda;
import sintulabs.p2p.ILan;
import sintulabs.p2p.IWifiDirect;
import sintulabs.p2p.NearbyMedia;
import sintulabs.p2p.Neighbor;
import sintulabs.p2p.Server;


public class NearbyActivity extends AppCompatActivity {

    private final static String TAG = "Nearby";

    private TextView mTvNearbyLog;
    private DonutProgress mProgress;
    private LinearLayout mViewNearbyDevices;
    private boolean mIsServer = false;
    private Media mMedia = null;
    private Ayanda mAyanda;
    private HashMap<String,String> mPeers = new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAyanda = new Ayanda(this, null, mNearbyWifiLan, nearbyWifiDirect);

        mTvNearbyLog = findViewById(R.id.tvnearbylog);
        mViewNearbyDevices = findViewById(R.id.nearbydevices);

        mProgress = findViewById(R.id.donut_progress);
        mProgress.setMax(100);


        mIsServer = getIntent().getBooleanExtra("isServer", false);

        if (mIsServer) {
            mProgress.setInnerBottomText(">>>>>>>>");
            try {
                startServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mProgress.setInnerBottomText("<<<<<<<<");

            getSupportActionBar().setTitle("Receiving...");

        }

        askForPermission("android.permission.BLUETOOTH", 1);
        askForPermission("android.permission.BLUETOOTH_ADMIN", 2);
        askForPermission("android.permission.ACCESS_COARSE_LOCATION", 3);
        askForPermission("android.permission.ACCESS_WIFI_STATE", 4);
        askForPermission("android.permission.CHANGE_WIFI_STATE", 5);
        askForPermission("android.permission.ACCESS_NETWORK_STATE", 6);
        askForPermission("android.permission.CHANGE_NETWORK_STATE", 7);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                cancelNearby();
                finish();
                break;
        }

        return true;
    }

    private boolean askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(NearbyActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(NearbyActivity.this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(NearbyActivity.this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(NearbyActivity.this, new String[]{permission}, requestCode);
            }

            return true;
        }

        return false;

    }

    
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

              @Override public void onClick(View v) {

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
              @Override public void onClick(View v) {

              Intent intent = new Intent(Intent.ACTION_VIEW);
              intent.setDataAndType(nearbyMedia.mUriMedia, nearbyMedia.mMimeType);
              startActivity(intent);
              }
              });

              snackbar.show();
              }
      }



    private void restartNearby() {
        mAyanda.wdDiscover();
        mAyanda.lanDiscover();
    }

    private void cancelNearby() {

    }

    private void log(String msg) {
        if (mTvNearbyLog != null)
            mTvNearbyLog.setText(msg);

        Log.d("Nearby", msg);
    }

    @Override
    public void onResume() {
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

    private void startServer() throws IOException {

        NearbyMedia nearbyMedia = new NearbyMedia();

        long currentMediaId = getIntent().getLongExtra(Globals.EXTRA_CURRENT_MEDIA_ID, -1);

        if (currentMediaId >= 0)
            mMedia = Media.findById(Media.class, currentMediaId);

        Uri uriMedia = Uri.parse(mMedia.getOriginalFilePath());
        nearbyMedia.mUriMedia = uriMedia;

        InputStream is = getContentResolver().openInputStream(uriMedia);
        byte[] digest = Utility.getDigest(is);
        nearbyMedia.mDigest = digest;

        String title = mMedia.getTitle();
        if (TextUtils.isEmpty(title))
            title = uriMedia.getLastPathSegment();

        Gson gson = new GsonBuilder()
                .setDateFormat(DateFormat.FULL, DateFormat.FULL).create();
        nearbyMedia.mMetadataJson = gson.toJson(mMedia);

        nearbyMedia.mTitle = title;
        nearbyMedia.mMimeType = mMedia.getMimeType();

        getSupportActionBar().setTitle("Sharing: " + title);

        try {
            int defaultPort = 8080;
            mAyanda.setServer(new AyandaServer(this, defaultPort));

            mAyanda.wdShareFile(nearbyMedia);
            mAyanda.lanShare(nearbyMedia);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void addPeerToView(String peerName) {

        LinearLayout.LayoutParams imParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView tv = new TextView(this);
        tv.setLayoutParams(imParams);
        tv.setText(peerName);

        mViewNearbyDevices.addView(tv, imParams);


    }



    ILan mNearbyWifiLan = new ILan() {

        @Override
        public void deviceListChanged() {

            ArrayList<Ayanda.Device> devices = new ArrayList<Ayanda.Device>(mAyanda.lanGetDeviceList());

            for (Ayanda.Device device: devices)
            {
                if ((!TextUtils.isEmpty(device.getName()))
                        && (!mPeers.containsKey(device.getHost().toString()))) {

                    mPeers.put(device.getHost().toString(), device.getName());
                    addPeerToView("LAN: " + device.getName());
                }
            }

        }

        @Override
        public void transferComplete(Neighbor neighbor, NearbyMedia nearbyMedia) {

        }

        @Override
        public void transferProgress(Neighbor neighbor, File file, String s, String s1, long l, long l1) {

        }

        @Override
        public void serviceRegistered(String s) {

        }

        @Override
        public void serviceResolved(NsdServiceInfo serviceInfo) {


             // Connected to desired service, so now make socket connection to peer
             final Ayanda.Device device = new Ayanda.Device(serviceInfo);
             new Thread(new Runnable() {
                @Override public void run() {
                    AyandaClient client = new AyandaClient(NearbyActivity.this);
                    String host = device.getHost().toString();
                    try {

                    final String response = client.get(host + ":" + Integer.toString(8080));

                    } catch (IOException e) {
                        Log.e(TAG,"error LAN get: " + e);
                    }

                    try {
                        final NearbyMedia nMedia = client.getNearbyMedia(host + ":" + Integer.toString(8080));

                        if (nMedia != null)
                            addMedia(nMedia);

                    } catch (IOException e) {
                        Log.e(TAG,"error LAN get: " + e);
                    }

                }
            }).start();

        }
    };

    IWifiDirect nearbyWifiDirect = new IWifiDirect() {

        @Override
        public void onConnectedAsClient(final InetAddress groupOwnerAddress) {

            mProgress.setInnerBottomText("Connected as client");

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
                        final NearbyMedia nearbyMedia = client.getNearbyMedia(groupOwnerAddress.getHostAddress() + ":" + Integer.toString(8080));

                        if (nearbyMedia != null)
                            addMedia (nearbyMedia);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    /**
                    if (mMedia != null) {

                        NearbyMedia nearbyMedia = new NearbyMedia();
                        client.uploadFile(groupOwnerAddress.getHostAddress() + ":" + Integer.toString(8080), nearbyMedia);
                    }**/
                }
            }).start();
        }

        @Override
        public void wifiP2pStateChangedAction(Intent intent) {

            Log.d(TAG, "wifiP2pStateChangedAction: " + intent.getAction() + ": " + intent.getData());

        }

        @Override
        public void wifiP2pPeersChangedAction() {

            ArrayList<WifiP2pDevice> devices = new ArrayList<WifiP2pDevice>(mAyanda.wdGetDevicesDiscovered());

            for (WifiP2pDevice device: devices)
            {
                if ((!TextUtils.isEmpty(device.deviceName))
                        && (!mPeers.containsKey(device.deviceAddress))) {

                    mPeers.put(device.deviceAddress, device.deviceName);
                    addPeerToView("Wifi: " + device.deviceName);
                }
            }
        }

        @Override
        public void wifiP2pConnectionChangedAction(Intent intent) {
            Log.d(TAG, "wifiP2pConnectionChangedAction: " + intent.getAction() + ": " + intent.getData());

        }

        @Override
        public void wifiP2pThisDeviceChangedAction(Intent intent) {
            Log.d(TAG, "wifiP2pThisDeviceChangedAction: " + intent.getAction() + ": " + intent.getData());

        }

        @Override
        public void onConnectedAsServer(Server server) {

            mProgress.setInnerBottomText("Connected as server: " + server.toString());

        }


    };

}

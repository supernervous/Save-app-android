package net.opendasharchive.openarchive;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;

import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaDeserializer;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Prefs;
import net.opendasharchive.openarchive.util.Utility;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.github.lizhangqu.coreprogress.ProgressUIListener;
import sintulabs.p2p.Ayanda;
import sintulabs.p2p.IBluetooth;
import sintulabs.p2p.ILan;
import sintulabs.p2p.IWifiDirect;
import sintulabs.p2p.NearbyMedia;
import sintulabs.p2p.Neighbor;
import sintulabs.p2p.Server;
import sintulabs.p2p.impl.AyandaActivity;
import sintulabs.p2p.impl.AyandaClient;
import sintulabs.p2p.impl.AyandaListener;
import sintulabs.p2p.impl.AyandaServer;


public class NearbyActivity extends AyandaActivity {


    private Media mMedia = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle("");

        startAyanda();

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


    @Override
    public Ayanda getAyandaInstance(IBluetooth iBluetooth, ILan iLan, IWifiDirect iWifiDirect) {

        Ayanda ayanda = ((OpenArchiveApp)getApplication()).getAyandaInstance(this,
                iBluetooth,
                iLan,
                iWifiDirect);

        return ayanda;


    }

      public synchronized void addMedia (final NearbyMedia nearbyMedia)
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

          //need better way to check original file
          List<Media> results = null;


          if (!TextUtils.isEmpty(media.getServerUrl())) {
              media.status = Media.STATUS_PUBLISHED;
              results = Media.find(Media.class, "SERVER_URL = ?", media.serverUrl);
          }
          else {
              media.status = Media.STATUS_LOCAL;
              results = Media.find(Media.class, "title = ? AND author = ?", media.title,media.author);

          }


          if (results == null || results.isEmpty()) {

              //it is new!
              media.save();

              Snackbar snackbar = Snackbar
              .make(findViewById(R.id.nearbydevices), getString(R.string.action_received) + ": " + media.getTitle(), Snackbar.LENGTH_LONG);

              final Long snackMediaId = media.getId();

                snackbar.setAction(getString(R.string.action_open), new View.OnClickListener() {

                      @Override public void onClick(View v) {

                          Intent reviewMediaIntent = new Intent(NearbyActivity.this, ReviewMediaActivity.class);
                          reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, snackMediaId);
                          reviewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                          startActivity(reviewMediaIntent);
                      }
                  });

              snackbar.show();
          }
          else
          {
              Snackbar snackbar = Snackbar
                      .make(findViewById(R.id.nearbydevices), getString(R.string.action_duplicate) + ": " + media.getTitle(), Snackbar.LENGTH_LONG);

          }

      }


    public void initNearbyMedia () throws IOException
    {

        long currentMediaId = getIntent().getLongExtra(Globals.EXTRA_CURRENT_MEDIA_ID, -1);

        if (currentMediaId >= 0)
            mMedia = Media.findById(Media.class, currentMediaId);
        else
            return;

        mNearbyMedia = new NearbyMedia();

        Uri uriMedia = Uri.parse(mMedia.getOriginalFilePath());
        mNearbyMedia.mUriMedia = uriMedia;

        Cursor returnCursor =
                getContentResolver().query(uriMedia, null, null, null, null);
        /*
         * Get the column indexes of the data in the Cursor,
         * move to the first row in the Cursor, get the data,
         * and display it.
         */
       // int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if (returnCursor.moveToFirst()) {
            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            mNearbyMedia.mLength = returnCursor.getLong(sizeIndex);
        }
        returnCursor.close();

        InputStream is = getContentResolver().openInputStream(uriMedia);
        byte[] digest = Utility.getDigest(is);
        mNearbyMedia.mDigest = digest;

        String title = mMedia.getTitle();
        if (TextUtils.isEmpty(title))
            title = uriMedia.getLastPathSegment();

        mNearbyMedia.mTitle = title;
        mNearbyMedia.mMimeType = mMedia.getMimeType();

        Gson gson = new GsonBuilder()
                .setDateFormat(DateFormat.FULL, DateFormat.FULL).create();
        mNearbyMedia.mMetadataJson = gson.toJson(mMedia);

    }

}

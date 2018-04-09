package net.opendasharchive.openarchive.nearby;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import sintulabs.p2p.Ayanda;
import sintulabs.p2p.Client;
import sintulabs.p2p.IWifiDirect;
import sintulabs.p2p.Server;

/**
 * Created by sabzo on 1/18/18.
 */

public class WifiDirectActivity extends AppCompatActivity {
    private ListView lvDevices;
    private List peers = new ArrayList();
    private List peerNames = new ArrayList();
    private ArrayAdapter<String> peersAdapter = null;

    private Button btnWdAnnounce;
    private Button btnWdDiscover;

    private Ayanda a;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createView();
        setListeners();

        a = new Ayanda(this, null, null, new IWifiDirect() {

            @Override
            public void onConnectedAsClient(final InetAddress inetAddress) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {


                        final String response = inetAddress.toString();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WifiDirectActivity.this, response, Toast.LENGTH_LONG).show();
                            }
                        });

                        // if (nearbyMedia != null) {
                        //    client.uploadFile(groupOwnerAddress.getHostAddress() + ":" + Integer.toString(8080), nearbyMedia);
                        // }
                    }
                }).start();
            }

            @Override
            public void wifiP2pStateChangedAction(Intent intent) {

            }

            @Override
            public void wifiP2pPeersChangedAction() {
                peers.clear();
                // TODO fix error when WiFi off
                peers.addAll(a.wdGetDevicesDiscovered() );
                peerNames.clear();
                for (int i = 0; i < peers.size(); i++) {
                    WifiP2pDevice device = (WifiP2pDevice) peers.get(i);
                    peersAdapter.add(device.deviceName);
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


        });
        a.wdDiscover();

    }

    /**
     * peers.clear();
     // TODO fix error when WiFi off
     peers.addAll(a.wdGetDevicesDiscovered() );
     peerNames.clear();
     for (int i = 0; i < peers.size(); i++) {
     WifiP2pDevice device = (WifiP2pDevice) peers.get(i);
     peersAdapter.add(device.deviceName);
     }
     */
    private void createView() {

    }


    private void setListeners() {

    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        a.wdRegisterReceivers();
    }

    /* unregister the broadcast receiver */

    @Override
    protected void onPause() {
        super.onPause();
        super.onPause();
        a.wdUnregisterReceivers();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
     //   getMenuInflater().inflate(R.menu.menu_wifi_direct, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            /**
            case R.id.miBt:
                startActivity(new Intent(this, BluetoothActivity.class ));
                finish();
                break;
            case R.id.miLan:
                startActivity(new Intent(this, LanActivity.class ));
                finish();
                break;
             **/
        }
        return true;
    }
}

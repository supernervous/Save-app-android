package net.opendasharchive.openarchive.nearby;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.CircleProgress;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.ramimartin.multibluetooth.activity.BluetoothActivity;
import com.ramimartin.multibluetooth.activity.BluetoothFragmentActivity;
import com.ramimartin.multibluetooth.bluetooth.mananger.BluetoothManager;
import com.simonguest.btxfr.ClientThread;
import com.simonguest.btxfr.MessageType;
import com.simonguest.btxfr.ProgressData;
import com.simonguest.btxfr.ServerThread;
import com.simonguest.btxfr.Utils;

import net.opendasharchive.openarchive.Globals;
import net.opendasharchive.openarchive.MainActivity;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ExceptionToResourceMapping;

public class NearbyActivity extends BluetoothFragmentActivity {


    private TextView mTvNearbyLog;
    private DonutProgress mProgress;

    private boolean mIsServer = false;
    private ServerThread serverThread = null;
    private Media mMedia = null;

    private Handler mHandler = new Handler ()
    {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MessageType.READY_FOR_DATA: {
                    log("ready for data");

                    break;
                }

                case MessageType.COULD_NOT_CONNECT: {
                    log("could not connect");

                    break;
                }

                case MessageType.SENDING_DATA: {
                    log("sending data");

                    break;
                }

                case MessageType.DATA_SENT_OK: {
                    log("data sent ok");

                    break;
                }

                case MessageType.DATA_RECEIVED: {
                    log("data received");

                    if (message.obj instanceof File) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile((File) message.obj), "image/jpeg");
                        startActivity(intent);
                    }

                    break;
                }

                case MessageType.DATA_PROGRESS_UPDATE: {
                    log("data progress update");

                    break;
                }

                case MessageType.DIGEST_DID_NOT_MATCH: {
                    log("digest did not match");

                    break;
                }
            }

            if (message.obj != null) {
                if (message.obj instanceof byte[])
                    log(new String((byte[])message.obj));
                else if (message.obj instanceof ProgressData)
                {
                    ProgressData pd = (ProgressData)message.obj;

                    long remaining = pd.totalSize-pd.remainingSize;

                    log ("progress: " + (pd.totalSize-pd.remainingSize) + "/" + pd.totalSize);
                    int perComplete = (int)((((float)remaining)/((float)pd.totalSize))*100f);

                    mProgress.setProgress(perComplete);
                }
                else
                    log(message.obj.toString());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);

        mTvNearbyLog = (TextView)findViewById(R.id.tvnearbylog);
        mProgress = (DonutProgress)findViewById(R.id.donut_progress);
        mProgress.setMax(100);

        EventBus.getDefault().register(this);

        mIsServer = getIntent().getBooleanExtra("isServer",false);


        if (mIsServer)
            startServer();
        else
            startClient();

    }

    private void log (String msg)
    {
        mTvNearbyLog.setText(msg);
        Log.d("Nearby",msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);

        if (serverThread != null)
            serverThread.cancel();

   //     if (clientThread != null)
     //       clientThread.cancel();

        if (mIsServer) {

            disconnectServer();

        }
        else
            disconnectClient();
    }

    private void startServer () {

        long currentMediaId = getIntent().getLongExtra(Globals.EXTRA_CURRENT_MEDIA_ID, -1);

        if (currentMediaId >= 0)
            mMedia = Media.findById(Media.class, currentMediaId);

        setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_3600_SEC);
        selectServerMode();
        if (mBluetoothManager.isDiscoverable())
        {

            onBluetoothStartDiscovery();

        }
        serverThread = new ServerThread(mBluetoothManager.getAdapter(),mHandler,false);
        sendMediaFile();
        serverThread.start();

        /**
        serverThread.setServerListener(new ServerThread.ServerListener() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {



            }
        });*/

    }

    private void sendMediaFile ()
    {
        try
        {
            File fileMedia = new File(mMedia.getOriginalFilePath());

            InputStream is = new FileInputStream(fileMedia);
            byte[] digest = Utils.calculateMD5(fileMedia);
            serverThread.setShareMedia(fileMedia,(int)fileMedia.length(),digest);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void startClient ()
    {
        selectClientMode();
        scanAllBluetoothDevice();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();


    }

    @Override
    public int myNbrClientMax() {
        return 6;
    }

    @Override
    public void onBluetoothDeviceFound(BluetoothDevice device) {

        log("Found device: " + device.getName());

    }

    @Override
    public void onClientConnectionSuccess() {
        log("client connected");
        sendMessage("Helllooooo client!");

    }

    @Override
    public void onClientConnectionFail() {
        log("client failed");

    }

    @Override
    public void onServeurConnectionSuccess() {
        log("server connected");
        sendMessage("Helllooooo server!");

    }

    @Override
    public void onServeurConnectionFail() {
        log("server failed");

    }

    @Override
    public void onBluetoothStartDiscovery() {
        log("Local BT Address: " + mBluetoothManager.getLocalMacAddress());
        log("bluetooth start discovery");

    }

    @Override
    public void onBluetoothCommunicator(String messageReceive) {
        log("BT communication: " + messageReceive);

    }

    @Override
    public void onBluetoothNotAviable() {
        log("BT failed");

    }

    public void onEventMainThread(BluetoothDevice device) {

        if (device != null && device.getName() != null &&
                (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA ||
                        device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA ||
                        device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART)) {
            log("Found device: " + device.getName() + ":" + device.getAddress());

            ClientThread clientThread = new ClientThread(device, mHandler, false);
            clientThread.start();
        }




    }


}

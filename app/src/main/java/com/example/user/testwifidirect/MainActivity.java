package com.example.user.testwifidirect;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public String chosen;
    Button btnOnOff,btnDiscover;
    ListView listView;
    TextView ConnectionStatus;

    WifiManager wifiManager;


    static WifiP2pManager mManager;
    static WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

  //  public static String fromstart;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    static final int MESSAGE_READ = 1;

    static String appName = "12345";
    static int count = 0;

    static boolean connectsa = false;

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialWork();
        disconnect();

        appName = "12345";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);


        }
      //  exqListener();
    }

    private void disconnect() {
        if (mManager != null && mChannel != null){
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener(){

                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null && group.isGroupOwner()){
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener(){

                            @Override
                            public void onSuccess() {
                                Log.d("Removal", "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d("Removal", "removeGroup onFailure -" + reason);


                            }
                        });

                    }
                }
            });
        }
    }

//    private void exqListener() {
//        btnOnOff.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(wifiManager.isWifiEnabled()){
//                    wifiManager.setWifiEnabled(false);
//                    btnOnOff.setText("TURN WIFI ON");
//                }else{
//                    wifiManager.setWifiEnabled(true);
//                    btnOnOff.setText(" TURN WIFI OFF");
//                }
//            }
//        });
//    }

    private void initialWork() {
       btnOnOff=(Button) findViewById(R.id.onOff);
       btnDiscover= (Button) findViewById(R.id.discover);
       listView=(ListView) findViewById(R.id.peerListView);
       ConnectionStatus=(TextView) findViewById(R.id.connectionStatus);
       wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
       
    }

    public void stopService(View view) {
       // stopService(new Intent(MainActivity.this, WiFiService.class));
       // stopService(new Intent(MainActivity.this, WiFiClient.class));
        stopService(new Intent(MainActivity.this, WiFiTwoWay.class));


    }
    public void twoWayComm(View view) {
        chosen = "TwoWayComm";
      //  Intent intent = new Intent(this, WiFiTwoWay.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(new Intent(getApplicationContext(), WiFiTwoWay.class));
        } else {
            getApplicationContext().startService(new Intent(getApplicationContext(), WiFiTwoWay.class));

        }
    }
}

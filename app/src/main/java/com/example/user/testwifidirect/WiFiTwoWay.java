package com.example.user.testwifidirect;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.rvalerio.fgchecker.AppChecker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WiFiTwoWay  extends Service {

    WifiManager wifiManager;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;



    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    static final int MESSAGE_READ =1;

    static int counter = 0;

    static boolean disconnect = false;

    static int ctne;
    static String appName = "12345";
    static int count = 0;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    static boolean connectedpeer = false;
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;
    public static final String CHANNEL_1_ID = "channel1";
    public static final String CHANNEL_2_ID = "channel2";
    public static final String CHANNEL_3_ID = "channel3";

    static PendingIntent pendingIntent;

    private List<String> listPackageName = new ArrayList<>();
    private List<String> listAppName = new ArrayList<>();
    private Timer timer = new Timer();
    private android.os.Handler handler = new android.os.Handler();

    private AppChecker appChecker;
    String current = "NULL";
    String previous = "NULL";
    String timeleft = "NULL";

    long startTime = 0;
    long previousStartTime = 0;
    long endTime = 0;
    long totlaTime = 0;






    private NotificationManagerCompat notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent5 = new Intent(this, MainActivity.class);
        intent5.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        pendingIntent = PendingIntent.getActivity(this, 0, intent5, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("use", "new ");
            startMyOwnForeground();

        } else {
            Log.d("use", "old");
            startForeground(1, new Notification());

        }
        Log.d("fafa", "WifiTwoWay made");

        initialWork();
        disconnect();
        registerReceiver(mReceiver, mIntentFilter);
        appName = "12345";
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        exqListener();
        installedapp();
        final Timer timer = new Timer();
        timer.schedule(new TimerTask(){
            public void run(){
                retrieveStats();
                aggregationapp();

            }
        },0,1000);


    }

    private void startMyOwnForeground() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.noti)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);

        }
    }

    private void createNotification(String chanid, String channame, String title, String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            String NOTIFICATION_CHANNEL_ID = chanid;
            String channelName = channame;
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.noti)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setTimeoutAfter(5000)
                    .build();
            manager.notify(1,notification);

        }
    }


    @Override
    public void onDestroy(){
        Log.d("Tis", "WE have destroyed service");
        super.onDestroy();
        mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener(){

            @Override
            public void onSuccess() {
                Log.d("cancel", "Success");
                unregisterReceiver(mReceiver);

            }

            @Override
            public void onFailure(int reason) {
                Log.d("cancel", "Failure");


            }
        });
        System.exit(0);
    }

    public void disconnect(){
        Log.d("Discone", "In the dicsone");
        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener(){

                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null
                            && group.isGroupOwner()){
                        Log.d("Removal","Removing group");
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener(){

                            @Override
                            public void onSuccess() {
                                Log.d("Removal", "removeGroup onSuccess -");

                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d("Removal", "removeGroup onFailure -" + reason);
                                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();

                            }
                        });
                    }
                }
            });

        }}

    private void exqListener(){
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener(){

            @Override
            public void onSuccess() {
                Log.d("conn", "disc");
                createNotification(CHANNEL_1_ID,"Discovery","Discovering Peers","Tap to reopen App");

            }

            @Override
            public void onFailure(int reason) {
                Log.d("conn", "failed");
                createNotification(CHANNEL_1_ID,"Discovery","Failed to Discover Peers", "Tap to reopen App");

            }
        });
    }
    private void retrieveStats(){


    }

    private void initialWork(){
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver = new WiFiDirectBroadcastTwoWay(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {

        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            Log.d("Peers", "Getting Peers");
            if (count != 1) {
                if (!peerList.getDeviceList().equals(peers)) {
                    peers.clear();
                    peers.addAll(peerList.getDeviceList());


                    deviceNameArray = new String[peerList.getDeviceList().size()];

                }
                if (peerList.getDeviceList().size() != 0) {

                    deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                    int index = 0;


                    for (WifiP2pDevice devices : peerList.getDeviceList()) {
                        deviceNameArray[0] = devices.deviceName;
                        deviceArray[0] = devices;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                    Log.d("randmpm", deviceArray[0].toString());
                    final WifiP2pDevice device = deviceArray[0];
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = device.deviceAddress;
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            Log.d("Peers", "Connected");
                            createNotification(CHANNEL_1_ID, "Connection", " Succesfully Connected", "Tap to reopen App");
                            count++;
                            connectedpeer = true;

                        }

                        @Override
                        public void onFailure(int reason) {

                            Log.d("Peers", "Not Connected");
                            createNotification(CHANNEL_2_ID, "Connection", "Failed to Connect", "Tap to reopen App");
                            connectedpeer = false;
                            Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();


                        }
                    });

                }
            }

            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();

            }
        }


    };
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener(){

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            Log.d("here","gege");
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;
            if(info.groupFormed && info.isGroupOwner){
                serverClass = new ServerClass();
                serverClass.start();

            }else if(info.groupFormed){
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();

            }

        }
    };

    public class ServerClass extends Thread{
        ServerSocket serverSocket;
        Socket socket;
        @Override
        public void run(){
            try{
                Log.d("Tis", "This server class started");
                serverSocket = new ServerSocket(1239);
                socket = serverSocket.accept();
                sendReceive = new SendReceive(socket);
                sendReceive.start();
                final Timer timer = new Timer();
                timer.schedule(new TimerTask(){
                    public void run(){
                        String msg = appName;
                        //   sendReceive.write(msg.getBytes());

                    }

                }, 0, 1000);
            }catch (IOException e){
                e.printStackTrace();

            }
        }

    }

    public class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt){
            Log.d("Tis", "This server receive started");

            socket = skt;
            try{
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

            }catch(IOException e){
                e.printStackTrace();
            }

        }
        @Override
        public void run(){
            Log.d("Tis", "This server run started");

            byte[] buffer = new byte[1024];
            int bytes;

            while (socket != null){
                try{
                    counter = 0;
                    bytes = inputStream.read(buffer);
                    if(bytes > 0){
                     //   handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
//    public void write(byte[] bytes){
//        try{
//            Log.d("Send","Sending stuff");
//            outputStream.write(bytes);
//        }catch (IOException e){
//            e.printStackTrace();
//            disconnect();//need too
//        }
//    }

    public class ClientClass extends Thread{
        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress){
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }
        @Override
        public void run(){
            try{
                socket.connect(new InetSocketAddress(hostAdd,1239),500);
                sendReceive = new SendReceive(socket);
                sendReceive.start();
                final Timer timer = new Timer();
                timer.schedule(new TimerTask(){

                    @Override
                    public void run() {
                        String msg = appName;
                        //    sendReceive.write(msg.getBytes());

                    }
                }, 0, 1000);

            } catch (IOException e) {
                e.printStackTrace();
                disconnect();//need to
            }
        }
    }

    public void installedapp() {
        List<PackageInfo> packageList = getPackageManager().getInstalledPackages(0);
        //  List<ApplicationInfo> applications = getPackageManager().getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        // Log.d("pkg inofo->", appInfo.packageName);
        for (int i = 0; i < packageList.size(); i++) {
            PackageInfo packageInfo = packageList.get(i);

            String appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
            String pacName = packageInfo.packageName;

            listAppName.add(appName);
            listPackageName.add(pacName);


            Log.e("APPNAME", "app is " + appName + "----" + pacName + "\n");

            String app = appName + "\t" + pacName + "\t" + "\n";


            try {
                File data3 = new File("appname.txt");
                FileOutputStream fos = openFileOutput("appname.txt", Context.MODE_APPEND);
                fos.write((app).getBytes());
                fos.close();
//                FileWriter fw =new FileWriter("appname.txt", false);
//                fw.write(app);
//                fw.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    public void aggregationapp() {
        String lastknown = "NULL";
        String appName = "NULL";
        String previous1 = "NULL";
//        UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
//        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        Date systemDate = Calendar.getInstance().getTime();
        String myDate = sdf.format(systemDate);
//        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
//        if (appList != null && appList.size() > 0) {
//            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
//            for (UsageStats usageStats : appList) {
//                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
//            }
//            if (mySortedMap != null && !mySortedMap.isEmpty()) {
//                String dateFormat = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
//                current = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
//
//                //  lastknown = String.valueOf(new Date(mySortedMap.get(mySortedMap.lastKey()).getLastTimeUsed()));
//                //  int index = listPackageName.indexOf(previous);
//                // appName = listAppName.get(index);
        AppChecker appChecker = new AppChecker();
        current = appChecker.getForegroundApp(getBaseContext());
        java.text.DateFormat df = new java.text.SimpleDateFormat("hh:mm:ss");
        {
            if (!current.equals(previous)) {
                Log.d("panda", "zebra" + previous);
                Log.d("side", "dish" + current);
                Log.d("tims", "Horton" + myDate);
//
//
                startTime = System.currentTimeMillis();
//                        previous = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                previous = appChecker.getForegroundApp(getBaseContext());
//
                int index = listPackageName.indexOf(previous);
                appName = listAppName.get(index);
//
//
                if (startTime != previousStartTime) {
                    totlaTime = startTime - previousStartTime;
//
                }
//
                Log.d("AppInfo", "app name " + previous + " App time" + totlaTime);
//
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                            // TODO: Consider calling
//                            //    ActivityCompat#requestPermissions
//                            // here to request the missing permissions, and then overriding
//                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                            //                                          int[] grantResults)
//                            // to handle the case where the user grants the permission. See the documentation
//                            // for ActivityCompat#requestPermissions for more details.
//                            return;
//                        }
//
//                        // Added to chcke if the phone is locked vs unlocked//
//
                String status = "NULL";
                KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (myKM.inKeyguardRestrictedInputMode()) {
                    status = "locked";
                } else {
                    status = "unlocked";
                }
                @SuppressLint("MissingPermission") Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    double longitude = location.getLongitude();
                    double latitude = location.getLatitude();
                    String date = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
                    String appt = date + "\t" + latitude + "\t" + longitude + "\t" + previous + "\t" + appName + "\t" + totlaTime + "\t" + status + "\n";
                    try {
                        File data7 = new File("individual.txt");
                        FileOutputStream fos = openFileOutput("individual.txt", Context.MODE_APPEND);
                        fos.write((appt).getBytes());
                        fos.close();
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
//
                    previousStartTime = startTime;
                }
            } else if (current.equals(previous)) {
//
//
//                        //endTime = startTime;
//
//                        lastknown = String.valueOf(new Date(mySortedMap.get(mySortedMap.lastKey()).getLastTimeUsed()));
                Log.d("Birds", "crow" + lastknown);
            }
            previous = current;

            Log.d("zoo", "animals" + previous);
//
//
        }


//
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

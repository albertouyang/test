package com.example.usbeacon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;

import com.THLight.USBeacon.App.Lib.BatteryPowerData;
import com.THLight.USBeacon.App.Lib.USBeaconConnection;
import com.THLight.USBeacon.App.Lib.USBeaconData;
import com.THLight.USBeacon.App.Lib.USBeaconList;
import com.THLight.USBeacon.App.Lib.USBeaconServerInfo;
import com.THLight.USBeacon.App.Lib.iBeaconData;
import com.THLight.USBeacon.App.Lib.iBeaconScanManager;
import com.THLight.Util.THLLog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.File;


public class MainActivity extends AppCompatActivity implements iBeaconScanManager.OniBeaconScan, USBeaconConnection.OnResponse {

    /** this UUID is generate by Server while register a new account. */
    final UUID QUERY_UUID		= UUID.fromString("7B80C9E2-F69D-4972-8EBC-6370AB9C1576");
    /** server http api url. */
    final String HTTP_API		= "http://www.usbeacon.com.tw/api/func";

    static String STORE_PATH	= Environment.getExternalStorageDirectory().toString()+ "/USBeaconSample/";
//    static String STORE_PATH	= Environment.getExternalStorageDirectory().toString();
//    static String filename      = "USBeaconSample";

    final int REQ_ENABLE_BT		        = 2000;
    final int REQ_ENABLE_WIFI	        = 2001;

    final int MSG_SCAN_IBEACON			= 1000;
    final int MSG_UPDATE_BEACON_LIST	= 1001;
    final int MSG_START_SCAN_BEACON	    = 2000;
    final int MSG_STOP_SCAN_BEACON		= 2001;
    final int MSG_SERVER_RESPONSE		= 3000;

    final int TIME_BEACON_TIMEOUT		= 5000;//原本30000

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    THLApp App		= null;
    THLConfig Config= null;


    BluetoothAdapter mBLEAdapter= BluetoothAdapter.getDefaultAdapter();

    /** scaner for scanning iBeacon around. */
    iBeaconScanManager miScaner	= null;

    /** USBeacon server. */
    USBeaconConnection mBServer	= new USBeaconConnection();

    USBeaconList mUSBList		= null;

    ListView mLVBLE = null;

    WebView webView;

//    BLEListAdapter mListAdapter		= null;

    TextView textView;

//    ActionBar actionBar;

    Button now;

    Button toLottery;

    List<ScanediBeacon> miBeacons	= new ArrayList<ScanediBeacon>();


    /** ================================================ */
    Handler mHandler= new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            Log.i("start", String.valueOf(msg.what));
            switch(msg.what)
            {
                case MSG_SCAN_IBEACON:
                {
                    int timeForScaning		= msg.arg1;
                    int nextTimeStartScan	= msg.arg2;

                    miScaner.startScaniBeacon(timeForScaning);
                    this.sendMessageDelayed(Message.obtain(msg), nextTimeStartScan);
                }
                break;

                case MSG_UPDATE_BEACON_LIST:
                    //                   synchronized(mListAdapter)
                    //                   {
                    verifyiBeacons();
                    //                       mListAdapter.notifyDataSetChanged();
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);
                    //                   }
                    break;

                case MSG_SERVER_RESPONSE:
                    switch(msg.arg1)
                    {
                        case USBeaconConnection.MSG_NETWORK_NOT_AVAILABLE:
                            break;

                        case USBeaconConnection.MSG_HAS_UPDATE:
                            mBServer.downloadBeaconListFile();
                            Toast.makeText(MainActivity.this, "HAS_UPDATE.", Toast.LENGTH_SHORT).show();
                            break;

                        case USBeaconConnection.MSG_HAS_NO_UPDATE:
                            Toast.makeText(MainActivity.this, "No new BeaconList.", Toast.LENGTH_SHORT).show();
                            break;

                        case USBeaconConnection.MSG_DOWNLOAD_FINISHED:
                            break;

                        case USBeaconConnection.MSG_DOWNLOAD_FAILED:
                            Toast.makeText(MainActivity.this, "Download file failed!", Toast.LENGTH_SHORT).show();
                            break;

                        case USBeaconConnection.MSG_DATA_UPDATE_FINISHED:
                        {
                            USBeaconList BList= mBServer.getUSBeaconList();

                            if(null == BList)
                            {
                                Toast.makeText(MainActivity.this, "Data Updated failed.", Toast.LENGTH_SHORT).show();
                                THLLog.d("debug", "update failed.");
                            }
                            else if(BList.getList().isEmpty())
                            {
                                Toast.makeText(MainActivity.this, "Data Updated but empty.", Toast.LENGTH_SHORT).show();
                                THLLog.d("debug", "this account doesn't contain any devices.");
                            }
                            else
                            {
                                Toast.makeText(MainActivity.this, "Data Updated("+ BList.getList().size()+ ")", Toast.LENGTH_SHORT).show();

                                for(USBeaconData data : BList.getList())
                                {
                                    THLLog.d("debug", "Name("+ data.name+ "), Ver("+ data.major+ "."+ data.minor+ ")");
                                }
                            }
                        }
                        break;

                        case USBeaconConnection.MSG_DATA_UPDATE_FAILED:
                            Toast.makeText(MainActivity.this, "UPDATE_FAILED!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
            }
        }
    };

    int[] D = new int[1000];


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_EXTERNAL_STORAGE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    createFolder();
                    checkNetwork();
//                    sentmHandler();
                }
                break;
            case PERMISSION_REQUEST_COARSE_LOCATION:

                    break;
        }
    }

    /** ================================================ */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("main", "start");

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.hide();


//        Toast.makeText(this, "yo", Toast.LENGTH_SHORT).show();
//        Intent intent1 = new Intent(MainActivity.this, Main4Activity.class);
//        startActivity(intent1);
//        finish();

        /*webview*/
//        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//        RelativeLayout parent = (RelativeLayout) inflater.inflate(R.layout.activity_main, null);
        webView = (WebView)findViewById(R.id.webview);
//        WebView webView = (WebView)findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
//        parent.removeView(webView);
//        setContentView(webView);
//        webView.setWebViewClient(new WebViewClient(){
//            @Override
//            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//                super.onReceivedSslError(view, handler, error);
//                handler.proceed();
//            }
//        });
        webView.setVerticalScrollbarOverlay(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAppCacheEnabled(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUserAgentString("User-Agent:Android");
        webView.loadUrl("http://jiheng.eu.org/imarket/");
//        webView.loadUrl("https://stu-web.tkucs.cc/404412024/test/index.html");
//        setContentView(parent);
        webView.addJavascriptInterface(new JsInterface(this), "AndroidWebView");
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        textView = (TextView)findViewById(R.id.textView);

        App		= THLApp.getApp();
        Config	= THLApp.Config;

        /** create instance of iBeaconScanManager. */
        miScaner = new iBeaconScanManager(this, this);

//        mListAdapter = new BLEListAdapter(this);

//        mLVBLE = (ListView)findViewById(R.id.beacon_list);
//        mLVBLE.setAdapter(mListAdapter);

        /*if bluetooth does not work, open bluetooth*/
        if(!mBLEAdapter.isEnabled())
        {
            Intent intent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE_BT);
        }
        else
        {
            Message msg= Message.obtain(mHandler, MSG_SCAN_IBEACON, 1000, 1100);
            msg.sendToTarget();
        }

        /*initialize beacon distance array*/
        for(int i = 0; i < 1000; i++) {
            D[i] = -10000;
        }

        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }

//        /** create store folder. */
//        File f= new File(STORE_PATH);
//        if(!f.exists())
//        {
//            if(!f.mkdirs())
//            {
//                Toast.makeText(this, "Create folder("+ STORE_PATH+ ") failed.", Toast.LENGTH_SHORT).show();
//            }
//        }
//
//        /** check network is available or not. */
//        ConnectivityManager cm	= (ConnectivityManager)getSystemService(MainActivity.CONNECTIVITY_SERVICE);
//        if(null != cm)
//        {
//            NetworkInfo ni = cm.getActiveNetworkInfo();
//            if(null == ni || (!ni.isConnected()))
//            {
//                dlgNetworkNotAvailable();
//            }
//            else
//            {
//                THLLog.d("debug", "NI not null");
//
//                NetworkInfo niMobile= cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
//                if(null != niMobile)
//                {
//                    boolean is3g	= niMobile.isConnectedOrConnecting();
//
//                    if(is3g)
//                    {
//                        dlgNetwork3G();
//                    }
//                    else
//                    {
//                        USBeaconServerInfo info= new USBeaconServerInfo();
//
//                        info.serverUrl		= HTTP_API;
//                        info.queryUuid		= QUERY_UUID;
//                        info.downloadPath	= STORE_PATH;
//
//                        mBServer.setServerInfo(info, this);
//                        mBServer.checkForUpdates();
//                    }
//                }
//            }
//        }
//        else
//        {
//            THLLog.d("debug", "CM null");
//        }

        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);

        now = (Button)findViewById(R.id.now);
        now.setOnClickListener(Now);
        toLottery = (Button)findViewById(R.id.toLottery);
        toLottery.setOnClickListener(ToLottery);

    }

    /** ================================================ */
    @Override
    public void onResume()
    {
        super.onResume();
    }

    /** ================================================ */
    @Override
    public void onPause()
    {
        super.onPause();
    }

    /** ================================================ */
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }

    /** ================================================ */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        THLLog.d("DEBUG", "onActivityResult()");

        switch(requestCode)
        {
            case REQ_ENABLE_BT:
                if(RESULT_OK == resultCode)
                {
                }
                break;

            case REQ_ENABLE_WIFI:
                if(RESULT_OK == resultCode)
                {
                }
                break;
        }
    }

    /** ================================================ */
    /* implementation of {@link iBeaconScanManager#OniBeaconScan } */
    @Override
    public void onScaned(iBeaconData iBeacon)
    {
        Log.i("start", "onScaned");
        addOrUpdateiBeacon(iBeacon);
//        synchronized(mListAdapter)
//        {
//            addOrUpdateiBeacon(iBeacon);
//        }
    }
    /** ================================================ */
    /* implementation of {@link iBeaconScanManager#OniBeaconScan } */
    @Override
    public void onBatteryPowerScaned(BatteryPowerData batteryPowerData) {
        // TODO Auto-generated method stub
        Log.d("debug", batteryPowerData.batteryPower+"");
        for(int i = 0 ; i < miBeacons.size() ; i++)
        {
            if(miBeacons.get(i).macAddress.equals(batteryPowerData.macAddress))
            {
                ScanediBeacon ib = miBeacons.get(i);
                ib.batteryPower = batteryPowerData.batteryPower;
                miBeacons.set(i, ib);
            }
        }
    }

    /** ========================================================== */
    public void onResponse(int msg)
    {
        THLLog.d("debug", "Response("+ msg+ ")");
        mHandler.obtainMessage(MSG_SERVER_RESPONSE, msg, 0).sendToTarget();
    }

    /** ========================================================== */
    public void dlgNetworkNotAvailable()
    {
        final AlertDialog dlg = new AlertDialog.Builder(MainActivity.this).create();

        dlg.setTitle("Network");
        dlg.setMessage("本程式需要使用網路，請將網路或wifi打開!!!");

        dlg.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dlg.dismiss();
            }
        });

        dlg.show();
    }

    /** ========================================================== */
    public void dlgNetwork3G()
    {
        final AlertDialog dlg = new AlertDialog.Builder(MainActivity.this).create();

        dlg.setTitle("3G");
        dlg.setMessage("目前是使用3G/4G網路，可能會導致額外花費!!!");

        dlg.setButton(AlertDialog.BUTTON_POSITIVE, "Allow", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                Config.allow3G= true;
                dlg.dismiss();
                USBeaconServerInfo info= new USBeaconServerInfo();

                info.serverUrl		= HTTP_API;
                info.queryUuid		= QUERY_UUID;
                info.downloadPath	= STORE_PATH;

                mBServer.setServerInfo(info, MainActivity.this);
                mBServer.checkForUpdates();
            }
        });

        dlg.setButton(AlertDialog.BUTTON_NEGATIVE, "Reject", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                Config.allow3G= false;
                dlg.dismiss();
            }
        });

        dlg.show();
    }

    /** ========================================================== */
    public void addOrUpdateiBeacon(iBeaconData iBeacon)
    {
        Log.i("Scaned", "addOrUpdateiBeacon");
        long currTime= System.currentTimeMillis();

        ScanediBeacon beacon= null;

        for(ScanediBeacon b : miBeacons)
        {
            if(b.equals(iBeacon, false))
            {
                beacon= b;
                break;
            }
        }

        if(null == beacon)
        {
            beacon= ScanediBeacon.copyOf(iBeacon);
            D[beacon.minor] = beacon.rssi;
            miBeacons.add(beacon);
            Log.i("show", "D[beacon.minor]");
        }
        else
        {
            beacon.rssi= iBeacon.rssi;
        }

        beacon.lastUpdate= currTime;
    }

    /** ========================================================== */
    public void verifyiBeacons()
    {
        Log.i("start", "verifyiBeacons");
        {
            long currTime	= System.currentTimeMillis();

            int len= miBeacons.size();
            ScanediBeacon beacon= null;

            for(int i= len- 1; 0 <= i; i--)
            {
                beacon= miBeacons.get(i);

                if(null != beacon && TIME_BEACON_TIMEOUT < (currTime- beacon.lastUpdate))
                {
                    miBeacons.remove(i);
                    D[beacon.minor] = -10000;
                }
            }
        }

        {
//            mListAdapter.clear();

            for(ScanediBeacon beacon : miBeacons)
            {
                //mListAdapter.addItem(new ListItem(beacon.beaconUuid.toString().toUpperCase(), ""+ beacon.major, ""+ beacon.minor, ""+ beacon.rssi,""+beacon.batteryPower));
                D[beacon.minor] = beacon.rssi;
                textView.setText(String.valueOf(beacon.minor));
//                textView.setText(D[1] + ":" + String.valueOf(D[1])+"\n"+"D[2]" + ":" + String.valueOf(D[2]));
//                Toast.makeText(MainActivity.this, String.valueOf(D[beacon.minor]), Toast.LENGTH_SHORT).show();
                chose();
            }
        }
    }

    /** ========================================================== */
    public void createFolder(){
        /** create store folder. */
        File f= new File(STORE_PATH);
        if(!f.exists())
        {
            if(!f.mkdirs())
            {
                Toast.makeText(this, "Create folder("+ STORE_PATH+ ") failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void checkNetwork(){
        /** check network is available or not. */
        ConnectivityManager cm	= (ConnectivityManager)getSystemService(MainActivity.CONNECTIVITY_SERVICE);
        if(null != cm)
        {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if(null == ni || (!ni.isConnected()))
            {
                dlgNetworkNotAvailable();
            }
            else
            {
                THLLog.d("debug", "NI not null");

                NetworkInfo niMobile= cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if(null != niMobile)
                {
                    boolean is3g	= niMobile.isConnectedOrConnecting();

                    if(is3g)
                    {
                        dlgNetwork3G();
                    }
                    else
                    {
                        USBeaconServerInfo info= new USBeaconServerInfo();

                        info.serverUrl		= HTTP_API;
                        info.queryUuid		= QUERY_UUID;
                        info.downloadPath	= STORE_PATH;

                        mBServer.setServerInfo(info, this);
                        mBServer.checkForUpdates();
                    }
                }
            }
        }
        else
        {
            THLLog.d("debug", "CM null");
        }
    }

    public void sentmHandler(){
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);
    }

//    public void cleariBeacons()
//    {
//        mListAdapter.clear();
//    }

    private class JsInterface {
        private Context mContext;

        public JsInterface(Context context) {
            this.mContext = context;
        }

        //在js中调用window.AndroidWebView.showInfoFromJs(name)，便会触发此方法。
        @JavascriptInterface
        public void showInfoFromJs(String name) {
            Toast.makeText(mContext, name, Toast.LENGTH_SHORT).show();
        }
    }

    public void chose(){
        int i;
        int nowBeacon = 999;
        for(i = 0; i < 999; i++){
            if(D[nowBeacon] < D[i + 1])   nowBeacon = i + 1;
        }
//        for(i = 0; i < 99; i++){
//            if((D[k] < D[i + 1]) && ((i + 1) != j))   k = i + 1;
//            if(E[k] < E[i+1])   k = i + 1;
//            if(k == j){
//                E[k] = -10000;
//
//            }
//        }
//        textView.setText(String.valueOf(j)+ " :" +String.valueOf(D[j]));
        //Toast.makeText(MainActivity.this, String.valueOf(j)+ " :" +String.valueOf(D[j]), Toast.LENGTH_SHORT).show();
        Log.i("show", String.valueOf(nowBeacon)+ " :" +String.valueOf(D[nowBeacon]));
//        webView.loadUrl("javascript:showInfoFromJava(\"" + 0 + "\")");
        webView.loadUrl("javascript:showInfoFromJava(\"" + nowBeacon + "\")");
    }


//    //在java中调用js代码
//    public void sendInfoToJs(View view) {
//        String msg = ;
//        //调用js中的函数：showInfoFromJava(msg)
//        webView.loadUrl("javascript:showInfoFromJava('" + msg + "')");
//    }

    public View.OnClickListener Now = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            chose();
        }
    };

    public View.OnClickListener ToLottery = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int inMarket = 0;
            //檢查人是否在商場內
            for(int i = 0; i < 1000; i++){
                if(D[i] != -10000){
                    inMarket++;
                }
            }
            if (inMarket == 0){
                Toast.makeText(MainActivity.this, "您不在賣場內或不符合抽獎資格!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, lottery.class);
                startActivity(intent);
            }else{
                Toast.makeText(MainActivity.this, String.valueOf(inMarket), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MainActivity.this, lottery.class);
                startActivity(intent);
            }
        }
    };
}


/** ============================================================== */
//class ListItem
//{
//    public String text1= "";
//    public String text2= "";
//    public String text3= "";
//    public String text4= "";
//    public String text5= "";
//
//    public ListItem()
//    {
//    }
//
//    public ListItem(String text1, String text2, String text3, String text4, String text5)
//    {
//        this.text1= text1;
//        this.text2= text2;
//        this.text3= text3;
//        this.text4= text4;
//        this.text5= text5;
//    }
//}


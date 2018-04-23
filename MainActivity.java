/** ============================================================== */
package com.example.testusbeacon;
/** ============================================================== */
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.THLight.USBeacon.App.Lib.BatteryPowerData;
import com.THLight.USBeacon.App.Lib.USBeaconConnection;
import com.THLight.USBeacon.App.Lib.USBeaconData;
import com.THLight.USBeacon.App.Lib.USBeaconList;
import com.THLight.USBeacon.App.Lib.USBeaconServerInfo;
import com.THLight.USBeacon.App.Lib.iBeaconData;
import com.THLight.USBeacon.App.Lib.iBeaconScanManager;
//import com.example.testusbeacon.R;
//import com.example.testusbeacon.ScanediBeacon;
//import com.example.testusbeacon.THLApp;
//import com.example.testusbeacon.THLConfig;
import com.THLight.Util.THLLog;

/** ============================================================== */
public class MainActivity extends Activity implements iBeaconScanManager.OniBeaconScan, USBeaconConnection.OnResponse
{
    /** this UUID is generate by Server while register a new account. */
    final UUID QUERY_UUID		= UUID.fromString("7B80C9E2-F69D-4972-8EBC-6370AB9C1576");
    /** server http api url. */
    final String HTTP_API		= "http://www.usbeacon.com.tw/api/func";

    static String STORE_PATH	= Environment.getExternalStorageDirectory().toString()+ "/USBeaconSample/";

    final int REQ_ENABLE_BT		= 2000;
    final int REQ_ENABLE_WIFI	= 2001;

    final int MSG_SCAN_IBEACON			= 1000;
    final int MSG_UPDATE_BEACON_LIST	= 1001;
    final int MSG_START_SCAN_BEACON		= 2000;
    final int MSG_STOP_SCAN_BEACON		= 2001;
    final int MSG_SERVER_RESPONSE		= 3000;

    final int TIME_BEACON_TIMEOUT		= 30000;

    THLApp App		= null;
    THLConfig Config= null;

    BluetoothAdapter mBLEAdapter= BluetoothAdapter.getDefaultAdapter();

    /** scaner for scanning iBeacon around. */
    iBeaconScanManager miScaner	= null;

    /** USBeacon server. */
    USBeaconConnection mBServer	= new USBeaconConnection();

    USBeaconList mUSBList		= null;

    ListView mLVBLE= null;

    BLEListAdapter mListAdapter		= null;

    List<ScanediBeacon> miBeacons	= new ArrayList<ScanediBeacon>();

    /** ================================================ */
    Handler mHandler= new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
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
                    synchronized(mListAdapter)
                    {
                        verifyiBeacons();
                        mListAdapter.notifyDataSetChanged();
                        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);
                    }
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

    /** ================================================ */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        App		= THLApp.getApp();
        Config	= THLApp.Config;

        /** create instance of iBeaconScanManager. */
        miScaner		= new iBeaconScanManager(this, this);

        mListAdapter	= new BLEListAdapter(this);

        mLVBLE			= (ListView)findViewById(R.id.beacon_list);
        mLVBLE.setAdapter(mListAdapter);

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

        /** create store folder. */
        File file= new File(STORE_PATH);
        if(!file.exists())
        {
            if(!file.mkdirs())
            {
                Toast.makeText(this, "Create folder("+ STORE_PATH+ ") failed.", Toast.LENGTH_SHORT).show();
            }
        }

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

        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);
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
//    /** implementation of {@link iBeaconScanManager#OniBeaconScan } */
    @Override
    public void onScaned(iBeaconData iBeacon)
    {
        synchronized(mListAdapter)
        {
            addOrUpdateiBeacon(iBeacon);
        }
    }
    /** ================================================ */
//    /** implementation of {@link iBeaconScanManager#OniBeaconScan } */
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
        dlg.setMessage("Please enable your network for updating beacon list.");

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
        dlg.setMessage("App will send/recv data via 3G, this may result in significant data charges.");

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
        Toast.makeText(this, "addorUpdateiBeacon", Toast.LENGTH_SHORT).show();

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
            miBeacons.add(beacon);
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
        Toast.makeText(this, "verifyiBeacons", Toast.LENGTH_SHORT).show();

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
                }
            }
        }

        {
            mListAdapter.clear();

            for(ScanediBeacon beacon : miBeacons)
            {
                Toast.makeText(this, "have scaned beacon!", Toast.LENGTH_SHORT).show();
                mListAdapter.addItem(new ListItem(beacon.beaconUuid.toString().toUpperCase(), ""+ beacon.major, ""+ beacon.minor, ""+ beacon.rssi,""+beacon.batteryPower));
            }
        }
    }

    /** ========================================================== */
    public void cleariBeacons()
    {
        mListAdapter.clear();
    }

}

/** ============================================================== */
class ListItem
{
    public String text1= "";
    public String text2= "";
    public String text3= "";
    public String text4= "";
    public String text5= "";

    public ListItem()
    {
    }

    public ListItem(String text1, String text2, String text3, String text4, String text5)
    {
        this.text1= text1;
        this.text2= text2;
        this.text3= text3;
        this.text4= text4;
        this.text5= text5;
    }
}

/** ============================================================== */
class BLEListAdapter extends BaseAdapter
{
    private Context mContext;

    List<ListItem> mListItems= new ArrayList<ListItem>();

    /** ================================================ */
    public BLEListAdapter(Context c) { mContext= c; }

    /** ================================================ */
    public int getCount() { return mListItems.size(); }

    /** ================================================ */
    public Object getItem(int position)
    {
        if((!mListItems.isEmpty()) && mListItems.size() > position)
        {
            return mListItems.toArray()[position];
        }

        return null;
    }

    public String getItemText(int position)
    {
        if((!mListItems.isEmpty()) && mListItems.size() > position)
        {
            return ((ListItem)mListItems.toArray()[position]).text1;
        }

        return null;
    }

    /** ================================================ */
    public long getItemId(int position) { return 0; }

    /** ================================================ */
    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view= (View)convertView;

        if(null == view)
            view= View.inflate(mContext, R.layout.item_text_3, null);

        // view.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

        if((!mListItems.isEmpty()) && mListItems.size() > position)
        {
            TextView text1	= (TextView)view.findViewById(R.id.it3_text1);
            TextView text2	= (TextView)view.findViewById(R.id.it3_text2);
            TextView text3	= (TextView)view.findViewById(R.id.it3_text3);
            TextView text4	= (TextView)view.findViewById(R.id.it3_text4);
            TextView text5	= (TextView)view.findViewById(R.id.it3_text5);

            ListItem item= (ListItem)mListItems.toArray()[position];

            text1.setText(item.text1);
            text2.setText(item.text2);
            text3.setText(item.text3);
            text4.setText(item.text4+ " dbm");
            text5.setText(item.text5+ " V");
        }
        else
        {
            view.setVisibility(View.GONE);
        }

        return view;
    }

    /** ================================================ */
    @Override
    public boolean isEnabled(int position)
    {
        if(mListItems.size() <= position)
            return false;

        return true;
    }

    /** ================================================ */
    public boolean addItem(ListItem item)
    {
        mListItems.add(item);
        return true;
    }

    /** ================================================ */
    public void clear()
    {
        mListItems.clear();
    }
}

/** ============================================================== */

package gyq.wifixiaofang.ui.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import gyq.wifixiaofang.R;
import gyq.wifixiaofang.ui.adapter.MyListViewAdapter;
import gyq.wifixiaofang.ui.api.OnNetworkChangeListener;
import gyq.wifixiaofang.ui.component.MyListView;
import gyq.wifixiaofang.ui.dialog.WifiConnDialog;
import gyq.wifixiaofang.ui.dialog.WifiStatusDialog;
import gyq.wifixiaofang.utils.WifiAdminUtils;
import gyq.wifixiaofang.utils.WifiConnectUtils;

/**
 * Created by Xiho on 2016/2/2.
 */
public class MainActivity extends ActionBarActivity {

    protected static final String TAG = "MainActivity";

    private static final int REFRESH_CONN = 100;
    // Wifi管理类
    private WifiAdminUtils mWifiAdmin;
    // 扫描结果列表
    private List<ScanResult> list = new ArrayList<ScanResult>();
    // 显示列表
    private MyListView listView;

    private MyListViewAdapter mAdapter;
    //下标
    private int mPosition;

    private WifiReceiver mReceiver;

    private OnNetworkChangeListener mOnNetworkChangeListener = new OnNetworkChangeListener() {

        @Override
        public void onNetWorkDisConnect() {
            getWifiListInfo();
            mAdapter.setDatas(list);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onNetWorkConnect() {
            getWifiListInfo();
            mAdapter.setDatas(list);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mAdapter.notifyDataSetChanged();
        }
    };
    public void msg(String text){
        AlertDialog.Builder  builder=new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setIcon(R.drawable.abc_btn_radio_to_on_mtrl_000);
        builder.setMessage(text);

        //为builder对象添加确定按钮，不过这里嵌套了一个函数
        builder.setPositiveButton("确定",new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface arg0,int arg1)
                    {
                        //android.os.Process.killProcess(android.os.Process.myPid());
                //        finish();
                    }
                }
        );
        //builder创建对话框对象AlertDialog
        AlertDialog simpledialog=builder.create();
        simpledialog.show();


    }

    public void execCommand(String command) throws IOException {
        // start the ls command running
        //String[] args =  new String[]{"sh", "-c", command};
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec(command);        //这句话就是shell与高级语言间的调用
        //如果有参数的话可以用另外一个被重载的exec方法
        //实际上这样执行时启动了一个子进程,它没有父进程的控制台
        //也就看不到输出,所以我们需要用输出流来得到shell执行后的输出
        InputStream inputstream = proc.getInputStream();
        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
        // read the ls output
        String line = "";
        StringBuilder sb = new StringBuilder(line);
        while ((line = bufferedreader.readLine()) != null) {
            //System.out.println(line);
            sb.append(line);
            sb.append('\n');
        }
        //tv.setText(sb.toString());
        //使用exec执行不会等执行成功以后才返回,它会立即返回
        //所以在某些情况下是很要命的(比如复制文件的时候)
        //使用wairFor()可以等待命令执行完成以后才返回
        try {
            if (proc.waitFor() != 0) {
                System.err.println("exit value = " + proc.exitValue());
            }
        }
        catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    public void setMobileDataStatus(Context context,boolean enabled) throws InvocationTargetException
    {
        Method dataConnSwitchmethod = null;
        Class telephonyManagerClass = null;
        Object ITelephonyStub = null;
        Class ITelephonyClass = null;

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

//        if(telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED){
//            isEnabled = true;
//        }else{
//            isEnabled = false;
//        }

        try {
            telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Method getITelephonyMethod = null;
        try {
            getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        getITelephonyMethod.setAccessible(true);
        try {
            ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        try {
            ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (!enabled) {
            try {
                dataConnSwitchmethod = ITelephonyClass.getDeclaredMethod("disableDataConnectivity");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        } else {
            try {
                dataConnSwitchmethod = ITelephonyClass.getDeclaredMethod("enableDataConnectivity");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        dataConnSwitchmethod.setAccessible(true);
        try {
            dataConnSwitchmethod.invoke(ITelephonyStub);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void setMobileDataEnabled(Context context, boolean enabled){
        final ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Class cmClass = null;
        try {
            cmClass = Class.forName(cm.getClass().getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Field iConnectivityManagerField = null;
        try {
            iConnectivityManagerField = cmClass.getDeclaredField("mService");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        iConnectivityManagerField.setAccessible(true);
        Object iConnectivityManager = null;
        try {
            iConnectivityManager = iConnectivityManagerField.get(cm);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        Class iConnectivityManagerClass = null;
        try {
            iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Method setMobileDataEnabledMethod = null;
        try {
            setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled",Boolean.TYPE );
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        setMobileDataEnabledMethod.setAccessible(true);
        try {
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();

//        mWifiAdmin.closeWifi();
//        try{
//            setMobileDataEnabled(this,true);
//        }catch(Exception e){
//            msg("打开数据网络出错");
//            //  Toast.makeText(this, "没有安装米家APP", Toast.LENGTH_LONG).show();
//        }
        Intent intent = null;
        try{
            intent = this.getPackageManager().getLaunchIntentForPackage("com.xiaomi.smarthome");
            startActivity(intent);
        }catch(Exception e){
            msg("没有安装米家APP");
          //  Toast.makeText(this, "没有安装米家APP", Toast.LENGTH_LONG).show();
        }
        Thread.currentThread();
       try {
            Thread.sleep(5000);
        } catch (Exception e) {
//            msg("Thread.sleep出错");
        }
//        try {
//            setMobileDataEnabled(this,false);
//        } catch (Exception e) {
//            msg("关闭数据网络出错");
//        }
        //|| networkInfo.getType() != ConnectivityManager.TYPE_MOBILE) {
            //mWifiAdmin = new WifiAdminUtils(getApplicationContext());
        while(!mWifiAdmin.openWifi()){
  //      while (mWifiAdmin.checkState() != WifiManager.WIFI_STATE_ENABLING) {
            try {
                // 为了避免程序一直while循环，让它睡个100毫秒在检测……
                Thread.currentThread();
                Thread.sleep(100);
     //           mWifiAdmin.openWifi();
            } catch (InterruptedException ie) {
                Toast.makeText(this, "打开WIFI出错", Toast.LENGTH_LONG).show();
            }
        }
        if(mWifiAdmin.getBSSID().indexOf("miap9EA6") <= 0)
            mWifiAdmin.disConnectionWifi(mWifiAdmin.getConnNetId());
        int i= 20;
        while(i > 0){
            //      while (mWifiAdmin.checkState() != WifiManager.WIFI_STATE_ENABLING) {
            try {
                // 为了避免程序一直while循环，让它睡个100毫秒在检测……
                Thread.currentThread();
                Thread.sleep(100);
                mWifiAdmin.connect("isa-camera-isc5_miap9EA6","20110331",
                        WifiConnectUtils.WifiCipherType.WIFICIPHER_WPA);
                i = i - 1;;
                //           mWifiAdmin.openWifi();
            } catch (InterruptedException ie) {
                Toast.makeText(this, "连接小方WIFI出错", Toast.LENGTH_LONG).show();
            }
        }
        Toast.makeText(this, "连接成功", Toast.LENGTH_LONG).show();
      //  msg("连接成功");
//    initData();

//        initView();
//        setListener();
//        refreshWifiStatusOnTime();
    }
    private void simulateClick(View view, float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        final MotionEvent downEvent = MotionEvent.obtain(downTime, downTime,MotionEvent.ACTION_DOWN, x, y, 0);
        downTime += 1000;
        final MotionEvent upEvent = MotionEvent.obtain(downTime, downTime,MotionEvent.ACTION_UP, x, y, 0);
        view.onTouchEvent(downEvent);
        view.onTouchEvent(upEvent);
        downEvent.recycle();
        upEvent.recycle();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        mWifiAdmin = new WifiAdminUtils(MainActivity.this);
        // 获得Wifi列表信息
        getWifiListInfo();
    }

    /**
     * 初始化View
     */
    private void initView() {

        listView = (MyListView) findViewById(R.id.freelook_listview);
        mAdapter = new MyListViewAdapter(this, list);
        listView.setAdapter(mAdapter);
        //检查当前wifi状态
        int wifiState = mWifiAdmin.checkState();
        //WIFI_STATE_DISABLED  WIFI网卡不可用
        //WIFI_STATE_DISABLING  WIFI网卡正在关闭
        //WIFI_STATE_ENABLED  WIFI网卡状态未知
        if (wifiState == WifiManager.WIFI_STATE_DISABLED
                || wifiState == WifiManager.WIFI_STATE_DISABLING
                || wifiState == WifiManager.WIFI_STATE_UNKNOWN) {

        } else {
        }
    }

    private void registerReceiver() {
        mReceiver = new WifiReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mReceiver, filter);
    }

    private void setListener() {
        // 设置刷新监听
        listView.setonRefreshListener(new MyListView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new AsyncTask<Void, Void, Void>() {
                    protected Void doInBackground(Void... params) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        getWifiListInfo();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        mAdapter.setDatas(list);
                        mAdapter.notifyDataSetChanged();
                        listView.onRefreshComplete();
                    }

                }.execute();
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos,
                                    long id) {
                mPosition = pos - 1;
                ScanResult scanResult = list.get(mPosition);
                String desc = "";
                String descOri = scanResult.capabilities;
                if (descOri.toUpperCase().contains("WPA-PSK")) {
                    desc = "WPA";
                }
                if (descOri.toUpperCase().contains("WPA2-PSK")) {
                    desc = "WPA2";
                }
                if (descOri.toUpperCase().contains("WPA-PSK")
                        && descOri.toUpperCase().contains("WPA2-PSK")) {
                    desc = "WPA/WPA2";
                }

                if (desc.equals("")) {
                    isConnectSelf(scanResult);
                    return;
                }
                isConnect(scanResult);
            }

            /**
             * 有密码验证连接
             * @param scanResult
             */
            private void isConnect(ScanResult scanResult) {
                if (mWifiAdmin.isConnect(scanResult)) {
                    // 已连接，显示连接状态对话框
                    WifiStatusDialog mStatusDialog = new WifiStatusDialog(
                            MainActivity.this, R.style.defaultDialogStyle,
                            scanResult, mOnNetworkChangeListener);
                    mStatusDialog.show();
                } else {
                    // 未连接显示连接输入对话框
                    WifiConnDialog mDialog = new WifiConnDialog(
                            MainActivity.this, R.style.defaultDialogStyle, listView, mPosition, mAdapter,
                            scanResult, list, mOnNetworkChangeListener);
                    mDialog.show();
                }
            }

            /**
             * 无密码直连
             * @param scanResult
             */
            private void isConnectSelf(ScanResult scanResult) {
                if (mWifiAdmin.isConnect(scanResult)) {
                    // 已连接，显示连接状态对话框
                    WifiStatusDialog mStatusDialog = new WifiStatusDialog(
                            MainActivity.this, R.style.defaultDialogStyle,
                            scanResult, mOnNetworkChangeListener);
                    mStatusDialog.show();
                } else {
                    boolean iswifi = mWifiAdmin.connectSpecificAP(scanResult);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (iswifi) {
                        Toast.makeText(MainActivity.this, "连接成功！",Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "连接失败！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * 得到wifi的列表信息
     */
    private void getWifiListInfo() {
        Log.d(TAG, "getWifiListInfo");
        mWifiAdmin.startScan();
        List<ScanResult> tmpList = mWifiAdmin.getWifiList();
        if (tmpList == null) {
            list.clear();
        } else {
            list = tmpList;
        }
    }

    private Handler mHandler = new MyHandler(this);

    protected boolean isUpdate = true;

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    private static class MyHandler extends Handler {

        private WeakReference<MainActivity> reference;

        public MyHandler(MainActivity activity) {
            this.reference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {

            MainActivity activity = reference.get();

            switch (msg.what) {
                case REFRESH_CONN:
                    activity.getWifiListInfo();
                    activity.mAdapter.setDatas(activity.list);
                    activity.mAdapter.notifyDataSetChanged();
                    break;

                default:
                    break;
            }

            super.handleMessage(msg);
        }
    }

    /**
     * 定时刷新Wifi列表信息
     *
     * @author Xiho
     */
    private void refreshWifiStatusOnTime() {
        new Thread() {
            public void run() {
                while (isUpdate) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mHandler.sendEmptyMessage(REFRESH_CONN);
                }
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isUpdate = false;
        unregisterReceiver();
    }

    /**
     * 取消广播
     */
    private void unregisterReceiver() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }

    private class WifiReceiver extends BroadcastReceiver {
        protected static final String TAG = "MainActivity";
        //记录网络断开的状态
        private boolean isDisConnected = false;
        //记录正在连接的状态
        private boolean isConnecting = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {// wifi连接上与否
                Log.d(TAG, "网络已经改变");
                NetworkInfo info = intent
                        .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    if (!isDisConnected) {
                        Log.d(TAG, "wifi已经断开");
                        isDisConnected = true;
                    }
                } else if (info.getState().equals(NetworkInfo.State.CONNECTING)) {
                    if (!isConnecting) {
                        Log.d(TAG, "正在连接...");
                        isConnecting = true;
                    }
                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    WifiManager wifiManager = (WifiManager) context
                            .getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    Log.d(TAG, "连接到网络：" + wifiInfo.getBSSID());
                }

            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR,
                        0);
                switch (error) {

                    case WifiManager.ERROR_AUTHENTICATING:
                        Log.d(TAG, "密码认证错误Code为：" + error);
                        Toast.makeText(getApplicationContext(), "wifi密码认证错误！", Toast.LENGTH_SHORT).show();
                        break;

                    default:
                        break;
                }

            } else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                Log.e("H3c", "wifiState" + wifiState);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLING:
                        Log.d(TAG, "wifi正在启用");
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.d(TAG, "Wi-Fi已启用。");
                        break;

                }
            }
        }

    }

}

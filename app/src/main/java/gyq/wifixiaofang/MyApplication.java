package gyq.wifixiaofang;

import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;

import gyq.wifixiaofang.utils.WifiAdminUtils;
import gyq.wifixiaofang.utils.WifiConnectUtils;

/**
 * Created by Xiho on 2016/2/1.
 */
public class MyApplication extends Application {
    protected static final String TAG = "MyApplication";

    private WifiAdminUtils mWifiAdmin;

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
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                }
        );
        //builder创建对话框对象AlertDialog
        AlertDialog simpledialog=builder.create();
        simpledialog.show();


    }
    public void onCreate() {
        super.onCreate();
        mWifiAdmin = new WifiAdminUtils(getApplicationContext());
        mWifiAdmin.openWifi();
        if(!mWifiAdmin.connect("3#5","12345", WifiConnectUtils.WifiCipherType.WIFICIPHER_WEP))
        {
            msg("无法连接");
        }
        else
            msg("连接成功");
    }

}

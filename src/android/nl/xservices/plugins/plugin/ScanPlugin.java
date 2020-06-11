package nl.xservices.plugins.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import nl.xservices.plugins.base.ICordovaPlugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Description: 条码扫描
 * Date: 2020/6/11
 *
 * @author wangke
 */
public class ScanPlugin implements ICordovaPlugin {
    private CordovaInterface mCordova;
    private CallbackContext mCurrCallbackContext;
    private boolean isRegBroadcast = false;
    private static final String TAG = "wk";

    public ScanPlugin(CordovaInterface cordova) {
        this.mCordova = cordova;
    }

    @Override
    public void onResume(boolean multitasking) {
        registerReceiver();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.mCurrCallbackContext = callbackContext;
        if ("openScanReceiver".equals(action)) {
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            mCurrCallbackContext.sendPluginResult(pr);
            registerReceiver();
            return true;
        } else if ("closeScanReceiver".equals(action)) {
            unRegisterReceiver();
            return true;
        }
        return false;
    }

    private void unRegisterReceiver() {
        if (!isRegBroadcast) {
            return;
        }
        mCordova.getActivity().unregisterReceiver(receiver);
        isRegBroadcast = false;
    }

    private void registerReceiver() {
        if (isRegBroadcast) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter("com.android.server.scannerservice.broadcast");
        intentFilter.addAction("com.android.server.scannerservice.broadcast");
        mCordova.getActivity().registerReceiver(receiver, intentFilter);
        isRegBroadcast = true;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.android.server.scannerservice.broadcast".equals(action)) {
                try {
                    String code = intent.getStringExtra("scannerdata");
                    if (code != null) {
                        Log.e(TAG, "扫码结果：" + code);
                        PluginResult pr = new PluginResult(PluginResult.Status.OK, code);
                        pr.setKeepCallback(true);
                        mCurrCallbackContext.sendPluginResult(pr);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };


    @Override
    public void onPause(boolean multitasking) {
        unRegisterReceiver();
    }

    @Override
    public void onDestroy() {
        unRegisterReceiver();
    }
}

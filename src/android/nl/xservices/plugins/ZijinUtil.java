package nl.xservices.plugins;

import nl.xservices.plugins.base.ICordovaPlugin;
import nl.xservices.plugins.plugin.UHFPlugin;
import nl.xservices.plugins.plugin.ScanPlugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Description: CZ8800设备插件
 * Date: 2020/6/8
 *
 * @author wangke
 */
public class ZijinUtil extends CordovaPlugin {
    private List<ICordovaPlugin> mPluginList = new ArrayList<>();

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.mPluginList.add(new UHFPlugin(cordova));
        this.mPluginList.add(new ScanPlugin(cordova));
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        for (ICordovaPlugin plugin : mPluginList) {
            if (plugin.execute(action, args, callbackContext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPause(boolean multitasking) {
        for (ICordovaPlugin plugin : mPluginList) {
            plugin.onPause(multitasking);
        }
        super.onPause(multitasking);
    }

    @Override
    public void onResume(boolean multitasking) {
        for (ICordovaPlugin plugin : mPluginList) {
            plugin.onResume(multitasking);
        }
        super.onResume(multitasking);
    }

    @Override
    public void onDestroy() {
        for (ICordovaPlugin plugin : mPluginList) {
            plugin.onDestroy();
        }
        super.onDestroy();
    }
}

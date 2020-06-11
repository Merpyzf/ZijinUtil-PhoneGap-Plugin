package nl.xservices.plugins.plugin;

import android.util.Log;
import android.widget.Toast;

import nl.xservices.plugins.MyApp;
import nl.xservices.plugins.base.ICordovaPlugin;
import nl.xservices.plugins.utils.DevBeep;
import com.uhf.api.cls.Reader;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;

/**
 * Description: UHF电子标签识别
 * Date: 2020/6/8
 *
 * @author wangke
 */
public class UHFPlugin implements ICordovaPlugin {
    private MyApp mApp;
    private CordovaInterface mCordova;
    private CallbackContext mCurrCallbackContext;
    private boolean isRunning = false;
    private boolean isConnectedUHFDevice = false;
    private boolean inventoryOpened = false;
    private Thread mThread;
    private static final String TAG = "wk";

    public UHFPlugin(CordovaInterface cordova) {
        this.mCordova = cordova;
        init();
    }

    private void init() {
        this.mApp = (MyApp) this.mCordova.getActivity().getApplication();
    }

    @Override
    public void onResume(boolean multitasking) {
        if (inventoryOpened) {
            isRunning = true;
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.mCurrCallbackContext = callbackContext;
        if ("openUHF".equals(action)) {
            Log.e(TAG, "execute: openUHF");
            openUHF();
            return true;
        } else if ("closeUHF".equals(action)) {
            Log.e(TAG, "execute: closeUHF");
            closeUHF(true);
            return true;
        } else if ("startInventoryReal".equals(action)) {
            Log.e(TAG, "execute: startInventoryReal");
            startInventoryReal();
            return true;
        } else if ("stopInventoryReal".equals(action)) {
            Log.e(TAG, "execute: stopInventoryReal");
            stopInventoryReal();
            return true;
        } else if ("setOutputPower".equals(action)) {
            Log.e(TAG, "execute: setOutputPower");
            setOutputPower(args);
            return true;
        }
        return false;
    }

    /**
     * 暴露给前端的设置读取功率方法（Promise）
     *
     * @param args
     */
    private void setOutputPower(JSONArray args) {
        try {
            int outputPower = args.getJSONObject(0).getInt("outputPower");
            if (outputPower >= 500 && outputPower <= 3000) {
                setReadPower(outputPower);
            } else {
                this.mCurrCallbackContext.error("设置功率失败，功率值超出范围值");
            }
        } catch (JSONException e) {
            this.mCurrCallbackContext.error("参数设置异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置设备的读功率
     *
     * @param power 功率大小
     */
    private void setReadPower(int power) {
        Reader.AntPowerConf antPowerConf = mApp.Mreader.new AntPowerConf();
        antPowerConf.antcnt = 1;
        int[] rpow = new int[antPowerConf.antcnt];
        Reader.AntPower jaap = mApp.Mreader.new AntPower();
        jaap.antid = 1;
        jaap.readPower = (short) power;
        antPowerConf.Powers[0] = jaap;
        rpow[0] = jaap.readPower;
        try {
            Reader.READER_ERR er = mApp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, antPowerConf);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                mApp.Rparams.rpow = rpow;
                this.mCurrCallbackContext.success("设置成功");
                Log.e(TAG, "设置成功");
            } else {
                this.mCurrCallbackContext.error("设置失败");
                Log.e(TAG, "设置失败");
            }

        } catch (Exception e) {
            this.mCurrCallbackContext.error("设置异常：" + e.getMessage());
            Log.e(TAG, "设置异常：" + e.getMessage());
        }
    }

    /**
     * 停止盘点（Void）
     */
    private void stopInventoryReal() {
        isRunning = false;
        inventoryOpened = false;
    }

    /**
     * 开始盘点（Observable）
     */
    private void startInventoryReal() {
        inventoryOpened = true;
        PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);
        this.mCurrCallbackContext.sendPluginResult(pr);
        // 如果线程对象不为空表示盘点任务已经执行，只需要更改isRunning的状态即可继续唤醒任务的执行
        if (mThread != null) {
            isRunning = true;
        } else {
            isRunning = true;
            mThread = new Thread(inventoryRunnable);
            mThread.start();
        }
    }

    /**
     * 断开与UHF设备的连接（Promise）
     */
    private void closeUHF(boolean sendResut) {
        DevBeep.release();
        mCordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                isRunning = false;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mApp.Mreader != null) {
                    mApp.Mreader.CloseReader();
                }
                boolean powerDownRet = powerDown();
                if (powerDownRet) {
                    if (sendResut) {
                        mCurrCallbackContext.success("设备断电成功");
                    }
                    isConnectedUHFDevice = false;
                } else {
                    if (sendResut) {
                        mCurrCallbackContext.error("设备断电失败");
                    }
                }
            }
        });
    }

    /**
     * 创建与UHF设备的连接
     */
    private void openUHF() {
        mCordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                DevBeep.init(mCordova.getActivity());
                mApp.Mreader = new Reader();
                mApp.Rparams = mApp.new ReaderParams();
                boolean powerUpRet = powerUp();
                if (!powerUpRet) {
                    mCurrCallbackContext.error("连接失败");
                    return;
                }
                String address = "/dev/ttyHSL1";
                Reader.READER_ERR err = mApp.Mreader.InitReader_Notype(address, 1);
                if (err == Reader.READER_ERR.MT_OK_ERR) {
                    mApp.antportc = 1;
                    mApp.Address = address;
                    Log.e(TAG, "连接成功");
                    isConnectedUHFDevice = true;
                    mCurrCallbackContext.success("连接成功");
                } else {
                    mCurrCallbackContext.error("连接失败：" + err.toString());
                    Log.e(TAG, "连接失败:" + err.toString());
                }
            }
        });
    }

    /**
     * 设备上电
     *
     * @return
     */
    private boolean powerUp() {
        try {
            FileWriter localFileWriterOn = new FileWriter(new File("/sys/class/tty/ttyHSL1/device/uart_switch"));
            localFileWriterOn.write("slr53");
            localFileWriterOn.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "设备上电：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 设备断电
     *
     * @return
     */
    private boolean powerDown() {
        try {
            FileWriter localFileWriterOn = new FileWriter(new File("/sys/class/tty/ttyHSL1/device/uart_switch"));
            localFileWriterOn.write("disable");
            localFileWriterOn.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "设备断电异常：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 用于执行盘点任务
     */
    private Runnable inventoryRunnable = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if(isRunning == false){
                    continue;
                }
                Log.e(TAG, " inventory running");
                String[] tag = null;
                // 存储读取到的标签数量
                int[] tagcnt = new int[1];
                tagcnt[0] = 0;
                // 盘存标签
                // ants：输入参数，存放操作使用的天线，可以有多个天线
                // antcnt：ants数组的长度
                // timeout：操作超时的时间
                // tagcnt：读取到的标签个数
                Reader.READER_ERR er = mApp.Mreader.TagInventory_Raw
                        (mApp.Rparams.uants, mApp.Rparams.uants.length,
                                (short) mApp.Rparams.readtime, tagcnt);
                Log.e(TAG, "read:" + er.toString() + " cnt:" + String.valueOf(tagcnt[0]));
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    // 如果读到数据了，就播放声音
                    if (tagcnt[0] > 0) {
                        DevBeep.PlayOK();
                        synchronized (this) {
                            JSONArray jsonArray = new JSONArray();
                            for (int i = 0; i < tagcnt[0]; i++) {
                                Reader.TAGINFO tfs = mApp.Mreader.new TAGINFO();
                                er = mApp.Mreader.GetNextTag(tfs);
                                Log.e("MYINFO", "get tag index:" + String.valueOf(i) + " er:" + er.toString());
                                // 读取出错
                                if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                                    Log.e(TAG, "读取出错：Msg_error_1 error:" + String.valueOf(er.value()) + er.toString());
                                    isRunning = false;
                                    mApp.needreconnect = true;
                                    isConnectedUHFDevice = false;
                                    PluginResult pr = new PluginResult(PluginResult.Status.ERROR, er.value());
                                    pr.setKeepCallback(true);
                                    mCurrCallbackContext.sendPluginResult(pr);
                                }
                                // 读取到了标签内容
                                if (er == Reader.READER_ERR.MT_OK_ERR) {
                                    // 每次读取到标签内容就直接返回
                                    String hexEpcStr = Reader.bytes_Hexstr(tfs.EpcId);
                                    jsonArray.put(hexEpcStr);
                                    Log.e(TAG, "读取到的标签内容：" + Reader.bytes_Hexstr(tfs.EpcId));
                                } else {
                                    break;
                                }
                            }
                            if (jsonArray.length() > 0) {
                                  Log.i(TAG, jsonArray.toString());
                                  PluginResult pr = new PluginResult(PluginResult.Status.OK, jsonArray.toString());
                                  pr.setKeepCallback(true);
                                  mCurrCallbackContext.sendPluginResult(pr);
                            }
                        }
                    }
                } else {
                    // 标签读取出错
                    mApp.needreconnect = true;
                    isRunning = false;
                    isConnectedUHFDevice = false;
                    PluginResult pr = new PluginResult(PluginResult.Status.ERROR, "error:" + er.value());
                    pr.setKeepCallback(true);
                    mCurrCallbackContext.sendPluginResult(pr);
                }
                // 设置每次读取标签的间隔时间
                try {
                    Thread.sleep(mApp.Rparams.sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onPause(boolean multitasking) {
        if (inventoryOpened) {
            isRunning = false;
        }
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "UHF-PLUGIN ON-DESTORY");
        closeUHF(false);
    }

}

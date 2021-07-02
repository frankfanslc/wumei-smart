package com.kerwin.wumei;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.multidex.MultiDex;

import com.kerwin.wumei.http.interceptor.CustomLoggingInterceptor;
import com.kerwin.wumei.utils.SettingSPUtils;
import com.kerwin.wumei.utils.XToastUtils;
import com.kerwin.wumei.utils.sdkinit.ANRWatchDogInit;
import com.kerwin.wumei.utils.sdkinit.UMengInit;
import com.kerwin.wumei.utils.sdkinit.XBasicLibInit;
import com.kerwin.wumei.utils.sdkinit.XUpdateInit;
import com.xuexiang.xhttp2.XHttpSDK;
import com.xuexiang.xrouter.launcher.XRouter;

import static com.kerwin.wumei.utils.SettingUtils.getServeUrl;

/**
 * @author xuexiang
 * @since 2018/11/7 下午1:12
 */
public class MyApp extends Application {

    private static MyApp app;
    private MutableLiveData<String> mBroadcastData;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            switch (action) {
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                case LocationManager.PROVIDERS_CHANGED_ACTION:
                    mBroadcastData.setValue(action);
                    break;
            }
        }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //解决4.x运行崩溃的问题
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initLibs();
        initHttp();
        app = this;
        mBroadcastData = new MutableLiveData<>();
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        }
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(mReceiver);
    }

    public static MyApp getInstance() {
        return app;
    }

    public void observeBroadcast(LifecycleOwner owner, Observer<String> observer) {
        mBroadcastData.observe(owner, observer);
    }


    /**
     * 初始化基础库
     */
    private void initLibs() {
        XBasicLibInit.init(this);

        XUpdateInit.init(this);

        //运营统计数据运行时不初始化
        if (!MyApp.isDebug()) {
            UMengInit.init(this);
        }

        //ANR监控
        ANRWatchDogInit.init();

//        initXRouter();
    }

    private void initHttp() {
        XHttpSDK.init(this);   //初始化网络请求框架，必须首先执行
        XHttpSDK.setSuccessCode(200);
//        XHttpSDK.debug();  //需要调试的时候执行
//        XHttpSDK.debug(new CustomLoggingInterceptor()); //设置自定义的日志打印拦截器
        XHttpSDK.setBaseUrl(getServeUrl());  //设置网络请求的基础地址
//        XHttpSDK.addInterceptor(new CustomDynamicInterceptor()); //设置动态参数添加拦截器
//        XHttpSDK.addInterceptor(new CustomExpiredInterceptor()); //请求失效校验拦截器
    }

    private void initXRouter() {
        if (BuildConfig.DEBUG) {   // 这两行必须写在init之前，否则这些配置在init过程中将无效
            XRouter.openLog();     // 打印日志
            XRouter.openDebug();   // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
        }
        XRouter.init(this);
    }


    /**
     * @return 当前app是否是调试开发模式
     */
    public static boolean isDebug() {
        return BuildConfig.DEBUG;
    }


}

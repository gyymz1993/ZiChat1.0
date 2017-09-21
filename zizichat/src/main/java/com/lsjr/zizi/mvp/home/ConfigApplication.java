package com.lsjr.zizi.mvp.home;

import android.app.Application;

import com.google.gson.Gson;
import com.lsjr.bean.ObjectResult;
import com.lsjr.bean.Result;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.net.DcodeService;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.chat.bean.ConfigBean;
import com.lsjr.zizi.chat.db.User;
import com.lsjr.zizi.chat.helper.SQLiteHelper;
import com.nostra13.universalimageloader.utils.L;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/4 15:08
 */

public class ConfigApplication {
    private static ConfigApplication configApplication;
    private static Application mApplication;


    /*********************** 保存当前登陆用户的全局信息 ***************/
    public String roomName;
    public String mAccessToken;
    public long mExpiresIn;
    public int mUserStatus;
    public boolean mUserStatusChecked = false;
    public User mLoginUser = new User();// 当前登陆的用户



    private double mLongitude;
    private double mLatitude;



    public String getmAccessToken() {
        return mAccessToken;
    }

    public void setmAccessToken(String mAccessToken) {
        this.mAccessToken = mAccessToken;
    }

    private ConfigApplication(){

    }

    public double getmLongitude() {
        return mLongitude;
    }

    public void setmLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public double getmLatitude() {
        return mLatitude;
    }

    public void setmLatitude(double mLatitude) {
        this.mLatitude = mLatitude;
    }

    public static ConfigApplication instance(){
        if (configApplication==null){
            synchronized (ConfigApplication.class){
                if (mApplication==null){
                    configApplication=new ConfigApplication();
                }
            }
        }
        return configApplication;
    }

    public String getLoginUserId() {
        if (mLoginUser==null){
           return null;
        }
        return mLoginUser.getUserId();
    }


    public void initialize(Application application) {
        mApplication=application;
        DcodeService.initialize(application);
       // initConfig();
        // 初始化数据库
        SQLiteHelper.copyDatabaseFile(application);
    }


    /********************* 百度地图定位服务 ************************/
//    private BdLocationHelper mBdLocationHelper;
//    public BdLocationHelper getBdLocationHelper() {
//        if (mBdLocationHelper == null) {
//            mBdLocationHelper = new BdLocationHelper(this);
//        }
//        return mBdLocationHelper;
//    }


    /**
     * 配置参数初始化
     */
    private void initConfig() {
        HttpUtils.getInstance().postServiceData(AppConfig.CONFIG_URL, null,new ChatObjectCallBack<ConfigBean>(ConfigBean.class) {
            @Override
            protected void onXError(String exception) {

            }

            @Override
            protected void onSuccess(ObjectResult<ConfigBean> result) {
                ConfigBean configBean = null;
                if (result == null || result.getResultCode() != Result.CODE_SUCCESS || result.getData() == null) {
                    configBean = new ConfigBean();// 读取网络配置失败，使用默认配置
                    L.e("--------->读取网关配置失败",configBean.toString());
                } else {
                    configBean = result.getData();
                    L.e("--------->读取网关配置成功"+new Gson().toJson(configBean));
                }
            }
        });

    }

}

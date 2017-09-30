package com.lsjr.zizi;

import android.app.Application;

import com.lsjr.net.DcodeService;
import com.lsjr.zizi.db.User;
import com.lsjr.zizi.helper.SQLiteHelper;

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

}

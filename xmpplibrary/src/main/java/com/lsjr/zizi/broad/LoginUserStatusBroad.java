package com.lsjr.zizi.broad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lsjr.zizi.ConfigApplication;
import com.lsjr.zizi.db.User;
import com.lsjr.zizi.helper.LoginHelper;
import com.ymz.baselibrary.utils.L_;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/27 15:11
 */

public class LoginUserStatusBroad extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(LoginHelper.ACTION_LOGIN)) {
            User user = ConfigApplication.instance().mLoginUser;
            L_.e("登陆成功---------------》启动服务"+user.toString());

        } else if (action.equals(LoginHelper.ACTION_LOGOUT)) {
            ConfigApplication.instance().mUserStatus = LoginHelper.STATUS_USER_SIMPLE_TELPHONE;


        } else if (action.equals(LoginHelper.ACTION_CONFLICT)) {
            // 改变用户状态
            ConfigApplication.instance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;

        } else if (action.equals(LoginHelper.ACTION_NEED_UPDATE)) {

        } else if (action.equals(LoginHelper.ACTION_LOGIN_GIVE_UP)) {

        }

    }
}

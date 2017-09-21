package com.lsjr.zizi.chat.bean;

import com.lsjr.bean.Result;
import com.lsjr.zizi.chat.helper.LoginHelper;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/5 11:01
 */

public class ResultCode extends Result {

    public static boolean defaultParser(Result result, boolean showToast) {
        if (result == null) {
            if (showToast) {
                T_.showToastReal("异常");
            }
            return false;
        }
        if (result.resultCode == CODE_SUCCESS) {// 成功
            return true;
        } else if (result.resultCode == CODE_NO_TOKEN) {// 缺少参数Token
            LoginHelper.broadcastConflict(UIUtils.getContext());
            if (showToast)
            T_.showToastReal("缺少参数Token");
            return false;
        } else if (result.resultCode == CODE_TOKEN_ERROR) {// Token过期或错误
            LoginHelper.broadcastConflict(UIUtils.getContext());
            if (showToast)
                T_.showToastReal("Token过期或错误");
            return false;
        } else {
            if (showToast)
                T_.showToastReal("未知异常");
            return false;
        }
    }



}

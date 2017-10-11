package com.lsjr.zizi.mvp.home.session;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.githang.statusbar.StatusBarCompat;
import com.lsjr.bean.ObjectResult;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.bean.LoginRegisterResult;
import com.lsjr.zizi.chat.helper.LoginHelper;
import com.lsjr.zizi.chat.utils.StringUtils;
import com.lsjr.zizi.mvp.home.HomeActivity;
import com.lsjr.zizi.mvp.listener.MyEditTextChangeListener;
import com.lsjr.zizi.util.DeviceInfoUtil;
import com.lsjr.zizi.util.Md5Util;
import com.ymz.baselibrary.BaseApplication;
import com.ymz.baselibrary.NetWorkObservable;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.HashMap;

import butterknife.BindView;

/**
 * 登陆界面
 */
public class LoginActivity extends MvpActivity implements View.OnClickListener{
    @BindView(R.id.login_btn)
    Button loginBtn;
    private EditText mPhoneNumberEdit;
    private EditText mPasswordEdit;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }


    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("用户登录");
    }


    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        mPhoneNumberEdit = (EditText) findViewById(R.id.phone_numer_edit);
        mPasswordEdit = (EditText) findViewById(R.id.password_edit);
        //忘记密码按钮点击事件
        findViewById(R.id.forget_password_btn).setOnClickListener(this);
        //注册账号
        findViewById(R.id.register_account_btn).setOnClickListener(this);
        //登陆账号
        findViewById(R.id.login_btn).setOnClickListener(this);

        mPhoneNumberEdit.addTextChangedListener(new MyEditTextChangeListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0 && s.toString() != null) {
                    loginBtn.setEnabled(true);
                } else {
                    loginBtn.setEnabled(false);
                }
            }
        });

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_account_btn:// 注册
                openActivity(RegisterActivity.class);
                break;
            case R.id.forget_password_btn:// 忘记密码
                break;
            case R.id.login_btn:// 登陆
                login();
                break;
        }
    }

    private void login() {

        final String phoneNumber = mPhoneNumberEdit.getText().toString().trim();
        String password = mPasswordEdit.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            return;
        }
        if (TextUtils.isEmpty(password)) {
            return;
        }

        if (!StringUtils.isMobileNumber(phoneNumber)) {
            mPhoneNumberEdit.requestFocus();
            mPhoneNumberEdit.setError(StringUtils.editTextHtmlErrorTip(
                    UIUtils.getContext(), "请输入正确的手机号码"));
            return;
        }
        // 加密之后的密码
        final String digestPwd = Md5Util.toMD5(password);
        HashMap<String, String> params = new HashMap<>();
        params.put("telephone", Md5Util.toMD5(phoneNumber));// 账号登陆的时候需要MD5加密，服务器需求
        params.put("password", digestPwd);
        // 附加信息
        params.put("model", DeviceInfoUtil.getModel());
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        params.put("serial", DeviceInfoUtil.getDeviceId(BaseApplication.getApplication()));
        // 地址信息
        //double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        //double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        params.put("latitude", String.valueOf(100));
        params.put("longitude", String.valueOf(100));
        ConfigApplication.instance().setmLatitude(100);
        ConfigApplication.instance().setmLongitude(100);
//		if (latitude != 0)
//			params.put("latitude", String.valueOf(latitude));
//		if (longitude != 0)
//			params.put("longitude", String.valueOf(longitude));
        //mConfig.USER_LOGIN
        showProgressDialogWithText("登陆中");
        L_.e(AppConfig.USER_LOGIN);
        HttpUtils.getInstance().postServiceData(AppConfig.USER_LOGIN, params, new ChatObjectCallBack<LoginRegisterResult>(LoginRegisterResult.class) {

            @Override
            protected void onXError(String exception) {
                T_.showToastReal(exception);
                dismissProgressDialog();
            }

            @Override
            protected void onSuccess(ObjectResult<LoginRegisterResult> result) {
                dismissProgressDialog();
                L_.e("login   -------->"+result.getData().toString());
                boolean success = LoginHelper.setLoginUser(BaseApplication.getApplication(), phoneNumber, digestPwd, result);// 设置登陆用户信息
                if (success) {// 登陆成功
                    //showProgressSuccess("登陆成功");
                    ConfigApplication.instance().setmAccessToken(result.getData().getAccess_token());
                    //LoginHelper.broadcastLogin(UIUtils.getContext());
                    Bundle bundle=new Bundle();
                    bundle.putBoolean("isLoginJump",true);
                    openActivity(HomeActivity.class,bundle);
                } else {// 登录失败
                    if (TextUtils.isEmpty(result.getResultMsg())) {
                        T_.showToastReal(R.string.login_error);
                    } else {
                        T_.showToastReal( result.getResultMsg());
                    }
                }

            }
        });
    }


    @Override
    protected void onDestroy() {
       // BaseApplication.instance().unregisterNetWorkObserver(this);
        super.onDestroy();
    }
}

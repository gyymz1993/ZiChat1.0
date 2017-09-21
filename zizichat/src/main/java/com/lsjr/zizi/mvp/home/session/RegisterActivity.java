package com.lsjr.zizi.mvp.home.session;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.lsjr.bean.ObjectResult;
import com.lsjr.bean.Result;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.callback.HttpSubscriber;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.chat.helper.UserSp;
import com.lsjr.zizi.chat.utils.RongGenerate;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.bean.LoginRegisterResult;
import com.lsjr.zizi.chat.helper.LoginHelper;
import com.lsjr.zizi.chat.utils.StringUtils;
import com.lsjr.zizi.mvp.home.HomeActivity;
import com.lsjr.zizi.mvp.listener.MyEditTextChangeListener;
import com.lsjr.zizi.util.DeviceInfoUtil;
import com.lsjr.zizi.util.Md5Util;
import com.lsjr.zizi.view.ClearEditText;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/2 14:58
 */

public class RegisterActivity extends MvpActivity {

    @BindView(R.id.tv_lg_number)
    ClearEditText mPhoneNumEdit;
    @BindView(R.id.btn_lg_code)
    Button btnLgCode;
    @BindView(R.id.id_pwd)
    ClearEditText idPwd;
    @BindView(R.id.id_lay_login)
    LinearLayout idLayLogin;
    @BindView(R.id.id_btn_confir)
    Button idBtnConfir;
    @BindView(R.id.id_re_pwd)
    ClearEditText idRePwd;
    @BindView(R.id.id_tv_name)
    ClearEditText idtvNname;



    String password;
    String phoneNumber;
    String userName;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_register;
    }

    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("用户注册");
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {


        mPhoneNumEdit.addTextChangedListener(new MyEditTextChangeListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0 && s.toString() != null) {
                    btnLgCode.setEnabled(true);
                    idBtnConfir.setEnabled(true);
                } else {
                    btnLgCode.setEnabled(false);
                    idBtnConfir.setEnabled(false);
                }
            }
        });

        idBtnConfir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phoneNumber = mPhoneNumEdit.getText().toString().trim();
                if (phoneNumber.isEmpty()) {
                    T_.showToastReal("手机号码不能为空");
                    return;
                }
                if (!StringUtils.isMobileNumber(phoneNumber)) {
                    mPhoneNumEdit.requestFocus();
                    mPhoneNumEdit.setError(StringUtils.editTextHtmlErrorTip(
                            UIUtils.getContext(), "请输入正确的手机号码"));
                    return;
                }

                userName=idtvNname.getText().toString().trim();
                if (TextUtils.isEmpty(userName)){
                    T_.showToastReal("请输入用户名");
                    return;
                }

                password = idPwd.getText().toString().trim();
                String confirmPassword = idRePwd.getText().toString().trim();
                if (TextUtils.isEmpty(password) || password.length() < 6) {
                    idPwd.requestFocus();
                    idPwd.setError(StringUtils.editTextHtmlErrorTip(UIUtils.getContext(), R.string.password_empty_error));
                    return;
                }
                if (TextUtils.isEmpty(confirmPassword) || confirmPassword.length() < 6) {
                    idRePwd.requestFocus();
                    idRePwd.setError(StringUtils.editTextHtmlErrorTip(UIUtils.getContext(), R.string.confirm_password_empty_error));
                    return;
                }
                if (!confirmPassword.equals(password)) {
                    idRePwd.requestFocus();
                    idRePwd.setError(StringUtils.editTextHtmlErrorTip(UIUtils.getContext(), R.string.password_confirm_password_not_match));
                    return;
                }
                register();
            }
        });
    }

    public void register(){
       // intent.putExtra(RegisterActivity.EXTRA_PHONE_NUMBER, mPhoneNum);
       // intent.putExtra(RegisterActivity.EXTRA_PASSWORD, Md5Util.toMD5(password));

        showProgressDialogWithText("注册中");
        HashMap<String, String> params = new HashMap<String, String>();
        // 加密之后的密码
        final String digestPwd = Md5Util.toMD5(password);
        // 前面页面传递的信息
        params.put("userType", "1");
        params.put("telephone", phoneNumber);
        params.put("password", digestPwd);
        // params.put("countryId", mAreaCode);//TODO AreaCode 区号暂时不带
        // 本页面信息
        params.put("nickname", userName);
        params.put("sex", String.valueOf(1));
        params.put("birthday", String.valueOf("1"));

       // params.put("countryId", String.valueOf(mTempData.getCountryId()));
        //params.put("provinceId", String.valueOf(mTempData.getProvinceId()));
       // params.put("cityId", String.valueOf(mTempData.getCityId()));
      //  params.put("areaId", String.valueOf(mTempData.getAreaId()));

        // 附加信息
        params.put("apiVersion", DeviceInfoUtil.getVersionCode(UIUtils.getContext()) + "");
        params.put("model", DeviceInfoUtil.getModel());
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        params.put("serial", DeviceInfoUtil.getDeviceId(UIUtils.getContext()));
        // 地址信息
        double latitude = 100;
        double longitude = 100;
        String location = "";
        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));
        if (!TextUtils.isEmpty(location))
            params.put("location", location);

        HttpUtils.getInstance().postServiceData(AppConfig.USER_REGISTER, params, new ChatObjectCallBack<LoginRegisterResult>(LoginRegisterResult.class) {

            @Override
            protected void onXError(String exception) {
                dismissProgressDialog();
                //T_.showToastReal(exception);
                T_.showToastReal(exception);
            }

            @Override
            protected void onSuccess(ObjectResult<LoginRegisterResult> result) {
                dismissProgressDialog();
                if (result == null) {
                    T_.showToastReal("exception");
                    return;
                }
                L_.e("注册成功----------》"+result);
                if (result.getResultCode() == Result.CODE_SUCCESS) {// 注册成功
                    boolean success = LoginHelper.setLoginUser(UIUtils.getContext(), phoneNumber, digestPwd, result);
                    if (success) {
                        ConfigApplication.instance().setmAccessToken(result.getData().getAccess_token());
                        Bundle bundle=new Bundle();
                        bundle.putBoolean("isLoginJump",true);
                        openActivity(HomeActivity.class);
                        //uploadAvatar();
                    } else {// 失败
                        if (TextUtils.isEmpty(result.getResultMsg())) {
                            T_.showToastReal(R.string.register_error);
                        } else {
                            T_.showToastReal( result.getResultMsg());
                        }
                    }
                } else {// 失败
                    if (TextUtils.isEmpty(result.getResultMsg())) {
                        T_.showToastReal(R.string.register_error);
                    } else {
                        T_.showToastReal( result.getResultMsg());
                    }
                }
            }
        });
    }

    private void uploadAvatar() {
        // 显示正在上传的ProgressDialog
        // showProgressDialogWithText("上传头像中");
        String avatarFilePath=RongGenerate.generateDefaultAvatar(ConfigApplication.instance().mLoginUser.getUserId(),
                ConfigApplication.instance().mLoginUser.getNickName());
        File file=new File(avatarFilePath);
        if (file==null||!file.exists())return;
        Map<String,String> params=new HashMap<>();
        String loginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        params.put("userId", loginUserId);
        HttpUtils.getInstance().uploadFileWithParts(AppConfig.AVATAR_UPLOAD_URL, params, file, new HttpSubscriber() {
            @Override
            protected void onXError(String exception) {
                // dismissProgressDialog();
                T_.showToastReal("上传失败"+exception);
            }

            @Override
            protected void onFailure(String msg) {
                //dismissProgressDialog();
                T_.showToastReal("上传失败");
                //com.lsjr.zizi.loader.ImageLoader.getInstance().showfriendImage();
            }

            @Override
            protected void onSuccess(String response) {
                // dismissProgressDialog();
                L_.e("上传成功"+response);
                showProgressSuccess("注册成功");

                Bundle bundle=new Bundle();
                bundle.putBoolean("isLoginJump",true);
                openActivity(HomeActivity.class);
               // ImageLoader.getInstance().displayImage(cropImageUri.toString(), mAvatarImg);
            }
        });

    }


}


package com.lsjr.zizi.mvp.home.session;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.chat.helper.LoginHelper;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.HomeActivity;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.UIUtils;

import butterknife.BindView;


/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/2 15:55
 */

public class SplashActivity extends MvpActivity {
    @BindView(R.id.welcome_iv)
    ImageView welcomeIv;
    @BindView(R.id.welcome_ly)
    FrameLayout welcomeLy;
    @BindView(R.id.select_login_btn)
    Button selectLoginBtn;
    @BindView(R.id.select_register_btn)
    Button selectRegisterBtn;
    @BindView(R.id.select_lv)
    RelativeLayout selectLv;
    private long mStartTimeMs;// 记录进入该界面时间，保证至少在该界面停留3秒

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

        //setTheme(R.style.AppTheme);
        mStartTimeMs = System.currentTimeMillis();
        selectLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity(LoginActivity.class);
                //startActivity(new Intent(mContext, LoginActivity.class));

            }
        });
        selectRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(mContext, RegisterActivity.class));
                openActivity(RegisterActivity.class);
            }
        });
        selectLv.setVisibility(View.INVISIBLE);
        int userStatus = LoginHelper.prepareUser(UIUtils.getContext());
        int status = ConfigApplication.instance().mUserStatus;
        Intent intent = new Intent();
        switch (userStatus) {
            case LoginHelper.STATUS_USER_FULL:
            case LoginHelper.STATUS_USER_NO_UPDATE:
            case LoginHelper.STATUS_USER_TOKEN_OVERDUE:
            case LoginHelper.STATUS_USER_TOKEN_CHANGE:
                intent.setClass(SplashActivity.this, HomeActivity.class);
                //jumpToActivityAndClearTask(HomeActivity.class);
                startActivity(intent);
                finish();
                break;
            case LoginHelper.STATUS_USER_SIMPLE_TELPHONE:
                intent.setClass(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                //jumpToActivityAndClearTask(LoginActivity.class);
                break;
            case LoginHelper.STATUS_NO_USER:
            default:
                long currentTimeMs = System.currentTimeMillis();
                int useTime = (int) (currentTimeMs - mStartTimeMs);
                int delayTime = useTime > 2000 ? 0 : 2000 - useTime;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stay();
                    }
                }, delayTime);

        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // 停留在此界面
    private void stay() {
        selectLv.setVisibility(View.VISIBLE);
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.scale_in);
        selectLv.startAnimation(anim);
    }
}

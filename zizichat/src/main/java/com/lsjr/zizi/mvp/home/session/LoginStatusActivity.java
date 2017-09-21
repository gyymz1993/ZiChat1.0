package com.lsjr.zizi.mvp.home.session;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.helper.LoginHelper;
import com.lsjr.zizi.chat.helper.UserSp;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.ActivityUtils;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/17 14:30
 */

public class LoginStatusActivity extends MvpActivity {
    @BindView(R.id.title_tv)
    TextView mTitleTv;
    @BindView(R.id.des_tv)
    TextView mDesTv;
    @BindView(R.id.left_btn)
    Button leftBtn;
    @BindView(R.id.right_btn)
    Button rightBtn;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_loginstatus;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

        // 能进入此Activity的只允许三种用户状态
        int status = ConfigApplication.instance().mUserStatus;
        if (status == LoginHelper.STATUS_USER_TOKEN_OVERDUE) {
            mTitleTv.setText(R.string.overdue_title);
            mDesTv.setText(R.string.token_overdue_des);
        } else if (status == LoginHelper.STATUS_USER_NO_UPDATE) {
            mTitleTv.setText(R.string.overdue_title);
            mDesTv.setText(R.string.deficiency_data_des);
        } else if (status == LoginHelper.STATUS_USER_TOKEN_CHANGE) {
            mTitleTv.setText(R.string.logout_title);
            mDesTv.setText(R.string.logout_des);
        } else {// 其他的状态，一般不会出现，为了容错，加个判断
            loginAgain();
            return;
            // throw new IllegalStateException("用户状态错误");
        }

        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // ActivityStack.getInstance().exit();
                ActivityUtils.removeAllActivity();
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginAgain();
            }
        });
    }

    private void loginAgain() {
        boolean idIsEmpty = TextUtils.isEmpty(UserSp.getInstance().getUserId());
        boolean telephoneIsEmpty = TextUtils.isEmpty(UserSp.getInstance().getTelephone());
        if (!idIsEmpty && !telephoneIsEmpty) {//
            jumpToActivityAndClearTask(LoginActivity.class);
        } else {
            jumpToActivityAndClearTask(LoginActivity.class);
        }

    }

}

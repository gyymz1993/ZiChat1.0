package com.lsjr.zizi.mvp.home.session;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.EditText;

import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.UIUtils;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/24 11:11
 */

@SuppressLint("Registered")
public class UpdateInfoActivity extends MvpActivity {
    @BindView(R.id.etName)
    EditText etName;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected void initTitle() {
        setTitleText("修改名称");
        setTopLeftButton(R.drawable.ic_back).
                setTitleTextColor(UIUtils.getColor(R.color.white)).
                setBackgroundColor(UIUtils.getColor(R.color.colorPrimary));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_up_group_info;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

    }
}

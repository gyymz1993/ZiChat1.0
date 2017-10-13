package com.lsjr.zizi.mvp.frag;

import android.os.Bundle;

import com.example.smackdemo.MainActivity;
import com.lsjr.zizi.R;
import com.ymz.baselibrary.mvp.BaseMvpFragment;
import com.ymz.baselibrary.mvp.BasePresenter;


/**
 * Created by admin on 2017/5/11.
 */

public class FindFragment extends BaseMvpFragment {
    @Override
    protected BasePresenter createPresenter() {
        return null;
    }


    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initTitle() {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_find;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

    }

    @Override
    protected void lazyLoad() {
        openActivity(MainActivity.class);
    }
}

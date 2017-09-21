package com.lsjr.zizi.mvp.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpFragment;
import com.lsjr.zizi.mvp.home.session.FriendCircleActivity;
import com.lsjr.zizi.mvp.home.session.ScanActivity;
import com.lsjr.zizi.view.OptionItemView;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.widget.ItemView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/19 16:00
 */

public class WordFragment extends MvpFragment {
    @BindView(R.id.id_circle_firind)
    ItemView idCircleFirind;
    @BindView(R.id.oivScan)
    ItemView oivScan;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_word;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        idCircleFirind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity(FriendCircleActivity.class);
            }
        });

        idCircleFirind.setItemOnclickListener(v -> {
            openActivity(FriendCircleActivity.class);
        });

        oivScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity(ScanActivity.class);
            }
        });

        oivScan.setItemOnclickListener(v -> openActivity(ScanActivity.class));

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}

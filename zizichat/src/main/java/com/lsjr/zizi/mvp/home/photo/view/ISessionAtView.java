package com.lsjr.zizi.mvp.home.photo.view;


import android.widget.EditText;

import com.andview.autofresh.BGARefreshLayout;
import com.andview.myrvview.LQRRecyclerView;


public interface ISessionAtView {

    BGARefreshLayout getRefreshLayout();

    LQRRecyclerView getRvMsg();

    EditText getEtContent();
}

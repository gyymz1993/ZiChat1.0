package com.lsjr.zizi.mvp.home.session.adapter;

import android.content.Context;
import android.widget.TextView;

import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.PhoneInfo;

import java.util.List;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/2 10:15
 */

public class PhoneContactAdapter extends ABaseRefreshAdapter<PhoneInfo> {

    public PhoneContactAdapter(Context context, List<PhoneInfo> datas, int itemLayoutId) {
        super(context, datas, itemLayoutId);
    }

    @Override
    protected void convert(BaseRecyclerHolder viewHolder, PhoneInfo phoneInfo, int var3) {
        TextView tv_name = viewHolder.getView(R.id.tv_name);
        tv_name.setText(phoneInfo.getName());
    }
}

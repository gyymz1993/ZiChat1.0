package com.lsjr.zizi.mvp.home.session.cicle;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.PublicMessage;
import com.lsjr.zizi.mvp.home.session.presenter.CircleContract;
import com.ys.uilibrary.base.RVBaseAdapter;
import com.ys.uilibrary.base.RVBaseViewHolder;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/16 11:52
 */

public class URLCellAdapter extends ACicleAdapter {

    /** 链接的图片 */
    public ImageView urlImageIv;
    /** 链接的标题 */
    public TextView urlContentTv;
    private PublicMessage mBody;

    public URLCellAdapter(PublicMessage body,RVBaseAdapter rvBaseAdapter,CircleContract.CirclePresenter circlePresenter) {
        super(body,rvBaseAdapter,circlePresenter);
        mBody=body;
    }

    @Override
    public int getItemType() {
        return TYPE_URL;
    }

    public void initSubView(RVBaseViewHolder viewStub,int position) {
        if(viewStub == null){
            throw new IllegalArgumentException("viewStub is null...");
        }
        LinearLayout urlBodyView = (LinearLayout) viewStub.getView(R.id.urlBody);
        if(urlBodyView != null){
            urlImageIv = (ImageView) viewStub.getView(R.id.urlImageIv);
            urlContentTv = (TextView) viewStub.getView(R.id.urlContentTv);
        }
       // viewStub.getItemView().setVisibility(View.GONE);

    }

}

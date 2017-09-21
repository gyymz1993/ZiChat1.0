package com.lsjr.zizi.mvp.home.session.cicle;

import android.view.View;

import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.PublicMessage;
import com.lsjr.zizi.mvp.home.photo.ImagePagerActivity;
import com.lsjr.zizi.mvp.circledemo.bean.PhotoInfo;
import com.lsjr.zizi.mvp.circledemo.widgets.MultiImageView;
import com.lsjr.zizi.mvp.home.session.presenter.CircleContract;
import com.ymz.baselibrary.utils.UIUtils;
import com.ys.uilibrary.base.RVBaseAdapter;
import com.ys.uilibrary.base.RVBaseViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/16 14:29
 */

public class ImageCellAdapter extends ACicleAdapter {

    /** 图片*/
    private PublicMessage mBody;
    public ImageCellAdapter(PublicMessage body,RVBaseAdapter rvBaseAdapter,CircleContract.CirclePresenter circlePresenter) {
        super(body,rvBaseAdapter,circlePresenter);
        mBody=body;
    }

    @Override
    public int getItemType() {
        return TYPE_IMG;
    }

    @Override
    public void initSubView(RVBaseViewHolder viewStub,int position) {
        if(viewStub == null){
            throw new IllegalArgumentException("viewStub is null...");
        }
        MultiImageView multiImageView = (MultiImageView) viewStub.getView(R.id.multiImagView);
        PublicMessage.Body body = mBody.getBody();
        if (body.getImages() != null) {
            multiImageView.setVisibility(View.VISIBLE);
            List<PhotoInfo> lists=new ArrayList<>();
            if (body.getImages().size()>0&&body.getImages()!=null){
                for (PublicMessage.Resource resource:body.getImages()){
                    PhotoInfo photoInfo=new PhotoInfo();
                    if (body.getImages().size()<3){
                        photoInfo.url=resource.getOriginalUrl();
                    }else {
                        photoInfo.url=resource.getThumbnailUrl();
                    }
                    lists.add(photoInfo);
                }
            }

            multiImageView.setList(lists);
            multiImageView.setOnItemClickListener(new MultiImageView.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    //imagesize是作为loading时的图片size
                    ImagePagerActivity.ImageSize imageSize = new ImagePagerActivity.ImageSize(view.getMeasuredWidth(), view.getMeasuredHeight());
                    List<String> photoUrls = new ArrayList<>();
                    for(PublicMessage.Resource resource:body.getImages()){
                        photoUrls.add(resource.getOriginalUrl());
                        //photoUrls.add(resource.getThumbnailUrl());
                    }
                    ImagePagerActivity.startImagePagerActivity(UIUtils.getContext(), photoUrls, position, imageSize);
                }
            });
        } else {
            multiImageView.setVisibility(View.GONE);
        }
    }
}

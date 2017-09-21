package com.lsjr.zizi.mvp.home.session.cicle;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.PublicMessage;
import com.lsjr.zizi.mvp.circledemo.widgets.CircleVideoView;
import com.lsjr.zizi.mvp.home.session.presenter.CircleContract;
import com.ymz.baselibrary.utils.UIUtils;
import com.ys.uilibrary.base.RVBaseAdapter;
import com.ys.uilibrary.base.RVBaseViewHolder;

import java.util.List;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/16 14:29
 */

public class VideoCellAdapter extends ACicleAdapter {

    private PublicMessage mBody;
    private int curPlayIndex=-1;
    CircleVideoView videoView;
    public VideoCellAdapter(PublicMessage body,RVBaseAdapter rvBaseAdapter,CircleContract.CirclePresenter circlePresenter) {
        super(body,rvBaseAdapter,circlePresenter);
        this.mBody=body;
    }

    @Override
    public int getItemType() {
        return TYPE_VIDEO;
    }

    public void initSubView(RVBaseViewHolder viewStub,int position) {
        if(viewStub == null){
            throw new IllegalArgumentException("viewStub is null...");
        }
        videoView = (CircleVideoView) viewStub.getView(R.id.videoView);
        videoView.resetVideo();
        List<PublicMessage.Resource> videos = mBody.getBody().getVideos();
        List<PublicMessage.Resource> images = mBody.getBody().getImages();
       // Bitmap videoThumbnail = CacheUtils.getInstance().getCacheVideoImage(images.get(0).getOriginalUrl());
        if (images!=null&&images.size()!=0){
            String videoThumbnail = images.get(0).getOriginalUrl();
            if (!TextUtils.isEmpty(videoThumbnail)){
                videoView.setVideoImgUrl(videoThumbnail);//视频封面图片
            }
        }
        if (videos!=null&&videos.size()!=0){
            String videosUrl = videos.get(0).getOriginalUrl();
            if (!TextUtils.isEmpty(videosUrl)){
                videoView.setVideoUrl(videos.get(0).getOriginalUrl());
            }
        }
        videoView.setPostion(position);
        videoView.setOnPlayClickListener(new CircleVideoView.OnPlayClickListener() {
            @Override
            public void onPlayClick(int pos) {
                curPlayIndex = pos;
            }
        });
//        FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) videoView.getVideoButton().getLayoutParams();
//        layoutParams1.gravity= Gravity.CENTER;
//        ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
//        layoutParams.width= UIUtils.WHD()[0]/5*3;
//        layoutParams.height=UIUtils.dip2px(150);
    }


    @Override
    public void releaseResource() {
        videoView.videoStopped();
    }
}

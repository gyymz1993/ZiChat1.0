package com.lsjr.zizi.mvp.home.session.cicle;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.lsjr.callback.DownloadSubscriber;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.PublicMessage;
import com.lsjr.zizi.chat.dao.ChatMessageDao;
import com.lsjr.zizi.loader.ImageLoader;
import com.lsjr.zizi.mvp.circledemo.widgets.CircleVideoView;
import com.lsjr.zizi.mvp.home.session.presenter.CircleContract;
import com.lsjr.zizi.util.FileOpenUtils;
import com.lsjr.zizi.view.CircularProgressBar;
import com.ymz.baselibrary.AppCache;
import com.ymz.baselibrary.BaseApplication;
import com.ymz.baselibrary.utils.FileUtils;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ys.uilibrary.base.RVBaseAdapter;
import com.ys.uilibrary.base.RVBaseViewHolder;

import java.io.File;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/16 14:29
 */

public class VideoCellAdapter1 extends ACicleAdapter {

    private PublicMessage mBody;
    public VideoCellAdapter1(PublicMessage body, RVBaseAdapter rvBaseAdapter, CircleContract.CirclePresenter circlePresenter) {
        super(body,rvBaseAdapter,circlePresenter);
        this.mBody=body;
    }

    @Override
    public int getItemType() {
        return TYPE_VIDEO;
    }

    @SuppressLint("RtlHardcoded")
    public void initSubView(RVBaseViewHolder viewStub, int position) {
        if(viewStub == null){
            throw new IllegalArgumentException("viewStub is null...");
        }
        ImageView igvideoThumbnail = (ImageView) viewStub.getView(R.id.iv_video_frame);
        ImageView igvideoPlay = (ImageView) viewStub.getView(R.id.iv_video_play);
        CircularProgressBar circularProgressBar = (CircularProgressBar) viewStub.getView(R.id.video_progress);
        List<PublicMessage.Resource> videos = mBody.getBody().getVideos();
        List<PublicMessage.Resource> images = mBody.getBody().getImages();
       // Bitmap videoThumbnail = CacheUtils.getInstance().getCacheVideoImage(images.get(0).getOriginalUrl());
        if (images!=null&&images.size()!=0){
            String videoThumbnail = images.get(0).getThumbnailUrl();
            if (!TextUtils.isEmpty(videoThumbnail)){
                //视频封面图片
                ImageLoader.getInstance().showRealSizeImage(videoThumbnail,igvideoThumbnail);
            }
        }
       // videoView.setPostion(position);
        igvideoPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //T_.showToastReal("点击播放");
                //igvideoPlay.setVisibility(GONE);
                String videosUrl = videos.get(0).getOriginalUrl();
                boolean downLoad=true;
                if (!TextUtils.isEmpty(videosUrl)&&AppCache.getInstance().videoExists(AppCache.getInstance().mVideosDir, FileUtils.getFileName(videosUrl)))
                {
                    downLoad = false;
                    L_.e("文件已存在---->"+AppCache.getInstance().videoPath(FileUtils.getFileName(videosUrl)));
                    FileOpenUtils.openFile(UIUtils.getContext(), AppCache.getInstance().videoPath(FileUtils.getFileName(videosUrl)));
                }
                if (downLoad) {
                    circularProgressBar.setProgress(0);
                    circularProgressBar.setMax(100);
                    circularProgressBar.setVisibility(VISIBLE);
                    HttpUtils.getInstance().downloadFile(videosUrl, new DownloadSubscriber(BaseApplication.getApplication(),
                            AppCache.getInstance().mVideosDir, FileUtils.getFileName(videosUrl)) {
                        @Override
                        public void onComplete(String path) {
                             L_.e("文件下载成功保存路径：" + path);
                            circularProgressBar.setVisibility(GONE);
                            FileOpenUtils.openFile(UIUtils.getContext(), path);
                        }
                        @Override
                        public void update(long bytesRead, long contentLength, boolean done) {
                            int progress = (int) (bytesRead * 100 / contentLength);
                            L_.e(progress + "% ");
                            circularProgressBar.setProgress(progress);
                        }

                        @Override
                        protected void onXError(String exception) {
                            L_.e(exception + "% ");

                        }
                    });

                }
            }
        });
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) igvideoThumbnail.getLayoutParams();
        //layoutParams.width= UIUtils.WHD()[0]/5*3;
        layoutParams.height=UIUtils.dip2px(180);
       // layoutParams.gravity= Gravity.CENTER;

        //FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) igvideoPlay.getLayoutParams();
        //layoutParams1.gravity= Gravity.CENTER;
    }

}

package com.lsjr.zizi.mvp.upload;

/**
 * Created by admin on 2017/5/6.
 */

public interface FreshImaCallBack {

    void previewImag(int position);//图片预览

    void updateGvIgShow(int postition); //刷新GridView

    void openGallery();  //打开相册
}

package com.lsjr.zizi.mvp.upload;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.lsjr.callback.HttpSubscriber;
import com.lsjr.param.ProgressResponseCallBack;
import com.lsjr.param.RxHttpParams;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.yanzhenjie.album.Album;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by admin on 2017/5/6.
 */

public class UploadActivity extends MvpActivity<UploadPresenter> implements FreshImaCallBack, IUploadView {

    private static final int REQUEST_CODE_GALLERY = 100;  //打开相册
    private static final int REQUEST_CODE_PREVIEW = 101; //图片预览
    private final static int maxImageSize = 5;
    private GridView gvImage;
    private ImgGridAdapter adapter;
    private ArrayList<String> igList = new ArrayList<>();

    LinearLayout llSend;
    List<File> fileList = new ArrayList<>();


    @Override
    protected void initData() {
       // setTitleText("图片选择器");
    }

    @Override
    protected UploadPresenter createPresenter() {
        return new UploadPresenter(this);
    }

    public void uploadImageFiles() {
        for (String image : igList) {
            //创建 一个压缩后的路径
            Bitmap bitmap = BitmapFactory.decodeFile(image);
            File file = new File(image);
            L_.e("压缩前：图片大小" + file.length() / 1024 + "k");
            compressWithLs(file);
            fileList.add(file);
        }
        Log.d("roamer", "开始上传...");
        Map<String,String> params=new HashMap<>();
        final String loginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        params.put("userId", loginUserId);
        params.put("access_token", ConfigApplication.instance().mAccessToken);
//        HttpUtils.getInstance().uploadFileWithParts(AppConfig.UPLOAD_URL, params, fileList, new HttpSubscriber() {
//            @Override
//            protected void onXError(String exception) {
//
//            }
//
//            @Override
//            protected void onFailure(String msg) {
//
//            }
//
//            @Override
//            protected void onSuccess(String response) {
//
//            }
//        });


        RxHttpParams params1=new RxHttpParams();
        //params1.put("avatar", fileList.get(0));,
        params1.put("userId", loginUserId);
        params1.put("access_token", ConfigApplication.instance().mAccessToken);
        L_.e(fileList.get(0).getAbsolutePath());
        params1.put("file1", fileList.get(0), fileList.get(0).getName(),new ProgressResponseCallBack() {
            @Override
            public void onResponseProgress(long bytesRead, long contentLength, boolean done) {
                int progress = (int) (bytesRead * 100 / contentLength);
                L_.i("progress=" + progress);
                if (done){
                    L_.e("上传完成");
                }
            }
        });
        HttpUtils.getInstance().uploadFileWithParts(AppConfig.UPLOAD_URL,params1,new HttpSubscriber(){

            @Override
            protected void onXError(String exception) {
                T_.showToastReal("上传失败"+exception);
            }

            @Override
            protected void onFailure(String msg) {
                T_.showToastReal("上传失败"+msg);
            }

            @Override
            protected void onSuccess(String response) {
                T_.showToastReal("上传成功"+response);
                // mService.sendChatMessage(mFriend.getUserId(), message);
            }
        });



    }


    


    /**
     * 压缩单张图片 Listener 方式
     */
    private void compressWithLs(File file) {

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_GALLERY:
                if (resultCode == RESULT_OK) {
                    ArrayList<String> pathList = Album.parseResult(data);
                    //L_.e(pathList.toString());
                    igList.clear();//不可直接指向
                    igList.addAll(pathList);
                    L_.e(igList.toString());
                    adapter.notifyDataSetChanged();
                }
                break;
        }
    }


    @Override
    protected int getLayoutId() {
        return R.layout.upload_main;
    }

    @Override
    protected void afterCreate(Bundle bundle) {
        gvImage = (GridView) findViewById(R.id.gvImage);
        llSend = (LinearLayout) findViewById(R.id.ll_send);
        adapter = new ImgGridAdapter(this, igList, maxImageSize);
        adapter.setImgShowFresh(this);//实现刷新接口
        gvImage.setAdapter(adapter);
        llSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImageFiles();
            }
        });
    }


    @Override
    public void previewImag(int position) {
        Album.gallery(this)//预览图片
                .requestCode(REQUEST_CODE_PREVIEW)
                .toolBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .statusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .checkedList(igList)
                .currentPosition(position)
                .checkFunction(false)
                .start();
    }

    @Override
    public void updateGvIgShow(int postition) {
        igList.remove(postition);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void openGallery() {
        Album.album(this)//打开相册
                .requestCode(REQUEST_CODE_GALLERY)
                .toolBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .statusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .selectCount(maxImageSize)
                .columnCount(3)
                .camera(true)
                .checkedList(igList)
                .start();
    }

    @Override
    public void onUploadSucceed(boolean isSucceeds) {
        //FileUtil.deleteFile(FileUtil.getTempPahtFile());
        L_.e("onUploadSucceed" + isSucceeds);
    }
}

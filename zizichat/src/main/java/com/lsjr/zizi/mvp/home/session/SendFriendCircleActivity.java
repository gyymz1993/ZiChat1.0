package com.lsjr.zizi.mvp.home.session;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.alibaba.fastjson.JSON;
import com.lsjr.bean.ObjectResult;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.bean.UploadFileResult;
import com.lsjr.zizi.chat.db.Area;
import com.lsjr.zizi.chat.helper.LoginHelper;
import com.lsjr.zizi.http.UploadService;
import com.lsjr.zizi.mvp.home.session.adapter.SelectPhotoAdapter;
import com.lsjr.zizi.mvp.upload.FreshImaCallBack;
import com.lsjr.zizi.util.CameraUtil;
import com.lsjr.zizi.util.DeviceInfoUtil;
import com.lsjr.zizi.view.CustomVideoView;
import com.yanzhenjie.album.Album;
import com.ymz.baselibrary.image.Compressor;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.CacheUtils;
import com.ymz.baselibrary.utils.FileUtils;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/24 14:31
 */

public class SendFriendCircleActivity extends MvpActivity implements FreshImaCallBack {

    private static final int REQUEST_CODE_GALLERY = 100;  //打开相册
    private static final int REQUEST_CODE_PREVIEW = 101; //图片预览
    private final static int maxImageSize = 9;
    @BindView(R.id.id_gr_view)
    RecyclerView idGrView;
    @BindView(R.id.id_ed_text)
    EditText editText;

    @BindView(R.id.show_videoview)
    CustomVideoView customVideoView;

    private ArrayList<String> igList;
    SelectPhotoAdapter adapter;
    private String mImageData;
    private int mType;
    private String videoFile;
    Bitmap  videoThumbnail;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent() != null) {
            mType = getIntent().getIntExtra("type", 0);
            videoFile = getIntent().getStringExtra("videoFile");
        }
        super.onCreate(savedInstanceState);

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_send_circle;
    }


    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        if (mType!=4){
            igList = new ArrayList<>();
            igList.add("");
            adapter=new SelectPhotoAdapter(UIUtils.getContext(),igList,R.layout.item_select_photo);
            adapter.setImgShowFresh(this);
            GridLayoutManager gridLayoutManager=new GridLayoutManager(UIUtils.getContext(),4);
            idGrView.setLayoutManager(gridLayoutManager);
            idGrView.setAdapter(adapter);
            customVideoView.setVisibility(View.GONE);
        }else {
            idGrView.setVisibility(View.GONE);
            customVideoView.setVisibility(View.VISIBLE);
            customVideoView.setVideoPath(videoFile);
            videoThumbnail = CacheUtils.getInstance().getCacheVideoImage(videoFile);
            //customVideoView.start();
            customVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setVolume(0f, 0f);
                    mp.start();
                    customVideoView.start();
                }
            });

        }
    }


    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("发朋友圈");
        setTopLeftButton(R.drawable.ic_back);
        getToolBarView().getRightTextView().setVisibility(View.VISIBLE);
        getToolBarView().getRightTextView().setText("确定");
       // getToolBarView().setRightText("确定").setVisibility(View.VISIBLE);
        getToolBarView().getRightTextView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                T_.showToastReal("发布朋友圈");
                if (mType==4){
                    //T_.showToastReal("发布视频");
                    showProgressDialogWithText("发布视频");
                    new UploadVideo().execute();
                }else {
                    if (igList.size() <= 0 && TextUtils.isEmpty(editText.getText().toString())) {// 没有照片，也没有说说，直接返回
                        return;
                    }
                    if (igList.size() <= 1) {// 发文字
                        // T_.showToastReal("发布文字");
                        showProgressDialogWithText("发布文字");
                        sendShuoshuo();
                    } else {// 布图片+文字
                        // T_.showToastReal("发布图文");
                        showProgressDialogWithText("发布图文");
                        new UploadPhpto().execute();
                    }
                }

            }
        });
    }

    @Override
    protected void initView() {



    }


    // 发布一条说说
    public void sendShuoshuo() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        // 消息类型：1=文字消息；2=图文消息；3=语音消息；4=视频消息；
        if (mType!=4){
            if (TextUtils.isEmpty(mImageData)) {
                params.put("type", "1");
            } else {
                params.put("type", "2");
            }
        }else {
            params.put("type", "4");
        }

        // 消息标记：1：求职消息；2：招聘消息；3：普通消息；
        params.put("flag", "3");
        // 消息隐私范围 0=不可见；1=朋友可见；2=粉丝可见；3=广场
        params.put("visible", "3");
        params.put("text", editText.getText().toString());// 消息内容
        if (!TextUtils.isEmpty(mImageData)) {
            params.put("images", mImageData);
        }
        if (!TextUtils.isEmpty(mVideoData)) {
            params.put("videos", mVideoData);
        }
        // 附加信息
        params.put("model", DeviceInfoUtil.getModel());
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        params.put("serialNumber", DeviceInfoUtil.getDeviceId(UIUtils.getContext()));
        double latitude = ConfigApplication.instance().getmLatitude();
        double longitude = ConfigApplication.instance().getmLongitude();

        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));

        String address = "湖北";
        if (!TextUtils.isEmpty(address))
            params.put("location", address);

        Area area = Area.getDefaultCity();
        if (area != null) {
            params.put("cityId", String.valueOf(area.getId()));// 城市Id
        } else {
            params.put("cityId", "0");
        }

       // showProgressDialogWithText("发布朋友圈");

        HttpUtils.getInstance().postServiceData(AppConfig.MSG_ADD_URL, params, new ChatObjectCallBack<String>(String.class) {

            @Override
            protected void onXError(String exception) {
                dismissProgressDialog();
                T_.showToastReal(exception);
            }

            @Override
            protected void onSuccess(ObjectResult<String> result) {
                dismissProgressDialog();
                boolean parserResult = ResultCode.defaultParser( result, true);
                if (parserResult) {
                    Intent intent = new Intent();
                    intent.putExtra(Constants.EXTRA_MSG_ID, result.getData());
                    setResult(RESULT_OK, intent);
                    finish();
                }

            }
        });

    }

    private String mVideoData;
    @SuppressLint("StaticFieldLeak")
    private class UploadVideo extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * 上传的结果： <br/>
         * return 1 Token过期，请重新登陆 <br/>
         * return 2 视频为空，请重新录制 <br/>
         * return 3 上传出错<br/>
         * return 4 上传成功<br/>
         */
        @Override
        protected Integer doInBackground(Void... params) {
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }
            if (TextUtils.isEmpty(videoFile)) {
                return 2;
            }
            return upLoadVideoStatus();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == 1) {

            } else if (result == 2) {

            } else if (result == 3) {

            } else {
                sendShuoshuo();
            }

        }
    }



    @SuppressLint("StaticFieldLeak")
    private class UploadPhpto extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * 上传的结果： <br/>
         * return 1 Token过期，请重新登陆 <br/>
         * return 2 上传出错<br/>
         * return 3 上传成功<br/>
         */
        @Override
        protected Integer doInBackground(Void... params) {
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }
            return upLoadImageStatus();
        }


        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == 1) {

            } else if (result == 2) {
                T_.showToastReal("上传失败");
            } else {
                sendShuoshuo();
            }
        }

    }



    private int upLoadVideoStatus(){
        // 保存视频缩略图至sd卡
        // 保存视频缩略图至sd卡
        String imageSavePsth = CameraUtil.getOutputMediaFileUri(SendFriendCircleActivity.this, CameraUtil.MEDIA_TYPE_IMAGE).getPath();
        if (!FileUtils.saveBitmapToFile(videoThumbnail, imageSavePsth)) {// 保存缩略图失败
            return 3;
        }
        Map<String, String> mapParams = new HashMap<>();
        mapParams.put("userId", ConfigApplication.instance().mLoginUser.getUserId() + "");
        mapParams.put("access_token", ConfigApplication.instance().mAccessToken);
        List<String> dataList = new ArrayList<>();
        dataList.add(videoFile);
        if (!TextUtils.isEmpty(imageSavePsth)) {
            dataList.add(imageSavePsth);
        }
        String result = new UploadService().uploadFile(AppConfig.UPLOAD_URL, mapParams, dataList);
        if (TextUtils.isEmpty(result)) {
            return 3;
        }
        UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
        boolean success = ResultCode.defaultParser(recordResult, true);
        if (success) {
            if (recordResult.getSuccess() != recordResult.getTotal()) {// 上传丢失了某些文件
                return 3;
            }
            if (recordResult.getData() != null) {
                UploadFileResult.Data data = recordResult.getData();
                if (data.getVideos() != null && data.getVideos().size() > 0) {
                    while (data.getVideos().size() > 1) {// 因为正确情况下只有一个视频，所以要保证只有一个视频
                        data.getVideos().remove(data.getVideos().size() - 1);
                    }
                    data.getVideos().get(0).setSize(new File(videoFile).length());
                    //data.getVideos().get(0).setLength(mTimeLen);
                    mVideoData = JSON.toJSONString(data.getVideos(), UploadFileResult.sAudioVideosFilter);
                } else {
                    return 3;
                }
                if (data.getImages() != null && data.getImages().size() > 0) {
                    mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
                }

                Log.d("roamer", "mVideoData:" + mVideoData);
                Log.d("roamer", "mImageData:" + mImageData);
                return 4;
            } else {// 没有文件数据源，失败
                return 3;
            }
        } else {
            return 3;
        }
    }
    private int upLoadImageStatus(){
        Map<String, String> mapParams = new HashMap<>();
        mapParams.put("userId", ConfigApplication.instance().mLoginUser.getUserId() + "");
        mapParams.put("access_token",ConfigApplication.instance().mAccessToken);
        String result = new UploadService().uploadFile(AppConfig.UPLOAD_URL, mapParams, igList);
        L_.e( "上传图片消息：" + result);
        if (TextUtils.isEmpty(result)) {
            return 2;
        }
        UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
        boolean success = ResultCode.defaultParser(recordResult, true);
        if (success) {
            if (recordResult.getSuccess() != recordResult.getTotal()) {// 上传丢失了某些文件
                return 2;
            }
            if (recordResult.getData() != null) {
                L_.e("------------->"+recordResult.getData().toString());
                UploadFileResult.Data data = recordResult.getData();
                if (data.getImages() != null && data.getImages().size() > 0) {
                    mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
                }
                Log.d("roamer", "mImageData:" + mImageData);
                return 3;
            } else {// 没有文件数据源，失败
                return 2;
            }
        } else {
            return 2;
        }
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
    public void updateGvIgShow(int postition) {
        igList.remove(postition);
        adapter.notifyDataSetChanged();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_GALLERY:
                if (resultCode == RESULT_OK) {
                    ArrayList<String> pathList = Album.parseResult(data);
                    ArrayList<String> comPahtList = new ArrayList<>();
                    for (int i=0;i<pathList.size();i++){
                       // File newFile = CompressHelper.getDefault(this).compressToFile( new File(pathList.get(i)));
                        try {
                            File  compressedImageFile = new Compressor(this).compressToFile(new File(pathList.get(i)));
                            comPahtList.add(compressedImageFile.getAbsolutePath());
                        } catch (IOException e) {
                            e.printStackTrace();
                            comPahtList.add(new File(pathList.get(i)).getAbsolutePath());
                        }

                    }
                    //L_.e(pathList.toString());
                    igList.clear();//不可直接指向
                    igList.addAll(comPahtList);
                    igList.add("");
                    adapter.notifyDataSetChanged();
                }
                break;
        }
    }



}

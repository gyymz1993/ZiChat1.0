package com.lsjr.zizi.mvp.home.photo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;

import com.cjt2325.cameralibrary.JCameraView;
import com.cjt2325.cameralibrary.lisenter.JCameraLisenter;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.ymz.baselibrary.AppCache;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import kr.co.namee.permissiongen.PermissionGen;


/**
 * 拍照界面
 */
public class TakePhotoActivity extends MvpActivity {

    private JCameraView mJCameraView;
    public void init() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            PermissionGen.with(this)
                    .addRequestCode(100)
                    .permissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
                    .request();
    }


    int takeMode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras!=null){
            takeMode = extras.getInt("take",0);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void initView() {
        init();

       // L_.e("机型"+DeviceUtil.getDeviceInfo());
        mJCameraView = (JCameraView) findViewById(R.id.cameraview);
        //(0.0.7+)设置视频保存路径（如果不设置默认为Environment.getExternalStorageDirectory().getPath()）
       // mJCameraView.setSaveVideoPath(Environment.getExternalStorageDirectory().getPath());
       // mJCameraView.setSaveVideoPath(AppCache.getInstance().mVideosDir);

       //设置视频保存路径
        mJCameraView.setSaveVideoPath(AppCache.getInstance().mAppDir+ File.separator + "JCamera");
        //设置只能录像或只能拍照或两种都可以（默认两种都可以）
       // mJCameraView.setFeatures(JCameraView.BUTTON_STATE_ONLY_CAPTURE);

        if(takeMode!=0){
            mJCameraView.setFeatures(takeMode);
        }
        L_.e("当前的选择类型"+takeMode);
        //设置视频质量
       // mJCameraView.setMediaQuality(JCameraView.MEDIA_QUALITY_MIDDLE);

        //(0.0.8+)设置手动/自动对焦，默认为自动对焦
       // mJCameraView.setAutoFoucs(false);
        //设置小视频保存路径
//        File file = new File(AppCache.getInstance().mVideosDir);
//        if (!file.exists())
//            file.mkdirs();
//        mJCameraView.setSaveVideoPath(AppCache.getInstance().mVideosDir);
        initListener();
    }


    @Override
    protected boolean isImmersionBarEnabled() {
        return true;
    }

    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("拍摄");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_take_photo;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

    }

    public void initListener() {

        //JCameraView监听
//        mJCameraView.setErrorLisenter(new ErrorLisenter() {
//            @Override
//            public void onError() {
//                //打开Camera失败回调
//                L_.e("CJT", "open camera error");
//            }
//            @Override
//            public void AudioPermissionError() {
//                //没有录取权限回调
//                L_.e("CJT", "AudioPermissionError");
//            }
//        });



        mJCameraView.setJCameraLisenter(new JCameraLisenter() {
            @Override
            public void captureSuccess(Bitmap bitmap) {
                //获取到拍照成功后返回的Bitmap
                String path = saveBitmap(bitmap, AppCache.getInstance().mPicturesDir);
                Intent data = new Intent();
                data.putExtra("take_photo", true);
                data.putExtra("path", path);
                setResult(RESULT_OK, data);
                finish();
            }

            @Override
            public void recordSuccess(String path) {
                //获取成功录像后的视频路径
                Intent data = new Intent();
                data.putExtra("take_photo", false);
                data.putExtra("path", path);
                setResult(RESULT_OK, data);
                finish();
            }

//            @Override
//            public void recordSuccess(String path, Bitmap bitmap) {
//
//            }

            @Override
            public void quit() {
                //返回按钮的点击时间监听
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mJCameraView != null)
            mJCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mJCameraView != null)
            mJCameraView.onPause();
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }



    public String saveBitmap(Bitmap bm, String dir) {
        String path = "";
        File f = new File(dir, "CSDN_LQR_" + SystemClock.currentThreadTimeMillis() + ".png");
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            path = f.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }
}

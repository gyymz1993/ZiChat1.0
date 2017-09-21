package com.lsjr.zizi.mvp.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.lqr.emoji.LQREmotionKit;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.loader.ImageLoader;
import com.lqr.imagepicker.view.CropImageView;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.ymz.baselibrary.AppCache;
import com.ymz.baselibrary.BaseApplication;
import com.ymz.baselibrary.utils.L_;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/4 9:29
 */

public class AppService extends IntentService {

    public AppService() {
        super("AppService");
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, AppService.class);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        Fresco.initialize(getApplication());
       // DcodeService.initialize(this);
        BaseApplication.instance().registerUiScreen(720,1280);
        BaseApplication.instance().initLeakCanary();

       // LeakCanary.install((Application) mContext);
        AppCache.getInstance().initCreateAppDir();
        //初始化表情控件
        LQREmotionKit.init(getApplication(), (mContext, path, imageView) -> Glide.with(getApplication()).load(path).centerCrop().diskCacheStrategy(DiskCacheStrategy.SOURCE).into(imageView));
        initImagePicker();
        // test();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .threadPriority(Thread.NORM_PRIORITY - 2).threadPoolSize(4).build();
        // Initialize ImageLoader with configuration.
        com.nostra13.universalimageloader.core.ImageLoader.getInstance().init(config);


        L_.e("AppService  onHandleIntent初始化成功");
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            L_.e("LeakCanary  初始化失败");
//                        return;
//        }

       // LeakCanary.install(getApplication());

    }


    /**
     * 初始化仿微信控件ImagePicker
     */
    private void initImagePicker() {
        ImagePicker imagePicker = ImagePicker.getInstance();
        imagePicker.setImageLoader(new ImageLoader() {
            @Override
            public void displayImage(Activity activity, String path, ImageView imageView, int width, int height) {
                Glide.with(getApplicationContext()).load(Uri.parse("file://" + path).toString()).centerCrop().into(imageView);
            }

            @Override
            public void clearMemoryCache() {

            }
        });   //设置图片加载器
        imagePicker.setShowCamera(true);  //显示拍照按钮
        imagePicker.setCrop(true);        //允许裁剪（单选才有效）
        imagePicker.setSaveRectangle(true); //是否按矩形区域保存
        imagePicker.setSelectLimit(9);    //选中数量限制
        imagePicker.setStyle(CropImageView.Style.RECTANGLE);  //裁剪框的形状
        imagePicker.setFocusWidth(800);   //裁剪框的宽度。单位像素（圆形自动取宽高最小值）
        imagePicker.setFocusHeight(800);  //裁剪框的高度。单位像素（圆形自动取宽高最小值）
        imagePicker.setOutPutX(1000);//保存文件的宽度。单位像素
        imagePicker.setOutPutY(1000);//保存文件的高度。单位像素
    }


    private void handleGirlItemData() {

       // EventBus.getDefault().post();
    }

}

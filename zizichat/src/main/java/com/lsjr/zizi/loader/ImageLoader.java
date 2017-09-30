package com.lsjr.zizi.loader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.db.ChatMessage;
import com.ymz.baselibrary.AppCache;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/17 10:27
 */

public class ImageLoader {
    private static final ImageLoader ourInstance = new ImageLoader();

    public static ImageLoader getInstance() {
        return ourInstance;
    }


    public void laal(ChatMessage message,ImageView bivPic){
        int imageWidth = message.getWidth();
        int imageHeight = message.getHeight();
        int height,width;
        if (imageWidth>UIUtils.dip2px(150) && imageWidth > imageHeight ){
            width = UIUtils.dip2px(150);
            height=UIUtils.dip2px(80);
        } else if (imageHeight > UIUtils.dip2px(150)&&imageHeight> imageWidth ){
            width = UIUtils.dip2px(80);
            height=UIUtils.dip2px(150);
        }else if (imageHeight>UIUtils.dip2px(150) && imageWidth > UIUtils.dip2px(150)&&imageHeight==imageWidth){
            width = UIUtils.dip2px(150);
            height=UIUtils.dip2px(150);
        }else {
            L_.e("原始图片"+imageWidth+"  imageHeight ---->"+imageHeight);
            width=imageWidth;
            height=imageHeight;
        }

        L_.e("显示图片width："+width+"height  -->"+height);
        if (message.getFilePath()!=null&&new File(message.getFilePath()).exists()){
            //  ImageLoader.getInstance().show(message.getFilePath(),bivPic);
            ImageLoader.getInstance().showImage(message.getFilePath(),bivPic,R.mipmap.default_img,width==0?UIUtils.dip2px(150):width,height==0?UIUtils.dip2px(80):height);
        }else {
            //ImageLoader.getInstance().show(message.getContent(),bivPic);
            ImageLoader.getInstance().showImage(message.getContent(),bivPic,R.mipmap.default_img,width==0?UIUtils.dip2px(150):width,height==0?UIUtils.dip2px(80):height);
        }

    }
    private ImageLoader() {
    }

    public void show(String url,ImageView imageView){
        //imageView.setTag(url);
        Glide.with(UIUtils.getContext()).load(url).asBitmap().skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.ALL).thumbnail(0.1f).dontAnimate().into(new SimpleTarget<Bitmap>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                int imageWidth = resource.getWidth();
                int imageHeight = resource.getHeight();
                L_.e("原始图片"+imageWidth+"  imageHeight ---->"+imageHeight);
                int height,width;
                if (imageWidth>50 && imageWidth > imageHeight ){
                    width = UIUtils.dip2px(130);
                    height=UIUtils.dip2px(90);
                } else if (imageHeight > 50&&imageHeight> imageWidth ){
                    width = UIUtils.dip2px(90);
                    height=UIUtils.dip2px(130);
                }else if (imageHeight>50&& imageWidth > 50&&imageHeight==imageWidth){
                    width = UIUtils.dip2px(80);
                    height=UIUtils.dip2px(80);
                }else {
                    width=imageWidth;
                    height=imageHeight;
                }
                ViewGroup.LayoutParams para = imageView.getLayoutParams();
                para.height = height;
                para.width = width;
                imageView.setImageBitmap(resource);
//                if(resource != null && !resource.isRecycled()){
//                    resource.recycle();
//                    resource = null;
//                }
//                System.gc();
            }
        });
    }




    public void showRealSizeImage(String url,ImageView imageView){
        Glide.with(UIUtils.getContext()).load(url).override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(new ColorDrawable(UIUtils.getColor(R.color.gray7))).into(imageView);
    }

    public void showRealSizeImage(Uri url,ImageView imageView){
        Glide.with(UIUtils.getContext()).load(url).override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(new ColorDrawable(UIUtils.getColor(R.color.gray7))).
                into(imageView);

//        Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(false).
//                diskCacheStrategy(DiskCacheStrategy.SOURCE)
//                .override(width,height).
//                error(errorResId).centerCrop().into(imageView);
    }

    public void showRealWHImage(String url,ImageView imageView,ProgressBar mProgressBar){
        Glide.with(UIUtils.getContext()).load(url).asBitmap().into(new SimpleTarget<Bitmap>() {
            @SuppressLint("NewApi")
            @Override
            public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
                //BitmapDrawable drawable = new BitmapDrawable(UIUtils.getResources(),bitmap);
                imageView.setImageBitmap(bitmap);
                mProgressBar.setVisibility(View.GONE);
            }
        }); //方法中设置asBitmap可以设置回调类型
    }


    public void showVideoImage(String url, ImageView imageView,int errorResId) {
        Glide.with(UIUtils.getContext()).load(Uri.fromFile(new File(url))).into(imageView);
    }


    public File comPressImage(String imgName,File oldFile){
//        File newFile = new CompressHelper.Builder(UIUtils.getContext())
//                .setMaxWidth(720)  // 默认最大宽度为720
//                .setMaxHeight(960) // 默认最大高度为960
//                .setQuality(80)    // 默认压缩质量为80
//                .setFileName(imgName) // 设置你需要修改的文件名
//                .setCompressFormat(Bitmap.CompressFormat.JPEG) // 设置默认压缩为jpg格式
//                .setDestinationDirectoryPath(AppCache.getInstance().mPicturesDir)
//                .build()
//                .compressToFile(oldFile);

       // File newFile = CompressHelper.getDefault(UIUtils.getContext()).compressToFile(AppCache.getInstance().getmPicturesDir());

        /*Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).getAbsolutePath()*/
        //return newFile;
        return null;
    }


//    .placeholder(R.mipmap.default_img)
    public void showImage(String url, ImageView imageView,int errorResId) {
        Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.ALL).
                error(errorResId).placeholder(errorResId).centerCrop().into(imageView);
    }


    //    .placeholder(R.mipmap.default_img)
    public void showMultiImageView(String url, ImageView imageView,int errorResId) {
        Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.ALL).
                error(errorResId).placeholder(errorResId).into(imageView);
    }


    //    .placeholder(R.mipmap.default_img)
    public void showMultiImageView1(String url, ImageView imageView,int errorResId) {
        Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.ALL).
                error(errorResId).placeholder(errorResId).dontAnimate().into(imageView);
    }


    public void showImage(String url, ImageView imageView,int errorResId,int width,int height) {
        //L_.e("showImage------------>"+returnBitmap(url).getWidth()+"----------->"+returnBitmap(url).getHeight());
        Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(new ColorDrawable(0xffdcdcdc))
                .dontAnimate().override(width,height).
                error(errorResId).centerCrop().into(imageView);
        //.animate(android.R.anim.slide_in_left)
    }

    public void showImage(Uri url, ImageView imageView,int errorResId,int placeResId) {
        Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.SOURCE).placeholder(placeResId)
                .error(errorResId).centerCrop().into(imageView);
    }

    public void showImage(String url, ImageView imageView,int errorResId,int placeResId) {
        Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.SOURCE).placeholder(new ColorDrawable(UIUtils.getColor(R.color.gray7)))
                .error(errorResId).centerCrop().into(imageView);
    }


    //    .placeholder(R.mipmap.default_img)
    public void showImage(Uri url, ImageView imageView, int errorResId, int width, int height) {
        Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.SOURCE).skipMemoryCache(true)
                .override(width,height).
                error(errorResId).placeholder(new ColorDrawable(UIUtils.getColor(R.color.gray7))).centerCrop().into(imageView);
    }

    public void showVideo(String url, ImageView imageView) {
        Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(false).
                diskCacheStrategy(DiskCacheStrategy.SOURCE).centerCrop().into(imageView);
    }
    //    .placeholder(R.mipmap.default_img)
    public void showfriendImage(String url, ImageView imageView) {
        Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(true).
                diskCacheStrategy(DiskCacheStrategy.ALL).centerCrop().into(imageView);
    }



    public static void load(Context context, String url, ImageView iv) {
        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)//让Glide既缓存全尺寸图片，下次在任何ImageView中加载图片的时候，全尺寸的图片将从缓存中取出，重新调整大小，然后缓存
                .crossFade()
                .into(iv);
    }

    public static void load(Context context, String url, ImageView iv, int placeholder) {
        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)//让Glide既缓存全尺寸图片，下次在任何ImageView中加载图片的时候，全尺寸的图片将从缓存中取出，重新调整大小，然后缓存
                .crossFade()
                .placeholder(placeholder)
                .into(iv);
    }

    public static void load(Context context, int resId, ImageView iv) {
        Glide.with(context)
                .load(resId)
                .crossFade()
                .into(iv);
    }

    /**
     * 需要在子线程执行
     *
     * @param url
     * @return
     */
    public static Bitmap load( String url) {
        try {
            return Glide.with(UIUtils.getContext())
                    .load(url)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 根据图片的url路径获得Bitmap对象
     * @param url
     * @return
     */
    private Bitmap returnBitmap(String url) {
        URL fileUrl = null;
        Bitmap bitmap = null;
        try {
            fileUrl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) fileUrl
                    .openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;

    }


}

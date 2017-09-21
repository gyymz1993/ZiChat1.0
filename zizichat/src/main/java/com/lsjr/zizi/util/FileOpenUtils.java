package com.lsjr.zizi.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;

import com.ymz.baselibrary.AppCache;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import java.io.File;
import java.util.UUID;

/**
 * @创建者 CSDN_LQR
 * @描述 文件打开工具类
 */
public class FileOpenUtils {

    /**
     * 调用自带的视频播放器
     *
     * @param context
     * @param path
     */
    private static void openVideo(Context context, String path) {
        Uri uri = Uri.parse(path);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, "video/mp4");// "video/mp4"
        context.startActivity(intent);
    }

    /**
     * 调用自带的音频播放器
     *
     * @param context
     * @param path
     */
    private static void openAudio(Context context, String path) {
        File f = new File(path);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(f), "audio/*");// "audio/mp3"
        context.startActivity(intent);
    }

    /**
     * 调用自带的图库
     *
     * @param context
     * @param path
     */
    private static void openPic(Context context, String path) {
        File f = new File(path);
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(f), "image/*");
        context.startActivity(intent);
    }

    /**
     * 调用手机上能打开对应类型文件的程序
     *
     * @param context
     * @param path
     * @return true表示成功找到程序，false表示找不到能成功打开的程序
     */
    public static boolean openFile(Context context, String path) {
        String mimeType = MimeTypeUtils.getMimeType(path);
        File f = new File(path);
        Intent intent = new Intent();

        intent.setAction(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                Uri contentUri =  FileProvider.getUriForFile(context, "com.lsjr.zizi.fileProvider", f);
                intent.setDataAndType(contentUri, mimeType);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(f), mimeType);
        }
        context.startActivity(intent);


//        Intent intent = new Intent();
//        intent.setAction(android.content.Intent.ACTION_VIEW);
//        Uri uri;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            Uri contentUri = FileProvider.getUriForFile(context, "com.lsjr.zizi.fileProvider", new File(path));
//            intent.setDataAndType(contentUri, "video/*");
//        } else {
//            uri = Uri.fromFile(new File(path));
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.setDataAndType(uri, "video/*");
//        }
//
//        context.startActivity(intent);
        return true;

    }



    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_ADUIO = 2;
    private static final int TYPE_VIDEO = 3;
    /**
     * {@link #TYPE_IMAGE}<br/>
     * {@link #TYPE_ADUIO}<br/>
     * {@link #TYPE_VIDEO} <br/>
     *
     * @param type
     * @return
     */
    public static String getPublicFilePath(int type) {
        String fileDir = null;
        String fileSuffix = null;
        switch (type) {
            case TYPE_ADUIO:
                fileDir = AppCache.getInstance().mVoicesDir;
                fileSuffix = ".mp3";
                break;
            case TYPE_VIDEO:
                fileDir = AppCache.getInstance().mVideosDir;
                fileSuffix = ".mp4";
                break;
            case TYPE_IMAGE:
                fileDir = AppCache.getInstance().mPicturesDir;
                fileSuffix = ".jpg";
                break;
        }
        if (fileDir == null) {
            return null;
        }
        File file = new File(fileDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return null;
            }
        }
        return fileDir + File.separator + UUID.randomUUID().toString().replaceAll("-", "") + fileSuffix;
    }

}

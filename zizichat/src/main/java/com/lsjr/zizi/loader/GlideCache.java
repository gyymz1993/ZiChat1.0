package com.lsjr.zizi.loader;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.bumptech.glide.module.GlideModule;
import com.ymz.baselibrary.AppCache;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/17 9:10
 */

public class GlideCache implements GlideModule{
    @Override
    public void applyOptions(Context context, GlideBuilder glideBuilder) {
        /*设置格式*/
        glideBuilder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
        //设置目录
        String storageDir= AppCache.getInstance().getmFilesDir().getAbsolutePath();
        int cacheSize=100*1000*1000;
        glideBuilder.setDiskCache(new DiskLruCacheFactory(storageDir,cacheSize));
    }

    @Override
    public void registerComponents(Context context, Glide glide) {

    }
}

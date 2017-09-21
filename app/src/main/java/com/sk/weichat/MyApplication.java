package com.sk.weichat;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import com.baidu.mapapi.SDKInitializer;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.MemoryCacheAware;
import com.nostra13.universalimageloader.cache.memory.impl.LRULimitedMemoryCache;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.sk.weichat.NetWorkObservable.NetWorkObserver;
import com.sk.weichat.bean.ConfigBean;
import com.sk.weichat.bean.User;
import com.sk.weichat.db.SQLiteHelper;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.volley.FastVolley;

import java.io.File;

public class MyApplication extends Application {
	private static MyApplication INSTANCE = null;

	public static MyApplication getInstance() {
		return INSTANCE;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		INSTANCE = this;
		if (!"generic".equalsIgnoreCase(Build.BRAND)) {
			//SDKInitializer.initialize(getApplicationContext());
		}
		//SDKInitializer.initialize(getApplicationContext());
		if (AppConfig.DEBUG) {
			Log.d(AppConfig.TAG, "MyApplication onCreate");
		}
		if (AppConfig.DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
		}
		// 初始化网络监听
		mNetWorkObservable = new NetWorkObservable(this);
		// 初始化数据库
		SQLiteHelper.copyDatabaseFile(this);
		// 初始化定位
		getBdLocationHelper();
		// 初始化App目录
		initAppDir();
		// 初始化图片加载
		initImageLoader();

		Fresco.initialize(this);
	}
	private void SaveOfflineTime() {
		//将现在的时间存起来,
		long time=System.currentTimeMillis()/1000;
		Log.d("wang", "time_destory::" + time + "");
		PreferenceUtils.putLong(this, Constants.OFFLINE_TIME, time);
	}
	/**
	 * 在程序内部关闭时，调用此方法
	 */
	public void destory() {
		SaveOfflineTime();//在某些机型上可能不适用,不会成功保存
		mLoginUser.setOfflineTime(System.currentTimeMillis()/1000);
		UserDao.getInstance().updateUnLineTime(mLoginUser.getUserId(),System.currentTimeMillis()/1000);
		if (AppConfig.DEBUG) {
			Log.d(AppConfig.TAG, "MyApplication destory");
		}
		// 结束百度定位
		if (mBdLocationHelper != null) {
			mBdLocationHelper.release();
		}
		// 关闭网络状态的监听
		if (mNetWorkObservable != null) {
			mNetWorkObservable.release();
		}
		// 清除图片加载
		ImageLoader.getInstance().destroy();
		//
		releaseFastVolley();

		// 释放数据库
		// SQLiteHelper.release();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	/********************* 百度地图定位服务 ************************/
	private BdLocationHelper mBdLocationHelper;

	public BdLocationHelper getBdLocationHelper() {
		if (mBdLocationHelper == null) {
			mBdLocationHelper = new BdLocationHelper(this);
		}
		return mBdLocationHelper;
	}

	/********************* 提供网络全局监听 ************************/
	private NetWorkObservable mNetWorkObservable;

	public boolean isNetworkActive() {
		if (mNetWorkObservable != null) {
			return mNetWorkObservable.isNetworkActive();
		}
		return true;
	}

	public void registerNetWorkObserver(NetWorkObserver observer) {
		if (mNetWorkObservable != null) {
			mNetWorkObservable.registerObserver(observer);
		}
	}

	public void unregisterNetWorkObserver(NetWorkObserver observer) {
		if (mNetWorkObservable != null) {
			mNetWorkObservable.unregisterObserver(observer);
		}
	}

	/* 文件缓存的目录 */
	public String mAppDir;
	public String mPicturesDir;
	public String mVoicesDir;
	public String mVideosDir;
	public String mFilesDir;

	private void initAppDir() {
		File file = getExternalFilesDir(null);
		assert file != null;
		if (!file.exists()) {
			file.mkdirs();
		}
		mAppDir = file.getAbsolutePath();

		file = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		if (!file.exists()) {
			file.mkdirs();
		}
		mPicturesDir = file.getAbsolutePath();

		file = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
		if (!file.exists()) {
			file.mkdirs();
		}
		mVoicesDir = file.getAbsolutePath();

		file = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
		if (!file.exists()) {
			file.mkdirs();
		}
		mVideosDir = file.getAbsolutePath();
		
		file = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		if (!file.exists()) {
			file.mkdirs();
		}		
		mFilesDir = file.getAbsolutePath();
	}

	/******************* 初始化图片加载 **********************/
	// 显示的设置
	public static DisplayImageOptions mNormalImageOptions;
	public static DisplayImageOptions mAvatarRoundImageOptions;
	public static DisplayImageOptions mAvatarNormalImageOptions;

	private void initImageLoader() {
		int memoryCacheSize = (int) (Runtime.getRuntime().maxMemory() / 5);
		MemoryCacheAware<String, Bitmap> memoryCache;
		memoryCache = new LruMemoryCache(memoryCacheSize);
		/*
	* bitmapConfig(Config.RGB_565) 图片解码类型
	* cacheInMemory 设置下载的图片是否缓存在内存中
	* cacheOnDisc 设置下载的图片是否缓存在SD卡中
	* resetViewBeforeLoading 设置图片在下载前是否重置，复位
	* showImageForEmptyUri 设置图片Uri为空或是错误的时候显示的图片
	* showImageOnFail 设置图片加载/解码过程中错误时候显示的图片
	* */
		mNormalImageOptions = new DisplayImageOptions.Builder().bitmapConfig(Config.RGB_565).cacheInMemory(true).cacheOnDisc(true)
				.resetViewBeforeLoading(false).showImageForEmptyUri(R.drawable.image_download_fail_icon)
				.showImageOnFail(R.drawable.image_download_fail_icon).build();

		mAvatarRoundImageOptions = new DisplayImageOptions.Builder().bitmapConfig(Config.RGB_565).cacheInMemory(true).cacheOnDisc(true)
				.displayer(new RoundedBitmapDisplayer(10)).resetViewBeforeLoading(true).showImageForEmptyUri(R.drawable.avatar_normal)
				.showImageOnFail(R.drawable.avatar_normal).showImageOnLoading(R.drawable.avatar_normal).build();

		mAvatarNormalImageOptions = new DisplayImageOptions.Builder().bitmapConfig(Config.RGB_565).cacheInMemory(true).cacheOnDisc(true)
				.resetViewBeforeLoading(true).showImageForEmptyUri(R.drawable.avatar_normal).showImageOnFail(R.drawable.avatar_normal)
				.showImageOnLoading(R.drawable.avatar_normal).build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).defaultDisplayImageOptions(mNormalImageOptions)
				// .denyCacheImageMultipleSizesInMemory()
				.discCache(new TotalSizeLimitedDiscCache(new File(mPicturesDir), 50 * 1024 * 1024))
				// 最多缓存50M的图片
				.discCacheFileNameGenerator(new Md5FileNameGenerator()).memoryCache(memoryCache).tasksProcessingOrder(QueueProcessingType.LIFO)
				.threadPriority(Thread.NORM_PRIORITY - 2).threadPoolSize(4).build();

		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);
	}

	/********************* 提供全局配置 ************************/
	private AppConfig mConfig;

	public void setConfig(AppConfig config) {
		mConfig = config;
	}

	public AppConfig getConfig() {
		if (mConfig == null) {
			mConfig = AppConfig.initConfig(getApplicationContext(), new ConfigBean());
		}
		return mConfig;
	}

	/***************** 提供全局的Volley ***************************/

	private FastVolley mFastVolley;

	public FastVolley getFastVolley() {
		if (mFastVolley == null) {
			synchronized (MyApplication.class) {
				if (mFastVolley == null) {
					mFastVolley = new FastVolley(this);
					mFastVolley.start();
				}
			}
		}
		return mFastVolley;
	}

	private void releaseFastVolley() {
		if (mFastVolley != null) {
			mFastVolley.stop();
		}
	}

	/*********************** 保存当前登陆用户的全局信息 ***************/
	public String roomName;
	public String mAccessToken;
	public long mExpiresIn;
	public int mUserStatus;
	public boolean mUserStatusChecked = false;
	public User mLoginUser = new User();// 当前登陆的用户
	
	/*********************** 保存其他用户坐标信息 ***************/
}

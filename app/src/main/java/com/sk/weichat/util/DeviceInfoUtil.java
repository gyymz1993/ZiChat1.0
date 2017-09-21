package com.sk.weichat.util;

import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;

/**
 * 获取系统设备信息的工具类
 * 
 * @author dty
 * 
 */
public class DeviceInfoUtil {

	/* 获取手机唯一序列号 */
	public static String getDeviceId(Context context) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String deviceId = tm.getDeviceId();// 手机设备ID，这个ID会被用为用户访问统计
		if (deviceId == null) {
			deviceId = UUID.randomUUID().toString().replaceAll("-", "");
		}
		return deviceId;
	}

	/* 获取操作系统版本号 */
	public static String getOsVersion() {
		return android.os.Build.VERSION.RELEASE;
	}

	/* 获取手机型号 */
	public static String getModel() {
		return android.os.Build.MODEL;
	}

	/* 获取app的版本信息 */
	public static int getVersionCode(Context context) {
		PackageManager manager = context.getPackageManager();
		try {
			PackageInfo packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;// 系统版本号
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static String getVersionName(Context context) {
		PackageManager manager = context.getPackageManager();
		try {
			PackageInfo packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionName;// 系统版本名
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return "";
	}


}

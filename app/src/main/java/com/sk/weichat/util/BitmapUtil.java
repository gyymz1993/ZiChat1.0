package com.sk.weichat.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.text.TextUtils;

public class BitmapUtil {

	public static boolean saveBitmapToSDCard(Bitmap bmp, String strPath) {
		if (bmp == null) {
			return false;
		}
		if (TextUtils.isEmpty(strPath)) {
			return false;
		}
		try {
			File file = new File(strPath.substring(0, strPath.lastIndexOf("/")));
			if (!file.exists()) {
				file.mkdirs();
			}
			file = new File(strPath);
			FileOutputStream fos = new FileOutputStream(file);
			byte[] buffer = BitmapUtil.bitampToByteArray(bmp);
			fos.write(buffer);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public static byte[] bitampToByteArray(Bitmap bitmap) {
		byte[] array = null;
		try {
			if (null != bitmap) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
				array = os.toByteArray();
				os.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return array;
	}

}

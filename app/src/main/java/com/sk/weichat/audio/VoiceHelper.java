package com.sk.weichat.audio;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;

import android.media.MediaPlayer;
/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.audio
 * @作者:王阳
 * @创建时间: 2015年10月15日 下午5:00:14
 * @描述: voice简单处理类
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class VoiceHelper {
	private static StringBuilder mFormatBuilder=new StringBuilder();
	private static Formatter mFormatter=new Formatter(mFormatBuilder, Locale.getDefault());
	
	/**
	 * 时间显示的工具方法
	 */
	public static String stringForTime(int timeMs) {
		// timeMs=timeMs/1000; 百度播放器的时间单位就是秒，系统的VideoView是毫秒
		int seconds = timeMs % 60;
		int minutes = (timeMs / 60) % 60;
		int hours = timeMs / 3600;

		mFormatBuilder.setLength(0);
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	
	public static int getVoiceSeconds(String path) {
		int len = 0;
		if (!new File(path).exists()) {
			return len;
		}
		MediaPlayer m = new MediaPlayer();

		try {
			m.setDataSource(path);
			m.prepare();
			len = m.getDuration();
			m.release();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return len;

	}
}

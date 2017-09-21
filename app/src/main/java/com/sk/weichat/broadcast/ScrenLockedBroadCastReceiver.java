package com.sk.weichat.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.broadcast
 * @作者:王阳
 * @创建时间: 2015年10月16日 下午4:46:35
 * @描述: 监测屏幕锁屏和解锁
 * @SVN版本号: $Rev: 2143 $
 * @修改人: $Author: luorc $
 * @修改时间: $Date: 2015-10-23 09:31:46 +0800 (Fri, 23 Oct 2015) $
 * @修改的内容: TODO
 */
public class ScrenLockedBroadCastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		 if(action.equals(Intent.ACTION_SCREEN_OFF)){
			 Log.d("wang","锁屏了");
		}else if(action.equals(Intent.ACTION_SCREEN_ON)){
			Log.d("wang","解锁了");
		}
	}

}

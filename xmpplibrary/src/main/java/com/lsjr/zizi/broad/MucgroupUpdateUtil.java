package com.lsjr.zizi.broad;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ymz.baselibrary.utils.UIUtils;

/**
 * 群聊UI的更新
 * 
 */
public class MucgroupUpdateUtil {

	public static final String ACTION_UPDATE = UIUtils.getPackageName() + ".action.muc_group.update";// 群聊界面

	public static IntentFilter getUpdateActionFilter() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_UPDATE);
		return intentFilter;
	}

	public static void broadcastUpdateUi(Context context) {
		context.sendBroadcast(new Intent(ACTION_UPDATE));
	}

}

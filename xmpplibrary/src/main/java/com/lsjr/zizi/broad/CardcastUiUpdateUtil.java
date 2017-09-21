package com.lsjr.zizi.broad;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ymz.baselibrary.utils.UIUtils;

/**
 * 名片盒的Ui更新
 * 
 */
public class CardcastUiUpdateUtil {

	public static final String ACTION_UPDATE_UI = UIUtils.getPackageName() + ".action.cardcast.update_ui";// 关注界面

	public static IntentFilter getUpdateActionFilter() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_UPDATE_UI);
		return intentFilter;
	}

	public static void broadcastUpdateUi(Context context) {
		context.sendBroadcast(new Intent(ACTION_UPDATE_UI));
	}

}

package com.lsjr.zizi.chat.broad;

import android.content.Context;
import android.content.Intent;

import com.ymz.baselibrary.utils.UIUtils;

/**
 * 用于聊天消息的广播，更新MainActivity Tab栏显示的未读数量 和 消息界面数据的更新
 * 
 */
public class MsgBroadcast {
	public static final String ACTION_MSG_UI_UPDATE = UIUtils.getPackageName() + ".action.msg_ui_update";// 界面的更新
	public static final String ACTION_MSG_NUM_UPDATE = UIUtils.getPackageName()  + ".intent.action.msg_num_update";// 未读数量的更新
	public static final String ACTION_MSG_NUM_RESET = UIUtils.getPackageName() + ".action.msg_num_reset";// 未读数量需要重置，即从数据库重新查
	public static final String EXTRA_NUM_COUNT = "count";
	public static final String EXTRA_NUM_OPERATION = "operation";
	public static final int NUM_ADD = 0;// 消息加
	public static final int NUM_REDUCE = 1;// 消息减

	/**
	 * 更新消息Fragment的广播
	 * 
	 * @return
	 */
	public static void broadcastMsgUiUpdate(Context context) {
		context.sendBroadcast(new Intent(ACTION_MSG_UI_UPDATE));
	}

	public static void broadcastMsgNumReset(Context context) {
		context.sendBroadcast(new Intent(ACTION_MSG_NUM_RESET));
	}

	public static void broadcastMsgNumUpdate(Context context, boolean add, int count) {
		Intent intent = new Intent(ACTION_MSG_NUM_UPDATE);
		intent.putExtra(EXTRA_NUM_COUNT, count);
		if (add) {
			intent.putExtra(EXTRA_NUM_OPERATION, NUM_ADD);
		} else {
			intent.putExtra(EXTRA_NUM_OPERATION, NUM_REDUCE);
		}
		context.sendBroadcast(intent);
	}

}

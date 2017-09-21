package com.sk.weichat.ui;

import android.content.Context;
import android.util.Log;

import com.baidu.android.pushservice.PushMessageReceiver;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.sk.weichat.MyApplication;

import org.apache.http.Header;

import java.util.List;

public class PushNetMessageReceiver extends PushMessageReceiver {

	@Override
	public void onBind(Context context, int errorCode, String appid, String userId, String channelId,
			String requestId) {
		String responseString = "onBind errorCode=" + errorCode + " appid=" + appid + " userId=" + userId
				+ " channelId=" + channelId + " requestId=" + requestId;
		Log.d("wang", responseString);
		RequestParams params = new RequestParams();
		params.put("channelId", channelId);
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("deviceId", 1);
		AsyncHttpClient client = new AsyncHttpClient();
		client.post("http://imapi.youjob.co/user/channelId/set", params, new AsyncHttpResponseHandler() {

			@Override
			public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
				Log.d("wang","上传失败"+arg3.toString());

			}

			@Override
			public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
				// TODO Auto-generated method stub
				Log.d("wang", "上传channelId成功了");
			}

		});
	}

	@Override
	public void onDelTags(Context arg0, int arg1, List<String> arg2, List<String> arg3, String arg4) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onListTags(Context arg0, int arg1, List<String> arg2, String arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessage(Context arg0, String arg1, String arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNotificationArrived(Context arg0, String arg1, String arg2, String arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNotificationClicked(Context arg0, String arg1, String arg2, String arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSetTags(Context arg0, int arg1, List<String> arg2, List<String> arg3, String arg4) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnbind(Context arg0, int arg1, String arg2) {
		// TODO Auto-generated method stub

	}

}

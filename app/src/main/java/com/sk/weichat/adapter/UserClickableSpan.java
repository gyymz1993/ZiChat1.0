package com.sk.weichat.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.ui.circle.BusinessCircleActivity;

public class UserClickableSpan extends ClickableSpan {

	int color = -1;
	private Context context;
	private String userId;
	private String nickName;// 可能也是备注名字，但是不影响，传递到BusinessCircleActivity页面，一样的会查询出备注名

	public UserClickableSpan(Context context, String userId, String nickName) {
		this(-1, context, userId, nickName);
	}

	/**
	 * constructor
	 * 
	 * @param color
	 *            the link color
	 * @param context
	 * @param intent
	 */
	public UserClickableSpan(int color, Context context, String userId, String nickName) {
		if (color != -1) {
			this.color = color;
		}
		this.context = context;
		this.userId = userId;
		this.nickName = nickName;
	}

	/**
	 * Performs the click action associated with this span.
	 */
	public void onClick(View widget) {
		Log.d("wang","跳到我的空间去");
		Intent intent = new Intent(context, BusinessCircleActivity.class);
		intent.putExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_PERSONAL_SPACE);
		intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
		intent.putExtra(AppConstant.EXTRA_NICK_NAME, nickName);
		context.startActivity(intent);
	}

	/**
	 * Makes the text without underline.
	 */
	@Override
	public void updateDrawState(TextPaint ds) {
		if (color == -1) {
			ds.setColor(context.getResources().getColor(R.color.link_nick_name_color));
		} else {
			ds.setColor(color);
		}
		ds.setUnderlineText(false);
	}

	public static void setClickableSpan(Context context, SpannableStringBuilder builder, String appendData, String userId) {
		builder.append(appendData);
		int end = builder.length();
		int length = appendData.length();
		builder.setSpan(new UserClickableSpan(context, userId, appendData), end - length, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
}
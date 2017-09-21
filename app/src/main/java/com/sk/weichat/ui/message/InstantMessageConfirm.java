package com.sk.weichat.ui.message;

import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.helper.AvatarHelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;
/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.ui.message
 * @作者:王阳
 * @创建时间: 2015年10月21日 下午4:51:59
 * @描述: T确认选择联系人转发消息
 * @SVN版本号: $Rev: 2143 $
 * @修改人: $Author: luorc $
 * @修改时间: $Date: 2015-10-23 09:31:46 +0800 (Fri, 23 Oct 2015) $
 * @修改的内容: TODO
 */
public class InstantMessageConfirm extends PopupWindow {
	private Button mSend, mCancle;
	private View mMenuView;
	private ImageView mIvHead;
	private TextView mTvName;

	public InstantMessageConfirm(Activity context, OnClickListener itemsOnClick, Friend friend) {
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mMenuView = inflater.inflate(R.layout.message_instantconfirm, null);
		mSend = (Button) mMenuView.findViewById(R.id.btn_send);
		mCancle = (Button) mMenuView.findViewById(R.id.btn_cancle);

		mIvHead = (ImageView) mMenuView.findViewById(R.id.iv_instant_head);
		mTvName = (TextView) mMenuView.findViewById(R.id.tv_constacts_name);
		if (friend != null) {
			mTvName.setText(friend.getNickName());
			if (friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)) {// 系统消息的头像
				mIvHead.setImageResource(R.drawable.im_notice);
			} else if (friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)) {// 新朋友的头像
				mIvHead.setImageResource(R.drawable.im_new_friends);
			} else {// 其他
				AvatarHelper.getInstance().displayAvatar(friend.getUserId(), mIvHead, true);
			}
		}
		// 取消按钮
		/*
		 * btn_cancel.setOnClickListener(new OnClickListener() {
		 * 
		 * public void onClick(View v) { //销毁弹出框 dismiss(); } });
		 */
		// 设置按钮监听
		mSend.setOnClickListener(itemsOnClick);
		mCancle.setOnClickListener(itemsOnClick);
		// 设置SelectPicPopupWindow的View
		this.setContentView(mMenuView);
		// 设置SelectPicPopupWindow弹出窗体的宽
		this.setWidth(LayoutParams.MATCH_PARENT);
		// 设置SelectPicPopupWindow弹出窗体的高
		this.setHeight(LayoutParams.WRAP_CONTENT);
		// 设置SelectPicPopupWindow弹出窗体可点击
		this.setFocusable(true);
		// 设置SelectPicPopupWindow弹出窗体动画效果
		this.setAnimationStyle(R.style.Buttom_Popwindow);
		// 实例化一个ColorDrawable颜色为半透明
		ColorDrawable dw = new ColorDrawable(0xb0000000);
		// 设置SelectPicPopupWindow弹出窗体的背景
		this.setBackgroundDrawable(dw);
		// mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
		mMenuView.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {

				int height = mMenuView.findViewById(R.id.pop_layout).getTop();
				int bottom = mMenuView.findViewById(R.id.pop_layout).getBottom();
				int y = (int) event.getY();
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (y < height) {
						dismiss();
					} else if (y > bottom) {
						dismiss();
					}

				}
				return true;
			}
		});

	}
}

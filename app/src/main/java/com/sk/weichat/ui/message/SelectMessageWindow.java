package com.sk.weichat.ui.message;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;

import com.sk.weichat.R;
import com.sk.weichat.bean.message.XmppMessage;

public class SelectMessageWindow extends PopupWindow {
	private Button mCopy, mInstant, mCancle, mDelete;
	private View mMenuView;
	int type;



	private void hideButton() {
		if (mCopy != null) {
			if (type != XmppMessage.TYPE_TEXT) {
				mCopy.setVisibility(View.GONE);
			} else {
				mCopy.setVisibility(View.VISIBLE);
			}

		}
		if (mInstant != null) {
			if (type == XmppMessage.TYPE_CARD||type==XmppMessage.TYPE_FILE||type==XmppMessage.TYPE_GIF) {
				mInstant.setVisibility(View.GONE);
			}else{
				mInstant.setVisibility(View.VISIBLE);
			}
		}
	}

	public SelectMessageWindow(Context context, OnClickListener itemsOnClick, int type) {
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mMenuView = inflater.inflate(R.layout.message_dialog, null);
		mCopy = (Button) mMenuView.findViewById(R.id.btn_copy);
		mInstant = (Button) mMenuView.findViewById(R.id.btn_instant);
		mCancle = (Button) mMenuView.findViewById(R.id.btn_cancle);
		mDelete = (Button) mMenuView.findViewById(R.id.btn_delete);
		this.type = type;
		hideButton();
		// 取消按钮
		/*
		 * btn_cancel.setOnClickListener(new OnClickListener() {
		 * 
		 * public void onClick(View v) { //销毁弹出框 dismiss(); } });
		 */
		// 设置按钮监听
		mCopy.setOnClickListener(itemsOnClick);
		mInstant.setOnClickListener(itemsOnClick);
		mCancle.setOnClickListener(itemsOnClick);
		mDelete.setOnClickListener(itemsOnClick);
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

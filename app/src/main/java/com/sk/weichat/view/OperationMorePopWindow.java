package com.sk.weichat.view;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sk.weichat.R;

/**
 * 更多操作的PopupWindow
 * 
 */
public class OperationMorePopWindow {
	private PopupWindow mPopupWindow;
	private Activity mActivity;
	private int mOperationMorePopWidth;
	private int mOperationMorePopHeight;
	private TextView mPraiseTv;
	private TextView mCommentTv;
	private TextView mGiftTv;

	public OperationMorePopWindow(Activity activity) {
		mActivity = activity;
		mPopupWindow = new PopupWindow(mActivity);
		mPopupWindow.setBackgroundDrawable(new ColorDrawable());
		mPopupWindow.setFocusable(true);
		mPopupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		mPopupWindow.setOutsideTouchable(true);
		View rootView = LayoutInflater.from(mActivity).inflate(R.layout.p_msg_operation_more_pop_view, null);
		int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
		rootView.measure(measureSpec, measureSpec);
		mOperationMorePopHeight = rootView.getMeasuredHeight();
		mOperationMorePopWidth = rootView.getMeasuredWidth();
		mPopupWindow.setHeight(mOperationMorePopHeight);
		mPopupWindow.setWidth(mOperationMorePopWidth);
		mPopupWindow.setAnimationStyle(R.style.IMAnimation_Popwindow);
		mPopupWindow.setContentView(rootView);

		mPraiseTv = (TextView) rootView.findViewById(R.id.praise_tv);
		mCommentTv = (TextView) rootView.findViewById(R.id.comment_tv);
		mGiftTv = (TextView) rootView.findViewById(R.id.gift_tv);
		mPraiseTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onPraise(messagePosition, isPraise);
				}
				dismiss();
			}
		});
		mCommentTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onComment(messagePosition);
				}
				dismiss();
			}
		});
		mGiftTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onGift(messagePosition);
				}
				dismiss();
			}
		});
	}

	private int messagePosition;
	private boolean isPraise;

	/**
	 * 
	 * @param anchowView
	 * @param messagePosition
	 * @param isPraise 点击去做什么操作，如果已经赞过，下一步就是取消赞，那么isPraise false，反之
	 */
	public void show(View anchowView, int messagePosition, boolean isPraise) {
		this.messagePosition = messagePosition;
		this.isPraise = isPraise;
		if(isPraise){
			mPraiseTv.setText(R.string.praise);
		}else{
			mPraiseTv.setText(R.string.cancel_praise);
		}
		mPopupWindow.showAsDropDown(anchowView, -mOperationMorePopWidth - 10, -(mOperationMorePopHeight + anchowView.getHeight()) / 2);
	}

	private OperationMoreListener listener;

	public void setOperationMoreListener(OperationMoreListener listener) {
		this.listener = listener;
	}

	public static interface OperationMoreListener {
		void onPraise(int messagePosition, boolean isPraise);

		void onGift(int messagePosition);

		void onComment(int messagePosition);
	}

	public void dismiss() {
		mPopupWindow.dismiss();
	}
}

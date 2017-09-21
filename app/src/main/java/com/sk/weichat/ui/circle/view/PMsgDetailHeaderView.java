package com.sk.weichat.ui.circle.view;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.User;
import com.sk.weichat.bean.circle.Praise;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.circle.BasicInfoActivity;
import com.sk.weichat.util.DisplayUtil;
import com.sk.weichat.util.TimeUtils;

public class PMsgDetailHeaderView extends LinearLayout {

	public static interface PMsgDetailListener {
		void doPraise(boolean praise);// 去进行赞或者取消赞的操作

		void doFriend();// 去进行朋友操作
	}

	private PMsgDetailListener mPMsgDetailListener;

	private LinearLayout mContentLayout;
	private ImageView mAvatarImg;
	private TextView mNicknameTv;
	private TextView mLevelTv;
	private TextView mAddAttentionTv;
	private TextView mFansCountTv;
	private TextView mTimeTv;
	private TextView mContentTv;
	private TextView mListenCountTv;
	private TextView mShareCountTv;
	private TextView mGiftCountTv;
	private TextView mPraiseCountTv;
	private LinearLayout mPraiseUserLayout;
	private ImageView mPraiseMoreBtn;
	private TextView mCommentCountTv;

	private PublicMessage mPublicMessage;

	private ImageView[] mPraiseUserAvatars;

	public PMsgDetailHeaderView(Context context) {
		super(context);
		init();
	}

	public PMsgDetailHeaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public PMsgDetailHeaderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	int displayWith;
	float density;

	@SuppressWarnings("deprecation")
	private void init() {
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		Display d = wm.getDefaultDisplay();
		displayWith = d.getWidth();
		DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
		density = dm.density;

		LayoutInflater.from(getContext()).inflate(R.layout.header_view_p_msg_detail, this);
		mContentLayout = (LinearLayout) findViewById(R.id.content_ll);
		mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
		mNicknameTv = (TextView) findViewById(R.id.nick_name_tv);
		mLevelTv = (TextView) findViewById(R.id.level_tv);
		mAddAttentionTv = (TextView) findViewById(R.id.add_attention_tv);
		mFansCountTv = (TextView) findViewById(R.id.fans_count_tv);
		mTimeTv = (TextView) findViewById(R.id.time_tv);
		mContentTv = (TextView) findViewById(R.id.content_tv);
		mListenCountTv = (TextView) findViewById(R.id.listen_count_tv);
		mShareCountTv = (TextView) findViewById(R.id.share_count_tv);
		mGiftCountTv = (TextView) findViewById(R.id.gift_count_tv);
		mPraiseCountTv = (TextView) findViewById(R.id.praise_count_tv);
		mPraiseUserLayout = (LinearLayout) findViewById(R.id.praise_user_layout);
		mPraiseMoreBtn = (ImageView) findViewById(R.id.more_icon_img);
		mCommentCountTv = (TextView) findViewById(R.id.comment_count_tv);
		initPraiseUserIconParams();

		mAddAttentionTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 没有登陆，提示登陆
				// if (LoginHelper.checkStatusForLogin(getContext(), true)) {
				// return;
				// }
				if (mPMsgDetailListener != null) {
					mPMsgDetailListener.doFriend();
				}
			}
		});

		mPraiseCountTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 没有登陆，提示登陆
				// if (LoginHelper.checkStatusForLogin(getContext(), true)) {
				// return;
				// }
				Message msg = mPraiseHandler.obtainMessage(MSG_PRAISE);
				msg.arg1 = mPublicMessage.getIsPraise() == 0 ? 1 : 0;// 0代表赞，那么赋值1代表点赞，如果是1，那么就是赋值0，取消赞
				mPraiseHandler.removeMessages(MSG_PRAISE);// 把之前可能存在的该点赞的消息去掉，防止狂点重复发送请求
				mPraiseHandler.sendMessageDelayed(msg, 500);
				// 直接改变界面的显示
				setPraise();
			}
		});

		mPraiseMoreBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO
			}
		});

		mPraiseYesDrawable = getContext().getResources().getDrawable(R.drawable.icon_nice_press);
		mPraiseNoDrawable = getContext().getResources().getDrawable(R.drawable.icon_nice);

		mPraiseYesDrawable.setBounds(0, 0, mPraiseYesDrawable.getIntrinsicWidth(), mPraiseYesDrawable.getIntrinsicHeight());
		mPraiseNoDrawable.setBounds(0, 0, mPraiseNoDrawable.getIntrinsicWidth(), mPraiseNoDrawable.getIntrinsicHeight());
	}

	@Override
	public void setVisibility(int visibility) {
		mContentLayout.setVisibility(visibility);
	}

	private Drawable mPraiseYesDrawable;
	private Drawable mPraiseNoDrawable;

	/**
	 * 1个赞的改变包过：<br/>
	 * 1、赞的那个图标的改变<br/>
	 * 2、多少人赞过的那个改变<br/>
	 * 3、赞的那条消息实体的改变<br/>
	 */
	private void setPraise() {

		boolean isPraise = mPublicMessage.getIsPraise() == 0 ? true : false;// 是否是去执行赞的操作，true去咱，false取消赞

		mPublicMessage.setIsPraise(isPraise ? 1 : 0);

		List<Praise> praises = mPublicMessage.getPraises();
		if (praises == null) {
			praises = new ArrayList<Praise>();
			mPublicMessage.setPraises(praises);
		}

		int praiseCount = mPublicMessage.getPraise();

		String loginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		String loginNickName = MyApplication.getInstance().mLoginUser.getNickName();

		if (isPraise) {// 代表我点赞
			// 赞的图标的改变
			mPraiseCountTv.setCompoundDrawables(mPraiseYesDrawable, null, null, null);
			// 消息实体的改变
			Praise praise = new Praise();
			praise.setUserId(loginUserId);
			praise.setNickName(loginNickName);
			praises.add(0, praise);
			praiseCount++;
			mPublicMessage.setPraise(praiseCount);
		} else {// 取消我的赞
			// 赞的图标的改变
			mPraiseCountTv.setCompoundDrawables(mPraiseNoDrawable, null, null, null);
			// 消息实体的改变
			for (int i = 0; i < praises.size(); i++) {
				if (loginUserId == praises.get(i).getUserId()) {
					praises.remove(i);
					praiseCount--;
					mPublicMessage.setPraise(praiseCount);
					break;
				}
			}
		}

		mPraiseCountTv.setText(mPublicMessage.getPraise() + "");

		updatePraiseListAvatar();
	}

	private static final int MSG_PRAISE = 0x1;// 点赞那个按钮发出来的消息，包括赞和取消赞
	private Handler mPraiseHandler = new Handler() {
		public void handleMessage(Message msg) {
			boolean isPraise = msg.arg1 == 1 ? true : false;
			if (mPMsgDetailListener != null) {
				mPMsgDetailListener.doPraise(isPraise);
			}
		}
	};// 防止别人狂点赞那个按钮

	/**
	 * 因为布局文件都是写死的，因此可以直接算
	 */
	public void initPraiseUserIconParams() {
		int paddingDip = 22 * 2 + 70 + 6 * 2;
		int moreIconWidth = (int) (density / 2.0f * 40);
		int width = displayWith - moreIconWidth - DisplayUtil.dip2px(getContext(), paddingDip);

		int imgWidth = DisplayUtil.dip2px(getContext(), 25);// 小头像带下为25dp
		int baseMargin = DisplayUtil.dip2px(getContext(), 8);// 两个小头像中间最少间距8dp

		int iconCount = 0;
		int sumWidth = 0;
		while (sumWidth < width) {
			sumWidth += imgWidth;
			if (sumWidth > width) {
				break;
			}
			sumWidth += baseMargin;
			iconCount++;
		}

		int iconMargin = (int) ((width - imgWidth * iconCount) / (float) (iconCount - 1));

		mPraiseUserAvatars = new ImageView[iconCount];

		for (int i = 0; i < mPraiseUserAvatars.length; i++) {
			mPraiseUserAvatars[i] = new ImageView(getContext());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imgWidth, imgWidth);
			if (i != 0) {
				params.leftMargin = iconMargin;
			}
			mPraiseUserLayout.addView(mPraiseUserAvatars[i], params);
			mPraiseUserAvatars[i].setScaleType(ScaleType.FIT_XY);
			mPraiseUserAvatars[i].setImageResource(R.drawable.avatar_normal);
			// mPraiseUserAvatars[i].setVisibility(View.GONE);
		}
	}

	public void setPMsgDetailListener(PMsgDetailListener listener) {
		mPMsgDetailListener = listener;
	}

	/**
	 * 绑定PublicMessage
	 */
	public void setPublicMessage(PublicMessage publicMessage) {
		if (publicMessage == null) {
			return;
		}
		mPublicMessage = publicMessage;
		// Update UI
		AvatarHelper.getInstance().displayAvatar(mPublicMessage.getUserId(), mAvatarImg, true);

		mAvatarImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 没有登陆，提示登陆
				// if (LoginHelper.checkStatusForLogin(getContext(), true)) {
				// return;
				// }
				Intent intent = new Intent(getContext(), BasicInfoActivity.class);
				intent.putExtra(AppConstant.EXTRA_USER_ID, mPublicMessage.getUserId());
				getContext().startActivity(intent);
			}
		});

		mNicknameTv.setText(Friend.getShowName(mPublicMessage.getUserId(), mPublicMessage.getNickName()));

		mTimeTv.setText(TimeUtils.getFriendlyTimeDesc(getContext(), (int) mPublicMessage.getTime()));

		mContentTv.setText(mPublicMessage.getBody().getText());

		mListenCountTv.setText(mPublicMessage.getPlay() + "");
		mShareCountTv.setText(mPublicMessage.getForward() + "");
		mGiftCountTv.setText(mPublicMessage.getMoney() + "");

		// 赞人数的限制
		mPraiseCountTv.setText(mPublicMessage.getPraise() + "");
		// 赞icon的显示
		if (mPublicMessage.getIsPraise() == 0) {// 我没赞过
			mPraiseCountTv.setCompoundDrawables(mPraiseNoDrawable, null, null, null);
		} else {// 我赞过了
			mPraiseCountTv.setCompoundDrawables(mPraiseYesDrawable, null, null, null);
		}

		updatePraiseListAvatar();

		updateCommentCount();
	}

	public void updateCommentCount() {
		mCommentCountTv.setText(getContext().getString(R.string.comment_count, mPublicMessage.getCommnet()));
	}

	/**
	 * 更新赞的头像列表
	 */
	private void updatePraiseListAvatar() {
		// 赞列表头像的显示
		List<Praise> praises = mPublicMessage.getPraises();
		if (praises == null || praises.size() <= 0) {
			for (int i = 0; i < mPraiseUserAvatars.length; i++) {
				mPraiseUserAvatars[i].setVisibility(View.GONE);
			}
		} else {
			for (int i = 0; i < mPraiseUserAvatars.length; i++) {
				if (i < praises.size()) {
					mPraiseUserAvatars[i].setVisibility(View.VISIBLE);
					AvatarHelper.getInstance().displayAvatar(praises.get(i).getUserId(), mPraiseUserAvatars[i], true);
				} else {
					mPraiseUserAvatars[i].setVisibility(View.GONE);
				}
			}
		}
	}

	public void setUser(User userDetail) {
		if (userDetail == null) {
			return;
		}
		mLevelTv.setText(getContext().getString(R.string.level_inner, userDetail.getVip()));// 等级
		mFansCountTv.setText(userDetail.getFansCount() + "");// 粉丝数量
	}

	public void setFriendStatus(int friendStatus) {
		if (friendStatus == Friend.STATUS_SELF) {
			mAddAttentionTv.setVisibility(View.GONE);
		} else if (friendStatus == Friend.STATUS_FRIEND) {
			mAddAttentionTv.setText("发消息");
		} else if (friendStatus == Friend.STATUS_ATTENTION) {
			mAddAttentionTv.setText("打招呼");
		} else {// 黑名单或者游客
			mAddAttentionTv.setText("加关注");
		}
	}
}

package com.sk.weichat.view;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.sk.weichat.R;

public class CommentBottomView extends LinearLayout implements View.OnClickListener {

	private Context mContext;
	private ImageButton mEmotionBtn;
	private EditText mChatEdit;
	private Button mGifBtn;
	private Button mSendBtn;
	private ChatFaceView mChatFaceView;

	private InputMethodManager mInputManager;
	private Handler mHandler = new Handler();

	private int mDelayTime = 0;

	private CommentBottomListener mBottomListener;

	public CommentBottomView(Context context) {
		super(context);
		init(context);
	}

	public CommentBottomView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CommentBottomView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private static final int RIGHT_VIEW_GIFT = 0;
	private static final int RIGHT_VIEW_SNED = 1;
	private int mRightView = RIGHT_VIEW_GIFT;// 当前右边的模式，用int变量保存，效率更高点

	private void init(Context context) {
		mContext = context;
		mInputManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		mDelayTime = mContext.getResources().getInteger(android.R.integer.config_shortAnimTime);

		LayoutInflater.from(mContext).inflate(R.layout.comment_bottom, this);

		mEmotionBtn = (ImageButton) findViewById(R.id.emotion_btn);
		mChatEdit = (EditText) findViewById(R.id.chat_edit);
		mGifBtn = (Button) findViewById(R.id.gift_btn);
		mSendBtn = (Button) findViewById(R.id.send_btn);

		mChatFaceView = (ChatFaceView) findViewById(R.id.chat_face_view);

		mEmotionBtn.setOnClickListener(this);
		mGifBtn.setOnClickListener(this);
		mSendBtn.setOnClickListener(this);
		mChatEdit.setOnClickListener(this);

		mChatEdit.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mChatEdit.requestFocus();
				return false;
			}
		});

		mChatEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if(!mHasGiftBtn){
					return;
				}
				int currentView = 0;
				if (s.length() <= 0) {
					currentView = RIGHT_VIEW_GIFT;
				} else {
					currentView = RIGHT_VIEW_SNED;
				}

				if (currentView == mRightView) {
					return;
				}
				mRightView = currentView;
				if (mRightView == 0) {
					mGifBtn.setVisibility(View.VISIBLE);
					mSendBtn.setVisibility(View.GONE);
				} else {
					mGifBtn.setVisibility(View.GONE);
					mSendBtn.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});

		mChatFaceView.setEmotionClickListener(new ChatFaceView.EmotionClickListener() {
			@Override
			public void onNormalFaceClick(SpannableString ss) {
				if (mChatEdit.hasFocus()) {
					mChatEdit.getText().insert(mChatEdit.getSelectionStart(), ss);
				} else {
					mChatEdit.getText().insert(mChatEdit.getText().toString().length(), ss);
				}
			}

			@Override
			public void onGifFaceClick(String resName) {
			}
		});

		if(!mHasGiftBtn){
			mSendBtn.setVisibility(View.VISIBLE);
			mGifBtn.setVisibility(View.GONE);
		}
	}
	
	private boolean mHasGiftBtn=true;//是否有送礼物的按钮，默认为有
	
	public void setHasGiftBtn(boolean has){
		mHasGiftBtn=has;
		if(!mHasGiftBtn){
			mSendBtn.setVisibility(View.VISIBLE);
			mGifBtn.setVisibility(View.GONE);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		mChatEdit.setFocusable(hasWindowFocus);
		mChatEdit.setFocusableInTouchMode(hasWindowFocus);
		super.onWindowFocusChanged(hasWindowFocus);
	}

	/**
	 * 显示或隐藏表情布局
	 * 
	 * @param show
	 */
	private void changeChatFaceView(boolean show) {
		boolean isShowing = mChatFaceView.getVisibility() != View.GONE;
		if (isShowing == show) {
			return;
		}
		if (show) {
			mChatFaceView.setVisibility(View.VISIBLE);
			mEmotionBtn.setBackgroundResource(R.drawable.im_btn_keyboard_bg);
		} else {
			mChatFaceView.setVisibility(View.GONE);
			mEmotionBtn.setBackgroundResource(R.drawable.im_btn_emotion_bg);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		/*************************** 控制底部栏状态变化 **************************/
		case R.id.emotion_btn:
			if (mChatFaceView.getVisibility() != View.GONE) {// 表情布局在显示,那么点击则是隐藏表情，显示键盘
				mInputManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
				changeChatFaceView(false);
			} else {// 表情布局没有显示,那么点击则是显示表情，隐藏键盘、录音、更多布局
				mInputManager.hideSoftInputFromWindow(mChatEdit.getApplicationWindowToken(), 0);
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						changeChatFaceView(true);
					}
				}, mDelayTime);
			}
			break;

		case R.id.chat_edit:// 隐藏其他所有布局，显示键盘
			changeChatFaceView(false);
			break;
		case R.id.gift_btn:
			if (mBottomListener != null) {
				mBottomListener.onGiftClick();
			}

			break;
		case R.id.send_btn:// 发送文字的回调
			if (mBottomListener != null) {
				String msg = mChatEdit.getText().toString();
				if (TextUtils.isEmpty(msg)) {
					return;
				}
				mBottomListener.sendText(msg);
				mChatEdit.setText("");
			}
			break;
		}

	}

	public void setHint(String hint){
		mChatEdit.setHint(hint);
	}
	
	public void show(String hint){
		mChatEdit.setHint(hint);
		mInputManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
	}
	
	public void show(){
		mChatEdit.requestFocus();
		mInputManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
	}
	
	public void reset() {
		changeChatFaceView(false);
		mInputManager.hideSoftInputFromWindow(mChatEdit.getApplicationWindowToken(), 0);
	}

	public void setCommentBottomListener(CommentBottomListener listener) {
		mBottomListener = listener;
	}

	public static interface CommentBottomListener {
		public void sendText(String text);

		public void onGiftClick();
	}

}

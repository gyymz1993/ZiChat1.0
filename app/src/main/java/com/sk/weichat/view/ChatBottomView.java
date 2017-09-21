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
import android.widget.Toast;

import com.sk.weichat.R;
import com.sk.weichat.audio.IMRecordController;
import com.sk.weichat.audio.RecordListener;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.view
 * @作者:王阳
 * @创建时间: 2015年10月15日 下午5:59:56
 * @描述: 聊天界面下面输入操作的view
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: 修改录音问题
 */
public class ChatBottomView extends LinearLayout implements View.OnClickListener {

	private Context mContext;
	private ImageButton mEmotionBtn;
	private ImageButton mMoreBtn;
	private EditText mChatEdit;
	private Button mRecordBtn;
	private Button mSendBtn;
	private ImageButton mVoiceImgBtn;

	private ChatFaceView mChatFaceView;
	/* Tool */
	private View mChatToolsView;

	private InputMethodManager mInputManager;
	private Handler mHandler = new Handler();

	private int mDelayTime = 0;

	private IMRecordController mRecordController;
	private ChatBottomListener mBottomListener;

	public ChatBottomView(Context context) {
		super(context);
		init(context);
	}

	public ChatBottomView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ChatBottomView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private static final int RIGHT_VIEW_RECORD = 0;
	private static final int RIGHT_VIEW_SNED = 1;
	private int mRightView = RIGHT_VIEW_RECORD;// 当前右边的模式，用int变量保存，效率更高点

	private void init(Context context) {
		mContext = context;
		mInputManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		mDelayTime = mContext.getResources().getInteger(android.R.integer.config_shortAnimTime);

		LayoutInflater.from(mContext).inflate(R.layout.chat_bottom, this);

		mEmotionBtn = (ImageButton) findViewById(R.id.emotion_btn);
		mMoreBtn = (ImageButton) findViewById(R.id.more_btn);
		mChatEdit = (EditText) findViewById(R.id.chat_edit);
		mRecordBtn = (Button) findViewById(R.id.record_btn);
		mSendBtn = (Button) findViewById(R.id.send_btn);
		mVoiceImgBtn = (ImageButton) findViewById(R.id.voice_img_btn);

		mChatFaceView = (ChatFaceView) findViewById(R.id.chat_face_view);
		mChatToolsView = (View) findViewById(R.id.chat_tools_view);
		/* Tool */
		findViewById(R.id.im_photo_tv).setOnClickListener(this);
		findViewById(R.id.im_camera_tv).setOnClickListener(this);
		findViewById(R.id.im_video_tv).setOnClickListener(this);
		findViewById(R.id.im_audio_tv).setOnClickListener(this);
		findViewById(R.id.im_file_tv).setOnClickListener(this);
		findViewById(R.id.im_loc_tv).setOnClickListener(this);
		findViewById(R.id.im_card_tv).setOnClickListener(this);

		mEmotionBtn.setOnClickListener(this);
		mMoreBtn.setOnClickListener(this);
		mVoiceImgBtn.setOnClickListener(this);
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
				int currentView = 0;
				if (s.length() <= 0) {
					currentView = RIGHT_VIEW_RECORD;
				} else {
					currentView = RIGHT_VIEW_SNED;
				}

				if (currentView == mRightView) {
					return;
				}
				mRightView = currentView;
				if (mRightView == 0) {
					mVoiceImgBtn.setVisibility(View.VISIBLE);
					mSendBtn.setVisibility(View.GONE);
				} else {
					mVoiceImgBtn.setVisibility(View.GONE);
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

		mRecordController = new IMRecordController(mContext);
		mRecordController.setRecordListener(new RecordListener() {
			@Override
			public void onRecordSuccess(String filePath, int timeLen) {
				// 录音成功，返回录音文件的路径
				mRecordBtn.setText(R.string.motalk_voice_chat_tip_1);
				mRecordBtn.setBackgroundResource(R.drawable.im_voice_button_normal);
				if(timeLen<1){
					Toast.makeText(mContext, "录音太短了,我还没听清楚呢", 0).show();
					return;
				}
				if (mBottomListener != null) {
					mBottomListener.sendVoice(filePath, timeLen);
				}
			}

			@Override
			public void onRecordStart() {
				mBottomListener.stopVoicePlay();//停止播放聊天记录里的语音
				// 录音开始
				mRecordBtn.setText(R.string.motalk_voice_chat_tip_2);
				mRecordBtn.setBackgroundResource(R.drawable.im_voice_button_pressed);
			}

			@Override
			public void onRecordCancel() {
				// 录音取消
				mRecordBtn.setText(R.string.motalk_voice_chat_tip_1);
				mRecordBtn.setBackgroundResource(R.drawable.im_voice_button_normal);
			}
		});
		mRecordBtn.setOnTouchListener(mRecordController);

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
				// 发送GIF图片的回调
				if (mBottomListener != null) {
					mBottomListener.sendGif(resName);
				}
			}
		});

	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		mChatEdit.setFocusable(hasWindowFocus);
		mChatEdit.setFocusableInTouchMode(hasWindowFocus);
		super.onWindowFocusChanged(hasWindowFocus);
	}

	/**
	 * 改变录音按钮的状态<br/>
	 * 1、当处于非录音状态，显示录音按钮<br/>
	 * true的状态 2、当处于录音状态，显示键盘按钮<br/>
	 * false的状态
	 */
	private void changeRecordBtn(boolean show) {
		boolean isShowing = mRecordBtn.getVisibility() != View.GONE;
		if (isShowing == show) {
			return;
		}
		if (show) {
			mChatEdit.setVisibility(View.GONE);
			mRecordBtn.setVisibility(View.VISIBLE);
			mVoiceImgBtn.setImageResource(R.drawable.im_keyboard_nor);
		} else {
			mChatEdit.setVisibility(View.VISIBLE);
			mRecordBtn.setVisibility(View.GONE);
			mVoiceImgBtn.setImageResource(R.drawable.im_voice_nor);
		}
	}

	/**
	 * 改变更多按钮的状态<br/>
	 * 1、当更多布局显示时，显示隐藏按钮<br/>
	 * false的状态 2、当更多布局隐藏时，显示更多按钮<br/>
	 * true的状态
	 */
	private void changeChatToolsView(boolean show) {
		boolean isShowing = mChatToolsView.getVisibility() != View.GONE;
		if (isShowing == show) {
			return;
		}
		if (show) {
			mChatToolsView.setVisibility(View.VISIBLE);
			mMoreBtn.setBackgroundResource(R.drawable.im_btn_collapse_bg);
		} else {
			mChatToolsView.setVisibility(View.GONE);
			mMoreBtn.setBackgroundResource(R.drawable.im_btn_more_bg);
		}
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
						changeChatToolsView(false);
						changeRecordBtn(false);
					}
				}, mDelayTime);
			}
			break;
		case R.id.more_btn:
			if (mChatToolsView.getVisibility() != View.GONE) {// 表情布局在显示,那么点击则是隐藏表情，显示键盘
				mInputManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
				changeChatToolsView(false);
			} else {// 更多布局没有显示,那么点击则是显示更多，隐藏表情、录音、键盘布局
				mInputManager.hideSoftInputFromWindow(mChatEdit.getApplicationWindowToken(), 0);
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						changeChatFaceView(false);
						changeChatToolsView(true);
						changeRecordBtn(false);
					}
				}, mDelayTime);
			}
			break;
		case R.id.chat_edit:// 隐藏其他所有布局，显示键盘
			changeChatFaceView(false);
			changeChatToolsView(false);
			changeRecordBtn(false);
			break;
		case R.id.voice_img_btn:
			if (mRecordBtn.getVisibility() != View.GONE) {// 录音布局在显示,那么点击则是隐藏录音，显示键盘
				mInputManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
				changeRecordBtn(false);
			} else {// 录音布局没有显示,那么点击则是显示录音，隐藏表情、更多、键盘布局
				mInputManager.hideSoftInputFromWindow(mChatEdit.getApplicationWindowToken(), 0);
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						changeChatFaceView(false);
						changeChatToolsView(false);
						changeRecordBtn(true);
					}
				}, mDelayTime);
			}
			break;

		/*************************** 底部栏事件回调 **************************/
		case R.id.send_btn:// 发送文字的回调
			if (mBottomListener != null) {
				String msg = mChatEdit.getText().toString().trim();
				if (TextUtils.isEmpty(msg)) {
					return;
				}
				mBottomListener.sendText(msg);
				mChatEdit.setText("");
			}
			break;

		/********** Chat Tool View 的事件 *********/

		case R.id.im_photo_tv:// 选择图片的回调
			if (mBottomListener != null) {
				mBottomListener.clickPhoto();
			}
			break;
		case R.id.im_camera_tv:// 照相的回调
			if (mBottomListener != null) {
				mBottomListener.clickCamera();
			}
			break;
		case R.id.im_video_tv:// 视频通话
			if (mBottomListener != null) {
				mBottomListener.clickVideo();
			}
			break;
		case R.id.im_audio_tv:// 语音通话
			if (mBottomListener != null) {
				mBottomListener.clickAudio();
			}
			break;
		case R.id.im_file_tv:// 发送文件
			if (mBottomListener != null) {
				mBottomListener.clickFile();
			}
			break;
		case R.id.im_loc_tv:// 地理位置
			if (mBottomListener != null) {
				mBottomListener.clickLocation();
			}
			break;
		case R.id.im_card_tv:// card
			if (mBottomListener != null) {
				mBottomListener.clickCard();
			}
			break;
		}

	}

	public void reset() {
		changeChatFaceView(false);
		changeChatToolsView(false);
		changeRecordBtn(false);
		mInputManager.hideSoftInputFromWindow(mChatEdit.getApplicationWindowToken(), 0);
	}

	public void setChatBottomListener(ChatBottomListener listener) {
		mBottomListener = listener;
	}

	public static interface ChatBottomListener {
		public void stopVoicePlay();

		public void sendText(String text);

		public void sendGif(String text);

		public void sendVoice(String filePath, int timeLen);

		public void clickPhoto();

		public void clickCamera();

		public void clickVideo();

		public void clickAudio();

		public void clickFile();

		public void clickLocation();

		public void clickCard();
	}

	public void recordCancel() {
		if (mRecordController != null) {
			mRecordController.cancel();
		}
	}

}
package com.lsjr.zizi.mvp.home.session;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andview.autofresh.BGANormalRefreshViewHolder;
import com.andview.autofresh.BGARefreshLayout;
import com.andview.myrvview.LQRRecyclerView;
import com.cjt2325.cameralibrary.JCameraView;
import com.lqr.audio.AudioRecordManager;
import com.lqr.audio.IAudioRecordListener;
import com.lqr.emoji.EmotionKeyboard;
import com.lqr.emoji.EmotionLayout;
import com.lqr.emoji.IEmotionExtClickListener;
import com.lqr.emoji.IEmotionSelectedListener;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;
import com.lqr.imagepicker.ui.ImagePreviewActivity;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.AppConst;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.bean.LocationData;
import com.lsjr.zizi.chat.broad.MsgBroadcast;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.ChatMessage;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.utils.FileUtils;
import com.lsjr.zizi.chat.xmpp.CoreService;
import com.lsjr.zizi.chat.xmpp.ListenerManager;
import com.lsjr.zizi.chat.xmpp.listener.ChatMessageListener;
import com.lsjr.zizi.chat.xmpp.listener.ChatReadStateListener;
import com.lsjr.zizi.chat.xmpp.listener.MucListener;
import com.lsjr.zizi.mvp.circledemo.utils.CommonUtils;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.mvp.home.photo.ImageGridActivity;
import com.lsjr.zizi.mvp.home.photo.MyLocationActivity;
import com.lsjr.zizi.mvp.home.photo.TakePhotoActivity;
import com.lsjr.zizi.mvp.home.photo.view.ISessionAtView;
import com.lsjr.zizi.mvp.home.session.presenter.ChatAtPresenter;
import com.lsjr.zizi.mvp.home.session.presenter.MessageUtils;
import com.lsjr.zizi.mvp.home.zichat.AddressListActivity;
import com.lsjr.zizi.mvp.home.zichat.GroupInfoActivity;
import com.nostra13.universalimageloader.utils.L;
import com.ymz.baselibrary.AppCache;
import com.ymz.baselibrary.BaseApplication;
import com.ymz.baselibrary.image.Compressor;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ymz.baselibrary.view.PermissionListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static com.lsjr.zizi.mvp.home.photo.ImageGridActivity.REQUEST_PERMISSION_STORAGE;

/**
 * 会话界面（单聊、群聊）
 */
public class ChatActivity extends MvpActivity<ChatAtPresenter> implements ISessionAtView,
        IEmotionSelectedListener,
        BGARefreshLayout.BGARefreshLayoutDelegate
         ,MucListener,ChatReadStateListener , ChatMessageListener {

    private static ChatReadStateListener chatReadState;
    private static final int REQUEST_CODE_SELECT_FILE = 4;
    public static final int REQUEST_IMAGE_PICKER = 1000;
    public final static int REQUEST_TAKE_PHOTO = 1001;
    public final static int REQUEST_MY_LOCATION = 1002;
    public final static int SESSION_TYPE_PRIVATE = 1;
    public final static int SESSION_TYPE_GROUP = 2;
    private boolean mIsFirst = false;
    @BindView(R.id.llRoot)
    LinearLayout mLlRoot;
    @BindView(R.id.llContent)
    LinearLayout mLlContent;
    @BindView(R.id.refreshLayout)
    BGARefreshLayout mRefreshLayout;
    @BindView(R.id.rvMsg)
    LQRRecyclerView mRvMsg;
    @BindView(R.id.ivAudio)
    ImageView mIvAudio;
    @BindView(R.id.btnAudio)
    Button mBtnAudio;
    @BindView(R.id.etContent)
    EditText mEtContent;
    @BindView(R.id.ivEmo)
    ImageView mIvEmo;
    @BindView(R.id.ivMore)
    ImageView mIvMore;
    @BindView(R.id.btnSend)
    Button mBtnSend;

    @BindView(R.id.ivAlbum)
    ImageView ivAlbum;
    @BindView(R.id.rlTakePhoto)
    ImageView rlTakePhoto;
    @BindView(R.id.ivLocation)
    ImageView ivLocation;
    @BindView(R.id.rlTakeCrama)
    ImageView rlTakeCrama;
    @BindView(R.id.ffaudio)
    RelativeLayout ffaudio;

    @BindView(R.id.flEmotionView)
    FrameLayout mFlEmotionView;
    @BindView(R.id.elEmotion)
    EmotionLayout mElEmotion;
    @BindView(R.id.llMore)
    LinearLayout mLlMore;


    @BindView(R.id.rlLocation)
    RelativeLayout mRlLocation;
    @BindView(R.id.rlRedPacket)
    RelativeLayout mRlRedPacket;
    @BindView(R.id.im_card_tvrlRly)
    RelativeLayout mRlCard;
    @BindView(R.id.im_file_tvrlRly)
    RelativeLayout mRlFile;
    private String mLoginUserId;
    private EmotionKeyboard mEmotionKeyboard;
    private Friend mFriend;// 存储所有的当前聊天对象
    public static final String FRIEND = "friend";
    private List<ChatMessage> chatMessageList=new ArrayList<>();
    private MessageUtils messageUtils;
    private PopupWindow mRecordWindow;
    private View windowView;

    /*--------------增加群聊------------------*/
    private String[] noticeFriendList;
    private boolean isGroupChat;// 是否是群聊
    private String mUseId;// 当前聊天对象的UserId
    private String mRoomName;// 当前聊天对象的昵称（房间就是房间名称）
    private boolean isError = false;
    private String mLoginNickName;

    public void initAudioRecord() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO
                ,  Manifest.permission.MODIFY_AUDIO_SETTINGS
        }, new PermissionListener() {
            @Override
            public void onGranted() {
                initAudioRecordManager();
            }

            @Override
            public void onDenied(List<String> deniedPermissions) {
            }
        });
        initAudioRecordManager();
    }


    @Override
    protected int getLayoutId() {
        return R.layout.activity_session;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        initAudioRecord();
        initEmotionKeyboard();
        initRefreshLayout();
        initListener();
        initDataChat();

    }

    @Override
    protected void initTitle() {
        super.initTitle();
        setTopLeftButton(R.drawable.ic_back);
        setTopRightButton(R.drawable.actionbar_particular_icon, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGroupChat){
                    Intent intent=new Intent(ChatActivity.this,GroupInfoActivity.class);
                    intent.putExtra(Constants.EXTRA_USER_ID, mUseId);
                    startActivity(intent);
                }else {
                    Intent intent = new Intent(ChatActivity.this, BasicInfoActivity.class);
                    intent.putExtra(AppConfig.EXTRA_USER_ID, mFriend.getUserId());
                    startActivity(intent);
                    //startActivityForResult(intent,100);
                }

            }
        });


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void nameUpdate(String update) {
        if (!TextUtils.isEmpty(update)){
            L_.e("nameUpdate---------->"+update);
             setTitleText(update);
        }
    }


    protected void initDataChat() {

        mvpPresenter.init();
        mvpPresenter.getChatContentAdapter().setUserOnClickListener(userId -> {
            Intent intent = new Intent(UIUtils.getContext(), BasicInfoActivity.class);
            intent.putExtra(AppConfig.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        Bundle bundle = null;
        if (savedInstanceState != null) {
            bundle = savedInstanceState;
            mFriend = (Friend) savedInstanceState.getSerializable(Constants.EXTRA_FRIEND);
        } else if (getIntent() != null) {
            bundle = getIntent().getExtras();
        }
        if (bundle != null) {
            mUseId = bundle.getString(Constants.EXTRA_USER_ID);
            mRoomName = bundle.getString(Constants.EXTRA_NICK_NAME);
            isGroupChat = bundle.getBoolean(Constants.EXTRA_IS_GROUP_CHAT, false);
            noticeFriendList=bundle.getStringArray(Constants.GROUP_JOIN_NOTICE);//获得加入群新朋友的列表
            //单聊
            mFriend = (Friend) getIntent().getSerializableExtra(Constants.EXTRA_FRIEND);
        }

        if (isGroupChat){
            if (TextUtils.isEmpty(mUseId) || TextUtils.isEmpty(mRoomName)) {
                isError = true;
                return;
            }
            mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
            mLoginNickName = ConfigApplication.instance().mLoginUser.getNickName();
            ListenerManager.getInstance().addMucListener(this);
            mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUseId);
        }

        ListenerManager.getInstance().addChatMessageListener(this);
        messageUtils=new MessageUtils(chatMessageList);
        messageUtils.setmFriend(mFriend);
        messageUtils.setGroupChat(isGroupChat);
        if (mFriend!=null&&!TextUtils.isEmpty(mFriend.getUserId())){
            //表示已读
            ListenerManager.getInstance().setChatReadStateListener(this);
            FriendDao.getInstance().markUserMessageRead(mLoginUserId, mFriend.getUserId());
        }else {
            return;
        }
        bindService(CoreService.getIntent(), mConnection, BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter(Constants.CHAT_MESSAGE_DELETE_ACTION);
        registerReceiver(broadcastReceiver, filter);

        super.onCreate(savedInstanceState);
        //setTitleText(mFriend.getRemarkName());
        setTitleText(mFriend.getRemarkName()==null?mFriend.getNickName():mFriend.getRemarkName());

    }



    @Override
    protected ChatAtPresenter createPresenter() {
        return new ChatAtPresenter(this,messageUtils);
    }
    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messageUtils.setmService(((CoreService.CoreServiceBinder) service).getService());
            if (isGroupChat){
                Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUseId);
                messageUtils.getmService().joinMucChat(mUseId, mLoginNickName, friend.getTimeSend());
            }
        }
    };


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            L_.d("wang", "接收到广播");

            int position = intent.getIntExtra(Constants.CHAT_REMOVE_MESSAGE_POSITION, 10000);
            if (position == 10000) {
                return;
            }
            messageUtils.getmChatMessages().remove(position);
            mvpPresenter.getChatContentAdapter().notifyDataSetChanged();
        }
    };


    @SuppressLint("ClickableViewAccessibility")
    public void initListener() {
        /*发送表情事件*/
        mElEmotion.setEmotionSelectedListener(this);
        mElEmotion.setEmotionAddVisiable(true);
        mElEmotion.setEmotionSettingVisiable(true);
        mElEmotion.setEmotionExtClickListener(new IEmotionExtClickListener() {
            @Override
            public void onEmotionAddClick(View view) {
                UIUtils.showToast("add");
            }

            @Override
            public void onEmotionSettingClick(View view) {
                UIUtils.showToast("setting");
            }
        });
        mLlContent.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case ACTION_DOWN:
                    closeBottomAndKeyboard();
                    break;
            }
            return false;
        });

        mRvMsg.setOnTouchListener((v, event) -> {
            //T_.showToastReal("隐藏键盘");
          //  CommonUtils.hideSoftInput( mEtContent.getContext(),  mEtContent);
            closeBottomAndKeyboard();
            mEmotionKeyboard.hideSoftInput();
            return false;
        });


        mRvMsg.setOnScrollListenerExtension(new LQRRecyclerView.OnScrollListenerExtension() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                   // Glide.with(UIUtils.getContext()).resumeRequests();
                   // L_.e("开始加载图片");
                } else {
                   // Glide.with(UIUtils.getContext()).pauseRequests();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

            }
        });

        mIvAudio.setOnClickListener(v -> {
            //TODO
            if (mBtnAudio.isShown()) {
                hideAudioButton();
                mEtContent.requestFocus();
                if (mEmotionKeyboard != null) {
                    mEmotionKeyboard.showSoftInput();
                }
                if (mEtContent.getText().toString().trim().length() > 0){
                    mBtnSend.setVisibility(View.VISIBLE);
                }
            } else {
                mEtContent.clearFocus();
                showAudioButton();
                hideEmotionLayout();
                hideMoreLayout();
                mBtnSend.setVisibility(View.GONE);
            }
            UIUtils.postTaskDelay(() -> mRvMsg.smoothMoveToPosition(mRvMsg.getAdapter().getItemCount() - 1), 50);
        });
        mEtContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mEtContent.getText().toString().trim().length() > 0) {
                    mBtnSend.setVisibility(View.VISIBLE);
                    mIvMore.setVisibility(View.GONE);
                } else {
                    mBtnSend.setVisibility(View.GONE);
                    mIvMore.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mEtContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                UIUtils.postTaskDelay(() ->
                        mRvMsg.smoothMoveToPosition(mRvMsg.getAdapter().getItemCount() - 1), 50);
            }


        });
        mBtnSend.setOnClickListener(v -> mvpPresenter.sendTextMsg());
        mBtnAudio.setOnTouchListener((v, event) -> {
            L_.e(""+event.getAction());
            switch (event.getAction()) {
                case ACTION_DOWN:
                    AudioRecordManager.getInstance(ChatActivity.this).startRecord();
                    break;
                case ACTION_MOVE:
                    if (isCancelled(v, event)) {
                        AudioRecordManager.getInstance(ChatActivity.this).willCancelRecord();
                    } else {
                        AudioRecordManager.getInstance(ChatActivity.this).continueRecord();
                    }
                    break;
                case ACTION_UP:
                case ACTION_CANCEL:
                    AudioRecordManager.getInstance(ChatActivity.this).stopRecord();
                    AudioRecordManager.getInstance(ChatActivity.this).destroyRecord();
                    break;
            }

            return false;
        });

//        mRlAlbum.setOnClickListener(v -> {
//            Intent intent = new Intent(this, ImageGridActivity.class);
//            startActivityForResult(intent, REQUEST_IMAGE_PICKER);
//        });
//        mRlTakePhoto.setOnClickListener(v -> {
//            Intent intent = new Intent(ChatActivity.this, TakePhotoActivity.class);
//            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
//        });

        ivAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatActivity.this, ImageGridActivity.class);
            startActivityForResult(intent, REQUEST_IMAGE_PICKER);
            }
        });
        rlTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatActivity.this, TakePhotoActivity.class);
                Bundle bundle=new Bundle();
                bundle.putInt("take",JCameraView.BUTTON_STATE_ONLY_RECORDER);
                intent.putExtras(bundle);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            }
        });

        rlTakeCrama.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatActivity.this, TakePhotoActivity.class);
                Bundle bundle=new Bundle();
                bundle.putInt("take",JCameraView.BUTTON_STATE_ONLY_CAPTURE);
                intent.putExtras(bundle);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            }
        });


        rlTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatActivity.this, TakePhotoActivity.class);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            }
        });

        ivLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(ChatActivity.this, MyLocationActivity.class);
                Intent intent = new Intent(ChatActivity.this, AddressListActivity.class);
                startActivityForResult(intent, REQUEST_MY_LOCATION);
            }
        });
        mRlLocation.setOnClickListener(v -> {
            //Intent intent = new Intent(ChatActivity.this, MyLocationActivity.class);
            Intent intent = new Intent(ChatActivity.this, AddressListActivity.class);
            startActivityForResult(intent, REQUEST_MY_LOCATION);
        });
        mRlCard.setOnClickListener(v -> {
            mvpPresenter.sendCard(mLoginUserId);
        });
        mRlFile.setOnClickListener(v -> {
            // Intent intent = new Intent(mContext, MemoryFileManagement.class);
            // startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
            Intent target = FileUtils.createGetContentIntent();
            // Create the chooser Intent
            Intent intent = Intent.createChooser(target, "选择发送文件");
            try {
                startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
            } catch (ActivityNotFoundException e) {
                // The reason for the existence of aFileChooser
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_STORAGE) {
            initAudioRecordManager();
        } else {
            UIUtils.showToast("权限被禁止");
        }

    }

    private void initAudioRecordManager() {
        AudioRecordManager.getInstance(this).setMaxVoiceDuration(AppConst.DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND);
        if (AppCache.getInstance().getmVoicesDir() == null) {
            UIUtils.showToast("创建文件夹失败");
            return;
        }
        File audioDir = AppCache.getInstance().getmVoicesDir();
        AudioRecordManager.getInstance(this).setAudioSavePath(audioDir.getAbsolutePath());
        AudioRecordManager.getInstance(this).setAudioRecordListener(new IAudioRecordListener() {

            private TextView mTimerTV;
            private TextView mStateTV;
            private ImageView mStateIV;

            @Override
            public void initTipView() {
                windowView = View.inflate(ChatActivity.this, R.layout.popup_audio_wi_vo, null);
                mStateIV = (ImageView) windowView.findViewById(R.id.rc_audio_state_image);
                mStateTV = (TextView) windowView.findViewById(R.id.rc_audio_state_text);
                mTimerTV = (TextView) windowView.findViewById(R.id.rc_audio_timer);
                mRecordWindow = new PopupWindow(windowView, -1, -1);
                mRecordWindow.showAtLocation(mLlRoot, 17, 0, 0);
                mRecordWindow.setFocusable(true);
                mRecordWindow.setOutsideTouchable(false);
                mRecordWindow.setTouchable(false);
            }

            @Override
            public void setTimeoutTipView(int counter) {
                if (mRecordWindow != null) {
                    this.mStateIV.setVisibility(View.GONE);
                    this.mStateTV.setVisibility(View.VISIBLE);
                    this.mStateTV.setText(R.string.voice_rec);
                    this.mStateTV.setBackgroundResource(R.drawable.bg_voice_popup);
                    this.mTimerTV.setText(String.format("%s", new Object[]{Integer.valueOf(counter)}));
                    this.mTimerTV.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void setRecordingTipView() {
                if (mRecordWindow != null) {
                    this.mStateIV.setVisibility(View.VISIBLE);
                    this.mStateIV.setImageResource(R.mipmap.ic_volume_1);
                    this.mStateTV.setVisibility(View.VISIBLE);
                    this.mStateTV.setText(R.string.voice_rec);
                    this.mStateTV.setBackgroundResource(R.drawable.bg_voice_popup);
                    this.mTimerTV.setVisibility(View.GONE);
                }
            }

            @Override
            public void setAudioShortTipView() {
                if (mRecordWindow != null) {
                    mStateIV.setImageResource(R.mipmap.ic_volume_wraning);
                    mStateTV.setText(R.string.voice_short);
                }
            }

            @Override
            public void setCancelTipView() {
                if (mRecordWindow != null) {
                    this.mTimerTV.setVisibility(View.GONE);
                    this.mStateIV.setVisibility(View.VISIBLE);
                    this.mStateIV.setImageResource(R.mipmap.ic_volume_cancel);
                    this.mStateTV.setVisibility(View.VISIBLE);
                    this.mStateTV.setText(R.string.voice_cancel);
                    this.mStateTV.setBackgroundResource(R.drawable.corner_voice_style);
                }
            }

            @Override
            public void destroyTipView() {
                if (mRecordWindow != null) {
                    mRecordWindow.dismiss();
                    mRecordWindow = null;
                    this.mStateIV = null;
                    this.mStateTV = null;
                    this.mTimerTV = null;
                }
            }

            @Override
            public void onStartRecord() {
                // RongIMClient.getInstance().sendTypingStatus(mConversationType, mSessionId, VoiceMessage.class.getAnnotation(MessageTag.class).value());
            }

            @Override
            public void onFinish(Uri audioPath, int duration) {
                if(mvpPresenter==null)return;
                mvpPresenter.sendVoice(audioPath, duration);
            }

            @Override
            public void onAudioDBChanged(int db) {
                if (mStateIV==null)return;
                switch (db / 5) {
                    case 0:
                        this.mStateIV.setImageResource(R.mipmap.ic_volume_1);
                        break;
                    case 1:
                        this.mStateIV.setImageResource(R.mipmap.ic_volume_2);
                        break;
                    case 2:
                        this.mStateIV.setImageResource(R.mipmap.ic_volume_3);
                        break;
                    case 3:
                        this.mStateIV.setImageResource(R.mipmap.ic_volume_4);
                        break;
                    case 4:
                        this.mStateIV.setImageResource(R.mipmap.ic_volume_5);
                        break;
                    case 5:
                        this.mStateIV.setImageResource(R.mipmap.ic_volume_6);
                        break;
                    case 6:
                        this.mStateIV.setImageResource(R.mipmap.ic_volume_7);
                        break;
                    default:
                        this.mStateIV.setImageResource(R.mipmap.ic_volume_8);
                }
            }
        });
    }


    private boolean isCancelled(View view, MotionEvent event) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return event.getRawX() < location[0] || event.getRawX() > location[0] + view.getWidth()
                || event.getRawY() < location[1] - 40;
    }


    @Override
    public void finish() {
        super.finish();
    }

    private void doBack() {
        if (messageUtils.ismHasSend()) {
            MsgBroadcast.broadcastMsgUiUpdate(UIUtils.getContext());
        }
    }

    /**
     * 给新加入群的小伙伴们发通知
     */
    private void sendNoticeJoinNewFriend(){
        if(noticeFriendList!=null){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //sendNotice("新加入的小伙伴们,快来聊天吧!");
                    messageUtils.sendNotice("新加入的小伙伴们,快来聊天吧!");
                    noticeFriendList=null;//防止重复发送提示消息
                }
            },1000);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (isGroupChat){
            sendNoticeJoinNewFriend();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mIsFirst) {
            mEtContent.clearFocus();
        } else {
            mIsFirst = false;
        }

    }

    @Override
    protected void onPause() {
        if (isError) {
            super.onPause();
            return;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        doBack();
        super.onDestroy();
        mRecordWindow=null;
        windowView=null;
        if (isGroupChat){
            ListenerManager.getInstance().removeMucListener(this);
        }
        //表示已读
        ListenerManager.getInstance().setChatReadStateListener(null);
        //ListenerManager.getInstance().addChatMessageListener(this);
        ListenerManager.getInstance().removeChatMessageListener(this);
        AudioRecordManager.getInstance(this).stopRecord();
        AudioRecordManager.getInstance(this).destroyRecord();
        unbindService(mConnection);
        unregisterReceiver(broadcastReceiver);
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_IMAGE_PICKER:
                if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {//返回多张照片
                    if (data != null) {
                        //是否发送原图
                        boolean isOrig = data.getBooleanExtra(ImagePreviewActivity.ISORIGIN, true);
                        ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                        L_.e(isOrig ? "发原图" : "不发原图");//若不发原图的话，需要在自己在项目中做好压缩图片算法
                        for (ImageItem imageItem : images) {
                            File imageFileThumb;
                            File imageFileSource = null;

                            if (isOrig) {
                                imageFileSource = new File(imageItem.path);
                               // imageFileThumb = ImageUtils.genThumbImgFile(imageItem.path);
                            } else {
                                imageFileSource = new File(imageItem.path);
                                //压缩图片
                               // imageFileSource = ImageUtils.genThumbImgFile(imageItem.path);
                               // imageFileThumb = ImageUtils.genThumbImgFile(imageFileSource.getAbsolutePath());
                            }

                            L_.e("压缩前：图片大小" + imageFileSource.length() / 1024 + "k");
                            try {
                                File  compressedImageFile = new Compressor(this).compressToFile(imageFileSource);
                                L_.e("压缩后：图片大小" + compressedImageFile.length() / 1024 + "k");
                                mvpPresenter.sendImgMsg(compressedImageFile, compressedImageFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                                mvpPresenter.sendImgMsg(imageFileSource, imageFileSource);
                            }
                            //  File newFile = CompressHelper.getDefault(this).compressToFileResize(imageFileSource);
                           // L_.e("压缩后：图片大小" + newFile.length() / 1024 + "k");
                           // mvpPresenter.sendImgMsg(imageFileSource, imageFileSource);
                        }
                    }
                }
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    String path = data.getStringExtra("path");
                    if (data.getBooleanExtra("take_photo", true)) {
                        //照片
                        //mvpPresenter.sendImgMsg(new File(path), new File(path));
                      //  File newFile = CompressHelper.getDefault(this).compressToFileResize(new File(path));
                        try {
                            File  compressedImageFile = new Compressor(this).compressToFile(new File(path));
                            mvpPresenter.sendImgMsg(compressedImageFile,compressedImageFile);
                        } catch (IOException e) {
                            mvpPresenter.sendImgMsg(new File(path),new File(path));
                            e.printStackTrace();
                        }
                        //ImageUtils.genThumbImgFile(path)

                    } else {
                        //小视频
                        // mvpPresenter.sendFileMsg(new File(path));
                        mvpPresenter.sendAudioFile(new File(path));
                    }
                }
                break;
            case REQUEST_MY_LOCATION:
                if (resultCode == RESULT_OK) {
                    LocationData locationData = (LocationData) data.getSerializableExtra("location");
                    mvpPresenter.sendLocationMessage(locationData);
                }
                break;
            case REQUEST_CODE_SELECT_FILE:
                if (resultCode == RESULT_OK) {
                    String filePath = null;
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        try {
                            // Get the file path from the URI
                            filePath = FileUtils.getPath(this, uri);
                        } catch (Exception e) {
                            Log.e("roamer", "File select error", e);
                        }
                    }
                    // String filePath = data.getStringExtra(AppConstant.FILE_PAT_NAME);
                    if (TextUtils.isEmpty(filePath)) {
                        return;
                    }
                    File file = new File(filePath);
                    L_.e("roamer" + file.getAbsolutePath());
                    if (!file.exists()) {
                        return;
                    }
                    mvpPresenter.sendFileMsg(file);
                }

            case 100:
                setTitleText(mFriend.getRemarkName()==null?mFriend.getNickName():mFriend.getRemarkName());
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);

    }



    private void initRefreshLayout() {
        mRefreshLayout.setDelegate(this);
        BGANormalRefreshViewHolder refreshViewHolder = new BGANormalRefreshViewHolder(this, false);
        refreshViewHolder.setRefreshingText("");
        refreshViewHolder.setPullDownRefreshText("");
        refreshViewHolder.setReleaseRefreshText("");
        mRefreshLayout.setRefreshViewHolder(refreshViewHolder);
    }

    private void initEmotionKeyboard() {
        mElEmotion.attachEditText(mEtContent);
        mEmotionKeyboard = EmotionKeyboard.with(this);
        mEmotionKeyboard.bindToEditText(mEtContent);
        mEmotionKeyboard.bindToContent(mLlContent);
        mEmotionKeyboard.setEmotionLayout(mFlEmotionView);
        mEmotionKeyboard.bindToEmotionButton(mIvEmo, mIvMore);
        mEmotionKeyboard.setOnEmotionButtonOnClickListener(view -> {
            switch (view.getId()) {
                case R.id.ivEmo:
                    UIUtils.postTaskDelay(() -> mRvMsg.smoothMoveToPosition(mRvMsg.getAdapter().getItemCount() - 1), 50);
                    mEtContent.clearFocus();
                    if (!mElEmotion.isShown()) {
                        if (mLlMore.isShown()) {
                            showEmotionLayout();
                            hideMoreLayout();
                            hideAudioButton();
                            return true;
                        }
                    } else if (mElEmotion.isShown() && !mLlMore.isShown()) {
                       // mIvEmo.setImageResource(R.mipmap.ic_cheat_emo);
                        mIvEmo.setImageResource(R.drawable.chat_bto_biaoqing);
                        return false;
                    }
                    showEmotionLayout();
                    hideMoreLayout();
                    hideAudioButton();
                    if (mEtContent.getText().toString().trim().length() > 0){
                        mBtnSend.setVisibility(View.VISIBLE);
                    }
                    break;
                case R.id.ivMore:
                    UIUtils.postTaskDelay(() -> mRvMsg.smoothMoveToPosition(mRvMsg.getAdapter().getItemCount() - 1), 50);
                    mEtContent.clearFocus();
                    if (!mLlMore.isShown()) {
                        if (mElEmotion.isShown()) {
                            showMoreLayout();
                            hideEmotionLayout();
                            hideAudioButton();
                            return true;
                        }
                    }
                    showMoreLayout();
                    hideEmotionLayout();
                    hideAudioButton();
                    break;
                case R.id.ivAlbum:
                    Intent intent = new Intent(this, ImageGridActivity.class);
                    startActivityForResult(intent, REQUEST_IMAGE_PICKER);
                    break;
                case R.id.rlTakePhoto:
                    Intent intent1 = new Intent(ChatActivity.this, TakePhotoActivity.class);
                   startActivityForResult(intent1, REQUEST_TAKE_PHOTO);
                    break;
                case R.id.ivAudio:
                    UIUtils.postTaskDelay(() -> mRvMsg.smoothMoveToPosition(mRvMsg.getAdapter().getItemCount() - 1), 50);
                    mEtContent.clearFocus();
                    if (!mElEmotion.isShown()) {
                        if (ffaudio.isShown()) {
                            showEmotionLayout();
                            hideMoreLayout();
                            hideAudioButton();
                            return true;
                        }
                    } else if (mElEmotion.isShown() && !ffaudio.isShown()) {
                        // mIvEmo.setImageResource(R.mipmap.ic_cheat_emo);
                        mIvEmo.setImageResource(R.drawable.chat_bto_biaoqing);
                        return false;
                    }
                    showEmotionLayout();
                    hideMoreLayout();
                    hideAudioButton();
                    break;
            }
            return false;
        });
    }

    private void showAudioButton() {
        mBtnAudio.setVisibility(View.VISIBLE);
        mEtContent.setVisibility(View.GONE);
       // mIvAudio.setImageResource(R.mipmap.ic_cheat_keyboard);
        mIvAudio.setImageResource(R.drawable.chat_input);

        if (mFlEmotionView.isShown()) {
            if (mEmotionKeyboard != null) {
                mEmotionKeyboard.interceptBackPress();
            }
        } else {
            if (mEmotionKeyboard != null) {
                mEmotionKeyboard.hideSoftInput();
            }
        }
    }

    private void hideAudioButton() {
        mBtnAudio.setVisibility(View.GONE);
        mEtContent.setVisibility(View.VISIBLE);
        //mIvAudio.setImageResource(R.mipmap.ic_cheat_voice);
       //TODO
        mIvAudio.setImageResource(R.drawable.chat_bto_yuyin);
    }

    private void showEmotionLayout() {
        mElEmotion.setVisibility(View.VISIBLE);
        //mIvEmo.setImageResource(R.mipmap.ic_cheat_keyboard);
        mIvEmo.setImageResource(R.drawable.chat_input);
    }

    private void showAudioLayout() {
        mElEmotion.setVisibility(View.GONE);
        ffaudio.setVisibility(View.VISIBLE);
        //mIvEmo.setImageResource(R.mipmap.ic_cheat_keyboard);
        mIvEmo.setImageResource(R.drawable.chat_input);
    }


    private void hideEmotionLayout() {
        mElEmotion.setVisibility(View.GONE);
        //mIvEmo.setImageResource(R.mipmap.ic_cheat_emo);
        mIvEmo.setImageResource(R.drawable.chat_bto_biaoqing);
    }

    private void showMoreLayout() {
        mLlMore.setVisibility(View.VISIBLE);
    }

    private void hideMoreLayout() {
        mLlMore.setVisibility(View.GONE);
    }

    private void closeBottomAndKeyboard() {
        mElEmotion.setVisibility(View.GONE);
        mLlMore.setVisibility(View.GONE);
        ffaudio.setVisibility(View.GONE);
        if (mEmotionKeyboard != null) {
            mEmotionKeyboard.interceptBackPress();
            //mIvEmo.setImageResource(R.mipmap.ic_cheat_emo);
            mIvEmo.setImageResource(R.drawable.chat_bto_biaoqing);
        }

    }

    @Override
    public void onBackPressed() {
        if (mElEmotion.isShown() || mLlMore.isShown()) {
            mEmotionKeyboard.interceptBackPress();
            //mIvEmo.setImageResource(R.mipmap.ic_cheat_emo);
            mIvEmo.setImageResource(R.drawable.chat_bto_biaoqing);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void onEmojiSelected(String key) {
//        LogUtils.e("onEmojiSelected : " + key);
        L_.e("选择头像    ------>  " + key);
    }

    @Override
    public void onStickerSelected(String categoryName, String stickerName, String stickerBitmapPath) {
        L_.e("选择头像    ------>"  + stickerBitmapPath);
        //mvpPresenter.sendFileMsg(stickerBitmapPath);
        //mvpPresenter.sendFileMsg(new File(stickerBitmapPath));
        mvpPresenter.sendGif(stickerBitmapPath);
       // T_.showToastReal("暂不支持");
        //StickerManager.getInstance().getStickerBitmapPath(sticker.getCategory(), sticker.getName()))
    }

    @Override
    public BGARefreshLayout getRefreshLayout() {
        return mRefreshLayout;
    }

    @Override
    public LQRRecyclerView getRvMsg() {
        return mRvMsg;
    }

    @Override
    public EditText getEtContent() {
        return mEtContent;
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {

        UIUtils.getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mvpPresenter.loadMore();
            }
        },1000);

    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        return false;
    }


    /**
     * 新消息到来
     */
    @Override
    public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg) {
        L_.e("收到消息" + message.getContent());
        L_.e("messageUtils.getmFriend().getUserId( "+messageUtils.getmFriend().getUserId());
        L_.e("messageUtils.getmFriend().fromUserId( "+fromUserId);
        // 是该人的聊天消息
        if (messageUtils.getmFriend().getUserId().compareToIgnoreCase(fromUserId) == 0) {
            mvpPresenter.getChatContentAdapter().addLastItem(message);
            mvpPresenter.rvMoveToBottom();
            return true;
        }
        return false;
    }

    @Override
    public void onMessageSendStateChange(int messageState, int msg_id) {
        // L_.e("收到消息");
        for (int i = 0; i < messageUtils.getmChatMessages().size(); i++) {
            ChatMessage msg = messageUtils.getmChatMessages().get(i);
            if (msg_id == msg.get_id()) {
                //L_.e("收到消息后刷新");
                //msg.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                L_.e("当前状态"+messageState);
                msg.setMessageState(messageState);
                mvpPresenter.newMessageNotifyChange();
                //setAdapter();
                break;
            }
        }
    }


    @Override
    public void onDeleteMucRoom(String toUserId) {
        if (toUserId != null && toUserId.equals(mUseId)) {
            T_.showToastReal( "房间 " + mRoomName + " 已被删除");
            finish();
        }
    }

    @Override
    public void onMyBeDelete(String toUserId) {
        if (toUserId != null && toUserId.equals(mUseId)) {
            T_.showToastReal(  "你被踢出了房间：" + mRoomName);
            finish();
        }
    }

    @Override
    public void onNickNameChange(String toUserId, String changedUserId, String changedName) {
        if (toUserId != null && toUserId.equals(mUseId)) {
            if (changedUserId.equals(mLoginUserId)) {
                mFriend.setRoomMyNickName(changedName);
                //mChatContentView.setRoomNickName(changedName);
            }
            mvpPresenter.getChatContentAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onMyVoiceBanned(String toUserId, int time) {
        if (toUserId != null && toUserId.equals(mUseId)) {
            mFriend.setRoomTalkTime(time);
        }
    }

    @Override
    public void onReadind(String userId) {

        L_.e(mFriend.toString());
        if (userId.equals(mFriend.getUserId())){
            //mFriend.setUnReadNum(0);
            L_.e("设置未读消息为0");
            //MsgBroadcast.broadcastMsgNumUpdate(UIUtils.getContext(), false, mFriend.getUnReadNum());
            FriendDao.getInstance().markUserMessageRead(mLoginUserId, mFriend.getUserId());
            //FriendDao.getInstance().markUserMessageRead(ConfigApplication.instance().getLoginUserId(), userId);
            MsgBroadcast.broadcastMsgNumUpdate(UIUtils.getContext(), false, mFriend.getUnReadNum());
            MsgBroadcast.broadcastMsgNumReset(BaseApplication.getApplication());
           // MsgBroadcast.broadcastMsgNumUpdate(UIUtils.getContext(), false, mFriend.getUnReadNum());
            MsgBroadcast.broadcastMsgUiUpdate(BaseApplication.getApplication());
        }

    }
}

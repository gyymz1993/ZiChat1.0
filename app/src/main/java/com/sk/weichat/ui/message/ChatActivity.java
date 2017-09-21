package com.sk.weichat.ui.message;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.FileUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.downloader.Downloader;
import com.sk.weichat.helper.UploadEngine;
import com.sk.weichat.inter.FreshImaCallBack;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.ui.circle.BasicInfoActivity;
import com.sk.weichat.ui.circle.SendBaiDuLocate;
import com.sk.weichat.ui.me.LocalVideoActivity;
import com.sk.weichat.util.CameraUtil;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.ChatBottomView;
import com.sk.weichat.view.ChatBottomView.ChatBottomListener;
import com.sk.weichat.view.ChatContentView;
import com.sk.weichat.view.ChatContentView.MessageEventListener;
import com.sk.weichat.view.PullDownListView;
import com.sk.weichat.xmpp.CoreService;
import com.sk.weichat.xmpp.CoreService.CoreServiceBinder;
import com.sk.weichat.xmpp.ListenerManager;
import com.sk.weichat.xmpp.ReceiptManager;
import com.sk.weichat.xmpp.listener.ChatMessageListener;
import com.yanzhenjie.album.Album;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * 聊天主界面
 *
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.ui.message
 * @作者:王阳
 * @创建时间: 2015年10月9日 下午2:48:05
 * @描述: TODO
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO 修改聊天，点击用户头像，加入黑名单，返回，还可以和加入黑名单的用户聊天的bug
 */
public class ChatActivity extends ActionBackActivity
        implements MessageEventListener, ChatBottomListener, ChatMessageListener ,FreshImaCallBack {
    public static final String FRIEND = "friend";
    @SuppressWarnings("unused")
    private TextView mAuthStateTipTv;
    private ChatContentView mChatContentView;
    private ChatBottomView mChatBottomView;
    private AudioManager mAudioManager = null;
    private String mLoginUserId;
    private String mLoginNickName;
    private Friend mFriend;// 存储所有的当前聊天对象
    private List<ChatMessage> mChatMessages;// 存储聊天消息
    private Handler mHandler = new Handler();
    private CoreService mService;
    private List<Friend> mBlackList;

    private boolean mHasSend = false;// 有没有发送过消息，发送过需要更新界面
    private static final int REQUEST_CODE_SELECT_FILE = 4;
    private static final int REQUEST_CODE_SELECT_Locate = 5;
    private ChatMessage instantMessage;//转发消息传过来的message
    private String instantFilePath;//转发文件传过来的path


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
        mLoginNickName = MyApplication.getInstance().mLoginUser.getNickName();

        if (savedInstanceState != null) {
            mFriend = (Friend) savedInstanceState.getSerializable(AppConstant.EXTRA_FRIEND);
        } else if (getIntent() != null) {
            mFriend = (Friend) getIntent().getSerializableExtra(AppConstant.EXTRA_FRIEND);
        }

        mAudioManager = (AudioManager) getSystemService(android.app.Service.AUDIO_SERVICE);
        mChatMessages = new ArrayList<ChatMessage>();

        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + mLoginUserId
                + File.separator + Environment.DIRECTORY_MUSIC);
        initView();
        // 表示已读
        FriendDao.getInstance().markUserMessageRead(mLoginUserId, mFriend.getUserId());
        loadDatas(true);
        ListenerManager.getInstance().addChatMessageListener(this);
        bindService(CoreService.getIntent(), mConnection, BIND_AUTO_CREATE);

        instantMessage = (ChatMessage) getIntent().getParcelableExtra(Constants.INSTANT_MESSAGE);
        instantFilePath = getIntent().getStringExtra(Constants.INSTANT_MESSAGE_FILE);//只有转发文件才会有
        IntentFilter filter = new IntentFilter(Constants.CHAT_MESSAGE_DELETE_ACTION);
        registerReceiver(broadcastReceiver, filter);

    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("wang", "接收到广播");
            if (mChatContentView != null) {
                int position = intent.getIntExtra(Constants.CHAT_REMOVE_MESSAGE_POSITION, 10000);
                if (position == 10000) {
                    return;
                }
                mChatMessages.remove(position);
                mChatContentView.notifyDataSetInvalidated(true);
            }

        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(AppConstant.EXTRA_FRIEND, mFriend);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((CoreServiceBinder) service).getService();

        }
    };

    private void initView() {
        String remarkName = mFriend.getRemarkName();
        if (remarkName == null) {
            getSupportActionBar().setTitle(mFriend.getNickName());
        } else {
            getSupportActionBar().setTitle(remarkName);
        }
        findViewById(R.id.root_view);
        mAuthStateTipTv = (TextView) findViewById(R.id.auth_state_tip);
        mChatContentView = (ChatContentView) findViewById(R.id.chat_content_view);
        mChatContentView.setToUserId(mFriend.getUserId());
        mChatContentView.setData(mChatMessages);
        mChatContentView.setMessageEventListener(this);
        mChatContentView.setRefreshListener(new PullDownListView.RefreshingListener() {
            @Override
            public void onHeaderRefreshing() {
                loadDatas(false);
            }
        });
        mChatBottomView = (ChatBottomView) findViewById(R.id.chat_bottom_view);
        mChatBottomView.setChatBottomListener(this);
    }

    private void doBack() {
        if (mHasSend) {
            MsgBroadcast.broadcastMsgUiUpdate(mContext);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        doBack();
    }

    @Override
    protected boolean onHomeAsUp() {
        doBack();
        return true;
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mChatBottomView.recordCancel();
        ListenerManager.getInstance().removeChatMessageListener(this);
        unbindService(mConnection);
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onPause() {
        mChatContentView.reset();
        super.onPause();
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();

        mBlackList = FriendDao.getInstance().getAllBlacklists(mLoginUserId);
        instantChatMessage();

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

    }

    /**
     * 转发消息
     */
    private void instantChatMessage() {
        if (instantMessage != null) {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    int messageType = instantMessage.getType();
                    if (messageType == XmppMessage.TYPE_TEXT) {// 转发文字

                        sendText(instantMessage.getContent());
                    } else if (messageType == XmppMessage.TYPE_IMAGE) {// 转发图片
                        if (instantMessage.getFromUserId().equals(mLoginUserId)) {
                            sendImage(new File(instantMessage.getFilePath()));

                        } else {
                            File file = ImageLoader.getInstance().getDiscCache().get(instantMessage.getContent());
                            if (file == null || !file.exists()) {// 文件不存在，那么就表示需要重新下载
                                Toast.makeText(ChatActivity.this, "图片还没有下载,稍等一会哦", Toast.LENGTH_SHORT).show();
                            } else {
                                sendImage(file);
                            }
                        }
                    } else if (messageType == XmppMessage.TYPE_VOICE) {// 转发语音
                        // if(instantMessage.getFromUserId().equals(mLoginUserId)){
                        sendVoice(instantMessage.getFilePath(), instantMessage.getTimeLen());
                        // }else{
                        // }
                    } else if (messageType == XmppMessage.TYPE_LOCATION) {// 转发地址
                        sendLocate(Double.parseDouble(instantMessage.getLocation_x()),
                                Double.parseDouble(instantMessage.getLocation_y()), instantMessage.getContent());

                    } else if (messageType == XmppMessage.TYPE_VIDEO) {// 转发视频
                        sendVideo(new File(instantMessage.getFilePath()));
                    } else if (messageType == XmppMessage.TYPE_FILE && instantFilePath != null) {//转发文件
						File file=new File(instantFilePath);
                        if (file.exists()) {
                            sendFile(file);
                        } else {
                            Toast.makeText(ChatActivity.this, "文件解析错误", Toast.LENGTH_SHORT).show();
                        }
                    }
                    /*
					 * else if(messageType==XmppMessage.TYPE_CARD){
					 * sendCard(instantMessage.getObjectId()); }
					 */
                    instantMessage = null;
                }
            }, 1000);

        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /***************
     * ChatContentView的回调
     ***************************************/
    @Override
    public void onMyAvatarClick() {
        Intent intent = new Intent(mContext, BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, mLoginUserId);
        startActivity(intent);
    }

    @Override
    public void onFriendAvatarClick(String friendUserId) {
        Intent intent = new Intent(mContext, BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friendUserId);
        startActivity(intent);
    }

    @Override
    public void onMessageClick(ChatMessage chatMessage) {
    }

    @Override
    public void onMessageLongClick(ChatMessage chatMessage) {
    }

    @Override
    public void onEmptyTouch() {
        mChatBottomView.reset();
    }

    @Override
    public void onSendAgain(ChatMessage message) {
        if (interprect(message)) {
            return;
        }
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE) {
            if (!message.isUpload()) {
                UploadEngine.uploadImFile(mFriend.getUserId(), message, mUploadResponse);
            } else {
                mService.sendChatMessage(mFriend.getUserId(), message);
            }
        } else {
            mService.sendChatMessage(mFriend.getUserId(), message);
        }
        // mService.sendChatMessage(mFriend.getUserId(), chatMessage);
    }

    /**
     * 拦截发送的消息
     *
     * @param message
     */
    public boolean interprect(ChatMessage message) {
        int len = 0;
        for (Friend friend : mBlackList) {
            if (friend.getUserId().equals(mFriend.getUserId())) {
                T_.showToastReal("已经加入黑名单,无法发送消息");
                len++;
            }
        }
        Log.d("wang", "....kkkkk");
        if (len != 0) {
            // finish();
            ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, mFriend.getUserId(),
                    message.get_id(), ChatMessageListener.MESSAGE_SEND_FAILED);
            return true;
        }
        return false;
    }

    /***************
     * ChatBottomView的回调
     ***************************************/

    private void sendMessage(final ChatMessage message) {
        if (interprect(message)) {
            return;
        }
        mHasSend = true;
        Log.d("roamer", "开始发送消息,ChatBottomView的回调 sendmessage");
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), message);
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE) {
            if (!message.isUpload()) {
                Log.d("roamer", "去更新服务器的数据");
                UploadEngine.uploadImFile(mFriend.getUserId(), message, mUploadResponse);

            } else {
                Log.d("roamer", "sendChatMessage....");
                mService.sendChatMessage(mFriend.getUserId(), message);
            }
        } else {
            Log.d("roamer", "sendChatMessage");
            mService.sendChatMessage(mFriend.getUserId(), message);
        }
    }

    private UploadEngine.ImFileUploadResponse mUploadResponse = new UploadEngine.ImFileUploadResponse() {
        @Override
        public void onSuccess(String toUserId, ChatMessage message) {
            mService.sendChatMessage(mFriend.getUserId(), message);

        }

        @Override
        public void onFailure(String toUserId, ChatMessage message) {
            for (int i = 0; i < mChatMessages.size(); i++) {
                ChatMessage msg = mChatMessages.get(i);
                if (message.get_id() == msg.get_id()) {
                    msg.setMessageState(ChatMessageListener.MESSAGE_SEND_FAILED);
                    ChatMessageDao.getInstance().updateMessageSendState(mLoginUserId, mFriend.getUserId(),
                            message.get_id(), ChatMessageListener.MESSAGE_SEND_FAILED);
                    mChatContentView.notifyDataSetInvalidated(false);
                    break;
                }
            }
        }

    };

    /**
     * 停止播放聊天的录音
     */
    @Override
    public void stopVoicePlay() {
        mChatContentView.stopPlayVoice();
    }

    @Override
    public void sendText(String text) {
        Log.d("wang", "sendText");
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TEXT);
        message.setContent(text);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    public void sendGif(String text) {
        Log.d("wang", "sendgif");
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_GIF);
        message.setContent(text);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    public void sendVoice(String filePath, int timeLen) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VOICE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        message.setTimeLen(timeLen);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);

        sendMessage(message);

    }




    public void sendImage(File file) {
        if (!file.exists()) {
            L_.e("文件不存在");
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendVideo(File file) {
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VIDEO);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        String filePath = file.getAbsolutePath();
        L_.e("视频绝对路径:"+filePath);
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendFile(File file) {
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_FILE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        Log.d("roamer", "开始发送文件");
        sendMessage(message);
    }

    public void sendLocate(double latitude, double longitude, String address) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_LOCATION);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setLocation_x(latitude + "");
        message.setLocation_y(longitude + "");
        message.setContent(address);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendCard(String ObjectId) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_CARD);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setObjectId(ObjectId);
        message.setContent(MyApplication.getInstance().mLoginUser.getSex() + "");// 性别
        // 0表示女，1表示男
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    public void clickPhoto() {
        //Log.d("roamer", "clickphoto");
       // CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_PHOTO);
       // selectPicFromLocal();
        openGallery();
        mChatBottomView.reset();
    }


    /**
     * select local image
     */
    protected void selectPicFromLocal() {


        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

        } else {
            intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
    }


    @Override
    public void clickCamera() {
        Log.d("roamer", "clickCamera");
        mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
        CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_PHOTO);
        mChatBottomView.reset();
    }

    @Override
    public void clickVideo() {
        Intent intent = new Intent(mContext, LocalVideoActivity.class);
        intent.putExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_SELECT);
        startActivityForResult(intent, REQUEST_CODE_SELECT_VIDE0);
    }

    @Override
    public void clickAudio() {
    }

    @Override
    public void clickFile() {
        // Intent intent = new Intent(mContext, MemoryFileManagement.class);
        // startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
        Intent target = FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(target, getString(R.string.chooser_title));
        try {
            startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
        } catch (ActivityNotFoundException e) {
            // The reason for the existence of aFileChooser
        }
    }

    @Override
    public void clickLocation() {
        Intent intent = new Intent(mContext, SendBaiDuLocate.class);
        startActivityForResult(intent, REQUEST_CODE_SELECT_Locate);
    }

    @Override
    public void clickCard() {
        sendCard(mLoginUserId);
    }

    /**
     * 新消息到来
     */
    @Override
    public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg) {
        if (isGroupMsg) {
            return false;
        }
        if (mFriend.getUserId().compareToIgnoreCase(fromUserId) == 0) {// 是该人的聊天消息
            mChatMessages.add(message);
            mChatContentView.notifyDataSetInvalidated(true);
            return true;
        }
        return false;
    }

    @Override
    public void onMessageSendStateChange(int messageState, int msg_id) {
        for (int i = 0; i < mChatMessages.size(); i++) {
            ChatMessage msg = mChatMessages.get(i);
            if (msg_id == msg.get_id()) {
                msg.setMessageState(messageState);
                mChatContentView.notifyDataSetInvalidated(false);
                break;
            }
        }
    }

    private int mMinId = 0;
    private int mPageSize = 20;
    private boolean mHasMoreData = true;

    private void loadDatas(final boolean scrollToBottom) {
        if (mChatMessages.size() > 0) {
            mMinId = mChatMessages.get(0).get_id();
        } else {
            mMinId = 0;
        }
        List<ChatMessage> chatLists = ChatMessageDao.getInstance().getSingleChatMessages(mLoginUserId,
                mFriend.getUserId(), mMinId, mPageSize);
        if (chatLists == null || chatLists.size() <= 0) {
            mHasMoreData = false;
        } else {
            long currentTime = System.currentTimeMillis() / 1000;

            for (int i = 0; i < chatLists.size(); i++) {
                ChatMessage message = chatLists.get(i);
                if (message.isMySend() && message.getMessageState() == ChatMessageListener.MESSAGE_SEND_ING) {// 如果是我发的消息，有时候在消息发送中，直接退出了程序，此时消息发送状态可能使用是发送中，
                    if (currentTime - message.getTimeSend() > ReceiptManager.MESSAGE_DELAY / 1000) {
                        ChatMessageDao.getInstance().updateMessageSendState(mLoginUserId, mFriend.getUserId(),
                                message.get_id(), ChatMessageListener.MESSAGE_SEND_FAILED);
                        message.setMessageState(ChatMessageListener.MESSAGE_SEND_FAILED);
                    }
                }
                mChatMessages.add(0, message);
            }
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mChatContentView.notifyDataSetInvalidated(scrollToBottom);
                mChatContentView.headerRefreshingCompleted();
                if (!mHasMoreData) {
                    mChatContentView.setNeedRefresh(false);
                }
            }
        }, 1000);
    }

    /***********************
     * 拍照和选择照片
     **********************/
    private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;
    private static final int REQUEST_CODE_PICK_PHOTO = 2;
    private Uri mNewPhotoUri;

    private static final int REQUEST_CODE_SELECT_VIDE0 = 3;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("roamer", "进入到activityResult...");
        if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {// 拍照返回
            if (resultCode == Activity.RESULT_OK) {

                ///storage/emulated/0/Android/data/com.sk.weichat/files/Pictures/574411a9108046a5865ca6f4e187ae5f.jpg
                L_.e("roamer", "选择了一张图片..."+mNewPhotoUri.getPath());
                L_.e("roamer", "拍照返回...");
                if (mNewPhotoUri != null) {
                    sendImage(new File(mNewPhotoUri.getPath()));
                } else {
                    // ToastUtil.showToast(this,
                    // R.string.c_take_picture_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_GALLERY) {// 选择一张图片,然后立即调用裁减
            if (resultCode == Activity.RESULT_OK) {
                ///storage/emulated/0/DCIM/Camera/IMG_20170412_114255.jpg
                ///sdcard/DCIM/Camera/IMG_20161227_153012.jpg
                ArrayList<String> pathList = Album.parseResult(data);
                igList.clear();//不可直接指向
                igList.addAll(pathList);
                L_.e("roamer", "选择了一张图片..."+pathList);
                for (int i=0;i<igList.size();i++){
                    sendImage(new File(igList.get(i)));
                }

//                if (data != null && data.getData() != null) {
//                    //sendImage(new File(CameraUtil.sendPicByUri(this, data.getData())));
//                } else {
//                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
//                    return;
//                }
//
//               Uri selectedImage = data.getData();
//               L_.e("roamer", "选择了一张图片..."+selectedImage.getEncodedPath());
//                sendImage(new File(CameraUtil.sendPicByUri(this,selectedImage)));
                //L_.e("roamer", "选择了一张图片..."+new File(CameraUtil.getImagePathFromUri(this, data.getData())).getAbsolutePath());
            }
        } else if (requestCode == REQUEST_CODE_SELECT_VIDE0 && resultCode == RESULT_OK) {// 选择视频的返回
            if (data == null) {
                return;
            }
            String filePath = data.getStringExtra(AppConstant.EXTRA_FILE_PATH);
            if (TextUtils.isEmpty(filePath)) {
                ToastUtil.showToast(this, R.string.select_failed);
                return;
            }
            File file = new File(filePath);
            if (!file.exists()) {
                ToastUtil.showToast(this, R.string.select_failed);
                return;
            }
            sendVideo(file);
        } else if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            String filePath = null;
            if (data != null) {
                // Get the URI of the selected file
                final Uri uri = data.getData();
                Log.i(TAG, "Uri = " + uri.toString());
                try {
                    // Get the file path from the URI
                    filePath = FileUtils.getPath(this, uri);
                } catch (Exception e) {
                    Log.e("roamer", "File select error", e);
                }
            }
            // String filePath = data.getStringExtra(AppConstant.FILE_PAT_NAME);
            if (TextUtils.isEmpty(filePath)) {
                ToastUtil.showToast(this, R.string.select_failed);
                return;
            }
            File file = new File(filePath);
            Log.d("roamer", file.getAbsolutePath());
            if (!file.exists()) {
                ToastUtil.showToast(this, R.string.select_failed);
                return;
            }
            sendFile(file);
        } else if (requestCode == REQUEST_CODE_SELECT_Locate && resultCode == RESULT_OK) {
            double latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
            double longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
            String address = MyApplication.getInstance().getBdLocationHelper().getAddress();
            if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)) {
                sendLocate(latitude, longitude, address);
            } else {
                ToastUtil.showToast(mContext, "请把定位开启!");
            }
        }
    }

    private static final int REQUEST_CODE_GALLERY = 100;  //打开相册
    private static final int REQUEST_CODE_PREVIEW = 101; //图片预览
    private final static int maxImageSize = 5;
    private ArrayList<String> igList = new ArrayList<>();
    @Override
    public void previewImag(int position) {
        Album.gallery(this)//预览图片
                .requestCode(REQUEST_CODE_PREVIEW)
                .toolBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .statusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .currentPosition(position)
                .checkFunction(false)
                .start();
    }

    @Override
    public void updateGvIgShow(int postition) {
    }

    @Override
    public void openGallery() {
        Album.album(this)//打开相册
                .requestCode(REQUEST_CODE_GALLERY)
                .toolBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .statusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .selectCount(maxImageSize)
                .columnCount(3)
                .camera(true)
                .start();
    }

}

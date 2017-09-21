package com.lsjr.zizi.mvp.home.session.presenter;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.lsjr.bean.Result;
import com.lsjr.callback.StringCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.bean.UploadFileResult;
import com.lsjr.zizi.chat.dao.ChatMessageDao;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.ChatMessage;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.utils.TimeUtils;
import com.lsjr.zizi.chat.xmpp.CoreService;
import com.lsjr.zizi.chat.xmpp.ListenerManager;
import com.lsjr.zizi.chat.xmpp.XmppMessage;
import com.lsjr.zizi.chat.xmpp.listener.ChatMessageListener;
import com.lsjr.zizi.mvp.service.DataService;
import com.nostra13.universalimageloader.utils.L;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.rong.message.FileMessage;
import io.rong.message.LocationMessage;
import io.rong.message.VoiceMessage;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/7/31 9:47
 */

public class MessageUtils {

    private List<ChatMessage> mChatMessages;// 存储聊天消息
    private CoreService mService;
    public String mLoginUserId;
    private String mLoginNickName;
    private Friend mFriend;
    private boolean isGroupChat;
    private boolean mHasSend = false;// 有没有发送过消息，发送过需要更新界面
    public MessageUtils(List<ChatMessage> chatMessages){
        this.mChatMessages=chatMessages;
        mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        mLoginNickName = ConfigApplication.instance().mLoginUser.getNickName();
        getmBlackList();
        //DataService.startService(UIUtils.getContext(),chatMessages,"1");
    }

    public void setGroupChat(boolean groupChat) {
        isGroupChat = groupChat;
    }

    public boolean isGroupChat() {
        return isGroupChat;
    }

    public boolean ismHasSend() {
        return mHasSend;
    }

    public void saveOffLineMsg(ChatMessage chatMessage){
        //ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, obj.toUserId, chatMessage.get_id(),
              //  ChatMessageListener.MESSAGE_SEND_SUCCESS);
    }

    public void setmFriend(Friend mFriend) {
        this.mFriend = mFriend;
    }
    public Friend getmFriend() {
        return mFriend;
    }
    public void setmChatMessages(List<ChatMessage> mChatMessages) {
        this.mChatMessages = mChatMessages;
    }

    public List<ChatMessage> getmChatMessages() {
        return mChatMessages;
    }

    /**
     * 在集合头部添加新的数据集合（下拉从服务器获取最新的数据集合）
     */
    public void addNewData(List<ChatMessage> messages) {
        if (messages != null) {
            mChatMessages.addAll(0, messages);
           // notifyItemRangeInsertedWrapper(0, data.size());
        }
    }

    public CoreService getmService() {
        return mService;
    }
    public void setmService(CoreService service) {
        this.mService = service;
        L_.e("setmService :"+mService);
    }



    /**
     * 新加入群发送通知
     * @param text
     */
    public void sendNotice(String text){
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TIP);
        message.setContent(text);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        mChatMessages.add(message);
        sendMessage(message);
    }


    void sendText(String text) {
        L_.e("wang", "sendText");
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
        sendMessage(message);
    }

    void sendGif(String text) {
        Log.d("wang", "sendgif");
        File  file = new File(text);
        if (!file.exists()) {
            L_.e("文件不存在");
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_GIF);
        message.setContent(text);
        message.setFilePath(file.getAbsolutePath());
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        mChatMessages.add(message);
        sendMessage(message);
        //TODO 刷新界面
        //TODO 刷新界面

    }

    void sendVoice(Uri audioPath, int timeLen) {

        File file = new File(audioPath.getPath());
        L.e("file------------------>","1111111111"+file.getAbsolutePath());
        VoiceMessage voiceMessage = VoiceMessage.obtain(audioPath, timeLen);
        if (!file.exists()) {
            L.e("file------------------------>","没有此路径");
            return;
        }
        ChatMessage message = new ChatMessage();
        long fileSize = file.length();
        message.setMessageContent(voiceMessage);
        message.setType(XmppMessage.TYPE_VOICE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setFilePath(file.getAbsolutePath());
        message.setFileSize((int) fileSize);
        message.setTimeLen(timeLen);
        sendMessage(message);
        mChatMessages.add(message);
        //TODO 刷新界面

    }


    void sendImage(File file) {
        if (!file.exists()) {
            L_.e("文件不存在");
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        //message.setMessageContent(imageMessage);
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        mChatMessages.add(message);
        sendMessage(message);
        //TODO 刷新界面

    }

    void sendVideo(File file) {
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        FileMessage fileMessage=FileMessage.obtain(Uri.fromFile(file));
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VIDEO);
        message.setMessageContent(fileMessage);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        String filePath = file.getAbsolutePath();
        L_.e("视频绝对路径:" + filePath);
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        sendMessage(message);
        mChatMessages.add(message);
        //TODO 刷新界面

    }


    void sendFile(File file) {
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
        L_.e("上传文件路径"+filePath);
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        mChatMessages.add(message);
        sendMessage(message);
        Log.d("roamer", "开始发送文件");
        //TODO 刷新界面

    }


    void sendLocate(LocationMessage locationMessage, double latitude, double longitude, String address
            , String fileImage) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_LOCATION);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setLocation_x(latitude + "");
        message.setLocation_y(longitude + "");
        message.setContent(address);
        message.setFilePath(fileImage);
        message.setMessageContent(locationMessage);
        mChatMessages.add(message);
        sendMessage(message);
        //TODO 刷新界面

    }

    void sendCard(String ObjectId) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_CARD);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setObjectId(ObjectId);
        message.setContent(ConfigApplication.instance().mLoginUser.getSex() + "");// 性别
        // 0表示女，1表示男
        mChatMessages.add(message);
        sendMessage(message);
        //TODO 刷新界面
        // mChatContentView.notifyDataSetInvalidated(true);
    }

    public void onSendAgain(ChatMessage message) {
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO|| message.getType() == XmppMessage.TYPE_FILE) {
            if (!message.isUpload()) {
                postFiles(mFriend.getUserId(),message);
            } else {
                if (mService==null)return;
                if (isGroupChat){
                    mService.sendMucChatMessage(mFriend.getUserId(), message);
                }else {
                    mService.sendChatMessage(mFriend.getUserId(), message);
                }
            }
        } else {
            if (getmService()==null)return;
            if (isGroupChat){
                mService.sendMucChatMessage(mFriend.getUserId(), message);
            }else {
                mService.sendChatMessage(mFriend.getUserId(), message);
            }

        }
    }


    private void sendGroupMessage(ChatMessage message) {
        if (mFriend != null && mFriend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
            mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriend.getUserId());//重新去加载一遍数据
            if (mFriend != null && mFriend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                T_.showToastReal("你已经被禁言，不能发言");
                return;

            }
        }
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        if (isGroupChat && !TextUtils.isEmpty(mFriend.getRoomMyNickName())) {
            message.setFromUserName(mFriend.getRoomMyNickName());
        }
        ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), message);
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE) {
            if (!message.isUpload()) {
                //TODO  上传文件
                postFiles(mFriend.getUserId(),message);
            } else {
                L_.e("roamer", "开始发送消息,ChatBottomView的回调 sendmessage"+mService);
                if (mService==null)return;
                mService.sendMucChatMessage(mFriend.getUserId(), message);
               // getmService().sendChatMessage(mFriend.getUserId(), message);
            }
        } else {
            L_.e("roamer", "开始发送消息,ChatBottomView的回调 sendmessage"+mService);
            if (mService==null)return;
            //getmService().sendChatMessage(mFriend.getUserId(), message);
            mService.sendMucChatMessage(mFriend.getUserId(), message);
        }
    }


    /**
     所有发送消息的口子
     **/
    public void sendMessage(final ChatMessage message) {
        mHasSend = true;
        if (isGroupChat){
            L_.e("发送群聊消息");
            sendGroupMessage(message);
            return;
        }
        if (interprect(message)) {
            return;
        }
        L_.e("roamer", "开始发送消息,ChatBottomView的回调 sendmessage");
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        L_.e("roamer", "sendChatMessage...."+mService);
        ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), message);
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE) {
            if (!message.isUpload()) {
                L_.e("roamer", "去更新服务器的数据"+message.getType());
                //TODO  上传文件
                postFiles(mFriend.getUserId(),message);

            } else {
                L_.e("roamer", "sendChatMessage...."+mService);
                if (mService==null)return;
                mService.sendChatMessage(mFriend.getUserId(), message);
            }
        } else {
            L_.e("roamer", "sendChatMessage...."+mService);
            if (mService==null)return;
            mService.sendChatMessage(mFriend.getUserId(), message);
        }
    }


    public void postFiles(final String toUserId ,final ChatMessage message){
        Log.d("roamer", "开始上传...");
        Map<String,String> params=new HashMap<>();
        final String loginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        params.put("userId", loginUserId);
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        File file=new File(message.getFilePath());
        List<File> files=new ArrayList<>();
        files.add(file);
        L_.e("roamer", "开始上传...类型"+message.getType()+"...."+message.getFilePath());
        HttpUtils.getInstance().uploadFileWithParts(AppConfig.UPLOAD_URL, params, files, new StringCallBack() {
            @Override
            protected void onXError(String exception) {
               L.e("发送失败---------exception");
            }

            @Override
            protected void onSuccess(String response) {
                L.e("发送成功--------->");
                String url = null;
                String thumbnailUrl = null;
                UploadFileResult result = null;
                L_.e(response);
                try {
                    result = JSON.parseObject(response, UploadFileResult.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (result == null || result.getResultCode() != Result.CODE_SUCCESS || result.getData() == null
                        || result.getSuccess() != result.getTotal()) {
                } else {
                    UploadFileResult.Data data = result.getData();
                    if (message.getType() == XmppMessage.TYPE_IMAGE) {
                        if (data.getImages() != null && data.getImages().size() > 0) {
                            url = data.getImages().get(0).getOriginalUrl();
                            thumbnailUrl = data.getImages().get(0).getThumbnailUrl();
                        }
                    } else if (message.getType() == XmppMessage.TYPE_VOICE) {
                        if (data.getAudios() != null && data.getAudios().size() > 0) {
                            url = data.getAudios().get(0).getOriginalUrl();
                        }
                        if (TextUtils.isEmpty(url)){
                            url=data.getOthers().get(0).getOriginalUrl();
                        }
                    } else if (message.getType() == XmppMessage.TYPE_VIDEO) {
                        if (data.getVideos() != null && data.getVideos().size() > 0) {
                            url = data.getVideos().get(0).getOriginalUrl();
                        }
                    } else if (message.getType() == XmppMessage.TYPE_FILE){
                        if (data.getFiles() != null && data.getFiles().size() > 0) {
                            url = data.getFiles().get(0).getOriginalUrl();
                        }else if (data.getVideos() != null && data.getVideos().size() > 0) {
                            url = data.getVideos().get(0).getOriginalUrl();
                        }else if (data.getAudios() != null && data.getAudios().size() > 0) {
                            url = data.getAudios().get(0).getOriginalUrl();
                        }else if (data.getImages() != null && data.getImages().size() > 0) {
                            url = data.getImages().get(0).getOriginalUrl();
                        }else if(data.getOthers()!=null&&data.getOthers().size()>0){
                            url=data.getOthers().get(0).getOriginalUrl();
                        }
                    }
                }
                L_.e("url:"+url);
                if (TextUtils.isEmpty(url)) {
                    ChatMessageDao.getInstance().updateMessageUploadState(loginUserId, toUserId, message.get_id(), false, url);
                    for (int i = 0; i < mChatMessages.size(); i++) {
                        ChatMessage msg = mChatMessages.get(i);
                        if (message.get_id() == msg.get_id()) {
                            msg.setMessageState(ChatMessageListener.MESSAGE_SEND_FAILED);
                            ChatMessageDao.getInstance().updateMessageSendState(mLoginUserId, mFriend.getUserId(),
                                    message.get_id(), ChatMessageListener.MESSAGE_SEND_FAILED);
                            break;
                        }
                    }
                } else {
                    if (message.getType() == XmppMessage.TYPE_IMAGE) {
                        //message.setThumbnailUrl(thumbnailUrl);
                        message.setFilePath(thumbnailUrl);
                        ChatMessageDao.getInstance().updateMessageUploadState(loginUserId, toUserId, message.get_id(), true, url,thumbnailUrl);
                    }else {
                        ChatMessageDao.getInstance().updateMessageUploadState(loginUserId, toUserId, message.get_id(), true, url);

                    }
                   // ChatMessageDao.getInstance().updateMessageUploadState(loginUserId, toUserId, message.get_id(), true, url);
                    Log.d("roamer", "上传文件成功了");
                    message.setContent(url);
                    message.setUpload(true);
                    message.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                    if (getmService()==null)return;
                    if (isGroupChat){
                        getmService().sendMucChatMessage(mFriend.getUserId(), message);
                    }else {
                        getmService().sendChatMessage(mFriend.getUserId(), message);
                    }
                    //sendMessage(message);
                   // mService.sendChatMessage(mFriend.getUserId(), message);
                }
            }
        });
    }


    /**
     * 拦截发送的消息
     *
     * @param message
     */
    private List<Friend> mBlackList;//TODO 黑名单列表
    //得到当前用户的黑名单列表
    public List<Friend> getmBlackList() {
        return mBlackList = FriendDao.getInstance().getAllBlacklists(mLoginUserId);
    }

    public boolean interprect(ChatMessage message) {
        int len = 0;
        for (Friend friend : mBlackList) {
            if (friend.getUserId().equals(mFriend.getUserId())) {
                T_.showToastReal("已经加入黑名单,无法发送消息");
                len++;
            }
        }
        if (len != 0) {
            // finish();
            ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, mFriend.getUserId(),
                    message.get_id(), ChatMessageListener.MESSAGE_SEND_FAILED);
            return true;
        }
        return false;
    }


}

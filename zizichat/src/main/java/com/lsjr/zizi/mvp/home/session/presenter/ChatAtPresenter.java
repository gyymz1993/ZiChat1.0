package com.lsjr.zizi.mvp.home.session.presenter;

import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import com.lsjr.bean.ArrayResult;
import com.lsjr.callback.ChatArrayCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.bean.LocationData;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.bean.OffLineMessage;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.dao.ChatMessageDao;
import com.lsjr.zizi.chat.db.ChatMessage;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.xmpp.ListenerManager;
import com.lsjr.zizi.chat.xmpp.ReceiptManager;
import com.lsjr.zizi.chat.xmpp.XmppMessage;
import com.lsjr.zizi.chat.xmpp.listener.ChatMessageListener;
import com.lsjr.zizi.mvp.home.photo.view.ISessionAtView;
import com.lsjr.zizi.mvp.home.session.adapter.ChatContentAdapter;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.FileMessage;
import io.rong.message.ImageMessage;
import io.rong.message.LocationMessage;


public class ChatAtPresenter extends BasePresenter<ISessionAtView>  {
    private String mSessionId;
    private Conversation.ConversationType mConversationType = Conversation.ConversationType.PRIVATE;
    private ChatContentAdapter chatContentAdapter;
    private String mUserId;
    private String mLoginUserId;
    private MessageUtils messageUtils;
    private Friend mFriend;
    private boolean isFisrCreate=true;

    public ChatAtPresenter(ISessionAtView mvpView, MessageUtils msgtils) {
        super(mvpView);
        EventBus.getDefault().register(this);

        messageUtils = msgtils;
        mFriend=messageUtils.getmFriend();
        mUserId = messageUtils.getmFriend().getUserId();
        mLoginUserId = messageUtils.mLoginUserId;
    }


    /**
     * 离线消息
     */
    private void downOnlineMsg() {
        isFisrCreate=false;
        /*下载离线*/
        L_.e("下载离线消息");
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        params.put("from",mFriend.getUserId());
        params.put("to",  mFriend.getOwnerId());
        params.put("startTime",(System.currentTimeMillis()-60 * 60 * 24 )+"");
        params.put("endTime", System.currentTimeMillis()+"");
        //params.put("pageIndex", "1");
        //params.put("pageSize", "50");
        HttpUtils.getInstance().postServiceData(AppConfig.ONLINE_MSG, params, new ChatArrayCallBack<OffLineMessage>(OffLineMessage.class) {

            @Override
            protected void onXError(String exception) {
               // T_.showToastReal(exception);
            }

            @Override
            protected void onSuccess(ArrayResult<OffLineMessage> result) {

                boolean success = ResultCode.defaultParser(result, true);
                if (success){
                    List<ChatMessage> chatMessages = new ArrayList<>();
                    List<OffLineMessage> data = result.getData();
                    if (data==null||data.size()==0)return;
                    //L_.e(result.getData()+"");
                    for (OffLineMessage offLineMessage:data){
                        ChatMessage message=new ChatMessage();
                        message.setType(Integer.valueOf(offLineMessage.getType()));
                        message.setContent(offLineMessage.getContent());
                        message.setFromUserName(mFriend.getNickName());
                        message.setFromUserId(mFriend.getUserId());
                        L_.e("录音时间"+offLineMessage.getTimeLen());
                        message.setTimeLen(offLineMessage.getTimeLen());
                        message.setPacketId(offLineMessage.getMessageId());
                        //message.setTimeSend(chatMessages.);
                        //message.set_id(offLineMessage.getMessageId());
                        //L_.e("当前时间："+TimeUtils.sk_time_current_time());
                       // L_.e("离线时间："+offLineMessage.getTs());
                        message.setTimeReceive(Integer.valueOf(offLineMessage.getTs()));
                        message.setTimeSend(Integer.valueOf(offLineMessage.getTs()));
                        message.setRead(false);
                        message.setMySend(false);
                        chatMessages.add(0,message);
                        L_.e("当前离线消息ID"+ offLineMessage.getMessageId());
                    }
                    if (chatMessages==null||chatMessages.size()==0)return;
                    chatContentAdapter.addMoreData(chatMessages);
                    rvMoveToBottom();
                    for (int i=0;i<chatMessages.size();i++){
                        notifyNewMesssage(mLoginUserId,mFriend.getUserId(),chatMessages.get(i),false);
                    }
                }

            }
        });
    }

    /**
     * 离线消息存储
     */
    private void notifyNewMesssage(final String loginUserId, final String fromUserId, final ChatMessage message, final boolean isGroupMsg) {
        UIUtils.getHandler().post(() -> {
            if (message != null) {
                ChatMessageDao.getInstance().saveNewSingleChatMessage(loginUserId, fromUserId, message);
            }
        });
    }

    public void loadDatas(boolean isFisrCreate) {
        int mMinId;
        if (messageUtils.getmChatMessages().size() > 0) {
            mMinId = messageUtils.getmChatMessages().get(0).get_id();
        } else {
            mMinId = 0;
        }
        int mPageSize = 50;
        List<ChatMessage> chatLists = ChatMessageDao.getInstance().getSingleChatMessages(mLoginUserId,
                mUserId, mMinId, mPageSize);
        if (chatLists == null || chatLists.size() <= 0&&!isFisrCreate) {
            getView().getRefreshLayout().endRefreshing();
            getView().getRefreshLayout().setPullDownRefreshEnable(false);
            T_.showToastReal("没有更多数据了呦");
            //getView().getRefreshLayout().endLoadingMore();
        } else {
            List<ChatMessage> chatMessages = new ArrayList<>();
            long currentTime = System.currentTimeMillis() / 1000;
            for (int i = 0; i < chatLists.size(); i++) {
                ChatMessage message = chatLists.get(i);
                if (message.isMySend() && message.getMessageState() == ChatMessageListener.MESSAGE_SEND_ING) {
                    // 如果是我发的消息，有时候在消息发送中，直接退出了程序，此时消息发送状态可能使用是发送中，
                    if (currentTime - message.getTimeSend() > ReceiptManager.MESSAGE_DELAY / 1000) {
                        ChatMessageDao.getInstance().updateMessageSendState(mLoginUserId, mUserId,
                                message.get_id(), ChatMessageListener.MESSAGE_SEND_FAILED);
                        message.setMessageState(ChatMessageListener.MESSAGE_SEND_FAILED);
                    }
                }
                chatMessages.add(0, message);
                // messageUtils.getmChatMessages().add(0,message);

            }
            getView().getRefreshLayout().endRefreshing();
            if (chatMessages.size() == 0){
                return;
            }
            if (isFisrCreate) {
                messageUtils.getmChatMessages().clear();
                messageUtils.getmChatMessages().addAll(chatMessages);
               //scollToPosition(messageUtils.getmChatMessages().size() - 1);
                firstCreatMoveToBottom();
               // UIUtils.postTaskDelay(() -> getView().getRvMsg().smoothMoveToPosition(chatMessages.size() - 1), 200);
               // notifyChange();
            } else {
                //scollToTop();
                chatContentAdapter.addNewData(chatMessages);
                //getScollYDistance();

                //UIUtils.postTaskDelay(() -> getView().getRvMsg().smoothMoveToPosition(chatMessages.size() - 1), 200);
            }
        }
        if (isFisrCreate&&!messageUtils.isGroupChat()){
            //downOnlineMsg();
        }

    }



    public void init(){
        chatContentAdapter = new ChatContentAdapter(UIUtils.getContext(), messageUtils.getmChatMessages(), this, mUserId, messageUtils);
        getView().getRvMsg().setAdapter(chatContentAdapter);
        if (messageUtils.getmChatMessages().size()!=0&&messageUtils.getmChatMessages()!=null){
            //UIUtils.postTaskDelay(() -> getView().getRvMsg().smoothMoveToPosition(messageUtils.getmChatMessages().size() - 1), 0);
            //scollToPosition(messageUtils.getmChatMessages().size() - 1);
            firstCreatMoveToBottom();
        }
        loadDatas(true);
    }


    public void loadMore() {
        loadDatas(false);
    }


    public void newMessageNotifyChange(){
        if (getView() != null && getView().getRvMsg() != null){
            rvMoveToBottom();
            chatContentAdapter.notifyDataSetChangedWrapper();
        }

    }

    public ChatContentAdapter getChatContentAdapter() {
        return chatContentAdapter;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void dataEvent(List<ChatMessage> datas) {
        if (datas!=null&&datas.size()==0)return;
        for (ChatMessage chatMessage: datas) {
            if (chatMessage.getType()== XmppMessage.TYPE_IMAGE){
                //Bitmap bitmap = ImageLoader.load(chatMessage.getContent());
                L_.e("得到宽高 dataEvent----->bitmap.getWidth()"+chatMessage.getWidth()+"bitmap.getHeight()"+chatMessage.getHeight());
            }
            //data.setSubtype(subtype);
        }
    }


    /**
     * 第一次平滑滑到底部
     */
    private void firstCreatMoveToBottom() {
        LinearLayoutManager manager = (LinearLayoutManager) getView().getRvMsg().getLayoutManager();
        L_.e("getView().getRvMsg().getMeasuredHeight() - target.getMeasuredHeight()"+
                (getView().getRvMsg().getMeasuredHeight()));
        manager.scrollToPositionWithOffset(messageUtils.getmChatMessages().size()-1, 0);//先要滚动到这个位置
        getView().getRvMsg().post(new Runnable() {
            @Override
            public void run() {
                View target = manager.findViewByPosition(messageUtils.getmChatMessages().size()-1);//然后才能拿到这个View
                if (target != null) {
                    L_.e("getView().getRvMsg().getMeasuredHeight() - target.getMeasuredHeight()"+
                            (getView().getRvMsg().getMeasuredHeight() - target.getMeasuredHeight()));
                    L_.e("getView().getRvMsg().getMeasuredHeight() - target.getMeasuredHeight()"+
                            UIUtils.WHD()[1]);
                    manager.scrollToPositionWithOffset(messageUtils.getmChatMessages().size()-1,
                            getView().getRvMsg().getMeasuredHeight() - target.getMeasuredHeight());//滚动偏移到底部
                    //manager.scrollToPositionWithOffset(messageUtils.getmChatMessages().size()-1,
                      //      0);//滚动偏移到底部
                }
            }
        });
    }



    public void rvMoveToBottom() {
        if (getView() != null && getView().getRvMsg() != null){
            //scollToPosition(messageUtils.getmChatMessages().size() - 1);
            //getView().getRvMsg().smoothMoveToPosition(messageUtils.getmChatMessages().size() - 1);
            firstCreatMoveToBottom();
        }

    }

    public void sendTextMsg() {
        sendTextMsg(getView().getEtContent().getText().toString());
        getView().getEtContent().setText("");
    }

    private void sendTextMsg(String content) {
        messageUtils.sendText(content);
    }

    public void sendImgMsg(File imageFileThumb, File imageFileSource) {
        Uri imageFileThumbUri = Uri.fromFile(imageFileThumb);
        Uri imageFileSourceUri = Uri.fromFile(imageFileSource);
        // sendImgMsg(imageFileThumbUri, imageFileSourceUri);
        ImageMessage imgMsg = ImageMessage.obtain(imageFileThumbUri, imageFileSourceUri);
        messageUtils.sendImage(imageFileSource);
        rvMoveToBottom();
    }

    public void sendImgMsg(File imageFileThumb) {
        ImageMessage imgMsg = ImageMessage.obtain();
        Uri imageFileThumbUri = Uri.fromFile(imageFileThumb);
        imgMsg.setRemoteUri(imageFileThumbUri);
        messageUtils.sendImage(imageFileThumb);
    }


    public void sendVoice(Uri audioPath, int timeLen) {
        //发送录音
        messageUtils.sendVoice(audioPath, timeLen);
    }

    public void sendCard(String ObjectId) {
        //发送录音
        messageUtils.sendCard(ObjectId);
    }


    public void sendFileMsg(File file) {
        messageUtils.sendFile(file);
        rvMoveToBottom();
        //发送成功
    }

    public void sendLocationMessage(LocationData locationData) {
        LocationMessage locationMessage = LocationMessage.obtain(locationData.getLat(), locationData.getLng(), locationData.getPoi(), Uri.parse(locationData.getImgUrl()));
        //保存数据库成功
        messageUtils.sendLocate(locationMessage, locationData.getLat(), locationData.getLng(), locationData.getPoi()
                ,locationData.getImgUrl());
        rvMoveToBottom();

    }

    public void sendAudioFile(File file) {
        messageUtils.sendVideo(file);
        rvMoveToBottom();
        //发送成功
    }


    public void sendGif(String file) {
        messageUtils.sendGif(file);
        rvMoveToBottom();
        //发送成功
    }

    public void sendGifFile(File file){
        Message fileMessage = Message.obtain(mSessionId, mConversationType, FileMessage.obtain(Uri.fromFile(file)));
        L_.e("mSessionId  --->" + mSessionId + "----->" + FileMessage.obtain(Uri.fromFile(file)));
        mSessionId = "1";
        FileMessage obtain = FileMessage.obtain(Uri.fromFile(file));
        sendTextMsg(obtain.getName());
    }




}

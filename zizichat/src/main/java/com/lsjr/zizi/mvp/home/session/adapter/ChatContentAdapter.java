package com.lsjr.zizi.mvp.home.session.adapter;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.lqr.adapter.LQRAdapterForRecyclerView;
import com.lqr.adapter.LQRViewHolder;
import com.lqr.adapter.LQRViewHolderForRecyclerView;
import com.lqr.adapter.OnItemLongClickListener;
import com.lqr.audio.AudioPlayManager;
import com.lqr.audio.IAudioPlayListener;
import com.lqr.emoji.MoonUtils;
import com.lsjr.callback.DownloadSubscriber;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.mvp.dialog.SelectMessageWindow;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.chat.dao.ChatMessageDao;
import com.lsjr.zizi.chat.db.ChatMessage;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.chat.utils.DisplayUtil;
import com.lsjr.zizi.chat.utils.HtmlUtils;
import com.lsjr.zizi.chat.utils.SmileyParser;
import com.lsjr.zizi.chat.utils.StringUtils;
import com.lsjr.zizi.chat.xmpp.XmppMessage;
import com.lsjr.zizi.chat.xmpp.listener.ChatMessageListener;
import com.lsjr.zizi.loader.ImageLoader;
import com.lsjr.zizi.mvp.home.photo.CircleActivity;
import com.lsjr.zizi.mvp.home.photo.FileReceiverActivity;
import com.lsjr.zizi.mvp.home.photo.SingleImagePreviewActivity;
import com.lsjr.zizi.mvp.home.session.BasicInfoActivity;
import com.lsjr.zizi.mvp.home.session.presenter.ChatAtPresenter;
import com.lsjr.zizi.mvp.home.session.presenter.MessageUtils;
import com.lsjr.zizi.mvp.home.zichat.GeoCoderActivity;
import com.lsjr.zizi.util.FileOpenUtils;
import com.lsjr.zizi.util.TimeUtils;
import com.lsjr.zizi.view.BubbleImageView;
import com.lsjr.zizi.view.CircularProgressBar;
import com.ymz.baselibrary.AppCache;
import com.ymz.baselibrary.BaseApplication;
import com.ymz.baselibrary.utils.CacheUtils;
import com.ymz.baselibrary.utils.FileUtils;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

import java.io.File;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.rong.imlib.model.MessageContent;
import io.rong.message.ImageMessage;
import io.rong.message.LocationMessage;
import io.rong.message.VoiceMessage;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ChatContentAdapter extends LQRAdapterForRecyclerView<ChatMessage> {

    /*根据mLoginUserId和mToUserId 唯一确定一张表 */
    private String mLoginUserId;
    private String mToUserId;
    private boolean isSend;
    private Context mContext;
    private List<ChatMessage> mData;
    private static final int SEND_TEXT = R.layout.item_text_send;
    private static final int RECEIVE_TEXT = R.layout.item_text_receive;

    private static final int SEND_IMAGE = R.layout.item_image_send;
    private static final int RECEIVE_IMAGE = R.layout.item_image_receive;


    private static final int SEND_STICKER = R.layout.item_sticker_send;
    private static final int RECEIVE_STICKER = R.layout.item_sticker_receive;


    private static final int SEND_VIDEO = R.layout.item_video_send;
    private static final int RECEIVE_VIDEO = R.layout.item_video_receive;


    private static final int SEND_LOCATION = R.layout.item_location_send;
    private static final int RECEIVE_LOCATION = R.layout.item_location_receive;


    private static final int RECEIVE_NOTIFICATION = R.layout.item_notification;

    private static final int RECEIVE_VOICE = R.layout.item_audio_receive;
    private static final int SEND_VOICE = R.layout.item_audio_send;

    private static final int RECEIVE_RED_PACKET = R.layout.item_red_packet_receive;
    private static final int SEND_RED_PACKET = R.layout.item_red_packet_send;

    private static final int RECALL_NOTIFICATION = R.layout.item_notification;
    private static final int VIEW_SYSTEM = R.layout.item_notification;

    private static final int UNDEFINE_MSG = R.layout.item_no_support_msg_type;
    /*自定义类型*/
    private static final int SEND_GIF = R.layout.item_send_gif;
    private static final int RECEIVE_GIF = R.layout.item_receive_gif;

    private static final int SEND_CARD = R.layout.itme_send_card;
    private static final int RECEIVE_CARD = R.layout.itme_receive_card;


    private static final int SEND_FILE = R.layout.itme_send_file;
    private static final int RECEIVE_FILE = R.layout.itme_receive_file;

    // 匹配图标时用到的文件类型
    String[] fileTypes = new String[]{"apk", "avi", "bat", "bin", "bmp", "chm", "css", "dat", "dll", "doc", "docx",
            "dos", "dvd", "gif", "html", "ifo", "inf", "iso", "java", "jpeg", "jpg", "log", "m4a", "mid", "mov",
            "movie", "mp2", "mp2v", "mp3", "mp4", "mpe", "mpeg", "mpg", "pdf", "php", "png", "ppt", "pptx", "psd",
            "rar", "tif", "ttf", "txt", "wav", "wma", "wmv", "xls", "xlsx", "xml", "xsl", "zip"};
    String[] fileImage = new String[]{"jpg", "jpeg", "png"};
    String[] fileAudio = new String[]{"wam", "mp4"};
    private MessageUtils messageUtils;
    public ChatContentAdapter(Context context, List<ChatMessage> data, ChatAtPresenter presenter, String toUserId,MessageUtils utils) {
        super(context, data);
        if (data == null) {
            return;
        }
        messageUtils=utils;
        mLoginUserId = messageUtils.mLoginUserId;
        mContext = context;
        mData = data;
        mToUserId = toUserId;

    }

    @Override
    public void convert(LQRViewHolderForRecyclerView helper, ChatMessage item, int position) {
        setTime(helper, item, position);
        setName(helper, item);
        setView(helper, item, position);
        //setReadStaus(helper,item);
        //根据发送还是接受状态改变显示
        setStatus(helper, item, position);
        setReadStaus(helper,item);
        setLongClickInterface(helper,item);
        setItmeOnclick(helper,item,position);
    }


    private void setTime(LQRViewHolderForRecyclerView helper, ChatMessage message, int position) {
          /* 设置时间 */
          /* 是否显示日期 */
        boolean showTime = true;
        if (position >= 1) {
            ChatMessage prevMessage = mData.get(position - 1);
            int prevTime = prevMessage.getTimeSend();
            int nowTime = message.getTimeSend();
            if (nowTime - prevTime < 15 * 60) {// 小于15分钟，不显示
                showTime = false;
            }
        }
        if (showTime) {
            helper.setViewVisibility(R.id.tvTime, View.VISIBLE).setText(R.id.tvTime, TimeUtils.sk_time_long_to_chat_time_str(message.getTimeSend()));
        } else {
            helper.setViewVisibility(R.id.tvTime, GONE);
        }

    }

    private int parserType(ChatMessage message) {
        String filePath = message.getFilePath();
        if (filePath != null) {
            int pointIndex = filePath.lastIndexOf(".");
            if (pointIndex != -1) {
                String type = filePath.substring(pointIndex + 1).toLowerCase();
                for (String aFileImage : fileImage) {
                    if (type.equals(aFileImage)) {
                        return XmppMessage.TYPE_IMAGE;
                    }
                }
            }
        }
        return 0;
    }


    private int parserFileType(ChatMessage message) {
        String filePath = message.getFilePath();
        if (filePath != null) {
            int pointIndex = filePath.lastIndexOf(".");
            if (pointIndex != -1) {
                String type = filePath.substring(pointIndex + 1).toLowerCase();
                for (String aFileImage : fileImage) {
                    if (type.equals(aFileImage)) {
                        return XmppMessage.TYPE_IMAGE;
                    }
                }
//                for (String aFileAudio : fileAudio) {
//                    if (type.equals(aFileAudio)) {
//                        return XmppMessage.TYPE_VIDEO;
//                    }
//                }
            }
        }
        return 0;
    }

    private void setName(LQRViewHolderForRecyclerView helper, ChatMessage message) {
        int messageType = message.getType();
        if (messageType == XmppMessage.TYPE_TEXT || messageType == XmppMessage.TYPE_IMAGE || messageType == XmppMessage.TYPE_VIDEO
                || messageType == XmppMessage.TYPE_VOICE || messageType == XmppMessage.TYPE_LOCATION || messageType == XmppMessage.TYPE_GIF
                || messageType == XmppMessage.TYPE_CARD|| messageType == XmppMessage.TYPE_FILE) {

            if(messageUtils.isGroupChat()){
                helper.setViewVisibility(R.id.tvName, View.VISIBLE).setText(R.id.tvName, message.getFromUserName());
            }else {
                helper.setViewVisibility(R.id.tvName, View.GONE).setText(R.id.tvName, message.getFromUserName());
            }

            CircleImageView chat_head_iv= helper.getView(R.id.ivAvatar);
            final String fromUserId = message.getFromUserId();
            if (message.isMySend()){
                AvatarHelper.getInstance().displayAvatar(ConfigApplication.instance().mLoginUser, chat_head_iv, true);
            }else {
                Friend friend=new Friend();
                friend.setUserId(fromUserId);
                friend.setNickName(message.getFromUserName());
                AvatarHelper.getInstance().displayAvatar(friend, chat_head_iv, true);
            }

        }
    }

    private void setReadStaus(LQRViewHolderForRecyclerView helper, ChatMessage message){
        int messageType = message.getType();
        ImageView unView=helper.getView(R.id.unread_img_view);
        switch (messageType) {
            case XmppMessage.TYPE_FILE:
            case XmppMessage.TYPE_VIDEO:
            case XmppMessage.TYPE_CARD:
            case XmppMessage.TYPE_VOICE:
            case XmppMessage.TYPE_LOCATION:
                if (!message.isMySend()) {
                    if (!message.isRead()) {
                        if (unView!=null)
                        helper.setViewVisibility(R.id.unread_img_view,View.VISIBLE);
                    } else {
                        if (unView!=null)
                        helper.setViewVisibility(R.id.unread_img_view,View.GONE);
                    }
                }
            break;
        }
    }

    public interface UserOnClickListener{
        void onClick(String  userId);
    }

    UserOnClickListener userOnClickListener;

    public void setUserOnClickListener(UserOnClickListener userOnClickListener) {
        this.userOnClickListener = userOnClickListener;
    }


    public class ClickListener implements View.OnClickListener {
        private ChatMessage message;
        private int position;

        public ClickListener(ChatMessage message, int position) {
            this.message = message;
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            menuWindow.dismiss();
            switch (v.getId()) {
                case R.id.btn_copy:// copy文字
                    String s = StringUtils.replaceSpecialChar(message.getContent());
                    CharSequence charSequence = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);
                    // 获得剪切板管理者,复制文本内容
                    ClipboardManager cmb = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    cmb.setText(charSequence);

                    break;
                case R.id.btn_instant:// 转发消息
//                    Intent intent = new Intent(mContext, InstantMessageActivity.class);
//                    Bundle bundle = new Bundle();
//                    bundle.putParcelable(Constants.INSTANT_MESSAGE, message);
//                    intent.putExtras(bundle);
//                    mContext.startActivity(intent);
//                    ((Activity) mContext).finish();
                    break;
                case R.id.btn_delete:// 删除

              /* if(mChatMessages!=null){
            	   mChatMessages.remove(position);
               }*/
                   if(ChatMessageDao.getInstance().deleteChatOneMsg(mLoginUserId,mToUserId,message)){
                       T_.showToastReal("删除消息成功");
                       Intent broadcast = new Intent(Constants.CHAT_MESSAGE_DELETE_ACTION);//发送广播去界面更新
                       broadcast.putExtra(Constants.CHAT_REMOVE_MESSAGE_POSITION, position);
                       mContext.sendBroadcast(broadcast);
                   }else {
                       T_.showToastReal("删除消息失败");
                   }
                    break;
                case R.id.btn_cancle:// 取消

                    break;
                default:
                    break;
            }
        }
    }

    SelectMessageWindow menuWindow;

    private void setLongClickInterface(LQRViewHolderForRecyclerView helper, ChatMessage message) {
        helper.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(LQRViewHolder helper, ViewGroup parent, View itemView, int position) {
                Vibrator vib = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
                vib.vibrate(40);// 只震动一秒，一次
                // Toast.makeText(mContext, "长按了", 0).show();
                // Log.d("wang", "长按了文本内容");
                // 实例化SelectMessageWindow
                  menuWindow = new SelectMessageWindow(mContext, new ClickListener(message, position),
                        message.getType());
                // 显示窗口
                menuWindow.showAtLocation(itemView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                return false;
            }
        });

    }


    private void setItmeOnclick(LQRViewHolderForRecyclerView helper, ChatMessage message, int position){
        int messageType = message.getType();
      //  MessageContent messageContent = message.getMessageContent();
        CircleImageView chat_head_iv= helper.getView(R.id.ivAvatar);
        if (chat_head_iv!=null){
            chat_head_iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String fromUserId;
                    if (message.isMySend()){
                        fromUserId = ConfigApplication.instance().mLoginUser.getUserId();
                    }else {
                        fromUserId = message.getFromUserId();
                    }
                    if (userOnClickListener!=null){
                        userOnClickListener.onClick(fromUserId);
                    }
                }
            });
        }


        switch (messageType){
            case XmppMessage.TYPE_CARD:
                helper.getView(R.id.chat_warp_view).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        upMessageStausToRead(helper,message);
                        Intent intent = new Intent(mContext, BasicInfoActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Constants.EXTRA_USER_ID, message.getFromUserId());
                        mContext.startActivity(intent);
                    }
                });

                break;
            case XmppMessage.TYPE_FILE:
                    helper.getView(R.id.chat_file_pic).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            upMessageStausToRead(helper,message);
                            Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(AppConfig.EXTRA_IMAGE_URI, message.getContent());
                            mContext.startActivity(intent);
                        }
                    });

                    helper.getView(R.id.chat_file_view).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            upMessageStausToRead(helper,message);
                            L_.e("先查找本地---->"+message.getFilePath());
                            //先查找本地   必须保证在我们自己的目录下打开才能找到  如果自己发出相当于重新下载一次
                            if (message.getFilePath()!=null&&new File(message.getFilePath()).exists()) {
                                Intent intent = new Intent(mContext, FileReceiverActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putParcelable(Constants.INSTANT_MESSAGE, message);
                                intent.putExtras(bundle);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                mContext.startActivity(intent);
                            }
                        }
                    });

                break;
            case XmppMessage.TYPE_IMAGE:
                helper.getView(R.id.bivPic).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        upMessageStausToRead(helper,message);
                        Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(AppConfig.EXTRA_IMAGE_URI, message.getContent());
                        mContext.startActivity(intent);
                    }
                });
                break;
            case XmppMessage.TYPE_VIDEO:
                helper.getView(R.id.bivPic).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        upMessageStausToRead(helper,message);
                        String filePath = message.getFilePath();
                        if (new File(filePath).exists()) {
                            L_.e("播放视频:"+filePath);
                            FileOpenUtils.openFile(UIUtils.getContext(), filePath);
                        }else {
                            T_.showToastReal("路径不存在");
                        }
                    }
                });
                break;
            case XmppMessage.TYPE_VOICE:
                helper.getView(R.id.rlAudio).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        upMessageStausToRead(helper,message);
                        //T_.showToastReal("点击录音");
                        ImageView ivAudio = helper.getView(R.id.ivAudio);
                        if (message.getFilePath()!=null&&new File(message.getFilePath()).exists()){
                            VoiceMessage voiceMessage = VoiceMessage.obtain(Uri.parse(message.getFilePath()), message.getTimeLen());
                            playVoice(voiceMessage, ivAudio);
                        }
                    }
                });
                break;
            case XmppMessage.TYPE_LOCATION:
                helper.getView(R.id.id_location_root).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        upMessageStausToRead(helper,message);
                        Intent intent = new Intent(mContext, GeoCoderActivity.class);
                        intent.putExtra(Constants.EXTRA_LATITUDE,message.getLocation_x());
                        intent.putExtra(Constants.EXTRA_LONGITUDE,message.getLocation_y());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    }
                });
                break;
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    private void setView(LQRViewHolderForRecyclerView helper, ChatMessage message, int position) {
        L_.e(message.toString());
        //根据消息类型设置消息显示内容
        int messageType = message.getType();
        String content = message.getContent();
        L_.e("收到消息内容---------------->"+message.toString());
        MessageContent messageContent = message.getMessageContent();
        if (messageType == 0) return;
        switch (messageType) {
            case XmppMessage.TYPE_FILE:
                RelativeLayout relativeLayout = helper.getView(R.id.chat_file_view);
                BubbleImageView chat_file_pic = helper.getView(R.id.chat_file_pic);
                ImageView chat_warp_file = helper.getView(R.id.chat_to_file);
                TextView chat_file_name = helper.getView(R.id.file_name);
                ProgressBar progress = helper.getView(R.id.progress);
                progress.setVisibility(View.GONE);
                //L_.e("roamer", "....TYPE_FILE要显示了....filePath" + message.getContent()+message.getFilePath());
                switch (parserFileType(message)) {
                    case XmppMessage.TYPE_IMAGE:
                        relativeLayout.setVisibility(GONE);
                        chat_file_pic.setVisibility(VISIBLE);
                        L_.e("显示文件图片");
                        ImageLoader.getInstance().showImage(message.getContent(),chat_file_pic,R.drawable.defaultpic,UIUtils.dip2px(80),UIUtils.dip2px(150));
                        isSend = mData.get(position).getFromUserId().compareToIgnoreCase(mLoginUserId) == 0;
                        if (isSend) {
                            switch (message.getMessageState()) {
                                case ChatMessageListener.MESSAGE_SEND_ING:
                                    chat_file_pic.setProgressVisible(true);
                                    chat_file_pic.showShadow(true);
                                    helper.setViewVisibility(R.id.llError, GONE);
                                    break;
                                case ChatMessageListener.MESSAGE_SEND_SUCCESS:
                                    chat_file_pic.setProgressVisible(false);
                                    chat_file_pic.showShadow(false);
                                    helper.setViewVisibility(R.id.llError, GONE);
                                    break;
                                case ChatMessageListener.MESSAGE_SEND_FAILED:
                                    chat_file_pic.setProgressVisible(false);
                                    chat_file_pic.showShadow(false);
                                    helper.setViewVisibility(R.id.llError, View.VISIBLE);
                                    break;
                            }
                        } else {
                            message.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                            chat_file_pic.setProgressVisible(false);
                            chat_file_pic.showShadow(false);
                            helper.setViewVisibility(R.id.llError, GONE);
                        }
                        break;
//                    case XmppMessage.TYPE_VIDEO:
//                        break;
                    case 0:
                        chat_file_pic.setVisibility(GONE);
                        relativeLayout.setVisibility(VISIBLE);
                        //L_.e("roamer", "....TYPE_FILE要显示了....filePath" +message.getFilePath());
                       if (message.getFilePath()==null)return;
                        int pointIndex = message.getFilePath().lastIndexOf(".");
                        if (pointIndex != -1) {
                            String type = message.getFilePath().substring(pointIndex + 1).toLowerCase();
                            for (String fileType : fileTypes) {
                                if (type.equals(fileType)) {
                                    int resId = mContext.getResources().getIdentifier(type, "drawable", mContext.getPackageName());
                                    if (resId!=0){
                                        chat_warp_file.setImageResource(resId);
                                    }else {
                                        chat_warp_file.setImageResource(R.drawable.defaultpic);
                                    }
                                }
                            }
                        }
                        String name = FileUtils.getFileName(message.getFilePath());
                        chat_file_name.setText(name);
                        //先查找本地   必须保证在我们自己的目录下打开才能找到  如果自己发出相当于重新下载一次
                        boolean downLoad = true;
                        if (AppCache.getInstance().fileExists(AppCache.getInstance().mFilesDir, name)) {
                            downLoad = false;
                        }
                        if (downLoad) {
                            L_.e("下载文件" + name);
                            HttpUtils.getInstance().downloadFile(message.getContent(), new DownloadSubscriber(UIUtils.getContext(),
                                    AppCache.getInstance().mFilesDir, name) {
                                @Override
                                public void onComplete(String path) {
                                    L_.e("文件保存路径：" + path);
                                    message.setFilePath(path);
                                    ChatMessageDao.getInstance().updateMessageDownloadState(mLoginUserId, mToUserId, message.get_id(), true, path);
                                }

                                @Override
                                public void update(long bytesRead, long contentLength, boolean done) {
                                    int progress = (int) (bytesRead * 100 / contentLength);
                                    L_.e(progress + "% ");
                                }

                                @Override
                                protected void onXError(String exception) {
                                    L_.e(exception + "% ");
                                }
                            });
                        }

                        break;
                }
                break;
            case XmppMessage.TYPE_TIP:
                /* 设置数据 */
                helper.setText(R.id.tvTime, TimeUtils.sk_time_s_long_2_str(message.getTimeSend())).setViewVisibility(R.id.tvNotification, VISIBLE);
                if (content.contains("面试系统")){
                    helper.setText(R.id.tvNotification, "欢迎使用孜孜管家聊天系统").setViewVisibility(R.id.tvNotification, VISIBLE);
                }else {
                    helper.setText(R.id.tvNotification, content).setViewVisibility(R.id.tvNotification, VISIBLE);
                }

                break;
            case XmppMessage.TYPE_TEXT:
                String s = StringUtils.replaceSpecialChar(content);
                CharSequence charSequence = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);
                MoonUtils.identifyFaceExpression(mContext, helper.getView(R.id.tvText), charSequence.toString(), ImageSpan.ALIGN_BOTTOM);
                break;
            case XmppMessage.TYPE_IMAGE:
                BubbleImageView bivPic = helper.getView(R.id.bivPic);
                ImageMessage imageMessage = (ImageMessage) messageContent;


//                if (message.getThumbnailUrl()!=null&&new File(message.getThumbnailUrl()).exists()){
//                    ImageLoader.getInstance().show(message.getThumbnailUrl(),bivPic);
//                }else {
//
//                }

                //&&new File(message.getFilePath()).exists()
                L_.e("----------->显示图片大小"+message.getFilePath());
                if (message.getFilePath()!=null){
                    L_.e("显示压缩图");
                    ImageLoader.getInstance().show(message.getFilePath(),bivPic);
                   // ImageLoader.getInstance().showMultiImageView1(message.getThumbnailUrl(),bivPic,R.drawable.defaultpic);
                    //Bitmap bitmap = BitmapFactory.decodeFile();
                   // L_.e("显示图片大小-------------<<"+new File(message.getFilePath()).length()/1024+"K");
                    //ImageLoader.getInstance().showImage(message.getFilePath(),bivPic,R.mipmap.default_img,UIUtils.dip2px(80),UIUtils.dip2px(150));
                }else {
                    ImageLoader.getInstance().show(message.getContent(),bivPic);
                    //ImageLoader.getInstance().showImage(message.getContent(),bivPic,R.mipmap.default_img,UIUtils.dip2px(80),UIUtils.dip2px(150));
                }

                break;
            case XmppMessage.TYPE_VIDEO:
                //FileMessage fileMessage = (FileMessage) messageContent;
                CircularProgressBar circularProgressBar = helper.getView(R.id.cpbLoading);
                ImageView igPaly = helper.getView(R.id.ig_play);
                bivPic = helper.getView(R.id.bivPic);
                if (message.getFilePath()!=null&&new File(message.getFilePath()).exists()) {
                    ChatMessageDao.getInstance().updateMessageDownloadState(mLoginUserId, mToUserId, message.get_id(), true, message.getFilePath());
                    Bitmap bitmap = getCacheVideoImage(message.getFilePath());
                    bivPic.setImageBitmap(bitmap);
                } else {
                    boolean downLoad = true;
                   // L_.e("下载视频："+message.getFilePath()+":"+FileUtils.getFileName(content));
                    if (!TextUtils.isEmpty(content)&&AppCache.getInstance().videoExists(AppCache.getInstance().mVideosDir, FileUtils.getFileName(content)))
                     {
                        downLoad = false;
                        // L_.e("文件已存在");
                         message.setFilePath(AppCache.getInstance().videoPath(FileUtils.getFileName(content)));

                    }
                    if (downLoad) {
                        igPaly.setVisibility(GONE);
                        circularProgressBar.setProgress(0);
                        circularProgressBar.setMax(100);
                        circularProgressBar.setVisibility(VISIBLE);
                        HttpUtils.getInstance().downloadFile(content, new DownloadSubscriber(BaseApplication.getApplication(),
                                AppCache.getInstance().mVideosDir, FileUtils.getFileName(content)) {
                            @Override
                            public void onComplete(String path) {
                               // L_.e("文件下载成功保存路径：" + path);
                                circularProgressBar.setVisibility(GONE);
                                igPaly.setVisibility(VISIBLE);
                                message.setFilePath(path);
                                ChatMessageDao.getInstance().updateMessageDownloadState(mLoginUserId, mToUserId, message.get_id(), true, path);
                                Bitmap bitmap = getCacheVideoImage(path);
                                bivPic.setImageBitmap(bitmap);
                            }

                            @Override
                            public void update(long bytesRead, long contentLength, boolean done) {
                                int progress = (int) (bytesRead * 100 / contentLength);
                                L_.e(progress + "% ");
                                circularProgressBar.setProgress(progress);
                            }

                            @Override
                            protected void onXError(String exception) {
                                L_.e(exception + "% ");
                                bivPic.setImageResource(R.drawable.defaultpic);
                            }
                        });

                    } else {
                        Bitmap bitmap = getCacheVideoImage(AppCache.getInstance().videoPath(FileUtils.getFileName(content)));
                        bivPic.setImageBitmap(bitmap);
                    }

                }
                break;
            case XmppMessage.TYPE_VOICE:
                //VoiceMessage voiceMessage = (VoiceMessage) messageContent;
                int voiceTime = message.getTimeLen();
                int increment = voiceTime*2;
                //int increment = ( voiceTime / AppConst.DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND ) * AppConst.DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND;
                RelativeLayout  rlAudio = helper.setText(R.id.tvDuration, voiceTime+ "s" + "''").
                        setTextColor(R.id.tvDuration,R.color.gray3).getView(R.id.rlAudio);
                ViewGroup.LayoutParams params = rlAudio.getLayoutParams();
                L_.e("increment-->"+increment);
                params.width =  UIUtils.dip2px(65)+UIUtils.dip2px(increment);
                rlAudio.setLayoutParams(params);
                boolean downLoad = true;
                // String filePath = message.getFilePath();
                if (message.getFilePath()!=null&&new File(message.getFilePath()).exists()) {
                    downLoad = false;
                }
                if (downLoad) {
                    L_.e("下载录音" + message.getContent());
                    HttpUtils.getInstance().downloadFile(message.getContent(), new DownloadSubscriber(UIUtils.getContext(),
                            AppCache.getInstance().mVoicesDir, FileUtils.getFileName(message.getContent())) {
                        @Override
                        public void onComplete(String path) {
                            L_.e("文件保存路径：" + path);
                            message.setFilePath(path);
                            ChatMessageDao.getInstance().updateMessageDownloadState(mLoginUserId, mToUserId, message.get_id(), true, path);
                        }

                        @Override
                        public void update(long bytesRead, long contentLength, boolean done) {
                            int progress = (int) (bytesRead * 100 / contentLength);
                            L_.e(progress + "% ");
                        }

                        @Override
                        protected void onXError(String exception) {
                            L_.e(exception + "% ");
                        }
                    });
                }
                break;
            case XmppMessage.TYPE_LOCATION:
                LocationMessage locationMessage = (LocationMessage) messageContent;
                ImageView ivLocation = helper.getView(R.id.ivLocation);
                if (locationMessage != null) {
                   // Glide.with(mContext).load(locationMessage.getImgUri()).placeholder(R.mipmap.default_location).centerCrop().into(ivLocation);
                    ImageLoader.getInstance().showImage(locationMessage.getImgUri(),ivLocation,R.mipmap.default_location,R.mipmap.default_location);
                }else {
                    ImageLoader.getInstance().showImage(message.getFilePath(),ivLocation,R.mipmap.default_location,R.mipmap.default_location);
                }
                helper.setText(R.id.tvTitle, content);
                break;
            case XmppMessage.TYPE_GIF:
                //holder.chat_gif_view = (GifImageView) convertView.findViewById(R.id.chat_to_gif_view);
                String gifName = message.getContent();
                int resId = SmileyParser.Gifs.textMapId(gifName);
                L_.e("显示GIF---------->"+parserType(message)+message.getFilePath()+"---->gifName"+gifName);
                int margin = DisplayUtil.dip2px(mContext, 80);
                //GifView gifView = helper.getView(R.id.chat_to_gif_view);
                ImageView gifView = helper.getView(R.id.chat_to_gif_view);
                ViewGroup.LayoutParams layoutParams = gifView.getLayoutParams();
                layoutParams.width = margin;
                layoutParams.height = margin;
                if (resId != -1) {
                    Glide.with(UIUtils.getContext()).load(message.getFilePath()).asGif().diskCacheStrategy(DiskCacheStrategy.SOURCE).into(gifView);
                    return;
                }
                if (parserType(message)==2){
                    Glide.with(UIUtils.getContext()).load(message.getFilePath()).fitCenter().skipMemoryCache(false).
                            diskCacheStrategy(DiskCacheStrategy.SOURCE).into(gifView);
                }else {
                    // gifView.setMovieResource(resId);
                    Glide.with(UIUtils.getContext()).load(message.getFilePath()).asGif().diskCacheStrategy(DiskCacheStrategy.SOURCE).into(gifView);
                }

                break;
            case XmppMessage.TYPE_CARD:
                if (!TextUtils.isEmpty(message.getContent())) {
                    if (!message.isMySend()){
                        helper.getView(R.id.card_progress).setVisibility(View.GONE);
                    }
                    helper.setText(R.id.person_name, "昵称:" + message.getFromUserName());
                    if (content.equals("0")) {
                        helper.setText(R.id.person_sex, "性别:女");
                    } else {
                        helper.setText(R.id.person_sex, "性别:男");
                    }

                }
                break;
        }

    }

    private Bitmap getCacheVideoImage(String filePath){
        Bitmap bitmap = CacheUtils.getInstance().getBitmap(filePath);
        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.MINI_KIND);
            if (bitmap != null) {
                CacheUtils.getInstance().put(filePath, bitmap);
            }
        }
        return bitmap;
    }

    private void setStatus(LQRViewHolderForRecyclerView helper, ChatMessage message, int position) {
        // 如果消息是从我发出的，就会有个发送的状态
        //根据消息类型设置消息显示内容
        int messageType = message.getType();
        // 我的消息
        switch (messageType) {
            case XmppMessage.TYPE_TEXT:
            case XmppMessage.TYPE_VOICE:
            case XmppMessage.TYPE_LOCATION:
                //只需要设置自己发送的状态
                isSend = mData.get(position).getFromUserId().compareToIgnoreCase(mLoginUserId) == 0;
                if (isSend) {
                    switch (message.getMessageState()) {
                        case ChatMessageListener.MESSAGE_SEND_ING:
                            helper.setViewVisibility(R.id.pbSending, View.VISIBLE).setViewVisibility(R.id.llError, GONE);
                            break;
                        case ChatMessageListener.MESSAGE_SEND_SUCCESS:
                            helper.setViewVisibility(R.id.pbSending, GONE).setViewVisibility(R.id.llError, GONE);
                            break;
                        case ChatMessageListener.MESSAGE_SEND_FAILED:
                            helper.setViewVisibility(R.id.pbSending, GONE).setViewVisibility(R.id.llError, View.VISIBLE);
                            break;
                    }
                } else {
                    helper.setViewVisibility(R.id.pbSending, GONE).setViewVisibility(R.id.llError, GONE);
                }

                break;
            case XmppMessage.TYPE_IMAGE:
                BubbleImageView bivPic = helper.getView(R.id.bivPic);
                isSend = mData.get(position).getFromUserId().compareToIgnoreCase(mLoginUserId) == 0;
                if (isSend) {
                    switch (message.getMessageState()) {
                        case ChatMessageListener.MESSAGE_SEND_ING:
                            bivPic.setProgressVisible(true);
                            bivPic.showShadow(true);
                            helper.setViewVisibility(R.id.llError, GONE);
                            break;
                        case ChatMessageListener.MESSAGE_SEND_SUCCESS:
                            bivPic.setProgressVisible(false);
                            bivPic.showShadow(false);
                            helper.setViewVisibility(R.id.llError, GONE);
                            break;
                        case ChatMessageListener.MESSAGE_SEND_FAILED:
                            bivPic.setProgressVisible(false);
                            bivPic.showShadow(false);
                            helper.setViewVisibility(R.id.llError, View.VISIBLE);
                            break;
                    }
                } else {
                    message.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                    bivPic.setProgressVisible(false);
                    bivPic.showShadow(false);
                    helper.setViewVisibility(R.id.llError, GONE);
                }
                break;
            case XmppMessage.TYPE_VIDEO:
                break;
        }

        if (helper.getView(R.id.llError)==null)return;
        helper.getView(R.id.llError).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (message.getMessageState() == ChatMessageListener.MESSAGE_SEND_FAILED){
                    T_.showToastReal("重试");
                    messageUtils.onSendAgain(message);
                }

            }
        });

    }


    private void upMessageStausToRead(LQRViewHolder helper, ChatMessage message) {
        //改变阅读状态
        ImageView readStausView = helper.getView(R.id.unread_img_view);
        //L_.e("将消息标记为已读：" + message.isRead());
        if (!message.isMySend() && !message.isRead()) {
            message.setRead(true);
            //L_.e("将消息标记为已读：" + message.isRead());
            ChatMessageDao.getInstance().updateMessageReadState(mLoginUserId, mToUserId,
                    message.get_id());
            if (readStausView != null) {
                readStausView.setVisibility(View.GONE);
            }
        }
    }


    private void playVoice(VoiceMessage voiceMessage, ImageView ivAudio) {
        AudioPlayManager.getInstance().startPlay(UIUtils.getContext(), voiceMessage.getUri(), new IAudioPlayListener() {
            @Override
            public void onStart(Uri var1) {
                if (ivAudio != null && ivAudio.getBackground() instanceof AnimationDrawable) {
                    AnimationDrawable animation = (AnimationDrawable) ivAudio.getBackground();
                    animation.start();
                }
            }

            @Override
            public void onStop(Uri var1) {
                if (ivAudio != null && ivAudio.getBackground() instanceof AnimationDrawable) {
                    AnimationDrawable animation = (AnimationDrawable) ivAudio.getBackground();
                    animation.stop();
                    animation.selectDrawable(0);
                }

            }

            @Override
            public void onComplete(Uri var1) {
                if (ivAudio != null && ivAudio.getBackground() instanceof AnimationDrawable) {
                    AnimationDrawable animation = (AnimationDrawable) ivAudio.getBackground();
                    animation.stop();
                    animation.selectDrawable(0);
                }
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        int messageType = mData.get(position).getType();
        //L_.e("当前的数据类型L------" + messageType);
        if (messageType == XmppMessage.TYPE_TIP) {
            return VIEW_SYSTEM;// 消息提示
        }
        // 我的消息
        isSend = mData.get(position).getFromUserId().compareToIgnoreCase(mLoginUserId) == 0;
        switch (messageType) {
            case XmppMessage.TYPE_TEXT:
                return isSend ? SEND_TEXT : RECEIVE_TEXT;
            case XmppMessage.TYPE_IMAGE:
                return isSend ? SEND_IMAGE : RECEIVE_IMAGE;
            case XmppMessage.TYPE_VIDEO:
                return isSend ? SEND_VIDEO : RECEIVE_VIDEO;
            case XmppMessage.TYPE_VOICE:
                return isSend ? SEND_VOICE : RECEIVE_VOICE;
            case XmppMessage.TYPE_LOCATION:
                return isSend ? SEND_LOCATION : RECEIVE_LOCATION;
            case XmppMessage.TYPE_GIF:
                return isSend ? SEND_GIF : RECEIVE_GIF;
            case XmppMessage.TYPE_CARD:
                return isSend ? SEND_CARD : RECEIVE_CARD;
            case XmppMessage.TYPE_FILE:
                return isSend ? SEND_FILE : RECEIVE_FILE;
        }
        return UNDEFINE_MSG;
    }

}
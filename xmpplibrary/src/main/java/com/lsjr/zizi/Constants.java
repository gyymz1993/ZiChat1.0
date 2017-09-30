package com.lsjr.zizi;

public class Constants {
    public static String INSTANT_MESSAGE="instant_message";//转发消息的标记
    public static String INSTANT_MESSAGE_FILE="instant_message_file";//转发文件稍有不同
    public static String INSTANT_SEND="instant_send";//转发
    public static String CHAT_MESSAGE_DELETE_ACTION="chat_message_delete";
    public static String CHAT_REMOVE_MESSAGE_POSITION="CHAT_REMOVE_MESSAGE_POSITION";
    public static String ONRECORDSTART="onrecordstart";
    public static String GROUP_JOIN_NOTICE="group_join_notice";//加入新群的通知


    public static String GROUP_JOIN_NOTICE_ACTION="group_join_notice_action";//加入新群的通知
    public static String GROUP_JOIN_NOTICE_FRIEND_ID="group_join_notice_friend_id";//加入新群发送朋友的id

     public static String OFFLINE_TIME="offline_time";//离线时间
    public  static String LAST_OFFLINE_TIME="last_offline_time";




    /** 某些地方选择数据使用的常量 */
    public static final String EXTRA_ACTION = "action";// 进入这个类的执行的操作
    public static final int ACTION_NONE = 0;// 不执行操作
    public static final int ACTION_SELECT = 1;// 执行选择操作
    public static final String EXTRA_SELECT_IDS = "select_ids";// 选择对应项目的ids
    public static final String EXTRA_SELECT_ID = "select_id";// 选择对应项目的id
    public static final String EXTRA_SELECT_NAME = "select_name";// 选择的对应项目的名称


    /** 某些地方需要传递如ListView Position的数据 */
    public static final String EXTRA_POSITION = "position";
    public static final int INVALID_POSITION = -1;

    // ///////////////////////////////////////////////////////////////////////////
    // 用户信息参数，很多地方需要
    public static final String EXTRA_USER_ID = "userId";// userId
    public static final String EXTRA_NICK_NAME = "nickName";// nickName
    public static final String EXTRA_IS_GROUP_CHAT = "isGroupChat";// 是否是群聊

    // BusinessCircleActivity需要的
    public static final String EXTRA_CIRCLE_TYPE = "circle_type";// 看的商务圈类型
    public static final int CIRCLE_TYPE_MY_BUSINESS = 0;// 看的商务圈类型,是我的商务圈
    public static final int CIRCLE_TYPE_PERSONAL_SPACE = 1;// 看的商务圈类型，是个人空间

    // ////////////////

    /** 商务圈发布的常量 */
	/* 发说说(图文) */
    public static final String EXTRA_IMAGES = "images";// 预览的那组图片
    public static final String EXTRA_CHANGE_SELECTED = "change_selected";// 是否可以改变选择，这样在ActivityResult中会回传重新选择的结果

    public static final String EXTRA_MSG_ID = "msg_id";// 公共消息id
    public static final String EXTRA_FILE_PATH = "file_path";// 语音、视频文件路径
    public static final String FILE_PAT_NAME = "file_name";//文件的名字
    public static final String EXTRA_IMAGE_FILE_PATH = "image_file_path";// 图片文件路径
    public static final String EXTRA_TIME_LEN = "time_len";// 语音、视频文件时长
    //位置经纬度
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";

    /* IM */
    public static final String EXTRA_FRIEND = "friend";

    /* 进入SingleImagePreviewActivity需要带上的参数 */
    public static final String EXTRA_IMAGE_URI = "image_uri";

    public static final String ISADD_USER = "add_user";


    public static  final String FILE_PATH="file_paht";//转发消息的标记

}

package com.lsjr.zizi;


import com.ymz.baselibrary.utils.UIUtils;

public class AppConfig {

	//http://192.168.100.10:8092/config

	/*
	*
	* {"XMPPDomain":"192.168.100.10","XMPPHost":"192.168.100.10","androidAppUrl":""
	* ,"androidExplain":"","androidVersion":0,"apiUrl":"http://192.168.100.10:8092/","
	* audioLen":"20","displayRedPacket":1,"distance":20,"downloadAvatarUrl":"http://192.168.100.10/",
	* "downloadUrl":"http://192.168.100.10/","freeswitch":"","helpUrl":"","id":10000,"iosAppUrl":"",
	* "iosExplain":"","iosVersion":0,"liveUrl":"","meetingHost":"","shareUrl":"","softUrl":"",
	* "uploadUrl":"http://192.168.100.10:8081/upload/","videoLen":"20",
	* "xMPPDomain":"192.168.100.10",
	* "xMPPHost":"192.168.100.10"
	* */


	//把内网版的192.168.100.10换成  dev.zizi.com.cn
	/*李桥测试*/
	//private static final String CONFIG_IP = "http://192.168.100.10:8092/";
	//private static final String CONFIG_IP = "http://192.168.100.187:8092/";
	private static final String CONFIG_IP = "http://dev.zizi.com.cn:8092/";


	//public static final String  XMPPHost="192.168.100.10";
	//public static final String  XMPPHost="192.168.100.187";
	public static final String  XMPPHost="dev.zizi.com.cn";

	/* 头像下载地址--》衍生地址 */
	//private static final String IP = "http://192.168.100.10/";
	//private static final String IP = "http://192.168.100.187/";
	private static final String IP = "http://dev.zizi.com.cn/";

	//private static final String CONFIG_UPLOAD = "http://192.168.100.10:8081/";
	//private static final String CONFIG_UPLOAD = "http://192.168.187.10:8081/";
	private static final String CONFIG_UPLOAD = "http://dev.zizi.com.cn/";

	/*李桥测试*/
	public static final String CONFIG_URL = CONFIG_IP+"config";
	public static String USER_LOGIN=CONFIG_IP + "user/login";// 登陆
	public static final String DOWNLOAD_CIRCLE_MESSAGE = CONFIG_IP + "b/circle/msg/ids";// 下载商务圈消息
	public static final String FRIENDS_ATTENTION_LIST=CONFIG_IP + "friends/attention/list";// 获取关注列表

	public static final String USER_GET_URL=CONFIG_IP + "user/get";// 获取用户资料，更新本地数据的接口;

	public static final  String USER_PHOTO_LIST=CONFIG_IP + "user/photo/list";// 获取相册列表;// 获取相册列表



	public static final  String ROOM_LIST_HIS= CONFIG_IP + "/room/list/his";// 获取某个用户已加入的房间列表;

	public static final String USER_LOGIN_AUTO=CONFIG_IP + "user/login/auto";// 检测Token是否过期 0未更换 1更换过


	/* 商务圈 */
	public static final String USER_CIRCLE_MESSAGE=CONFIG_IP + "b/circle/msg/user/ids";// 获取某个人的商务圈消息

	//http://192.168.100.10:8081/

	public static final String UPLOAD_URL= CONFIG_UPLOAD + "upload/UploadServlet";// 上传图片接口;// 上传图片接口


	public static final int XMPP_PORT=5222 ;



	public static final String ACTION_LOCATION_UPDATE = UIUtils.getPackageName()+ ".action.location_update";

	/* 附近 */
	public static final String NEARBY_USER= CONFIG_IP + "nearby/user";// 获取附近的用户;// 获取附近的用户
	/* 分页的Size */
	public static final int PAGE_SIZE = 100;

	/* 进入SingleImagePreviewActivity需要带上的参数 */
	public static final String EXTRA_IMAGE_URI = "image_uri";


	public static final String FRIENDS_ATTENTION_ADD=CONFIG_IP + "friends/attention/add";// 加关注
	public static final String FRIENDS_BLACKLIST_DELETE=CONFIG_IP + "friends/blacklist/delete";// 取消拉黑

	//public static final String USER_GET_URL= CONFIG_IP + "user/get";;// 获取用户资料，更新本地数据的接口
	public static final String EXTRA_USER_ID = "userId";// userId



	public static final String AVATAR_ORIGINAL_PREFIX=IP+"avatar/o";// 头像原图前缀地址
	public static final String AVATAR_THUMB_PREFIX=IP+"avatar/t";// 头像缩略图前缀地址


	// BusinessCircleActivity需要的
	public static final String EXTRA_CIRCLE_TYPE = "circle_type";// 看的商务圈类型
	public static final int CIRCLE_TYPE_MY_BUSINESS = 0;// 看的商务圈类型,是我的商务圈
	public static final int CIRCLE_TYPE_PERSONAL_SPACE = 1;// 看的商务圈类型，是个人空间
	public static final String MSG_USER_LIST = CONFIG_IP + "b/circle/msg/user";// 获取某个用户的最新公共消息

	public static final String ONLINE_MSG = CONFIG_IP + "user/read_offline_msg";	//	单聊聊天记录 /tigase/shiku_msgs


	/* 群聊 */
	public static final String ROOM_ADD = CONFIG_IP + "room/add";// 创建群组;// 创建群组

	public static final String ROOM_GET = CONFIG_IP + "room/get";// 获取群组;// 获取群组

	public static final String ROOM_MEMBER_DELETE = CONFIG_IP + "room/member/delete";// 删除成员

	public static final String ROOM_DELETE = CONFIG_IP + "room/delete";// 删除群组;// 删除群组

	public static final String ROOM_MEMBER_UPDATE= CONFIG_IP + "room/member/update";// 设置群员;// 设置群员

	public static final String ROOM_JOIN = CONFIG_IP + "/room/join";// 加入房间;// 加入房间


	public static final  String MSG_ADD_URL = CONFIG_IP + "b/circle/msg/add";// 发布一条公共消息的接口;// 发布一条公共消息的接口

	public static final  String  MSG_COMMENT_ADD=CONFIG_IP + "b/circle/msg/comment/add";// 增加一条评论;// 增加一条评论

	public static final  String  CIRCLE_MSG_DELETE=CONFIG_IP + "b/circle/msg/delete";// 删除一条商务圈消息

	public static final  String AVATAR_UPLOAD_URL=CONFIG_UPLOAD+ "upload/UploadAvatarServlet";// 上传头像接口;// 上传头像接口


	public static final  String USER_NEAR = CONFIG_IP + "nearby/user";//查询搜索列表;


	public static final  String  USER_REGISTER= CONFIG_IP + "user/register";// 注册;// 注册


	//public static final  String  ROOM_LIST= CONFIG_IP + "room/user_room_list";// 获取房间列表
	public static final String ROOM_LIST= CONFIG_IP + "room/list";// 获取群组列表;  sK


	public static final String USER_UPDATE = CONFIG_IP + "user/update";// 用户资料修改;// 用户资料修改
	public static final String ROOM_UPDATE = CONFIG_IP + "room/update";// 设置群组;// 设置群组


	public static final String MSG_PRAISE_ADD = CONFIG_IP + "b/circle/msg/praise/add";// 赞;// 赞
	public static final String MSG_PRAISE_DELETE= CONFIG_IP + "b/circle/msg/praise/delete";// 取消赞;// 取消赞

	//public static final String ROOM_MEMBER_UPDATE= CONFIG_IP + "room/member/update";// 设置群员;// 设置群员

	public static final String MSG_GETS= CONFIG_IP + "b/circle/msg/gets";// 根据IDS批量获取公共消息的接口(我的商务圈使用)
	//新的朋友圈
	public static final String GET_ALL_CIRCLE=CONFIG_IP+"/b/circle/msg/getCommunityMsg";

}

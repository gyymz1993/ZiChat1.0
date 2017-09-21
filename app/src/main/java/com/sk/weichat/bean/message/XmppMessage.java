package com.sk.weichat.bean.message;

import com.alibaba.fastjson.JSONObject;
import com.j256.ormlite.field.DatabaseField;

public abstract class XmppMessage {

	/** 消息类型：系统广播消息 */
	public static final int TYPE_800 = 800;// 系统广播
	public static final int TYPE_801 = 801;// 活动报名
	public static final int TYPE_802 = 802;// 奖励促销
	/** 消息类型：群聊提示消息 */
	public static final int TYPE_900 = 900;// 已进群
	public static final int TYPE_901 = 901;// 已退群
	// /** 消息类型：面试中心消息 */
	// public static final int TYPE_70 = 70;// 新面试
	// public static final int TYPE_71 = 71;// 笔试成绩
	// public static final int TYPE_72 = 72;// 静态面试成绩
	// public static final int TYPE_73 = 73;// 动态面试成绩
	/** 消息类型：商务圈消息 */
	public static final int TYPE_600 = 600;// 新评论
	public static final int TYPE_601 = 601;// 新礼物
	public static final int TYPE_602 = 602;// 新赞
	public static final int TYPE_603 = 603;// 新公共消息
	/** 消息类型：新朋友消息 */
	public static final int TYPE_SAYHELLO = 500;// 打招呼
	public static final int TYPE_PASS = 501;// 同意加好友
	public static final int TYPE_FEEDBACK = 502;// 回话
	public static final int TYPE_NEWSEE = 503;// 新关注
	public static final int TYPE_DELSEE = 504;// 删除关注
	public static final int TYPE_DELALL = 505;// 彻底删除
	public static final int TYPE_RECOMMEND = 506;// 新推荐好友
	public static final int TYPE_BLACK = 507;// 黑名单
	public static final int TYPE_FRIEND = 508;// 直接成为好友
	/** 消息类型：正在输入消息 */
	public static final int TYPE_ENTERING = 400;// 正在输入消息
	// //////////////////////////////以上均为广播消息的类型///////////////////////////////

	// //////////////////////////////以下为在聊天界面显示的类型///////////////////////////////
	public static final int TYPE_TEXT = 1;// 文字
	public static final int TYPE_IMAGE = 2;// 图片
	public static final int TYPE_VOICE = 3;// 语音
	public static final int TYPE_LOCATION = 4;// 位置
	public static final int TYPE_GIF = 5;// gif
	public static final int TYPE_VIDEO = 6;// 视频
	public static final int TYPE_SIP_AUDIO = 7;// 音频
	public static final int TYPE_CARD = 8;// 名片
	public static final int TYPE_FILE = 9;//文件
	public static final int TYPE_TIP = 10;// 自己添加的消息类型,代表系统的提示

	// 面试者收到的职位、初试、面试推送类型
	public static final int TYPE_201 = 201;// 收到招聘方同意申请，进入初试
	public static final int TYPE_202 = 202;// 收到招聘方通过初试，进入面试
	public static final int TYPE_203 = 203;// 收到招聘方未通过初试
	public static final int TYPE_204 = 204;// 收到招聘方面试邀请(后台发送的)
	public static final int TYPE_205 = 205;// 收到招聘方重设的面试时间
	public static final int TYPE_206 = 206;// 收到招聘方通过面试
	public static final int TYPE_207 = 207;// 收到招聘方未通过面试
	public static final int TYPE_208 = 208;// 收到招聘方物理删除面试（暂时无此推送）
	public static final int TYPE_209 = 209;// 收到招聘方物理删除初始（暂时无此推送）
	public static final int TYPE_210 = 210;// 申请通过，直接面试
	public static final int TYPE_211 = 211;// 申请通过，直接线下复试
	public static final int TYPE_212 = 212;// 初试通过，直接线下复试
	// 自定义的模拟推送
	public static final int TYPE_299 = 299;// 自己模拟的面试从未开始到进行中

	// 招聘者收到的职位、初试、面试推送类型
	public static final int TYPE_300 = 300;// 收到应聘方应聘申请
	public static final int TYPE_301 = 301;// 收到应聘方交卷
	public static final int TYPE_302 = 302;// 收到应聘方拒绝初试
	public static final int TYPE_303 = 303;// 收到应聘方确定面试时间
	public static final int TYPE_304 = 304;// 收到应聘方拒绝面试
	public static final int TYPE_305 = 305;// 收到另一HR暂停职位
	public static final int TYPE_306 = 306;// 收到另一HR恢复职位（无用）
	public static final int TYPE_307 = 307;// 收到另一HR取消职位
	public static final int TYPE_308 = 308;// 收到另一HR删除职位（无用）
	// 自定义的模拟推送
	public static final int TYPE_398 = 398;// 自己模拟的面试未开始推送
	public static final int TYPE_399 = 399;// 自己模拟的初试待交卷推送
	// 群聊推送
	public static final int TYPE_CHANGE_NICK_NAME = 901;// 修改昵称
	public static final int TYPE_CHANGE_ROOM_NAME = 902;// 修改房间名
	public static final int TYPE_DELETE_ROOM = 903;// 删除房间
	public static final int TYPE_DELETE_MEMBER = 904;// 删除成员
	public static final int TYPE_NEW_NOTICE = 905;// 新公告
	public static final int TYPE_GAG = 906;// 禁言
	public static final int NEW_MEMBER = 907;//增加新成员

	@DatabaseField(canBeNull = false)
	protected String packetId;// 消息包的Id

	/* 网络传输字段 */
	@DatabaseField(canBeNull = false)
	protected int type;// 消息的类型

	@DatabaseField(canBeNull = false)
	protected int timeSend;// 发送时间,秒级别的,为点击发送按钮，开始发送的时间

	@DatabaseField
	protected boolean isMySend = true;// 是否是由我自己发送，代替toUserId，toUserId废弃不用,默认值true，代表是我发送的

	public boolean isMySend() {
		return isMySend;
	}

	public void setMySend(boolean isMySend) {
		this.isMySend = isMySend;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getTimeSend() {
		return timeSend;
	}

	public void setTimeSend(int timeSend) {
		this.timeSend = timeSend;
	}

	public String getPacketId() {
		return packetId;
	}

	public void setPacketId(String packetId) {
		this.packetId = packetId;
	}

	protected String getStringValueFromJSONObject(JSONObject jObject, String key) {
		String value = "";
		try {
			value = jObject.getString(key);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (value == null) {
			value = "";
		}
		return value;
	}

	protected int getIntValueFromJSONObject(JSONObject jObject, String key) {
		int value = 0;
		try {
			value = jObject.getIntValue(key);
		} catch (Exception e) {
			e.printStackTrace();
			value = 0;
		}
		return value;
	}

}

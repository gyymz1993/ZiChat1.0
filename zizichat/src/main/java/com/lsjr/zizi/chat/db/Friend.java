package com.lsjr.zizi.chat.db;

import android.text.TextUtils;

import com.alibaba.fastjson.annotation.JSONField;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.ys.cn.CN;

import java.io.Serializable;

import cn.ittiger.indexlist.entity.BaseEntity;

@DatabaseTable
public class Friend  implements Serializable ,BaseEntity,CN {
	private static final long serialVersionUID = -6859528031175998594L;
	public static final String ID_SYSTEM_MESSAGE = "10000";// 系统消息ID
	public static final String ID_NEW_FRIEND_MESSAGE = "10001";// 新朋友消息 ID
	public static final String ID_BLOG_MESSAGE = "10002";// 商务圈消息ID
	public static final String ID_INTERVIEW_MESSAGE = "10004";// 面试中心ID（用于职位、初试、面试的推送）
	public static final String ID_MUC_ROOM = "10005";// 群聊管理ID（群聊房间的推送）

	public static final String ONLIN_MSG = "10000018";

	public static final String NICKNAME_SYSTEM_MESSAGE = "系统消息";// 系统消息ID
	public static final String NICKNAME_NEW_FRIEND_MESSAGE = "新朋友消息";// 新朋友消息
	public static final String NICKNAME_BLOG_MESSAGE = "商务圈消息";// 商务圈消息ID
	public static final String NICKNAME_INTERVIEW_MESSAGE = "面试中心";// 面试中心ID

	// -1:黑名单；0：陌生人；1:单方关注；2:互为好友；8:显示系统号；9:非显示系统号
	public static final int STATUS_NO_SHOW_SYSTEM = 9;// 非显示系统号
	public static final int STATUS_SYSTEM = 8;// 显示系统号
	public static final int STATUS_FRIEND = 2;// 好友
	public static final int STATUS_ATTENTION = 1;// 关注
	public static final int STATUS_UNKNOW = 0;// 陌生人(不可能出现在好友表，只可能在新朋友消息表)
	public static final int STATUS_BLACKLIST = -1;// 黑名单
	public static final int STATUS_SELF = 9999;// 本人，特殊状态，在数据库中没有，在UI层判断是不是当前登陆者本人，显示控制不同表现

	@DatabaseField(generatedId = true)
	private int _id;

	@DatabaseField(canBeNull = false)
	private String ownerId; // 属于哪个用户的id

	@DatabaseField(canBeNull = false)
	private String userId; // 用户id或者聊天室id

	@DatabaseField(canBeNull = false)
	@JSONField(name = "nickname")
	private String nickName;// 用户昵称或者聊天室名称

	@DatabaseField
	private String description;// 签名

	@DatabaseField
	private int timeCreate;// 创建好友关系的时间

	@DatabaseField(defaultValue = "0")
	private int unReadNum; // 未读消息数量

	@DatabaseField
	private String content;// 最后一条消息内容

	@DatabaseField
	private int type;// 最后一条消息类型

	@DatabaseField
	private int timeSend;// 最后一条消息发送时间

	@DatabaseField(defaultValue = "0")
	private int roomFlag;// 0朋友 1群组

	@DatabaseField(defaultValue = "0")
	private int companyId; // 0表示不是公司

	@DatabaseField
	private int status;// -1:黑名单；0：陌生人；1:单方关注；2:互为好友；8:系统号；9:非显示系统号

	@DatabaseField
	private String privacy;// 隐私

	@DatabaseField
	private String remarkName;// 备注

	@DatabaseField
	private int version;// 本地表的版本

	@DatabaseField
	private String roomId;// 仅仅当roomFlag==1，为群组的时候才有效

	@DatabaseField
	private String roomCreateUserId;// 仅仅当roomFlag==1，为群组的时候才有效

	@DatabaseField
	private String roomMyNickName;// 我在这个房间的昵称

	@DatabaseField
	private int roomTalkTime;// wi在这个房间的禁言时间

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public String getRoomCreateUserId() {
		return roomCreateUserId;
	}

	public void setRoomCreateUserId(String roomCreateUserId) {
		this.roomCreateUserId = roomCreateUserId;
	}

	public String getRemarkName() {
		return remarkName;
	}

	public void setRemarkName(String remarkName) {
		this.remarkName = remarkName;
	}

	public int get_id() {
		return _id;
	}

	public void set_id(int _id) {
		this._id = _id;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickname) {
		this.nickName = nickname;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getTimeCreate() {
		return timeCreate;
	}

	public void setTimeCreate(int timeCreate) {
		this.timeCreate = timeCreate;
	}

	public int getUnReadNum() {
		return unReadNum;
	}

	public void setUnReadNum(int unReadNum) {
		this.unReadNum = unReadNum;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
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

	public int getRoomFlag() {
		return roomFlag;
	}

	public void setRoomFlag(int roomFlag) {
		this.roomFlag = roomFlag;
	}

	public int getCompanyId() {
		return companyId;
	}

	public void setCompanyId(int companyId) {
		this.companyId = companyId;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getPrivacy() {
		return privacy;
	}

	public void setPrivacy(String privacy) {
		this.privacy = privacy;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getRoomMyNickName() {
		return roomMyNickName;
	}

	public void setRoomMyNickName(String roomMyNickName) {
		this.roomMyNickName = roomMyNickName;
	}

	public int getRoomTalkTime() {
		return roomTalkTime;
	}

	public void setRoomTalkTime(int roomTalkTime) {
		this.roomTalkTime = roomTalkTime;
	}

	/* 快捷方法，获取在好友列表中显示的名称 */
	public String getShowName() {
		if (!TextUtils.isEmpty(remarkName)) {
			return remarkName.trim();
		} else if (!TextUtils.isEmpty(nickName)) {
			return nickName.trim();
		} else {
			return "";
		}
	}

	/**
	 * 快捷方法
	 *
	 * @param userId
	 * @param nickName
	 * @return
	 */
	public static String getShowName(String userId, String nickName) {
		User loginUser = ConfigApplication.instance().mLoginUser;
		if (loginUser == null || TextUtils.isEmpty(loginUser.getUserId())) {
			return nickName;
		}
		String showName = FriendDao.getInstance().getRemarkName(loginUser.getUserId(), userId);
		if (TextUtils.isEmpty(showName)) {
			showName = nickName;
		}
		return showName;
	}

	@Override
	public String toString() {
		return "Friend{" +
				"_id=" + _id +
				", ownerId='" + ownerId + '\'' +
				", userId='" + userId + '\'' +
				", nickName='" + nickName + '\'' +
				", description='" + description + '\'' +
				", timeCreate=" + timeCreate +
				", unReadNum=" + unReadNum +
				", content='" + content + '\'' +
				", type=" + type +
				", timeSend=" + timeSend +
				", roomFlag=" + roomFlag +
				", companyId=" + companyId +
				", status=" + status +
				", privacy='" + privacy + '\'' +
				", remarkName='" + remarkName + '\'' +
				", version=" + version +
				", roomId='" + roomId + '\'' +
				", roomCreateUserId='" + roomCreateUserId + '\'' +
				", roomMyNickName='" + roomMyNickName + '\'' +
				", roomTalkTime=" + roomTalkTime +
				'}';
	}

	@Override
	public String getIndexField() {
		return remarkName==null?nickName:remarkName;
	}

	@Override
	public String chinese() {
		return getShowName();
	}
}

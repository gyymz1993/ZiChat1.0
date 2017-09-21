package com.lsjr.zizi.bean;

import android.text.TextUtils;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

/**
 * 评论
 * 
 */
public class Comment implements Serializable {
	private static final long serialVersionUID = -540132570820293901L;
	private String commentId;
	private String userId;// 评论者的id

	@JSONField(name = "nickname")
	private String nickName;// 评论者名字
	private String toUserId;// 被评论者的Id
	private String toNickname;// 被评论者的名字
	private String body;// 评论内容
	private String toBody;// 被评论内容
	private String msgId;
	private long time;

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public String getCommentId() {
		return commentId;
	}

	public void setCommentId(String commentId) {
		this.commentId = commentId;
	}

	public String getToUserId() {
		return toUserId;
	}

	public void setToUserId(String toUserId) {
		this.toUserId = toUserId;
	}

	public String getToNickname() {
		return toNickname;
	}

	public void setToNickname(String toNickname) {
		this.toNickname = toNickname;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getToBody() {
		return toBody;
	}

	public void setToBody(String toBody) {
		this.toBody = toBody;
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

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	// 快捷方法，判断是不是针对某个人的回复
	public boolean isReplaySomeBody() {
		if (!TextUtils.isEmpty(toUserId) && !TextUtils.isEmpty(toNickname)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Comment{" +
				"commentId='" + commentId + '\'' +
				", userId='" + userId + '\'' +
				", nickName='" + nickName + '\'' +
				", toUserId='" + toUserId + '\'' +
				", toNickname='" + toNickname + '\'' +
				", body='" + body + '\'' +
				", toBody='" + toBody + '\'' +
				", msgId='" + msgId + '\'' +
				", time=" + time +
				'}';
	}
}
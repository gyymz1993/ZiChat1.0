package com.lsjr.zizi.chat.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class MucRoomMember {
	private String userId;// id

	@JSONField(name = "nickname")
	private String nickName;// 昵称
	private int role;// 1创建者，2管理员，3成员
	private int talkTime;// 禁言时间
	private int active;// 最后一次互动时间
	private int sub;// 0屏蔽消息，1不屏蔽
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getNickName() {
		return nickName;
	}
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	public int getRole() {
		return role;
	}
	public void setRole(int role) {
		this.role = role;
	}
	public int getTalkTime() {
		return talkTime;
	}
	public void setTalkTime(int talkTime) {
		this.talkTime = talkTime;
	}
	public int getActive() {
		return active;
	}
	public void setActive(int active) {
		this.active = active;
	}
	public int getSub() {
		return sub;
	}

	@Override
	public String toString() {
		return "MucRoomMember{" +
				"userId='" + userId + '\'' +
				", nickName='" + nickName + '\'' +
				", role=" + role +
				", talkTime=" + talkTime +
				", active=" + active +
				", sub=" + sub +
				'}';
	}

	public void setSub(int sub) {
		this.sub = sub;
	}


	
}

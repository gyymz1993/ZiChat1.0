package com.lsjr.zizi.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class AttentionUser {

	private int blacklist;// 0 表示不是黑名单，1表示是在黑名单
	private int createTime;
	private int groupId;// 分组Id（用不到）
	private String groupName;// 分组名称
	private int modifyTime;// 修改时间（用不到）
	private int status;
	private String toUserId;// 被关注的人
	private String userId;// 发起关注的人
	
	private int companyId;// 
	
	@JSONField(name="toNickname")
	private String toNickName;
	
	private String remarkName;
	public int getBlacklist() {
		return blacklist;
	}
	public void setBlacklist(int blacklist) {
		this.blacklist = blacklist;
	}
	public int getCreateTime() {
		return createTime;
	}
	public void setCreateTime(int createTime) {
		this.createTime = createTime;
	}
	public int getGroupId() {
		return groupId;
	}
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public int getModifyTime() {
		return modifyTime;
	}
	public void setModifyTime(int modifyTime) {
		this.modifyTime = modifyTime;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getToUserId() {
		return toUserId;
	}
	public void setToUserId(String toUserId) {
		this.toUserId = toUserId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getRemarkName() {
		return remarkName;
	}
	public void setRemarkName(String remarkName) {
		this.remarkName = remarkName;
	}
	public String getToNickName() {
		return toNickName;
	}
	public void setToNickName(String toNickName) {
		this.toNickName = toNickName;
	}
	public int getCompanyId() {
		return companyId;
	}
	public void setCompanyId(int companyId) {
		this.companyId = companyId;
	}
	
}

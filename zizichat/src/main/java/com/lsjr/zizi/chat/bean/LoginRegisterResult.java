package com.lsjr.zizi.chat.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class LoginRegisterResult {

	private String access_token;
	private int expires_in;// 多少秒后过时
	private String userId;
	@JSONField(name = "nickname")
	private String nickName;// 昵称
	private int companyId;// 公司Id
	private Login login;
	private Cv cv;

	public Cv getCv() {
		return cv;
	}

	public void setCv(Cv cv) {
		this.cv = cv;
	}

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	public int getExpires_in() {
		return expires_in;
	}

	public void setExpires_in(int expires_in) {
		this.expires_in = expires_in;
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

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public Login getLogin() {
		return login;
	}

	public void setLogin(Login login) {
		this.login = login;
	}

	public int getCompanyId() {
		return companyId;
	}

	public void setCompanyId(int companyId) {
		this.companyId = companyId;
	}

	public static class Cv {
		private String resumeId;
		private String resumeName;

		public String getResumeId() {
			return resumeId;
		}

		public void setResumeId(String resumeId) {
			this.resumeId = resumeId;
		}

		public String getResumeName() {
			return resumeName;
		}

		public void setResumeName(String resumeName) {
			this.resumeName = resumeName;
		}
	}

	public static class Login {
		private String userId;
		private int isFirstLogin;
		private long loginTime;
		private long offlineTime;
		private String apiVersion;
		private String model;
		private String osVersion;
		private String serial;
		private String latitude;
		private String longitude;
		private String location;

		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public int getIsFirstLogin() {
			return isFirstLogin;
		}

		public void setIsFirstLogin(int isFirstLogin) {
			this.isFirstLogin = isFirstLogin;
		}

		public long getLoginTime() {
			return loginTime;
		}

		public void setLoginTime(long loginTime) {
			this.loginTime = loginTime;
		}

		public String getApiVersion() {
			return apiVersion;
		}

		public void setApiVersion(String apiVersion) {
			this.apiVersion = apiVersion;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public String getOsVersion() {
			return osVersion;
		}

		public void setOsVersion(String osVersion) {
			this.osVersion = osVersion;
		}

		public String getSerial() {
			return serial;
		}

		public void setSerial(String serial) {
			this.serial = serial;
		}

		public String getLatitude() {
			return latitude;
		}

		public void setLatitude(String latitude) {
			this.latitude = latitude;
		}

		public String getLongitude() {
			return longitude;
		}

		public void setLongitude(String longitude) {
			this.longitude = longitude;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public long getOfflineTime() {
			return offlineTime;
		}

		public void setOfflineTime(long offlineTime) {
			this.offlineTime = offlineTime;
		}

	}
}

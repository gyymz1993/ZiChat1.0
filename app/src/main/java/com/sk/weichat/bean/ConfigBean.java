package com.sk.weichat.bean;

public class ConfigBean {
	@Override
	public String toString() {
		return "ConfigBean{" +
				"android=" + android +
				", ftpHost='" + ftpHost + '\'' +
				", ftpUsername='" + ftpUsername + '\'' +
				", ftpPassword='" + ftpPassword + '\'' +
				", apiUrl='" + apiUrl + '\'' +
				", uploadUrl='" + uploadUrl + '\'' +
				", downloadUrl='" + downloadUrl + '\'' +
				", downloadAvatarUrl='" + downloadAvatarUrl + '\'' +
				", XMPPHost='" + XMPPHost + '\'' +
				", XMPPDomain='" + XMPPDomain + '\'' +
				", resumeBaseUrl='" + resumeBaseUrl + '\'' +
				", meetingHost='" + meetingHost + '\'' +
				", shareUrl='" + shareUrl + '\'' +
				", softURL='" + softURL + '\'' +
				", helpURL='" + helpURL + '\'' +
				", buyURL='" + buyURL + '\'' +
				", aboutURL='" + aboutURL + '\'' +
				", videoLen=" + videoLen +
				", audioLen=" + audioLen +
				", money=" + money +
				'}';
	}

	public static class Android {
		private String disableVersion;// 不可见的版本号
		private String version;// 最新版本号
		private String versionRemark;// 最新版本描述
		private String message;// 广告

		public String getDisableVersion() {
			return disableVersion;
		}

		public void setDisableVersion(String disableVersion) {
			this.disableVersion = disableVersion;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getVersionRemark() {
			return versionRemark;
		}

		public void setVersionRemark(String versionRemark) {
			this.versionRemark = versionRemark;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}

	public static class Money {
		private int isCanChange;// 礼物能兑换
		private int Login;// 登录送多少
		private int Share;// 分享送多少
		private int Intro;// 推荐送多少

		public int getIsCanChange() {
			return isCanChange;
		}

		public void setIsCanChange(int isCanChange) {
			this.isCanChange = isCanChange;
		}

		public int getLogin() {
			return Login;
		}

		public void setLogin(int login) {
			Login = login;
		}

		public int getShare() {
			return Share;
		}

		public void setShare(int share) {
			Share = share;
		}

		public int getIntro() {
			return Intro;
		}

		public void setIntro(int intro) {
			Intro = intro;
		}
	}

	private Android android;// Android版本信息(无用)

	private String ftpHost;// ftp(无用)
	private String ftpUsername;// ftp用户名(无用)
	private String ftpPassword;// ftp密码(无用)

	private String apiUrl;// Api 的服务器地址
	private String uploadUrl;// 上传 的服务器地址
	private String downloadUrl;// 下载文件的前缀(无用)
	private String downloadAvatarUrl;// 下载头像的前缀
	private String XMPPHost;// xmpp主机
	private String XMPPDomain;// xmpp 群聊的域名
	private String resumeBaseUrl;// 简历预览前缀地址
	private String meetingHost;// 视频语音服务的ip

	private String shareUrl;// 分享后，访问的URL
	private String softURL;// 新版本的下载URL，分苹果安卓(无效，使用友盟的更新即可)
	private String helpURL;// 使用帮助
	private String buyURL;// 促销URL
	private String aboutURL;// 关于界面URL

	private int videoLen;// 录像最大时长
	private int audioLen;// 录音最大时长
	private Money money;// （无用）

	public Android getAndroid() {
		return android;
	}

	public void setAndroid(Android android) {
		this.android = android;
	}

	public String getApiUrl() {
		return apiUrl;
	}

	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	public String getUploadUrl() {
		return uploadUrl;
	}

	public void setUploadUrl(String uploadUrl) {
		this.uploadUrl = uploadUrl;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public String getDownloadAvatarUrl() {
		return downloadAvatarUrl;
	}

	public void setDownloadAvatarUrl(String downloadAvatarUrl) {
		this.downloadAvatarUrl = downloadAvatarUrl;
	}

	public String getXMPPHost() {
		return XMPPHost;
	}

	public void setXMPPHost(String xMPPHost) {
		XMPPHost = xMPPHost;
	}

	public String getXMPPDomain() {
		return XMPPDomain;
	}

	public void setXMPPDomain(String xMPPDomain) {
		XMPPDomain = xMPPDomain;
	}

	public String getFtpHost() {
		return ftpHost;
	}

	public void setFtpHost(String ftpHost) {
		this.ftpHost = ftpHost;
	}

	public String getFtpUsername() {
		return ftpUsername;
	}

	public void setFtpUsername(String ftpUsername) {
		this.ftpUsername = ftpUsername;
	}

	public String getFtpPassword() {
		return ftpPassword;
	}

	public void setFtpPassword(String ftpPassword) {
		this.ftpPassword = ftpPassword;
	}

	public String getMeetingHost() {
		return meetingHost;
	}

	public void setMeetingHost(String meetingHost) {
		this.meetingHost = meetingHost;
	}

	public String getShareUrl() {
		return shareUrl;
	}

	public void setShareUrl(String shareUrl) {
		this.shareUrl = shareUrl;
	}

	public String getSoftURL() {
		return softURL;
	}

	public void setSoftURL(String softURL) {
		this.softURL = softURL;
	}

	public String getHelpURL() {
		return helpURL;
	}

	public void setHelpURL(String helpURL) {
		this.helpURL = helpURL;
	}

	public String getBuyURL() {
		return buyURL;
	}

	public void setBuyURL(String buyURL) {
		this.buyURL = buyURL;
	}

	public String getAboutURL() {
		return aboutURL;
	}

	public void setAboutURL(String aboutURL) {
		this.aboutURL = aboutURL;
	}

	public int getVideoLen() {
		return videoLen;
	}

	public void setVideoLen(int videoLen) {
		this.videoLen = videoLen;
	}

	public int getAudioLen() {
		return audioLen;
	}

	public void setAudioLen(int audioLen) {
		this.audioLen = audioLen;
	}

	public Money getMoney() {
		return money;
	}

	public void setMoney(Money money) {
		this.money = money;
	}

	public String getResumeBaseUrl() {
		return resumeBaseUrl;
	}

	public void setResumeBaseUrl(String resumeBaseUrl) {
		this.resumeBaseUrl = resumeBaseUrl;
	}


	

}

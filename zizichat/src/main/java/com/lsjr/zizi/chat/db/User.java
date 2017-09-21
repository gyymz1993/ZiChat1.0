package com.lsjr.zizi.chat.db;

import java.io.Serializable;

import com.alibaba.fastjson.annotation.JSONField;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.lsjr.zizi.chat.bean.AttentionUser;
import com.lsjr.zizi.chat.utils.StringUtils;


/**
 * 用户表
 *
 *
 */
@DatabaseTable(tableName = "user")
public class User implements Serializable, Cloneable {
	private static final long serialVersionUID = 8216104856016715920L;

	// 无参构造函数
	public User() {
	}


	@DatabaseField(id = true)
	private String userId;// 用户Id

	@DatabaseField
	private int userType;// 用户类型

	@DatabaseField(canBeNull = false)
	@JSONField(name = "nickname")
	private String nickName;// 昵称

	@DatabaseField(canBeNull = false)
	private String telephone;

	@DatabaseField(canBeNull = false)
	private String password;

	@DatabaseField
	private String description;// 签名

	@DatabaseField
	private long birthday;// 公历生日

	@DatabaseField(defaultValue = "0")
	private int sex;// 性别 0表示女，1表示男

	@DatabaseField
	private int countryId;// 国家编号

	@DatabaseField
	private int provinceId;// 省份编号

	@DatabaseField
	private int cityId;// 城市编号

	@DatabaseField
	private int areaId;// 地区编号

	@DatabaseField
	private int integral;// 积分

	@DatabaseField
	private int integralTotal;// 积分总数

	@DatabaseField
	private int level;// 等级

	@DatabaseField
	private float money;// 金钱

	@DatabaseField
	private float moneyTotal;// 金钱总数

	@DatabaseField
	private int vip;// vip等级

	@DatabaseField
	private int friendsCount;// 朋友总数

	@DatabaseField
	private int fansCount;// 粉丝总数

	@DatabaseField
	private int attCount;// 关注总数

	@DatabaseField
	private int isAuth;// 是否认证

	@DatabaseField
	private int status;// 状态(未知)

	@DatabaseField
	private long offlineTime;

	@DatabaseField(foreign = true, foreignAutoRefresh = true)
	private Company company;// 所在公司信息

	/* 请求用户与该用户的关系 */
	private AttentionUser friends;

	private LoginLog loginLog;

	public static class LoginLog {
		private int isFirstLogin;
		private double latitude;
		private double longitude;
		private int loginTime;
		private String model;
		private String osVersion;
		private String serial;
		private int offlineTime;

		public int getIsFirstLogin() {
			return isFirstLogin;
		}

		public void setIsFirstLogin(int isFirstLogin) {
			this.isFirstLogin = isFirstLogin;
		}

		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		public int getLoginTime() {
			return loginTime;
		}

		public void setLoginTime(int loginTime) {
			this.loginTime = loginTime;
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

		public int getOfflineTime() {
			return offlineTime;
		}

		public void setOfflineTime(int offlineTime) {
			this.offlineTime = offlineTime;
		}

	}

	public LoginLog getLoginLog() {
		return loginLog;
	}

	public void setLoginLog(LoginLog loginLog) {
		this.loginLog = loginLog;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		User user = (User) super.clone();
		// 不需要下面两个成员变量的克隆
		user.setCompany(null);
		user.setFriends(null);
		return user;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof User)) {
			return false;
		}
		if (o == this) {
			return true;
		}
		User other = (User) o;
		boolean equals = true;
		equals &= StringUtils.strEquals(userId, other.userId);
		equals &= userType == other.userType;
		equals &= StringUtils.strEquals(nickName, other.nickName);
		equals &= StringUtils.strEquals(telephone, other.telephone);
		equals &= StringUtils.strEquals(password, other.password);
		equals &= StringUtils.strEquals(description, other.description);
		equals &= birthday == other.birthday;
		equals &= sex == other.sex;
		equals &= countryId == other.countryId;
		equals &= provinceId == other.provinceId;
		equals &= cityId == other.cityId;
		equals &= areaId == other.areaId;
		equals &= integral == other.integral;
		equals &= integralTotal == other.integralTotal;
		equals &= level == other.level;
		equals &= money == other.money;
		equals &= moneyTotal == other.moneyTotal;
		equals &= offlineTime == other.offlineTime;
		equals &= vip == other.vip;
		equals &= friendsCount == other.friendsCount;
		equals &= fansCount == other.fansCount;
		equals &= attCount == other.attCount;
		equals &= isAuth == other.isAuth;
		equals &= status == other.status;
		// 下面两个成员变量不比较
		// Company company;// 所在公司信息
		// AttentionUser friends;
		return equals;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public int getUserType() {
		return userType;
	}

	public void setUserType(int userType) {
		this.userType = userType;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getTelephone() {
		return telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getBirthday() {
		return birthday;
	}

	public void setBirthday(long birthday) {
		this.birthday = birthday;
	}

	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}

	public int getCountryId() {
		return countryId;
	}

	public void setCountryId(int countryId) {
		this.countryId = countryId;
	}

	public int getProvinceId() {
		return provinceId;
	}

	public void setProvinceId(int provinceId) {
		this.provinceId = provinceId;
	}

	public int getCityId() {
		return cityId;
	}

	public void setCityId(int cityId) {
		this.cityId = cityId;
	}

	public int getAreaId() {
		return areaId;
	}

	public void setAreaId(int areaId) {
		this.areaId = areaId;
	}

	public int getIntegral() {
		return integral;
	}

	public void setIntegral(int integral) {
		this.integral = integral;
	}

	public int getIntegralTotal() {
		return integralTotal;
	}

	public void setIntegralTotal(int integralTotal) {
		this.integralTotal = integralTotal;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public float getMoney() {
		return money;
	}

	public void setMoney(float money) {
		this.money = money;
	}

	public float getMoneyTotal() {
		return moneyTotal;
	}

	public void setMoneyTotal(float moneyTotal) {
		this.moneyTotal = moneyTotal;
	}

	public int getVip() {
		return vip;
	}

	public void setVip(int vipLevel) {
		this.vip = vipLevel;
	}

	public int getFriendsCount() {
		return friendsCount;
	}

	public long getOfflineTime() {
		return offlineTime;
	}

	public void setOfflineTime(long offlineTime) {
		this.offlineTime = offlineTime;
	}

	public void setFriendsCount(int friendsCount) {
		this.friendsCount = friendsCount;
	}

	public int getFansCount() {
		return fansCount;
	}

	public void setFansCount(int fansCount) {
		this.fansCount = fansCount;
	}

	public int getAttCount() {
		return attCount;
	}

	public void setAttCount(int attCount) {
		this.attCount = attCount;
	}

	public int getIsAuth() {
		return isAuth;
	}

	public void setIsAuth(int isAuth) {
		this.isAuth = isAuth;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public AttentionUser getFriends() {
		return friends;
	}

	public void setFriends(AttentionUser friends) {
		this.friends = friends;
	}

	/* 快捷方法 */
	public boolean isCompanyUser() {
		if (company != null && company.getId() > 0) {
			return true;
		} else {
			return false;
		}
	}

	public int getCompanyId() {
		if (company != null && company.getId() > 0) {
			return company.getId();
		} else {
			return 0;
		}
	}

	/* 获取两者之间的好友关系 */
	public int getRelationshipStatus() {
		if (friends == null) {
			return Friend.STATUS_UNKNOW;
		} else {
			return friends.getStatus();
		}
	}

	/* 获取两者之间的黑名单关系 */
	public int getBlacklist() {
		if (friends == null) {
			return 0;
		} else {
			return friends.getBlacklist();
		}
	}

	@Override
	public String toString() {
		return "User{" +
				"userId='" + userId + '\'' +
				", userType=" + userType +
				", nickName='" + nickName + '\'' +
				", telephone='" + telephone + '\'' +
				", password='" + password + '\'' +
				", description='" + description + '\'' +
				", birthday=" + birthday +
				", sex=" + sex +
				", countryId=" + countryId +
				", provinceId=" + provinceId +
				", cityId=" + cityId +
				", areaId=" + areaId +
				", integral=" + integral +
				", integralTotal=" + integralTotal +
				", level=" + level +
				", money=" + money +
				", moneyTotal=" + moneyTotal +
				", vip=" + vip +
				", friendsCount=" + friendsCount +
				", fansCount=" + fansCount +
				", attCount=" + attCount +
				", isAuth=" + isAuth +
				", status=" + status +
				", offlineTime=" + offlineTime +
				", company=" + company +
				", friends=" + friends +
				", loginLog=" + loginLog +
				'}';
	}
}


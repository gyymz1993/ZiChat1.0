package com.lsjr.zizi.chat.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * 手机发送的验证码，存到本地，以便退出程序，依然可以使用上次没有用过的验证码
 *
 *
 */
@DatabaseTable
public class AuthCode {

	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField(canBeNull = false)
	private String phoneNumber;
	@DatabaseField(canBeNull = false)
	private String randcode;
	@DatabaseField(canBeNull = false)
	private int overdueTime;// 秒级别的时间戳

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}


	public String getRandcode() {
		return randcode;
	}

	public void setRandcode(String randcode) {
		this.randcode = randcode;
	}

	public int getOverdueTime() {
		return overdueTime;
	}

	public void setOverdueTime(int overdueTime) {
		this.overdueTime = overdueTime;
	}

}

package com.lsjr.zizi.chat.bean;
/**
 * 检查Token是否过期的Bean
 *
 */
public class LoginAuto {
	
	private int tokenExists;// 1=令牌存在、0=令牌不存在
	private int serialStatus;//1=没有设备号、2=设备号一致、3=设备号不一致
	public int getTokenExists() {
		return tokenExists;
	}
	public void setTokenExists(int tokenExists) {
		this.tokenExists = tokenExists;
	}
	public int getSerialStatus() {
		return serialStatus;
	}
	public void setSerialStatus(int serialStatus) {
		this.serialStatus = serialStatus;
	}
	
}

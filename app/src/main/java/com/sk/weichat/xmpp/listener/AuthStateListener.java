package com.sk.weichat.xmpp.listener;

public interface AuthStateListener {
	public static final int AUTH_STATE_NOT = 1; // 未登录
	public static final int AUTH_STATE_ING = 2; // 登录中
	public static final int AUTH_STATE_SUCCESS = 3;// 已经认证
	public void onAuthStateChange(int authState);
}

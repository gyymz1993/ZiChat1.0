package com.sk.weichat.xmpp.listener;

import com.sk.weichat.bean.message.ChatMessage;

public interface ChatMessageListener {

	public static final int MESSAGE_SEND_ING = 0; // 发送中
	public static final int MESSAGE_SEND_SUCCESS = 1; // 发送成功
	public static final int MESSAGE_SEND_FAILED = 2; // 发送失败

	public void onMessageSendStateChange(int messageState, int msg_id);

	public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg);

}

package com.lsjr.zizi.chat.xmpp.listener;


import com.lsjr.zizi.chat.db.ChatMessage;

public interface ChatMessageListener {

	int MESSAGE_SEND_ING = 0; // 发送中
	int MESSAGE_SEND_SUCCESS = 1; // 发送成功
	int MESSAGE_SEND_FAILED = 2; // 发送失败

	 void onMessageSendStateChange(int messageState, int msg_id);

	 boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg);

}

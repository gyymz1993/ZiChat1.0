package com.lsjr.zizi.chat.xmpp.listener;


import com.lsjr.zizi.chat.db.NewFriendMessage;

public interface NewFriendListener {
	/**
	 * 
	 * @param toUserId  用于在收到回执的界面判断是不是当前操作的用户
	 * @param message 新朋友消息
	 * @param messageState 消息发送状态
	 */
	public void onNewFriendSendStateChange(String toUserId, NewFriendMessage message, int messageState);

	/**
	 * 
	 * @param message
	 * @param isPreRead  前一条消息是否已经读了
	 * @return 这条消息有没有人处理
	 */
	public boolean onNewFriend(NewFriendMessage message);
}

package com.lsjr.zizi.chat.xmpp.listener;

public interface MucListener {

	// public void newNotice(String roomJid);新公告作为一个消息去通知
	// public void onMucStateChange(String roomJid,Friend friendEntity,String state);
	// public void onMucAddMember(String roomJid,String nickName,String result);
	// public void onMucGetMember(String roomJid,String nickName,String result);
	// public void onMucKicked(String roomJid,String reason);//被提出
	/**
	 * 
	 * @param toUserId
	 *            实际上是RoomJid的前缀
	 */
	public void onDeleteMucRoom(String toUserId);

	/**
	 * 
	 * @param toUserId
	 *            实际上是RoomJid的前缀
	 */
	public void onMyBeDelete(String toUserId);

	/**
	 * 
	 * @param toUserId
	 *            实际上是RoomJid的前缀
	 */
	public void onNickNameChange(String toUserId, String changedUserId, String changedName);

	/**
	 * 
	 * @param toUserId
	 *            实际上是RoomJid的前缀
	 */
	public void onMyVoiceBanned(String toUserId, int time);

}

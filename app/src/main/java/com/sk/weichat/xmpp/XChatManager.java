package com.sk.weichat.xmpp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sk.weichat.MyApplication;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.NewFriendMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.broadcast.CardcastUiUpdateUtil;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.broadcast.MucgroupUpdateUtil;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.NewFriendDao;
import com.sk.weichat.helper.FriendHelper;
import com.sk.weichat.util.ThreadManager;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.xmpp.listener.ChatMessageListener;
import com.sk.weichat.xmpp.util.XmppStringUtil;
import com.ymz.baselibrary.utils.L_;

public class XChatManager {
	// private static final String TAG = XChatManager.class.getSimpleName();
	private XMPPConnection mConnection;
	private ChatManager mChatManager;
	private String mLoginUserId;
	private String mServerName;

	private Map<String, Chat> mChatMaps = new HashMap<String, Chat>();

	private CoreService mService;

	public XChatManager(CoreService service, XMPPConnection connection) {
		mService = service;
		mConnection = connection;
		mConnection.addPacketListener(packetListener,packetFilter);
		mLoginUserId = StringUtils.parseName(mConnection.getUser());
		mServerName = mConnection.getServiceName();
		mChatManager = ChatManager.getInstanceFor(mConnection);
		mChatManager.setNormalIncluded(true);// 包含普通的消息
		CardcastUiUpdateUtil.broadcastUpdateUi(mService);
		mChatManager.addChatListener(new ChatManagerListener() {
			@Override
			public void chatCreated(Chat arg0, boolean arg1) {
				String userId = StringUtils.parseName(arg0.getParticipant());
				Chat existChat = mChatMaps.get(userId);
				if (existChat == arg0) {
					Log.d("roamer","existChat == arg0");
					return;
				}

				if (existChat != null) {
					existChat.removeMessageListener(mMessageListener);
					existChat.close();
				}
				mChatMaps.put(userId, arg0);
				arg0.addMessageListener(mMessageListener);//添加消息监听
			}
		});
	}
	PacketListener packetListener = new PacketListener() {
		@Override
		public void processPacket(Packet arg0) throws NotConnectedException {
			Message message = (Message) arg0;
			String from = message.getFrom();
			String to = message.getTo();
		}
	};

	PacketFilter packetFilter = new PacketFilter() {
		@Override
		public boolean accept(Packet arg0) {
			if (arg0 instanceof Message) {
				Message message = (Message) arg0;
				if (message.getType() == Message.Type.chat) {
					return true;
				}
				return false;
			} else {
				return false;
			}
		}
	};
	public void reset() {
		String userId = StringUtils.parseName(mConnection.getUser());
		if (!mLoginUserId.equals(userId)) {
			mChatMaps.clear();
			mLoginUserId = userId;
		}
	}
/**
 * 消息change的监听
 */
	private MessageListener mMessageListener = new MessageListener() {
		@Override
		public void processMessage(Chat arg0, Message message) {
			String from = message.getFrom();
			String to = message.getTo();
			Log.d("roamer","from:"+from+"to:"+to);
			if (TextUtils.isEmpty(from) || TextUtils.isEmpty(to)) {
				return;
			}
			if (!XmppStringUtil.isJID(from) || !XmppStringUtil.isJID(to)) {
				return;
			}
			if (!StringUtils.parseName(to).equals(mLoginUserId)) {// 不是发给我的，基本上是不可能的情况，还是麻痹的判断下
				return;
			}
			saveSingleMessage(message, false);//将消息保存到本地
			Log.d("roamer","将消息保存到本地");
		}
	};

	/**
	 * 发送聊天的消息
	 * 
	 * @param toUserId
	 *            要发送给的用户，
	 * @param chatMessage
	 *            已经存到本地数据库的一条即将发送的消息
	 */
	public void sendMessage(final String toUserId, final ChatMessage chatMessage) {
		ThreadManager.getPool().execute(new Runnable() {
			public void run() {
				Chat chat = getChat(toUserId);
				try {
					Message msg = new Message();
					msg.setType(Message.Type.chat);
					if (chatMessage.getType() >= XmppMessage.TYPE_TEXT && chatMessage.getType() <= XmppMessage.TYPE_TIP) {
						msg.setBody(chatMessage.toJsonString(false));
					} else {
						msg.setBody(chatMessage.toJsonString(true));// 新朋友推送消息
																	// 需要fromUserId字段
					}
					msg.setPacketID(chatMessage.getPacketId());
					DeliveryReceiptManager.addDeliveryReceiptRequest(msg);
					// 发送消息
					chat.sendMessage(msg);
					Log.d("roamer","消息发送中");
					ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.get_id(),
							ChatMessageListener.MESSAGE_SEND_ING);
				} catch (NotConnectedException e) {
					e.printStackTrace();
					ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.get_id(),
							ChatMessageListener.MESSAGE_SEND_FAILED);
				}
			}
		});
	}

	/**
	 * 发送新朋友推送消息
	 * 
	 * @param toUserId
	 *            要发送给的用户，
	 */
	public void sendMessage(final String toUserId, final NewFriendMessage newFriendMessage) {
		ThreadManager.getPool().execute(new Runnable() {
			public void run() {
				Chat chat = getChat(toUserId);
				try {
					Message msg = new Message();
					msg.setType(Message.Type.chat);
					msg.setBody(newFriendMessage.toJsonString());// 新朋友推送消息
					msg.setPacketID(newFriendMessage.getPacketId());
					DeliveryReceiptManager.addDeliveryReceiptRequest(msg);
					// 发送消息
					chat.sendMessage(msg);
					ListenerManager.getInstance().notifyNewFriendSendStateChange(toUserId, newFriendMessage, ChatMessageListener.MESSAGE_SEND_ING);
				} catch (NotConnectedException e) {
					e.printStackTrace();
					ListenerManager.getInstance().notifyNewFriendSendStateChange(toUserId, newFriendMessage, ChatMessageListener.MESSAGE_SEND_FAILED);
				}
			}
		});
	}

	private Chat getChat(String toUserId) {
		Log.d("roamer","getChat....");
		Chat chat = mChatMaps.get(toUserId);
		if (chat != null) {
			return chat;
		}
		chat = mChatManager.createChat(toUserId + "@" + mServerName, mMessageListener);
		return chat;
	}

	/**
	 * 保存接收到的聊天信息(单聊)
	 * 
	 * @param packetId
	 * @param fromUserId
	 * @param messageBody
	 * @param isRead
	 * @return
	 */
	private void saveSingleMessage(Message message, boolean isRead) {
		String fromUserId = StringUtils.parseName(message.getFrom());
		String messageBody = message.getBody();
		String packetId = message.getPacketID();
		if (fromUserId.equals(Friend.ID_SYSTEM_MESSAGE)) {// 推送的系统消息

		} else if (fromUserId.equals(Friend.ID_INTERVIEW_MESSAGE)) {
			// InterviewBrodcast.getInstance().newMessageCome(new InterviewMessage(messageBody));
		} else if (fromUserId.equals(Friend.ID_BLOG_MESSAGE)) {// 商务圈的推送消息

		} else if (fromUserId.equals(Friend.ID_MUC_ROOM)) {
			saveMucMessage(messageBody, packetId);
		} else {// 普通用户发送的消息
			if (message.getType() != Message.Type.chat) {// 朋友的消息必须是Chat类型的
				return;
			}
			JSONObject jObject = JSON.parseObject(messageBody);
			int type = 0;
			try {
				type = jObject.getIntValue("type");
				Log.d("roamer", "type:"+type);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// 根据消息的不同类型，做不同的存储操作
			if (type <=10) {// 普通的聊天消息，没有带fromUserId，所以要自己加上
				saveChatMessage(messageBody, fromUserId, packetId);
				Log.d("roamer", "普通的聊天消息，没有带fromUserId，所以要自己加上");
			} else {// 广播消息
				if (type == XmppMessage.TYPE_ENTERING) {// 正在输入
					// TODO 暂时不处理
				} else if (type >= XmppMessage.TYPE_SAYHELLO && type <= XmppMessage.TYPE_FRIEND) {// 新朋消息的处理
					saveNewFriendMessage(fromUserId, messageBody, packetId);
					
				}
			}
		}
	}

	private void saveMucMessage(String messageBody, String packetId) {
		JSONObject jObject = JSON.parseObject(messageBody);
		System.out.println(messageBody.toString()+"========================");
		int type = 0;
		String objectId = null;
		String content = null;
		int timeSend = 0;
		String fromUserId = null;
		String fromUserName = null;
		String toUserId = null;
		String toUserName = null;
		try {
			objectId = jObject.getString("objectId");
			if (TextUtils.isEmpty(objectId)) {
				return;
			}
			type = jObject.getIntValue("type");
			content = jObject.getString("content");
			timeSend = jObject.getIntValue("timeSend");
			fromUserId = jObject.getString("fromUserId");
			fromUserName = jObject.getString("fromUserName");
			toUserId = jObject.getString("toUserId");
			toUserName = jObject.getString("toUserName");
		} catch (Exception e) {
			e.printStackTrace();
		}

		Friend friend = FriendDao.getInstance().getMucFriendByRoomId(objectId);
		if (friend == null) {
			return;
		}

		switch (type) {
		case XmppMessage.TYPE_CHANGE_NICK_NAME: {// 昵称修改
			String currentName = toUserName;
			String name = friend.getRoomMyNickName();
			if (toUserId != null && toUserId.equals(mLoginUserId)) {// 我修改了昵称
				if (content != null && !currentName.equals(friend.getRoomMyNickName())) {// 我的昵称变了
					friend.setRoomMyNickName(content);
					FriendDao.getInstance().createOrUpdateFriend(friend);
					ListenerManager.getInstance().notifyNickNameChanged(friend.getUserId(), toUserId, content);
					ChatMessageDao.getInstance().updateNickName(mLoginUserId, friend.getUserId(), toUserId, content);
				}
			} else {// 其他人的昵称变了，通知下就可以了
				ChatMessage message = new ChatMessage();
				message.setTimeSend(timeSend);
				message.setContent("用户：" + currentName + " 昵称修改为 ‘" + content + "’");
				message.setPacketId(packetId);
				message.setType(XmppMessage.TYPE_TIP);
				if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), message)) {
					ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), message, true);
				}
				ChatMessageDao.getInstance().updateNickName(mLoginUserId, friend.getUserId(), toUserId, content);
				ListenerManager.getInstance().notifyNickNameChanged(friend.getUserId(), toUserId, content);
			}
		}
			break;
		case XmppMessage.TYPE_CHANGE_ROOM_NAME: // 房间名字修改
		case XmppMessage.TYPE_NEW_NOTICE:// 新公告
		{
			if (TextUtils.isEmpty(content)) {
				return;
			}
			ChatMessage message = new ChatMessage();
			message.setTimeSend(timeSend);
			if (type == XmppMessage.TYPE_CHANGE_ROOM_NAME) {
				FriendDao.getInstance().updateMucFriendRoomName(objectId, content);
				message.setContent("房间名字修改为：" + content);
				MyApplication.getInstance().roomName = content;
			} else {
				message.setContent("新公告为：" + content);
			}
			message.setPacketId(packetId);
			message.setType(XmppMessage.TYPE_TIP);
			if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), message)) {
				ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), message, true);
			}
		}
			break;
		case XmppMessage.TYPE_DELETE_ROOM: {// 删除房间

			L_.e("删除房间了----------");
			// 删除这个房间
			FriendDao.getInstance().deleteFriend(mLoginUserId, friend.getUserId());
			// 消息表中删除
			ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, friend.getUserId());
			// 通知界面更新
			MsgBroadcast.broadcastMsgNumReset(mService);
			MsgBroadcast.broadcastMsgUiUpdate(mService);
			CardcastUiUpdateUtil.broadcastUpdateUi(mService);
			MucgroupUpdateUtil.broadcastUpdateUi(mService);
			ListenerManager.getInstance().notifyDeleteMucRoom(friend.getUserId());
		}
			break;
		case XmppMessage.TYPE_DELETE_MEMBER: {// 删除成员
			if (TextUtils.isEmpty(toUserId)) {
				return;
			}

			L_.e("删除删除成员了----------");
			if (toUserId.equals(mLoginUserId)) {// 如果这个成员是我，从这个房间退出
				// 删除这个房间
				FriendDao.getInstance().deleteFriend(mLoginUserId, friend.getUserId());
				// 消息表中删除
				ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, friend.getUserId());
				// 通知界面更新
				MsgBroadcast.broadcastMsgNumReset(mService);
				MsgBroadcast.broadcastMsgUiUpdate(mService);
				CardcastUiUpdateUtil.broadcastUpdateUi(mService);
				ListenerManager.getInstance().notifyMyBeDelete(friend.getUserId());
			} else {// 其他人被房间删除了
				ChatMessage message = new ChatMessage();
				message.setTimeSend(timeSend);
				message.setContent("成员：" + toUserName + " 已退出了房间");
				message.setPacketId(packetId);
				message.setType(XmppMessage.TYPE_TIP);
				if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), message)) {
					ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), message, true);
				}
			}
		}
			break;
		case XmppMessage.TYPE_GAG: {// 禁言
			int time = 0;
			try {
				time = Integer.parseInt(content);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			if (toUserId != null && toUserId.equals(mLoginUserId)) {// 我被禁言了或者取消禁言
				friend.setRoomTalkTime(time);
				FriendDao.getInstance().createOrUpdateFriend(friend);
				ListenerManager.getInstance().notifyMyVoiceBanned(friend.getUserId(), time);
			}
			ChatMessage message = new ChatMessage();
			message.setTimeSend(timeSend);
			if (time > (System.currentTimeMillis()/1000)) {
				message.setContent("用户：" + toUserName + " 已被禁言");
			} else {
				message.setContent("用户：" + toUserName + " 已被取消禁言");
			}
			message.setPacketId(packetId);
			message.setType(XmppMessage.TYPE_TIP);
			if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), message)) {
				ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), message, true);
			}
		}
			break;
		case XmppMessage.NEW_MEMBER:
			// {
			// "type": 907,
			// "objectId": "房间Id",
			// "fromUserId": 邀请人Id,
			// "fromUserName": "邀请人昵称",
			// "toUserId": 新成员Id,
			// "toUserName": "新成员昵称",
			// "timeSend": 123
			// }
			break;
		default:
			break;
		}
	}

	/**
	 * @param messageBody
	 * @param fromUserId
	 *            因为普通聊天消息在传输中，没有带fromUserId，所以要从xmpp报文中取值并赋值（新朋友就不用，它自带了）
	 * @param packetId
	 */
	private void saveChatMessage(String messageBody, String fromUserId, String packetId) {
		Log.d("roamer","开始保存消息啦");
		ChatMessage chatMessage = new ChatMessage(messageBody);
		chatMessage.setFromUserId(fromUserId);
		if (TextUtils.isEmpty(packetId)) {
			chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
		} else {
			chatMessage.setPacketId(packetId);
		}

		Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, fromUserId);
		// if (friend == null || friend.getStatus() == Friend.STATUS_BLACKLIST) {
		// return;// 不是我的好友，或者在黑名单中，直接返回
		// }
		if (friend == null) {// 如果不是朋友，那么直接加为好友
			friend = new Friend();
			friend.setTimeCreate((int) System.currentTimeMillis() / 1000);
			friend.setOwnerId(mLoginUserId);
			friend.setUserId(fromUserId);
			friend.setNickName(chatMessage.getFromUserName());
			friend.setRoomFlag(0);// 0朋友 1群组
			friend.setStatus(2);// 2是互为好友
			friend.setTimeSend(TimeUtils.sk_time_current_time());
			FriendDao.getInstance().createOrUpdateFriend(friend);
		}

		if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage)) {
			ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
		}
	}

	private void saveNewFriendMessage(String fromUserId, String messageBody, String packetId) {
		// 新朋友的消息的处理
		NewFriendMessage newFriend = new NewFriendMessage(messageBody);
		newFriend.setOwnerId(mLoginUserId);
		newFriend.setPacketId(packetId);
		newFriend.setMySend(false);
		newFriend.setRead(false);
		newFriend.setUserId(fromUserId);
		switch (newFriend.getType()) {
		case XmppMessage.TYPE_BLACK:// 我进入了别人的黑名单,那么就不能再去看商务圈
			FriendHelper.beAddBlacklist(newFriend.getOwnerId(), newFriend.getUserId());
			return;
		case XmppMessage.TYPE_DELSEE:// 别人的删除了关注我
			FriendHelper.beDeleteSeeNewFriend(newFriend.getOwnerId(), newFriend.getUserId());
			return;
		case XmppMessage.TYPE_DELALL:// 别人彻底删除了我
			FriendHelper.beDeleteAllNewFriend(newFriend.getOwnerId(), newFriend.getUserId());
			return;
		}

		int status = FriendDao.getInstance().getFriendStatus(newFriend.getOwnerId(), newFriend.getUserId());
		if (status == Friend.STATUS_BLACKLIST) {// 如果是黑名单中，那么下面的那些消息就不用处理了
			return;
		}

		if (status == Friend.STATUS_FRIEND) {// 如果已经是好友了，那么下面的那些消息没有处理的必要了
			return;
		}

		boolean isPreRead = NewFriendDao.getInstance().isNewFriendRead(newFriend);// 之前可能存在的消息读没读,那么未读数量就不再+1
		switch (newFriend.getType()) {
		case XmppMessage.TYPE_NEWSEE:// 别人发的新关注消息
		case XmppMessage.TYPE_SAYHELLO:// 别人发的打招呼 status=STATUS_UNKNOW
			NewFriendDao.getInstance().createOrUpdateNewFriend(newFriend);
			break;
		case XmppMessage.TYPE_PASS:// 通过别人的验证 status=STATUS_FRIEND
		case XmppMessage.TYPE_FRIEND:// 直接成为了好友status=STATUS_FRIEND
			NewFriendDao.getInstance().ascensionNewFriend(newFriend, Friend.STATUS_FRIEND);
			FriendHelper.beAddFriendExtraOperation(newFriend.getOwnerId(), newFriend.getUserId());
			break;
		case XmppMessage.TYPE_FEEDBACK:// 别人的回话 status=STATUS_ATTENTION
			NewFriendDao.getInstance().createOrUpdateNewFriend(newFriend);
			break;
		case XmppMessage.TYPE_RECOMMEND:// 新推荐好友 TODO
			break;
		default:
			break;
		}
		// 更新朋友表中 100001号未读数量和最后一条的消息
		FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, new ChatMessage(messageBody));
		ListenerManager.getInstance().notifyNewFriend(mLoginUserId, newFriend, isPreRead);
	}

}

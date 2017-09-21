package com.lsjr.zizi.chat.xmpp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.chat.bean.MucRoomSimple;
import com.lsjr.zizi.chat.broad.CardcastUiUpdateUtil;
import com.lsjr.zizi.chat.dao.ChatMessageDao;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.ChatMessage;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.thread.ThreadManager;
import com.lsjr.zizi.chat.utils.TimeUtils;
import com.lsjr.zizi.chat.xmpp.listener.ChatMessageListener;
import com.lsjr.zizi.chat.xmpp.util.XmppStringUtil;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.SpUtils;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class XMucChatManager {
	private CoreService mService;
	private XMPPConnection mConnection;
	private String mLoginUserId;// 当前登录用户的昵称
	private String mLoginNickName;// 当前登录用户的昵称
	private Map<String, MultiUserChat> mMucChatMap;// 存储所有已经加入的聊天室
	private long mJoinTimeOut;
    private Context context;
	public XMucChatManager(CoreService service, XMPPConnection connection) {
		L_.e("roamer","XMucChatManager");
		mService = service;
		mConnection = connection;
		mJoinTimeOut = mConnection.getPacketReplyTimeout();
		mLoginUserId = StringUtils.parseName(mConnection.getUser());
		mLoginNickName = ConfigApplication.instance().mLoginUser.getNickName();
		mMucChatMap = new HashMap<>();

		mConnection.addPacketListener(packetListener, packetFilter);
		joinExistRoom();
		MultiUserChat.addInvitationListener(mConnection, (arg0, arg1, arg2, arg3, arg4, arg5) -> {
            // 受到聊天室的邀请，就将聊天室加入为好友
  		  Log.d("roamer","受到聊天室的邀请，就将聊天室加入为好友");
            String roomJIDPrefix = XmppStringUtil.getRoomJIDPrefix(arg1);
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, roomJIDPrefix);
            MucRoomSimple mucRoomSimple = JSON.parseObject(arg3, MucRoomSimple.class);
            if (mucRoomSimple == null) {
                return;
            }
            /* 将房间作为一个好友存到好友表 */
            if (friend == null) {// 将该房间存到好友表中
                friend = new Friend();// 将房间也存为好友
                friend.setOwnerId(mLoginUserId);
                friend.setUserId(roomJIDPrefix);
                friend.setNickName(mucRoomSimple.getName());
                friend.setDescription(mucRoomSimple.getDesc());
                friend.setRoomFlag(1);
                friend.setStatus(Friend.STATUS_FRIEND);
                friend.setRoomId(mucRoomSimple.getId());
                friend.setRoomCreateUserId(mucRoomSimple.getUserId());
                // timeSend作为取群聊离线消息的标志，所以要在这里设置一个初始值
                friend.setTimeSend(mucRoomSimple.getTimeSend());
                FriendDao.getInstance().createOrUpdateFriend(friend);
                // 更新名片盒（可能需要更新）
                CardcastUiUpdateUtil.broadcastUpdateUi(mService);
            }
            long lastTime = ConfigApplication.instance().mLoginUser.getOfflineTime();
            Log.d("wang","lastTime:"+lastTime);
            if (friend.getTimeSend() > lastTime) {
                lastTime = friend.getTimeSend();
            }
            int lastSeconds = (int) (TimeUtils.sk_time_current_time() - lastTime);
            joinMucChat(roomJIDPrefix, mLoginNickName, lastSeconds);
        });
	}

	public void reset() {
		String userId = StringUtils.parseName(mConnection.getUser());
		mMucChatMap.clear();
		if (!mLoginUserId.equals(userId)) {
			mLoginUserId = userId;
			mLoginNickName = ConfigApplication.instance().mLoginUser.getNickName();
		}
		joinExistRoom();
	}

	public static String getMucChatServiceName(XMPPConnection connection) {
		return "@muc." + connection.getServiceName();
	}

	/**
	 * 
	 * @param myNickName
	 * @param roomName
	 * @param roomSubject
	 *            (no use)
	 * @param roomDesc
	 *            (no use)
	 * @return 返回房间的Id
	 */
	public String createMucRoom(String myNickName, String roomName, String roomSubject, String roomDesc) {
		try {
			String roomId = UUID.randomUUID().toString().replaceAll("-", "");
			String roomJid = roomId + getMucChatServiceName(mConnection);
			Log.d("roamer", "开始创建房间:" + roomJid);
			// 创建聊天室
			MultiUserChat muc = new MultiUserChat(mConnection, roomJid);
			muc.create(myNickName);

			// 获得聊天室的配置表单
			Form form = muc.getConfigurationForm();
			// 根据原始表单创建一个要提交的新表单。
			Form submitForm = form.createAnswerForm();
			// 向要提交的表单添加默认答复

			List<FormField> fields = form.getFields();
			for (int i = 0; i < fields.size(); i++) {
				FormField field = (FormField) fields.get(i);
				if (!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable() != null) {
					// 设置默认值作为答复
					submitForm.setDefaultAnswer(field.getVariable());
				}
			}

			// 设置聊天室的新拥有者
			// List owners = new ArrayList();
			// owners.add("liaonaibo2\\40slook.cc");
			// owners.add("liaonaibo1\\40slook.cc");
			// submitForm.setAnswer("muc#roomconfig_roomowners", owners);

			// 设置聊天室的名字
			submitForm.setAnswer("muc#roomconfig_roomname", roomName);
			// 设置聊天室描述
			// if (!TextUtils.isEmpty(roomDesc)) {
			// submitForm.setAnswer("muc#roomconfig_roomdesc", roomDesc);
			// }
			// 登录房间对话
			submitForm.setAnswer("muc#roomconfig_enablelogging", true);
			// 允许修改主题
			// submitForm.setAnswer("muc#roomconfig_changesubject", true);
			// 允许占有者邀请其他人
			// submitForm.setAnswer("muc#roomconfig_allowinvites", true);
			// 最大人数
			// List<String> maxusers = new ArrayList<String>();
			// maxusers.add("50");
			// submitForm.setAnswer("muc#roomconfig_maxusers", maxusers);
			// 公开的，允许被搜索到
			// submitForm.setAnswer("muc#roomconfig_publicroom", true);
			// 设置聊天室是持久聊天室，即将要被保存下来
			submitForm.setAnswer("muc#roomconfig_persistentroom", true);

			// 是否主持腾出空间(加了这个默认游客进去不能发言)
			// submitForm.setAnswer("muc#roomconfig_moderatedroom", true);
			// 房间仅对成员开放
			// submitForm.setAnswer("muc#roomconfig_membersonly", true);
			// 不需要密码
			// submitForm.setAnswer("muc#roomconfig_passwordprotectedroom",
			// false);
			// 房间密码
			// submitForm.setAnswer("muc#roomconfig_roomsecret", "111");
			// 允许主持 能够发现真实 JID
			// List<String> whois = new ArrayList<String>();
			// whois.add("anyone");
			// submitForm.setAnswer("muc#roomconfig_whois", whois);

			// 管理员
			// <field var='muc#roomconfig_roomadmins'>
			// <value>wiccarocks@shakespeare.lit</value>
			// <value>hecate@shakespeare.lit</value>
			// </field>

			// 仅允许注册的昵称登录
			// submitForm.setAnswer("x-muc#roomconfig_reservednick", true);
			// 允许使用者修改昵称
			// submitForm.setAnswer("x-muc#roomconfig_canchangenick", false);
			// 允许用户注册房间
			// submitForm.setAnswer("x-muc#roomconfig_registration", false);
			// 发送已完成的表单（有默认值）到服务器来配置聊天室
			muc.sendConfigurationForm(submitForm);

			// muc.changeSubject(roomSubject);
			// mMucChatMap.put(roomJid, muc);
			mMucChatMap.put(roomJid, muc);
			return roomId;
		} catch (XMPPException e) {
			e.printStackTrace();
		} catch (NoResponseException e) {
			e.printStackTrace();
		} catch (SmackException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 邀请好友进入房间
	 * 
	 * @param roomId
	 * @param userId
	 * @param reason
	 */
	public void invite(String roomId, String userId, String reason) {
		String roomJid = roomId + getMucChatServiceName(mConnection);
		if (mMucChatMap.get(roomJid) != null) {
			try {
				mMucChatMap.get(roomJid).invite(userId + "@" + mConnection.getServiceName(), reason);
			} catch (NotConnectedException e) {
				e.printStackTrace();
			}
		}
	}

	public MultiUserChat getRoom(String roomJid) {
		return mMucChatMap.get(roomJid);
	}

	/**
	 * @param toUserId
	 *            要发送消息的房间Id
	 * @param chatMessage
	 *            已经存到本地数据库的一条即将发送的消息
	 */
	public void sendMessage(final String toUserId, final ChatMessage chatMessage) {
		ThreadManager.getPool().execute(new Runnable() {
			public void run() {
				String roomJid = toUserId + getMucChatServiceName(mConnection);
				MultiUserChat chat = getRoom(roomJid);
				if (chat == null || !chat.isJoined()) {
					ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.get_id(),
							ChatMessageListener.MESSAGE_SEND_FAILED);
					return;
				}
				Message msg = new Message();
				msg.setType(Message.Type.groupchat);
				msg.setBody(chatMessage.toJsonString(true));
				msg.setPacketID(chatMessage.getPacketId());
				msg.setTo(roomJid);
				DeliveryReceiptManager.addDeliveryReceiptRequest(msg);

				int sendStatus = ChatMessageListener.MESSAGE_SEND_FAILED;
				// 发送消息
				try {
					chat.sendMessage(msg);
					sendStatus = ChatMessageListener.MESSAGE_SEND_ING;
				} catch (NotConnectedException e) {
					e.printStackTrace();
				} catch (XMPPException e) {
					e.printStackTrace();
				}
				ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.get_id(), sendStatus);
			}
		});
	}

	public void joinMucChat(final String toUserId, String nickName, int lastSeconds) {
		String roomJid = toUserId + getMucChatServiceName(mConnection);
		if (mMucChatMap.containsKey(roomJid)) {
			MultiUserChat mucChat = mMucChatMap.get(roomJid);
			if (mucChat != null && mucChat.isJoined()) {
				return;
			}
		}
		final MultiUserChat mucChat = new MultiUserChat(mConnection, roomJid);
		try {
			mMucChatMap.put(roomJid, mucChat);
			DiscussionHistory history = new DiscussionHistory();
			if (lastSeconds > 0) {
				history.setSeconds(lastSeconds - 1);// 减去1秒，防止最后一条消息重复（当然有可能导致在这个时间点的其他消息丢失，不会概率极小）
				// history.setSince(new Date(new Date().getTime() - 300 * 1000));
			} else {
				history.setSeconds(0);// request no history
			}
			mucChat.join(nickName, null, history, mJoinTimeOut);// 必须放在后面，要不然取mMucNickNameMap得时候肯呢过为空，因为这是个异步的
		} catch (XMPPException e) {
			e.printStackTrace();
		} catch (NoResponseException e) {
			e.printStackTrace();
		} catch (NotConnectedException e) {
			e.printStackTrace();
		}
	}

	public void exitMucChat(String toUserId) {
		String roomJid = toUserId + getMucChatServiceName(mConnection);
		if (mMucChatMap.containsKey(roomJid)) {
			MultiUserChat mucChat = mMucChatMap.get(roomJid);
			if (mucChat != null && mucChat.isJoined()) {
				try {
					mucChat.leave();
				} catch (NotConnectedException e) {
					e.printStackTrace();
				}
				mMucChatMap.remove(roomJid);
			}
		}
	}

	PacketListener packetListener = new PacketListener() {
		@Override
		public void processPacket(Packet arg0) throws NotConnectedException {
			Message message = (Message) arg0;
			String from = message.getFrom();
			String to = message.getTo();
           Log.d("wang","messageId::"+message.getBody());
			if (TextUtils.isEmpty(from) || TextUtils.isEmpty(to)) {
				return;
			}
			if (!XmppStringUtil.isJID(from) || !XmppStringUtil.isJID(to)) {
				return;
			}
			if (!StringUtils.parseName(to).equals(mLoginUserId)) {// 不是发给我的，基本上是不可能的情况，还是麻痹的判断下
				return;
			}
			String content = message.getBody();
			int changeTimeSend = 0;// 如果是历史记录，那么要篡改Json数据中的TimeSend字段，防止多次加入放假获取重复的历史记录
			//下面这样写会在某些机型上重复收到消息
			DelayInformation delayInformation = (DelayInformation) message.getExtension("x", "jabber:x:delay");
			if (delayInformation != null) {// 这是历史记录
				Log.d("roamer1","这是历史记录........"+message.getBody()+"delay:"+delayInformation.getStamp().getTime());
				Date date = delayInformation.getStamp();
				if (date != null) {
					changeTimeSend = (int) (date.getTime() / 1000);
					saveGroupMessage(content, false, from, message.getPacketID(),changeTimeSend);
					return;
				}

			}
			Log.d("roamer1","........messageBody:"+message.getBody());
			saveGroupMessage(content, false, from, message.getPacketID(),changeTimeSend);
		}
	};

	PacketFilter packetFilter = new PacketFilter() {
		@Override
		public boolean accept(Packet arg0) {
			if (arg0 instanceof Message) {
				Message message = (Message) arg0;
				if (message.getType() == Type.groupchat) {
					return true;
				}
				return false;
			} else {
				return false;
			}
		}
	};

	// 自动加入到以前所有已经加入的房间
	private void joinExistRoom() {
		new Thread(() -> {
//				long lastTime = MyApplication.getInstance().mLoginUser.getOfflineTime();
            long lastTime= SpUtils.getInstance().getLong(mService, Constants.OFFLINE_TIME);
            if(lastTime==1){
                lastTime = ConfigApplication.instance().mLoginUser.getOfflineTime();
            }
            Log.d("wang","joinExistRoom:lasttime::"+lastTime);
            List<Friend> friends = FriendDao.getInstance().getAllRooms(mLoginUserId);
            if (friends == null) {
                return;
            }
            for (int i = 0; i < friends.size(); i++) {
                String roomJid = friends.get(i).getUserId();
                if (friends.get(i).getTimeSend() > lastTime) {
                    lastTime = friends.get(i).getTimeSend();
                }
                int lastSeconds = (int) (TimeUtils.sk_time_current_time() - lastTime);
                Log.e("roamer", "RoomJid:" + roomJid + "     RoomName:" + friends.get(i).getNickName() + "   lastSeconds:" + lastSeconds);
                String nickName = friends.get(i).getRoomMyNickName();
                if (TextUtils.isEmpty(nickName)) {
                    nickName = mLoginNickName;
                }
                joinMucChat(roomJid, mLoginNickName, lastSeconds);
            }
        }).start();
	}

	/**
	 * 保存接收到的聊天信息(群聊)
	 * 
	 * @param messageBody
	 * @param isRead
	 * @return
	 */
	private void saveGroupMessage(String messageBody, boolean isRead, String from, String packetId, int changeTimeSend) {
		String roomJid = XmppStringUtil.getRoomJID(from);
		String myNickName = mMucChatMap.get(roomJid).getNickname();
		String fromUserNick = XmppStringUtil.getRoomUserNick(from);
		if (TextUtils.isEmpty(fromUserNick)) {// 来自于系统的消息
			//
		} else if (fromUserNick.equals(myNickName)) {// 我自己发的消息，不处理
			//
		} else {
			ChatMessage message = new ChatMessage(messageBody);
			if (!message.validate()) {
				return;
			}
			if (TextUtils.isEmpty(packetId)) {
				packetId = UUID.randomUUID().toString().replaceAll("-", "");
			}
			message.setPacketId(packetId);
			if (changeTimeSend > 0) {// 始终以延迟消息的时间戳为准
				Log.e("roamer", "TimeSend:" + message.getTimeSend());
				Log.e("roamer", "TimeStamp:" + changeTimeSend);
				message.setTimeSend(changeTimeSend);
			}
			if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, XmppStringUtil.getRoomJIDPrefix(from), message)) {
				ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, XmppStringUtil.getRoomJIDPrefix(roomJid), message, true);
			}
		}
	}

	// 踢人
	public boolean kickParticipant(String roomJid, String nickName) {
		roomJid = roomJid + getMucChatServiceName(mConnection);
		MultiUserChat mucChat = mMucChatMap.get(roomJid);
		if (mucChat == null) {
			return false;
		}
		try {
			mucChat.kickParticipant(nickName, "你被踢出该房间");
		} catch (XMPPException e) {
			e.printStackTrace();
			return false;
		} catch (NoResponseException e) {
			e.printStackTrace();
			return false;
		} catch (NotConnectedException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	// 被踢出
	public boolean kicked(String roomJid) {
		MultiUserChat mucChat = mMucChatMap.get(roomJid);
		if (mucChat == null) {
			return false;
		}
		try {
			mucChat.leave();
		} catch (NotConnectedException e) {
			e.printStackTrace();
		}
		mMucChatMap.remove(roomJid);
		return true;
	}

}

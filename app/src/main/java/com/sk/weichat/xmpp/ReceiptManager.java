package com.sk.weichat.xmpp;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.NewFriendMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.xmpp.listener.ChatMessageListener;
/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.xmpp
 * @作者:王阳
 * @创建时间: 2015年10月15日 下午5:04:34
 * @描述: 消息回执的处理
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class ReceiptManager {
	/**
	 * 发送的消息类型，用于类型判断和消息回执的分发
	 * 
	 * 
	 */
	public static enum SendType {
		NORMAL, PUSH_NEW_FRIEND
	}

	private XMPPConnection mConnection;
	private DeliveryReceiptManager mDeliveryReceiptManager;

	private String mLoginUserId;// 用于切换用户后，判断是否清除回执内容

	public ReceiptManager(XMPPConnection connection) {
		mConnection = connection;
		mLoginUserId = StringUtils.parseName(mConnection.getUser());
		mDeliveryReceiptManager = DeliveryReceiptManager.getInstanceFor(mConnection);
		mDeliveryReceiptManager.setAutoReceiptsEnabled(true);
		mDeliveryReceiptManager.addReceiptReceivedListener(mReceiptReceivedListener);
	} 

	public void reset() {
		String userId = StringUtils.parseName(mConnection.getUser());
		if (!mLoginUserId.equals(userId)) {
			mLoginUserId = userId;
			mReceiptMapHandler.removeCallbacksAndMessages(null);
			mReceiptMap.clear();
		}
	}

	ReceiptReceivedListener mReceiptReceivedListener = new ReceiptReceivedListener() {
		@Override
		public void onReceiptReceived(String fromJid, String toJid, String receiptId) {
			Log.d("roamer", "收到消息回执:fromJid=" + fromJid + "----toJid=" + toJid + "----receiptId=" + receiptId);
			mReceiptMapHandler.removeMessages(RECEIPT_NO, receiptId);
			android.os.Message handlerMsg = mReceiptMapHandler.obtainMessage(RECEIPT_YES);
			handlerMsg.obj = receiptId;
			mReceiptMapHandler.sendMessage(handlerMsg);
		}
	};

	/* 添加一个即将发送的消息 */
	public void addWillSendMessage(final String toUserId, final XmppMessage xmppMessage, SendType sendType) {
		// 将之前可能存在的回执缓存清除掉
		if (mReceiptMap.containsKey(xmppMessage.getPacketId())) {
			ReceiptObj oldObj = mReceiptMap.get(xmppMessage.getPacketId());
			mReceiptMapHandler.removeMessages(RECEIPT_NO, oldObj);
			mReceiptMap.remove(xmppMessage.getPacketId());
		}
		// 将这个回执对象缓存起来，这样在接到回执的时候容易定位到是发给谁的哪条消息
		ReceiptObj obj = new ReceiptObj();
		obj.toUserId = toUserId;
		obj.msg = xmppMessage;
		obj.sendType = sendType;
		mReceiptMap.put(xmppMessage.getPacketId(), obj);

		android.os.Message handlerMsg = mReceiptMapHandler.obtainMessage(RECEIPT_NO);
		handlerMsg.obj = xmppMessage.getPacketId();
		mReceiptMapHandler.sendMessageDelayed(handlerMsg, MESSAGE_DELAY);
	}

	public static final int MESSAGE_DELAY = 20 * 1000;

	/** 处理消息回执 */
	private Map<String, ReceiptObj> mReceiptMap = new HashMap<String, ReceiptObj>();

	class ReceiptObj {
		String toUserId;// 普通消息和新朋友消息公用
		XmppMessage msg;// 用于普通消息和新朋友消息公用
		SendType sendType;// 用于分发普通消息和新朋友消息的回执
	}

	private static final int RECEIPT_NO = 0x1;// 没有收到回执
	private static final int RECEIPT_YES = 0x2;// 收到回执
	private Handler mReceiptMapHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			String packetId = (String) msg.obj;
			if (TextUtils.isEmpty(packetId)) {
				return;
			}
			ReceiptObj obj = mReceiptMap.get(packetId);
			if (obj == null || obj.msg == null) {
				return;
			}
			if (msg.what == RECEIPT_NO) {// 认为这条消息未发送成功
				Log.d("roamer","认为这条消息...未发送成功");
				if (obj.sendType == SendType.NORMAL) {
					ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, obj.toUserId, ((ChatMessage) obj.msg).get_id(),
							ChatMessageListener.MESSAGE_SEND_FAILED);
				} else {
					ListenerManager.getInstance().notifyNewFriendSendStateChange(obj.toUserId, ((NewFriendMessage) obj.msg),
							ChatMessageListener.MESSAGE_SEND_FAILED);
				}
			} else if (msg.what == RECEIPT_YES) {// 认为发送成功
				Log.d("roamer","认为这条消息...发送成功");
				if (obj.sendType == SendType.NORMAL) {
					Log.d("roamer","SendType.NORMAL");
					ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, obj.toUserId, ((ChatMessage) obj.msg).get_id(),
							ChatMessageListener.MESSAGE_SEND_SUCCESS);
				} else {
					Log.d("roamer","SendType....");
					ListenerManager.getInstance().notifyNewFriendSendStateChange(obj.toUserId, ((NewFriendMessage) obj.msg),
							ChatMessageListener.MESSAGE_SEND_SUCCESS);
				}
			}
			mReceiptMap.remove(packetId);
		}
	};

}

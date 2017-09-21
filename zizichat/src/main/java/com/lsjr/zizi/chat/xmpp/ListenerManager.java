package com.lsjr.zizi.chat.xmpp;

import android.os.Handler;
import android.os.Looper;

import com.lsjr.zizi.chat.broad.MsgBroadcast;
import com.lsjr.zizi.chat.dao.ChatMessageDao;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.ChatMessage;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.db.NewFriendMessage;
import com.lsjr.zizi.chat.xmpp.listener.AuthStateListener;
import com.lsjr.zizi.chat.xmpp.listener.ChatMessageListener;
import com.lsjr.zizi.chat.xmpp.listener.MucListener;
import com.lsjr.zizi.chat.xmpp.listener.NewFriendListener;
import com.ymz.baselibrary.BaseApplication;
import com.ymz.baselibrary.utils.L_;

import java.util.ArrayList;
import java.util.List;

/**
 * 
  * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.xmpp
 * @作者:王阳
 * @创建时间: 2015年10月10日 上午11:42:56
 * @描述: TODO
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class ListenerManager {
	/* 回调监听 */
	private List<ChatMessageListener> mChatMessageListeners = new ArrayList<ChatMessageListener>();
	private List<AuthStateListener> mAuthStateListeners = new ArrayList<AuthStateListener>();
	private List<MucListener> mMucListeners = new ArrayList<MucListener>();
	private List<NewFriendListener> mNewFriendListeners = new ArrayList<NewFriendListener>();

	private static ListenerManager instance;

	private ListenerManager() {
	}

	public static final ListenerManager getInstance() {
		if (instance == null) {
			instance = new ListenerManager();
		}
		return instance;
	}

	public void reset() {
		instance = null;
	}

	/********************** 注册和移除监听 **************************/
	public void addChatMessageListener(ChatMessageListener messageListener) {
		mChatMessageListeners.add(messageListener);
	}

	public void removeChatMessageListener(ChatMessageListener messageListener) {
		mChatMessageListeners.remove(messageListener);
	}

	public void addAuthStateChangeListener(AuthStateListener authStateChangeListener) {
		mAuthStateListeners.add(authStateChangeListener);
	}

	public void removeAuthStateChangeListener(AuthStateListener authStateChangeListener) {
		mAuthStateListeners.remove(authStateChangeListener);
	}

	public void addMucListener(MucListener listener) {
		mMucListeners.add(listener);
	}

	public void removeMucListener(MucListener listener) {
		mMucListeners.remove(listener);
	}

	public void addNewFriendListener(NewFriendListener listener) {
		mNewFriendListeners.add(listener);
	}

	public void removeNewFriendListener(NewFriendListener listener) {
		mNewFriendListeners.remove(listener);
	}

	private Handler mHandler = new Handler(Looper.getMainLooper());

	/********************** 监听回调 **************************/
	public void notifyAuthStateChange(final int authState) {
		if (mAuthStateListeners.size() <= 0) {
			return;
		}
		mHandler.post(new Runnable() {
			public void run() {
				for (AuthStateListener authStateChangeListener : mAuthStateListeners) {
					authStateChangeListener.onAuthStateChange(authState);
				}
			}
		});
	}
/**
 * 通知一条新消息到来
 * @param loginUserId
 * @param fromUserId
 * @param message
 * @param isGroupMsg
 */
	public void notifyNewMesssage(final String loginUserId, final String fromUserId, final ChatMessage message, final boolean isGroupMsg) {
		mHandler.post(new Runnable() {
			public void run() {
				if (message != null) {
					L_.e("roamer","新消息到来  loginUserId"+loginUserId+"--->"+fromUserId);
					boolean hasRead = false;
					for (int i = mChatMessageListeners.size() - 1; i >= 0; i--) {
						hasRead = mChatMessageListeners.get(i).onNewMessage(fromUserId, message, isGroupMsg);
					}
					if (!hasRead) {
						FriendDao.getInstance().markUserMessageUnRead(loginUserId, fromUserId);
						MsgBroadcast.broadcastMsgNumUpdate(BaseApplication.getApplication(), true, 1);
					}
					MsgBroadcast.broadcastMsgUiUpdate(BaseApplication.getApplication());
				}
			}
		});
	}

	public void notifyMessageSendStateChange(String loginUserId, String toUserId, final int msgId, final int messageState) {
		if (mChatMessageListeners.size() <= 0) {
			return;
		}
		ChatMessageDao.getInstance().updateMessageSendState(loginUserId, toUserId, msgId, messageState);
		mHandler.post(new Runnable() {
			public void run() {
				for (ChatMessageListener listener : mChatMessageListeners) {
					listener.onMessageSendStateChange(messageState, msgId);
				}
			}
		});
	}

	/**
	 * 新朋友消息
	 */
	public void notifyNewFriend(final String loginUserId, final NewFriendMessage message, final boolean isPreRead) {
		mHandler.post(new Runnable() {
			public void run() {
				boolean hasRead = false;// 是否已经被读了
				for (NewFriendListener listener : mNewFriendListeners) {
					if (listener.onNewFriend(message)) {
						hasRead = true;
					}
				}
				if (!hasRead && isPreRead) {
					FriendDao.getInstance().markUserMessageUnRead(loginUserId, Friend.ID_NEW_FRIEND_MESSAGE);
					MsgBroadcast.broadcastMsgNumUpdate(BaseApplication.getApplication(), true, 1);
				}
				MsgBroadcast.broadcastMsgUiUpdate(BaseApplication.getApplication());
			}
		});
	}

	/**
	 * 新朋友发送消息的状态变化
	 */
	public void notifyNewFriendSendStateChange(final String toUserId, final NewFriendMessage message, final int messageState) {
		if (mNewFriendListeners.size() <= 0) {
			return;
		}
		mHandler.post(new Runnable() {
			public void run() {
				for (NewFriendListener listener : mNewFriendListeners) {
					listener.onNewFriendSendStateChange(toUserId, message, messageState);
				}
			}
		});
	}

	// ////////////////////Muc Listener//////////////////////
	public void notifyDeleteMucRoom(final String toUserId) {
		if (mMucListeners.size() <= 0) {
			return;
		}
		mHandler.post(new Runnable() {
			public void run() {
				for (MucListener listener : mMucListeners) {
					listener.onDeleteMucRoom(toUserId);
				}
			}
		});
	}

	public void notifyMyBeDelete(final String toUserId) {
		if (mMucListeners.size() <= 0) {
			return;
		}
		mHandler.post(new Runnable() {
			public void run() {
				for (MucListener listener : mMucListeners) {
					listener.onMyBeDelete(toUserId);
				}
			}
		});
	}

	public void notifyNickNameChanged(final String toUserId, final String changedUserId, final String changedName) {
		if (mMucListeners.size() <= 0) {
			return;
		}
		mHandler.post(new Runnable() {
			public void run() {
				for (MucListener listener : mMucListeners) {
					listener.onNickNameChange(toUserId, changedUserId, changedName);
				}
			}
		});
	}

	public void notifyMyVoiceBanned(final String toUserId, final int time) {
		if (mMucListeners.size() <= 0) {
			return;
		}
		mHandler.post(new Runnable() {
			public void run() {
				for (MucListener listener : mMucListeners) {
					listener.onMyVoiceBanned(toUserId, time);
				}
			}
		});
	}
}

package com.lsjr.zizi.chat.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.Dao.CreateOrUpdateStatus;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.AttentionUser;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.bean.MucRoomMember;
import com.lsjr.zizi.chat.db.ChatMessage;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.db.NewFriendMessage;
import com.lsjr.zizi.chat.helper.SQLiteHelper;
import com.lsjr.zizi.chat.helper.SQLiteRawUtil;
import com.lsjr.zizi.chat.helper.TableVersionSp;

import com.lsjr.zizi.chat.inter.OnCompleteListener;
import com.lsjr.zizi.chat.utils.TimeUtils;
import com.lsjr.zizi.chat.xmpp.XmppMessage;
import com.lsjr.zizi.chat.xmpp.listener.ChatMessageListener;
import com.ymz.baselibrary.BaseApplication;


/**
 * 访问朋友数据的Dao
 *
 *
 */
public class FriendDao {
	private static FriendDao instance = null;

	public static FriendDao getInstance() {
		if (instance == null) {
			synchronized (FriendDao.class) {
				if (instance == null) {
					instance = new FriendDao();
				}
			}
		}
		return instance;
	}

	private Dao<Friend, Integer> friendDao;

	private SQLiteHelper mHelper;

	private FriendDao() {
		try {
			mHelper = OpenHelperManager.getHelper(BaseApplication.getApplication(), SQLiteHelper.class);
			friendDao = DaoManager.createDao(mHelper.getConnectionSource(), Friend.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		OpenHelperManager.releaseHelper();
	}

	/**
	 * 检测4个系统号是否存在
	 *
	 * @param ownerId
	 */
	public void checkSystemFriend(String ownerId) {
		try {
			List<Friend> friendsList = friendDao.queryForEq("ownerId", ownerId);
			if (friendsList != null && friendsList.size() > 0) {// 说明不是第一次创建，直接返回
				return;
			}

			Friend friend = new Friend();
			friend.setOwnerId(ownerId);
			// 系统消息10000号
			friend.setUserId(Friend.ID_SYSTEM_MESSAGE);
			friend.setNickName(Friend.NICKNAME_SYSTEM_MESSAGE);
			friend.setStatus(Friend.STATUS_SYSTEM);
			friendsList = friendDao.queryForMatching(friend);
			if (friendsList == null || friendsList.size() <= 0) {// 添加这个系统消息10000号
				friendDao.create(friend);
				// 添加一条系统提示
				ChatMessage chatMessage = new ChatMessage();
				chatMessage.setType(XmppMessage.TYPE_TIP);
				chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
				chatMessage.setFromUserId(Friend.ID_SYSTEM_MESSAGE);
				chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
				// 为了使得初始生成的系统消息排在新朋友前面，所以在时间节点上延迟一点 1s
				chatMessage.setTimeSend(TimeUtils.sk_time_current_time() + 1);
				chatMessage.setContent(BaseApplication.getApplication().getString(R.string.welcome_user_software));
				chatMessage.setMySend(false);// 表示不是自己发的
				// 往消息表里插入一条记录
				ChatMessageDao.getInstance().saveNewSingleChatMessage(ownerId, Friend.ID_SYSTEM_MESSAGE, chatMessage);
				// 往朋友表里面插入一条未读记录
				//markUserMessageUnRead(ownerId, Friend.ID_SYSTEM_MESSAGE);
				// 更新消息记录
				//updateLastChatMessage(ownerId, Friend.ID_SYSTEM_MESSAGE, chatMessage);
			}

//			// 新朋友消息10001号
//			friend.setUserId(Friend.ID_NEW_FRIEND_MESSAGE);
//			friend.setNickName(Friend.NICKNAME_NEW_FRIEND_MESSAGE);
//			friend.setStatus(Friend.STATUS_SYSTEM);
//			friendsList = friendDao.queryForMatching(friend);
//			if (friendsList == null || friendsList.size() <= 0) {// 添加这个新朋友消息10001号
//				friendDao.create(friend);
//				// 添加一条新朋友提示，更新到好友表中
//				ChatMessage chatMessage = new ChatMessage();
//				chatMessage.setType(XmppMessage.TYPE_TIP);
//				chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
//				chatMessage.setFromUserId(Friend.ID_NEW_FRIEND_MESSAGE);
//				chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
//				chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
//				chatMessage.setContent("");
//				chatMessage.setMySend(false);// 表示不是自己发的
//				// 更新消息记录
//				updateLastChatMessage(ownerId, Friend.ID_NEW_FRIEND_MESSAGE, chatMessage);
//			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 更新与某个好友的阅读状态为已读
	 *
	 * @param friendId
	 * @param ownerId
	 */
	public void markUserMessageRead(String ownerId, String friendId) {
		UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
		try {
			Log.d("roamer","........标为已读.......");
			builder.updateColumnValue("unReadNum", 0);
			builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
			friendDao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 更新某个好友的阅读状态，+1条未读信息
	 *
	 * @return
	 */
	public void markUserMessageUnRead(String ownerId, String friendId) {
		try {
			PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where().eq("ownerId", ownerId).and().eq("userId", friendId).prepare();
			List<Friend> friendsList = friendDao.query(preparedQuery);
			if (friendsList != null && friendsList.size() > 0) {
				Friend friend = friendsList.get(0);
				int unReadCount = friend.getUnReadNum();
				friend.setUnReadNum(++unReadCount);
				friendDao.update(friend);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/* 获取消息模块未读数量总和 */
	public int getMsgUnReadNumTotal(String ownerId) {
		try {
			QueryBuilder<Friend, Integer> builder = friendDao.queryBuilder();
			builder.selectRaw("sum(unReadNum)");
			// 条件 好友status2，显示系统号8
			builder.where().eq("ownerId", ownerId).and().ge("status", 2).and().le("status", 8);
			GenericRawResults<String[]> results = friendDao.queryRaw(builder.prepareStatementString());
			if (results != null) {
				String[] first = results.getFirstResult();
				if (first != null && first.length > 0) {
					return Integer.parseInt(first[0]);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 分页查询最近聊天的好友
	 *
	 * @return
	 */
	@Deprecated
	public List<Friend> getNearlyFriendMsg(String ownerId, int pageIndex, int pageSize) {
		List<Friend> friends = null;
		try {
			// 过滤条件，content不为空，status ==2（好友） status==8（显示系统号）
			PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().orderBy("timeSend", false).limit((long) pageSize)
					.offset((long) pageSize * pageIndex).where().eq("ownerId", ownerId).and().isNotNull("content").and().ge("status", 2).and()
					.le("status", 8).prepare();
			friends = friendDao.query(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return friends;
	}

	/**
	 * 查询所有最近聊天的好友
	 *
	 * @return
	 */
	public List<Friend> getNearlyFriendMsg(String ownerId) {
		List<Friend> friends = null;
		try {
			// 过滤条件，content不为空，status ==2（好友） status==8（显示系统号）
			PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().orderBy("timeSend", false).where().eq("ownerId", ownerId).and()
					.isNotNull("content").and().ge("status", Friend.STATUS_FRIEND).and().le("status", Friend.STATUS_SYSTEM).prepare();
			friends = friendDao.query(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return friends;
	}

	/**
	 * 创建或者更新好友
	 *
	 * @param friend
	 * @return
	 */
	public boolean createOrUpdateFriend(Friend friend) {
		try {
			CreateOrUpdateStatus status = friendDao.createOrUpdate(friend);
			return status.isCreated() || status.isUpdated();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 新建立的朋友关系，那么在朋友表中加入这个新朋友，并提示新朋友
	 */
	public boolean addNewFriendInMsgTable(String loginUserId, String friendId) {
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setType(XmppMessage.TYPE_TIP);
		chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
		chatMessage.setFromUserId(friendId);
		chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
		chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
		chatMessage.setContent("你们已经是好友了，开始聊天吧");
		chatMessage.setMySend(false);// 表示不是自己发的
		// 往消息表里插入一条记录
		ChatMessageDao.getInstance().saveNewSingleChatMessage(loginUserId, friendId, chatMessage);
		// 往朋友表里面插入一条未读记录
		markUserMessageUnRead(loginUserId, friendId);
		return true;
	}

	/**
	 * 获取备注名
	 *
	 * @param ownerId
	 * @param userId
	 * @return
	 */
	public String getRemarkName(String ownerId, String userId) {
		QueryBuilder<Friend, Integer> builder = friendDao.queryBuilder();
		builder.selectRaw("remarkName");
		try {
			builder.where().eq("ownerId", ownerId).and().eq("userId", userId);
			GenericRawResults<String[]> results = friendDao.queryRaw(builder.prepareStatementString());
			if (results != null) {
				String[] first = results.getFirstResult();
				if (first != null && first.length > 0) {
					return first[0];
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "";

	}

	/**
	 * 设置备注名
	 *
	 * @param loginUserId
	 * @param userId
	 * @return
	 */
	public void setRemarkName(String loginUserId, String userId, String remarkName) {
		UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
		try {
			builder.updateColumnValue("remarkName", remarkName);
			builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
			friendDao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param ownerId
	 * @param friendId
	 * @return
	 */
	public Friend getFriend(String ownerId, String friendId) {
		try {
			PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where().eq("ownerId", ownerId).and().eq("userId", friendId).prepare();
			Friend existFriend = friendDao.queryForFirst(preparedQuery);
			return existFriend;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取好友的关系
	 *
	 * @param ownerId
	 * @param friendId
	 * @return
	 */
	public int getFriendStatus(String ownerId, String friendId) {
		QueryBuilder<Friend, Integer> builder = friendDao.queryBuilder();
		try {
			builder.selectColumns("status");
			builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
			GenericRawResults<String[]> results = friendDao.queryRaw(builder.prepareStatementString());
			if (results != null) {
				String[] first = results.getFirstResult();
				if (first != null && first.length > 0) {
					return Integer.parseInt(first[0]);
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return Friend.STATUS_UNKNOW;
	}

	/**
	 *
	 * @param ownerId
	 * @param friendId
	 * @return
	 */
	public void deleteFriend(String ownerId, String friendId) {
		try {
			DeleteBuilder<Friend, Integer> builder = friendDao.deleteBuilder();
			builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
			friendDao.delete(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// ////////////////
	/**
	 * 更新朋友表里面的最后一条未读信息
	 *
	 * @param friendId
	 * @param ownerId
	 * @param message
	 */
	public void updateLastChatMessage(String ownerId, String friendId, ChatMessage message) {
		Context context = BaseApplication.getApplication();
		String content = "";
		int type = message.getType();
		if (type == XmppMessage.TYPE_TEXT) {
			content = message.getContent();
		} else if (type == XmppMessage.TYPE_IMAGE) {
			content = context.getString(R.string.msg_picture);
		} else if (type == XmppMessage.TYPE_VOICE) {
			content = context.getString(R.string.msg_voice);
		} else if (type == XmppMessage.TYPE_LOCATION) {
			content = context.getString(R.string.msg_location);
		} else if (type == XmppMessage.TYPE_GIF) {
			content = context.getString(R.string.msg_animation);
		} else if (type == XmppMessage.TYPE_VIDEO) {
			content = context.getString(R.string.msg_video);
		}else if (type == XmppMessage.TYPE_FILE){
			content = context.getString(R.string.msg_file);
		} else if (type == XmppMessage.TYPE_TIP) {
			content = message.getContent();
		} else if (type == XmppMessage.TYPE_NEWSEE) {// 新关注提示
			if (!message.isMySend()) {
				content = context.getString(R.string.msg_be_add_attention);
			}
		} else if (type == XmppMessage.TYPE_SAYHELLO) {// 打招呼提示
			if (!message.isMySend()) {
				if (TextUtils.isEmpty(message.getContent())) {
					content = context.getString(R.string.msg_be_say_hello);
				} else {
					content = message.getContent();
				}
			}
		} else if (type == XmppMessage.TYPE_PASS) {// 验证通过提示
			if (!message.isMySend()) {
				content = context.getString(R.string.msg_be_passed);
			}
		} else if (type == XmppMessage.TYPE_FRIEND) {// 新朋友提示
			if (!message.isMySend()) {
				content = context.getString(R.string.msg_has_new_friend);
			}
		} else if (type == XmppMessage.TYPE_FEEDBACK) {// 回话
			if (!message.isMySend()) {
				if (!TextUtils.isEmpty(message.getContent())) {
					content = message.getContent();
				}
			}
		} else if (type == XmppMessage.TYPE_RECOMMEND) {
			content = context.getString(R.string.msg_has_new_recommend_friend);
		}

		/*
		 * else if (type == XmppMessage.MSG_TYPE_DELSEE) {// 取消关注 if (!message.isMySend()) content = "有人取消关注了你"; } else if (type == XmppMessage.MSG_TYPE_DELALL) {// 新朋友提示 if
		 * (!message.isMySend()) content = "有人彻底删除了你"; }else if (type == XmppMessage.MSG_TYPE_BLACK) {// 被人拉黑 if (!message.isMySend()) { content =
		 * context.getString(R.string.be_add_blacklist); } }
		 */

		else {
			content = message.getContent();
		}
		if (TextUtils.isEmpty(content)) {
			content = "";
		}
		UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
		try {
			builder.updateColumnValue("content", content);
			builder.updateColumnValue("type", type);
			builder.updateColumnValue("timeSend", message.getTimeSend());
			builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
			friendDao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public boolean createOrUpdateFriendByNewFriend(NewFriendMessage newFriend, int friendStatus) {
		try {
			Friend existFriend = getFriend(newFriend.getOwnerId(), newFriend.getUserId());
			if (existFriend == null) {
				existFriend = new Friend();
				existFriend.setOwnerId(newFriend.getOwnerId());
				existFriend.setUserId(newFriend.getUserId());
				existFriend.setNickName(newFriend.getNickName());
				existFriend.setTimeCreate(TimeUtils.sk_time_current_time());
				existFriend.setCompanyId(newFriend.getCompanyId());// 可能是公司，这个需要设置
				existFriend.setVersion(TableVersionSp.getInstance().getFriendTableVersion(newFriend.getOwnerId()));
			}
			existFriend.setStatus(friendStatus);

			CreateOrUpdateStatus status = friendDao.createOrUpdate(existFriend);
			return status.isCreated() || status.isUpdated();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/* 获取面试未读消息总和，面试未读消息直接查询 Friend.ID_INTERVIEW_MESSAGE 该虚拟好友的未读数量即可 */
	// public int getInterviewUnReadNum(String ownerId) {
	// try {
	// QueryBuilder<Friend, Integer> builder = friendDao.queryBuilder();
	// builder.where().eq("ownerId", ownerId).and().eq("userId", Friend.ID_INTERVIEW_MESSAGE);
	// Friend friend = friendDao.queryForFirst(builder.prepare());
	// if (friend != null) {
	// return friend.getUnReadNum();
	// }
	// } catch (SQLException e) {
	// e.printStackTrace();
	// }
	// return 0;
	// }

	// ///////////////////////////////朋友界面///////////
	public List<Friend> getAllFriends(String ownerId) {
		try {
			PreparedQuery<Friend> preparedQuery;
			preparedQuery = friendDao.queryBuilder().where().eq("ownerId", ownerId).and()
					.in("status", new Object[] { Friend.STATUS_SYSTEM, Friend.STATUS_FRIEND }).and().eq("roomFlag", 0).and().eq("companyId", 0)
					.prepare();
			return friendDao.query(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

	}

	public List<Friend> getAllAttentions(String ownerId) {
		try {
			PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where().eq("ownerId", ownerId).and().eq("status", Friend.STATUS_ATTENTION)
					.and().eq("roomFlag", 0).and().eq("companyId", 0).prepare();
			return friendDao.query(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<Friend> getAllEnterprises(String ownerId) {
		try {
			PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where().eq("ownerId", ownerId).and()
					.in("status", new Object[] { Friend.STATUS_ATTENTION, Friend.STATUS_FRIEND }).and().eq("roomFlag", 0).and().gt("companyId", 0)
					.prepare();
			return friendDao.query(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<Friend> getAllRooms(String ownerId) {
		try {
			PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where().eq("ownerId", ownerId).and().eq("roomFlag", 1).prepare();
			return friendDao.query(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<Friend> getAllBlacklists(String ownerId) {
		try {
			PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where().eq("ownerId", ownerId).and().eq("status", Friend.STATUS_BLACKLIST)
					.and().eq("roomFlag", 0).prepare();
			return friendDao.query(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取所有联系人，即互相关注和单项关注的人，不包括系统号
	 *
	 * @param ownerId
	 * @return
	 */
	public List<Friend> getAllContacts(String ownerId) {
		try {
			PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where().eq("ownerId", ownerId).and()
					.in("status", new Object[] { Friend.STATUS_ATTENTION, Friend.STATUS_FRIEND }).and().eq("roomFlag", 0).and().eq("companyId", 0)
					.prepare();
			return friendDao.query(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

	}

	public Friend getMucFriendByRoomId(String roomId) {
		try {
			PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where().eq("userId", roomId).prepare();
			return friendDao.queryForFirst(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void updateMucFriendRoomName(String roomId, String roomName) {
		try {
			UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
			builder.updateColumnValue("nickName", roomName).where().eq("userId", roomId);
			friendDao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 更新好友的状态
	 *
	 * @param loginUserId
	 * @param userId
	 * @return
	 */
	public void updateFriendStatus(String loginUserId, String userId, int status) {
		UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
		try {
			builder.updateColumnValue("status", status);
			builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
			friendDao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 重置好友的消息，让他不在消息界面查询出来
	 */
	public void resetFriendMessage(String loginUserId, String userId) {
		UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
		try {
			builder.updateColumnValue("unReadNum", 0);
			builder.updateColumnValue("content", null);
			builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
			friendDao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 用户数据更新，下载关注的用户（包括好友和关注）时候调用
	 */
	public void addAttentionUsers(final Handler handler, final String loginUserId, final List<AttentionUser> attentionUsers,
								  final OnCompleteListener listener) {
		new Thread(() -> {
            checkSystemFriend(loginUserId);
            int tableVersion = TableVersionSp.getInstance().getFriendTableVersion(loginUserId);
            int newVersion = tableVersion + 1;
            if (attentionUsers != null && attentionUsers.size() > 0) {
                for (int i = 0; i < attentionUsers.size(); i++) {
                    AttentionUser attentionUser = attentionUsers.get(i);
                    if (attentionUser == null) {
                        continue;
                    }
                    String userId = attentionUser.getToUserId();// 好友的Id
                    QueryBuilder<Friend, Integer> builder = friendDao.queryBuilder();
                    Friend friend = null;
                    try {
                        builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
                        friend = friendDao.queryForFirst(builder.prepare());
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                    if (friend == null) {
                        friend = new Friend();
                    }
                    friend.setTimeCreate(attentionUser.getCreateTime());
                    friend.setOwnerId(attentionUser.getUserId());
                    friend.setUserId(attentionUser.getToUserId());
                    friend.setNickName(attentionUser.getToNickName());
                    friend.setRemarkName(attentionUser.getRemarkName());
                    friend.setRoomFlag(0);// 0朋友 1群组
                    friend.setCompanyId(attentionUser.getCompanyId());// 公司
                    int status = (attentionUser.getBlacklist() == 0) ? attentionUser.getStatus() : -1;
                    friend.setStatus(status);
                    friend.setVersion(newVersion);// 更新版本
                    try {
                        friendDao.createOrUpdate(friend);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            // 本地Sp中保存的版本号更新（+1）
            TableVersionSp.getInstance().setFriendTableVersion(loginUserId, newVersion);
            // 更新完成，把过期的好友数据删除
            try {

                DeleteBuilder<Friend, Integer> builder = friendDao.deleteBuilder();
                /**
                 * 删除条件 eq("ownerId", loginUserId) 当前登陆用户的数据<br/>
                 * eq("roomFlag", 0) 朋友数据（非群聊数据） <br/>
                 * .eq("companyId", 0) 不是企业用户<br/>
                 * in("status", new Integer[]{Friend.STATUS_FRIEND,Friend.STATUS_ATTENTION,Friend.STATUS_BLACKLIST}) 好友、关注或者黑名单<br/>
                 */
                builder.where().eq("ownerId", loginUserId).and().eq("roomFlag", 0).and()
                        .in("status", new Object[] { Friend.STATUS_FRIEND, Friend.STATUS_ATTENTION, Friend.STATUS_BLACKLIST }).and()
                        .ne("version", newVersion);
                friendDao.delete(builder.prepare());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // 朋友数据更新了，在去删除不存在的消息表
            List<String> tables = SQLiteRawUtil.getUserChatMessageTables(mHelper.getReadableDatabase(), loginUserId);
            if (tables != null && tables.size() > 0) {
                for (int i = 0; i < tables.size(); i++) {
                    String tableName = tables.get(i);
                    String tablePrefix = SQLiteRawUtil.CHAT_MESSAGE_TABLE_PREFIX + loginUserId;
                    int index = tableName.indexOf(tablePrefix);
                    if (index == -1) {
                        continue;
                    }
                    String toUserId = tableName.substring(index + tablePrefix.length(), tableName.length());
                    if (toUserId.equals(Friend.ID_BLOG_MESSAGE) || toUserId.equals(Friend.ID_INTERVIEW_MESSAGE)
                            || toUserId.equals(Friend.ID_NEW_FRIEND_MESSAGE) ||

                            toUserId.equals(Friend.ID_SYSTEM_MESSAGE)) {
                        continue;
                    }
                    Friend friend = getFriend(loginUserId, toUserId);
                    if (friend == null) {// 删除这张消息表
                        SQLiteRawUtil.dropTable(mHelper.getReadableDatabase(), tableName);
                    }
                }
            }

            if (handler != null && listener != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onCompleted();
                    }
                });
            }
        }

		).start();
	}

	/**
	 * 用户数据更新，下载进入的房间
	 */
	public void addRooms(final Handler handler, final String loginUserId, final List<MucRoom> rooms, final OnCompleteListener listener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				int tableVersion = TableVersionSp.getInstance().getFriendTableVersion(loginUserId);
				int newVersion = tableVersion + 1;
				if (rooms != null && rooms.size() > 0) {
					for (int i = 0; i < rooms.size(); i++) {
						MucRoom mucRoom = rooms.get(i);
						if (mucRoom == null) {
							continue;
						}
						String userId = mucRoom.getJid();// 好友的Id
						QueryBuilder<Friend, Integer> builder = friendDao.queryBuilder();
						Friend friend = null;
						try {
							builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
							friend = friendDao.queryForFirst(builder.prepare());
						} catch (SQLException e1) {
							e1.printStackTrace();
						}
						if (friend == null) {
							friend = new Friend();
							friend.setOwnerId(loginUserId);
							friend.setUserId(mucRoom.getJid());
							// 新建的房间，根据创建时间取历史记录
							friend.setTimeSend((int) mucRoom.getCreateTime());
						}
						friend.setNickName(mucRoom.getName());
						friend.setDescription(mucRoom.getDesc());
						friend.setRoomFlag(1);
						friend.setRoomId(mucRoom.getId());
						friend.setRoomCreateUserId(mucRoom.getUserId());
						// timeSend作为取群聊离线消息的标志，所以要在这里设置一个初始值
						// friend.setTimeSend(TimeUtils.sk_time_current_time());
						friend.setStatus(Friend.STATUS_FRIEND);
						friend.setVersion(newVersion);// 更新版本
						MucRoomMember memberMy = mucRoom.getMember();
						if (memberMy != null) {
							friend.setRoomMyNickName(memberMy.getNickName());
							friend.setRoomTalkTime(memberMy.getTalkTime());
						}
						try {
							friendDao.createOrUpdate(friend);
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
				// 本地Sp中保存的版本号更新（+1）
				TableVersionSp.getInstance().setFriendTableVersion(loginUserId, newVersion);
				// 更新完成，把过期的房间数据删除
				try {
					DeleteBuilder<Friend, Integer> builder = friendDao.deleteBuilder();
					builder.where().eq("ownerId", loginUserId).and().eq("roomFlag", 1).and().eq("status", Friend.STATUS_FRIEND).and()
							.ne("version", newVersion);
					friendDao.delete(builder.prepare());
				} catch (SQLException e) {
					e.printStackTrace();
				}

				// 朋友数据更新了，在去删除不存在的消息表
				List<String> tables = SQLiteRawUtil.getUserChatMessageTables(mHelper.getReadableDatabase(), loginUserId);
				if (tables != null && tables.size() > 0) {
					for (int i = 0; i < tables.size(); i++) {
						String tableName = tables.get(i);
						String tablePrefix = SQLiteRawUtil.CHAT_MESSAGE_TABLE_PREFIX + loginUserId;
						int index = tableName.indexOf(tablePrefix);
						if (index == -1) {
							continue;
						}
						String toUserId = tableName.substring(index + tablePrefix.length(), tableName.length());
						if (toUserId.equals(Friend.ID_BLOG_MESSAGE) || toUserId.equals(Friend.ID_INTERVIEW_MESSAGE)
								|| toUserId.equals(Friend.ID_NEW_FRIEND_MESSAGE) ||

								toUserId.equals(Friend.ID_SYSTEM_MESSAGE)) {
							continue;
						}
						Friend friend = getFriend(loginUserId, toUserId);
						if (friend == null) {// 删除这张消息表
							SQLiteRawUtil.dropTable(mHelper.getReadableDatabase(), tableName);
						}
					}
				}

				if (handler != null && listener != null) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							listener.onCompleted();
						}
					});
				}
			}
		}

		).start();
	}

	public void updateNickName(String ownerId, String friendId, String myNickName) {
		UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
		try {
			builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
			builder.updateColumnValue("roomMyNickName", myNickName);
			friendDao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}

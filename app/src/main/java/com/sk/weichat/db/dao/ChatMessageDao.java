package com.sk.weichat.db.dao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;
import android.util.Log;

import com.j256.ormlite.android.DatabaseTableConfigUtil;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.sk.weichat.BuildConfig;
import com.sk.weichat.MyApplication;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.db.SQLiteHelper;
import com.sk.weichat.db.SQLiteRawUtil;
import com.sk.weichat.db.UnlimitDaoManager;
import com.sk.weichat.util.TimeUtils;
import com.ymz.baselibrary.utils.L_;

public class ChatMessageDao {
	private static ChatMessageDao instance = null;

	public static final ChatMessageDao getInstance() {
		if (instance == null) {
			synchronized (ChatMessageDao.class) {
				if (instance == null) {
					instance = new ChatMessageDao();
				}
			}
		}
		return instance;
	}

	private SQLiteHelper mHelper;

	private ChatMessageDao() {
		mHelper = OpenHelperManager.getHelper(MyApplication.getInstance(), SQLiteHelper.class);
		mDaoMap = new HashMap<String, Dao<ChatMessage, Integer>>();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		OpenHelperManager.releaseHelper();
	}

	private Map<String, Dao<ChatMessage, Integer>> mDaoMap;

	private Dao<ChatMessage, Integer> getDao(String ownerId, String friendId) {
		if (TextUtils.isEmpty(ownerId) || TextUtils.isEmpty(friendId)) {
			return null;
		}
		String tableName = SQLiteRawUtil.CHAT_MESSAGE_TABLE_PREFIX + ownerId + friendId;
		if (mDaoMap.containsKey(tableName)) {
			return mDaoMap.get(tableName);
		}
		Dao<ChatMessage, Integer> dao = null;
		try {
			DatabaseTableConfig<ChatMessage> config = DatabaseTableConfigUtil.fromClass(mHelper.getConnectionSource(), ChatMessage.class);
			config.setTableName(tableName);
			SQLiteRawUtil.createTableIfNotExist(mHelper.getWritableDatabase(), tableName, SQLiteRawUtil.getCreateChatMessageTableSql(tableName));
			dao = UnlimitDaoManager.createDao(mHelper.getConnectionSource(), config);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (dao != null)
			mDaoMap.put(tableName, dao);
		return dao;
	}

	/**
	 * 保存一条新的聊天记录
	 * 
	 * @param db
	 * @param myUserId
	 * @param chatMessageEntity
	 */
	public boolean saveNewSingleChatMessage(String ownerId, String friendId, ChatMessage message) {


		L_.e("message.get_id()------------>:" + message.getPacketId());
		L_.e( "message.getContent()------------>:" + message.getContent());
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		if (dao == null) {
			return false;
		}

		try {
			// 重复消息去除
			List<ChatMessage> chatMessages = dao.queryForEq("packetId", message.getPacketId());
			if (chatMessages != null && chatMessages.size() > 0) {
				return false;// 重复消息
			}

			// 保存这次的消息
			dao.create(message);
			if (BuildConfig.DEBUG) {
				Log.d("roamer", "message.get_id():" + message.get_id());
				Log.d("roamer", "message.getContent():" + message.getContent());
			}
			// 更新朋友表最后一次消息事件
			FriendDao.getInstance().updateLastChatMessage(ownerId, friendId, message);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 更新消息发送状态OK
	 * 
	 */
	public void updateMessageSendState(String ownerId, String friendId, int msg_id, int messageState) {
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		if (dao == null) {
			return;
		}
		UpdateBuilder<ChatMessage, Integer> builder = dao.updateBuilder();
		try {
			builder.updateColumnValue("messageState", messageState);
			builder.updateColumnValue("timeReceive", TimeUtils.sk_time_current_time());
			builder.where().idEq(msg_id);
			dao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 更新消息上传状态OK
	 * 
	 */
	public void updateMessageUploadState(String ownerId, String friendId, int msg_id, boolean isUpload, String url) {
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		if (dao == null) {
			return;
		}
		UpdateBuilder<ChatMessage, Integer> builder = dao.updateBuilder();
		try {
			builder.updateColumnValue("isUpload", isUpload);
			builder.updateColumnValue("content", url);
			builder.where().idEq(msg_id);
			dao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 更新语音消息是否已读的状态
	 * 
	 */
	public void updateMessageReadState(String ownerId, String friendId, int msg_id, boolean isRead) {
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		if (dao == null) {
			return;
		}
		UpdateBuilder<ChatMessage, Integer> builder = dao.updateBuilder();
		try {
			builder.updateColumnValue("isRead", isRead);
			builder.where().idEq(msg_id);
			dao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 更新消息下载状态OK
	 */
	public void updateMessageDownloadState(String ownerId, String friendId, int msg_id, boolean isDownload, String filePath) {
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		if (dao == null) {
			return;
		}
		UpdateBuilder<ChatMessage, Integer> builder = dao.updateBuilder();
		try {
			builder.updateColumnValue("isDownload", isDownload);
			builder.updateColumnValue("filePath", filePath);
			builder.where().idEq(msg_id);
			dao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * OK 取与某人的聊天记录
	 * 
	 * @param myUserId
	 * @param toUserId
	 * @param mMinId
	 *            大于此ID
	 * @param pageSize
	 *            查询几条数据
	 * @return
	 */
	public List<ChatMessage> getSingleChatMessages(String ownerId, String friendId, int mMinId, int pageSize) {
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		if (dao == null) {
			return null;
		}
		QueryBuilder<ChatMessage, Integer> builder = dao.queryBuilder();
		List<ChatMessage> messages = null;
		try {
			if (mMinId != 0) {
				builder.where().lt("_id", mMinId);
			}
			builder.orderBy("_id", false);
			builder.limit((long) pageSize);
			builder.offset(0L);
			messages = dao.query(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return messages;
	}

	/**
	 * 删除与某人的聊天消息表
	 */
	public void deleteMessageTable(String ownerId, String friendId) {
		String tableName = SQLiteRawUtil.CHAT_MESSAGE_TABLE_PREFIX + ownerId + friendId;
		if (mDaoMap.containsKey(tableName)) {
			mDaoMap.remove(tableName);
		}
		if (SQLiteRawUtil.isTableExist(mHelper.getWritableDatabase(), tableName)) {
			SQLiteRawUtil.dropTable(mHelper.getWritableDatabase(), tableName);
		}
	}

	public void updateNickName(String ownerId, String friendId, String fromUserId, String newNickName) {
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		UpdateBuilder<ChatMessage, Integer> builder = dao.updateBuilder();
		try {
			builder.where().eq("fromUserId", fromUserId);
			builder.updateColumnValue("fromUserName", newNickName);
			dao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}

package com.lsjr.zizi.chat.dao;

import android.text.TextUtils;

import com.j256.ormlite.android.DatabaseTableConfigUtil;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.db.ChatMessage;
import com.lsjr.zizi.chat.helper.SQLiteHelper;
import com.lsjr.zizi.chat.helper.SQLiteRawUtil;
import com.lsjr.zizi.chat.helper.UnlimitDaoManager;
import com.lsjr.zizi.chat.utils.TimeUtils;
import com.ymz.baselibrary.BaseApplication;
import com.ymz.baselibrary.utils.L_;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lsjr.zizi.chat.xmpp.listener.ChatMessageListener.MESSAGE_SEND_SUCCESS;

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
		mHelper = OpenHelperManager.getHelper(BaseApplication.getApplication(), SQLiteHelper.class);
		mDaoMap = new HashMap<>();
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
	 */
	public boolean saveNewSingleChatMessage(String ownerId, String friendId, ChatMessage message) {
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		if (dao == null) {
			return false;
		}
		try {

			L_.e("saveNewSingleChatMessage---message.get_id()------------>:" + message.getFromUserId());
			L_.e("saveNewSingleChatMessage---message.get_id()------------>:" + message.get_id());
			L_.e( "saveNewSingleChatMessage----message.getContent()------------>:" + message.getContent());
			L_.e( "saveNewSingleChatMessage----message.getContent()------------>:" + message.getMessageState());
			if (message.getFromUserId().equals(ConfigApplication.instance().mLoginUser.getUserId())){
				message.setMessageState(MESSAGE_SEND_SUCCESS);
			}
			// 重复消息去除
			List<ChatMessage> chatMessages = dao.queryForEq("packetId", message.getPacketId());
			if (chatMessages != null && chatMessages.size() > 0) {
				return false;// 重复消息
			}
			// 保存这次的消息
			dao.create(message);
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



//	public void queryLastMessage(String ownerId, String friendId){
//		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
//		if (dao == null) {
//			return;
//		}
//		return dao.queryBuilder().where().eq("user_id", userId)
//				.query();
//	}

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
	 * 更新消息上传状态OK
	 *
	 */
	public void updateMessageUploadState(String ownerId, String friendId, int msg_id, boolean isUpload, String url,String thumbnailUrl) {
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		if (dao == null) {
			return;
		}
		UpdateBuilder<ChatMessage, Integer> builder = dao.updateBuilder();
		try {
			builder.updateColumnValue("isUpload", isUpload);
			builder.updateColumnValue("content", url);
			builder.updateColumnValue("filePath", thumbnailUrl);
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
	public void updateMessageReadState(String ownerId, String friendId, int msg_id) {

		L_.e("updateMessageReadState  "+ownerId+":------>"+friendId);
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		L_.e("updateMessageReadState  "+dao);
		if (dao == null) {
			return;
		}
		UpdateBuilder<ChatMessage, Integer> builder = dao.updateBuilder();
		try {
			builder.updateColumnValue("isRead", true);
			builder.where().idEq(msg_id);
			dao.update(builder.prepare());
			L_.e("改变状态成功");
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
	public boolean deleteChatOneMsg(String ownerId, String friendId, ChatMessage message) {
		Dao<ChatMessage, Integer> dao = getDao(ownerId, friendId);
		if (dao == null) {
			return false;
		}
		try {
			// 重复消息去除
			List<ChatMessage> chatMessages = dao.queryForEq("packetId", message.getPacketId());
			if (chatMessages != null && chatMessages.size() > 0) {
				dao.delete(message);
				QueryBuilder<ChatMessage, Integer> builder = dao.queryBuilder();
				try {
					builder.orderBy("_id", false);
					List<ChatMessage> messages = dao.query(builder.prepare());
					for (int i=0;i<messages.size();i++){
						if (messages.get(i).getPacketId().equals(message.getPacketId())){
							messages.remove(i);
						}
					}
					// 更新朋友表最后一次消息事件
					if (messages.size()!=0){
						FriendDao.getInstance().updateLastChatMessage(ownerId, friendId, messages.get(0));
					}else {
						ChatMessage chatMessage=new ChatMessage();
						chatMessage.setContent("暂无消息");
						FriendDao.getInstance().updateLastChatMessage(ownerId, friendId, chatMessage);
					}
					return true;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}catch (Exception e){

		}
		return false;
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


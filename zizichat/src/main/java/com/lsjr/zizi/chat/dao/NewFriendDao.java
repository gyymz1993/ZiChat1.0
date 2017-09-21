package com.lsjr.zizi.chat.dao;

import java.sql.SQLException;
import java.util.List;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.Dao.CreateOrUpdateStatus;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.lsjr.zizi.chat.db.NewFriendMessage;
import com.lsjr.zizi.chat.helper.SQLiteHelper;
import com.ymz.baselibrary.BaseApplication;

/**
 * 访问NewFriend数据的Dao
 * 
 * 
 */
public class NewFriendDao {
	private static NewFriendDao instance = null;

	public static final NewFriendDao getInstance() {
		if (instance == null) {
			synchronized (NewFriendDao.class) {
				if (instance == null) {
					instance = new NewFriendDao();
				}
			}
		}
		return instance;
	}

	public Dao<NewFriendMessage, Integer> newFriendDao;

	private NewFriendDao() {
		try {
			newFriendDao = DaoManager.createDao(OpenHelperManager.getHelper(BaseApplication.getApplication(), SQLiteHelper.class).getConnectionSource(),
					NewFriendMessage.class);
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
	 * 
	 * @param userId
	 *            当前登陆用户的userId
	 * @param friendXmppId
	 * @return
	 */
	public NewFriendMessage getNewFriendById(String ownerId, String friendId) {
		try {
			PreparedQuery<NewFriendMessage> preparedQuery = newFriendDao.queryBuilder().where().eq("ownerId", ownerId).and().eq("userId", friendId)
					.prepare();
			List<NewFriendMessage> friends = newFriendDao.query(preparedQuery);
			if (friends != null && friends.size() > 0) {
				return friends.get(0);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 删除一条新朋友记录
	 */
	public void deleteNewFriend(String ownerId, String friendId) {
		DeleteBuilder<NewFriendMessage, Integer> builder = newFriendDao.deleteBuilder();
		try {
			builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
			newFriendDao.delete(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 通过特定新朋友消息，可以提升好友关系的操作
	 * 
	 * @param newFriend
	 * @param updateFriend
	 *            是不是要更新好友表
	 */
	public void ascensionNewFriend(NewFriendMessage newFriend, int status) {
		createOrUpdateNewFriend(newFriend);

		FriendDao.getInstance().createOrUpdateFriendByNewFriend(newFriend, status);
	}

	public boolean createOrUpdateNewFriend(NewFriendMessage newFriend) {
		// 往新朋友表里面增加一条记录
		try {
			PreparedQuery<NewFriendMessage> preparedQuery = newFriendDao.queryBuilder().where().eq("ownerId", newFriend.getOwnerId()).and()
					.eq("userId", newFriend.getUserId()).prepare();
			NewFriendMessage existFriend = newFriendDao.queryForFirst(preparedQuery);
			if (existFriend != null) {
				newFriend.set_id(existFriend.get_id());
			}
			CreateOrUpdateStatus status = newFriendDao.createOrUpdate(newFriend);
			return status.isCreated() || status.isUpdated();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 分页查询最近聊天的好友
	 * 
	 * @return
	 */
	@Deprecated
	public List<NewFriendMessage> getNearlyNewFriendMsg(String ownerId, int pageIndex, int pageSize) {
		List<NewFriendMessage> friends = null;
		try {
			PreparedQuery<NewFriendMessage> preparedQuery = newFriendDao.queryBuilder().orderBy("timeSend", false).limit((long) pageSize)
					.offset((long) pageSize * pageIndex).where().eq("ownerId", ownerId).prepare();
			friends = newFriendDao.query(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return friends;
	}

	public List<NewFriendMessage> getAllNewFriendMsg(String ownerId) {
		List<NewFriendMessage> friends = null;
		try {
			PreparedQuery<NewFriendMessage> preparedQuery = newFriendDao.queryBuilder().orderBy("timeSend", false).where().eq("ownerId", ownerId)
					.prepare();
			friends = newFriendDao.query(preparedQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return friends;
	}

	public boolean isNewFriendRead(NewFriendMessage newFriend) {
		try {
			PreparedQuery<NewFriendMessage> preparedQuery = newFriendDao.queryBuilder().where().eq("ownerId", newFriend.getOwnerId()).and()
					.eq("userId", newFriend.getUserId()).prepare();
			NewFriendMessage existFriend = newFriendDao.queryForFirst(preparedQuery);
			if (existFriend == null) {
				return true;
			} else {
				return existFriend.isRead();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 更新阅读状态
	 * 
	 * @param myUserId
	 * @param userId
	 */
	public void markNewFriendRead(String ownerId) {
		UpdateBuilder<NewFriendMessage, Integer> builder = newFriendDao.updateBuilder();
		try {
			builder.updateColumnValue("isRead", true);
			builder.where().eq("ownerId", ownerId).and().eq("isRead", false);
			newFriendDao.update(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}

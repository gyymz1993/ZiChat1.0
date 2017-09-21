package com.sk.weichat.db.dao;

import java.sql.SQLException;
import java.util.List;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.sk.weichat.MyApplication;
import com.sk.weichat.bean.AuthCode;
import com.sk.weichat.db.SQLiteHelper;

/**
 * 操作手机验证码的Dao
 * 
 * 
 */
public class AuthCodeDao {
	private static AuthCodeDao instance = null;

	public static final AuthCodeDao getInstance() {
		if (instance == null) {
			synchronized (AuthCodeDao.class) {
				if (instance == null) {
					instance = new AuthCodeDao();
				}
			}
		}
		return instance;
	}

	public Dao<AuthCode, Integer> dao;

	private AuthCodeDao() {
		try {
			dao = DaoManager.createDao(OpenHelperManager.getHelper(MyApplication.getInstance(), SQLiteHelper.class).getConnectionSource(),
					AuthCode.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		OpenHelperManager.releaseHelper();
	}

	public void saveAuthCode(AuthCode authCode) {
		try {
			dao.create(authCode);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<AuthCode> getAuthCode(String phoneNumber) {
		List<AuthCode> codes = null;
		try {
			codes = dao.queryForEq("phoneNumber", phoneNumber);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return codes;
	}

	public void clearAuthCode(String phoneNumber) {
		DeleteBuilder<AuthCode, Integer> builder = dao.deleteBuilder();
		try {
			builder.where().eq("phoneNumber", phoneNumber);
			dao.delete(builder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}

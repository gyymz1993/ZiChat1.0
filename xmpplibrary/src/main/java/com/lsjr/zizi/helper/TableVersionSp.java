package com.lsjr.zizi.helper;

import com.ymz.baselibrary.utils.SpUtils;

/**
 * 根据UserId，保存用户的数据库某些的表的本地版本
 * 
 * 
 */
public class TableVersionSp  {
	private static final String SP_NAME = "table_version";// FILE_NAME
	private static TableVersionSp instance;

	/* known key */
	private static final String KEY_FRIEND_TABLE = "friend_";//朋友表的version +userId

	public static  TableVersionSp getInstance() {
		if (instance == null) {
			synchronized (TableVersionSp.class) {
				if (instance == null) {
					instance = new TableVersionSp();
				}
			}
		}
		return instance;
	}

	// friend_
	public int getFriendTableVersion (String userId) {
		return SpUtils.getInstance().getIntValue(KEY_FRIEND_TABLE+userId,0);
	}

	public void setFriendTableVersion(String userId,int value) {
		SpUtils.getInstance().setIntValue(KEY_FRIEND_TABLE+userId,value);
	}

	
}

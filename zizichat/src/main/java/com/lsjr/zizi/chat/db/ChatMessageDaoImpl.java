package com.lsjr.zizi.chat.db;

import java.sql.SQLException;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

public class ChatMessageDaoImpl extends BaseDaoImpl<ChatMessage, Integer>{
	public ChatMessageDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<ChatMessage> tableConfig) throws SQLException {
		super(connectionSource, tableConfig);
	}
}

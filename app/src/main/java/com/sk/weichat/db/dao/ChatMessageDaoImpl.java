package com.sk.weichat.db.dao;

import java.sql.SQLException;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.sk.weichat.bean.message.ChatMessage;

public class ChatMessageDaoImpl extends BaseDaoImpl<ChatMessage, Integer>{
	public ChatMessageDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<ChatMessage> tableConfig) throws SQLException {
		super(connectionSource, tableConfig);
	}
}

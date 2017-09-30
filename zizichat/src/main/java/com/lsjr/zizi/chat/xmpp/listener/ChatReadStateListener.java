package com.lsjr.zizi.chat.xmpp.listener;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/26 17:38
 */

public interface ChatReadStateListener {

    public static final int UNREADINTG = 0;// 已经认证
    public static final int READINTG = 1;// 已经认证
    public void onReadind(String userId);
}

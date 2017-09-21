package com.lsjr.zizi.chat.xmpp.util;


public class XmppStringUtil {


	public static boolean isJID(String jid) {
		int i = jid.indexOf("@");
		if (i <= 0) {
			return false;
		}
		return true;
	}

	public static boolean isMucJID(String serviceName, String from) {
		if (from == null || serviceName == null) {
			return false;
		}
		int i = from.indexOf(serviceName);
		if (i == 0) {
			return false;
		} else {
			char c = from.charAt(i - 1);
			return c == '.';
		}
	}

	public static String getRoomJID(String from) {
		int i = from.indexOf("/");
		if (i <= 0) {
			return "";
		}
		return from.substring(0, i);
	}
	
	public static String getRoomJIDPrefix(String roomJid) {
		int i = roomJid.indexOf("@");
		if (i <= 0) {
			return "";
		}
		return roomJid.substring(0, i);
	}

	public static String getRoomUserNick(String from) {
		int i = from.indexOf("/");
		if (i <= 0) {
			return "";
		}
		return from.substring(i + 1, from.length());
	}


}

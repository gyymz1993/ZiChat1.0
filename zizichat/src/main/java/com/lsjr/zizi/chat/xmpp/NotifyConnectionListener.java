package com.lsjr.zizi.chat.xmpp;


import org.jivesoftware.smack.XMPPConnection;

public interface NotifyConnectionListener{
	 void notifyConnectting();
	 void notifyAuthenticated(XMPPConnection arg0);
	 void notifyConnected(XMPPConnection arg0);
	 void notifyConnectionClosed();
	 void notifyConnectionClosedOnError(Exception arg0);
}

package com.lsjr.zizi.chat.db;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.lsjr.zizi.chat.xmpp.XmppMessage;

import io.rong.imlib.model.MessageContent;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.bean.message
 * @作者:王阳
 * @创建时间: 2015年10月12日 上午11:59:36
 * @描述: TODO
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: 聊天消息表, 其中会有上传字段的设置和解析字段的设置
 */
@DatabaseTable(daoClass = ChatMessageDaoImpl.class)
public class ChatMessage extends XmppMessage implements Parcelable {

	public ChatMessage() {
	}

	public ChatMessage(String jsonData) {
		parserJsonData(jsonData);
	}

	@DatabaseField
	private String fromUserId;

	@DatabaseField
	private String fromUserName;// 发送者名称

	/**
	 * 在不同的消息类型里，代表不同的含义：<br/>
	 * {@link XmppMessage#TYPE_TEXT} 文字 <br/>
	 * {@link XmppMessage#TYPE_IMAGE} 图片的Url<br/>
	 * {@link XmppMessage#TYPE_VOICE} 语音的Url <br/>
	 * {@link XmppMessage#TYPE_LOCATION} 地理<br/>
	 * {@link XmppMessage#TYPE_GIF} Gif图的名称 <br/>
	 * {@link XmppMessage#TYPE_TIP} 系统提示的字<br/>
	 * {@link XmppMessage#TYPE_FILE} 文件的url<br/>
	 */
	@DatabaseField
	private String content;

	@DatabaseField
	private String location_x;// 当为地理位置时，有效

	@DatabaseField
	private String location_y;// 当为地理位置时，有效

	@DatabaseField
	private int fileSize;// 当为图片、语音消息时，此节点有效。图片、语音文件的大小

	@DatabaseField
	private int timeLen;// 当为语音消息时，此节点有效。语音信息的长度

	/* 本地额外存数数据 */
	@DatabaseField(generatedId = true)
	private int _id;

	@DatabaseField
	private int timeReceive;// 接收到消息回执的时间

	@DatabaseField
	private String filePath;// 为语音视频图片文件的 本地路径（IOS端叫fileName），注意本地文件可能清除了，此节点代表的数据不一定有效

	@DatabaseField
	private boolean isUpload;// 当为图片和语音类型是，此节点有效，代表是否上传完成，默认false。isMySend=true，此节点有效，

	@DatabaseField
	private boolean isDownload;// 当为图片和语音类型是，此节点有效，代表是否下载完成，默认false。isMySend=false,此节点有效

	@DatabaseField
	private int messageState;// 只有当消息是我发出的，此节点才有效。消息的发送状态,默认值=0，代表发送中

	// 当为语音文件时，此节点代表语音是否已经读了。当为新朋友推送消息时，代表改推送消息是否已读。只在这两种情况下有效
	@DatabaseField
	private boolean isRead;// 默认为false 代表我未读

	@DatabaseField
	private int sipStatus;// 语音或者视频通话的状态，本地数据库存储即可

	@DatabaseField
	private int sipDuration;// 语音或者视频通话的时间，本地数据库存储即可

	// //////推送特有的//////
	@DatabaseField
	private String objectId;// 用于商务圈推送，代表哪一条公共消息



	private MessageContent messageContent;

	public int get_id() {
		return _id;
	}

	public void set_id(int _id) {
		this._id = _id;
	}

	public String getFromUserId() {
		return fromUserId;
	}

	public void setFromUserId(String fromUserId) {
		this.fromUserId = fromUserId;
	}


	private int width;
	private int height;

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}




// public String getToUserId() {
	// return toUserId;
	// }
	//
	// public void setToUserId(String toUserId) {
	// this.toUserId = toUserId;
	// }


	public MessageContent getMessageContent() {
		return messageContent;
	}

	public void setMessageContent(MessageContent content) {
		this.messageContent = content;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getLocation_x() {
		return location_x;
	}

	public void setLocation_x(String location_x) {
		this.location_x = location_x;
	}

	public String getLocation_y() {
		return location_y;
	}

	public void setLocation_y(String location_y) {
		this.location_y = location_y;
	}

	public int getFileSize() {
		return fileSize;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	public int getTimeLen() {
		return timeLen;
	}

	public void setTimeLen(int timeLen) {
		this.timeLen = timeLen;
	}

	public int getMessageState() {
		return messageState;
	}

	public void setRead(boolean isRead) {
		this.isRead = isRead;
	}

	public void setMessageState(int messageState) {
		this.messageState = messageState;
	}

	public boolean isRead() {
		return isRead;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public String getFromUserName() {
		return fromUserName;
	}

	public void setFromUserName(String fromUserName) {
		this.fromUserName = fromUserName;
	}

	public boolean isUpload() {
		return isUpload;
	}

	public void setUpload(boolean isUpload) {
		this.isUpload = isUpload;
	}

	public boolean isDownload() {
		return isDownload;
	}

	public void setDownload(boolean isDownload) {
		this.isDownload = isDownload;
	}

	public int getTimeReceive() {
		return timeReceive;
	}

	public void setTimeReceive(int timeReceive) {
		this.timeReceive = timeReceive;
	}

	/**
	 * 解析接收到的消息
	 * 
	 * @param jsonData
	 */
	private void parserJsonData(String jsonData) {
		try {
			JSONObject jObject = JSON.parseObject(jsonData);
			type = getIntValueFromJSONObject(jObject, "type");
			timeSend = getIntValueFromJSONObject(jObject, "timeSend");
			fromUserId = getStringValueFromJSONObject(jObject, "fromUserId");
			fromUserName = getStringValueFromJSONObject(jObject, "fromUserName");
			content = getStringValueFromJSONObject(jObject, "content");
			location_x = getStringValueFromJSONObject(jObject, "location_x");
			location_y = getStringValueFromJSONObject(jObject, "location_y");
			fileSize = getIntValueFromJSONObject(jObject, "fileSize");
			timeLen = getIntValueFromJSONObject(jObject, "timeLen");
			filePath=getStringValueFromJSONObject(jObject,"filePath");//增加解析文件路径
			objectId=getStringValueFromJSONObject(jObject,"objectId");

			// 表示未读
			isRead = false;
			isMySend = false;
			isDownload = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String toJsonString(boolean isGroupChatMsg) {
		String msg = "";
		JSONObject object = new JSONObject();
		object.put("type", this.type);
		object.put("timeSend", this.timeSend);
		if (isGroupChatMsg) {
			object.put("fromUserId", this.fromUserId);
		}

		if (!TextUtils.isEmpty(this.fromUserName)) {
			object.put("fromUserName", this.fromUserName);
		}

		if (!TextUtils.isEmpty(this.content)) {
			object.put("content", this.content);
		}

		if (!TextUtils.isEmpty(this.location_x)) {
			object.put("location_x", this.location_x);
		}

		if (!TextUtils.isEmpty(this.location_y)) {
			object.put("location_y", this.location_y);
		}

		if (!TextUtils.isEmpty(this.objectId)) {
			object.put("objectId", this.objectId);
		}

		if (this.fileSize > 0) {
			object.put("fileSize", this.fileSize);
		}
		//增加filePath
        if(!TextUtils.isEmpty(this.filePath)){
        	object.put("filePath",this.filePath);
        }
		if (this.timeLen > 0) {
			object.put("timeLen", this.timeLen);
		}

		msg = object.toString();
		return msg;
	}

	public boolean validate() {
		return type != 0 && !TextUtils.isEmpty(fromUserId) && !TextUtils.isEmpty(fromUserName) && timeSend != 0;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(_id);
		dest.writeString(content);
		dest.writeString(filePath);
		dest.writeInt(fileSize);
		dest.writeString(fromUserId);
		dest.writeString(fromUserName);
		dest.writeString(location_x);
		dest.writeString(location_y);
		dest.writeInt(messageState);
		dest.writeString(objectId);
		dest.writeString(packetId);
		dest.writeInt(sipDuration);
		dest.writeInt(sipStatus);
		dest.writeInt(timeLen);
		dest.writeInt(timeReceive);
		dest.writeInt(timeSend);
		dest.writeInt(type);
//		dest.writeBooleanArray(val);
	}
	 public static final Parcelable.Creator<ChatMessage> CREATOR =new Creator<ChatMessage>() {

		@Override
		public ChatMessage createFromParcel(Parcel source) {
			ChatMessage message=new ChatMessage();
			message._id=source.readInt();
			message.content=source.readString();
			message.filePath=source.readString();
			message.fileSize=source.readInt();
			message.fromUserId=source.readString();
			message.fromUserName=source.readString();
//			boolean[] val={message.isDownload,message.isMySend,message.isRead,message.isUpload};
//			source.readBooleanArray(val);
			message.location_x=source.readString();
			message.location_y=source.readString();
			message.messageState=source.readInt();
			message.objectId=source.readString();
			message.packetId=source.readString();
			message.sipDuration=source.readInt();
			message.sipStatus=source.readInt();
			message.timeLen=source.readInt();
			message.timeReceive=source.readInt();
			message.timeSend=source.readInt();
			message.type=source.readInt();
	
			return message;
		}

		@Override
		public ChatMessage[] newArray(int size) {
			
			return new ChatMessage[size];
		}
		 
	 };


	public static enum SentStatus {
		SENDING(10),
		FAILED(20),
		SENT(30),
		RECEIVED(40),
		READ(50),
		DESTROYED(60),
		CANCELED(70);

		private int value = 1;

		private SentStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static SentStatus setValue(int code) {
			SentStatus[] arr$ = values();
			int len$ = arr$.length;

			for(int i$ = 0; i$ < len$; ++i$) {
				SentStatus c = arr$[i$];
				if(code == c.getValue()) {
					return c;
				}
			}

			return SENDING;
		}
	}

	public static enum MessageDirection {
		SEND(1),
		RECEIVE(2);

		private int value = 1;

		private MessageDirection(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static MessageDirection setValue(int code) {
			MessageDirection[] arr$ = values();
			int len$ = arr$.length;

			for(int i$ = 0; i$ < len$; ++i$) {
				MessageDirection c = arr$[i$];
				if(code == c.getValue()) {
					return c;
				}
			}

			return SEND;
		}
	}

	@Override
	public String toString() {
		return "ChatMessage{" +
				"fromUserId='" + fromUserId + '\'' +
				", fromUserName='" + fromUserName + '\'' +
				", content='" + content + '\'' +
				", location_x='" + location_x + '\'' +
				", location_y='" + location_y + '\'' +
				", fileSize=" + fileSize +
				", timeLen=" + timeLen +
				", _id=" + _id +
				", timeReceive=" + timeReceive +
				", filePath='" + filePath + '\'' +
				", isUpload=" + isUpload +
				", isDownload=" + isDownload +
				", messageState=" + messageState +
				", isRead=" + isRead +
				", sipStatus=" + sipStatus +
				", sipDuration=" + sipDuration +
				", objectId='" + objectId + '\'' +
				", messageContent=" + messageContent +
				", width=" + width +
				", height=" + height +
				'}';
	}
}

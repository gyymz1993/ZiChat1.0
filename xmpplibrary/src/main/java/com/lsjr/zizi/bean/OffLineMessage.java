package com.lsjr.zizi.bean;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/23 9:20
 */

public class OffLineMessage{

    /**
     * content : test 13027181000
     * messageId : b701b498c4ca42e3b2b5f3dba9d309ec
     * ts : 1503394048343
     * type : 1
     */

    private String content;
    private String messageId;
    private String ts;
    private String type;

    private int timeLen;

    public int getTimeLen() {
        return timeLen;
    }

    public void setTimeLen(int timeLen) {
        this.timeLen = timeLen;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

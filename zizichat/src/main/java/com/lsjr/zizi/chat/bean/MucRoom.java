package com.lsjr.zizi.chat.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class MucRoom {
    private String id;// 房间Id
    private String jid;// 房间jid
    private String name;// 房间名字
    private String subject;// 房间主题
    private String desc;// 房间描述
    private long createTime;// 创建时间
    private int maxUserSize;// 最大用户数
    private int userSize;// 当前用户数
    private String userId;// 创建者的Id
    @JSONField(name = "nickname")
    private String nickName;// 创建者的昵称
    private int areaId;
    private int cityId;
    private int provinceId;
    private int countryId;
    private double latitude;
    private double longitude;

    private int s;// 状态，无用
    private int category;// 类别，无用

    private List<MucRoomMember> members;

    private MucRoomMember member;// 代表我在这个房间的状态

    private List<Notice> notices;

    public static class Notice {
        private String id;
        private String text;
        private String userId;
        private String nickname;
        private long time;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

    }

    public List<Notice> getNotices() {
        return notices;
    }

    public void setNotices(List<Notice> notices) {
        this.notices = notices;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getMaxUserSize() {
        return maxUserSize;
    }

    public void setMaxUserSize(int maxUserSize) {
        this.maxUserSize = maxUserSize;
    }

    public int getUserSize() {
        return userSize;
    }

    public void setUserSize(int userSize) {
        this.userSize = userSize;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getAreaId() {
        return areaId;
    }

    public void setAreaId(int areaId) {
        this.areaId = areaId;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public int getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }

    public int getCountryId() {
        return countryId;
    }

    public void setCountryId(int countryId) {
        this.countryId = countryId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getS() {
        return s;
    }

    public void setS(int s) {
        this.s = s;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public List<MucRoomMember> getMembers() {
        return members;
    }

    public void setMembers(List<MucRoomMember> members) {
        this.members = members;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public MucRoomMember getMember() {
        return member;
    }

    public void setMember(MucRoomMember member) {
        this.member = member;
    }

    @Override
    public String toString() {
        return "MucRoom{" +
                "id='" + id + '\'' +
                ", jid='" + jid + '\'' +
                ", name='" + name + '\'' +
                ", subject='" + subject + '\'' +
                ", desc='" + desc + '\'' +
                ", createTime=" + createTime +
                ", maxUserSize=" + maxUserSize +
                ", userSize=" + userSize +
                ", userId='" + userId + '\'' +
                ", nickName='" + nickName + '\'' +
                ", areaId=" + areaId +
                ", cityId=" + cityId +
                ", provinceId=" + provinceId +
                ", countryId=" + countryId +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", s=" + s +
                ", category=" + category +
                ", members=" + members +
                ", member=" + member +
                ", notices=" + notices +
                '}';
    }
}

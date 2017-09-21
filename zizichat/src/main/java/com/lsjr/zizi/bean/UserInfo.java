//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lsjr.zizi.bean;

import android.net.Uri;
import android.os.Parcel;

public class UserInfo extends io.rong.imlib.model.UserInfo {
    private String id;
    private String name;
    private Uri portraitUri;
    public static final Creator<UserInfo> CREATOR = new Creator<UserInfo>() {
        public UserInfo createFromParcel(Parcel source) {
            return new UserInfo(source);
        }

        public UserInfo[] newArray(int size) {
            return new UserInfo[size];
        }
    };

    public UserInfo() {
        super(null);
    }


    public UserInfo(Parcel in) {
        super(in);
    }

    public UserInfo(String id, String name, Uri portraitUri) {
        super(id, name, portraitUri);
    }
}

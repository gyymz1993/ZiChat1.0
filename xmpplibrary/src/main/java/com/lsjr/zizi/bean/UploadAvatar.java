package com.lsjr.zizi.bean;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/13 10:50
 */

public class UploadAvatar {


    /**
     * data : {"oUrl":"http://dev.zizi.com.cn/avatar/o/12/12.jpg","tUrl":"http://dev.zizi.com.cn/avatar/t/12/12.jpg"}
     * resultCode : 1
     */

    private DataBean data;
    private int resultCode;

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public static class DataBean {
        /**
         * oUrl : http://dev.zizi.com.cn/avatar/o/12/12.jpg
         * tUrl : http://dev.zizi.com.cn/avatar/t/12/12.jpg
         */

        private String oUrl;
        private String tUrl;

        public String getOUrl() {
            return oUrl;
        }

        public void setOUrl(String oUrl) {
            this.oUrl = oUrl;
        }

        public String getTUrl() {
            return tUrl;
        }

        public void setTUrl(String tUrl) {
            this.tUrl = tUrl;
        }
    }
}

package com.lsjr.zizi.bean;

import cn.ittiger.indexlist.entity.BaseEntity;

public  class PhoneInfo implements BaseEntity{
        private String name;
        private String number;
        private String photo;

        public PhoneInfo(String name, String number, String photo) {
            this.name = name;
            this.number = number;
            this.photo = photo;
        }

        public String getName() {
            return name;
        }

        public String getNumber() {
            return number;
        }

        public String getPhoto() {
            return photo;
        }

    @Override
    public String toString() {
        return "PhoneInfo{" +
                "name='" + name + '\'' +
                ", number='" + number + '\'' +
                ", photo='" + photo + '\'' +
                '}';
    }

    @Override
    public String getIndexField() {
        return name;
    }
}
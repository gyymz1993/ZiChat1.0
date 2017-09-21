package com.lsjr.zizi.bean;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.util.TypeUtils;

import java.util.List;


/**
 * @创建者 CSDN_LQR
 * @描述 json管理器(fastjson)
 */
public class JsonMananger {

    static {
        TypeUtils.compatibleWithJavaBean = true;
    }

    private static final String tag = JsonMananger.class.getSimpleName();

    /**
     * 将json字符串转换成java对象
     *
     * @param json
     * @param cls
     * @return
     */
    public static <T> T jsonToBean(String json, Class<T> cls) throws Exception {
        return JSON.parseObject(json, cls);
    }

    /**
     * 将json字符串转换成java List对象
     *
     * @param json
     * @param cls
     * @return
     */
    public static <T> List<T> jsonToList(String json, Class<T> cls) throws Exception {
        return JSON.parseArray(json, cls);
    }

    /**
     * 将bean对象转化成json字符串
     *
     * @param obj
     * @return
     */
    public static String beanToJson(Object obj) throws Exception {
        String result = JSON.toJSONString(obj);
        Log.e(tag, "beanToJson: " + result);
        return result;
    }

}

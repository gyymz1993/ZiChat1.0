package com.lsjr.zizi.util;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("deprecation")
public class TimeUtils {

    // ///s 代表Simple日期格式：yyyy-MM-dd
    // ///f 代表Full日期格式：yyyy-MM-dd hh:mm:ss

    public static final SimpleDateFormat ss_format = new SimpleDateFormat("MM-dd");
    public static final SimpleDateFormat s_format = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat f_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static long s_str_2_long(String dateString) {
        try {
            Date d = s_format.parse(dateString);
            return d.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long f_str_2_long(String dateString) {
        try {
            Date d = f_format.parse(dateString);
            return d.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String ss_long_2_str(long timestamp) {
        return ss_format.format(new Date(timestamp));
    }

    public static String s_long_2_str(long timestamp) {
        return s_format.format(new Date(timestamp));
    }

    public static String f_long_2_str(long timestamp) {
        return f_format.format(new Date(timestamp));
    }

    /**
     * 获取字符串时间的年份
     *
     * @param dateString
     *            格式为yyyy-MM-ss，或者yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static int getYear(String dateString) {
        try {
            Date d = s_format.parse(dateString);
            return d.getYear() + 1900;// 年份是基于格林威治时间，所以加上1900
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取字符串时间的月份
     *
     * @param dateString
     *            格式为yyyy-MM-ss，或者yyyy-MM-dd hh:mm:ss
     * @return
     */
    public static int getMonth(String dateString) {
        try {
            Date d = s_format.parse(dateString);
            return d.getMonth();// 月份从0-11
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取字符串时间的天
     *
     * @param dateString
     *            格式为yyyy-MM-ss，或者yyyy-MM-dd hh:mm:ss
     * @return
     */
    public static int getDayOfMonth(String dateString) {
        try {
            Date d = s_format.parse(dateString);
            return d.getDate();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

    public static int getHours(String timeString) {
        SimpleDateFormat formart = new SimpleDateFormat("HH:mm:ss");
        try {
            Date date = formart.parse(timeString);
            return date.getHours();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

    public static int getMinutes(String timeString) {
        SimpleDateFormat formart = new SimpleDateFormat("HH:mm:ss");
        try {
            Date date = formart.parse(timeString);
            return date.getMinutes();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

    public static int getSeconds(String timeString) {
        SimpleDateFormat formart = new SimpleDateFormat("HH:mm:ss");
        try {
            Date date = formart.parse(timeString);
            return date.getSeconds();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

    public static String getCurrentTime() {
        return f_format.format(new Date(System.currentTimeMillis()));
    }

    /**
     * 在当前时间上加上多少毫秒，返回这个时间
     *
     * @param mask
     * @return
     */
    public static String getCurrentTimeMask(long mask) {
        return f_format.format(new Date(System.currentTimeMillis() + mask));
    }

    // /////////////////////以上是通用的，下面为特殊需求的////////////////////////
    // /**
    // * 时间戳转换日期格式
    // *
    // * @param timestamp
    // * 单位秒
    // * @return
    // */
    // public static String getCurrentTime(long timestamp) {
    // SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // return f.format(new Date(timestamp * 1000));
    // }

    /**
     * 获取精简的日期
     *
     * @param time
     * @return
     */
    public static String getSimpleDate(String time) {
        SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = f_format.parse(time);
            return formater.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     *
     * @param time
     * @return
     */
    public static String getSimpleDateTime(String time) {
        SimpleDateFormat formater = new SimpleDateFormat("yy-MM-dd HH:mm");
        Date date = null;
        try {
            date = f_format.parse(time);
            return formater.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getSimpleTime(String time) {
        SimpleDateFormat formater = new SimpleDateFormat("HH:mm");
        Date date = null;
        try {
            date = f_format.parse(time);
            return formater.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getChatSimpleDate(String time) {
        SimpleDateFormat formater = new SimpleDateFormat("yy-MM-dd");
        Date date = null;
        try {
            date = f_format.parse(time);
            return formater.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getTimeHM(String time) {
        SimpleDateFormat formater = new SimpleDateFormat("HH:mm");
        Date date = null;
        try {
            date = f_format.parse(time);
            return formater.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static SimpleDateFormat friendly_format1 = new SimpleDateFormat("HH:mm");
    public static SimpleDateFormat friendly_format2 = new SimpleDateFormat("MM-dd HH:mm");



    public static String sk_time_friendly_format2(long time) {
        return friendly_format2.format(new Date(time * 1000));
    }

    public static String sk_time_s_long_2_str(long time) {
        return s_long_2_str(time * 1000);
    }

    public static String sk_time_ss_long_2_str(long time) {
        return ss_long_2_str(time * 1000);
    }

    public static long sk_time_s_str_2_long(String dateString) {
        return s_str_2_long(dateString) / 1000;
    }

    public static int sk_time_current_time() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    private static SimpleDateFormat hm_formater = new SimpleDateFormat("HH:mm");

    public static String sk_time_long_to_hm_str(long time) {
        try {
            return hm_formater.format(new Date(time * 1000));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String sk_time_long_to_chat_time_str(long time) {
        String date1 = sk_time_s_long_2_str(time);
        String date2 = sk_time_s_long_2_str(System.currentTimeMillis() / 1000);
        if (date1.compareToIgnoreCase(date2) == 0) {// 是同一天
            return sk_time_long_to_hm_str(time);
        } else {
            return long_to_yMdHm_str(time * 1000);
        }
    }

    public static final SimpleDateFormat sk_format_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    // 日期加小时的字符串
    public static String long_to_yMdHm_str(long time) {
        return sk_format_1.format(new Date(time));
    }

    public static long sk_time_yMdHm_str_to_long(String time) {
        try {
            return sk_format_1.parse(time).getTime() / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int yMdHm_getYear(String dateString) {
        try {
            Date d = sk_format_1.parse(dateString);
            return d.getYear() + 1900;// 年份是基于格林威治时间，所以加上1900
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int yMdHm_getMonth(String dateString) {
        try {
            Date d = sk_format_1.parse(dateString);
            return d.getMonth();// 月份从0-11
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int yMdHm_getDayOfMonth(String dateString) {
        try {
            Date d = sk_format_1.parse(dateString);
            return d.getDate();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int yMdHm_getHours(String timeString) {
        try {
            Date date = sk_format_1.parse(timeString);
            return date.getHours();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int yMdHm_getMinutes(String timeString) {
        try {
            Date date = sk_format_1.parse(timeString);
            return date.getMinutes();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     *
     * @param textView
     * @param time
     *            时间戳/1000
     * @return
     */
    public static long getSpecialBeginTime(TextView textView, long time) {
        long currentTime = System.currentTimeMillis() / 1000;
        if (time > currentTime) {
            time = currentTime;
        }
        textView.setText(sk_time_s_long_2_str(time));
        return time;
    }

    /**
     *
     * @param textView
     * @param time
     *            时间戳/1000
     * @return
     */
    public static long getSpecialEndTime(TextView textView, long time) {
        long currentTime = System.currentTimeMillis() / 1000;
        if (time == 0 || time > currentTime - 24 * 60 * 60) {
            //textView.setText(R.string.to_this_day);
            return 0;
        }
        textView.setText(sk_time_s_long_2_str(time));
        return time;
    }

    public static int sk_time_age(long birthday) {
        int age = (new Date().getYear()) - (new Date(birthday * 1000).getYear());
        if (age < 0 || age > 100) {
            return 25;
        }
        return age;
    }
}

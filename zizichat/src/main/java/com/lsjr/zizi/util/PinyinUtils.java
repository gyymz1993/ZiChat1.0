package com.lsjr.zizi.util;


import android.text.TextUtils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * @创建者 CSDN_LQR
 * @描述 拼音工具（需要pinyin4j-2.5.0.jar）
 */
public class PinyinUtils {

    /**
     * 根据传入的字符串(包含汉字),得到拼音
     * 沧晓 -> CANGXIAO
     * 沧  晓*& -> CANGXIAO
     * 沧晓f5 -> CANGXIAO
     *
     * @param str 字符串
     * @return
     */
    public static String getPinyin(String str) {

        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.UPPERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        StringBuilder sb = new StringBuilder();

        char[] charArray = str.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            // 如果是空格, 跳过
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c >= -127 && c < 128 || !(c >= 0x4E00 && c <= 0x9FA5)) {
                // 肯定不是汉字
                sb.append(c);
            } else {
                String s = "";
                try {
                    // 通过char得到拼音集合. 单 -> dan, shan
                    s = PinyinHelper.toHanyuPinyinStringArray(c, format)[0];
                    sb.append(s);
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                    sb.append(s);
                }
            }
        }

        return sb.toString();
    }


    private static HanyuPinyinOutputFormat format = null;
    static {
        format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.UPPERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
    }




    /**
     * 将字符串中的中文转换为全拼<br/>
     * 1、如果字符串为空，返回#<br/>
     * 2、如为中文，张三输出ZHANGSAN<br/>
     * 3、如果为字符 abc输出ABC<br/>
     * 4、如果为其他字符，输入#<br/>
     */
    public static String getPingYin(String inputString) {
        if (TextUtils.isEmpty(inputString)) {
            return "#";
        }
        char[] input = inputString.toCharArray();
        String output = "";
        try {
            for (int i = 0; i < input.length; i++) {
                String charStr = Character.toString(input[i]);
                if (charStr.matches("[\\u4E00-\\u9FA5]+")) {
                    String[] temp = PinyinHelper.toHanyuPinyinStringArray(input[i], format);
                    output += temp[0];
                } else if (charStr.matches("[A-Z]") || charStr.matches("[a-z]")) {
                    output += charStr.toUpperCase();
                } else {
                    output += "#";
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }




    /**
     * 将字符串中的中文转换为首字母简写，如输入张三，输出ZS。其他字符不变
     */
    public static String converterToFirstSpell(String inputString) {
        if (TextUtils.isEmpty(inputString)) {
            return "#";
        }
        char[] input = inputString.toCharArray();
        String output = "";
        try {
            for (int i = 0; i < input.length; i++) {
                String charStr = Character.toString(input[i]);
                if (charStr.matches("[\\u4E00-\\u9FA5]+")) {
                    String[] temp = PinyinHelper.toHanyuPinyinStringArray(input[i], format);
                    output += temp[0].charAt(0);
                } else if (charStr.matches("[A-Z]") || charStr.matches("[a-z]")) {
                    output += charStr.toUpperCase();
                } else {
                    output += "#";
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }


}

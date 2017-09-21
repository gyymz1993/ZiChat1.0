package com.lsjr.zizi.chat.utils;

import android.text.TextUtils;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class PingYinUtil {
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

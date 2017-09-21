package com.sk.weichat.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;

public class StringUtils {
	private final static Pattern email_pattern = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");

	/**
	 * EditText显示Error
	 * 
	 * @param context
	 * @param resId
	 * @return
	 */
	public static CharSequence editTextHtmlErrorTip(Context context, int resId) {
		CharSequence html = Html.fromHtml("<font color='red'>" + context.getString(resId) + "</font>");
		return html;
	}

	public static CharSequence editTextHtmlErrorTip(Context context, String text) {
		CharSequence html = Html.fromHtml("<font color='red'>" + text + "</font>");
		return html;
	}

	static Pattern phoneNumberPat = Pattern.compile("^((13[0-9])|(147)|(15[0-3,5-9])|(17[0,6-8])|(18[0-9]))\\d{8}$");
	static Pattern nickNamePat = Pattern.compile("^[\u4e00-\u9fa5_a-zA-Z0-9_]{3,15}$");// 3-10个字符
	static Pattern searchNickNamePat = Pattern.compile("^[\u4e00-\u9fa5_a-zA-Z0-9_]*$");// 不限制长度，可以为空字符串
	static Pattern companyNamePat = Pattern.compile("^[\u4e00-\u9fa5_a-zA-Z0-9_]{3,50}$");// 3-50个字符

	/* 是否是手机号 */
	public static boolean isMobileNumber(String mobiles) {
		Matcher mat = phoneNumberPat.matcher(mobiles);
		return mat.matches();
	}

	/* 检测之前，最好检测是否为空。检测是否是正确的昵称格式 */
	public static boolean isNickName(String nickName) {
		if (TextUtils.isEmpty(nickName)) {
			return false;
		}
		Matcher mat = nickNamePat.matcher(nickName);
		return mat.matches();
	}

	public static boolean isSearchNickName(String nickName) {
		if (nickName == null) {// 防止异常
			return false;
		}
		Matcher mat = searchNickNamePat.matcher(nickName);
		return mat.matches();
	}

	public static boolean isCompanyName(String name) {
		if (TextUtils.isEmpty(name)) {
			return false;
		}
		Matcher mat = companyNamePat.matcher(name);
		return mat.matches();
	}

	/**
	 * 判断是不是一个合法的电子邮件地址
	 * 
	 * @param email
	 * @return
	 */
	public static boolean isEmail(String email) {
		if (email == null || email.trim().length() == 0)
			return false;
		return email_pattern.matcher(email).matches();
	}

	public static boolean strEquals(String s1, String s2) {
		if (s1 == s2) {// 引用相等直接返回true
			return true;
		}
		boolean emptyS1 = s1 == null || s1.trim().length() == 0;
		boolean emptyS2 = s2 == null || s2.trim().length() == 0;
		if (emptyS1 && emptyS2) {// 都为空，认为相等
			return true;
		}
		if (s1 != null) {
			return s1.equals(s2);
		}
		if (s2 != null) {
			return s2.equals(s1);
		}
		return false;
	}

	/**
	 * 去掉特殊字符
	 */
	public static String replaceSpecialChar(String str) {
		if (str != null && str.length() > 0) {
			return str.replaceAll("&#39;", "’").replaceAll("&#039;", "’").replaceAll("&nbsp;", " ").replaceAll("\r\n", "\n").replaceAll("\n", "\r\n");
		}
		return "";
	}
}
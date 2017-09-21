/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sk.weichat.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;

import com.sk.weichat.R;

/**
 * A class for annotating a CharSequence with spans to convert textual emoticons to graphical ones.
 */
public class SmileyParser {
	private static SmileyParser sInstance;

	public static SmileyParser getInstance(Context context) {
		if (sInstance == null) {
			synchronized (SmileyParser.class) {
				if (sInstance == null) {
					sInstance = new SmileyParser(context);
				}
			}
		}
		return sInstance;
	}

	private final Context mContext;
	private final Pattern mPattern;
	private final Pattern mHtmlPattern;

	private SmileyParser(Context context) {
		mContext = context;
		mPattern = buildPattern();
		mHtmlPattern = buildHtmlPattern();
	}

	public static class Smilies {
		public static int[][] getIds() {
			return IDS;
		}

		public static String[][] getTexts() {
			return TEXTS;
		}

		public static int textMapId(String text) {
			if (MAPS.containsKey(text)) {
				return MAPS.get(text);
			} else {
				return -1;
			}
		}

		private static final int[][] IDS = { { R.drawable.emoji_001, R.drawable.emoji_002, R.drawable.emoji_003, R.drawable.emoji_004,
				R.drawable.emoji_005, R.drawable.emoji_006, R.drawable.emoji_007, R.drawable.emoji_008, R.drawable.emoji_009, R.drawable.emoji_010,
				R.drawable.emoji_011, R.drawable.emoji_012, R.drawable.emoji_013, R.drawable.emoji_014, R.drawable.emoji_015, R.drawable.emoji_016,
				R.drawable.emoji_017, R.drawable.emoji_018, } };

		private static final String[][] TEXTS = { { "[微笑]", "[得意]", "[害羞]", "[汗]", "[奸笑]", "[惊呆了]", "[开心]", "[哭]", "[呕吐]", "[亲亲]", "[色眯眯]", "[生病]",
				"[生气]", "[爽]", "[委屈]", "[严肃]", "[疑问]", "[晕]" } };

		private static final Map<String, Integer> MAPS = new HashMap<String, Integer>();
		static {
			// 取最小的长度，防止长度不一致出错
			int length = IDS.length > TEXTS.length ? TEXTS.length : IDS.length;
			for (int i = 0; i < length; i++) {
				int[] subIds = IDS[i];
				String[] subTexts = TEXTS[i];
				if (subIds == null || subTexts == null) {
					continue;
				}
				int subLength = subIds.length > subTexts.length ? subTexts.length : subIds.length;
				for (int j = 0; j < subLength; j++) {
					MAPS.put(TEXTS[i][j], IDS[i][j]);
				}
			}
		}
	}

	public static class Gifs {
		public static int[][] getIds() {
			return IDS;
		}

		public static String[][] getTexts() {
			return TEXTS;
		}

		public static int textMapId(String text) {
			if (MAPS.containsKey(text)) {
				return MAPS.get(text);
			} else {
				return -1;
			}
		}

		private static final int[][] IDS = {
				{ R.drawable.gif_001, R.drawable.gif_002, R.drawable.gif_003, R.drawable.gif_004, R.drawable.gif_005, R.drawable.gif_006,
						R.drawable.gif_007, R.drawable.gif_008 },
				{ R.drawable.gif_009, R.drawable.gif_010, R.drawable.gif_011, R.drawable.gif_012, R.drawable.gif_013, R.drawable.gif_014,
						R.drawable.gif_015, R.drawable.gif_016 },
				{ R.drawable.gif_017, R.drawable.gif_018, R.drawable.gif_019, R.drawable.gif_020, R.drawable.gif_021, R.drawable.gif_022,
						R.drawable.gif_023, R.drawable.gif_024 },
				{ R.drawable.gif_025, R.drawable.gif_026, R.drawable.gif_027, R.drawable.gif_028, R.drawable.gif_029, R.drawable.gif_030,
						R.drawable.gif_031, R.drawable.gif_032 },
				{ R.drawable.gif_033, R.drawable.gif_034, R.drawable.gif_035, R.drawable.gif_036, R.drawable.gif_037, R.drawable.gif_038,
						R.drawable.gif_039, R.drawable.gif_040 },
				{ R.drawable.gif_041, R.drawable.gif_042, R.drawable.gif_043, R.drawable.gif_044, R.drawable.gif_045 } };
		private static final String[][] TEXTS = {
				{ "bad手势.gif", "come手势.gif", "fuck手势.gif", "good手势.gif", "nono手势.gif", "ok手势.gif", "yeh手势.gif", "爱你.gif" },
				{ "抱抱.gif", "鄙视.gif", "闭嘴.gif", "吃惊.gif", "打哈秋.gif", "鼓掌.gif", "哈哈.gif", "害羞.gif" },
				{ "好吃.gif", "呵呵.gif", "哼.gif", "花心.gif", "奸笑.gif", "见钱开眼.gif", "可爱.gif", "困.gif" },
				{ "流汗.gif", "流泪.gif", "怒.gif", "抛媚眼.gif", "亲亲.gif", "傻眼.gif", "生病.gif", "失望.gif" },
				{ "睡觉.gif", "思考.gif", "送花.gif", "调皮.gif", "偷笑.gif", "吐白沫.gif", "委屈.gif", "嘻嘻.gif" },
				{ "喜欢.gif", "兴奋.gif", "嘘！安静.gif", "疑问.gif", "再见.gif" } };
		private static final Map<String, Integer> MAPS = new HashMap<String, Integer>();
		static {
			// 取最小的长度，防止长度不一致出错
			int length = IDS.length > TEXTS.length ? TEXTS.length : IDS.length;
			for (int i = 0; i < length; i++) {
				int[] subIds = IDS[i];
				String[] subTexts = TEXTS[i];
				if (subIds == null || subTexts == null) {
					continue;
				}
				int subLength = subIds.length > subTexts.length ? subTexts.length : subIds.length;
				for (int j = 0; j < subLength; j++) {
					MAPS.put(TEXTS[i][j], IDS[i][j]);
				}
			}
		}
	}

	/**
	 * Builds the regular expression we use to find smileys in {@link #addSmileySpans}.
	 */
	private Pattern buildPattern() {
		// Set the StringBuilder capacity with the assumption that the average
		// smiley is 3 characters long.
		StringBuilder patternString = new StringBuilder();

		// Build a regex that looks like (:-)|:-(|...), but escaping the smilies
		// properly so they will be interpreted literally by the regex matcher.
		patternString.append('(');
		for (int i = 0; i < Smilies.TEXTS.length; i++) {
			for (int j = 0; j < Smilies.TEXTS[i].length; j++) {
				patternString.append(Pattern.quote(Smilies.TEXTS[i][j]));
				patternString.append('|');
			}
		}

		// Replace the extra '|' with a ')'
		patternString.replace(patternString.length() - 1, patternString.length(), ")");

		return Pattern.compile(patternString.toString());
	}

	private Pattern buildHtmlPattern() {
		// Set the StringBuilder capacity with the assumption that the average
		// smiley is 3 characters long.
		// StringBuilder patternString = new StringBuilder();

		// Build a regex that looks like (:-)|:-(|...), but escaping the smilies
		// properly so they will be interpreted literally by the regex matcher.
		// patternString.append('(');
		// patternString.append(Pattern.quote("(<a)(\\w)+(?=</a>)"));
		// // Replace the extra '|' with a ')'
		// patternString.replace(patternString.length() - 1,
		// patternString.length(), ")");

		return Pattern.compile("(http://(\\S+?)(\\s))|(www.(\\S+?)(\\s))");
	}

	/**
	 * Adds ImageSpans to a CharSequence that replace textual emoticons such as :-) with a graphical version.
	 * 
	 * @param text
	 *            A CharSequence possibly containing emoticons
	 * @return A CharSequence annotated with ImageSpans covering any recognized emoticons.
	 */
	public CharSequence addSmileySpans(CharSequence text, boolean canClick) {

		SpannableStringBuilder builder = new SpannableStringBuilder(text);
		Matcher matcher = mPattern.matcher(text);
		while (matcher.find()) {
			int resId = Smilies.textMapId(matcher.group());
			if (resId != -1) {
				builder.setSpan(new MyImageSpan(mContext, resId), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		if (canClick) {
			Matcher htmlmatcher = mHtmlPattern.matcher(text);
			while (htmlmatcher.find()) {
				builder.setSpan(new URLSpan(htmlmatcher.group()), htmlmatcher.start(), htmlmatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		return builder;
	}

	ImageGetter imgGetter = new Html.ImageGetter() {
		public Drawable getDrawable(String source) {
			Drawable drawable = null;
			drawable = Drawable.createFromPath(source); // 显示本地图片
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			return drawable;
		}
	};

}

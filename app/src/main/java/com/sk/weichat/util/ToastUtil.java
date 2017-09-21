package com.sk.weichat.util;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sk.weichat.R;
/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.util
 * @作者:王阳
 * @创建时间: 2015年10月16日 下午2:22:59
 * @描述: toast工具类
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: 新增限定EditText字数弹出的toast
 */
public class ToastUtil {
	/**
	 * 设置EditText的字数限制
	 * 
	 * @param mTextEdit
	 * @param maxTextNum
	 *            最大字符数
	 */
	public static void addEditTextNumChanged(final Context context,final EditText mTextEdit, final int maxTextNum) {

		mTextEdit.addTextChangedListener(new TextWatcher() {
			private CharSequence temp;
			private boolean isEdit = true;
			private int selectionStart;
			private int selectionEnd;

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				temp = s;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				selectionStart = mTextEdit.getSelectionStart();
				selectionEnd = mTextEdit.getSelectionEnd();
				Log.i("gongbiao1", "" + selectionStart);
				if (temp.length() > maxTextNum) {
					Toast toast = Toast.makeText(context, "只能输入"+maxTextNum+"个字符哦", 0);
					toast.setGravity(Gravity.CENTER,0,0);
					TextView tv=new TextView(context);
					tv.setText("只能输入"+maxTextNum+"个字符哦");
					tv.setTextColor(Color.RED); 
					toast.setView(tv);
					toast.show();
					s.delete(selectionStart - 1, selectionEnd);
					int tempSelection = selectionStart;
					mTextEdit.setText(s);
					mTextEdit.setSelection(tempSelection);
				}
			}
		});
	}

	public static void showErrorNet(Context context) {
		if (context == null) {
			return;
		}
		showToast(context, R.string.net_exception);
	}

	public static void showErrorData(Context context) {
		if (context == null) {
			return;
		}
		showToast(context, R.string.data_exception);
	}

	//
	// public static void showFrequent(Context context) {
	// if (context == null) {
	// return;
	// }
	// showNormalToast(context, R.string.request_busy);
	// }
	//
	// public static void showNoMoreData(Context context) {
	// if (context == null) {
	// return;
	// }
	// showNormalToast(context, R.string.no_more_data);
	// }

	public static void showToast(Context context, String message) {
		if (context == null) {
			return;
		}
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	public static void showToast(Context context, int resId) {
		if (context == null) {
			return;
		}
		Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show();
	}

}

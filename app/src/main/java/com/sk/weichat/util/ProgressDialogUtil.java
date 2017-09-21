package com.sk.weichat.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;

public class ProgressDialogUtil {

	public static ProgressDialog init(Context context, CharSequence title, CharSequence message) {
		return init(context, title, message, false);
	}

	public static ProgressDialog init(Context context, CharSequence title, CharSequence message, boolean cancelable) {
		return init(context, title, message, false, cancelable, null);
	}

	public static ProgressDialog init(Context context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable) {
		return init(context, title, message, indeterminate, cancelable, null);
	}

	public static ProgressDialog init(Context context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable,
			OnCancelListener cancelListener) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setIndeterminate(indeterminate);
		dialog.setCancelable(cancelable);
		dialog.setOnCancelListener(cancelListener);
		return dialog;
	}

	public static void show(ProgressDialog dialog) {
		if (dialog != null && !dialog.isShowing()) {
			dialog.show();
		}
	}

	public static void show(ProgressDialog dialog, String message) {
		if (dialog != null) {
			dialog.setMessage(message);
		} else {
			return;
		}
		if (dialog.isShowing()) {
			dialog.dismiss();
		}
		dialog.show();
	}

	public static void dismiss(ProgressDialog dialog) {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}
	}

}

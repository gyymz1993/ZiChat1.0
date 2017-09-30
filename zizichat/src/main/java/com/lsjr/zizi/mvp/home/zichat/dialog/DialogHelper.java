package com.lsjr.zizi.mvp.home.zichat.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentManager;

import com.lsjr.zizi.R;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/29 15:13
 */

public class DialogHelper {


    /**
     * 加载中的弹出窗
     */
    private static final int PROGRESS_THEME = R.style.AlertDialog_Theme;

    public interface IDialogResultListener<T> {
        void onDataResult(T result);
    }

    public static DialogUtils showProgress(FragmentManager fragmentManager, String message) {
        return showProgress(fragmentManager, message, true);
    }

    public static DialogUtils showProgress(FragmentManager fragmentManager, String message, boolean cancelable) {
        return showProgress(fragmentManager, message, true, null);
    }

    public static DialogUtils showProgress(FragmentManager fragmentManager, String message, boolean cancelable, DialogUtils.OnDialogCancelListener cancelListener) {
        DialogUtils dialogFragment = DialogUtils.newInstance(context -> {
            ProgressDialog progressDialog = new ProgressDialog(context, PROGRESS_THEME);
            progressDialog.setMessage(message);
            return progressDialog;
        }, cancelable, cancelListener);
        dialogFragment.show(fragmentManager, "");
        return dialogFragment;
    }

    /**
     * 带输入框的弹出窗
     */

    public static void showInsertDialog(FragmentManager manager, final String title, final IDialogResultListener<String> resultListener, final boolean cancelable) {

//        DialogUtils dialogFragment = DialogUtils.newInstance(new DialogUtils.OnCallDialog() {
//            @Override
//            public Dialog getDialog(Context context) {
//                // ...
//                AlertDialog.Builder builder = new AlertDialog.Builder(context, PROGRESS_THEME);
//                builder.setPositiveButton(DIALOG_POSITIVE, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (resultListener != null) {
//                            resultListener.onDataResult(editText.getText().toString());
//                        }
//                    }
//                });
//                builder.setNegativeButton(DIALOG_NEGATIVE, null);
//                return builder.create();
//            }
//        }, cancelable, null);
//        dialogFragment.show(manager, INSERT_TAG);
    }


    /**
     * 带列表的弹出窗
     */

    public static void showListDialog(FragmentManager fragmentManager, final String title, final String[] items
            , final IDialogResultListener<Integer> resultListener, boolean cancelable ){
        DialogUtils dialogFragment = DialogUtils.newInstance(new DialogUtils.OnCallDialog() {
            @Override
            public Dialog getDialog(Context context) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context, PROGRESS_THEME);
                builder.setTitle(title);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(resultListener != null){
                            resultListener.onDataResult(which);
                        }
                    }
                });
                return builder.create();
            }
        }, cancelable, null);
        dialogFragment.show(fragmentManager, "");
    }

}

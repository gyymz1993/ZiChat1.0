package com.lsjr.zizi.mvp.home.zichat.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.lsjr.zizi.R;
import com.ymz.baselibrary.utils.UIUtils;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/29 15:04
 */

public class DialogUtils extends DialogFragment {

    /**
     * 监听弹出窗是否被取消
     */
    private OnDialogCancelListener mCancelListener;

    /**
     * 回调获得需要显示的dialog
     */
    private OnCallDialog mOnCallDialog;


    public interface OnDialogCancelListener {
        void onCancel();
    }

    public interface OnCallDialog {
        Dialog getDialog(Context context);
    }


    public static DialogUtils newInstance(OnCallDialog call, boolean cancelable){
        return newInstance(call, cancelable, null);
    }

    public static DialogUtils newInstance(OnCallDialog call, boolean cancelable,OnDialogCancelListener cancelListener){
        DialogUtils instance = new DialogUtils();
        instance.setCancelable(cancelable);
        instance.mCancelListener = cancelListener;
        instance.mOnCallDialog = call;
        return instance;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (null==mOnCallDialog){
            super.onCreateDialog(savedInstanceState);
        }
        return super.onCreateDialog(savedInstanceState);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       //

       // int style = R.style.AlertDialog_Theme, theme = 0;
       // setStyle(style,theme);
    }

    @Override
    public void onStart() {
        super.onStart();
        setStyle(DialogUtils.STYLE_NO_TITLE, R.style.AlertDialog_Theme);
        Dialog dialog=getDialog();
        if (dialog!=null){
            if (Build.VERSION.SDK_INT<=Build.VERSION.SDK_INT){
                if (dialog instanceof ProgressDialog|| dialog instanceof DatePickerDialog){
                    //getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
            }


           // setCancelable(false);
            Window window=getDialog().getWindow();
            assert window != null;
            WindowManager.LayoutParams layoutParams=window.getAttributes();
            layoutParams.dimAmount=0.0f;
            window.setGravity(Gravity.CENTER);
            window.setLayout((int) (UIUtils.WHD()[0] *0.75), ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setAttributes(layoutParams);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       // getDialog().setTitle("选择性别");//添加标题
        //getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_sex_select, container);
        //TextView title = (TextView) view.findViewById(R.id.title);
       // title.setText("选择性别");
        return view;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mCancelListener!=null){
            mCancelListener.onCancel();
        }
    }
}

package com.lsjr.zizi.mvp.home.zichat.dialog

import android.app.Dialog
import android.app.DialogFragment
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/22 10:07
 */

class ComonDialogFragment : DialogFragment(){

    interface OnDialogCancelListener{
        fun onCancel();
    }

    public interface OnCallDialog{
         fun getDialog(context: Context): ProgressDialog;
    }

    public interface IDialogResult<T>{
        fun onDataResult(result: T);
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    var onDialogCancleListener: OnDialogCancelListener = null!!
    var onCallDialog: OnCallDialog? = null;

     fun newInstance(call: OnCallDialog, cancle:Boolean):
             ComonDialogFragment {
        return newInstance(call,cancle, null!!);
    }

     fun newInstance(call: OnCallDialog, cancle:Boolean, onDialogCancelListener: OnDialogCancelListener):
             ComonDialogFragment {
        val instance = ComonDialogFragment();
        instance.isCancelable = cancle;
        instance.onCallDialog=call;
        instance.onDialogCancleListener=onDialogCancleListener;
        return instance;
    }

    override fun onStart() {
        super.onStart()
        val window=dialog.window;
        val windowParams=window.attributes;
        windowParams.dimAmount=0.0f;
        window.attributes=windowParams;
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        onDialogCancleListener.onCancel();
    }

}



package com.lsjr.zizi.mvp.home.zichat.dialog

import android.app.FragmentManager
import android.app.ProgressDialog
import android.content.Context
import com.lsjr.zizi.R
import com.lsjr.zizi.mvp.home.zichat.dialog.ComonDialogFragment.OnCallDialog

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/22 11:18
 */
class DialogFragmentHelper{
    val TAG: String = DialogFragmentHelper::class.simpleName!!


    public fun showProgress(fragmentManager: FragmentManager,message:String)
    :ComonDialogFragment{
        return showProgress(fragmentManager,message,true);
    }

    public fun showProgress(fragmentManager: FragmentManager, message: String,
                            cancelable:Boolean) :ComonDialogFragment{
        return showProgress(fragmentManager,message,true, null!!);
    }

    public fun showProgress(fragmentManager: FragmentManager,message: String,
                            cancelable:Boolean,cancelListener : ComonDialogFragment.OnDialogCancelListener)
    :ComonDialogFragment{
        val dialogFragment = ComonDialogFragment();
        val dialogCall=DialogTest(message);
        val newInstance = dialogFragment.newInstance(dialogCall, cancelable, cancelListener);
        dialogFragment.show(fragmentManager, TAG);
        return dialogFragment;
        //val newInstance1 = dialogFragment.newInstance(dialogCall, cancelable, cancelListener);

    }


    class DialogTest : OnCallDialog {
        val messag:String = "";
        val DIALOG_THEME:Int = R.style.AlertDialog_Theme;
        constructor(message: String)

        override fun getDialog(context: Context) :ProgressDialog{
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            val progressDialog = ProgressDialog(context, DIALOG_THEME)
            progressDialog.setMessage(messag)
            return progressDialog;
        }



    }
}
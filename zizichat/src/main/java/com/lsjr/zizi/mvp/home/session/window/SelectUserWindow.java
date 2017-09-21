package com.lsjr.zizi.mvp.home.session.window;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/19 10:03
 */

public class SelectUserWindow extends PopupWindow{
    private Button mCopy;
    private Button mInstant;
    private Button mCancle;
    private Button mDelete;


   public void initWindowParme(){
       this.setContentView(getContentView());
       this.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
       this.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
       this.setFocusable(true);
       ColorDrawable dw=new ColorDrawable(0xb000000);
       this.setBackgroundDrawable(dw);

   }

    @Override
    public View getContentView() {
        return super.getContentView();

    }
}

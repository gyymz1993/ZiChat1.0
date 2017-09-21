package com.ipaulpro.afilechooser;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.sk.weichat.R;

public class SelectFileWindow extends PopupWindow {
    private Button  mInstant, mCancle, mDelete,mInfo;
    private View mMenuView;
    int type;


    public SelectFileWindow(Context context, View.OnClickListener itemsOnClick) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuView = inflater.inflate(R.layout.file_instant, null);
        mInstant = (Button) mMenuView.findViewById(R.id.btn_instant);
        mCancle = (Button) mMenuView.findViewById(R.id.btn_cancle);
        mDelete = (Button) mMenuView.findViewById(R.id.btn_delete);
        mInfo = (Button) mMenuView.findViewById(R.id.btn_information);
        this.type = type;
        // 取消按钮
		/*
		 * btn_cancel.setOnClickListener(new OnClickListener() {
		 *
		 * public void onClick(View v) { //销毁弹出框 dismiss(); } });
		 */
        // 设置按钮监听
        mInstant.setOnClickListener(itemsOnClick);
        mCancle.setOnClickListener(itemsOnClick);
        mDelete.setOnClickListener(itemsOnClick);
        mInfo.setOnClickListener(itemsOnClick);
        // 设置SelectPicPopupWindow的View
        this.setContentView(mMenuView);
        // 设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        // 设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        // 设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        // 设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.Buttom_Popwindow);
        // 实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0xb0000000);
        // 设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);
        // mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
        mMenuView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
                int bottom = mMenuView.findViewById(R.id.pop_layout).getBottom();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();
                    } else if (y > bottom) {
                        dismiss();
                    }

                }
                return true;
            }
        });

    }

}

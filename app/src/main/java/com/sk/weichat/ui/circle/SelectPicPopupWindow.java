package com.sk.weichat.ui.circle;

import com.sk.weichat.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;

public class SelectPicPopupWindow extends PopupWindow {
	  private Button mSend_text,mSend_picture,mSend_voice,mSend_video;
	    private View mMenuView;
	 
	    public SelectPicPopupWindow(FragmentActivity context,OnClickListener itemsOnClick) {
	        super(context);
	        LayoutInflater inflater = (LayoutInflater) context
	                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        mMenuView = inflater.inflate(R.layout.business_dialog, null);
	        mSend_text = (Button) mMenuView.findViewById(R.id.btn_send_text);
	        mSend_picture = (Button) mMenuView.findViewById(R.id.btn_send_picture);
	        mSend_voice = (Button) mMenuView.findViewById(R.id.btn_send_voice);
	        mSend_video = (Button) mMenuView.findViewById(R.id.btn_send_video);
	        //取消按钮
	      /*  btn_cancel.setOnClickListener(new OnClickListener() {
	 
	            public void onClick(View v) {
	                //销毁弹出框
	                dismiss();
	            }
	        });*/
	        //设置按钮监听
	        mSend_text.setOnClickListener(itemsOnClick);
	        mSend_picture.setOnClickListener(itemsOnClick);
	        mSend_voice.setOnClickListener(itemsOnClick);
	        mSend_video.setOnClickListener(itemsOnClick);
	        //设置SelectPicPopupWindow的View
	        this.setContentView(mMenuView);
	        //设置SelectPicPopupWindow弹出窗体的宽
	        this.setWidth(LayoutParams.MATCH_PARENT);
	        //设置SelectPicPopupWindow弹出窗体的高
	        this.setHeight(LayoutParams.WRAP_CONTENT);
	        //设置SelectPicPopupWindow弹出窗体可点击
	        this.setFocusable(true);
	        //设置SelectPicPopupWindow弹出窗体动画效果
	        this.setAnimationStyle(R.style.Buttom_Popwindow);
	        //实例化一个ColorDrawable颜色为半透明
	        ColorDrawable dw = new ColorDrawable(0xb0000000);
	        //设置SelectPicPopupWindow弹出窗体的背景
	        this.setBackgroundDrawable(dw);
	        //mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
	        mMenuView.setOnTouchListener(new OnTouchListener() {
	             
	            public boolean onTouch(View v, MotionEvent event) {
	                 
	                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
	                int bottom = mMenuView.findViewById(R.id.pop_layout).getBottom();
	                int y=(int) event.getY();
	                if(event.getAction()==MotionEvent.ACTION_UP){
	                    if(y<height){
	                        dismiss();
	                    }else if(y>bottom){
	                    	dismiss();
	                    }
	                }               
	                return true;
	            }
	        });
	 
	    }
}

package com.lsjr.zizi.mvp.home.zichat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;

import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.mvp.home.zichat.presenter.UpdateSource;
import com.lsjr.zizi.mvp.listener.MyEditTextChangeListener;
import com.lsjr.zizi.view.ClearEditText;
import com.ymz.baselibrary.utils.T_;

import org.greenrobot.eventbus.EventBus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/16 14:31
 */

@SuppressLint("Registered")
public class UpdateSourceActivity extends MvpActivity<UpdateSource.Presenter> implements UpdateSource.IView {
    @BindView(R.id.up_edit)
    ClearEditText upEdit;
    String titleContent;
    private  int UPDATA_KEY=1;
    private  static final int UPDATA_ROOM_NAME=1;
    private  static final int UPDATA__FEATURE_NAME=2;
    private  static final int UPDATA_NICK_NAME=3;
    private  static final int UPDAT_FRIEND_RKNAME=4;
    private  static final int UPDAT_USER_NAME=5;

    private Friend mroom;
    private Friend friend;
    private String roomId;
    @Override
    protected UpdateSource.Presenter createPresenter() {
        return new UpdateSource.Presenter(this, this);
    }


    @Override
    protected void initTitle() {
        super.initTitle();
        if (!TextUtils.isEmpty(titleContent)){
            setTitleText(titleContent);
        }else {
            setTitleText("编辑群名称");
        }
        getToolBarView().getLeftimageView().setVisibility(View.GONE);
        getToolBarView().getLeftTextView().setVisibility(View.VISIBLE);
        getToolBarView().getLeftTextView().setText("取消");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras!=null){
            titleContent = extras.getString("updateContent");
            UPDATA_KEY = extras.getInt("key",0);
            mroom= (Friend) getIntent().getSerializableExtra("room");
            friend= (Friend) getIntent().getSerializableExtra("Friend");
            roomId=  extras.getString("roomId");
        }
        super.onCreate(savedInstanceState);

    }

    @Override
    protected int getLayoutId() {
        return R.layout.ac_update_source;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

        getToolBarView().getLeftTextView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               finish();
            }
        });
        getToolBarView().getRightTextView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //T_.showToastReal("打算修改"+UPDATA_KEY);
                String input = upEdit.getText().toString();
                if (TextUtils.isEmpty(input)){
                    T_.showToastReal("请输入名称");
                    return;
                }
                Pattern pattern = Pattern.compile("[0-9a-zA-Z\u4E00-\u9FA5]+");
                Matcher matcher = pattern.matcher(input);
                if (!matcher.matches() ) {
                    T_.showToastReal("有特殊字符");
                    return;
                }
                switch (UPDATA_KEY){
                    case 0:
                        return;
                    case UPDATA_ROOM_NAME:
                        mvpPresenter.updateRoom(mroom,upEdit.getText().toString(),null,null,roomId);
                        break;
                    case UPDATA__FEATURE_NAME:
                        mvpPresenter.updateRoom(mroom,null,null,upEdit.getText().toString(),roomId);
                        break;
                    case UPDATA_NICK_NAME:
                        mvpPresenter.updateNickName(mroom,upEdit.getText().toString());
                        break;
                    case UPDAT_FRIEND_RKNAME:
                        mvpPresenter.remarkFriend(friend,upEdit.getText().toString());
                        break;
                    case UPDAT_USER_NAME:
                        mvpPresenter.updateUserMarkName(upEdit.getText().toString());
                        break;
                }
            }
        });

        upEdit.addTextChangedListener(new MyEditTextChangeListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0 && s.toString() != null) {
                    getToolBarView().getRightTextView().setVisibility(View.VISIBLE);
                    getToolBarView().getRightTextView().setText("确定");
                } else {
                    getToolBarView().getRightTextView().setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void updateSucceed(String content) {
       if (UPDATA_KEY==UPDATA_ROOM_NAME||UPDATA_KEY==UPDAT_FRIEND_RKNAME){
           EventBus.getDefault().post(content);
       }
        Intent intent = new Intent();
        if (TextUtils.isEmpty(content))return;
        intent.putExtra("udaptes",content);
        intent.putExtra("key",UPDATA_KEY);
        setResult(RESULT_OK,intent);
        finish();
    }

    @Override
    public void updateNickNameFail() {

    }
}

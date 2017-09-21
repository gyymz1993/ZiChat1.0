package com.lsjr.zizi.mvp.home.zichat.presenter;

import android.app.ProgressDialog;
import android.text.TextUtils;

import com.lsjr.bean.ObjectResult;
import com.lsjr.bean.Result;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.broad.CardcastUiUpdateUtil;
import com.lsjr.zizi.chat.broad.MsgBroadcast;
import com.lsjr.zizi.chat.dao.ChatMessageDao;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.dao.UserDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.db.User;
import com.lsjr.zizi.chat.xmpp.ListenerManager;
import com.lsjr.zizi.mvp.circledemo.MyApplication;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.HomeActivity;
import com.lsjr.zizi.mvp.home.session.BasicInfoActivity;
import com.lsjr.zizi.mvp.home.zichat.UpdateSourceActivity;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.HashMap;

import javax.xml.transform.ErrorListener;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/16 14:31
 */

public interface UpdateSource {
    interface IView{
        void updateSucceed(String content);
        void updateNickNameFail();
    }

    class Presenter extends BasePresenter<UpdateSource.IView> {
        UpdateSourceActivity sourceActivity;
        public Presenter( UpdateSourceActivity sourceActivity,IView mvpView) {
            super(mvpView);
            this.sourceActivity=sourceActivity;
        }

        //private Friend mRoom;
        public void updateNickName(Friend mRoom, final String nickName) {
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            params.put("roomId", mRoom.getRoomId());
            params.put("userId", ConfigApplication.instance().getLoginUserId());
            params.put("nickname", nickName);
           // showProgressDialogWithText("更改昵称中");
            HttpUtils.getInstance().postServiceData(AppConfig.ROOM_MEMBER_UPDATE, params, new ChatObjectCallBack<Void>(Void.class) {
                @Override
                protected void onXError(String exception) {
                    //dismissProgressDialog();
                }

                @Override
                protected void onSuccess(ObjectResult<Void> result) {
                    //dismissProgressDialog();
                    boolean success = ResultCode.defaultParser(result, true);
                    if (success) {
                        //myName.setRightText(nickName);
                        String loginUserId = ConfigApplication.instance().mLoginUser.getUserId();
                        FriendDao.getInstance().updateNickName(loginUserId, mRoom.getUserId(), nickName);
                        ChatMessageDao.getInstance().updateNickName(loginUserId, mRoom.getUserId(), loginUserId, nickName);
                        mRoom.setRoomMyNickName(nickName);
                        FriendDao.getInstance().createOrUpdateFriend(mRoom);
                        ListenerManager.getInstance().notifyNickNameChanged(mRoom.getUserId(), loginUserId, nickName);
                        mvpView.updateSucceed(nickName);
                    }
                }

            });

        }



        public void remarkFriend(Friend mFriend,final String remarkName) {
            if(mFriend==null){
                return;
            }
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            params.put("toUserId", mFriend.getUserId());
            params.put("remarkName", remarkName);
            //showProgressDialogWithText("请稍等");
            HttpUtils.getInstance().postServiceData(AppConfig.CONFIG_URL, params, new ChatObjectCallBack<Result>(Result.class) {
                @Override
                protected void onXError(String exception) {
                   // dismissProgressDialog();
                    L_.e(exception);
                }

                @Override
                protected void onSuccess(ObjectResult<Result> result) {
                   // dismissProgressDialog();
                    boolean success = ResultCode.defaultParser( result, true);
                    if (success) {
                        // 更新到数据库
                        L_.e("设置好友备注-----------》"+mFriend.toString()+"---->"+remarkName);
                        FriendDao.getInstance().setRemarkName(ConfigApplication.instance().getLoginUserId(), mFriend.getUserId(), remarkName);
                        //mTvName.setText(remarkName);
                        mFriend.setRemarkName(remarkName);
                        // 更新消息界面（因为昵称变了，所有要更新）
                        L_.e("发送广播-----》");
                        MsgBroadcast.broadcastMsgUiUpdate(UIUtils.getContext());
                        CardcastUiUpdateUtil.broadcastUpdateUi(UIUtils.getContext());
                        mvpView.updateSucceed(remarkName);
                    }
                }

            });

        }


        public void updateUserMarkName(String input) {
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            User mLoginUser = ConfigApplication.instance().mLoginUser;
            if (!ConfigApplication.instance().mLoginUser.getNickName().equals(input)) {
                params.put("nickname", input);
            }
            params.put("sex", String.valueOf(mLoginUser.getSex()));
            params.put("birthday", String.valueOf(mLoginUser.getBirthday()));
            params.put("countryId", String.valueOf(mLoginUser.getCountryId()));
            params.put("provinceId", String.valueOf(mLoginUser.getProvinceId()));
            params.put("cityId", String.valueOf(mLoginUser.getCityId()));
            params.put("areaId", String.valueOf(mLoginUser.getAreaId()));
            //showProgressDialogWithText("更新中");
            HttpUtils.getInstance().postServiceData(AppConfig.USER_UPDATE, params, new ChatObjectCallBack<Void>(Void.class) {

                @Override
                protected void onXError(String exception) {
                   // dismissProgressDialog();
                }

                @Override
                protected void onSuccess(ObjectResult<Void> result) {
                  //  dismissProgressDialog();
                    boolean success = ResultCode.defaultParser( result, true);
                    if (success) {
                        ConfigApplication.instance().mLoginUser.setNickName(input);
                        UserDao.getInstance().updateNickName(mLoginUser.getUserId(), input);// 更新数据库
                        // mTvName.setText(input);
                       // oivAliasName.setRightText(ConfigApplication.instance().mLoginUser.getNickName());
                        mvpView.updateSucceed(input);
                    }
                }
            });

        }



        public void updateRoom(Friend mRoom,String roomName, final String roomNotice, final String roomDes, final String roomId) {
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            params.put("roomId", roomId);
            if (!TextUtils.isEmpty(roomName)) {
                params.put("roomName",roomName);
            }
            if (!TextUtils.isEmpty(roomNotice)) {
                params.put("notice", roomNotice);
            }
            if (!TextUtils.isEmpty(roomDes)) {
                params.put("desc", roomDes);
            }

            HttpUtils.getInstance().postServiceData(AppConfig.ROOM_UPDATE, params, new ChatObjectCallBack<Void>(Void.class) {

                @Override
                protected void onXError(String exception) {
                    // dismissProgressDialog();
                }

                @Override
                protected void onSuccess(ObjectResult<Void> result) {
                    //  dismissProgressDialog();
                    boolean success = ResultCode.defaultParser( result, true);
                    if (success) {
                        String updataStr = "";
                        if (!TextUtils.isEmpty(roomName)) {
                            //mRoomNameTv.setText(roomName);
                            mRoom.setNickName(roomName);
                            updataStr=roomName;
                            // 不去存入数据库，因为修改了房间名称后，会发一条推送，处理这条推送即可
                        }
                        if (!TextUtils.isEmpty(roomNotice)) {
                            // 修改了notice，也会有推送过来
                           // mNoticeTv.setText(roomNotice);
                            updataStr=roomNotice;
                            //mRoom.setDescription(roomNotice);
                        }
                        if (!TextUtils.isEmpty(roomDes)) {
                            //mRoomDescTv.setText(roomDes);
                            mRoom.setDescription(roomDes);
                            updataStr=roomDes;
                            // 更新数据库

                        }
                        if (!TextUtils.isEmpty(updataStr)){
                            FriendDao.getInstance().createOrUpdateFriend(mRoom);
                            mvpView.updateSucceed(updataStr);
                        }

                    }
                }
            });
        }

    }
}

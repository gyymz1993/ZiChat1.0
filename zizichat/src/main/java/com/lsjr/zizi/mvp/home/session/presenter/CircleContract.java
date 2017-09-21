package com.lsjr.zizi.mvp.home.session.presenter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;

import com.alibaba.fastjson.JSON;
import com.lsjr.bean.ArrayResult;
import com.lsjr.bean.ObjectResult;
import com.lsjr.callback.ChatArrayCallBack;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.bean.Comment;
import com.lsjr.zizi.chat.bean.PublicMessage;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.dao.CircleMessageDao;
import com.lsjr.zizi.mvp.circledemo.bean.CommentConfig;
import com.lsjr.zizi.mvp.circledemo.bean.CommentItem;
import com.lsjr.zizi.mvp.home.session.FriendCircleActivity;
import com.lsjr.zizi.mvp.home.session.cicle.CircleConstact;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface CircleContract {

    interface CircleView{
        void update2AddComment(int circlePosition, CommentItem addItem);
        void update2DeleteComment(int circlePosition, String commentId);
        void updateEditTextBodyVisible(int visibility, CommentConfig commentConfig);

        void notifityChange();


        void onfaile();
        void notifityChange(List<PublicMessage> mMessages);
    }

    class CirclePresenter extends BasePresenter<CircleView>  {
        String mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        String mLoginNickName = ConfigApplication.instance().mLoginUser.getNickName();
        private FriendCircleActivity activity;
        public CirclePresenter(FriendCircleActivity activity, CircleView mvpView) {
            super(mvpView);
            this.activity=activity;
        }

        public CirclePresenter(CircleView mvpView) {
            super(mvpView);
        }

        /**
         *
         * @param commentConfig
         */
        public void showEditTextBody(CommentConfig commentConfig){
            if(mvpView != null){
                mvpView.updateEditTextBodyVisible(View.VISIBLE, commentConfig);
            }
        }

        public void deleteMsg(int position) {
            final PublicMessage message = CircleConstact.getCircleConstact().getCurrentPublicMessage();
            if (message == null) {
                return;
            }
            HashMap<String, String> params = new HashMap<>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            params.put("messageId", message.getMessageId());
            HttpUtils.getInstance().postServiceData(AppConfig.CIRCLE_MSG_DELETE, params, new ChatObjectCallBack<Void>(Void.class) {
                @Override
                protected void onXError(String exception) {

                }

                @Override
                protected void onSuccess(ObjectResult<Void> result) {
                    boolean success = ResultCode.defaultParser(result, true);
                    if (success) {
                        CircleMessageDao.getInstance().deleteMessage(message.getMessageId());// 删除数据库的记录（如果存在的话）
                        activity.mMessages.remove(message);
                        mvpView.notifityChange();
                    }
                }
            });

        }


        public void addCommentUser(String context,PublicMessage selectCurrentMessage){

            Comment comment;
            if (selectCurrentMessage==null){
                T_.showToastReal("请填写完整数据");
                return;
            }
            if (CircleConstact.getCircleConstact().getConfig().commentType==CommentConfig.Type.PUBLIC){
                L_.e("回复自己");
                comment = new Comment();
                comment.setToUserId(selectCurrentMessage.getUserId());
                comment.setToNickname(selectCurrentMessage.getNickName());
            }else {
                L_.e("回复评论区");
                comment = CircleConstact.getCircleConstact().getCurrentComment();
                comment.setToUserId(comment.getUserId());
                comment.setToNickname(comment.getNickName());
            }

            comment.setUserId(mLoginUserId);
            comment.setNickName(mLoginNickName);
            comment.setBody(context);
            comment.setMsgId(selectCurrentMessage.getMessageId());
            L_.e(comment.toString());
            L_.e("回复需要的字段"+comment.toString()+"messageId"+selectCurrentMessage.getMessageId());
            L_.e(context);
            addComment(comment);
        }


        /**
         * 新一条回复  添加一条评论的操作
         */
        private void addComment(final Comment comment) {
            Map<String, String> params = new HashMap<>();
            params.put("access_token",ConfigApplication.instance().mAccessToken);
            params.put("messageId", comment.getMsgId());
            if (!TextUtils.isEmpty(comment.getToUserId())) {
                params.put("toUserId", comment.getToUserId());
            }
            if (!TextUtils.isEmpty(comment.getToNickname())) {
                params.put("toNickname", comment.getToNickname());
            }
            params.put("body", comment.getBody());

            HttpUtils.getInstance().postServiceData(AppConfig.MSG_COMMENT_ADD, params, new ChatObjectCallBack<String>(String.class) {
                @Override
                protected void onXError(String exception) {
                }
                @Override
                protected void onSuccess(ObjectResult<String> result) {
                    boolean success = ResultCode.defaultParser(result, true);
                    if (success && result.getData() != null) {
                        List<Comment> comments = CircleConstact.getCircleConstact().getCurrentComments();
                        if (comments == null) {
                            comments = new ArrayList<>();
                            CircleConstact.getCircleConstact().getCurrentPublicMessage().setComments(comments);
                        }
                        comment.setCommentId(result.getData());
                        comments.add(0, comment);
                        L_.e("评论区  刷新"+comment.toString());
                        mvpView.notifityChange();
                    }
                }
            });

        }



        /* 操作事件 */
        public void showDeleteMsgDialog(final int position) {
            new AlertDialog.Builder(activity).setTitle(R.string.prompt_title).setMessage(R.string.delete_prompt)
                    .setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteMsg(position);
                        }
                    }).setNegativeButton(R.string.cancel, null).create().show();
        }


        private int mPageIndex ;
        public void requestMyBusiness() {
            mPageIndex = 0;
            List<String> msgIds = CircleMessageDao.getInstance().getCircleMessageIds(mLoginUserId, mPageIndex, AppConfig.PAGE_SIZE);
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            params.put("ids", JSON.toJSONString(msgIds));
            if (msgIds.size() == 0)return;
            HttpUtils.getInstance().postServiceData(AppConfig.MSG_GETS, params, new ChatArrayCallBack<PublicMessage>(PublicMessage.class) {
                @Override
                protected void onXError(String exception) {
                    L_.e("朋友圈。。。。");
                    //T_.showToastWhendebug(exception);
                    if (mvpView!=null){
                        mvpView.onfaile();
                    }
                   // recyclerView.setRefreshing(false);
                }

                @Override
                protected void onSuccess(ArrayResult<PublicMessage> result) {
                    L_.e(result.getData().toString());
                   // recyclerView.setRefreshing(false);
                    //mMessages = ;
                    mvpView.notifityChange(result.getData());
                   // firstNotifyAdapter();
                }
            });

        }

//        void loadData(int loadType);
//        void deleteCircle(final String circleId);
//        void addFavort(final int circlePosition);
//        void deleteFavort(final int circlePosition, final String favortId);
//        void deleteComment(final int circlePosition, final String commentId);

    }
}

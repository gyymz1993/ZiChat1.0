package com.lsjr.zizi.mvp.home.session.cicle;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.Praise;
import com.lsjr.zizi.mvp.circledemo.MyApplication;
import com.lsjr.zizi.mvp.circledemo.utils.DatasUtil;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.bean.Comment;
import com.lsjr.zizi.chat.bean.PublicMessage;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.circledemo.bean.ActionItem;
import com.lsjr.zizi.mvp.circledemo.bean.CommentConfig;
import com.lsjr.zizi.mvp.circledemo.widgets.ExpandTextView;
import com.lsjr.zizi.mvp.circledemo.widgets.PraiseListView;
import com.lsjr.zizi.mvp.circledemo.widgets.SnsPopupWindow;
import com.lsjr.zizi.mvp.home.session.presenter.CircleContract;
import com.lsjr.zizi.util.TimeUtils;
import com.lsjr.zizi.view.CommentView;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ys.uilibrary.base.RVBaseAdapter;
import com.ys.uilibrary.base.RVBaseCell;
import com.ys.uilibrary.base.RVBaseViewHolder;

import java.util.List;


/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/16 11:55
 */

public abstract class ACicleAdapter extends RVBaseCell<PublicMessage> {
    /**
     * 消息类型
     */
    static final int TYPE_URL = 1;// 文字类型
    static final int TYPE_IMG = 2;// 图片类型
    static final int TYPE_VOICE = 3;// 语音
    public static final int TYPE_VIDEO = 4;// 视频
    //public final static int TYPE_URL = 5;

    public int viewType;

    ImageView headIv;
    TextView nameTv;
    TextView urlTipTv;
    /**
     * 动态的内容
     */
    ExpandTextView contentTv;
    TextView timeTv;
    TextView deleteBtn;
    ImageView snsBtn;

    LinearLayout digCommentBody;
    View digLine;

    CircleContract.CirclePresenter circlePresenter;

    View vLine;

    // ===========================
    public SnsPopupWindow snsPopupWindow;

    private PublicMessage publicMessage;

    private RVBaseAdapter mRvBaseAdapter;

    private List<Comment> comments;
    /* 显示多少人赞过 */
    List<Praise> praises;

    public ACicleAdapter(PublicMessage publicMessage) {
        super(publicMessage);
        this.publicMessage = publicMessage;


    }

    public ACicleAdapter(PublicMessage publicMessage, RVBaseAdapter rvBaseAdapter) {
        super(publicMessage);
        this.publicMessage = publicMessage;
        this.mRvBaseAdapter=rvBaseAdapter;
    }

    public ACicleAdapter(PublicMessage publicMessage, RVBaseAdapter rvBaseAdapter, CircleContract.CirclePresenter circlePresenter) {
        super(publicMessage);
        this.publicMessage = publicMessage;
        this.mRvBaseAdapter=rvBaseAdapter;
        this.circlePresenter=circlePresenter;


    }

    @Override
    public RVBaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_circle_item, parent, false);
        ViewStub viewStub = (ViewStub) itemView.findViewById(R.id.viewStub);
        switch (getItemType()) {
            case TYPE_URL:
                viewStub.setLayoutResource(R.layout.viewstub_urlbody);
                break;
            case TYPE_IMG:
                viewStub.setLayoutResource(R.layout.viewstub_imgbody);
                break;
            case TYPE_VIDEO:
                viewStub.setLayoutResource(R.layout.viewstub_videobody1);
                break;
        }
        viewStub.inflate();
        return new RVBaseViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RVBaseViewHolder holder, int mCirclePosition) {
        initSubView(holder,mCirclePosition);
        headIv = (ImageView) holder.getView(R.id.headIv);
        nameTv = (TextView) holder.getView(R.id.nameTv);
        digLine = holder.getView(R.id.lin_dig);
        contentTv = (ExpandTextView) holder.getView(R.id.contentTv);
        urlTipTv = (TextView) holder.getView(R.id.urlTipTv);
        timeTv = (TextView) holder.getView(R.id.timeTv);
        deleteBtn = (TextView) holder.getView(R.id.deleteBtn);
        snsBtn = (ImageView) holder.getView(R.id.snsBtn);
        /*
        点赞列表
        */
        PraiseListView praiseListView = (PraiseListView) holder.getView(R.id.praiseListView);
        digCommentBody = (LinearLayout) holder.getView(R.id.digCommentBody);
        /*
      评论列表
        */
        CommentView commentList = (CommentView) holder.getView(R.id.commentList);
        vLine= (View) holder.getView(R.id.vLine);

        if (mRvBaseAdapter!=null&&mRvBaseAdapter.getItemCount()>0){
            if (mCirclePosition==mRvBaseAdapter.getItemCount()-1){
                vLine.setVisibility(View.GONE);
            }
        }
        //点赞和评论之间的线条
        digLine.setVisibility(View.GONE);

        //分享萧条
        urlTipTv.setVisibility(View.GONE);

        /*点赞*/
        digCommentBody.setVisibility(View.GONE);
        praiseListView.setVisibility(View.GONE);

        final PublicMessage message = publicMessage;
        PublicMessage.Body body = message.getBody();
        // 设备头像
        Friend friend=new Friend();
        friend.setNickName(message.getNickName());
        friend.setUserId(message.getUserId());
        AvatarHelper.getInstance().displayAvatar(friend, headIv, true);
        /* 设置昵称 */
        nameTv.setText(message.getNickName());
        // 设置body_tv
        if (TextUtils.isEmpty(body.getText())) {
            contentTv.setVisibility(View.GONE);
        } else {
            contentTv.setVisibility(View.VISIBLE);
            contentTv.setText(body.getText());

        }
        // 设置发布时间
        timeTv.setText(TimeUtils.getFriendlyTimeDesc(UIUtils.getContext(), (int) message.getTime()));
        if (ConfigApplication.instance().mLoginUser.getUserId().equals(message.getUserId())) {
            deleteBtn.setVisibility(View.VISIBLE);
            nameTv.setText("自己");
        } else {
            deleteBtn.setVisibility(View.GONE);
            deleteBtn.setOnClickListener(null);
        }


        /*点击点赞或者评论*/
        praises = message.getPraises();
        if(praises!=null&&praises.size()>0){//处理点赞列表
            praiseListView.setDatas(praises);
            praiseListView.setVisibility(View.VISIBLE);
            digCommentBody.setVisibility(View.VISIBLE);
        }else{
            praiseListView.setVisibility(View.GONE);
        }

        //评论区* 设置回复 */
        comments = message.getComments();
        if (comments != null && comments.size() > 0) {
            commentList.setDatas(comments);
            digCommentBody.setVisibility(View.VISIBLE);
            commentList.setVisibility(View.VISIBLE);
        } else {
            commentList.setVisibility(View.GONE);
        }


        snsPopupWindow = new SnsPopupWindow(UIUtils.getContext());
        //判断是否已点赞
        if(publicMessage.getIsPraise()==1){
            snsPopupWindow.getmActionItems().get(0).mTitle = "取消";
        }else{
            snsPopupWindow.getmActionItems().get(0).mTitle = "赞";
        }

        snsPopupWindow.update();
        snsPopupWindow.setmItemClickListener(new PopupItemClickListener(mCirclePosition));

        setItmeOnclick(holder,mCirclePosition);

    }


    private void setItmeOnclick(RVBaseViewHolder holder, int mCirclePosition){

        /* 回复模块*/
         holder.getView(R.id.snsBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CircleConstact.getCircleConstact().setCurrentPublicMessage(publicMessage);
                CircleConstact.getCircleConstact().setPosition(mCirclePosition);
                CircleConstact.getCircleConstact().setCurrentComments(comments);
                //L_.e("当前的："+CircleConstact.getCircleConstact().toString());

                CommentConfig config = new CommentConfig();
                config.circlePosition = CircleConstact.getCircleConstact().getPosition();
                config.commentType = CommentConfig.Type.PUBLIC;
                CircleConstact.getCircleConstact().setConfig(config);
                //弹出popupwindow
                snsPopupWindow.showPopupWindow(view);
                //回复公共部门
            }
        });

        CommentView commentView = (CommentView) holder.getView(R.id.commentList);
        /*点击评论列表*/
        commentView.setOnItemClickListener(new CommentView.OnItemClickListener() {
            @Override
            public void onItemClick(int commentPosition) {
                // PublicMessage publicMessage=new PublicMessage();
                CircleConstact.getCircleConstact().setCurrentPublicMessage(publicMessage);
                CircleConstact.getCircleConstact().setPosition(mCirclePosition);
                CircleConstact.getCircleConstact().setCurrentComments(comments);

                T_.showToastReal("回复别人");
                Comment comment = comments.get(commentPosition);
                CircleConstact.getCircleConstact().setCurrentComment(comment);
                //L_.e("当前的："+CircleConstact.getCircleConstact().toString());

                //回复别人的评论
                CommentConfig config = new CommentConfig();
                config.circlePosition = mCirclePosition;
                config.commentPosition = commentPosition;
                config.commentType = CommentConfig.Type.REPLY;
                CircleConstact.getCircleConstact().setConfig(config);
                L_.e("回复别人"+circlePresenter);
                if (circlePresenter!=null){
                    circlePresenter.showEditTextBody(config);
                }
            }
        });


        /*删除模块*/
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CircleConstact.getCircleConstact().setCurrentPublicMessage(publicMessage);
                circlePresenter.showDeleteMsgDialog(mCirclePosition);
            }
        });
    }

    private class PopupItemClickListener implements SnsPopupWindow.OnItemClickListener {
        private long mLasttime = 0;
        private int circlePosition;

        public PopupItemClickListener(int circlePosition) {
            this.circlePosition = circlePosition;
        }

        @Override
        public void onItemClick(ActionItem actionitem, int position) {
            switch (position) {
                case 0://点赞、取消点赞
                    if (System.currentTimeMillis() - mLasttime < 700)//防止快速点击操作
                        return;
                    mLasttime = System.currentTimeMillis();
                    if(circlePresenter != null){
                        int circlePosition = CircleConstact.getCircleConstact().getPosition();
                        CircleConstact.getCircleConstact().setCurrentPublicMessage(publicMessage);
                        if (publicMessage.getIsPraise()==1) {
                            circlePresenter.praiseOrCancle(false);
                        } else {//取消点赞
                            circlePresenter.praiseOrCancle( true);
                        }
                    }
                    break;
                case 1://发布评论
                    CommentConfig config = CircleConstact.getCircleConstact().getConfig();
                    if (circlePresenter!=null){
                        circlePresenter.showEditTextBody(config);
                    }
                    break;
                default:
                    break;
            }
        }
    }




//    public void setOnCommentListener(OnCommentListener onCommentListener) {
//        this.onCommentListener = onCommentListener;
//    }
//
//    OnCommentListener onCommentListener;
//    public interface  OnCommentListener{
//        void onCommentSucceed();
//    }

    public void setShowEditTextListener(ShowEditTextListener showEditTextListener) {
        this.showEditTextListener = showEditTextListener;
    }

   public ShowEditTextListener showEditTextListener;

    public interface ShowEditTextListener {
        void updateEditTextBodyVisible(int visible);
    }

    @Override
    public int getItemType() {
        return publicMessage.getBody().getType();
    }

    public abstract void initSubView(RVBaseViewHolder viewStub,int position);


}

package com.lsjr.zizi.mvp.home.session;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.barlibrary.ImmersionBar;
import com.bumptech.glide.Glide;
import com.cjt2325.cameralibrary.JCameraView;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.SwipeBackActivity;
import com.lsjr.zizi.chat.bean.PublicMessage;
import com.lsjr.zizi.chat.dao.CircleMessageDao;
import com.lsjr.zizi.chat.dao.MyPhotoDao;
import com.lsjr.zizi.chat.db.MyPhoto;
import com.lsjr.zizi.mvp.circledemo.bean.CommentConfig;
import com.lsjr.zizi.mvp.circledemo.utils.CommonUtils;
import com.lsjr.zizi.mvp.circledemo.widgets.DivItemDecoration;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.mvp.home.photo.TakePhotoActivity;
import com.lsjr.zizi.mvp.home.session.cicle.ACicleAdapter;
import com.lsjr.zizi.mvp.home.session.cicle.CircleConstact;
import com.lsjr.zizi.mvp.home.session.cicle.HeadCellAdapter;
import com.lsjr.zizi.mvp.home.session.cicle.ImageCellAdapter;
import com.lsjr.zizi.mvp.home.session.cicle.URLCellAdapter;
import com.lsjr.zizi.mvp.home.session.cicle.VideoCellAdapter1;
import com.lsjr.zizi.mvp.home.session.presenter.CircleContract;
import com.lsjr.zizi.view.CommentView;
import com.malinskiy.superrecyclerview.OnMoreListener;
import com.malinskiy.superrecyclerview.SuperRecyclerView;
import com.nostra13.universalimageloader.utils.L;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ymz.baselibrary.widget.NavigationBarView;
import com.ys.uilibrary.base.RVBaseAdapter;
import com.ys.uilibrary.base.RVBaseCell;
import com.ys.uilibrary.base.RVBaseViewHolder;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

import static com.lsjr.zizi.mvp.circledemo.widgets.TitleBar.getStatusBarHeight;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/15 16:40
 */

@SuppressLint("Registered")
public class FriendCircleActivity extends SwipeBackActivity<CircleContract.CirclePresenter> implements CircleContract.CircleView{
    @BindView(R.id.recyclerView)
    SuperRecyclerView recyclerView;
    @BindView(R.id.circleEt)
    EditText editText;
    @BindView(R.id.sendIv)
    ImageView sendIv;
    @BindView(R.id.editTextBodyLl)
    LinearLayout edittextbody;
    @BindView(R.id.bodyLayout)
    RelativeLayout bodyLayout;

    @BindView(R.id.id_bar_view1)
    NavigationBarView navigationBarView;

    private SwipeRefreshLayout.OnRefreshListener refreshListener;
    private String mLoginUserId;// 当前登陆用户的UserId
    public List<PublicMessage> mMessages = new ArrayList<>();
    /* 当前选择的是哪个用户的个人空间,仅用于查看个人空间的情况下 */
    /* mPageIndex仅用于商务圈情况下 */
    public final static int REQUEST_TAKE_PHOTO = 1001;
    private static final int REQUEST_CODE_SEND_MSG = 1;
    public List<RVBaseCell> cells;
    public ACicleAdapter aCicleAdapter = null;
    public LinearLayoutManager layoutManager;
    @Override
    protected CircleContract.CirclePresenter createPresenter() {
        return new CircleContract.CirclePresenter(this,this);
    }

    @Override
    protected void initTitle() {
        super.initTitle();
        navigationBarView.setTitleText("朋友圈");
        getToolBarView().setVisibility(View.GONE);
        navigationBarView.setVisibility(View.GONE);
        ImmersionBar.with(FriendCircleActivity.this).statusBarDarkFont(false, 1.0f).init();
        navigationBarView.setleftImageResource(UIUtils.getDrawable(R.drawable.ic_back));
        navigationBarView.getLeftimageView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected boolean isImmersionBarEnabled() {
        return true;
    }

    private void setBarEnabled(boolean isEnabled){
        if (isEnabled){
            initImmersionBar();
        }
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        mImmersionBar.keyboardEnable(true).init();
        mImmersionBar.fitsSystemWindows(false).transparentStatusBar().init();
    }


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_circle;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        if (TextUtils.isEmpty(mLoginUserId)) {// 容错
            return;
        }
        //实现自动下拉刷新功能
        recyclerView.getSwipeToRefresh().post(new Runnable() {
            @Override
            public void run() {
                recyclerView.setRefreshing(true);//执行下拉刷新的动画
                mvpPresenter.requestMyBusiness(mvpPresenter.ON_REFRESH);
                mvpPresenter.requestUserSpace(ConfigApplication.instance().getLoginUserId());
            }
        });

    }

    RVBaseAdapter rvBaseAdapter = new RVBaseAdapter() {
        @Override
        protected void onViewHolderBound(RVBaseViewHolder holder, int position) {

        }
    };

    @Override
    protected void initView() {

        setViewTreeObserver();
    }



    public void editTextBodyVisible(int visibility) {
        edittextbody.setVisibility(visibility);
        //showCommentEnterView();
        if (View.VISIBLE == visibility) {
            editText.requestFocus();
            //弹出键盘
            CommonUtils.showSoftInput(editText.getContext(), editText);
        } else if (View.GONE == visibility) {
            //隐藏键盘
            CommonUtils.hideSoftInput(editText.getContext(), editText);
        }
    }



    public void showTitle(int y){
        int imageHeight=70;
        if (y <= 0) {   //设置标题的背景颜色  0 透明度  （144,151,166）颜色:透明色
            getToolBarView().setTopBarBackgroundColor(Color.argb((int) 0, 255,255,255));
            getToolBarView().setVisibility(View.GONE);
        } else if (y > 0 && y <= imageHeight) { //滑动距离小于banner图的高度时，设置背景和字体颜色颜色透明度渐变
            float scale = (float) y / imageHeight;
            float alpha = (255 * scale);
            getToolBarView().setTopBarBackgroundColor(Color.argb((int) alpha, 255,255,255));
        } else {    //滑动到banner下面设置普通颜色
            getToolBarView().setTopBarBackgroundColor(Color.argb((int) 255, 255,255,255));
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initData() {
       // layoutManager = new LinearLayoutManager(this);

//        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
//
//            @Override
//            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
//                View viewByPosition = layoutManager.findViewByPosition(0);
//                int scrolled = super.scrollVerticallyBy(dy, recycler, state);
//                //View viewForPosition = recycler.getViewForPosition(0);
//                int itme_scrolled;
//                int titleHeight;
//                if (viewByPosition!=null){
//                    //L_.e("viewForPosition.getMeasuredHeight()"+viewByPosition.getMeasuredHeight());
//                    int firstItemPostion=layoutManager.findFirstVisibleItemPosition();
//                    int itemHeight=viewByPosition.getHeight();
//                    int firstItmeBontton=layoutManager.getDecoratedBottom(viewByPosition);
//                    itme_scrolled = UIUtils.px2dip((firstItemPostion + 1) * itemHeight - firstItmeBontton);
//                    titleHeight = UIUtils.px2dip(viewByPosition.getMeasuredHeight());
//                    if (titleHeight-itme_scrolled<15){
//                        ImmersionBar.with(FriendCircleActivity.this)
//                                .statusBarDarkFont(true, 0.0f)
//                                .init();
//                        navigationBarView.setVisibility(View.VISIBLE);
//                        navigationBarView.setTitleText("朋友圈");
//                        //在BaseActivity里初始化
//                    }else if(titleHeight-itme_scrolled>200){
//                        navigationBarView.setVisibility(View.GONE);
//                        ImmersionBar.with(FriendCircleActivity.this)
//                                .statusBarDarkFont(false, 1.0f)
//                                .init();
//                    }
//                }
//
//                return scrolled;
//            }
//        };
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DivItemDecoration(2, true));
        recyclerView.getMoreProgressView().getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (edittextbody.getVisibility() == View.VISIBLE) {
                    editTextBodyVisible(View.GONE);
                   // updateEditTextBodyVisible(View.GONE, null);
                    return true;
                }
                return false;
            }
        });


        refreshListener = () -> new Handler().postDelayed(() -> {
            mvpPresenter.requestMyBusiness(mvpPresenter.ON_REFRESH);
        }, 500);
        recyclerView.setRefreshListener(refreshListener);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Glide.with(UIUtils.getContext()).resumeRequests();
                } else {
                    Glide.with(UIUtils.getContext()).pauseRequests();
                }

            }
        });

        /*自定义RecycleView中多类型数据*/
        cells = new ArrayList<>();
        recyclerView.setAdapter(rvBaseAdapter);

        sendIv.setOnClickListener(v -> {
            //发布评论
            String content =  editText.getText().toString().trim();
            if(TextUtils.isEmpty(content)){
                T_.showToastReal("评论内容不能为空...");
                return;
            }
            //清空评论文本
            mvpPresenter.addCommentUser(content,CircleConstact.getCircleConstact().getCurrentPublicMessage());
            editText.setText("");
        });

    }



    private void loadPhotos() {
        List<MyPhoto> photos = MyPhotoDao.getInstance().getPhotos(mLoginUserId);
        // 别人的，那么就从网上请求
    }

    List<PublicMessage.Body> bodyList = new ArrayList<>();
    HeadCellAdapter headCellAdapter;
    public void firstNotifyAdapter() {
        cells.clear();
        headCellAdapter = new HeadCellAdapter(null);
        headCellAdapter.setOnTopBarClickLisetener(new HeadCellAdapter.onTopBarClickLisetener() {
            @Override
            public void onBack() {
                finish();
            }

            @Override
            public void onTalk() {
                Intent intent = new Intent(FriendCircleActivity.this, TakePhotoActivity.class);
                Bundle bundle=new Bundle();
                bundle.putInt("take", JCameraView.BUTTON_STATE_ONLY_RECORDER);
                intent.putExtras(bundle);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            }

            @Override
            public void onPhonto() {
                Intent intent = new Intent();
                intent.setClass(FriendCircleActivity.this, SendFriendCircleActivity.class);
                intent.putExtra("type", 0);
                startActivityForResult(intent, REQUEST_CODE_SEND_MSG);// 去发说说
            }
        });

        recyclerView.setupMoreListener(new OnMoreListener() {
            @Override
            public void onMoreAsked(int overallItemsCount, int itemsBeforeMore, int maxLastVisiblePosition) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //mvpPresenter.loadData(TYPE_UPLOADREFRESH);
                        if (mvpPresenter!=null){
                            mvpPresenter.requestMyBusiness(mvpPresenter.ON_LOAD);
                        }

                    }
                }, 2000);

            }
        }, 1);

        cells.add(headCellAdapter);
        for (int i = 0; i < mMessages.size(); i++) {
            PublicMessage publicMessage = mMessages.get(i);
            PublicMessage.Body body = publicMessage.getBody();
            bodyList.add(body);
            switch (body.getType()) {
                case PublicMessage.TYPE_TEXT:
                    aCicleAdapter = new URLCellAdapter(publicMessage,rvBaseAdapter,mvpPresenter);
                    cells.add(aCicleAdapter);
                    break;
                case PublicMessage.TYPE_VIDEO:
                    aCicleAdapter = new VideoCellAdapter1(publicMessage,rvBaseAdapter,mvpPresenter);
                    cells.add(aCicleAdapter);
                    break;
                case PublicMessage.TYPE_IMG:
                    aCicleAdapter = new ImageCellAdapter(publicMessage,rvBaseAdapter,mvpPresenter);
                    cells.add(aCicleAdapter);
                    break;
            }
        }
        rvBaseAdapter.setData(cells);
    }



    private CommentConfig commentConfig;
    @Override
    public void updateEditTextBodyVisible(int visibility, CommentConfig commentConfig) {
        this.commentConfig = commentConfig;
        edittextbody.setVisibility(visibility);
        measureCircleItemHighAndCommentItemOffset(commentConfig);
        if(View.VISIBLE==visibility){
            editText.requestFocus();
            //弹出键盘
            CommonUtils.showSoftInput( editText.getContext(),  editText);

        }else if(View.GONE==visibility){
            //隐藏键盘
            CommonUtils.hideSoftInput( editText.getContext(),  editText);
        }
    }

    @Override
    public void notifityChange() {
        showContentView();
        updateEditTextBodyVisible(View.GONE, null);
        recyclerView.setRefreshing(false);
        firstNotifyAdapter();
    }

    @Override
    public void onfaile() {
        //recyclerView.setRefreshing(false);
        //showEmptyView();
        notifityChange();
    }

    @Override
    public void notifityChange(int type,List<PublicMessage> messages) {
        if (messages.size()==0&&type==mvpPresenter.ON_LOAD){
            T_.showToastReal("没有更多数据了");
            recyclerView.removeMoreListener();
            recyclerView.hideMoreProgress();
            return;
        }
        if (type==mvpPresenter.ON_REFRESH){
            mMessages=messages;
        }else {
            mMessages.addAll(messages);
        }
        notifityChange();
    }

    private int screenHeight;
    private int editTextBodyHeight;
    private int currentKeyboardH;
    private int selectCircleItemH;
    private int selectCommentItemOffset;

    private void setViewTreeObserver() {
        bodyLayout = (RelativeLayout) findViewById(R.id.bodyLayout);
        final ViewTreeObserver swipeRefreshLayoutVTO = bodyLayout.getViewTreeObserver();
        swipeRefreshLayoutVTO.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                bodyLayout.getWindowVisibleDisplayFrame(r);
                int statusBarH =  getStatusBarHeight();//状态栏高度
                int screenH = bodyLayout.getRootView().getHeight();
                if(r.top != statusBarH ){
                    //在这个demo中r.top代表的是状态栏高度，在沉浸式状态栏时r.top＝0，通过getStatusBarHeight获取状态栏高度
                    r.top = statusBarH;
                }
                int keyboardH = screenH - (r.bottom - r.top);
                //Log.d(TAG, "screenH＝ "+ screenH +" &keyboardH = " + keyboardH + " &r.bottom=" + r.bottom + " &top=" + r.top + " &statusBarH=" + statusBarH);
                if(keyboardH == currentKeyboardH){//有变化时才处理，否则会陷入死循环
                    return;
                }
                currentKeyboardH = keyboardH;
                screenHeight = screenH;//应用屏幕的高度
                editTextBodyHeight = edittextbody.getHeight();

                if(keyboardH<150){//说明是隐藏键盘的情况
                    updateEditTextBodyVisible(View.GONE, null);
                    return;
                }
                //偏移listview
                if(layoutManager!=null && commentConfig != null){
                    layoutManager.scrollToPositionWithOffset(commentConfig.circlePosition, getListviewOffset(commentConfig));
                }
            }
        });
    }


    /**
     * 测量偏移量
     * @param commentConfig
     * @return
     */
    private int getListviewOffset(CommentConfig commentConfig) {
        if(commentConfig == null)
            return 0;
        //这里如果你的listview上面还有其它占高度的控件，则需要减去该控件高度，listview的headview除外。
        //int listviewOffset = mScreenHeight - mSelectCircleItemH - mCurrentKeyboardH - mEditTextBodyHeight;
        int listviewOffset = screenHeight - selectCircleItemH - currentKeyboardH - editTextBodyHeight - getToolBarView().getHeight();
        if(commentConfig.commentType == CommentConfig.Type.REPLY){
            //回复评论的情况
            listviewOffset = listviewOffset + selectCommentItemOffset;
        }
        //Log.i(TAG, "listviewOffset : " + listviewOffset);
        return listviewOffset;
    }

    private void measureCircleItemHighAndCommentItemOffset(CommentConfig commentConfig){
        if(commentConfig == null)
            return;
        int firstPosition = layoutManager.findFirstVisibleItemPosition();
        //只能返回当前可见区域（列表可滚动）的子项
        View selectCircleItem = layoutManager.getChildAt(commentConfig.circlePosition  - firstPosition);

        if(selectCircleItem != null){
            selectCircleItemH = selectCircleItem.getHeight();
        }

        if(commentConfig.commentType == CommentConfig.Type.REPLY){
            //回复评论的情况
            CommentView commentLv = (CommentView) selectCircleItem.findViewById(R.id.commentList);
            if(commentLv!=null){
                //找到要回复的评论view,计算出该view距离所属动态底部的距离
                View selectCommentItem = commentLv.getChildAt(commentConfig.commentPosition);
                if(selectCommentItem != null){
                    //选择的commentItem距选择的CircleItem底部的距离
                    selectCommentItemOffset = 0;
                    View parentView = selectCommentItem;
                    do {
                        int subItemBottom = parentView.getBottom();
                        parentView = (View) parentView.getParent();
                        if(parentView != null){
                            selectCommentItemOffset += (parentView.getHeight() - subItemBottom);
                        }
                    } while (parentView != null && parentView != selectCircleItem);
                }
            }
        }
    }



    /********** 公共消息的数据请求部分 *********/


    /**
     * 是否是商务圈类型
     * @return
     */
    private boolean isMyBusiness() {
        // return mType == AppConfig.CIRCLE_TYPE_MY_BUSINESS;
        return false;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SEND_MSG) {
            if (resultCode == Activity.RESULT_OK) {// 发说说成功
                String messageId = data.getStringExtra(Constants.EXTRA_MSG_ID);
                CircleMessageDao.getInstance().addMessage(mLoginUserId, messageId);
                mvpPresenter.requestMyBusiness(mvpPresenter.ON_REFRESH);
            }
        }

        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {// 发说说成功
                String path = data.getStringExtra("path");
                if (data.getBooleanExtra("take_photo", true)) {
                    //照片
                    //mvpPresenter.sendImgMsg(ImageUtils.genThumbImgFile(path), new File(path));
                } else {
                    //小视频
                    //mvpPresenter.sendAudioFile(new File(path));
                    Intent intent = new Intent();
                    intent.setClass(FriendCircleActivity.this, SendFriendCircleActivity.class);
                    intent.putExtra("videoFile",path);
                    intent.putExtra("type", 4);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);// 去发说说
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

package com.lsjr.zizi.mvp.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.PopupWindow;

import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.session.ScanActivity;
import com.lsjr.zizi.mvp.home.session.SeachFriendActivity;
import com.lsjr.zizi.mvp.home.zichat.CreatNewGroupActivity;
import com.lsjr.zizi.mvp.home.session.DataDownloadActivity;
import com.lsjr.zizi.mvp.home.session.LoginActivity;
import com.lsjr.zizi.mvp.home.session.LoginStatusActivity;
import com.lsjr.zizi.chat.broad.CardcastUiUpdateUtil;
import com.lsjr.zizi.chat.broad.MsgBroadcast;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.dao.UserDao;
import com.lsjr.zizi.chat.db.NewFriendMessage;
import com.lsjr.zizi.chat.db.User;
import com.lsjr.zizi.chat.helper.LoginHelper;
import com.lsjr.zizi.chat.helper.UserSp;
import com.lsjr.zizi.chat.xmpp.CoreService;
import com.lsjr.zizi.chat.xmpp.ListenerManager;
import com.lsjr.zizi.chat.xmpp.listener.AuthStateListener;
import com.lsjr.zizi.mvp.adapter.HomeAadapter;
import com.lsjr.zizi.mvp.contrl.FragmentController;
import com.lsjr.zizi.util.PopupWindowUtils;
import com.ymz.baselibrary.BaseApplication;
import com.ymz.baselibrary.NetWorkObservable;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.ActivityUtils;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.SpUtils;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ymz.baselibrary.view.PermissionListener;
import com.ys.uilibrary.tab.BottomTabView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;

/**
 * 创建人：gyymz1993
 * 创建时间：2017/5/29/3:12
 **/
public class HomeActivity extends MvpActivity implements AuthStateListener ,NetWorkObservable.NetWorkObserver {

    @BindView(R.id.pager)
    ViewPager mViewPager;
    private boolean mBind;
    public CoreService mXmppService;

    @BindView(R.id.bottomTabView)
    public BottomTabView bottomTabView;
    boolean isFristcreate=true;
    DataDownloadActivity dataDownloadActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 注册用户登录状态广播
        registerReceiver(mUserLogInOutReceiver, LoginHelper.getLogInOutActionFilter());
        //LoginHelper.broadcastLogin(UIUtils.getContext());

        BaseApplication.instance().registerNetWorkObserver(this);
        ListenerManager.getInstance().addAuthStateChangeListener(this);
        // 注册消息更新广播
        IntentFilter msgIntentFilter = new IntentFilter();
        msgIntentFilter.addAction(MsgBroadcast.ACTION_MSG_NUM_UPDATE);
        msgIntentFilter.addAction(MsgBroadcast.ACTION_MSG_NUM_RESET);
        registerReceiver(mUpdateUnReadReceiver, msgIntentFilter);

        //百度推送
        //PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY,"8h0GOjOlgP8dXRzp9nG1dGBT");
        registerReceiver(mUpdateReceiver, CardcastUiUpdateUtil.getUpdateActionFilter());
        mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
        // 绑定服务
        mXmppBind = bindService(CoreService.getIntent(), mXmppServiceConnection, BIND_AUTO_CREATE);


        // 检查用户的状态，做不同的初始化工作
        User loginUser = ConfigApplication.instance().mLoginUser;
        if (!LoginHelper.isUserValidation(loginUser)) {
            LoginHelper.prepareUser(this);
        }

        if (!ConfigApplication.instance().mUserStatusChecked) {// 用户状态没有检测，那么开始检测
            mUserCheckHander.sendEmptyMessageDelayed(MSG_USER_CHECK, mRetryCheckDelay);
        } else {
            if (ConfigApplication.instance().mUserStatus == LoginHelper.STATUS_USER_VALIDATION) {
                LoginHelper.broadcastLogin(this);
            } else {// 重新检测
                ConfigApplication.instance().mUserStatusChecked = false;
                mUserCheckHander.sendEmptyMessageDelayed(MSG_USER_CHECK, mRetryCheckDelay);
            }
        }

        super.onCreate(savedInstanceState);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void nameUpdate(String update) {
        if (!TextUtils.isEmpty(update)){
            MsgBroadcast.broadcastMsgUiUpdate(UIUtils.getContext());
        }
    }


    public CoreService getmXmppService() {
        return mXmppService;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        //百度推送
        // PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY,"8h0GOjOlgP8dXRzp9nG1dGBT");
        //mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        // 注册网络改变回调
        // ConfigApplication.instance().registerNetWorkObserver(this);
        // 绑定监听
        Bundle extras = getIntent().getExtras();
        if (extras!=null&&extras.getBoolean("isLoginJump")){
            SpUtils.getInstance().saveBoolean("isFirstCreate",true);
            showProgressDialogWithText("开始下载数据");
            showLoadingView();
            dataDownloadActivity=new DataDownloadActivity() {

                @Override
                protected void downloadOver() {
                    dismissProgressDialog();
                    showContentView();
                    UserSp.getInstance().setUpdate(true);
                    LoginHelper.broadcastLogin(UIUtils.getContext());
                    //jumpToActivityAndClearTask(HomeActivity.class);
                }

                @Override
                protected void resetLogin() {
                    openActivity(LoginActivity.class);
                }
            };
            //T_.showToastReal("正在执行下载方法"+ dataDownloadActivity.circle_msg_download_status);
            L_.e("正在执行下载方法"+ dataDownloadActivity.circle_msg_download_status);
            dataDownloadActivity.onStartDown();
        }

    }


    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("消息");
        getToolBarView().getLeftimageView().setVisibility(View.GONE);
        getToolBarView().getRightImageView().setVisibility(View.VISIBLE);
        getToolBarView().getRightImageView().setImageResource(R.drawable.icon_add);
        getToolBarView().getRightImageView().setOnClickListener(v -> {
            initMenuListener();
        });
    }


    public void initMenuListener() {
        //显示或隐藏popupwindow
        View menuView = View.inflate(this, R.layout.dialog_menu, null);
        PopupWindow popupWindow = PopupWindowUtils.getPopupWindowAtLocation(menuView,
                getWindow().getDecorView(), Gravity.TOP | Gravity.RIGHT,
                UIUtils.dip2px(10), UIUtils.getNavBarHeight(UIUtils.getContext())+getToolBarView().getHeight()/2);
        menuView.findViewById(R.id.tvCreateGroup).setOnClickListener(v1 -> {
            jumpToActivity(CreatNewGroupActivity.class);
            popupWindow.dismiss();
        });
        menuView.findViewById(R.id.tvAddFriend).setOnClickListener(v1 -> {
            jumpToActivity(SeachFriendActivity.class);
            popupWindow.dismiss();
        });
        menuView.findViewById(R.id.tvScan).setOnClickListener(v1 -> {
            jumpToActivity(ScanActivity.class);
            popupWindow.dismiss();
        });
    }

    @Override
    protected void initView() {
        initParams();
        initPermissions();
    }


    private void initPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO
                ,  Manifest.permission.MODIFY_AUDIO_SETTINGS,Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
        }, new PermissionListener() {
            @Override
            public void onGranted() {
               //T_.showToastReal("授权");
//                Intent intentService = new Intent(HomeActivity.this, SmsService.class);
//                startService(intentService);
//
//                registerReceiver(senMessage, SendMesReceiver.getSendMsgFilter());
//                SendMesReceiver.broadcastSendMsg(UIUtils.getContext());
               // Intent intent = new Intent(HomeActivity.this, SMSBroadcastReceiver.class);
               // sendBroadcast(intent);
            }

            @Override
            public void onDenied(List<String> deniedPermissions) {
            }
        });
    }


    @Override
    protected int getLayoutId() {
        return R.layout.chat_activity_home;
    }


    BottomTabView.TabItemView tabItemView;
    protected List<BottomTabView.TabItemView> getTabViews() {
        int[] mSelectors = new int[]{R.drawable.hy_tab1, R.drawable.hy_tab2, R.drawable.hy_tab3, R.drawable.hy_tab4};
        tabItemView =  new BottomTabView.TabItemView(this, "消息", R.color.tab_text_normal, R.color.tab_text_select, R.drawable.tab_job, R.drawable.tab_job_press);
       // tabItemView.setPromptNum(10);
        tabItemViews.add(tabItemView);
        tabItemViews.add(new BottomTabView.TabItemView(this, "通讯录", R.color.tab_text_normal, R.color.tab_text_select, R.drawable.tab_found, R.drawable.tab_found_press));
        tabItemViews.add(new BottomTabView.TabItemView(this, "发现", R.color.tab_text_normal, R.color.tab_text_select, R.drawable.tab_smile, R.drawable.tab_smile_press));
        tabItemViews.add(new BottomTabView.TabItemView(this, "我", R.color.tab_text_normal, R.color.tab_text_select, R.drawable.tab_me, R.drawable.tab_me_press));
        return tabItemViews;
    }



    public void initParams() {
        mViewPager.setOffscreenPageLimit(4);//设置ViewPager的缓存界面数,默认缓存为2
        int[] mSelectors = new int[]{R.drawable.hy_tab1, R.drawable.hy_tab2, R.drawable.hy_tab3, R.drawable.hy_tab4};
        HomeAadapter adapter = new HomeAadapter(getSupportFragmentManager(), mSelectors);
        mViewPager.setAdapter(adapter);
        bottomTabView.setTabItemViews(getTabViews());
        if (getOnTabItemSelectListener() != null) {
            bottomTabView.setOnTabItemSelectListener(getOnTabItemSelectListener());
        }
        bottomTabView.setOnSecondSelectListener(new BottomTabView.OnSecondSelectListener() {
            @Override
            public void onSecondSelect(int position) {

            }
        });
        if (getOnPageChangeListener() != null) {
            mViewPager.addOnPageChangeListener(getOnPageChangeListener());
        }

    }

    ArrayList<BottomTabView.TabItemView> tabItemViews = new ArrayList<>();
    protected BottomTabView.OnTabItemSelectListener getOnTabItemSelectListener() {
        return new BottomTabView.OnTabItemSelectListener() {
            @Override
            public void onTabItemSelect(int position) {
                //naView.setTitleText(titles[position]);
                mViewPager.setCurrentItem(position, true);
                setTitleChange(position);

            }
        };
    }

    protected ViewPager.OnPageChangeListener getOnPageChangeListener() {
        return new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                bottomTabView.updatePosition(position);
                setTitleChange(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };
    }


    private  void  setTitleChange(int position){
        if (position==0){
            setTitleText("消息");
            getToolBarView().getRightImageView().setImageResource(R.drawable.icon_add);
            getToolBarView().getRightImageView().setVisibility(View.VISIBLE);

        }if (position==1){
            setTitleText("通讯录");
            getToolBarView().getRightImageView().setVisibility(View.VISIBLE);
           // getToolBarView().getRightImageView().setImageResource(R.drawable.icon_add_contact);
            getToolBarView().getRightImageView().setImageResource(R.drawable.icon_add);
            // getToolBarView().setVisibility(View.GONE);
        }if (position==2){
            setTitleText("发现");
            getToolBarView().getRightImageView().setVisibility(View.GONE);
        }if (position==3){
            setTitleText("个人信息");
            getToolBarView().getRightImageView().setVisibility(View.GONE);
        }
    }





    @Override
    protected BasePresenter createPresenter() {
        return null;
    }


    @Override
    public void onAuthStateChange(int authState) {
        mImStatus = authState;
    }



    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CardcastUiUpdateUtil.ACTION_UPDATE_UI)) {
                 ContactsFragment contactsFragment= (ContactsFragment) FragmentController.getInstance().getFragment(1);
                if (contactsFragment != null) {
                    contactsFragment.loadData();
                }
            }
        }
    };


//    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent.getAction().equals(CardcastUiUpdateUtil.ACTION_UPDATE_UI)) {
//                AddressListFragment contactsFragment= (AddressListFragment) FragmentController.getInstance().getFragment(1);
//                if (contactsFragment != null) {
//                   // contactsFragment.loadData();
//                }
//            }
//        }
//    };


    /* UserCheck */
    private static final int MSG_USER_CHECK = 0x1;
    private static final int RETRY_CHECK_DELAY_MAX = 30000;// 为成功的情况下，最长30s检测一次
    private int mRetryCheckDelay = 0;
    @SuppressLint("HandlerLeak")
    private Handler mUserCheckHander = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_USER_CHECK) {
                if (mRetryCheckDelay < RETRY_CHECK_DELAY_MAX) {
                    mRetryCheckDelay += 5000;
                }
                mUserCheckHander.removeMessages(RETRY_CHECK_DELAY_MAX);
                doUserCheck();
            }
        }
    };

    private void doUserCheck() {
        if (!BaseApplication.instance().isNetworkActive()) {
            return;
        }
        if (ConfigApplication.instance().mUserStatusChecked) {
            return;
        }
        LoginHelper.checkStatusForUpdate(this, () ->
                mUserCheckHander.sendEmptyMessageDelayed(MSG_USER_CHECK, mRetryCheckDelay));
    }

    private void cancelUserCheckIfExist() {
        mUserCheckHander.removeMessages(RETRY_CHECK_DELAY_MAX);
    }

    private void checkUserDb(final String userId) {
        // 检测用户基本数据库信息的完整性
        new Thread(() -> {
            FriendDao.getInstance().checkSystemFriend(userId);
            initMsgUnReadTips(userId);
        }).start();
    }

    private BroadcastReceiver mUserLogInOutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(LoginHelper.ACTION_LOGIN)) {
                User user = ConfigApplication.instance().mLoginUser;
                L_.e("登陆成功---------------》启动服务"+user.toString());
                Intent startIntent = CoreService.getIntent(HomeActivity.this, user.getUserId(), user.getPassword(), user.getNickName());
                startService(startIntent);
                // ToastUtil.showNormalToast(MainActivity.this, "开始Xmpp登陆");
                checkUserDb(user.getUserId());

            } else if (action.equals(LoginHelper.ACTION_LOGOUT)) {
                ConfigApplication.instance().mUserStatus = LoginHelper.STATUS_USER_SIMPLE_TELPHONE;
                if (mCoreService!=null){
                    mCoreService.logout();
                }
                cancelUserCheckIfExist();
                if (dataDownloadActivity!=null){
                    dataDownloadActivity.cleanAllStatus();
                }
                UserSp.getInstance().clearUserInfo();
                removeNeedUserFragment();
                jumpToActivityAndClearTask(LoginActivity.class);
                ActivityUtils.removeAllActivity();
               // UserSp.getInstance().clearUserInfo();
                //startActivity(new Intent(HomeActivity.this, LoginHistoryActivity.class));
                // mFindRb.setChecked(true);


            } else if (action.equals(LoginHelper.ACTION_CONFLICT)) {
                // 改变用户状态
                ConfigApplication.instance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;
                if (mCoreService!=null){
                    mCoreService.logout();
                }
                // mFindRb.setChecked(true);
                removeNeedUserFragment();
                 UserSp.getInstance().clearUserInfo();
                cancelUserCheckIfExist();
                T_.showToastReal("异地登录");
                // 弹出对话框
                jumpToActivityAndClearTask(LoginStatusActivity.class);
//                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.HONEYCOMB) {
//                    mActivityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
//                } else {
//                    mActivityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
//                }
            } else if (action.equals(LoginHelper.ACTION_NEED_UPDATE)) {
                // mFindRb.setChecked(true);
                //removeNeedUserFragment(true);
                cancelUserCheckIfExist();
                // 弹出对话框
              //  startActivity(new Intent(MainActivity.this, UserCheckedActivity.class));
            } else if (action.equals(LoginHelper.ACTION_LOGIN_GIVE_UP)) {
                cancelUserCheckIfExist();
                ConfigApplication.instance().mUserStatus = LoginHelper.STATUS_USER_NO_UPDATE;
                if (mCoreService!=null){
                    mCoreService.logout();
                }
            }

        }

    };



    /* 当注销当前用户时，将那些需要当前用户的Fragment销毁，以后重新登陆后，重新加载为初始状态 */
    private void removeNeedUserFragment() {
        FragmentController.getInstance().clean();
    }


    private int mImStatus = AuthStateListener.AUTH_STATE_NOT;
    private ServiceConnection mXmppServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCoreService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCoreService = ((CoreService.CoreServiceBinder) service).getService();
            mImStatus = mCoreService.isAuthenticated() ? AuthStateListener.AUTH_STATE_SUCCESS : AuthStateListener.AUTH_STATE_NOT;
          //  T_.showToastReal(mImStatus==3?"成功上线":"登陆中");
        }
    };


    /*********************** 未读数量的更新功能 *****************/
    private Handler mUnReadHandler = new Handler();
    //private TextView mMsgUnReadTv;
    private int mMsgUnReadNum = 0;
    private boolean mMsgNumNeedUpdate = false;
    //
    private boolean mXmppBind;
    private CoreService mCoreService;
    private boolean isPause = true;// 界面是否暂停

    private void initMsgUnReadTips(String userId) {// 初始化未读条数
        // 消息未读条数累加
        mMsgUnReadNum = FriendDao.getInstance().getMsgUnReadNumTotal(userId);
        mUnReadHandler.post(() -> updateMsgUnReadTv());
    }

    private BroadcastReceiver mUpdateUnReadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MsgBroadcast.ACTION_MSG_NUM_UPDATE)) {
                int operation = intent.getIntExtra(MsgBroadcast.EXTRA_NUM_OPERATION, MsgBroadcast.NUM_ADD);
                int count = intent.getIntExtra(MsgBroadcast.EXTRA_NUM_COUNT, 0);
                mMsgUnReadNum = (operation == MsgBroadcast.NUM_ADD) ? mMsgUnReadNum + count : mMsgUnReadNum - count;
                updateMsgUnReadTv();
            } else if (action.equals(MsgBroadcast.ACTION_MSG_NUM_RESET)) {
                if (isPause) {// 等待恢复的时候更新
                    mMsgNumNeedUpdate = true;
                } else {// 立即更新
                    initMsgUnReadTips(ConfigApplication.instance().mLoginUser.getUserId());
                }
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mXmppService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mXmppService = ((CoreService.CoreServiceBinder) service).getService();
        }
    };

    private void updateMsgUnReadTv() {
        L_.e("消息列表"+mMsgUnReadNum);
        if (tabItemView==null)return;
        if (mMsgUnReadNum > 0) {
//            if (isFristcreate){
//                MsgBroadcast.broadcastMsgUiUpdate(UIUtils.getContext());
//                isFristcreate=false;
//            }
           // mMsgUnReadTv.setVisibility(View.VISIBLE);
            String numStr = mMsgUnReadNum >= 99 ? "99" : mMsgUnReadNum + "";
            tabItemView.setPromptNum(Integer.valueOf(numStr));
        }else {
            tabItemView.setPromptNum(0);
        }
    }

    public void exitMucChat(String toUserId) {
        if (mCoreService != null) {
            mCoreService.exitMucChat(toUserId);
        }
    }

    public void sendNewFriendMessage(String toUserId, NewFriendMessage message) {
        if (mBind && mXmppService != null) {
            mXmppService.sendNewFriendMessage(toUserId, message);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        isPause = false;
        if (mMsgNumNeedUpdate) {
            initMsgUnReadTips(ConfigApplication.instance().mLoginUser.getUserId());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPause = true;
    }


    @Override
    protected void onStop() {
        super.onStop();
        saveOfflineTime();

    }

    private void saveOfflineTime() {
        //将现在的时间存起来,
        long time=System.currentTimeMillis()/1000;
        Log.d("wang", "time_destory::" + time + "");
        SpUtils.putLong(this, Constants.OFFLINE_TIME, time);
        ConfigApplication.instance().mLoginUser.setOfflineTime(time);
        UserDao.getInstance().updateUnLineTime(ConfigApplication.instance().mLoginUser.getUserId(), time);
    }



    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        saveOfflineTime();
    }

    @Override
    protected void onDestroy() {
        saveOfflineTime();
        BaseApplication.instance().unregisterNetWorkObserver(this);
        ListenerManager.getInstance().removeAuthStateChangeListener(this);
        EventBus.getDefault().unregister(this);
        if (mXmppBind) {
            unbindService(mXmppServiceConnection);
        }
        if (mBind) {
            unbindService(mServiceConnection);
        }
        unregisterReceiver(mUpdateUnReadReceiver);
        unregisterReceiver(mUserLogInOutReceiver);
        unregisterReceiver(mUpdateReceiver);
        super.onDestroy();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            exitByDoubleClick();
        }
        return false;
    }

    boolean isExit;
    private void exitByDoubleClick() {
        Timer tExit=null;
        if(!isExit){
            isExit=true;
            T_.showToastReal("再按一次退出程序--->Timer");
            tExit=new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit=false;//取消退出
                }
            },2000);// 如果2秒钟内没有按下返回键，则启动定时器取消掉刚才执行的任务
        }else{
            finish();
            System.exit(0);
        }
    }


    //    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FragmentController.getInstance().getFragment(3).onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onNetWorkStatusChange(boolean connected) {
        // 当网络状态改变时，判断当前用户的状态，是否需要更新
        L_.e("--------------网络改变+connected"+connected);
        if (connected) {
            if (!ConfigApplication.instance().mUserStatusChecked) {
                mRetryCheckDelay = 0;
                mUserCheckHander.sendEmptyMessageDelayed(MSG_USER_CHECK, mRetryCheckDelay);
            }
        }
    }
}

package com.lsjr.zizi.mvp.home.session;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.broad.MsgBroadcast;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.dao.NewFriendDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.db.NewFriendMessage;
import com.lsjr.zizi.chat.xmpp.CoreService;
import com.lsjr.zizi.chat.xmpp.ListenerManager;
import com.lsjr.zizi.chat.xmpp.listener.NewFriendListener;
import com.lsjr.zizi.mvp.home.session.adapter.NewFriendAdapter;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/1 17:31
 */

public class NewFriendActivity extends MvpActivity implements NewFriendListener {


    @BindView(R.id.id_contacts)
    RecyclerView idContacts;
    //private NewFriendAdapter mAdapter;
    private List<NewFriendMessage> mNewFriends;
    private String mLoginUserId;
    private boolean mBind;
    private CoreService mXmppService;
    private Handler mHandler = new Handler();
    private NewFriendAdapter newFriendAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mNewFriends = new ArrayList<>();
        mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        super.onCreate(savedInstanceState);

        setTitleText("新的朋友");
        setTopLeftButton(R.drawable.ic_back).
                setTitleTextColor(UIUtils.getColor(R.color.white)).
                setBackgroundColor(UIUtils.getColor(R.color.colorPrimary));

        ListenerManager.getInstance().addNewFriendListener(this);
        mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
        // 是所有消息都变为已读
        FriendDao.getInstance().markUserMessageRead(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE);
        NewFriendDao.getInstance().markNewFriendRead(mLoginUserId);

        idContacts.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        newFriendAdapter = new NewFriendAdapter(getApplication(), mNewFriends, R.layout.item_new_friend);
        newFriendAdapter.setmListener(mNewFriendActionListener);
        idContacts.setAdapter(newFriendAdapter);
        loadData();
    }

    private NewFriendAdapter.NewFriendActionListener mNewFriendActionListener = new NewFriendAdapter.NewFriendActionListener() {
        @Override
        public void sayHellow(int position) {
            //doFeedbackOrSayHello(position, 0);
        }

        @Override
        public void feedback(int position) {

        }

        @Override
        public void agree(int position) {
            // doAgreeOrAttention(position, 1);
        }

        @Override
        public void addAttention(int position) {
            //doAgreeOrAttention(position, 0);
        }

        @Override
        public void removeBalckList(int position) {
            //removeBlacklist(position);
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


    protected void onDestroy() {
        super.onDestroy();
        ListenerManager.getInstance().removeNewFriendListener(this);
        MsgBroadcast.broadcastMsgUiUpdate(this);
        if (mBind) {
            unbindService(mServiceConnection);
        }
    }


    /**
     * 请求公共消息
     * <p>
     * 是下拉刷新，还是上拉加载
     */
    private void loadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                mNewFriends = NewFriendDao.getInstance().getAllNewFriendMsg(mLoginUserId);
                L_.e("新增还有列表"+mNewFriends.size());
                long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少200ms的刷新过程
                if (delayTime < 0) {
                    delayTime = 0;
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        newFriendAdapter.notifyDataSetChanged(mNewFriends);
                    }
                }, delayTime);
            }
        }).start();

    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_nearby;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

    }

    @Override
    public void onNewFriendSendStateChange(String toUserId, NewFriendMessage message, int messageState) {
    }

    @Override
    public boolean onNewFriend(NewFriendMessage message) {
        loadData();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }


}

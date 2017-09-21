package com.lsjr.zizi.mvp.home.session;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.andview.listener.OnItemClickListener;
import com.lsjr.bean.ObjectResult;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.chat.bean.BaseSortModel;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.bean.MucRoomMember;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.dao.ChatMessageDao;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.chat.utils.StringUtils;
import com.lsjr.zizi.chat.xmpp.CoreService;
import com.lsjr.zizi.chat.xmpp.ListenerManager;
import com.lsjr.zizi.mvp.contrl.FragmentController;
import com.lsjr.zizi.mvp.home.HomeActivity;
import com.lsjr.zizi.view.OptionItemView;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/21 15:12
 */

@SuppressLint("Registered")
public class GroupManagerActivity extends MvpActivity {

    @BindView(R.id.grid_manager)
    RecyclerView gridManager;
    GroupManagerAdapter gridAdapter;
    @BindView(R.id.oivGroupName)
    OptionItemView oivGroupName;
    @BindView(R.id.oivGroupDetail)
    OptionItemView oivGroupDetail;
    @BindView(R.id.my_name)
    OptionItemView myName;
    @BindView(R.id.btnQuit)
    Button btnQuit;
    private String mRoomJid;
    private String mLoginUserId;
    private Friend mRoom;

    private boolean mXmppBind;
    private CoreService mCoreService;

    private List<MucRoomMember> mMembers;
    private int add_minus_count = 2;// +号和-号的个数，如果权限可以踢人，就是2个，如果权限不可以踢人，就是1个


    MucRoom mucRoom;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }


    @Override
    protected void initView() {
        mMembers = new ArrayList<>();
        setTitleText("成员管理");
        setTopLeftButton(R.drawable.ic_back).
                setTitleTextColor(UIUtils.getColor(R.color.white)).
                setBackgroundColor(UIUtils.getColor(R.color.colorPrimary));

        gridAdapter = new GroupManagerAdapter(UIUtils.getContext(), mMembers, R.layout.item_groupmanager);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(UIUtils.getContext(), 5);
        gridManager.setNestedScrollingEnabled(false);
        gridManager.setLayoutManager(gridLayoutManager);
        gridManager.setAdapter(gridAdapter);

        gridAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(BaseRecyclerHolder baseRecyclerHolder, int position, Object item) {

                if (add_minus_count == 1) {
                    if (position == mMembers.size() - 1) {
                        gotoAddContact(add_minus_count);
                    } else {
                        if (!doDel && !doBannedVoice) {
                            MucRoomMember member = mMembers.get(position);
                            if (member != null) {
                                Intent intent = new Intent(GroupManagerActivity.this, BasicInfoActivity.class);
                                intent.putExtra(Constants.EXTRA_USER_ID, member.getUserId());
                                startActivity(intent);
                            }
                        }
                    }
                } else if (add_minus_count == 2) {
                    if (position == mMembers.size() - 2) {
                        gotoAddContact(add_minus_count);
                    } else if (position == mMembers.size() - 1) {
                        T_.showToastReal("执行删除");
                        // delete
                        doDel = true;
                        gridAdapter.notifyDataSetChanged();
                    } else {
                        if (!doDel && !doBannedVoice) {
                            MucRoomMember member = mMembers.get(position);
                            if (member != null) {
                                /*个人资料*/
                                Intent intent = new Intent(GroupManagerActivity.this, BasicInfoActivity.class);
                                intent.putExtra(Constants.EXTRA_USER_ID, member.getUserId());
                                startActivity(intent);
                            }
                        }
                    }
                }
            }
        });

        gridManager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!doDel) {
                    return false;
                }
                doDel = false;
                gridAdapter.notifyDataSetChanged();
                return false;
            }
        });

        myName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRemarkDialog();
            }
        });
    }




    private void showRemarkDialog() {
        final EditText editText = new EditText(this);
        editText.setMaxLines(2);
        editText.setLines(2);
        editText.setText("");
        editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
        editText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.set_remark_name).setView(editText)
                .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = editText.getText().toString();
                        if (input.equals(myNickName)||!StringUtils.isNickName(input)) {// 备注名没变
                            T_.showToastReal("备注名没变");
                            L_.e("不符合昵称"+input);
                            return;
                        }
                        updateNickName(input);
                    }
                }).setNegativeButton(getString(R.string.cancel), null);
        builder.create().show();
    }


    private void updateNickName(final String nickName) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        params.put("roomId", mRoom.getRoomId());
        params.put("userId", mLoginUserId);
        params.put("nickname", nickName);


        showProgressDialogWithText("更改昵称中");
        HttpUtils.getInstance().postServiceData(AppConfig.ROOM_MEMBER_UPDATE, params, new ChatObjectCallBack<Void>(Void.class) {
            @Override
            protected void onXError(String exception) {
                dismissProgressDialog();
            }

            @Override
            protected void onSuccess(ObjectResult<Void> result) {
                dismissProgressDialog();
                boolean success = ResultCode.defaultParser(result, true);
                if (success) {
                    myName.setRightText(nickName);
                    String loginUserId = ConfigApplication.instance().mLoginUser.getUserId();
                    FriendDao.getInstance().updateNickName(loginUserId, mRoom.getUserId(), nickName);
                    ChatMessageDao.getInstance().updateNickName(loginUserId, mRoom.getUserId(), loginUserId, nickName);
                    mRoom.setRoomMyNickName(nickName);
                    FriendDao.getInstance().createOrUpdateFriend(mRoom);
                    ListenerManager.getInstance().notifyNickNameChanged(mRoom.getUserId(), loginUserId, nickName);
                }
            }

        });

    }


    public void gotoAddContact(int count) {
        List<String> existIds = new ArrayList<>();
        for (int i = 0; i < mMembers.size() - count; i++) {
            existIds.add(mMembers.get(i).getUserId());
        }
        // 去添加人
        if (mucRoom == null) return;
        Intent intent = new Intent(GroupManagerActivity.this, CreateGroupActivity.class);
        intent.putExtra("roomId", mRoom.getRoomId());
        intent.putExtra("roomJid", mRoomJid);
        intent.putExtra("roomName", mucRoom.getName());
        intent.putExtra("roomDes", mRoom.getDescription());
        intent.putExtra("exist_ids", JSON.toJSONString(existIds));
        intent.putExtra(Constants.ISADD_USER, true);
        startActivityForResult(intent, 1);
        //T_.showToastReal("添加好友");

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_group_manager;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent() != null) {
            mRoomJid = getIntent().getStringExtra(AppConfig.EXTRA_USER_ID);
        }
        if (TextUtils.isEmpty(mRoomJid)) {
            return;
        }
        mLoginUserId = ConfigApplication.instance().getLoginUserId();
        mRoom = FriendDao.getInstance().getFriend(mLoginUserId, mRoomJid);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        if (mRoom == null || TextUtils.isEmpty(mRoom.getRoomId())) {
            return;
        }
        // 绑定服务
        mXmppBind = bindService(CoreService.getIntent(), mXmppServiceConnection, BIND_AUTO_CREATE);
        loadMembers();

    }


    private void loadMembers() {
        showProgressDialogWithText("获取数据");
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        params.put("roomId", mRoom.getRoomId());
        HttpUtils.getInstance().postServiceData(AppConfig.ROOM_GET, params, new ChatObjectCallBack<MucRoom>(MucRoom.class) {

            @Override
            protected void onXError(String exception) {
                dismissProgressDialog();
                T_.showToastReal(exception);
            }

            @Override
            protected void onSuccess(ObjectResult<MucRoom> result) {
                dismissProgressDialog();
                boolean success = ResultCode.defaultParser(result, true);
                if (success && result.getData() != null) {
                    mucRoom = result.getData();
                    mMembers.clear();
                    mMembers.addAll(mucRoom.getMembers());
                    updateUI();
                }
            }
        });
    }

    String myNickName = "";
    private void updateUI() {
        List<MucRoom.Notice> notices = mucRoom.getNotices();
        long createTime = mucRoom.getCreateTime();
        myNickName=mucRoom.getNickName();
        oivGroupName.setRightText(mucRoom.getName());
        oivGroupDetail.setRightText(mRoom.getDescription());
        myName.setRightText(mucRoom.getNickName());
        //mCountTv.setText(mucRoom.getMaxUserSize() + "");
        if (mMembers != null) {
            MucRoomMember my = null;
            for (int i = 0; i < mMembers.size(); i++) {
                String userId = mMembers.get(i).getUserId();
                if (userId.equals(mLoginUserId)) {
                    myNickName = mMembers.get(i).getNickName();
                    my = mMembers.get(i);
                    break;
                }
            }
            if (my != null) {// 将我自己移动到第一个的位置
                mMembers.remove(my);
                if (mucRoom.getUserId().equals(mLoginUserId)){
                    mMembers.add(0, my);
                }else {
                    mMembers.add(1, my);
                }

            }
        }

        if (mucRoom.getUserId().equals(mLoginUserId)) {// 我是创建者
            add_minus_count = 2;
            mMembers.add(null);// 一个+号
            mMembers.add(null);// 一个－号
        } else {
            add_minus_count = 1;
            mMembers.add(null);// 一个+号
        }
        gridAdapter.notifyDataSetChanged(mMembers);

        if (TextUtils.isEmpty(myNickName)) {
            myName.setRightText(ConfigApplication.instance().mLoginUser.getNickName());
        } else {
            myName.setRightText(myNickName);
        }


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mXmppBind) {
            unbindService(mXmppServiceConnection);
        }
    }

    private ServiceConnection mXmppServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCoreService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCoreService = ((CoreService.CoreServiceBinder) service).getService();
        }
    };

    private boolean doDel = false;
    private boolean doBannedVoice = false;
    private boolean dataInvalidate = true;// 数据是否有效，判断标准时传递进来的Occupant

    public class GroupManagerAdapter extends ABaseRefreshAdapter<MucRoomMember> {

        public GroupManagerAdapter(Context context, List<MucRoomMember> datas, int itemLayoutId) {
            super(context, datas, itemLayoutId);
        }

        @Override
        protected void convert(BaseRecyclerHolder viewHolder, MucRoomMember mucRoomMember, int position) {
            LinearLayout linearLayout = viewHolder.getView(R.id.id_root_manager);
            CircleImageView imageView = viewHolder.getView(R.id.content);
            Button button = viewHolder.getView(R.id.btn_del);
            TextView tvName = viewHolder.getView(R.id.id_tv_name);
            if (mucRoomMember != null) {
                Friend friend=new Friend();
                friend.setNickName(mucRoomMember.getNickName());
                friend.setUserId(mucRoomMember.getUserId());
                AvatarHelper.getInstance().displayAvatar(friend, imageView, true);
                tvName.setText(mucRoomMember.getNickName() == null ? "暂无昵称" : mucRoomMember.getNickName());
            } else {
                imageView.setBackgroundResource(R.drawable.defaultpic);
            }

            linearLayout.setVisibility(View.VISIBLE);
            if (mucRoom.getUserId().equals(mLoginUserId)) {// 我是创建者
                if (position == mMembers.size() - 2) {
                    imageView.setBackgroundResource(R.drawable.bg_room_info_add_btn);
                } else if (position == mMembers.size() - 1) {
                    imageView.setBackgroundResource(R.drawable.bg_room_info_minus_btn);
                } else {
                    imageView.setBackgroundResource(R.drawable.defaultpic);
                }
            } else {
                if (position == mMembers.size() - 1) {
                    imageView.setBackgroundResource(R.drawable.bg_room_info_add_btn);
                } else {
                    imageView.setBackgroundResource(R.drawable.defaultpic);
                }
            }
            button.setVisibility(View.GONE);
            if (doDel) {
                button.setVisibility(View.VISIBLE);
                if (position == mMembers.size() - 1 || position == mMembers.size() - 2) {
                    linearLayout.setVisibility(View.GONE);
                }
            }
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (add_minus_count == 1) {
                        return;
                    }
                    if (doDel) {
                        if (mMembers.get(position).getUserId().equals(mLoginUserId)) {
                            return;
                        }
                        deleteMember(position, mMembers.get(position).getUserId());
                    } else if (doBannedVoice) {
                        if (mucRoomMember.getUserId().equals(mLoginUserId)) {
                            return;
                        }
                    }
                }
            });
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(UIUtils.WHD()[0] / 5, UIUtils.WHD()[0] / 5 + UIUtils.dip2px(30));
            layoutParams.gravity = Gravity.CENTER;
            linearLayout.setLayoutParams(layoutParams);
        }
    }

    /*
    * 删除成员
    * */
    private void deleteMember(final int position, String userId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        params.put("roomId", mRoom.getRoomId());
        params.put("userId", userId);
        showProgressDialogWithText("删除中用户" + userId);
        HttpUtils.getInstance().postServiceData(AppConfig.ROOM_MEMBER_DELETE, params, new ChatObjectCallBack<Void>(Void.class) {

            @Override
            protected void onXError(String exception) {
                dismissProgressDialog();
                T_.showToastReal(exception);
            }

            @Override
            protected void onSuccess(ObjectResult<Void> result) {
                boolean success = ResultCode.defaultParser(result, true);
                if (success) {
                    dismissProgressDialog();
                    for (int i = 0; i < mMembers.size(); i++) {
                        if (mMembers.get(i) != null) {
                            L_.e(mMembers.get(i).getNickName());
                            L_.e(mMembers.get(i).getUserId() + "---------->个数");
                        }
                    }
                    FragmentController instance = FragmentController.getInstance();
                    HomeActivity activity1 = (HomeActivity) instance.getFragment(0).getActivity();
                    activity1.exitMucChat(mMembers.get(position).getUserId());
                    mMembers.remove(position);
                    //L_.e(mMembers.size()+"---------->个数");
                    //updateUI();
                    gridAdapter.notifyDataSetChanged();

                    // 更新消息界面
                    //MsgBroadcast.broadcastMsgNumReset(UIUtils.getContext());
                    //MsgBroadcast.broadcastMsgUiUpdate(UIUtils.getContext());


                   // CardcastActivity activity = (CardcastActivity) getActivity();
                   // activity.exitMucChat(friend.getUserId());


                }
            }
        });

    }


    private void deleteFriend(final BaseSortModel<Friend> sortFriend) {
//        mSortFriends.remove(sortFriend);
//        String firstLetter = sortFriend.getFirstLetter();
//        mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
//        mAdapter.notifyDataSetChanged();
//
//        Friend friend = sortFriend.getBean();
//        // 删除这个房间
//        FriendDao.getInstance().deleteFriend(mLoginUserId, friend.getUserId());
//        // 消息表中删除
//        ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, friend.getUserId());
//
//        // 更新消息界面
//        MsgBroadcast.broadcastMsgNumReset(getActivity());
//        MsgBroadcast.broadcastMsgUiUpdate(getActivity());
//
//        CardcastActivity activity = (CardcastActivity) getActivity();
//        activity.exitMucChat(friend.getUserId());
    }


    private void deleteRoom(final BaseSortModel<Friend> sortFriend) {

//        boolean deleteRoom = false;
//        if (mLoginUserId.equals(sortFriend.getBean().getRoomCreateUserId())) {
//            deleteRoom = true;
//        }
//        String url = null;
//        if (deleteRoom) {
//            url = activity.mConfig.ROOM_DELETE;
//        } else {
//            url = activity.mConfig.ROOM_MEMBER_DELETE;
//        }
//        HashMap<String, String> params = new HashMap<String, String>();
//        params.put("access_token", MyApplication.getInstance().mAccessToken);
//        params.put("roomId", sortFriend.getBean().getRoomId());
//        if (!deleteRoom) {
//            params.put("userId", mLoginUserId);
//        }
//
//        final ProgressDialog dialog = ProgressDialogUtil.init(getActivity(), null, getString(R.string.please_wait));
//        ProgressDialogUtil.show(dialog);
//        StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(url, new ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError arg0) {
//                ProgressDialogUtil.dismiss(dialog);
//                ToastUtil.showErrorNet(getActivity());
//            }
//        }, new StringJsonObjectRequest.Listener<Void>() {
//            @Override
//            public void onResponse(ObjectResult<Void> result) {
//                boolean success = Result.defaultParser(getActivity(), result, true);
//                if (success) {
//                    deleteFriend(sortFriend);
//                }
//                ProgressDialogUtil.dismiss(dialog);
//            }
//        }, Void.class, params);
//        activity.addDefaultRequest(request);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadMembers();
        }
    }
}

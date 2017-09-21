package com.sk.weichat.ui.message;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.message.MucRoom;
import com.sk.weichat.bean.message.MucRoom.Notice;
import com.sk.weichat.bean.message.MucRoomMember;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.circle.BasicInfoActivity;
import com.sk.weichat.util.DateFormatUtil;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.DataLoadView;
import com.sk.weichat.view.MyGridView;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;
import com.sk.weichat.volley.StringJsonObjectRequest.Listener;
import com.sk.weichat.xmpp.CoreService;
import com.sk.weichat.xmpp.CoreService.CoreServiceBinder;
import com.sk.weichat.xmpp.ListenerManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.ui.message
 * @作者:王阳
 * @创建时间: 2015年10月16日 下午3:13:40
 * @描述: 房间的详细信息
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: 添加EditText的字数限制
 */
public class RoomInfoActivity extends BaseActivity {
    private String mRoomJid;
    private String mLoginUserId;
    private Friend mRoom;

    private TextView mNoticeTv;
    private MyGridView mGridView;
    private TextView mRoomNameTv;
    private TextView mRoomDescTv;
    private TextView mCreatorTv;
    private TextView mCountTv;
    private TextView mNickNameTv;
    private TextView mCreateTime;
    private DataLoadView mDataLoadView;

    private List<MucRoomMember> mMembers;
    private GridViewAdapter mAdapter;

    private boolean dataInvalidate = true;// 数据是否有效，判断标准时传递进来的Occupant
    // list的首个人，是不是当前用户（因为传递进来的时候就把当前用户放到首位）
    private int add_minus_count = 2;// +号和-号的个数，如果权限可以踢人，就是2个，如果权限不可以踢人，就是1个

    private boolean mXmppBind;
    private CoreService mCoreService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_info);
        if (getIntent() != null) {
            mRoomJid = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        }
        if (TextUtils.isEmpty(mRoomJid)) {
            return;
        }
        mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
        mRoom = FriendDao.getInstance().getFriend(mLoginUserId, mRoomJid);
        if (mRoom == null || TextUtils.isEmpty(mRoom.getRoomId())) {
            return;
        }

        // 绑定服务
        mXmppBind = bindService(CoreService.getIntent(), mXmppServiceConnection, BIND_AUTO_CREATE);

        initView();
        loadMembers();
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
            mCoreService = ((CoreServiceBinder) service).getService();
        }
    };

    private void initView() {
        mNoticeTv = (TextView) findViewById(R.id.notice_tv);
        mGridView = (MyGridView) findViewById(R.id.grid_view);
        mRoomNameTv = (TextView) findViewById(R.id.room_name_tv);
        mRoomDescTv = (TextView) findViewById(R.id.room_desc_tv);
        mCreatorTv = (TextView) findViewById(R.id.creator_tv);
        mCountTv = (TextView) findViewById(R.id.count_tv);
        mNickNameTv = (TextView) findViewById(R.id.nick_name_tv);
        mCreateTime = (TextView) findViewById(R.id.create_timer);
        mDataLoadView = (DataLoadView) findViewById(R.id.data_load_view);
        mDataLoadView.setLoadingEvent(new DataLoadView.LoadingEvent() {
            @Override
            public void load() {
                loadMembers();
            }
        });

        mGridView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!doDel) {
                    return false;
                }
                doDel = false;
                mAdapter.notifyDataSetInvalidated();
                return false;
            }
        });
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (add_minus_count == 1) {
                    if (position == mMembers.size() - 1) {
                        List<String> existIds = new ArrayList<String>();
                        for (int i = 0; i < mMembers.size() - 1; i++) {
                            existIds.add(mMembers.get(i).getUserId());
                        }
                        // 去添加人
                        Intent intent = new Intent(RoomInfoActivity.this, AddContactsActivity.class);
                        intent.putExtra("roomId", mRoom.getRoomId());
                        intent.putExtra("roomJid", mRoomJid);
                        intent.putExtra("roomName", mRoomNameTv.getText().toString());
                        intent.putExtra("roomDes", mRoomDescTv.getText().toString());
                        intent.putExtra("exist_ids", JSON.toJSONString(existIds));
                        startActivityForResult(intent, 1);
                    } else {
                        if (!doDel && !doBannedVoice) {
                            MucRoomMember member = mMembers.get(position);
                            if (member != null) {
                                Intent intent = new Intent(RoomInfoActivity.this, BasicInfoActivity.class);
                                intent.putExtra(AppConstant.EXTRA_USER_ID, member.getUserId());
                                startActivity(intent);
                            }
                        }
                    }
                } else if (add_minus_count == 2) {
                    if (position == mMembers.size() - 2) {
                            List<String> existIds = new ArrayList<String>();
                            for (int i = 0; i < mMembers.size() - 2; i++) {
                                existIds.add(mMembers.get(i).getUserId());
                            }
                            // 去添加人
                            Intent intent = new Intent(RoomInfoActivity.this, AddContactsActivity.class);
                            intent.putExtra("roomId", mRoom.getRoomId());
                            intent.putExtra("roomJid", mRoomJid);
                            intent.putExtra("roomName", mRoomNameTv.getText().toString());
                            intent.putExtra("roomDes", mRoomDescTv.getText().toString());
                            intent.putExtra("exist_ids", JSON.toJSONString(existIds));
                            startActivityForResult(intent, 1);
                    } else if (position == mMembers.size() - 1) {
                        // delete
                        doDel = true;
                        mAdapter.notifyDataSetInvalidated();
                    } else {
                        if (!doDel && !doBannedVoice) {
                            MucRoomMember member = mMembers.get(position);
                            if (member != null) {
                                Intent intent = new Intent(RoomInfoActivity.this, BasicInfoActivity.class);
                                intent.putExtra(AppConstant.EXTRA_USER_ID, member.getUserId());
                                startActivity(intent);
                            }
                        }
                    }
                }
            }
        });
    }

    private void loadMembers() {
        mDataLoadView.showLoading();
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", MyApplication.getInstance().mAccessToken);
        params.put("roomId", mRoom.getRoomId());
        Log.d("wang", "mAccessToken::" + MyApplication.getInstance().mAccessToken + "roomId" + mRoom.getRoomId());
        StringJsonObjectRequest<MucRoom> request = new StringJsonObjectRequest<MucRoom>(mConfig.ROOM_GET, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError arg0) {
                ToastUtil.showErrorNet(mContext);
                mDataLoadView.showFailed();
            }
        }, new Listener<MucRoom>() {
            @Override
            public void onResponse(ObjectResult<MucRoom> result) {
                boolean success = Result.defaultParser(mContext, result, true);
                if (success && result.getData() != null) {
                    mDataLoadView.showSuccess();
                    updateUI(result.getData());
                } else {
                    ToastUtil.showErrorData(mContext);
                    mDataLoadView.showFailed();
                }
            }
        }, MucRoom.class, params);
        addDefaultRequest(request);
    }

    private void updateUI(MucRoom mucRoom) {
        List<Notice> notices = mucRoom.getNotices();
        if (notices != null && !notices.isEmpty()) {
            String text = notices.get(0).getText();
            mNoticeTv.setText(text);
        } else {
            mNoticeTv.setText("暂时无公告");
        }

        mRoomNameTv.setText(mucRoom.getName());
        mRoomDescTv.setText(mRoom.getDescription());
        mCreatorTv.setText(mucRoom.getNickName());
        mCountTv.setText(mucRoom.getMaxUserSize() + "");

        long createTime = mucRoom.getCreateTime();
        String formatDate = DateFormatUtil.getFormatDate(createTime * 1000);
        Log.d("wang", "createtime" + formatDate);
        Log.d("wang", "createtime:::" + mucRoom.toString() + "....." + createTime);
        mCreateTime.setText(TimeUtils.s_long_2_str(mucRoom.getCreateTime() * 1000));
        String myNickName = "";
        mMembers = mucRoom.getMembers();
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
                mMembers.add(0, my);
            }
        }
        mAdapter = new GridViewAdapter();
        mGridView.setAdapter(mAdapter);

        if (TextUtils.isEmpty(myNickName)) {
            mNickNameTv.setText(MyApplication.getInstance().mLoginUser.getNickName());
        } else {
            mNickNameTv.setText(myNickName);
        }

        if (mucRoom.getUserId().equals(mLoginUserId)) {// 我是创建者
            add_minus_count = 2;
            findViewById(R.id.room_name_arrow_img).setVisibility(View.VISIBLE);
            findViewById(R.id.room_desc_arrow_img).setVisibility(View.VISIBLE);
            findViewById(R.id.banned_voice_rl).setVisibility(View.VISIBLE);
            findViewById(R.id.room_name_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // change room name
                    showChangeRoomNameDialog(mRoomNameTv.getText().toString().trim());
                }
            });

            findViewById(R.id.room_desc_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // change room des
                    showChangeRoomDesDialog(mRoomDescTv.getText().toString().trim());
                }
            });
            findViewById(R.id.banned_voice_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {// 禁言
                    // change room des
                    doBannedVoice = true;
                    mAdapter.notifyDataSetChanged();
                }
            });
        } else {
            add_minus_count = 1;
            findViewById(R.id.room_name_arrow_img).setVisibility(View.INVISIBLE);
            findViewById(R.id.room_desc_arrow_img).setVisibility(View.INVISIBLE);
            findViewById(R.id.banned_voice_rl).setVisibility(View.INVISIBLE);
            findViewById(R.id.room_name_rl).setOnClickListener(null);
            findViewById(R.id.room_desc_rl).setOnClickListener(null);
            findViewById(R.id.banned_voice_rl).setOnClickListener(null);
        }

        findViewById(R.id.nick_name_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangeNickNameDialog(mNickNameTv.getText().toString().trim());
            }
        });

        if (add_minus_count == 1) {
            mMembers.add(null);// 一个+号
        } else if (add_minus_count == 2) {
            mMembers.add(null);// 一个+号
            mMembers.add(null);// 一个－号
        }
        // 添加新公告
        findViewById(R.id.notice_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // change room des
                showNewNoticeDialog(mNoticeTv.getText().toString());
            }
        });
    }

    private void showNewNoticeDialog(final String notice) {
        final EditText editText = new EditText(this);
        editText.setLines(2);
        editText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.add_notice).setView(editText)
                .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editText.getText().toString().trim();
                        if (TextUtils.isEmpty(text) || text.equals(notice)) {
                            return;
                        }
                        updateRoom(null, text, null);
                    }
                }).setNegativeButton(getString(R.string.cancel), null);
        builder.create().show();

    }

    private AlertDialog.Builder builderChangeNickNameDialog=null;

    /**
     * 修改群昵称
     *
     * @param nickName
     */
    private void showChangeNickNameDialog(final String nickName) {
        if (builderChangeNickNameDialog == null) {
            final EditText editText = new EditText(this);
            editText.setMaxLines(2);
            editText.setLines(2);
            editText.setText(nickName);
            //		editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
            ToastUtil.addEditTextNumChanged(RoomInfoActivity.this, editText, 8);
            editText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            builderChangeNickNameDialog = new AlertDialog.Builder(this).setTitle(R.string.change_my_nickname).setView(editText)
                    .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String text = editText.getText().toString().trim();
                            if (TextUtils.isEmpty(text) || text.equals(nickName)) {
                                return;
                            }
                            updateNickName(text);
                        }
                    }).setNegativeButton(getString(R.string.cancel), null);

            builderChangeNickNameDialog.create().show();
            builderChangeNickNameDialog=null;
        }
    }

    private void showChangeRoomNameDialog(final String roomName) {
        final EditText editText = new EditText(this);
        editText.setMaxLines(2);
        editText.setLines(2);
        editText.setText(roomName);
        //		editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
        ToastUtil.addEditTextNumChanged(RoomInfoActivity.this, editText, 8);
        editText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.change_room_name).setView(editText)
                .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editText.getText().toString().trim();
                        if (TextUtils.isEmpty(text) || text.equals(roomName)) {
                            return;
                        }
                        updateRoom(text, null, null);
                    }
                }).setNegativeButton(getString(R.string.cancel), null);
        builder.create().show();
    }

    private void showChangeRoomDesDialog(final String roomDes) {
        final EditText editText = new EditText(this);
        editText.setMaxLines(2);
        editText.setLines(2);
        editText.setText(roomDes);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        ToastUtil.addEditTextNumChanged(RoomInfoActivity.this, editText, 20);
        editText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.change_room_des).setView(editText)
                .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editText.getText().toString().trim();
                        if (TextUtils.isEmpty(text) || text.equals(roomDes)) {
                            return;
                        }
                        updateRoom(null, null, text);
                    }
                }).setNegativeButton(getString(R.string.cancel), null);
        builder.create().show();
    }

    private boolean doDel = false;
    private boolean doBannedVoice = false;

    private class GridViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mMembers.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(RoomInfoActivity.this).inflate(R.layout.item_room_info_view, parent, false);
            ImageView imageView = (ImageView) view.findViewById(R.id.content);
            Button button = (Button) view.findViewById(R.id.btn_del);

            if (position > mMembers.size() - (add_minus_count + 1)) {
                if (add_minus_count == 1) {
                    if (position == mMembers.size() - 1) {
                        imageView.setBackgroundResource(R.drawable.bg_room_info_add_btn);
                    }
                } else {
                    if (position == mMembers.size() - 2) {
                        imageView.setBackgroundResource(R.drawable.bg_room_info_add_btn);
                    }
                    if (position == mMembers.size() - 1) {
                        imageView.setBackgroundResource(R.drawable.bg_room_info_minus_btn);
                    }
                }
                button.setVisibility(View.GONE);
                if (doDel | doBannedVoice) {
                    view.setVisibility(View.GONE);
                } else {
                    view.setVisibility(View.VISIBLE);
                }
            } else {
                // String id=ChatJID.getID(mOccupants.get(position).getJid());
                AvatarHelper.getInstance().displayAvatar(mMembers.get(position).getUserId(), imageView, true);
                if (doDel | doBannedVoice) {
                    button.setVisibility(View.VISIBLE);
                } else {
                    button.setVisibility(View.GONE);
                }
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!dataInvalidate) {
                            return;
                        }
                        if (add_minus_count == 1) {
                            return;
                        }
                        if (doDel) {
                            if (mMembers.get(position).getUserId().equals(mLoginUserId)) {
                                ToastUtil.showToast(mContext, R.string.can_not_remove_self);
                                return;
                            }
                            deleteMember(position, mMembers.get(position).getUserId());
                        } else if (doBannedVoice) {
                            if (mMembers.get(position).getUserId().equals(mLoginUserId)) {
                                ToastUtil.showToast(mContext, R.string.can_not_banned_self);
                                return;
                            }
                            showBanndedVoiceDialog(position, mMembers.get(position).getUserId());
                        }

                    }
                });
            }
            return view;
        }
    }

    private void showBanndedVoiceDialog(final int position, final String userId) {
        CharSequence[] items = new CharSequence[]{"不禁言", "禁言一天", "禁言3天", "禁言一周", "禁言半个月", "禁言一个月"};
        new AlertDialog.Builder(mContext).setTitle(R.string.banned_voice).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int time = 0;
                int daySeconds = 24 * 60 * 60;
                switch (which) {
                    case 0:
                        time = 0;
                        break;
                    case 1:
                        time = daySeconds;
                        break;
                    case 2:
                        time = daySeconds * 3;
                        break;
                    case 3:
                        time = daySeconds * 7;
                        break;
                    case 4:
                        time = daySeconds * 15;
                        break;
                    case 5:
                        time = daySeconds * 30;
                        break;
                }
                bannedVoice(position, userId, TimeUtils.sk_time_current_time() + time);
            }
        }).create().show();
    }

    private void bannedVoice(final int position, String userId, final int time) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", MyApplication.getInstance().mAccessToken);
        params.put("roomId", mRoom.getRoomId());
        params.put("userId", userId);
        params.put("talkTime", String.valueOf(time));

        final ProgressDialog dialog = ProgressDialogUtil.init(mContext, null, getString(R.string.please_wait));
        ProgressDialogUtil.show(dialog);

        StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(mConfig.ROOM_MEMBER_UPDATE, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError arg0) {
                ToastUtil.showErrorNet(mContext);
                ProgressDialogUtil.dismiss(dialog);
            }
        }, new Listener<Void>() {
            @Override
            public void onResponse(ObjectResult<Void> result) {
                ProgressDialogUtil.dismiss(dialog);
                boolean success = Result.defaultParser(mContext, result, true);
                if (success) {
                    if (time > TimeUtils.sk_time_current_time()) {
                        ToastUtil.showToast(mContext, "禁言成功");
                    } else {
                        ToastUtil.showToast(mContext, "取消禁言成功");
                    }
                    doBannedVoice = false;
                    mAdapter.notifyDataSetInvalidated();
                }
            }
        }, Void.class, params);
        addDefaultRequest(request);
    }

    private void deleteMember(final int position, String userId) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", MyApplication.getInstance().mAccessToken);
        params.put("roomId", mRoom.getRoomId());
        params.put("userId", userId);

        final ProgressDialog dialog = ProgressDialogUtil.init(mContext, null, getString(R.string.please_wait));
        ProgressDialogUtil.show(dialog);

        StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(mConfig.ROOM_MEMBER_DELETE, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError arg0) {
                ToastUtil.showErrorNet(mContext);
                ProgressDialogUtil.dismiss(dialog);
            }
        }, new Listener<Void>() {
            @Override
            public void onResponse(ObjectResult<Void> result) {
                ProgressDialogUtil.dismiss(dialog);

                boolean success = Result.defaultParser(mContext, result, true);
                if (success) {
                    //System.out.println(result.toString()+"-----------------------");
                    mMembers.remove(position);
                    mAdapter.notifyDataSetInvalidated();
                }
            }
        }, Void.class, params);
        addDefaultRequest(request);
    }

    private void updateRoom(final String roomName, final String roomNotice, final String roomDes) {

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", MyApplication.getInstance().mAccessToken);
        params.put("roomId", mRoom.getRoomId());
        if (!TextUtils.isEmpty(roomName)) {
            params.put("roomName", roomName);
        }
        if (!TextUtils.isEmpty(roomNotice)) {
            params.put("notice", roomNotice);
        }
        if (!TextUtils.isEmpty(roomDes)) {
            params.put("desc", roomDes);
        }

        final ProgressDialog dialog = ProgressDialogUtil.init(mContext, null, getString(R.string.please_wait));
        ProgressDialogUtil.show(dialog);

        StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(mConfig.ROOM_UPDATE, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError arg0) {
                ToastUtil.showErrorNet(mContext);
                ProgressDialogUtil.dismiss(dialog);
            }
        }, new Listener<Void>() {
            @Override
            public void onResponse(ObjectResult<Void> result) {
                ProgressDialogUtil.dismiss(dialog);
                boolean success = Result.defaultParser(mContext, result, true);
                if (success) {
                    ToastUtil.showToast(mContext, R.string.update_success);
                    if (!TextUtils.isEmpty(roomName)) {
                        mRoomNameTv.setText(roomName);
                        mRoom.setNickName(roomName);
                        // 不去存入数据库，因为修改了房间名称后，会发一条推送，处理这条推送即可
                    }
                    if (!TextUtils.isEmpty(roomNotice)) {
                        // 修改了notice，也会有推送过来
                        mNoticeTv.setText(roomNotice);
                    }
                    if (!TextUtils.isEmpty(roomDes)) {
                        mRoomDescTv.setText(roomDes);
                        mRoom.setDescription(roomDes);
                        // 更新数据库
                        FriendDao.getInstance().createOrUpdateFriend(mRoom);
                    }
                }
            }
        }, Void.class, params);
        addDefaultRequest(request);
    }

    private void updateNickName(final String nickName) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", MyApplication.getInstance().mAccessToken);
        params.put("roomId", mRoom.getRoomId());
        params.put("userId", mLoginUserId);
        params.put("nickname", nickName);

        final ProgressDialog dialog = ProgressDialogUtil.init(mContext, null, getString(R.string.please_wait));
        ProgressDialogUtil.show(dialog);

        StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(mConfig.ROOM_MEMBER_UPDATE, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError arg0) {
                ToastUtil.showErrorNet(mContext);
                ProgressDialogUtil.dismiss(dialog);
            }
        }, new Listener<Void>() {
            @Override
            public void onResponse(ObjectResult<Void> result) {
                ProgressDialogUtil.dismiss(dialog);
                boolean success = Result.defaultParser(mContext, result, true);
                if (success) {
                    ToastUtil.showToast(mContext, R.string.update_success);
                    mNickNameTv.setText(nickName);
                    String loginUserId = MyApplication.getInstance().mLoginUser.getUserId();
                    FriendDao.getInstance().updateNickName(loginUserId, mRoom.getUserId(), nickName);
                    ChatMessageDao.getInstance().updateNickName(loginUserId, mRoom.getUserId(), loginUserId, nickName);
                    mRoom.setRoomMyNickName(nickName);
                    FriendDao.getInstance().createOrUpdateFriend(mRoom);
                    ListenerManager.getInstance().notifyNickNameChanged(mRoom.getUserId(), loginUserId, nickName);
                }
            }
        }, Void.class, params);
        addDefaultRequest(request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadMembers();
        }
    }
}

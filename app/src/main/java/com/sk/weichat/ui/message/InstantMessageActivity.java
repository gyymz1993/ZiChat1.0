package com.sk.weichat.ui.message;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.Constants;

import java.util.List;

public class InstantMessageActivity extends BaseActivity implements OnClickListener {
    private TextView mCreateChat;
    private ChatMessage message;
    private ListView mLvRecentlyMessage;
    private List<Friend> friends;
    private String instantFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messageinstant);
        message = (ChatMessage) getIntent().getParcelableExtra(Constants.INSTANT_MESSAGE);
        instantFilePath = getIntent().getStringExtra(Constants.INSTANT_MESSAGE_FILE);
        loadData();
        initView();

    }

    private void loadData() {
        friends = FriendDao.getInstance().getNearlyFriendMsg(MyApplication.getInstance().mLoginUser.getUserId());
    }

    private void initView() {
        getSupportActionBar().setTitle("选择");
        mCreateChat = (TextView) findViewById(R.id.tv_create_newmessage);
        mLvRecentlyMessage = (ListView) findViewById(R.id.lv_recently_message);
        mCreateChat.setOnClickListener(this);
        mLvRecentlyMessage.setAdapter(new MessageRecentlyAdapter());
        mLvRecentlyMessage.setOnItemClickListener(new ContactsItemClickListener());

    }

    private class ContactsItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // TODO Auto-generated method stub
            Friend friend = friends.get(position);
            showPopuWindow(view, friend);
        }

    }

    InstantMessageConfirm menuWindow;

    private void showPopuWindow(View view, Friend friend) {
        menuWindow = new InstantMessageConfirm(InstantMessageActivity.this, new ClickListener(message, friend), friend);
        // 显示窗口
        menuWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    /**
     * 事件的监听
     */
    class ClickListener implements OnClickListener {
        private ChatMessage message;
        private Friend friend;

        public ClickListener(ChatMessage message, Friend friend) {
            this.message = message;
            this.friend = friend;
        }

        @Override
        public void onClick(View v) {
            menuWindow.dismiss();
            switch (v.getId()) {
                case R.id.btn_send:// 发送
                    // TODO 判断单聊还是群聊
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(Constants.INSTANT_MESSAGE, message);
                    if (friend.getRoomFlag() == 0) {
                        if (friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)) {// 新朋友消息暂不处理
                        } else {// 单聊
                            Intent intent = new Intent(InstantMessageActivity.this, ChatActivity.class);
                              /*此处传文件这里message是没有什么用的,不过因为之前在chatactivity中已经对逻辑有了instantmessage非空的判断,所以这里就带上 */
                            intent.putExtra(Constants.INSTANT_MESSAGE_FILE, instantFilePath);
                            intent.putExtras(bundle);
                            intent.putExtra(ChatActivity.FRIEND, friend);
                            startActivity(intent);
                        }
                    } else {// 群聊
                        Intent intent = new Intent(InstantMessageActivity.this, MucChatActivity.class);
                        intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                         /*此处传文件这里message是没有什么用的,不过因为之前在chatactivity中已经对逻辑有了instantmessage非空的判断,所以这里就带上 */
                        intent.putExtra(Constants.INSTANT_MESSAGE_FILE, instantFilePath);
                        intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
                        intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }

                    InstantMessageActivity.this.finish();
                    break;
                case R.id.btn_cancle:// 取消

                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.tv_create_newmessage:
                Intent intent = new Intent(this, SelectNewContactsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constants.INSTANT_MESSAGE, message);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
                break;

            default:
                break;
        }

    }

    public class ViewHolder {
        ImageView mIvHead;
        TextView mTvName;
    }

    private class MessageRecentlyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (friends != null) {
                return friends.size();

            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (friends != null) {
                return friends.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            if (friends != null) {
                return position;
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = View.inflate(InstantMessageActivity.this, R.layout.item_recently_contacts, null);
                holder = new ViewHolder();
                holder.mIvHead = (ImageView) convertView.findViewById(R.id.iv_recently_contacts_head);
                holder.mTvName = (TextView) convertView.findViewById(R.id.tv_recently_contacts_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Friend friend = friends.get(position);
            if (friend.getRoomFlag() == 0) {// 这是单个人
                if (friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)) {// 系统消息的头像
                    holder.mIvHead.setImageResource(R.drawable.im_notice);
                } else if (friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)) {// 新朋友的头像
                    holder.mIvHead.setImageResource(R.drawable.im_new_friends);
                } else {// 其他
                    AvatarHelper.getInstance().displayAvatar(friend.getUserId(), holder.mIvHead, true);
                }
            } else {// 这是1个房间
                if (TextUtils.isEmpty(friend.getRoomCreateUserId())) {
                    holder.mIvHead.setImageResource(R.drawable.avatar_normal);
                } else {
                    AvatarHelper.getInstance().displayAvatar(friend.getRoomCreateUserId(), holder.mIvHead, true);// 目前在备注名放房间的创建者Id
                }
            }
            holder.mTvName.setText(friend.getNickName());
            return convertView;
        }
    }
}

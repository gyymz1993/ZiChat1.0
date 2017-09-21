package com.sk.weichat.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.message.NewFriendMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.util.ViewHolder;

public class NewFriendAdapter extends BaseAdapter {

	public static interface NewFriendActionListener {
		void addAttention(int position);// 加关注

		void sayHellow(int position);// 打招呼

		void removeBalckList(int position);// 移除黑名单

		void agree(int position);// 同意加好友

		void feedback(int position);// 拒绝加好友
	}

	private Context mContext;
	private List<NewFriendMessage> mNewFriends;
	private NewFriendActionListener mListener;

	public NewFriendAdapter(Context context, List<NewFriendMessage> newFriends, NewFriendActionListener listener) {
		mContext = context;
		mNewFriends = newFriends;
		mListener = listener;
	}

	@Override
	public int getCount() {
		return mNewFriends.size();
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
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.row_new_friend, parent, false);
		}
		ImageView avatar_img = ViewHolder.get(convertView, R.id.avatar_img);
		TextView nick_name_tv = ViewHolder.get(convertView, R.id.nick_name_tv);
		TextView des_tv = ViewHolder.get(convertView, R.id.des_tv);
		Button action_btn_1 = ViewHolder.get(convertView, R.id.action_btn_1);
		Button action_btn_2 = ViewHolder.get(convertView, R.id.action_btn_2);
		ImageView right_img = ViewHolder.get(convertView, R.id.right_img);

		final NewFriendMessage friend = mNewFriends.get(position);
		// 设置头像
		AvatarHelper.getInstance().displayAvatar(friend.getUserId(), avatar_img, true);
		// 昵称
		nick_name_tv.setText(friend.getNickName());
		// 重置状态
		action_btn_1.setVisibility(View.GONE);
		action_btn_2.setVisibility(View.GONE);
		action_btn_1.setOnClickListener(null);
		action_btn_2.setOnClickListener(null);
		right_img.setVisibility(View.GONE);
		des_tv.setText("");

		// 获取此人在我好友列表中的真实关系，以NewFriendMessage可能不准确
		Friend myFriend = FriendDao.getInstance().getFriend(friend.getOwnerId(), friend.getUserId());
		int status = (myFriend == null) ? Friend.STATUS_UNKNOW : myFriend.getStatus();

		if (status == Friend.STATUS_BLACKLIST) {// 黑名单关系,说明此人已经被我拉黑
			action_btn_1.setVisibility(View.VISIBLE);
			action_btn_1.setText(R.string.remove_blacklist);
			action_btn_1.setOnClickListener(new RemoveBlackListListener(position));
			des_tv.setText(R.string.his_in_the_blacklist);
			return convertView;
		}

		if (status == Friend.STATUS_FRIEND) {// 已经是朋友关系
			right_img.setVisibility(View.VISIBLE);

			if (friend.getType() == XmppMessage.TYPE_PASS) {// 别人发的同意加好友，那么就显示已通过
				if (friend.isMySend()) {// 是我发的
					des_tv.setText(R.string.agreed);// 已同意
				} else {
					des_tv.setText(R.string.passed);// 已通过
				}
			} else {// 其他状态不好判断的，时间显示为已互为好友
				des_tv.setText(R.string.is_the_friend);
			}
			return convertView;
		}

		/* 设置好友状态 */
		if (friend.isMySend()) {// 我发的状态
			switch (status) {
			case Friend.STATUS_UNKNOW:// 陌生人
				if (friend.getType() == XmppMessage.TYPE_FEEDBACK) {// OK 给陌生人的回复
					action_btn_1.setVisibility(View.VISIBLE);
					action_btn_1.setText(R.string.agree);
					action_btn_1.setOnClickListener(new AgreeListener(position));

					action_btn_2.setVisibility(View.VISIBLE);
					action_btn_2.setText(R.string.feedback);
					action_btn_2.setOnClickListener(new FeedbackListener(position));

					des_tv.setText(friend.getContent());
				} else {
					action_btn_1.setVisibility(View.VISIBLE);
					action_btn_1.setText(R.string.add_attention);
					action_btn_1.setOnClickListener(new AddAttentionListener(position));
				}
				break;
			case Friend.STATUS_ATTENTION:// 关注关系
				if (friend.getType() == XmppMessage.TYPE_NEWSEE) {// OK 我发的新关注，下一步是允许打招呼
					action_btn_1.setVisibility(View.VISIBLE);
					action_btn_1.setOnClickListener(new SayHelloListener(position));
					action_btn_1.setText(R.string.say_hello);
					des_tv.setText(R.string.add_attention_succ);
				} else if (friend.getType() == XmppMessage.TYPE_SAYHELLO) {// OK 我发的打招呼
					des_tv.setText(R.string.wait_verification);
				}
				break;
			}
		} else {// 别人发的状态
			switch (status) {
			case Friend.STATUS_UNKNOW:// 陌生人，可能是别人给我加关注、打招呼，也有可能是被拉入黑名单
				/*
				 * if (friend.getType() == XmppMessage.MSG_TYPE_BLACK) {// 是被别人拉入黑名单的，不能进行任何操作 des_tv.setText(R.string.you_in_black_list); } else
				 */if (friend.getType() == XmppMessage.TYPE_NEWSEE) {// OK 别人加关注的,那么收到方也显示加关注
					action_btn_1.setVisibility(View.VISIBLE);
					action_btn_1.setOnClickListener(new AgreeListener(position));
					action_btn_1.setText(R.string.add_attention);
				} else if (friend.getType() == XmppMessage.TYPE_SAYHELLO) {// OK 别人打招呼,那么收到方显示同意和回话
					action_btn_1.setVisibility(View.VISIBLE);
					action_btn_1.setOnClickListener(new AgreeListener(position));
					action_btn_1.setText(R.string.agree);

					action_btn_2.setVisibility(View.VISIBLE);
					action_btn_2.setText(R.string.feedback);
					action_btn_2.setOnClickListener(new FeedbackListener(position));

					des_tv.setText(friend.getContent());
				}
				break;
			case Friend.STATUS_ATTENTION:// OK 关注关系
				if (friend.getType() == XmppMessage.TYPE_FEEDBACK) {// 别人的回话
					action_btn_1.setVisibility(View.VISIBLE);
					action_btn_1.setOnClickListener(new SayHelloListener(position));
					action_btn_1.setText(R.string.say_hello_again);

					des_tv.setText(friend.getContent());
				} else {// 显示打招呼
					action_btn_1.setVisibility(View.VISIBLE);
					action_btn_1.setOnClickListener(new SayHelloListener(position));
					action_btn_1.setText(R.string.say_hello);
					des_tv.setText(R.string.add_attention_succ);
				}
				break;
			}
		}

		return convertView;
	}

	// 加关注
	private class AddAttentionListener implements View.OnClickListener {
		private int position;

		public AddAttentionListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			if (mListener != null) {
				mListener.addAttention(position);
			}
		}
	}

	// 打招呼
	private class SayHelloListener implements View.OnClickListener {
		private int position;

		public SayHelloListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			if (mListener != null) {
				mListener.sayHellow(position);
			}
		}
	}

	// 移除黑名单
	private class RemoveBlackListListener implements View.OnClickListener {
		private int position;

		public RemoveBlackListListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			if (mListener != null) {
				mListener.removeBalckList(position);
			}
		}
	}

	// 同意加好友
	private class AgreeListener implements View.OnClickListener {
		private int position;

		public AgreeListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			if (mListener != null) {
				mListener.agree(position);
			}
		}
	}

	// 同意加好友
	private class FeedbackListener implements View.OnClickListener {
		private int position;

		public FeedbackListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			if (mListener != null) {
				mListener.feedback(position);
			}
		}
	}

}

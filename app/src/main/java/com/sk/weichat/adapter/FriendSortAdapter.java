package com.sk.weichat.adapter;

import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.sortlist.BaseSortModel;
import com.sk.weichat.util.ViewHolder;

public class FriendSortAdapter extends BaseAdapter implements SectionIndexer {

	private Context mContext;
	private List<BaseSortModel<Friend>> mSortFriends;

	public FriendSortAdapter(Context context, List<BaseSortModel<Friend>> sortFriends) {
		mContext = context;
		mSortFriends = sortFriends;
	}

	@Override
	public int getCount() {
		return mSortFriends.size();
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
			convertView = LayoutInflater.from(mContext).inflate(R.layout.row_sort_friend, parent, false);
		}
		TextView catagoryTitleTv = ViewHolder.get(convertView, R.id.catagory_title);
		ImageView avatar_img = ViewHolder.get(convertView, R.id.avatar_img);
		TextView nick_name_tv = ViewHolder.get(convertView, R.id.nick_name_tv);
		TextView des_tv = ViewHolder.get(convertView, R.id.des_tv);

		// 根据position获取分类的首字母的Char ascii值
		int section = getSectionForPosition(position);
		// 如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
		if (position == getPositionForSection(section)) {
			catagoryTitleTv.setVisibility(View.VISIBLE);
			catagoryTitleTv.setText(mSortFriends.get(position).getFirstLetter());
		} else {
			catagoryTitleTv.setVisibility(View.GONE);
		}
		// 设置头像
		final Friend friend = mSortFriends.get(position).getBean();
		if (friend.getRoomFlag() == 0) {// 这是单个人
			if (friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)) {// 系统消息的头像
				avatar_img.setImageResource(R.drawable.im_notice);
			} else if (friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)) {// 新朋友的头像
				avatar_img.setImageResource(R.drawable.im_new_friends);
			} else {// 其他
				AvatarHelper.getInstance().displayAvatar(friend.getUserId(), avatar_img, true);
			}
		} else {// 这是1个房间
			if (TextUtils.isEmpty(friend.getRoomCreateUserId())) {
				avatar_img.setImageResource(R.drawable.avatar_normal);
			} else {
				AvatarHelper.getInstance().displayAvatar(friend.getRoomCreateUserId(), avatar_img, true);// 目前在备注名放房间的创建者Id
			}
		}

		// 昵称
		String name = friend.getRemarkName();
		if (TextUtils.isEmpty(name)) {
			name = friend.getNickName();
		}
		nick_name_tv.setText(name);

		// 个性签名
		des_tv.setText(friend.getDescription());
		return convertView;
	}

	/**
	 * 根据ListView的当前位置获取分类的首字母的Char ascii值
	 */
	public int getSectionForPosition(int position) {
		return mSortFriends.get(position).getFirstLetter().charAt(0);
	}

	/**
	 * 根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
	 */
	public int getPositionForSection(int section) {
		for (int i = 0; i < getCount(); i++) {
			String sortStr = mSortFriends.get(i).getFirstLetter();
			char firstChar = sortStr.toUpperCase().charAt(0);
			if (firstChar == section) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Object[] getSections() {
		return null;
	}

}

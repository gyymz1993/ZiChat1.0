package com.sk.weichat.ui.find;

import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.util.ViewHolder;

public class FindAdapter extends BaseAdapter {
	private List<PublicMessage> mMessages;
	private Context mContext;

	public FindAdapter(List<PublicMessage> messages, Context context) {
		this.mMessages = messages;
		this.mContext = context;
	}

	@Override
	public int getCount() {
		return mMessages.size();
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
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.item_find, parent, false);
		}
		ImageView avatar_img = ViewHolder.get(convertView, R.id.avatar_img);
		ImageView sex_img = ViewHolder.get(convertView, R.id.sex_img);
		sex_img.setVisibility(View.GONE);
		ImageView type_img = ViewHolder.get(convertView, R.id.type_img);
		TextView nickname_tv = ViewHolder.get(convertView, R.id.nick_name_tv);
		TextView praise_tv = ViewHolder.get(convertView, R.id.praise_tv);

		PublicMessage message = mMessages.get(position);

		AvatarHelper.getInstance().displayAvatar(message.getUserId(), avatar_img, false);

		String text = null;
		if (message.getBody() != null) {
			text = message.getBody().getText();
			if (!TextUtils.isEmpty(text)) {
				if (text.length() > 20) {
					text = text.substring(0, 20);
				}
			}
		}
		nickname_tv.setText(text);
		praise_tv.setText(message.getPraise() + "");
		int type = message.getType();
		type_img.setVisibility(View.VISIBLE);
		if (type == PublicMessage.TYPE_VIDEO) {
			type_img.setImageResource(R.drawable.avatar_icon_video);
		} else if (type == PublicMessage.TYPE_VOICE) {
			type_img.setImageResource(R.drawable.avatar_icon_voice);
		} else {
			type_img.setVisibility(View.GONE);
		}
		return convertView;
	}

}

package com.sk.weichat.adapter;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.audio.AudioPalyer;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.circle.Comment;
import com.sk.weichat.bean.circle.Praise;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.bean.circle.PublicMessage.Body;
import com.sk.weichat.bean.circle.PublicMessage.Resource;
import com.sk.weichat.db.dao.CircleMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.circle.BusinessCircleActivity;
import com.sk.weichat.ui.circle.BusinessCircleActivity.ListenerAudio;
import com.sk.weichat.ui.circle.BusinessCircleFragment;
import com.sk.weichat.ui.circle.BusinessCircleFragment.ListenerAudioFragment;
import com.sk.weichat.ui.circle.PMsgDetailActivity;
import com.sk.weichat.ui.circle.showCEView;
import com.sk.weichat.ui.tool.MultiImagePreviewActivity;
import com.sk.weichat.ui.tool.SingleImagePreviewActivity;
import com.sk.weichat.util.HtmlUtils;
import com.sk.weichat.util.LinkMovementClickMethod;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.SystemUtil;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.MyGridView;
import com.sk.weichat.view.OperationMorePopWindow;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.adapter
 * @作者:王阳
 * @创建时间: 2015年10月13日 下午5:21:17
 * @描述: 我的空间（商务圈）公共消息的接口
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: 增加一个接口在界面不可见时让音频停止
 */
public class PublicMessageAdapter extends BaseAdapter implements ListenerAudio, ListenerAudioFragment {

	private Context mContext;
	private List<PublicMessage> mMessages;
	private LayoutInflater mInflater;

	private String mLoginUserId;
	private String mLoginNickName;
	private ProgressDialog mProgressDialog;

	// 播放音频的记录
	private ViewHolder mVoicePlayViewHolder;// 当前正在播放的VocieViewHolder
	private AudioPalyer mAudioPalyer;
	private String mVoicePlayId = null;

	private Map<String, String> mShowNameMaps;
	private CommentAdapter mAdapter;

	public void reset() {
		if (mAudioPalyer != null) {
			mAudioPalyer.stop();
		}
	}

	public PublicMessageAdapter(Context context, List<PublicMessage> messages) {
		mContext = context;
		mMessages = messages;
		mInflater = LayoutInflater.from(mContext);
		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		mLoginNickName = MyApplication.getInstance().mLoginUser.getNickName();
		mShowNameMaps = new HashMap<String, String>();
		mProgressDialog = ProgressDialogUtil.init(mContext, null, mContext.getString(R.string.please_wait));
		mAudioPalyer = new AudioPalyer();
		mAudioPalyer.setAudioPlayListener(new AudioPalyer.AudioPlayListener() {
			@Override
			public void onSeekComplete() {
			}

			@Override
			public void onPrepared() {
			}

			@Override
			public void onError() {
				mVoicePlayId = null;
				if (mVoicePlayViewHolder != null) {
					updateVoiceViewHolderIconStatus(false, mVoicePlayViewHolder);
				}
				mVoicePlayViewHolder = null;
			}

			@Override
			public void onCompletion() {
				mVoicePlayId = null;
				if (mVoicePlayViewHolder != null) {
					updateVoiceViewHolderIconStatus(false, mVoicePlayViewHolder);
				}
				mVoicePlayViewHolder = null;
			}

			@Override
			public void onBufferingUpdate(int percent) {
			}

			@Override
			public void onPreparing() {

			}
		});

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
	public int getViewTypeCount() {
		return 10;
	}

	private static final int VIEW_TYPE_NORMAL_TEXT = 0;
	private static final int VIEW_TYPE_FW_TEXT = 1;
	private static final int VIEW_TYPE_NORMAL_SINGLE_IMAGE = 2;
	private static final int VIEW_TYPE_FW_SINGLE_IMAGE = 3;
	private static final int VIEW_TYPE_NORMAL_MULTI_IMAGE = 4;
	private static final int VIEW_TYPE_FW_MULTI_IMAGE = 5;
	private static final int VIEW_TYPE_NORMAL_VOICE = 6;
	private static final int VIEW_TYPE_FW_VOICE = 7;
	private static final int VIEW_TYPE_NORMAL_VIDEO = 8;
	private static final int VIEW_TYPE_FW_VIDEO = 9;

	/**
	 * @see PublicMessage#getType() <br/>
	 *      1=文字消息；2=图文消息；3=语音消息； 4=视频消息；5、转载<br/>
	 *      分的视图类型有： <br/>
	 *      {@link #VIEW_TYPE_NORMAL_TEXT}0、普通文字消息视图<br/>
	 *      {@link #VIEW_TYPE_FW_TEXT} 1、转载文字消息视图 <br/>
	 *      {@link #VIEW_TYPE_NORMAL_SINGLE_IMAGE} 2、普通单张图片的视图<br/>
	 *      {@link #VIEW_TYPE_FW_SINGLE_IMAGE} 3、转载单张图片的视图<br/>
	 *      {@link #VIEW_TYPE_NORMAL_MULTI_IMAGE}4、普通多张图片的视图<br/>
	 *      {@link #VIEW_TYPE_FW_MULTI_IMAGE} 5、转载多张图片的视图<br/>
	 *      {@link #VIEW_TYPE_NORMAL_VOICE} 6、普通音频视图<br/>
	 *      {@link #VIEW_TYPE_FW_VOICE} 7、转载音频视图<br/>
	 *      {@link #VIEW_TYPE_NORMAL_VIDEO}8、普通视频视图<br/>
	 *      {@link #VIEW_TYPE_FW_VIDEO} 9、转载视频视图<br/>
	 */
	@Override
	public int getItemViewType(int position) {
		PublicMessage message = mMessages.get(position);
		boolean fromSelf = message.getSource() == PublicMessage.SOURCE_SELF;
		Body body = message.getBody();
		if (body == null) {// 如果为空，那么可能是数据错误，直接返回一个普通的文本视图
			return VIEW_TYPE_NORMAL_TEXT;
		}
		if (body.getType() == PublicMessage.TYPE_TEXT) {// 文本视图
			if (fromSelf)
				return VIEW_TYPE_NORMAL_TEXT;
			else
				return VIEW_TYPE_FW_TEXT;
		} else if (body.getType() == PublicMessage.TYPE_IMG) {
			if (body.getImages() == null || body.getImages().size() <= 1) {// 普通的单张图片的视图
				if (fromSelf)
					return VIEW_TYPE_NORMAL_SINGLE_IMAGE;
				else
					return VIEW_TYPE_FW_SINGLE_IMAGE;
			} else {// 普通的多张图片视图
				if (fromSelf)
					return VIEW_TYPE_NORMAL_MULTI_IMAGE;
				else
					return VIEW_TYPE_FW_MULTI_IMAGE;
			}
		} else if (body.getType() == PublicMessage.TYPE_VOICE) {// 普通音频
			if (fromSelf)
				return VIEW_TYPE_NORMAL_VOICE;
			else
				return VIEW_TYPE_FW_VOICE;
		} else if (body.getType() == PublicMessage.TYPE_VIDEO) {// 普通视频
			if (fromSelf)
				return VIEW_TYPE_NORMAL_VIDEO;
			else
				return VIEW_TYPE_FW_VIDEO;
		} else {// 其他，数据错误
			return VIEW_TYPE_NORMAL_TEXT;
		}

	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		int viewType = getItemViewType(position);

		ViewHolder viewHolder = null;
		if (convertView == null || ((Integer) convertView.getTag(R.id.tag_key_list_item_type)) != viewType) {
			convertView = mInflater.inflate(R.layout.p_msg_item_main_body, null);
			View innerView = null;
			if (viewType == VIEW_TYPE_NORMAL_TEXT) {
				viewHolder = new NormalTextHolder();
			} else if (viewType == VIEW_TYPE_FW_TEXT) {
				FwTextHolder holder = new FwTextHolder();
				innerView = mInflater.inflate(R.layout.p_msg_item_fw_text, null);
				holder.text_tv = (TextView) innerView.findViewById(R.id.text_tv);
				viewHolder = holder;
			} else if (viewType == VIEW_TYPE_NORMAL_SINGLE_IMAGE) {
				NormalSingleImageHolder holder = new NormalSingleImageHolder();
				innerView = mInflater.inflate(R.layout.p_msg_item_normal_single_img, null);
				holder.image_view = (ImageView) innerView.findViewById(R.id.image_view);
				viewHolder = holder;
			} else if (viewType == VIEW_TYPE_FW_SINGLE_IMAGE) {
				FwSingleImageHolder holder = new FwSingleImageHolder();
				innerView = mInflater.inflate(R.layout.p_msg_item_fw_single_img, null);
				holder.text_tv = (TextView) innerView.findViewById(R.id.text_tv);
				holder.image_view = (ImageView) innerView.findViewById(R.id.image_view);
				viewHolder = holder;
			} else if (viewType == VIEW_TYPE_NORMAL_MULTI_IMAGE) {
				NormalMultiImageHolder holder = new NormalMultiImageHolder();
				innerView = mInflater.inflate(R.layout.p_msg_item_normal_multi_img, null);
				holder.grid_view = (MyGridView) innerView.findViewById(R.id.grid_view);
				viewHolder = holder;
			} else if (viewType == VIEW_TYPE_FW_MULTI_IMAGE) {
				FwMultiImageHolder holder = new FwMultiImageHolder();
				innerView = mInflater.inflate(R.layout.p_msg_item_fw_multi_img, null);
				holder.text_tv = (TextView) innerView.findViewById(R.id.text_tv);
				holder.grid_view = (MyGridView) innerView.findViewById(R.id.grid_view);
				viewHolder = holder;
			} else if (viewType == VIEW_TYPE_NORMAL_VOICE) {
				NormalVoiceHolder holder = new NormalVoiceHolder();
				innerView = mInflater.inflate(R.layout.p_msg_item_normal_voice, null);
				holder.img_view = (ImageView) innerView.findViewById(R.id.img_view);
				holder.voice_action_img = (ImageView) innerView.findViewById(R.id.voice_action_img);
				holder.voice_desc_tv = (TextView) innerView.findViewById(R.id.voice_desc_tv);
				viewHolder = holder;
			} else if (viewType == VIEW_TYPE_FW_VOICE) {
				FwVoiceHolder holder = new FwVoiceHolder();
				innerView = mInflater.inflate(R.layout.p_msg_item_fw_voice, null);
				holder.text_tv = (TextView) innerView.findViewById(R.id.text_tv);
				holder.img_view = (ImageView) innerView.findViewById(R.id.img_view);
				holder.voice_action_img = (ImageView) innerView.findViewById(R.id.voice_action_img);
				holder.voice_desc_tv = (TextView) innerView.findViewById(R.id.voice_desc_tv);
				viewHolder = holder;
			} else if (viewType == VIEW_TYPE_NORMAL_VIDEO) {
				NormalVideoHolder holder = new NormalVideoHolder();
				innerView = mInflater.inflate(R.layout.p_msg_item_normal_video, null);
				holder.video_thumb_img = (ImageView) innerView.findViewById(R.id.video_thumb_img);
				holder.video_desc_tv = (TextView) innerView.findViewById(R.id.video_desc_tv);
				viewHolder = holder;
			} else if (viewType == VIEW_TYPE_FW_VIDEO) {
				FwVideoHolder holder = new FwVideoHolder();
				innerView = mInflater.inflate(R.layout.p_msg_item_fw_video, null);
				holder.text_tv = (TextView) innerView.findViewById(R.id.text_tv);
				holder.video_thumb_img = (ImageView) innerView.findViewById(R.id.video_thumb_img);
				holder.video_desc_tv = (TextView) innerView.findViewById(R.id.video_desc_tv);
				viewHolder = holder;
			}
			viewHolder.avatar_img = (ImageView) convertView.findViewById(R.id.avatar_img);
			viewHolder.nick_name_tv = (TextView) convertView.findViewById(R.id.nick_name_tv);
			viewHolder.time_tv = (TextView) convertView.findViewById(R.id.time_tv);
			viewHolder.body_tv = (TextView) convertView.findViewById(R.id.body_tv);
			viewHolder.content_fl = (FrameLayout) convertView.findViewById(R.id.content_fl);
			viewHolder.delete_tv = (TextView) convertView.findViewById(R.id.delete_tv);
			viewHolder.operation_more_img = (ImageView) convertView.findViewById(R.id.operation_more_img);
			viewHolder.multi_praise_tv = (TextView) convertView.findViewById(R.id.multi_praise_tv);
			viewHolder.command_listView = (ListView) convertView.findViewById(R.id.command_listView);
			if (innerView != null) {
				viewHolder.content_fl.addView(innerView);
			}
			convertView.setTag(R.id.tag_key_list_item_type, viewType);
			convertView.setTag(R.id.tag_key_list_item_view, viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag(R.id.tag_key_list_item_view);
		}

		final ViewHolder finalHolder = viewHolder;// 和ViewHolder一样的，只不过用作匿名内部类里面调用需要final
		// set data
		final PublicMessage message = mMessages.get(position);
		if (message == null) {
			return convertView;
		}
		// 设备头像
		AvatarHelper.getInstance().displayAvatar(message.getUserId(), viewHolder.avatar_img, true);
		/* 设置昵称 */
		SpannableStringBuilder nickNamebuilder = new SpannableStringBuilder();
		final String userId = message.getUserId();
		String showName = getShowName(userId, message.getNickName());

		UserClickableSpan.setClickableSpan(mContext, nickNamebuilder, showName, message.getUserId());
		viewHolder.nick_name_tv.setText(nickNamebuilder);
		viewHolder.nick_name_tv.setLinksClickable(true);
		viewHolder.nick_name_tv.setMovementMethod(LinkMovementClickMethod.getInstance());
		// 设置头像的点击事件
		viewHolder.avatar_img.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mContext instanceof BusinessCircleActivity){//防止重复进入
					return;
				}
				Intent intent = new Intent(mContext, BusinessCircleActivity.class);
				intent.putExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_PERSONAL_SPACE);
				intent.putExtra(AppConstant.EXTRA_USER_ID, message.getUserId());
				intent.putExtra(AppConstant.EXTRA_NICK_NAME, mShowNameMaps.get(userId));
				mContext.startActivity(intent);
			}
		});

		// 获取消息本身的内容
		Body body = message.getBody();
		if (body == null) {
			return convertView;
		}

		// 是否是转载的
		boolean isForwarding = message.getSource() == PublicMessage.SOURCE_FORWARDING;

		// 设置body_tv
		if (TextUtils.isEmpty(body.getText())) {
			viewHolder.body_tv.setVisibility(View.GONE);
		} else {
			viewHolder.body_tv.setVisibility(View.VISIBLE);
			viewHolder.body_tv.setText(body.getText());
		}
		viewHolder.body_tv.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				showBodyTextLongClickDialog(finalHolder.body_tv.getText().toString());
				return true;
			}
		});

		// 设置发布时间
		viewHolder.time_tv.setText(TimeUtils.getFriendlyTimeDesc(mContext, (int) message.getTime()));

		// 设置删除按钮
		if (userId.equals(mLoginUserId)) {// 是我发的消息
			viewHolder.delete_tv.setVisibility(View.VISIBLE);
			viewHolder.delete_tv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showDeleteMsgDialog(position);
				}
			});
		} else {
			viewHolder.delete_tv.setVisibility(View.GONE);
			viewHolder.delete_tv.setOnClickListener(null);
		}

		viewHolder.operation_more_img.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOperationMorePopWindow(finalHolder.operation_more_img, position);
			}
		});

		/* 显示多少人赞过 */
		List<Praise> praises = message.getPraises();
		if (praises != null && praises.size() > 0) {
			viewHolder.multi_praise_tv.setVisibility(View.VISIBLE);
			SpannableStringBuilder builder = new SpannableStringBuilder();
			for (int i = 0; i < praises.size(); i++) {
				String praiseName = getShowName(praises.get(i).getUserId(), praises.get(i).getNickName());
				UserClickableSpan.setClickableSpan(mContext, builder, praiseName, praises.get(i).getUserId());
				if (i < praises.size() - 1)
					builder.append("、");
			}
			if (message.getPraise() > praises.size()) {
				builder.append("...");
			}
			viewHolder.multi_praise_tv.setText(builder);
		} else {
			viewHolder.multi_praise_tv.setVisibility(View.GONE);
			viewHolder.multi_praise_tv.setText("");
		}
		viewHolder.multi_praise_tv.setLinksClickable(true);
		viewHolder.multi_praise_tv.setMovementMethod(LinkMovementClickMethod.getInstance());

		/* 设置回复 */
		final List<Comment> comments = message.getComments();
		if (comments != null && comments.size() > 0) {
			viewHolder.command_listView.setVisibility(View.VISIBLE);
			mAdapter = new CommentAdapter(position, comments);
			viewHolder.command_listView.setAdapter(mAdapter);
		} else {
			viewHolder.command_listView.setVisibility(View.GONE);
			viewHolder.command_listView.setAdapter(null);
		}

		// //////////////////上面是公用的部分，下面是每个Type不同的部分/////////////////////////////////////////
		// 转载的消息会有一个转载人和text
		SpannableStringBuilder forwardingBuilder = null;
		if (isForwarding) {// 转载的那个人和说的话
			forwardingBuilder = new SpannableStringBuilder();
			String forwardName = getShowName(message.getFowardUserId(), message.getFowardNickname());
			UserClickableSpan.setClickableSpan(mContext, forwardingBuilder, forwardName, message.getFowardUserId());
			if (!TextUtils.isEmpty(message.getFowardText())) {
				forwardingBuilder.append(" : ");
				forwardingBuilder.append(message.getFowardText());
			}
		}

		if (viewType == VIEW_TYPE_NORMAL_TEXT) {
			viewHolder.content_fl.setVisibility(View.GONE);// 因为有个MarginTop
															// 5dp，所以没内容的时候隐藏，免得中间间隔有点大
		} else if (viewType == VIEW_TYPE_FW_TEXT) {
			TextView text_tv = ((FwTextHolder) viewHolder).text_tv;
			text_tv.setText(forwardingBuilder != null ? forwardingBuilder : "");
		} else if (viewType == VIEW_TYPE_NORMAL_SINGLE_IMAGE) {
			ImageView image_view = ((NormalSingleImageHolder) viewHolder).image_view;
			String url = message.getFirstImageOriginal();
			if (!TextUtils.isEmpty(url)) {
				ImageLoader.getInstance().displayImage(url, image_view);
				image_view.setOnClickListener(new SingleImageClickListener(url));
			} else {
				image_view.setImageBitmap(null);
			}
		} else if (viewType == VIEW_TYPE_FW_SINGLE_IMAGE) {
			TextView text_tv = ((FwSingleImageHolder) viewHolder).text_tv;
			ImageView image_view = ((FwSingleImageHolder) viewHolder).image_view;
			text_tv.setText(forwardingBuilder != null ? forwardingBuilder : "");

			String url = message.getFirstImageOriginal();
			if (!TextUtils.isEmpty(url)) {
				ImageLoader.getInstance().displayImage(url, image_view);
				image_view.setOnClickListener(new SingleImageClickListener(url));
			} else {
				image_view.setImageBitmap(null);
			}

		} else if (viewType == VIEW_TYPE_NORMAL_MULTI_IMAGE) {
			MyGridView grid_view = ((NormalMultiImageHolder) viewHolder).grid_view;
			if (body.getImages() != null) {
				grid_view.setAdapter(new ImagesInnerGridViewAdapter(mContext, body.getImages()));
				grid_view.setOnItemClickListener(new MultipleImagesClickListener(body.getImages()));
			} else {
				grid_view.setAdapter(null);
			}

		} else if (viewType == VIEW_TYPE_FW_MULTI_IMAGE) {
			TextView text_tv = ((FwMultiImageHolder) viewHolder).text_tv;
			MyGridView grid_view = ((FwMultiImageHolder) viewHolder).grid_view;
			text_tv.setText(forwardingBuilder != null ? forwardingBuilder : "");
			if (body.getImages() != null) {
				grid_view.setAdapter(new ImagesInnerGridViewAdapter(mContext, body.getImages()));
				grid_view.setOnItemClickListener(new MultipleImagesClickListener(body.getImages()));
			} else {
				grid_view.setAdapter(null);
			}
		} else if (viewType == VIEW_TYPE_NORMAL_VOICE) {
			final NormalVoiceHolder holder = (NormalVoiceHolder) viewHolder;
			if (mVoicePlayId == null || !mVoicePlayId.equals(message.getMessageId())) {// 处于非播放状态
				holder.voice_action_img.setImageResource(R.drawable.feed_main_player_play);
			} else {
				holder.voice_action_img.setImageResource(R.drawable.feed_main_player_pause);
			}
			holder.voice_action_img.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					play(holder, message);
				}
			});

			String imageUrl = message.getFirstImageOriginal();
			if (TextUtils.isEmpty(imageUrl)) {
				AvatarHelper.getInstance().displayAvatar(message.getUserId(), holder.img_view, false);
				holder.img_view.setOnClickListener(
						new SingleImageClickListener(AvatarHelper.getAvatarUrl(message.getUserId(), false)));
			} else {
				ImageLoader.getInstance().displayImage(imageUrl, holder.img_view);
				holder.img_view.setOnClickListener(new SingleImageClickListener(imageUrl));
			}

		} else if (viewType == VIEW_TYPE_FW_VOICE) {
			final FwVoiceHolder holder = (FwVoiceHolder) viewHolder;
			holder.text_tv.setText(forwardingBuilder != null ? forwardingBuilder : "");

			if (mVoicePlayId == null || !mVoicePlayId.equals(message.getMessageId())) {// 处于非播放状态
				holder.voice_action_img.setImageResource(R.drawable.feed_main_player_play);
			} else {
				holder.voice_action_img.setImageResource(R.drawable.feed_main_player_pause);
			}
			holder.voice_action_img.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					play(holder, message);
				}
			});

			String imageUrl = message.getFirstImageOriginal();
			if (TextUtils.isEmpty(imageUrl)) {
				AvatarHelper.getInstance().displayAvatar(message.getUserId(), holder.img_view, false);
				holder.img_view.setOnClickListener(
						new SingleImageClickListener(AvatarHelper.getAvatarUrl(message.getUserId(), false)));
			} else {
				ImageLoader.getInstance().displayImage(imageUrl, holder.img_view);
				holder.img_view.setOnClickListener(new SingleImageClickListener(imageUrl));
			}

		} else if (viewType == VIEW_TYPE_NORMAL_VIDEO) {
			NormalVideoHolder holder = (NormalVideoHolder) viewHolder;

			String imageUrl = message.getFirstImageOriginal();
			if (TextUtils.isEmpty(imageUrl)) {
				AvatarHelper.getInstance().displayAvatar(message.getUserId(), holder.video_thumb_img, false);
			} else {
				ImageLoader.getInstance().displayImage(imageUrl, holder.video_thumb_img);
			}

			// holder.video_desc_tv.setText();
		} else if (viewType == VIEW_TYPE_FW_VIDEO) {
			FwVideoHolder holder = (FwVideoHolder) viewHolder;
			holder.text_tv.setText(forwardingBuilder != null ? forwardingBuilder : "");

			String imageUrl = message.getFirstImageOriginal();
			if (TextUtils.isEmpty(imageUrl)) {
				AvatarHelper.getInstance().displayAvatar(message.getUserId(), holder.video_thumb_img, false);
			} else {
				ImageLoader.getInstance().displayImage(imageUrl, holder.video_thumb_img);
			}
		}

		return convertView;
	}

	class ViewHolder {
		ImageView avatar_img;
		TextView nick_name_tv;
		TextView time_tv;
		TextView body_tv;
		FrameLayout content_fl;
		TextView delete_tv;
		ImageView operation_more_img;
		TextView multi_praise_tv;
		ListView command_listView;
	}

	/* 普通的Text */
	class NormalTextHolder extends ViewHolder {
	}

	/* 转载的Text */
	class FwTextHolder extends ViewHolder {
		TextView text_tv;
	}

	/* 普通的单张图片 */
	class NormalSingleImageHolder extends ViewHolder {
		ImageView image_view;
	}

	/* 转载的单张图片 */
	class FwSingleImageHolder extends ViewHolder {
		TextView text_tv;
		ImageView image_view;
	}

	/* 普通的多张图片 */
	class NormalMultiImageHolder extends ViewHolder {
		MyGridView grid_view;
	}

	/* 转载的多张图片 */
	class FwMultiImageHolder extends ViewHolder {
		TextView text_tv;
		MyGridView grid_view;
	}

	/* 普通的音频 */
	class NormalVoiceHolder extends ViewHolder {
		ImageView img_view;
		ImageView voice_action_img;
		TextView voice_desc_tv;
	}

	/* 转载的音频 */
	class FwVoiceHolder extends ViewHolder {
		TextView text_tv;
		ImageView img_view;
		ImageView voice_action_img;
		TextView voice_desc_tv;
	}

	/* 普通的视频 */
	class NormalVideoHolder extends ViewHolder {
		ImageView video_thumb_img;
		TextView video_desc_tv;
	}

	/* 转载的视频 */
	class FwVideoHolder extends ViewHolder {
		TextView text_tv;
		ImageView video_thumb_img;
		TextView video_desc_tv;
	}

	public class CommentAdapter extends BaseAdapter {
		private int messagePosition;
		private List<Comment> datas;

		public CommentAdapter(int messagePosition, List<Comment> datas) {
			this.messagePosition = messagePosition;
			this.datas = datas;
		}

		@Override
		public int getCount() {
			return datas.size();
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
			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = LayoutInflater.from(mContext).inflate(R.layout.p_msg_comment_list_item, null);
				holder.text_view = (TextView) convertView.findViewById(R.id.text_view);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			final Comment comment = datas.get(position);
			SpannableStringBuilder builder = new SpannableStringBuilder();
			String showName = getShowName(comment.getUserId(), comment.getNickName());
			UserClickableSpan.setClickableSpan(mContext, builder, showName, comment.getUserId());// 设置评论者的ClickSpanned

			if (!TextUtils.isEmpty(comment.getToUserId()) && !TextUtils.isEmpty(comment.getToNickname())) {
				builder.append(mContext.getString(R.string.replay));
				String toShowName = getShowName(comment.getToUserId(), comment.getToNickname());
				UserClickableSpan.setClickableSpan(mContext, builder, toShowName, comment.getToUserId());// 设置被评论者的ClickSpanned
			}

			builder.append(":");
			// 设置评论内容
			String commentBody = comment.getBody();
			if (!TextUtils.isEmpty(commentBody)) {
				commentBody = StringUtils.replaceSpecialChar(comment.getBody());
				CharSequence charSequence = HtmlUtils.transform200SpanString(commentBody.replaceAll("\n", "\r\n"),
						true);
				builder.append(charSequence);
			}
			holder.text_view.setText(builder);
			holder.text_view.setLinksClickable(true);
			holder.text_view.setMovementMethod(LinkMovementClickMethod.getInstance());

			holder.text_view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (comment.getUserId().equals(mLoginUserId)) {// 如果消息是我发的，那么就弹出删除和复制的对话框
						showCommentLongClickDialog(messagePosition, position, CommentAdapter.this);
					} else {// 弹出回复的框
						String toShowName = getShowName(comment.getUserId(), comment.getNickName());
						// 懒得写回调的，直接强转，以后如果不适用，可以写个接口回调
						if (mContext instanceof BusinessCircleActivity) {
							((BusinessCircleActivity) mContext).showCommentEnterView(messagePosition,
									comment.getUserId(), comment.getNickName(), toShowName);

						} else {
							((MainActivity) mContext).getBusinessCircleFragment().showCommentEnterView(messagePosition,
									comment.getUserId(), comment.getNickName(), toShowName);
						}
						// show(messagePosition, comment.getUserId(),
						// comment.getNickName(),toShowName);
					}
				}
			});

			holder.text_view.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					showCommentLongClickDialog(messagePosition, position, CommentAdapter.this);
					return true;
				}
			});

			return convertView;
		}

		class ViewHolder {
			TextView text_view;
		}

	}

	/*
	 * 回调接口的编写 暂时没有用上
	 * 
	 */
	public showCEView ceView;

	public void setShowCEViewListener(showCEView ceView) {
		this.ceView = ceView;
	}

	public void show(int messagePosition, String toUserId, String toNickname, String toShowName) {
		BusinessCircleFragment fra = new BusinessCircleFragment();
		// BusinessCircleActivity fra=new BusinessCircleActivity();
		setShowCEViewListener(fra);
		if (ceView != null) {
			ceView.showView(messagePosition, toUserId, toNickname, toShowName);

		}
	}

	private class SingleImageClickListener implements View.OnClickListener {
		private String url;

		public SingleImageClickListener(String url) {
			this.url = url;
		}

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
			intent.putExtra(AppConstant.EXTRA_IMAGE_URI, url);
			mContext.startActivity(intent);
		}

	}

	private class MultipleImagesClickListener implements AdapterView.OnItemClickListener {
		private List<Resource> images;

		public MultipleImagesClickListener(List<Resource> images) {
			this.images = images;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if (images == null || images.size() <= 0) {
				return;
			}
			ArrayList<String> lists = new ArrayList<String>();
			for (int i = 0; i < images.size(); i++) {
				lists.add(images.get(i).getOriginalUrl());
			}
			Intent intent = new Intent(mContext, MultiImagePreviewActivity.class);
			intent.putExtra(AppConstant.EXTRA_IMAGES, lists);
			intent.putExtra(AppConstant.EXTRA_POSITION, position);
			intent.putExtra(AppConstant.EXTRA_CHANGE_SELECTED, false);
			mContext.startActivity(intent);
		}

	}

	public static String ToDBC(String input) {
		char[] c = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == 12288) {
				c[i] = (char) 32;
				continue;
			}
			if (c[i] > 65280 && c[i] < 65375)
				c[i] = (char) (c[i] - 65248);
		}
		return new String(c);
	}

	private String getShowName(String userId, String defaultName) {
		String showName = null;
		if (!mShowNameMaps.containsKey(userId)) {
			if (userId.equals(mLoginUserId)) {
				mShowNameMaps.put(userId, mLoginNickName);
			} else {
				Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, userId);
				if (friend != null) {
					mShowNameMaps.put(userId, friend.getShowName());
				}
			}
		}
		showName = mShowNameMaps.get(userId);
		if (TextUtils.isEmpty(showName)) {
			showName = defaultName;
			mShowNameMaps.put(userId, showName);
		}
		return showName;
	}

	/* 操作事件 */
	private void showDeleteMsgDialog(final int position) {
		new AlertDialog.Builder(mContext).setTitle(R.string.prompt_title).setMessage(R.string.delete_prompt)
				.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteMsg(position);
					}
				}).setNegativeButton(R.string.cancel, null).create().show();
	}

	private void deleteMsg(final int position) {
		final PublicMessage message = mMessages.get(position);
		if (message == null) {
			return;
		}
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("messageId", message.getMessageId());
		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(
				MyApplication.getInstance().getConfig().CIRCLE_MSG_DELETE, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtil.dismiss(mProgressDialog);
						ToastUtil.showErrorNet(mContext);
					}
				}, new StringJsonObjectRequest.Listener<Void>() {
					@Override
					public void onResponse(ObjectResult<Void> result) {
						boolean success = Result.defaultParser(mContext, result, true);
						if (success) {
							CircleMessageDao.getInstance().deleteMessage(message.getMessageId());// 删除数据库的记录（如果存在的话）
							mMessages.remove(position);
							notifyDataSetChanged();
						}
						ProgressDialogUtil.dismiss(mProgressDialog);
					}
				}, Void.class, params);
		((BaseActivity) mContext).addDefaultRequest(request);
	}

	private void showBodyTextLongClickDialog(final String text) {
		CharSequence[] items = new CharSequence[] { mContext.getString(R.string.copy) };
		new AlertDialog.Builder(mContext).setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0:// 复制文字
					SystemUtil.copyText(mContext, text);
					break;
				}
			}
		}).setCancelable(true).create().show();
	}

	private void showCommentLongClickDialog(final int messagePosition, final int commentPosition,
			final CommentAdapter adapter) {
		if (messagePosition < 0 || messagePosition >= mMessages.size()) {
			return;
		}
		final PublicMessage message = mMessages.get(messagePosition);
		if (message == null) {
			return;
		}
		final List<Comment> comments = message.getComments();
		if (comments == null) {
			return;
		}
		if (commentPosition < 0 || commentPosition >= comments.size()) {
			return;
		}
		final Comment comment = comments.get(commentPosition);

		CharSequence[] items;
		if (comment.getUserId().equals(mLoginUserId) || message.getUserId().equals(mLoginUserId)) {// 我的评论，或者我的消息，那么我就可以删除
			items = new CharSequence[] { mContext.getString(R.string.copy), mContext.getString(R.string.delete) };
		} else {
			items = new CharSequence[] { mContext.getString(R.string.copy) };
		}

		new AlertDialog.Builder(mContext).setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0:// 复制文字
					if (TextUtils.isEmpty(comment.getBody())) {
						return;
					}
					SystemUtil.copyText(mContext, comment.getBody());
					break;
				case 1:
					deleteComment(message.getMessageId(), comment.getCommentId(), comments, commentPosition, adapter);
					break;
				}
			}
		}).setCancelable(true).create().show();
	}

	/**
	 * 删除一条回复
	 */
	private void deleteComment(String messageId, String commentId, final List<Comment> comments,
			final int commentPosition, final CommentAdapter adapter) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("messageId", messageId);
		params.put("commentId", commentId);
		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(
				MyApplication.getInstance().getConfig().MSG_COMMENT_DELETE, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ToastUtil.showErrorNet(mContext);
					}
				}, new StringJsonObjectRequest.Listener<Void>() {

					@Override
					public void onResponse(ObjectResult<Void> result) {
						boolean success = Result.defaultParser(mContext, result, true);
						if (success) {
							comments.remove(commentPosition);
							adapter.notifyDataSetChanged();
						}
					}
				}, Void.class, params);
		((BaseActivity) mContext).addDefaultRequest(request);
	}

	/* 点击更多操作弹出的对话框 */
	private OperationMorePopWindow mOperationMorePop;

	private void showOperationMorePopWindow(View anchowView, final int position) {
		if (mOperationMorePop == null) {
			mOperationMorePop = new OperationMorePopWindow((Activity) mContext);
			mOperationMorePop.setOperationMoreListener(new OperationMorePopWindow.OperationMoreListener() {
				@Override
				public void onPraise(int messagePosition, boolean isPraise) {
					praiseOrCancle(messagePosition, isPraise);
				}

				@Override
				public void onGift(int messagePosition) {

				}

				@Override
				public void onComment(int messagePosition) {
					// 懒得写回调的，直接强转，以后如果不适用，可以写个接口回调
					if (mContext instanceof BusinessCircleActivity) {
						((BusinessCircleActivity) mContext).showCommentEnterView(messagePosition, null, null, null);
					} else {
						// 这里当处于的是一个fragment中时,我们让他评论的时候直接跳到详情页去评论
						PublicMessage message = mMessages.get(messagePosition);
						Intent intent = new Intent(mContext, PMsgDetailActivity.class);
						intent.putExtra("public_message", message);
						mContext.startActivity(intent);
					}
					// 这里有问题,所以暂不使用回调接口实现
					// show(messagePosition,null, null, null);
				}
			});
		}
		PublicMessage message = mMessages.get(position);
		if (message == null) {
			return;
		}
		mOperationMorePop.show(anchowView, position, message.getIsPraise() == 1 ? false : true);
	}

	/**
	 * 赞或者取消赞
	 * 
	 * @param messageId
	 * @param isPraise
	 */
	private void praiseOrCancle(final int position, final boolean isPraise) {
		final PublicMessage message = mMessages.get(position);
		if (message == null) {
			return;
		}

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("messageId", message.getMessageId());
		String requestUrl = null;
		if (isPraise) {
			requestUrl = MyApplication.getInstance().getConfig().MSG_PRAISE_ADD;
		} else {
			requestUrl = MyApplication.getInstance().getConfig().MSG_PRAISE_DELETE;
		}

		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(requestUrl, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ToastUtil.showErrorNet(mContext);
			}
		}, new StringJsonObjectRequest.Listener<Void>() {

			@Override
			public void onResponse(ObjectResult<Void> result) {
				boolean success = Result.defaultParser(mContext, result, true);
				if (success) {
					message.setIsPraise(isPraise ? 1 : 0);
					List<Praise> praises = message.getPraises();
					if (praises == null) {
						praises = new ArrayList<Praise>();
						message.setPraises(praises);
					}
					int praiseCount = message.getPraise();
					if (isPraise) {// 代表我点赞
						// 消息实体的改变
						Praise praise = new Praise();
						praise.setUserId(mLoginUserId);
						praise.setNickName(mLoginNickName);
						praises.add(0, praise);
						praiseCount++;
						message.setPraise(praiseCount);
					} else {// 取消我的赞
						// 消息实体的改变
						for (int i = 0; i < praises.size(); i++) {
							if (mLoginUserId.equals(praises.get(i).getUserId())) {
								praises.remove(i);
								praiseCount--;
								message.setPraise(praiseCount);
								break;
							}
						}
					}
					notifyDataSetChanged();
				}
			}
		}, Void.class, params);
		((BaseActivity) mContext).addDefaultRequest(request);
	}

	/**
	 * 停止播放声音
	 */
	private void stopVoice() {
		Log.d("wang", "stopVoice");
		if (mAudioPalyer != null) {
			mAudioPalyer.stop();
		}
	}
	private void stopVoiceIDE() {
		Log.d("wang", "stopVoiceIDE");
		if (mAudioPalyer != null) {
			mAudioPalyer.release();
		}
	}
	/********************** 播放声音 ***********************************/
	/**
	 * 
	 * @param status
	 * @param viewHolder
	 * @param msg_id
	 */
	private void play(ViewHolder viewHolder, PublicMessage message) {
		String voiceUrl = message.getFirstAudio();

		if (mVoicePlayId == null) {// 没有在播放
			try {
				mAudioPalyer.play(voiceUrl);
			} catch (Exception e) {
				e.printStackTrace();
			}
			mVoicePlayId = message.getMessageId();
			updateVoiceViewHolderIconStatus(true, viewHolder);
			mVoicePlayViewHolder = viewHolder;
		} else {
			if (mVoicePlayId == message.getMessageId()) {
				mAudioPalyer.stop();
				mVoicePlayId = null;
				updateVoiceViewHolderIconStatus(false, viewHolder);
				mVoicePlayViewHolder = null;
			} else {// 正在播放别的， 在播放这个
				mAudioPalyer.stop();
				mVoicePlayId = null;
				if (mVoicePlayViewHolder != null) {
					updateVoiceViewHolderIconStatus(false, mVoicePlayViewHolder);
				}
				try {
					mAudioPalyer.play(voiceUrl);
				} catch (Exception e) {
					e.printStackTrace();
				}

				mVoicePlayId = message.getMessageId();
				updateVoiceViewHolderIconStatus(true, viewHolder);
				mVoicePlayViewHolder = viewHolder;
			}

		}

	}

	private void updateVoiceViewHolderIconStatus(boolean play, ViewHolder viewHolder) {
		if (viewHolder instanceof NormalVoiceHolder) {// 普通音频
			if (play) {
				((NormalVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_pause);
			} else {
				((NormalVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_play);
			}
		} else {// 转载音频
			if (play) {
				((FwVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_pause);
			} else {
				((FwVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_play);
			}
		}
	}

	/**
	 * 节口回调的方法
	 */
	@Override
	public void ideChange() {
		stopVoice();
	}

	/**
	 * 节口回调的方法,
	 */
	@Override
	public void ideChangeFragment() {
//		stopVoiceIDE();
		
	}

}

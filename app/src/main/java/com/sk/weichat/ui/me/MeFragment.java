package com.sk.weichat.ui.me;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.ui.cardcast.CardcastActivity;
import com.sk.weichat.ui.circle.BusinessCircleActivity;
import com.sk.weichat.ui.tool.SingleImagePreviewActivity;

public class MeFragment extends EasyFragment implements View.OnClickListener {
	private ImageView mAvatarImg;
	private TextView mNickNameTv;
	private TextView mPhoneNumTv;

	public MeFragment() {
	}

	@Override
	protected int inflateLayoutId() {
		return R.layout.fragment_me;
	}

	@Override
	protected void onCreateView(Bundle savedInstanceState, boolean createView) {
		if (createView) {
			initView();
		}
	}

	private void initView() {
		findViewById(R.id.my_data_rl).setOnClickListener(this);
		findViewById(R.id.my_friend_rl).setOnClickListener(this);
		findViewById(R.id.my_space_rl).setOnClickListener(this);
		findViewById(R.id.local_video_rl).setOnClickListener(this);
		findViewById(R.id.setting_rl).setOnClickListener(this);

		mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
		mNickNameTv = (TextView) findViewById(R.id.nick_name_tv);
		mPhoneNumTv = (TextView) findViewById(R.id.phone_number_tv);

		String loginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		AvatarHelper.getInstance().displayAvatar(loginUserId, mAvatarImg, true);
		mNickNameTv.setText(MyApplication.getInstance().mLoginUser.getNickName());
		mPhoneNumTv.setText(MyApplication.getInstance().mLoginUser.getTelephone());

		mAvatarImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String loginUserId = MyApplication.getInstance().mLoginUser.getUserId();
				Intent intent = new Intent(getActivity(), SingleImagePreviewActivity.class);
				intent.putExtra(AppConstant.EXTRA_IMAGE_URI, AvatarHelper.getAvatarUrl(loginUserId, false));
				startActivity(intent);
				getActivity().overridePendingTransition(0, 0);
			}
		});

	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.my_data_rl:// 我的资料
			startActivityForResult(new Intent(getActivity(), BasicInfoEditActivity.class), 1);
			break;
		case R.id.my_friend_rl:// 我的朋友
			startActivity(new Intent(getActivity(), CardcastActivity.class));
			break;
		case R.id.my_space_rl:// 我的空间
		{
			Intent intent = new Intent(getActivity(), BusinessCircleActivity.class);
			intent.putExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_PERSONAL_SPACE);
			startActivity(intent);
		}
			break;
		case R.id.local_video_rl:// 本地视频
			startActivity(new Intent(getActivity(), LocalVideoActivity.class));
			break;
		case R.id.setting_rl:// 设置
			startActivity(new Intent(getActivity(), SettingActivity.class));
			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1 && resultCode == Activity.RESULT_OK) {// 个人资料更新了
			AvatarHelper.getInstance().displayAvatar(MyApplication.getInstance().mLoginUser.getUserId(), mAvatarImg, true);
			mNickNameTv.setText(MyApplication.getInstance().mLoginUser.getNickName());
		}
	}

}

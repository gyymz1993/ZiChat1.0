package com.sk.weichat.ui.circle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.android.volley.VolleyError;
import com.android.volley.Response.ErrorListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.sk.weichat.AppConfig;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.adapter.PublicMessageAdapter;
import com.sk.weichat.bean.MyPhoto;
import com.sk.weichat.bean.circle.Comment;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.db.dao.CircleMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.MyPhotoDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.FileDataHelper;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.ui.circle.BusinessCircleActivity.CommentReplyCache;
import com.sk.weichat.ui.circle.BusinessCircleActivity.ListenerAudio;
import com.sk.weichat.ui.groupchat.SelectContactsActivity;
import com.sk.weichat.ui.tool.MultiImagePreviewActivity;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.CarouselImageView;
import com.sk.weichat.view.PMsgBottomView;
import com.sk.weichat.view.ResizeLayout;
import com.sk.weichat.volley.ArrayResult;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonArrayRequest;
import com.sk.weichat.volley.StringJsonObjectRequest;
import com.ymz.baselibrary.utils.L_;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

public class BusinessCircleFragment extends EasyFragment implements showCEView {
	/**
	 * 本界面的类型 Constant.CIRCLE_TYPE_MY_BUSINESS,我的商务圈<br/>
	 * Constant。CIRCLE_TYPE_PERSONAL_SPACE，个人空间<br/>
	 */
	private int mType;
	/* mPageIndex仅用于商务圈情况下 */
	private int mPageIndex = 0;

	private PullToRefreshListView mPullToRefreshListView;

	/* 封面视图 */
	private View mMyCoverView;// 封面root view
	private CarouselImageView mCoverImg;// 封面图片ImageView
	private Button mInviteBtn;// 面试邀请按钮
	private ImageView mAvatarImg;// 用户头像
	private ResizeLayout mResizeLayout;
	private PMsgBottomView mPMsgBottomView;

	private List<PublicMessage> mMessages = new ArrayList<PublicMessage>();

	private PublicMessageAdapter mAdapter;

	private String mLoginUserId;// 当前登陆用户的UserId
	private String mLoginNickName;// 当前登陆用户的昵称

	/* 当前选择的是哪个用户的个人空间,仅用于查看个人空间的情况下 */
	private String mUserId;
	private String mNickName;
	// 自定义的弹出框类
	SelectPicPopupWindow menuWindow;

	public BusinessCircleFragment() {
		// initView();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		Log.d("wang", "onCreate");
		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		mLoginNickName = MyApplication.getInstance().mLoginUser.getNickName();

		if (TextUtils.isEmpty(mLoginUserId)) {// 容错
			return;
		}

		/*
		 * if (getIntent() != null) { mType =
		 * getIntent().getIntExtra(AppConstant.EXTRA_CIRCLE_TYPE,
		 * AppConstant.CIRCLE_TYPE_MY_BUSINESS);// 默认的为查看我的商务圈 mUserId =
		 * getIntent().getStringExtra(AppConstant.EXTRA_USER_ID); mNickName =
		 * getIntent().getStringExtra(AppConstant.EXTRA_NICK_NAME); }
		 */
		if (!isMyBusiness()) {// 如果查看的是个人空间的话，那么mUserId必须要有意义
			if (TextUtils.isEmpty(mUserId)) {// 没有带userId参数，那么默认看的就是自己的空间
				mUserId = mLoginUserId;
				mNickName = mLoginNickName;
			}
		}
	}

	@Override
	protected int inflateLayoutId() {
		return R.layout.activity_business_circle;
	}

	@Override
	protected void onCreateView(Bundle savedInstanceState, boolean createView) {
		Log.d("wang", "onCreateView");
		if (createView) {
			initView();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_add_business, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.add_myspace) {
			/*
			 * View contentView =
			 * View.inflate(getActivity().getApplicationContext(),
			 * R.layout.business_popupwindow,null); int width =
			 * LayoutParams.WRAP_CONTENT; int height =
			 * LayoutParams.WRAP_CONTENT; PopupWindow popupWindow = new
			 * PopupWindow(contentView, width, height);
			 * popupWindow.setFocusable(true);
			 * popupWindow.setOutsideTouchable(true);
			 * popupWindow.setBackgroundDrawable(new ColorDrawable(
			 * Color.TRANSPARENT));//这个属性一定要和上面的touchable一起设置,不然是没用的 //
			 * popupWindow.setAnimationStyle(R.style.popupmanager);
			 * popupWindow.showAsDropDown(item.getActionView(), 60, 60);
			 */
			// 实例化SelectPicPopupWindow
			menuWindow = new SelectPicPopupWindow(getActivity(), itemsOnClick);
			// 显示窗口
			menuWindow.showAtLocation(getActivity().findViewById(R.id.main_content),
					Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
			// 设置layout在PopupWindow中显示的位置
		}
		return super.onOptionsItemSelected(item);
	}

	// 为弹出窗口实现监听类
	private OnClickListener itemsOnClick = new OnClickListener() {

		public void onClick(View v) {
			menuWindow.dismiss();
			Intent intent = new Intent();
			switch (v.getId()) {
			case R.id.btn_send_text:// 发表文字
				// Toast.makeText(getActivity(), "btn_send_text被点击了",0).show();
				intent.setClass(getActivity(), SendShuoshuoActivity.class);
				intent.putExtra("type", 0);
				break;
			case R.id.btn_send_picture:// 发表图片
				// Toast.makeText(getActivity(),
				// "btn_send_picture被点击了",0).show();
				intent.setClass(getActivity(), SendShuoshuoActivity.class);
				intent.putExtra("type", 1);
				break;
			case R.id.btn_send_voice:// 发表语音
				// Toast.makeText(getActivity(), "btn_send_voice被点击了",0).show();
				intent.setClass(getActivity(), SendAudioActivity.class);
				break;
			case R.id.btn_send_video:// 发表视频
				// Toast.makeText(getActivity(), "btn_send_video被点击了",0).show();
				intent.setClass(getActivity(), SendVideoActivity.class);
				break;
			default:
				break;
			}
			startActivityForResult(intent, REQUEST_CODE_SEND_MSG);// 去发说说

		}

	};

	/**
	 * 是否是商务圈类型
	 * 
	 * @return
	 */
	private boolean isMyBusiness() {
		return mType == AppConstant.CIRCLE_TYPE_MY_BUSINESS;
	}

	/**
	 * 是否是个人空间类型之我的空间
	 * 
	 * @return
	 */
	private boolean isMySpace() {
		return mLoginUserId.equals(mUserId);
	}

	private void initView() {
		// initTopTitleBar();
		initCoverView();

		mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
		mPMsgBottomView = (PMsgBottomView) findViewById(R.id.bottom_view);

		mResizeLayout = (ResizeLayout) findViewById(R.id.resize_layout);
		mResizeLayout.setOnResizeListener(new ResizeLayout.OnResizeListener() {
			@Override
			public void OnResize(int w, int h, int oldw, int oldh) {
				if (oldh < h) {// 键盘被隐藏
					// mCommentReplyCache = null;
					// mPMsgBottomView.setHintText("");
					// mPMsgBottomView.reset();
				}
			}
		});

		mPMsgBottomView.setPMsgBottomListener(new PMsgBottomView.PMsgBottomListener() {
			@Override
			public void sendText(String text) {
				if (mCommentReplyCache != null) {
					mCommentReplyCache.text = text;
					addComment(mCommentReplyCache);
					mPMsgBottomView.hide();
				}
			}
		});
		mPullToRefreshListView.getRefreshableView().addHeaderView(mMyCoverView, null, false);
		mAdapter = new PublicMessageAdapter(getActivity(), mMessages);
		
		mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);

		mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>() {

			@Override
			public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
				requestData(true);
			}

			@Override
			public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
				requestData(false);
			}
		});

		mPullToRefreshListView.getRefreshableView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				PublicMessage message = mMessages.get((int) parent.getItemIdAtPosition(position));
				Intent intent = new Intent(getActivity(), PMsgDetailActivity.class);
				intent.putExtra("public_message", message);
				startActivity(intent);
			}
		});

		mPullToRefreshListView.getRefreshableView().setOnScrollListener(
				new PauseOnScrollListener(ImageLoader.getInstance(), true, true, new AbsListView.OnScrollListener() {
					@Override
					public void onScrollStateChanged(AbsListView view, int scrollState) {
						if (mPMsgBottomView.getVisibility() != View.GONE) {
							mPMsgBottomView.hide();
						}
					}

					@Override
					public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
							int totalItemCount) {
					}
				}));

		if (isMyBusiness()) {
			readFromLocal();
		} else {
			requestData(true);
		}

	}

	/*
	 * private void initTopTitleBar() { if (isMyBusiness()) {
	 * getSupportActionBar().setTitle(R.string.my_business_circle); } else { if
	 * (isMySpace()) { getSupportActionBar().setTitle(R.string.my_space); } else
	 * { String name = FriendDao.getInstance().getRemarkName(mLoginUserId,
	 * mUserId); if (TextUtils.isEmpty(name)) { name = mNickName; }
	 * getSupportActionBar().setTitle(name); } } }
	 */
	private void initCoverView() {
		Log.d("wang", "初始化View");
		mMyCoverView = LayoutInflater.from(getActivity()).inflate(R.layout.space_cover_view, null);
		mCoverImg = (CarouselImageView) mMyCoverView.findViewById(R.id.cover_img);
		mInviteBtn = (Button) mMyCoverView.findViewById(R.id.invite_btn);
		mAvatarImg = (ImageView) mMyCoverView.findViewById(R.id.avatar_img);
		// 邀请按钮
		mInviteBtn.setVisibility(View.GONE);// TODO 面试邀请按钮放这里太难看了，隐藏掉算求
		// 头像
		if (isMyBusiness() || isMySpace()) {
			AvatarHelper.getInstance().displayAvatar(mLoginUserId, mAvatarImg, true);
		} else {
			AvatarHelper.getInstance().displayAvatar(mUserId, mAvatarImg, true);
		}
		mAvatarImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {// 进入个人资料页
				Log.d("wang", "点击了mAvatarImg");
				Intent intent = new Intent(getActivity(), BasicInfoActivity.class);
				if (isMyBusiness() || isMySpace()) {
					intent.putExtra(AppConstant.EXTRA_USER_ID, mLoginUserId);
				} else {
					intent.putExtra(AppConstant.EXTRA_USER_ID, mUserId);
				}
				startActivity(intent);
			}
		});

		if (isMyBusiness() || isMySpace()) {
			mCoverImg.setUserId(mLoginUserId);
		} else {
			mCoverImg.setUserId(mUserId);
		}

		mCoverImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("wang", "点击了coverimg");
				if (mPhotos == null || mPhotos.size() <= 0) {
					return;
				}
				ArrayList<String> images = new ArrayList<String>();
				for (int i = 0; i < mPhotos.size(); i++) {
					images.add(mPhotos.get(i).getOriginalUrl());
				}
				Intent intent = new Intent(getActivity(), MultiImagePreviewActivity.class);
				intent.putExtra(AppConstant.EXTRA_IMAGES, images);
				startActivity(intent);
			}
		});
		loadPhotos();
	}

	private void loadPhotos() {
		if (isMyBusiness() || isMySpace()) {// 自己的，那么就直接从数据库加载我的相册
			mPhotos = MyPhotoDao.getInstance().getPhotos(mLoginUserId);
			setCoverPhotos(mPhotos);
			return;
		}
		// 别人的，那么就从网上请求
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("userId", mUserId);

		StringJsonArrayRequest<MyPhoto> request = new StringJsonArrayRequest<MyPhoto>(
				((ActionBackActivity) getActivity()).mConfig.USER_PHOTO_LIST, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
					}
				}, new StringJsonArrayRequest.Listener<MyPhoto>() {
					@Override
					public void onResponse(ArrayResult<MyPhoto> result) {
						boolean success = Result.defaultParser(getActivity(), result, false);
						if (success) {
							mPhotos = result.getData();
							setCoverPhotos(mPhotos);
						}
					}
				}, MyPhoto.class, params);
		((BaseActivity) getActivity()).addDefaultRequest(request);
	}

	private void setCoverPhotos(List<MyPhoto> photos) {
		if (photos == null || photos.size() <= 0) {
			return;
		}
		String[] coverPhotos = new String[photos.size()];
		for (int i = 0; i < photos.size(); i++) {
			coverPhotos[i] = photos.get(i).getOriginalUrl();
		}
		mCoverImg.setImages(coverPhotos);
	}

	private List<MyPhoto> mPhotos = null;

	private void readFromLocal() {
		FileDataHelper.readArrayData(getActivity(), mLoginUserId, FileDataHelper.FILE_BUSINESS_CIRCLE,
				new StringJsonArrayRequest.Listener<PublicMessage>() {
					@Override
					public void onResponse(ArrayResult<PublicMessage> result) {
						if (result != null && result.getData() != null) {
							mMessages.clear();
							mMessages.addAll(result.getData());
							mAdapter.notifyDataSetInvalidated();
						}
						requestData(true);
					}
				}, PublicMessage.class);
	}

	@Override
	public void onResume() {
		Log.d("wang", "onResume");
		if (mCoverImg != null) {
			mCoverImg.onResume();
		}
		super.onResume();
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onPause() {
		Log.d("wang", "onPause");
		super.onPause();
		
	}

	@Override
	public void onStop() {
		Log.d("wang", "onStop");
		if (mCoverImg != null) {
			mCoverImg.onStop();
		}
		if (listener != null) {
			listener.ideChangeFragment();
		}
		listener = null;
		super.onStop();
	}

	public void onStart() {
		super.onStart();
		setListenerAudioFragment(mAdapter);
		Log.d("wang", "onStart");
	};

	public void onDestroy() {
		super.onDestroy();
		Log.d("wang", "onDestroy");
	};

	/**
	 * 接口,调用外部类的方法,让应用不可见时停止播放声音
	 */
	ListenerAudioFragment listener;

	public void setListenerAudioFragment(ListenerAudioFragment listener) {
		this.listener = listener;
	}

	public interface ListenerAudioFragment {
		void ideChangeFragment();
	}

	private static final int REQUEST_CODE_SEND_MSG = 1;

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_SEND_MSG) {
			if (resultCode == Activity.RESULT_OK) {// 发说说成功
				String messageId = data.getStringExtra(AppConstant.EXTRA_MSG_ID);
				CircleMessageDao.getInstance().addMessage(mLoginUserId, messageId);
				requestData(true);
			}
		}
	}

	/********** 公共消息的数据请求部分 *********/

	/**
	 * 请求公共消息
	 * 
	 * @param isPullDwonToRefersh
	 *            是下拉刷新，还是上拉加载
	 */
	private void requestData(boolean isPullDwonToRefersh) {
		if (isMyBusiness()) {
			requestMyBusiness(isPullDwonToRefersh);
		} else {
			requestSpace(isPullDwonToRefersh);
		}
	}

	private void requestMyBusiness(final boolean isPullDwonToRefersh) {
		if (isPullDwonToRefersh) {
			mPageIndex = 0;
		}

		List<String> msgIds = CircleMessageDao.getInstance().getCircleMessageIds(mLoginUserId, mPageIndex,
				AppConfig.PAGE_SIZE);

		if (msgIds == null || msgIds.size() <= 0) {
			mPullToRefreshListView.onRefreshComplete(200);
			return;
		}

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("ids", JSON.toJSONString(msgIds));

		StringJsonArrayRequest<PublicMessage> request = new StringJsonArrayRequest<PublicMessage>(
				((ActionBackActivity) getActivity()).mConfig.MSG_GETS, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ToastUtil.showErrorNet(getActivity());
						mPullToRefreshListView.onRefreshComplete();
					}
				}, new StringJsonArrayRequest.Listener<PublicMessage>() {
					@Override
					public void onResponse(ArrayResult<PublicMessage> result) {
						boolean success = Result.defaultParser(getActivity(), result, true);
						if (success) {
							List<PublicMessage> datas = result.getData();
							if (isPullDwonToRefersh) {
								mMessages.clear();
							}
							if (datas != null && datas.size() > 0) {// 没有更多数据
								mPageIndex++;
								if (isPullDwonToRefersh) {
									FileDataHelper.writeFileData(getActivity(), mLoginUserId,
											FileDataHelper.FILE_BUSINESS_CIRCLE, result);
								}
								mMessages.addAll(datas);
							}
							mAdapter.notifyDataSetChanged();
						}
						mPullToRefreshListView.onRefreshComplete();
					}
				}, PublicMessage.class, params);
		((BaseActivity) getActivity()).addDefaultRequest(request);
	}

	private void requestSpace(final boolean isPullDwonToRefersh) {
		String messageId = null;
		if (!isPullDwonToRefersh && mMessages.size() > 0) {
			messageId = mMessages.get(mMessages.size() - 1).getMessageId();
		}

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("userId", mUserId);
		params.put("flag", PublicMessage.FLAG_NORMAL + "");

		if (!TextUtils.isEmpty(messageId)) {
			params.put("messageId", messageId);
		}
		params.put("pageSize", String.valueOf(AppConfig.PAGE_SIZE));

		StringJsonArrayRequest<PublicMessage> request = new StringJsonArrayRequest<PublicMessage>(
				((ActionBackActivity) getActivity()).mConfig.MSG_USER_LIST, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ToastUtil.showErrorNet(getActivity());
						mPullToRefreshListView.onRefreshComplete();
					}
				}, new StringJsonArrayRequest.Listener<PublicMessage>() {
					@Override
					public void onResponse(ArrayResult<PublicMessage> result) {
						boolean success = Result.defaultParser(getActivity(), result, true);
						if (success) {
							List<PublicMessage> datas = result.getData();
							if (isPullDwonToRefersh) {
								mMessages.clear();
							}
							if (datas != null && datas.size() > 0) {// 没有更多数据
								mMessages.addAll(datas);
							}
							mAdapter.notifyDataSetChanged();
						}
						mPullToRefreshListView.onRefreshComplete();
					}
				}, PublicMessage.class, params);
		((BaseActivity) getActivity()).addDefaultRequest(request);
	}

	private void addComment(CommentReplyCache cache) {
		Comment comment = new Comment();
		comment.setUserId(mLoginUserId);
		comment.setNickName(mLoginNickName);
		comment.setToUserId(cache.toUserId);
		comment.setToNickname(cache.toNickname);
		comment.setBody(cache.text);
		addComment(cache.messagePosition, comment);
	}

	/** 添加一条评论的操作 */
	/**
	 * 新一条回复
	 */
	private void addComment(final int position, final Comment comment) {
		final PublicMessage message = mMessages.get(position);
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("messageId", message.getMessageId());
		if (!TextUtils.isEmpty(comment.getToUserId())) {
			params.put("toUserId", comment.getToUserId());
		}
		if (!TextUtils.isEmpty(comment.getToNickname())) {
			params.put("toNickname", comment.getToNickname());
		}
		params.put("body", comment.getBody());

		L_.e("评论区");
		StringJsonObjectRequest<String> request = new StringJsonObjectRequest<String>(
				((ActionBackActivity) getActivity()).mConfig.MSG_COMMENT_ADD, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ToastUtil.showErrorNet(getActivity());
						L_.e("评论区  onErrorResponse"+arg0);
					}
				}, new StringJsonObjectRequest.Listener<String>() {

					@Override
					public void onResponse(ObjectResult<String> result) {
						L_.e("评论区  onErrorResponse"+result);
						L_.e(result.toString());
						boolean success = Result.defaultParser(getActivity(), result, true);
						if (success && result.getData() != null) {
							List<Comment> comments = message.getComments();
							if (comments == null) {
								comments = new ArrayList<Comment>();
								message.setComments(comments);
							}
							comment.setCommentId(result.getData());
							comments.add(0, comment);
							L_.e("评论区  刷新"+comment.toString());
							mAdapter.notifyDataSetChanged();
						}
					}
				}, String.class, params);
		((BaseActivity) getActivity()).addDefaultRequest(request);
	}

	public void showCommentEnterView(int messagePosition, String toUserId, String toNickname, String toShowName) {
		mCommentReplyCache = new CommentReplyCache();
		mCommentReplyCache.messagePosition = messagePosition;
		mCommentReplyCache.toUserId = toUserId;
		mCommentReplyCache.toNickname = toNickname;
		if (TextUtils.isEmpty(toUserId) || TextUtils.isEmpty(toNickname) || TextUtils.isEmpty(toShowName)) {
			mPMsgBottomView.setHintText("");
		} else {
			mPMsgBottomView.setHintText(getString(R.string.replay_text, toShowName));
		}
		mPMsgBottomView.show();
	}

	class CommentReplyCache {
		int messagePosition;// 消息的Position
		String toUserId;
		String toNickname;
		String text;
	}

	CommentReplyCache mCommentReplyCache = null;

	/**
	 * 这是的方法没有使用
	 */
	@Override
	public void showView(int messagePosition, String toUserId, String toNickname, String toShowName) {
		showCommentEnterView(messagePosition, toUserId, toNickname, toShowName);
	}

	/*
	 * @Override public void onBackPressed() { if (mPMsgBottomView != null &&
	 * mPMsgBottomView.getVisibility() == View.VISIBLE) {
	 * mPMsgBottomView.hide(); } else { super.onBackPressed(); } }
	 */

}

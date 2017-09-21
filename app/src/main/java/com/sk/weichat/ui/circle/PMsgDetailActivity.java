package com.sk.weichat.ui.circle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.User;
import com.sk.weichat.bean.circle.Comment;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.circle.view.PMsgAudioHeaderView;
import com.sk.weichat.ui.circle.view.PMsgDetailHeaderView;
import com.sk.weichat.ui.circle.view.PMsgImageHeaderView;
import com.sk.weichat.ui.circle.view.PMsgTypeView;
import com.sk.weichat.ui.circle.view.PMsgVideoHeaderView;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.CommentBottomView;
import com.sk.weichat.view.ResizeLayout;
import com.sk.weichat.volley.ArrayResult;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonArrayRequest;
import com.sk.weichat.volley.StringJsonObjectRequest;

public class PMsgDetailActivity extends BaseActivity {
	private static final int REUQEST_STATUS_NONE = 0;
	private static final int REUQEST_STATUS_LOADING = 1;
	private static final int REUQEST_STATUS_LOADED = 2;

	// 该条公共消息
	protected PublicMessage mPublicMessage;
	// UI
	private PullToRefreshListView mPullToRefreshListView;
	private PMsgDetailHeaderView mPMsgDetailView;
	private PMsgTypeView mPMsgTypeView;
	private ResizeLayout mResizeLayout;
	private CommentBottomView mCommentBottomView;
	private View mReplayLayout;
	private TextView mToNickNameTv;
	private ImageView mReplayClearImg;
	// listView数据和adapter
	private CommentDetailAdapter mAdapter;
	private List<Comment> mComments;
	// 个人信息的请求状态
	private int mUserInfoReuqestStatus = REUQEST_STATUS_NONE;

	/* 当前登陆用户的状态 */
	private int mFriendStatus;// 两者的好友状态
	private String mLoginUserId;// 当前登陆者的Id
	private String mLoginNickName;// 当前登陆者的昵称

	// 其他常量
	private int mTouchSlop;// Touch slop，判断滑动
	private float mLastY;// 最后一次滑动的位置
	private boolean mKeyboardShowed = false;// 键盘是否显示
	// 评论加载的index和pageSize
	private int mCommentPageIndex = 0;
	private final int mPageSize = 10;
	// 根据是否有值，判断是直接回复消息，还是回复某人
	private CommentReplyCache mCommentReplyCache;

	class CommentReplyCache {
		String toUserId;
		String toNickName;
		String toBody;
	}

	private boolean mNeedUpdate = true;// 首次进入在onResume需要更新数据，以后onResume不需要更新
	// private boolean mRegistered = false;

	private boolean isLandscape = false;// 是否是横屏

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setTitle(R.string.message_detail);
		mPublicMessage = (PublicMessage) getIntent().getSerializableExtra("public_message");
		if (mPublicMessage == null) {// 容错处理
			return;
		}

		mTouchSlop = ViewConfiguration.getTouchSlop();

		// 如果是音频和视频，就不自动锁屏
		if (mPublicMessage.getType() == PublicMessage.TYPE_VOICE || mPublicMessage.getType() == PublicMessage.TYPE_VIDEO) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		mComments = mPublicMessage.getComments();
		if (mComments == null) {// 确保mComments不为空
			mComments = new ArrayList<Comment>();
		}
		// if (mComments.size() <= 0) {
		// mCommentPageIndex = 0;
		// } else {
		// mCommentPageIndex = 1;
		// }
		mAdapter = new CommentDetailAdapter(mComments);
		setContentView(R.layout.activity_p_msg_detail);

		// if (LoginHelper.checkStatusForLogin(this, false)) {// 如果当前没有用户登录，那么就注册这个广播，监听登陆
		// registerReceiver(mUserLogInReceiver, new IntentFilter(LoginHelper.ACTION_LOGIN));
		// mRegistered = true;
		// }
      
		initView();
	}

	// private BroadcastReceiver mUserLogInReceiver = new BroadcastReceiver() {
	// @Override
	// public void onReceive(Context context, Intent intent) {
	// String action = intent.getAction();
	// if (action.equals(LoginHelper.ACTION_LOGIN)) {// 在此界面接收到登陆的消息
	// requestData(true, false);
	// }
	// }
	// };

	@Override
	protected void onResume() {
		super.onResume();

		if (mNeedUpdate) {
			mNeedUpdate = false;// 只在第一次进入的时候主动更新
			requestData(true, false);
		}

		if (mPMsgTypeView != null) {
			mPMsgTypeView.onResume();
		}
		// 放到onResume里面，因为有可能之前没登陆，点击某操作，然后登陆在返回，可以再onResume里获取最新登陆的mLoginUserId和mLoginNickName
		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		mLoginNickName = MyApplication.getInstance().mLoginUser.getNickName();
		// 同理，需要在onResume里面更新两个人之间的状态
		if (mPublicMessage != null) {
			if (mPublicMessage.getUserId().equals(mLoginUserId)) {
				mFriendStatus = Friend.STATUS_SELF;
			} else {
				mFriendStatus = FriendDao.getInstance().getFriendStatus(mLoginUserId, mPublicMessage.getUserId());
			}
		}
		mPMsgDetailView.setFriendStatus(mFriendStatus);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mPMsgTypeView != null) {
			mPMsgTypeView.onPause();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mPMsgTypeView != null && isFinishing()) {
			mPMsgTypeView.onDestory();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mPMsgTypeView != null) {
			mPMsgTypeView.onDestory();
		}
		// if (mRegistered) {
		// unregisterReceiver(mUserLogInReceiver);
		// }
	}

	private void initView() {
		mResizeLayout = (ResizeLayout) findViewById(R.id.resize_layout);
		mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
		mReplayLayout = findViewById(R.id.replay_layout);
		mToNickNameTv = (TextView) findViewById(R.id.to_nick_name_tv);
		mReplayClearImg = (ImageView) findViewById(R.id.replay_clear_img);

		// 判断键盘有没有显示
		mResizeLayout.setOnResizeListener(new ResizeLayout.OnResizeListener() {
			@Override
			public void OnResize(int w, int h, int oldw, int oldh) {
				mKeyboardShowed = !(oldh < h);
			}
		});

		mReplayClearImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mAdapter.selectPosition = -1;
				mCommentReplyCache = null;
				mAdapter.notifyDataSetChanged();
				updateReplayLayout();
			}
		});

		mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>() {
			@Override
			public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
				requestData(true, true);
			}

			@Override
			public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
				requestData(false, true);
			}
		});

		mPullToRefreshListView.getRefreshableView().setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == SCROLL_STATE_IDLE) {
					if (mPMsgTypeView instanceof PMsgVideoHeaderView) {
						mPullToRefreshListView.invalidate();
						((PMsgVideoHeaderView) mPMsgTypeView).showHide();
					}
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			}
		});

		mPullToRefreshListView.getRefreshableView().setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (isLandscape) {
					if (event.getAction() == MotionEvent.ACTION_MOVE) {
						return true;
					}
					return false;
				} else {
					if (!mKeyboardShowed) {
						return false;
					}
					if (event.getAction() != MotionEvent.ACTION_DOWN) {
						if (Math.abs(event.getY() - mLastY) > mTouchSlop) {
							mCommentBottomView.reset();
							return false;
						}
					}
					mLastY = event.getY();
					return false;
				}
			}
		});

		mCommentBottomView = (CommentBottomView) findViewById(R.id.comment_bottom_view);
		mCommentBottomView.setCommentBottomListener(new CommentBottomView.CommentBottomListener() {
			@Override
			public void sendText(String text) {
				// if (LoginHelper.checkStatusForLogin(PMsgDetailActivity.this, true)) {
				// return;
				// }

				Comment comment = new Comment();
				comment.setBody(text);
				comment.setUserId(mLoginUserId);
				comment.setNickName(mLoginNickName);
				comment.setTime(TimeUtils.sk_time_current_time());
				if (mCommentReplyCache != null) {
					comment.setToUserId(mCommentReplyCache.toUserId);
					comment.setToNickname(mCommentReplyCache.toNickName);
					comment.setToBody(mCommentReplyCache.toBody);
				}
				addComment(mPublicMessage.getMessageId(), comment);
				mCommentBottomView.reset();

				// 清除回复对象和隐藏视图
				if (mCommentReplyCache != null) {
					mAdapter.selectPosition = -1;
					mCommentReplyCache = null;
					mAdapter.notifyDataSetChanged();
					updateReplayLayout();
				}
			}

			@Override
			public void onGiftClick() {
				// if (LoginHelper.checkStatusForLogin(PMsgDetailActivity.this, true)) {
				// return;
				// }
				// TODO 跳转到礼物视图界面
				// startActivity(new Intent(PMsgDetailActivity.this, SendGiftActivity.class));
			}
		});

		mCommentBottomView.setHint(getString(R.string.replay));

		initHeaderView();
		initFooterView();
		mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();
	}

	protected void initHeaderView() {
		int type = mPublicMessage.getType();
		if (type == PublicMessage.TYPE_IMG) {
			mPMsgTypeView = new PMsgImageHeaderView(this);
		} else if (type == PublicMessage.TYPE_VOICE) {
			mPMsgTypeView = new PMsgAudioHeaderView(this);
		} else if (type == PublicMessage.TYPE_VIDEO) {
			mPMsgTypeView = new PMsgVideoHeaderView(this);
		}
		if (mPMsgTypeView != null) {
			mPullToRefreshListView.getRefreshableView().addHeaderView(mPMsgTypeView, null, false);
			mPMsgTypeView.attachPublicMessage(mPublicMessage);
		}

		mPMsgDetailView = new PMsgDetailHeaderView(this);
		mPMsgDetailView.setPublicMessage(mPublicMessage);
		mPMsgDetailView.setFriendStatus(mFriendStatus);

		mPMsgDetailView.setPMsgDetailListener(new PMsgDetailHeaderView.PMsgDetailListener() {
			@Override
			public void doPraise(boolean praise) {
				// TODO 赞或者取消赞
				praiseOrCancle(mPublicMessage.getMessageId(), praise);
			}

			@Override
			public void doFriend() {
				// TODO 关注，或者发消息等
			}
		});

		mPullToRefreshListView.getRefreshableView().addHeaderView(mPMsgDetailView, null, false);
	}

	/* 加一个INVISIBLE的FooterView主要是为了让ReplayLayout显示的时候，ListView不用刷新（刷新会导致跳动） */
	private void initFooterView() {
		View rooterView = LayoutInflater.from(this).inflate(R.layout.footer_view_p_msg_replay_layout, null);
		mFooterView = rooterView.findViewById(R.id.replay_layout);
		rooterView.setVisibility(View.INVISIBLE);
		mPullToRefreshListView.getRefreshableView().addFooterView(rooterView);
	}

	private View mFooterView;

	// private void initTopTitleBar() {
	// mTopTitleBar.initRightBtn(View.VISIBLE, R.drawable.title_share, new OnClickListener() {
	// @Override
	// public void onClick(View v) {
	// TODO 第三方平台分享
	// }
	// });
	// }

	@Override
	protected boolean onHomeAsUp() {
		doBack();
		return true;
	}

	@Override
	public void onBackPressed() {
		doBack();
	}

	private void doBack() {
		if (isLandscape) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			finish();
		}
	}

	/**
	 * 请求公共消息
	 * 
	 * @param isPullDwonToRefersh
	 *            是下拉刷新，还是上拉加载
	 */
	private void requestData(boolean isPullDwonToRefersh, boolean showToast) {
		if (isPullDwonToRefersh) {
			loadUserInfo();
			refreshPublicMessage(showToast);
		} else {
			loadComments();
		}
	}

	private void loadComments() {
		HashMap<String, String> params = new HashMap<String, String>();
		// params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("messageId", mPublicMessage.getMessageId());
		if (mComments != null && mComments.size() > 0) {
			params.put("commentId", mComments.get(mComments.size() - 1).getCommentId());
		}
		params.put("pageSize", mPageSize + "");
		params.put("pageIndex", mCommentPageIndex + "");
		StringJsonArrayRequest<Comment> request = new StringJsonArrayRequest<Comment>(mConfig.MSG_COMMENT_LIST, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ToastUtil.showErrorNet(PMsgDetailActivity.this);
				mPullToRefreshListView.onRefreshComplete();
			}
		}, new StringJsonArrayRequest.Listener<Comment>() {
			@Override
			public void onResponse(ArrayResult<Comment> result) {
				boolean success = Result.defaultParser(PMsgDetailActivity.this, result, true);
				if (success) {
					List<Comment> datas = result.getData();
					if (datas != null && datas.size() > 0) {
						mCommentPageIndex++;
						mComments.addAll(datas);
					}
					mAdapter.notifyDataSetChanged();
				}
				mPullToRefreshListView.onRefreshComplete();
			}
		}, Comment.class, params);
		addDefaultRequest(request);

	}

	private void refreshPublicMessage(final boolean showToast) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("messageId", mPublicMessage.getMessageId());

		// if (!LoginHelper.checkStatusForLogin(this, false)) {// 如果用户已经登录
		// params.put("access_token", MyApplication.getInstance().mAccessToken);
		// }

		StringJsonObjectRequest<PublicMessage> request = new StringJsonObjectRequest<PublicMessage>(mConfig.MSG_GET, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				if (showToast) {
					ToastUtil.showErrorNet(PMsgDetailActivity.this);
				}
				mPullToRefreshListView.onRefreshComplete();
			}
		}, new StringJsonObjectRequest.Listener<PublicMessage>() {
			@Override
			public void onResponse(ObjectResult<PublicMessage> result) {
				boolean success = Result.defaultParser(PMsgDetailActivity.this, result, showToast);
				if (success) {
					PublicMessage data = result.getData();
					if (data != null) {
						mPublicMessage = data;
						mComments.clear();
						if (data.getComments() != null) {
							mComments.addAll(data.getComments());
							mAdapter.notifyDataSetChanged();
						}
						// 刷新的话，只刷新出第一页的10条评论，所以这样判断
						if (mComments.size() <= 0) {
							mCommentPageIndex = 0;
						} else {
							mCommentPageIndex = 1;
						}

						mPMsgDetailView.setPublicMessage(mPublicMessage);
						// if (mPMsgTypeView != null) {
						// mPMsgTypeView.attachPublicMessage(mPublicMessage);
						// }
					}
				}
				mPullToRefreshListView.onRefreshComplete();
			}
		}, PublicMessage.class, params);
		addDefaultRequest(request);
	}

	/**
	 * 根据mUserInfoReuqestStatus的状态，保证UserInfo只刷新一次
	 */
	private void loadUserInfo() {
		if (mUserInfoReuqestStatus != REUQEST_STATUS_NONE) {// 如果已经请求完毕或者正在请求，那么不再请求
			return;
		}
		mUserInfoReuqestStatus = REUQEST_STATUS_LOADING;
		Map<String, String> params = new HashMap<String, String>();
		params.put("userId", mPublicMessage.getUserId());

		StringJsonObjectRequest<User> request = new StringJsonObjectRequest<User>(mConfig.USER_GET_URL, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				mUserInfoReuqestStatus = REUQEST_STATUS_NONE;
			}
		}, new StringJsonObjectRequest.Listener<User>() {

			@Override
			public void onResponse(ObjectResult<User> result) {
				boolean success = Result.defaultParser(PMsgDetailActivity.this, result, false);
				if (success && result.getData() != null) {
					mUserInfoReuqestStatus = REUQEST_STATUS_LOADED;
					mPMsgDetailView.setUser(result.getData());
				} else {
					mUserInfoReuqestStatus = REUQEST_STATUS_NONE;
				}
			}
		}, User.class, params);
		addDefaultRequest(request);

	}

	@SuppressWarnings("deprecation")
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		int type = mPublicMessage.getType();
		if (type == PublicMessage.TYPE_VIDEO) {
			if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mCommentBottomView.setVisibility(View.GONE);
				getSupportActionBar().hide();
				// 全屏设置
				WindowManager.LayoutParams params = getWindow().getAttributes();
				params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
				getWindow().setAttributes(params);
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

				mPullToRefreshListView.setPullToRefreshEnabled(false);
				isLandscape = true;
				/*
				 * 重写了setVisibility方法，使用的布局中最外层嵌套了一个LinearLayout，具体原因见 http://blog.csdn.net/sam_zhang1984/article/details/8157917
				 */
				mPMsgDetailView.setVisibility(View.GONE);
				mFooterView.setVisibility(View.GONE);
				// mPullToRefreshListView.getRefreshableView().removeHeaderView(mPMsgDetailView);
				// mPullToRefreshListView.getRefreshableView().removeFooterView(mFooterView);
				mAdapter.notifyDataSetInvalidated();

				((PMsgVideoHeaderView) mPMsgTypeView).setLandscapeMode();

			} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
				mCommentBottomView.setVisibility(View.VISIBLE);
				getSupportActionBar().show();
				// 非全屏设置
				WindowManager.LayoutParams params = getWindow().getAttributes();
				params.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
				getWindow().setAttributes(params);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

				mPullToRefreshListView.setPullToRefreshEnabled(true);
				isLandscape = false;
				mPMsgDetailView.setVisibility(View.VISIBLE);
				mFooterView.setVisibility(View.VISIBLE);
				// mPullToRefreshListView.getRefreshableView().addHeaderView(mPMsgDetailView);
				// mPullToRefreshListView.getRefreshableView().addFooterView(mFooterView);
				mAdapter.notifyDataSetInvalidated();
				((PMsgVideoHeaderView) mPMsgTypeView).setPortraitMode();

			}
		}

	}

	/**
	 * 赞或者取消赞
	 * 
	 * @param messageId
	 * @param isPraise
	 */
	private void praiseOrCancle(String messageId, boolean isPraise) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("messageId", messageId);
		String requestUrl = null;
		if (isPraise) {
			requestUrl = mConfig.MSG_PRAISE_ADD;
		} else {
			requestUrl = mConfig.MSG_PRAISE_DELETE;
		}

		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(requestUrl, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
			}
		}, new StringJsonObjectRequest.Listener<Void>() {

			@Override
			public void onResponse(ObjectResult<Void> result) {

			}
		}, Void.class, params);
		addShortRequest(request);
	}

	/**
	 * 新一条回复
	 */
	private void addComment(String messageId, final Comment comment) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("messageId", messageId);
		if (comment.isReplaySomeBody()) {
			params.put("toUserId", comment.getToUserId() + "");
			params.put("toNickname", comment.getToNickname());
			params.put("toBody", comment.getToBody());
		}
		params.put("body", comment.getBody());

		StringJsonObjectRequest<String> request = new StringJsonObjectRequest<String>(mConfig.MSG_COMMENT_ADD, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ToastUtil.showErrorNet(PMsgDetailActivity.this);
			}
		}, new StringJsonObjectRequest.Listener<String>() {
			@Override
			public void onResponse(ObjectResult<String> result) {
				boolean success = Result.defaultParser(PMsgDetailActivity.this, result, true);
				if (success && result.getData() != null) {// 评论成功
					comment.setCommentId(result.getData());
					mComments.add(0, comment);
					mAdapter.notifyDataSetChanged();
					upodateCommentCount(true);
				}
			}
		}, String.class, params);
		addShortRequest(request);
	}

	private void upodateCommentCount(boolean add) {
		if (mPublicMessage == null) {
			return;
		}
		int commentCount = mPublicMessage.getCommnet();
		if (add) {
			commentCount++;
		} else {
			commentCount--;
		}
		if (commentCount < 0) {
			commentCount = 0;
		}
		mPublicMessage.setCommnet(commentCount);
		if (mPMsgDetailView != null) {
			mPMsgDetailView.updateCommentCount();
		}
	}

	private void updateReplayLayout() {
		if (mCommentReplyCache == null) {
			mReplayLayout.setVisibility(View.GONE);
			mToNickNameTv.setText("");
		} else {
			mReplayLayout.setVisibility(View.VISIBLE);
			mToNickNameTv.setText(Friend.getShowName(mCommentReplyCache.toUserId, mCommentReplyCache.toNickName));
		}
	}

	private class CommentDetailAdapter extends BaseAdapter {
		private List<Comment> datas;
		private int selectPosition = -1;

		public CommentDetailAdapter(List<Comment> datas) {
			this.datas = datas;
		}

		@Override
		public int getCount() {
			if (isLandscape) {
				return 0;
			} else {
				if (datas.size() <= 0) {
					return 1;
				}
				return datas.size();
			}
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
				convertView = LayoutInflater.from(PMsgDetailActivity.this).inflate(R.layout.p_msg_comment_detail_list_item, parent, false);
				holder.avatar_img = (ImageView) convertView.findViewById(R.id.avatar_img);
				holder.content_tv = (TextView) convertView.findViewById(R.id.content_tv);
				holder.replay_tv = (TextView) convertView.findViewById(R.id.replay_tv);
				holder.replay_check_box = (CheckBox) convertView.findViewById(R.id.replay_check_box);
				holder.time_tv = (TextView) convertView.findViewById(R.id.time_tv);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			if (datas.size() <= 0) {
				convertView.setVisibility(View.GONE);
				return convertView;
			} else {
				convertView.setVisibility(View.VISIBLE);
			}

			final Comment comment = datas.get(position);

			AvatarHelper.getInstance().displayAvatar(comment.getUserId(), holder.avatar_img, true);
			String showName = Friend.getShowName(comment.getUserId(), comment.getNickName());
			holder.content_tv.setText(showName + ":" + comment.getBody());
			if (comment.isReplaySomeBody()) {
				holder.replay_tv.setText(getString(R.string.replay) + " " + Friend.getShowName(comment.getToUserId(), comment.getToNickname()));
			} else {
				holder.replay_tv.setText("");
			}
			holder.replay_check_box.setOnCheckedChangeListener(null);
			holder.replay_check_box.setChecked(selectPosition == position);
			holder.replay_check_box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						mCommentReplyCache = new CommentReplyCache();
						mCommentReplyCache.toUserId = comment.getUserId();
						mCommentReplyCache.toNickName = comment.getNickName();
						mCommentReplyCache.toBody = comment.getBody();

						selectPosition = position;

						notifyDataSetChanged();
						if (!mKeyboardShowed) {
							mCommentBottomView.show();
						}
					} else {
						selectPosition = -1;
						mCommentReplyCache = null;
					}
					updateReplayLayout();
				}
			});
			holder.time_tv.setText(TimeUtils.getFriendlyTimeDesc(PMsgDetailActivity.this, (int) comment.getTime()));
			return convertView;
		}

		class ViewHolder {
			ImageView avatar_img;
			TextView content_tv;
			TextView replay_tv;
			CheckBox replay_check_box;
			TextView time_tv;
		}

	}

}

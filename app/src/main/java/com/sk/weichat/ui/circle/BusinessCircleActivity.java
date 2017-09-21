package com.sk.weichat.ui.circle;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.alibaba.fastjson.JSON;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
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
import com.sk.weichat.ui.base.BaseActivity;
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

/**
 * 我的商务圈
 * 
 * 
 */
public class BusinessCircleActivity extends BaseActivity implements showCEView{
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
	public showCEView ceView;
	public void setShowCEViewListener(showCEView ceView){
		this.ceView=ceView;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		mLoginNickName = MyApplication.getInstance().mLoginUser.getNickName();

		if (TextUtils.isEmpty(mLoginUserId)) {// 容错
			return;
		}

		if (getIntent() != null) {
			mType = getIntent().getIntExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_MY_BUSINESS);// 默认的为查看我的商务圈
			mUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
			mNickName = getIntent().getStringExtra(AppConstant.EXTRA_NICK_NAME);
		}

		if (!isMyBusiness()) {// 如果查看的是个人空间的话，那么mUserId必须要有意义
			if (TextUtils.isEmpty(mUserId)) {// 没有带userId参数，那么默认看的就是自己的空间
				mUserId = mLoginUserId;
				mNickName = mLoginNickName;
			}
		}

		setContentView(R.layout.activity_business_circle);

		initView();
	}

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
		initTopTitleBar();
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
		mAdapter = new PublicMessageAdapter(this, mMessages);
		  setListenerAudio(mAdapter);//设置借口回调
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
				Intent intent = new Intent(BusinessCircleActivity.this, PMsgDetailActivity.class);
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
					public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					}
				}));

		if (isMyBusiness()) {
			readFromLocal();
		} else {
			requestData(true);
		}

	}

	private void initTopTitleBar() {
		if (isMyBusiness()) {
			getSupportActionBar().setTitle(R.string.my_business_circle);
		} else {
			if (isMySpace()) {
				getSupportActionBar().setTitle(R.string.my_space);
			} else {
				String name = FriendDao.getInstance().getRemarkName(mLoginUserId, mUserId);
				if (TextUtils.isEmpty(name)) {
					name = mNickName;
				}
				getSupportActionBar().setTitle(name);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (isMyBusiness() || isMySpace()) {// 允许发布说说等
			getMenuInflater().inflate(R.menu.menu_business, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	private static final int REQUEST_CODE_SEND_MSG = 1;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.send_text || id == R.id.send_image || id == R.id.send_audio || id == R.id.send_video) {
			Intent intent = new Intent();
			switch (id) {
			case R.id.send_text:// 发文字
				intent.setClass(BusinessCircleActivity.this, SendShuoshuoActivity.class);
				intent.putExtra("type", 0);
				break;
			case R.id.send_image:// 发图片
				intent.setClass(BusinessCircleActivity.this, SendShuoshuoActivity.class);
				intent.putExtra("type", 1);
				break;
			case R.id.send_audio:// 发语音
				intent.setClass(BusinessCircleActivity.this, SendAudioActivity.class);
				break;
			case R.id.send_video:// 发视频
				intent.setClass(BusinessCircleActivity.this, SendVideoActivity.class);
				break;
			}
			startActivityForResult(intent, REQUEST_CODE_SEND_MSG);// 去发说说
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void initCoverView() {
		mMyCoverView = LayoutInflater.from(this).inflate(R.layout.space_cover_view, null);
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
				Intent intent = new Intent(BusinessCircleActivity.this, BasicInfoActivity.class);
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
				if (mPhotos == null || mPhotos.size() <= 0) {
					return;
				}
				ArrayList<String> images = new ArrayList<String>();
				for (int i = 0; i < mPhotos.size(); i++) {
					images.add(mPhotos.get(i).getOriginalUrl());
				}
				Intent intent = new Intent(BusinessCircleActivity.this, MultiImagePreviewActivity.class);
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

		StringJsonArrayRequest<MyPhoto> request = new StringJsonArrayRequest<MyPhoto>(mConfig.USER_PHOTO_LIST, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
			}
		}, new StringJsonArrayRequest.Listener<MyPhoto>() {
			@Override
			public void onResponse(ArrayResult<MyPhoto> result) {
				boolean success = Result.defaultParser(BusinessCircleActivity.this, result, false);
				if (success) {
					mPhotos = result.getData();
					setCoverPhotos(mPhotos);
				}
			}
		}, MyPhoto.class, params);
		addDefaultRequest(request);
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
		FileDataHelper.readArrayData(this, mLoginUserId, FileDataHelper.FILE_BUSINESS_CIRCLE, new StringJsonArrayRequest.Listener<PublicMessage>() {
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
	protected void onResume() {
		if (mCoverImg != null) {
			mCoverImg.onResume();
		}
		super.onResume();
	}

	@Override
	protected void onStop() {
		if (mCoverImg != null) {
			mCoverImg.onStop();
		}
		if(listener!=null){
			listener.ideChange();
		}
		listener=null;
		super.onStop();
	}
	/**
	 * 接口,调用外部类的方法,让应用不可见时停止播放声音
	 */
	ListenerAudio listener;
    public void setListenerAudio(ListenerAudio listener){
    	this.listener=listener;
    }
	public interface ListenerAudio{
		void ideChange();
	}
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

		List<String> msgIds = CircleMessageDao.getInstance().getCircleMessageIds(mLoginUserId, mPageIndex, AppConfig.PAGE_SIZE);

		if (msgIds == null || msgIds.size() <= 0) {
			mPullToRefreshListView.onRefreshComplete(200);
			return;
		}

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("ids", JSON.toJSONString(msgIds));

		StringJsonArrayRequest<PublicMessage> request = new StringJsonArrayRequest<PublicMessage>(mConfig.MSG_GETS, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ToastUtil.showErrorNet(BusinessCircleActivity.this);
				mPullToRefreshListView.onRefreshComplete();
			}
		}, new StringJsonArrayRequest.Listener<PublicMessage>() {
			@Override
			public void onResponse(ArrayResult<PublicMessage> result) {
				boolean success = Result.defaultParser(BusinessCircleActivity.this, result, true);
				if (success) {
					List<PublicMessage> datas = result.getData();
					if (isPullDwonToRefersh) {
						mMessages.clear();
					}
					if (datas != null && datas.size() > 0) {// 没有更多数据
						mPageIndex++;
						if (isPullDwonToRefersh) {
							FileDataHelper.writeFileData(BusinessCircleActivity.this, mLoginUserId, FileDataHelper.FILE_BUSINESS_CIRCLE, result);
						}
						mMessages.addAll(datas);
					}
					mAdapter.notifyDataSetChanged();
				}
				mPullToRefreshListView.onRefreshComplete();
			}
		}, PublicMessage.class, params);
		addDefaultRequest(request);
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

		StringJsonArrayRequest<PublicMessage> request = new StringJsonArrayRequest<PublicMessage>(mConfig.MSG_USER_LIST, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ToastUtil.showErrorNet(BusinessCircleActivity.this);
				mPullToRefreshListView.onRefreshComplete();
			}
		}, new StringJsonArrayRequest.Listener<PublicMessage>() {
			@Override
			public void onResponse(ArrayResult<PublicMessage> result) {
				boolean success = Result.defaultParser(BusinessCircleActivity.this, result, true);
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
		addDefaultRequest(request);
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

		L_.e("addComment  :"+message.getMessageId()+"comment  :"+comment.toString());
		L_.e("评论区");
		StringJsonObjectRequest<String> request = new StringJsonObjectRequest<String>(mConfig.MSG_COMMENT_ADD, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ToastUtil.showErrorNet(BusinessCircleActivity.this);
			}
		}, new StringJsonObjectRequest.Listener<String>() {

			@Override
			public void onResponse(ObjectResult<String> result) {
				L_.e("评论区  onErrorResponse"+result);
				boolean success = Result.defaultParser(BusinessCircleActivity.this, result, true);
				if (success && result.getData() != null) {
					List<Comment> comments = message.getComments();
					if (comments == null) {
						comments = new ArrayList<Comment>();
						message.setComments(comments);
					}
					comment.setCommentId(result.getData());
					comments.add(0, comment);
					mAdapter.notifyDataSetChanged();
				}
			}
		}, String.class, params);
		addDefaultRequest(request);
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

	@Override
	public void onBackPressed() {
		if (mPMsgBottomView != null && mPMsgBottomView.getVisibility() == View.VISIBLE) {
			mPMsgBottomView.hide();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void showView(int messagePosition, String toUserId, String toNickname, String toShowName) {
           showCommentEnterView(messagePosition, toUserId, toNickname, toShowName);		
	}
}

//package com.sk.weichat.ui.cardcast;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//import android.content.Intent;
//import android.text.TextUtils;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.Button;
//import android.widget.ListView;
//import android.widget.TextView;
//
//import com.android.volley.Response.ErrorListener;
//import com.android.volley.VolleyError;
//import com.handmark.pulltorefresh.library.PullToRefreshBase;
//import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
//import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
//import com.handmark.pulltorefresh.library.PullToRefreshListView;
//import com.sk.im.AnalysisActivity;
//import com.sk.im.Constant;
//import com.sk.im.MyApplication;
//import com.sk.im.R;
//import com.sk.im.adapter.UserAdapter;
//import com.sk.im.bean.User;
//import com.sk.im.ui.circle.PersonalInfoActivity;
//import com.sk.im.util.ToastUtil;
//import com.sk.im.util.Utils;
//import com.sk.im.view.ClearEditText;
//import com.sk.im.view.TopNormalBar;
//import com.sk.im.volley.ArrayResult;
//import com.sk.im.volley.FastVolley;
//import com.sk.im.volley.Result;
//import com.sk.im.volley.StringJsonArrayRequest;
//import com.sk.im.volley.StringJsonArrayRequest.Listener;
//
///**
// * 
// */
//public class SearchFriendActivity extends AnalysisActivity {
//
//	private final int mPageSize = 20;
//
//	private TopNormalBar mTopTitleBar;
//	private ClearEditText mSearchEdit;
//	private Button mSearchBtn;
//	private PullToRefreshListView mPullToRefreshListView;
//	private TextView mNoResultTv;
//	private UserAdapter mAdapter;
//
//	private String mLastSearchText;// 保存最后一次有效搜索的text
//	private List<User> mUsers;
//
//	private String mLoginUserId;
//
//	@Override
//	protected void onCreate(android.os.Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_search_friend);
//		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
//		mUsers = new ArrayList<User>();
//		mAdapter = new UserAdapter(this, mLoginUserId, mUsers);
//		initView();
//	}
//
//	private void initView() {
//		initTopTitleBar();
//		mSearchEdit = (ClearEditText) findViewById(R.id.search_edit);
//		mSearchBtn = (Button) findViewById(R.id.search_btn);
//
//		mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
//		mNoResultTv = (TextView) findViewById(R.id.no_result_tv);
//		mPullToRefreshListView.setMode(Mode.PULL_FROM_END);
//		mPullToRefreshListView.setShowIndicator(false);
//		mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);
//
//		mPullToRefreshListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
//			@Override
//			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
//				search(false, mLastSearchText);
//			}
//		});
//
//		mPullToRefreshListView.getRefreshableView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//				Intent intent = new Intent(SearchFriendActivity.this, PersonalInfoActivity.class);
//				intent.putExtra(Constant.EXTRA_USER_ID, mUsers.get((int) id).getUserId());
//				startActivity(intent);
//			}
//		});
//
//		mSearchBtn.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				String text = mSearchEdit.getText().toString().trim();
//				if (text == null) {
//					return;
//				}
//				if (!Utils.isSearchNickName(text)) {
//					ToastUtil.showNormalToast(SearchFriendActivity.this, R.string.search_nick_name_format_error);
//					return;
//				}
//				mLastSearchText = text;
//				search(true, mLastSearchText);
//			}
//		});
//
//	}
//
//	/**
//	 * 请求公共消息
//	 * 
//	 * @param isPullDwonToRefersh
//	 *            是下拉刷新，还是上拉加载
//	 */
//	private void search(final boolean isPullDwonToRefersh, final String searchText) {
//
//		String userId = null;
//		if (!isPullDwonToRefersh && mUsers.size() > 0) {// 如果是下拉刷新，那么Index变为0
//			userId = mUsers.get(mUsers.size() - 1).getUserId();
//		}
//
//		HashMap<String, String> params = new HashMap<String, String>();
//		params.put("access_token", MyApplication.getInstance().mAccessToken);
//		if (!TextUtils.isEmpty(userId)) {
//			params.put("userId", userId);
//		}
//		params.put("pageSize", mPageSize + "");
//		if (!TextUtils.isEmpty(searchText)) {
//			params.put("nickname", searchText);
//		}
//
//		StringJsonArrayRequest<User> request = new StringJsonArrayRequest<User>(mConfig.FRIEND_SEARCH, new ErrorListener() {
//			@Override
//			public void onErrorResponse(VolleyError arg0) {
//				ToastUtil.showErrorNet(SearchFriendActivity.this);
//				mPullToRefreshListView.onRefreshComplete();
//			}
//		}, new Listener<User>() {
//			@Override
//			public void onResponse(ArrayResult<User> result) {
//				boolean success = Result.defaultParser(SearchFriendActivity.this, result, true);
//				if (success) {
//					if (isPullDwonToRefersh) {
//						mUsers.clear();
//					}
//
//					List<User> datas = result.getData();
//					if (datas == null || datas.size() <= 0) {// 没有更多数据
//						if (!isPullDwonToRefersh && mUsers.size() > 0) {
//							ToastUtil.showNoMoreData(SearchFriendActivity.this);
//						}
//					} else {
//						mUsers.addAll(datas);
//					}
//					mAdapter.notifyDataSetChanged();
//
//					if (mUsers.size() <= 0) {
//						mNoResultTv.setVisibility(View.VISIBLE);
//						mNoResultTv.setText(getString(R.string.search_no_result_user, searchText));
//					} else {
//						mNoResultTv.setVisibility(View.GONE);
//					}
//
//				}
//				mPullToRefreshListView.onRefreshComplete();
//
//			}
//		}, User.class, params);
//		request.setRetryPolicy(FastVolley.newDefaultRetryPolicy());
//		FastVolley.getInstance().add(request);
//	}
//
//	private void initTopTitleBar() {
//		mTopTitleBar = (TopNormalBar) findViewById(R.id.top_title_bar);
//		mTopTitleBar.title(R.string.search);
//		mTopTitleBar.rightBtn(View.GONE, 0, null);
//	}
//
//}

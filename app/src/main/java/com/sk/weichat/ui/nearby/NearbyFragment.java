package com.sk.weichat.ui.nearby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.sk.weichat.AppConfig;
import com.sk.weichat.AppConstant;
import com.sk.weichat.BdLocationHelper;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.adapter.UserAdapter;
import com.sk.weichat.bean.User;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.ui.circle.BasicInfoActivity;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.volley.ArrayResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonArrayRequest;

public class NearbyFragment extends EasyFragment {

	private PullToRefreshListView mPullToRefreshListView;
	private List<User> mUsers;
	private UserAdapter mAdapter;
	private int mPageIndex = 0;
	private boolean mNeedUpdate = true;
	/* 保存请求第一页数据时，定位的经纬度 */
	private double mLatitude;
	private double mLongitude;
	private Handler mHandler;
	//
	private BaseActivity mActivity;

	public NearbyFragment() {
		mUsers = new ArrayList<User>();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mHandler = new Handler();
	}

	@Override
	protected int inflateLayoutId() {
		return R.layout.layout_pullrefresh_list;
	}

	@Override
	protected void onCreateView(Bundle savedInstanceState, boolean createView) {
		if (createView) {
			initView();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mActivity = (BaseActivity) getActivity();
	}

	private void initView() {
		mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);

		mAdapter = new UserAdapter(mUsers, getActivity());
		mPullToRefreshListView.setAdapter(mAdapter);

		View emptyView = LayoutInflater.from(getActivity()).inflate(R.layout.layout_list_empty_view, null);
		mPullToRefreshListView.setEmptyView(emptyView);

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
				String userId = mUsers.get((int) id).getUserId();
				Intent intent = new Intent(getActivity(), BasicInfoActivity.class);
				intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
				startActivity(intent);
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().registerReceiver(mLocationUpdateReceiver, new IntentFilter(BdLocationHelper.ACTION_LOCATION_UPDATE));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mNeedUpdate) {
			mNeedUpdate = false;
			mPullToRefreshListView.post(new Runnable() {
				@Override
				public void run() {
					mPullToRefreshListView.setPullDownRefreshing(200);
				}
			});
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		getActivity().unregisterReceiver(mLocationUpdateReceiver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mHandler != null) {
			mHandler.removeCallbacksAndMessages(null);
		}
	}

	private BroadcastReceiver mLocationUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(BdLocationHelper.ACTION_LOCATION_UPDATE)) {
				mHandler.removeCallbacksAndMessages(null);
				mLatitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
				mLongitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
				HashMap<String, String> params = new HashMap<String, String>();
				params.put("pageIndex", String.valueOf(mPageIndex));
				params.put("pageSize", String.valueOf(AppConfig.PAGE_SIZE));
				params.put("latitude", String.valueOf(mLatitude));
				params.put("longitude", String.valueOf(mLongitude));
				params.put("access_token", MyApplication.getInstance().mAccessToken);
				requestData(params, true);
			}
		}
	};

	private void requestData(final boolean isPullDwonToRefersh) {
		if (isPullDwonToRefersh) {
			mPageIndex = 0;
		}

		if (mPageIndex == 0) {// 附近的请求需要定位，并且是请求第一页数据，那么就要重新获取下经纬度
			boolean waitUpdateLocation = true;
			double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
			double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
			if (MyApplication.getInstance().getBdLocationHelper().isLocationUpdate()) {
				if (latitude != 0 && longitude != 0) {
					waitUpdateLocation = false;
				}
			}
			if (waitUpdateLocation) {
				MyApplication.getInstance().getBdLocationHelper().requestLocation();// 等待请求定位返回
				mHandler.postDelayed(new Runnable() {// 5秒之内还没有定位成功，取消刷新
							@Override
							public void run() {
								ToastUtil.showToast(getActivity(), R.string.location_failed);
								mPullToRefreshListView.onRefreshComplete();
							}
						}, 5000);
				return;
			} else {
				mLatitude = latitude;
				mLongitude = longitude;
			}
		}

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("pageIndex", String.valueOf(mPageIndex));
		params.put("pageSize", String.valueOf(AppConfig.PAGE_SIZE));
		params.put("latitude", String.valueOf(mLatitude));
		params.put("longitude", String.valueOf(mLongitude));
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		requestData(params, isPullDwonToRefersh);
	}

	private void requestData(HashMap<String, String> params, final boolean isPullDwonToRefersh) {
		Log.e("NearbyFragment,",mActivity.mConfig.NEARBY_USER);
		StringJsonArrayRequest<User> request = new StringJsonArrayRequest<User>(mActivity.mConfig.NEARBY_USER, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ToastUtil.showErrorNet(getActivity());
				mPullToRefreshListView.onRefreshComplete();
			}
		}, new StringJsonArrayRequest.Listener<User>() {
			@Override
			public void onResponse(ArrayResult<User> result) {
				boolean success = Result.defaultParser(getActivity(), result, true);
				if (success) {
					mPageIndex++;
					if (isPullDwonToRefersh) {
						mUsers.clear();
					}
					List<User> datas = result.getData();
					if (datas != null && datas.size() > 0) {
						mUsers.addAll(datas);
					}
					mAdapter.notifyDataSetChanged();
				}
				mPullToRefreshListView.onRefreshComplete();
			}
		}, User.class, params);
		mActivity.addDefaultRequest(request);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_nearby, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.search) {
			startActivity(new Intent(getActivity(), UserSearchActivity.class));
		}
		return super.onOptionsItemSelected(item);
	}

}

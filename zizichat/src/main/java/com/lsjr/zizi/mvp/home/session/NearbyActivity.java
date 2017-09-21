package com.lsjr.zizi.mvp.home.session;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.lsjr.bean.ArrayResult;
import com.lsjr.callback.ChatArrayCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.db.User;
import com.lsjr.zizi.loader.AvatarHelper;
import com.tencent.mapsdk.raster.model.LatLng;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/14 18:01
 */

@SuppressLint("Registered")
public class NearbyActivity extends MvpActivity {

    @BindView(R.id.id_contacts)
    RecyclerView idRvGroup;
    private List<User> mUsers;
    private UserAdapter mAdapter;
    private int mPageIndex = 0;
    private boolean mNeedUpdate = true;
    /* 保存请求第一页数据时，定位的经纬度 */
    private double mLatitude;
    private double mLongitude;
    private Handler mHandler;
    //

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_nearby;
    }

    @Override
    protected void initView() {
        showProgressDialogWithText("加载中");
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        mUsers = new ArrayList<>();
        mHandler = new Handler();
        mAdapter = new UserAdapter(this,mUsers,R.layout.item_nearby);
        setTitleText("附近的人");
        setTopLeftButton(R.drawable.ic_back).
                setTitleTextColor(UIUtils.getColor(R.color.white)).
                setBackgroundColor(UIUtils.getColor(R.color.colorPrimary));
        idRvGroup.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        idRvGroup.setAdapter(mAdapter);
        requestData(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(mLocationUpdateReceiver, new IntentFilter(AppConfig.ACTION_LOCATION_UPDATE));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNeedUpdate) {
            mNeedUpdate = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
       unregisterReceiver(mLocationUpdateReceiver);
    }


    private BroadcastReceiver mLocationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AppConfig.ACTION_LOCATION_UPDATE)) {
                mHandler.removeCallbacksAndMessages(null);
                mLatitude = ConfigApplication.instance().getmLatitude();
                mLongitude = ConfigApplication.instance().getmLongitude();
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("pageIndex", String.valueOf(mPageIndex));
                params.put("pageSize", String.valueOf(AppConfig.PAGE_SIZE));
                params.put("latitude", String.valueOf(mLatitude));
                params.put("longitude", String.valueOf(mLongitude));
                params.put("access_token", ConfigApplication.instance().mAccessToken);
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
            double latitude = ConfigApplication.instance().getmLatitude();
            double longitude = ConfigApplication.instance().getmLongitude();
//            if (waitUpdateLocation) {
//                MyApplication.getInstance().getBdLocationHelper().requestLocation();// 等待请求定位返回
//                mHandler.postDelayed(new Runnable() {// 5秒之内还没有定位成功，取消刷新
//                    @Override
//                    public void run() {
//
//                    }
//                }, 5000);
//                return;
//            } else {
//                mLatitude = latitude;
//                mLongitude = longitude;
//            }
        }

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("pageIndex", String.valueOf(mPageIndex));
        params.put("pageSize", String.valueOf(AppConfig.PAGE_SIZE));
        params.put("latitude", String.valueOf(mLatitude));
        params.put("longitude", String.valueOf(mLongitude));
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        requestData(params, isPullDwonToRefersh);
    }

    private void requestData(HashMap<String, String> params, final boolean isPullDwonToRefersh) {
        Log.e("NearbyFragment,",AppConfig.NEARBY_USER);
        HttpUtils.getInstance().postServiceData(AppConfig.NEARBY_USER, params, new ChatArrayCallBack<User>(User.class) {
            @Override
            protected void onXError(String exception) {
                dismissProgressDialog();
            }

            @Override
            protected void onSuccess(ArrayResult<User> result) {
                dismissProgressDialog();
                boolean success = ResultCode.defaultParser(result, true);
                if (success) {
                    mPageIndex++;
                    if (isPullDwonToRefersh) {
                        mUsers.clear();
                    }
                    List<User> datas = result.getData();
                    if (datas != null && datas.size() > 0) {
                        mUsers.addAll(datas);
                    }
                    mAdapter.notifyDataSetChanged(datas);
                }
            }

        });


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public class UserAdapter extends ABaseRefreshAdapter<User> {

        public UserAdapter(Context context, List<User> datas, int itemLayoutId) {
            super(context, datas, itemLayoutId);
        }


        @Override
        protected void convert(BaseRecyclerHolder holder, User user, int position) {
            ImageView avatar_img = holder.getView(R.id.avatar_img);
            TextView nick_name_tv = holder.getView(R.id.nick_name_tv);
            TextView des_tv = holder.getView(R.id.des_tv);
            // 设置头像
            AvatarHelper.getInstance().displayAvatar(user, avatar_img, true);
            double latitude = 0;
            double longitude = 0;
            double latitude_end = 0;
            double longitude_end = 0;
            if (user != null && user.getLoginLog() != null) {
                latitude = user.getLoginLog().getLatitude();
                longitude = user.getLoginLog().getLongitude();
            }
            if (latitude != 0 && longitude != 0 && latitude_end != 0 && longitude_end != 0) {
                LatLng point_start = new LatLng(latitude, longitude);
                LatLng point_end = new LatLng(latitude_end , longitude_end);
                des_tv.setText("距离 "+ 100 + " 米");
            }else{
                des_tv.setText("该好友未公开位置信息");
            }
            nick_name_tv.setText(user.getNickName());
            holder.getView(R.id.info_layout).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(NearbyActivity.this, BasicInfoActivity.class);
                    intent.putExtra(AppConfig.EXTRA_USER_ID, user.getUserId());
                    startActivity(intent);
                    //openActivity(BasicInfoActivity.class);
                }
            });
        }
    }
}

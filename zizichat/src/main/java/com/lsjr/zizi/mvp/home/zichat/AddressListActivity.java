package com.lsjr.zizi.mvp.home.zichat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.andview.myrvview.LQRRecyclerView;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.Poi;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.lsjr.zizi.MyApp;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.SwipeBackActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.zichat.presenter.AddressList;
import com.lsjr.zizi.three.LocationService;
import com.lsjr.zizi.util.LogUtils;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ymz.baselibrary.view.PermissionListener;
import com.zhy.autolayout.AutoLinearLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;


/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/28 14:45
 */

public class AddressListActivity extends SwipeBackActivity<AddressList.Presenter> implements
        OnGetPoiSearchResultListener,AddressList.IView,OnGetGeoCoderResultListener {

    int maxHeight = UIUtils.dip2px(300);
    int minHeight = UIUtils.dip2px(150);
    @BindView(R.id.map)
    MapView map;
    @BindView(R.id.rlMap)
    RelativeLayout mRlMap;
    @BindView(R.id.ibShowLocation)
    ImageButton mIbShowLocation;
    @BindView(R.id.rvPOI)
    LQRRecyclerView mRvPOI;
    @BindView(R.id.pb)
    ProgressBar mPb;

    GeoCoder mSearch = null; // 搜索模块，也可去掉地图模块独立使用
    BaiduMap mBaiduMap = null;
    private LocationService locationService;
    private PoiSearch mPoiSearch = null;


    @Override
    protected AddressList.Presenter createPresenter() {
        return new AddressList.Presenter(this,this);
    }

    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("当前位置");
        getToolBarView().getRightTextView().setVisibility(View.VISIBLE);
        getToolBarView().getRightTextView().setText("发送");
        getToolBarView().getRightTextView().setOnClickListener(v -> mvpPresenter.sendLocation());
    }

    @Override
    protected void initData() {
        super.initData();
        setRlMapHeight(maxHeight);
        mRvPOI.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && Math.abs(dy) > 10 && ((GridLayoutManager) mRvPOI.getLayoutManager()).findFirstCompletelyVisibleItemPosition() <= 1 && mRlMap.getHeight() == maxHeight) {
                    LogUtils.sf("上拉缩小");
                    setRlMapHeight(minHeight);
                    UIUtils.postTaskDelay(() -> mRvPOI.moveToPosition(0), 0);
                } else if (dy < 0 && Math.abs(dy) > 10 && ((GridLayoutManager) mRvPOI.getLayoutManager()).findFirstCompletelyVisibleItemPosition() == 1 && mRlMap.getHeight() == minHeight) {
                    LogUtils.sf("下拉放大");
                    setRlMapHeight(maxHeight);
                    UIUtils.postTaskDelay(() -> mRvPOI.moveToPosition(0), 0);
                }
            }
        });
       // mIbShowLocation.setOnClickListener(v -> requestLocationUpdate());
    }


    private void setRlMapHeight(int height) {
        AutoLinearLayout.LayoutParams params = (AutoLinearLayout.LayoutParams) mRlMap.getLayoutParams();
        params.height = height;
        mRlMap.setLayoutParams(params);
    }


    @Override
    protected int getLayoutId() {
        return R.layout.ac_address_list;
    }

    @Override
    protected void afterCreate(Bundle bundle) {
        initPermissions();
        initLocation();
    }


    private void initPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /***
             * 定位权限为必须权限，用户如果禁止，则每次进入都会申请
             */
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_SETTINGS,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_SMS
            }, new PermissionListener() {
                @Override
                public void onGranted() {

                }

                @Override
                public void onDenied(List<String> deniedPermissions) {
                    //T_.showToastReal("未启用权限无法使用此功能");
                }
            });

        }


    }




    /**
     * 当前地点击点
     */
    private LatLng currentPt;

    private String touchType;

    BitmapDescriptor bdA = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_marka);

    /**
     * 对地图事件的消息响应
     */
    private void initListener() {
        mBaiduMap.setOnMapTouchListener(new BaiduMap.OnMapTouchListener() {

            @Override
            public void onTouch(MotionEvent event) {

            }
        });


        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            /**
             * 单击地图
             */
            public void onMapClick(LatLng point) {
                touchType = "单击地图";
                currentPt = point;
                updateMapState();
            }

            /**
             * 单击地图中的POI点
             */
            public boolean onMapPoiClick(MapPoi poi) {
                touchType = "单击POI点";
                currentPt = poi.getPosition();
                updateMapState();
                return false;
            }
        });
        mBaiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            /**
             * 长按地图
             */
            public void onMapLongClick(LatLng point) {
                touchType = "长按";
                currentPt = point;
                updateMapState();
            }
        });
        mBaiduMap.setOnMapDoubleClickListener(new BaiduMap.OnMapDoubleClickListener() {
            /**
             * 双击地图
             */
            public void onMapDoubleClick(LatLng point) {
                touchType = "双击";
                currentPt = point;
                updateMapState();
            }
        });

        /**
         * 地图状态发生变化
         */
        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            public void onMapStatusChangeStart(MapStatus status) {
                updateMapState();
            }

            @Override
            public void onMapStatusChangeStart(MapStatus status, int reason) {

            }

            public void onMapStatusChangeFinish(MapStatus status) {
                updateMapState();
            }

            public void onMapStatusChange(MapStatus status) {
                updateMapState();
            }
        });
    }


    /**
     * 更新地图状态显示面板
     */
    private void updateMapState() {
        String state = "";
        if (currentPt == null) {
            state = "点击、长按、双击地图以获取经纬度和地图状态";
        } else {
            state = String.format(touchType + ",当前经度： %f 当前纬度：%f",
                    currentPt.longitude, currentPt.latitude);
            MarkerOptions ooA = new MarkerOptions().position(currentPt).icon(bdA);
            mBaiduMap.clear();
            mBaiduMap.addOverlay(ooA);
        }
        state += "\n";
        MapStatus ms = mBaiduMap.getMapStatus();
        state += String.format(
                "zoom=%.1f rotate=%d overlook=%d",
                ms.zoom, (int) ms.rotate, (int) ms.overlook);



        /*不搜索*/
//        if (currentPt!= null){
//            LatLng center =  new LatLng(currentPt.latitude,currentPt.longitude);
//            LatLngBounds searchbound = new LatLngBounds.Builder().include(center).build();
//            mPoiSearch.searchInBound(new PoiBoundSearchOption().bound(searchbound)
//                    .keyword("写字楼").pageNum(loadIndex));
//            L_.e("搜索附近"+currentPt.longitude+"-------->"+currentPt.latitude);
//        }


    }



    private void initLocation() {
        // -----------location config ------------
        locationService = ((MyApp) getApplication()).locationService;
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService.registerListener(mListener);
        //注册监听
        locationService.setLocationOption(locationService.getDefaultLocationClientOption());

        // 初始化搜索模块，注册搜索事件监听
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(this);

        locationService.start();// 定位SDK
    }


    /*****
     *
     * 定位结果回调，重写onReceiveLocation方法，可以直接拷贝如下代码到自己工程中修改
     *
     */
    private BDLocationListener mListener = new BDLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (null != location && location.getLocType() != BDLocation.TypeServerError) {
                StringBuffer sb = new StringBuffer(256);
                sb.append("time : ");
                /**
                 * 时间也可以使用systemClock.elapsedRealtime()方法 获取的是自从开机以来，每次回调的时间；
                 * location.getTime() 是指服务端出本次结果的时间，如果位置不发生变化，则时间不变
                 */
                sb.append(location.getTime());
                sb.append("\nlocType : ");// 定位类型
                sb.append(location.getLocType());
                sb.append("\nlocType description : ");// *****对应的定位类型说明*****
                sb.append(location.getLocTypeDescription());
                sb.append("\nlatitude : ");// 纬度
                sb.append(location.getLatitude());
                sb.append("\nlontitude : ");// 经度
                sb.append(location.getLongitude());
                sb.append("\nradius : ");// 半径
                sb.append(location.getRadius());
                sb.append("\nCountryCode : ");// 国家码
                sb.append(location.getCountryCode());
                sb.append("\nCountry : ");// 国家名称
                sb.append(location.getCountry());
                sb.append("\ncitycode : ");// 城市编码
                sb.append(location.getCityCode());
                sb.append("\ncity : ");// 城市
                sb.append(location.getCity());
                sb.append("\nDistrict : ");// 区
                sb.append(location.getDistrict());
                sb.append("\nStreet : ");// 街道
                sb.append(location.getStreet());
                sb.append("\naddr : ");// 地址信息
                sb.append(location.getAddrStr());
                sb.append("\nUserIndoorState: ");// *****返回用户室内外判断结果*****
                sb.append(location.getUserIndoorState());
                sb.append("\nDirection(not all devices have value): ");
                sb.append(location.getDirection());// 方向
                sb.append("\nlocationdescribe: ");
                sb.append(location.getLocationDescribe());// 位置语义化信息
                sb.append("\nPoi: ");// POI信息
                if (location.getPoiList() != null && !location.getPoiList().isEmpty()) {
                    for (int i = 0; i < location.getPoiList().size(); i++) {
                        Poi poi = location.getPoiList().get(i);
                        sb.append(poi.getName() + ";");
                    }
                }
                if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                    sb.append("\nspeed : ");
                    sb.append(location.getSpeed());// 速度 单位：km/h
                    sb.append("\nsatellite : ");
                    sb.append(location.getSatelliteNumber());// 卫星数目
                    sb.append("\nheight : ");
                    sb.append(location.getAltitude());// 海拔高度 单位：米
                    sb.append("\ngps status : ");
                    sb.append(location.getGpsAccuracyStatus());// *****gps质量判断*****
                    sb.append("\ndescribe : ");
                    sb.append("gps定位成功");
                } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                    // 运营商信息
                    if (location.hasAltitude()) {// *****如果有海拔高度*****
                        sb.append("\nheight : ");
                        sb.append(location.getAltitude());// 单位：米
                    }
                    sb.append("\noperationers : ");// 运营商信息
                    sb.append(location.getOperators());
                    sb.append("\ndescribe : ");
                    sb.append("网络定位成功");
                } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                    sb.append("\ndescribe : ");
                    sb.append("离线定位成功，离线定位结果也是有效的");
                } else if (location.getLocType() == BDLocation.TypeServerError) {
                    sb.append("\ndescribe : ");
                    sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                    sb.append("\ndescribe : ");
                    sb.append("网络不同导致定位失败，请检查网络是否通畅");
                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                    sb.append("\ndescribe : ");
                    sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
                }
                L_.e(location.getCity());
                ConfigApplication.instance().setmLatitude(location.getLatitude());
                ConfigApplication.instance().setmLongitude(location.getLongitude());
                initMylocation();
                mvpPresenter.searchNeayBy();
                locationService.stop(); //停止定位服务
                L_.e(sb.toString());
            }
        }

    };



    public void initMylocation(){
        // 反Geo搜索
        // 初始化搜索模块，注册事件监听
        // 地图初始化
        LatLng ptCenter = new LatLng(ConfigApplication.instance().getmLatitude(),
                ConfigApplication.instance().getmLongitude());
        mBaiduMap = map.getMap();
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(this);
        mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(ptCenter));

        perfomZoom();
        //initListener();
    }



    /**
     * 处理缩放 sdk 缩放级别范围： [3.0,19.0]
     */
    private void perfomZoom() {
        try {
            MapStatusUpdate u = MapStatusUpdateFactory.zoomTo(16);
            mBaiduMap.animateMapStatus(u);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入正确的缩放级别", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onGetPoiResult(PoiResult poiResult) {
        if (poiResult == null || poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
            T_.showToastReal("未搜索到结果");
            mPb.setVisibility(View.GONE);
            mRvPOI.setVisibility(View.VISIBLE);
            return;
        }

        if (poiResult.getAllPoi()!=null&&poiResult.getAllPoi().size()>0){
            L_.e(poiResult.getAllPoi().size()+"-------->");

        }else {
            return;
        }


        for (int i=0;i<poiResult.getAllPoi().size();i++){
            L_.e(poiResult.getAllPoi().get(i).address);
        }

        mPb.setVisibility(View.GONE);
        mRvPOI.setVisibility(View.VISIBLE);
        mvpPresenter.loadData(poiResult.getAllPoi());
        //L_.e(poiResult.getAllAddr().size()+"");
        //L_.e(poiResult.getSuggestCityList().size()+"");
        //L_.e(poiResult.getAllPoi().size()+"");
    }

    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

    }



    @Override
    protected void onResume() {
        map.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {

        locationService.unregisterListener(mListener); //注销掉监听
        locationService.stop(); //停止定位服务

        if (mPoiSearch!=null){
            mPoiSearch.destroy();
        }
        if (map!=null){
            map.onDestroy();
        }
        if (mSearch!=null){
            mSearch.destroy();
        }

        super.onDestroy();
    }

    @Override
    public LQRRecyclerView getRvPOI() {
        return mRvPOI;
    }

    @Override
    public BaiduMap getBaiDuMap() {
        return mBaiduMap;
    }

    @Override
    public PoiSearch getSearchPoi() {
        return mPoiSearch;
    }

    @Override
    public MapView getMapView() {
        return null;
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            return;
        }
        mBaiduMap.clear();
        mBaiduMap.addOverlay(new MarkerOptions().position(result.getLocation())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)));
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(result
                .getLocation()));
        String strInfo = String.format("纬度：%f 经度：%f",
                result.getLocation().latitude, result.getLocation().longitude);
        //Toast.makeText(GeoCoderActivity.this, strInfo, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
//            Toast.makeText(GeoCoderActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG)
//                    .show();
            return;
        }
        mBaiduMap.clear();
        mBaiduMap.addOverlay(new MarkerOptions().position(result.getLocation())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)));
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(result
                .getLocation()));


    }
}

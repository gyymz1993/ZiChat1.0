package com.lsjr.zizi.mvp.home.zichat;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.lsjr.zizi.MyApp;
import com.lsjr.zizi.base.SwipeBackActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.zichat.presenter.AddressList;
import com.lsjr.zizi.three.LocationService;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/29 9:51
 */

public abstract class BaiduMapBaseActivity extends SwipeBackActivity implements
        OnGetPoiSearchResultListener, OnGetSuggestionResultListener,AddressList.IView,OnGetGeoCoderResultListener {

    GeoCoder mSearch = null; // 搜索模块，也可去掉地图模块独立使用
    BaiduMap mBaiduMap = null;
    private LocationService locationService;
    /*搜索周边的位置*/
    private PoiSearch mPoiSearch = null;


    @Override
    protected void initView() {
        super.initView();
        initLocation();
        initSeachNearByService();
    }

    private void initLocation() {
        // -----------location config ------------
        locationService = ((MyApp) getApplication()).locationService;
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService.registerListener(mListener);
        //注册监听
        locationService.setLocationOption(locationService.getDefaultLocationClientOption());
        locationService.start();// 定位SDK

    }


    private void initSeachNearByService(){
        // 初始化搜索模块，注册搜索事件监听
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(this);
    }


    public void setMarkForMyAdress(){
        // 反Geo搜索
        LatLng ptCenter = new LatLng(ConfigApplication.instance().getmLatitude(),
                ConfigApplication.instance().getmLongitude());
        mBaiduMap = getMapView().getMap();
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(this);
        mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(ptCenter));
        perfomZoom();
    }


    /**
     * 处理缩放 sdk 缩放级别范围： [3.0,19.0]
     */
    private void perfomZoom() {
        MapStatusUpdate u = MapStatusUpdateFactory.zoomTo(16);
        mBaiduMap.animateMapStatus(u);
    }



    /**
     * 搜索周边地理位置
     */
    private int loadIndex = 0;
    public void searchNeayBy() {
        LatLng center =  new LatLng(ConfigApplication.instance().getmLatitude(), ConfigApplication.instance().getmLongitude());
        /**
         * 搜索位置点周边POI
         */
        PoiNearbySearchOption nearbySearchOption = new PoiNearbySearchOption(); //POI附近检索参数设置类
        nearbySearchOption.keyword("写字楼");//搜索关键字，比如：银行、网吧、餐厅等
        nearbySearchOption.location(center);//搜索的位置点
        nearbySearchOption.radius(100);//搜索覆盖半径
        nearbySearchOption.sortType(PoiSortType.distance_from_near_to_far);//搜索类型，从近至远
        nearbySearchOption.pageNum(loadIndex);//查询第几页：POI量可能会很多，会有分页查询;
        nearbySearchOption.pageCapacity(10);//设置每页查询的个数，默认10个
        mPoiSearch.searchNearby(nearbySearchOption);//查询

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
                ConfigApplication.instance().setmLatitude(location.getLatitude());
                ConfigApplication.instance().setmLongitude(location.getLongitude());
                locationService.stop(); //停止定位服务
                setMarkForMyAdress();
                searchNeayBy();
            }
        }

    };

}

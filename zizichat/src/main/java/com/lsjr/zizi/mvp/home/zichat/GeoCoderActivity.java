package com.lsjr.zizi.mvp.home.zichat;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
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
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.SwipeBackActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.ymz.baselibrary.mvp.BasePresenter;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * 此demo用来展示如何进行地理编码搜索（用地址检索坐标）、反地理编码搜索（用坐标检索地址）
 */
public class GeoCoderActivity extends SwipeBackActivity implements OnGetGeoCoderResultListener {
    GeoCoder mSearch = null; // 搜索模块，也可去掉地图模块独立使用
    BaiduMap mBaiduMap = null;
    @BindView(R.id.bmapView)
    MapView mMapView;
    @BindView(R.id.id_tv_adress)
    TextView idTvAdress;

    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("当前位置");
    }


    @Override
    protected void initView() {


    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_geocoder;
    }

    @Override
    protected void afterCreate(Bundle bundle) {

        String latitude = getIntent().getStringExtra(Constants.EXTRA_LATITUDE);
        String longitude = getIntent().getStringExtra(Constants.EXTRA_LONGITUDE);

        if (latitude == null || longitude == null) {
            latitude = String.valueOf(ConfigApplication.instance().getmLatitude());
            longitude = String.valueOf(ConfigApplication.instance().getmLongitude());
        }

        LatLng ptCenter = new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));
        // 反Geo搜索
        // 初始化搜索模块，注册事件监听
        // 地图初始化
        mBaiduMap = mMapView.getMap();
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


    @Override
    protected BasePresenter createPresenter() {
        return null;
    }


    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        mSearch.destroy();
        super.onDestroy();
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(GeoCoderActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        mBaiduMap.clear();
        mBaiduMap.addOverlay(new MarkerOptions().position(result.getLocation())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)));
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(result
                .getLocation()));
        String strInfo = String.format("纬度：%f 经度：%f", result.getLocation().latitude, result.getLocation().longitude);

        //Toast.makeText(GeoCoderActivity.this, strInfo, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(GeoCoderActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        mBaiduMap.clear();
        mBaiduMap.addOverlay(new MarkerOptions().position(result.getLocation())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)));
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(result
                .getLocation()));
        idTvAdress.setText(result.getAddress());
       // Toast.makeText(GeoCoderActivity.this, result.getAddress(), Toast.LENGTH_LONG).show();

    }
}

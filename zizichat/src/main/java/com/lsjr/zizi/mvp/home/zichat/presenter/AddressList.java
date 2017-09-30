package com.lsjr.zizi.mvp.home.zichat.presenter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.andview.myrvview.LQRRecyclerView;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;
import com.lqr.adapter.LQRAdapterForRecyclerView;
import com.lqr.adapter.LQRViewHolderForRecyclerView;
import com.lsjr.zizi.R;
import com.lsjr.zizi.bean.LocationData;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.zichat.AddressListActivity;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/28 17:39
 */

public interface AddressList {

    interface IView{
        LQRRecyclerView getRvPOI();

        BaiduMap getBaiDuMap();

        PoiSearch getSearchPoi();

        MapView getMapView();
    }


    class Presenter extends BasePresenter<IView> {
        private List<PoiInfo> mData ;
        private int mSelectedPosi = 0;
        private LQRAdapterForRecyclerView<PoiInfo> mAdapter;
        private AddressListActivity activity;

        public Presenter(IView mvpView,AddressListActivity appCompatActivity) {
            super(mvpView);
            this.activity=appCompatActivity;
        }

        public void loadData(List<PoiInfo> data ) {
            if (data==null){
                mData=new ArrayList<>();
            }else {
                mData=data;
            }

            setAdapter();
        }

        private void setAdapter() {
            if (mAdapter == null) {
                mAdapter = new LQRAdapterForRecyclerView<PoiInfo>(UIUtils.getContext(), mData, R.layout.item_location_poi) {
                    @Override
                    public void convert(LQRViewHolderForRecyclerView helper, PoiInfo item, int position) {
                        helper.setText(R.id.tvTitle, item.address).setText(R.id.tvDesc, item.address)
                                .setViewVisibility(R.id.ivSelected, mSelectedPosi == position ? View.VISIBLE : View.GONE);
                    }
                };
                getView().getRvPOI().setAdapter(mAdapter);
                mAdapter.setOnItemClickListener((helper, parent, itemView, position) -> {
                    mSelectedPosi = position;
                    //setAdapter();
                    mAdapter.notifyDataSetChangedWrapper();
                });
            } else {
                mAdapter.setData(mData);
            }
        }

        public void sendLocation() {
            if (mData != null && mData.size() > mSelectedPosi) {
                PoiInfo poiInfo = mData.get(mSelectedPosi);
                Intent data = new Intent();
                LocationData locationData = new LocationData(poiInfo.location.latitude, poiInfo.location.longitude,
                        poiInfo.address, getMapUrl(poiInfo.location.latitude, poiInfo.location.longitude));
                data.putExtra("location", locationData);
                activity.setResult(Activity.RESULT_OK, data);
                activity.finish();
            }
        }


        /**
         * 搜索周边地理位置
         */
        private int loadIndex = 0;
        public void searchNeayBy() {
            LatLng center =  new LatLng(ConfigApplication.instance().getmLatitude(), ConfigApplication.instance().getmLongitude());
//        LatLngBounds searchbound = new LatLngBounds.Builder().include(center).build();
//        mPoiSearch.searchInBound(new PoiBoundSearchOption().bound(searchbound)
//                .keyword("写字楼").pageNum(loadIndex));
//        L_.e("搜索附近");

            /**
             * 搜索位置点周边POI
             */
            PoiNearbySearchOption nearbySearchOption = new PoiNearbySearchOption(); //POI附近检索参数设置类
            nearbySearchOption.keyword("写字楼");//搜索关键字，比如：银行、网吧、餐厅等
            nearbySearchOption.location(center);//搜索的位置点
            nearbySearchOption.radius(300);//搜索覆盖半径
            nearbySearchOption.sortType(PoiSortType.distance_from_near_to_far);//搜索类型，从近至远
            nearbySearchOption.pageNum(loadIndex);//查询第几页：POI量可能会很多，会有分页查询;
            nearbySearchOption.pageCapacity(10);//设置每页查询的个数，默认10个
            mvpView.getSearchPoi().searchNearby(nearbySearchOption);//查询

        }



        //    获取位置静态图
        //    http://apis.map.qq.com/ws/staticmap/v2/?center=39.8802147,116.415794&zoom=10&size=600*300&maptype=landform&markers=size:large|color:0xFFCCFF|label:k|39.8802147,116.415794&key=OB4BZ-D4W3U-B7VVO-4PJWW-6TKDJ-WPB77
        //    http://st.map.qq.com/api?size=708*270&center=114.215843,22.685120&zoom=17&referer=weixin
        //    http://st.map.qq.com/api?size=708*270&center=116.415794,39.8802147&zoom=17&referer=weixin
        private String getMapUrl(double x, double y) {
            String url = "http://st.map.qq.com/api?size=708*270&center=" + y + "," + x + "&zoom=17&referer=weixin";
            return url;
        }
    }
}

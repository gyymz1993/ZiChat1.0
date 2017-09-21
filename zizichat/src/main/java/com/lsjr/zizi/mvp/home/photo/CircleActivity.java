package com.lsjr.zizi.mvp.home.photo;

import android.os.Bundle;

import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.Constants;
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.Marker;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.QSupportMapFragment;
import com.tencent.tencentmap.mapsdk.map.TencentMap;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;


public class CircleActivity extends MvpActivity {

	private TencentMap tencentMap;

	@Override
	protected int getLayoutId() {
		return R.layout.activity_circle1;
	}

	@Override
	protected void afterCreate(Bundle savedInstanceState) {

		String latitude=getIntent().getStringExtra(Constants.EXTRA_LATITUDE);
		String longitude=getIntent().getStringExtra(Constants.EXTRA_LONGITUDE);

		L_.e("latitude:"+latitude+"longitude"+longitude);
		QSupportMapFragment mapFragment = (QSupportMapFragment)
				getSupportFragmentManager().findFragmentById(R.id.frag_map);
		tencentMap = mapFragment.getMapView().getMap();

		if (latitude==null||longitude==null){
			longitude="100";
			latitude="100";
		}
		LatLng	mLatLng = new LatLng(Double.valueOf(longitude), Double.valueOf(latitude));
		/**
		 * 通过添加OverlayItem添加标注物
		 */
		Marker mMarker = tencentMap.addMarker(new MarkerOptions()
				.icon(BitmapDescriptorFactory.defaultMarker())
				.position(mLatLng)
				.draggable(true));
	}

	@Override
	protected BasePresenter createPresenter() {
		return null;
	}




}

package com.sk.weichat.ui.circle;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class SendBaiDuLocate extends FragmentActivity{
	
	private double latitude;
	private double longitude;
	private String userName;
	private TextView textView;

	private MapView mMapView;
	InfoWindow mInfoWindow = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_baidu_map_locate);
		
		latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
		longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
		userName = MyApplication.getInstance().mLoginUser.getNickName();
		
		textView = (TextView) findViewById(R.id.sendbd_send);
		mMapView = (MapView) findViewById(R.id.bmapView);
		

		// 定义Maker坐标点
		LatLng point = new LatLng(latitude, longitude);
		// 构建Marker图标
		BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);
		// 构建MarkerOption，用于在地图上添加Marker
		OverlayOptions option = new MarkerOptions().position(point).icon(bitmap).title(userName);
		// 在地图上添加Marker，并显示
		mMapView.getMap().addOverlay(option);
		
		MapStatus mapStatus = new MapStatus.Builder().zoom(mMapView.getMap().getMaxZoomLevel() - 3).target(point).build();
		MapStatusUpdate u = MapStatusUpdateFactory.newMapStatus(mapStatus);
		mMapView.getMap().animateMapStatus(u);//设置为中心显示

		mMapView.getMap().setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(Marker arg0) {
				String name = arg0.getTitle();
				if (mInfoWindow == null) {
					// 创建InfoWindow展示的view
					Button button = new Button(SendBaiDuLocate.this);
					button.setBackgroundResource(R.drawable.popup);
					button.setText(name);
					// 创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
					mInfoWindow = new InfoWindow(button, arg0.getPosition(), -47);// 显示InfoWindow
					mMapView.getMap().showInfoWindow(mInfoWindow);
				} else {
					mInfoWindow = null;
					mMapView.getMap().hideInfoWindow();
				}
				return false;
			}
		});
		
		textView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.putExtra(AppConstant.EXTRA_LATITUDE, latitude);
				intent.putExtra(AppConstant.EXTRA_LONGITUDE, longitude);
				setResult(RESULT_OK, intent);
				finish();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mMapView.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mMapView.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mMapView.onPause();
	}
	
	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add("发送");
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getTitle().toString().equals("发送")) {
			
		}
		return super.onOptionsItemSelected(item);
	}*/

}

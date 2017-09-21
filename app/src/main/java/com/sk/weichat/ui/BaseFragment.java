package com.sk.weichat.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BaseFragment extends Fragment {

	public String TAG() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d("roamer", TAG() + " onAttach");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("roamer", TAG() + " onCreate");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d("roamer", TAG() + " onCreateView");
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d("roamer", TAG() + " onActivityCreated");
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d("roamer", TAG() + " onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("roamer", TAG() + " onResume");
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("roamer", TAG() + " onPause");
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d("roamer", TAG() + " onStop");
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.d("roamer", TAG() + " onDestroyView");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("roamer", TAG() + " onDestroy");
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d("roamer", TAG() + " onDetach");
	}
}

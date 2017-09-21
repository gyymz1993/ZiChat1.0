package com.lsjr.zizi.mvp.home.session;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;
import com.lqr.imagepicker.ui.ImageGridActivity;
import com.lsjr.zizi.AppConst;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.session.presenter.ScanContract;
import com.lsjr.zizi.util.LogUtils;
import com.lsjr.zizi.util.PopupWindowUtils;
import com.ymz.baselibrary.thread.ThreadPoolFactory;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.ArrayList;

import butterknife.BindView;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.QRCodeDecoder;
import cn.bingoogolapple.qrcode.zxing.ZXingView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/7 11:38
 */

public class ScanActivity extends MvpActivity<ScanContract.ScanPresenter> implements ScanContract.ScanView, QRCodeView.Delegate  {

    public static final int IMAGE_PICKER = 100;

    @BindView(R.id.zxingview)
    ZXingView mZxingview;

    @BindView(R.id.llSaoma)
    LinearLayout mLlSaoma;
    @BindView(R.id.llFengmian)
    LinearLayout mLlFengmian;
    @BindView(R.id.llJiejing)
    LinearLayout mLlJiejing;
    @BindView(R.id.llFanyi)
    LinearLayout mLlFanyi;

    @BindView(R.id.ivSaomaNormal)
    ImageView mIvSaomaNormal;
    @BindView(R.id.ivSaomaPress)
    ImageView mIvSaomaPress;
    @BindView(R.id.ivFengmianNormal)
    ImageView mIvFengmianNormal;
    @BindView(R.id.ivFengmianPress)
    ImageView mIvFengmianPress;
    @BindView(R.id.ivJiejingNormal)
    ImageView mIvJiejingNormal;
    @BindView(R.id.ivJiejingPress)
    ImageView mIvJiejingPress;
    @BindView(R.id.ivFanyiNormal)
    ImageView mIvFanyiNormal;
    @BindView(R.id.ivFanyiPress)
    ImageView mIvFanyiPress;
    private FrameLayout mView;
    private PopupWindow mPopupWindow;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_scan;
    }


    @Override
    protected void initView() {

        setTopLeftButton(R.drawable.ic_back).
                setTitleTextColor(UIUtils.getColor(R.color.white)).
                setBackgroundColor(UIUtils.getColor(R.color.colorPrimary));
        selectBottomOne(0);
       // mIbToolbarMore.setVisibility(View.GONE);

        //mIbToolbarMore.setOnClickListener(v -> showPopupMenu());
        mZxingview.setDelegate(this);
        mLlSaoma.setOnClickListener(v -> selectBottomOne(0));
        mLlFengmian.setOnClickListener(v -> selectBottomOne(1));
        mLlJiejing.setOnClickListener(v -> selectBottomOne(2));
        mLlFanyi.setOnClickListener(v -> selectBottomOne(3));
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {//返回多张照片
            if (data != null) {
                final ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                if (images != null && images.size() > 0) {
                    //取第一张照片
                    ThreadPoolFactory.getNormalPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            String result = QRCodeDecoder.syncDecodeQRCode(images.get(0).path);
                            if (TextUtils.isEmpty(result)) {
                                UIUtils.showToast(UIUtils.getString(R.string.scan_fail));
                            } else {
                                handleResult(result);
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mZxingview.startCamera();
        mZxingview.startSpotAndShowRect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mZxingview.stopCamera();
    }

    @Override
    protected void onDestroy() {
        mZxingview.onDestroy();
        super.onDestroy();

    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

    }

    @Override
    protected ScanContract.ScanPresenter createPresenter() {
        return null;
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        handleResult(result);
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        UIUtils.showToast(UIUtils.getString(R.string.open_camera_error));
    }


    public void selectBottomOne(int switchItem) {
        mIvSaomaPress.setVisibility(View.GONE);
        mIvFengmianPress.setVisibility(View.GONE);
        mIvJiejingPress.setVisibility(View.GONE);
        mIvFanyiPress.setVisibility(View.GONE);
        switch (switchItem) {
            case 0:
                setTitleText(UIUtils.getString(R.string.qr_cord_or_bar_code));
                mIvSaomaPress.setVisibility(View.VISIBLE);
                break;
            case 1:
                setTitleText(UIUtils.getString(R.string.cover_or_movie_poster));
                mIvFengmianPress.setVisibility(View.VISIBLE);
                break;
            case 2:
                setTitleText(UIUtils.getString(R.string.street_scape));
                mIvJiejingPress.setVisibility(View.VISIBLE);
                break;
            case 3:
                setTitleText(UIUtils.getString(R.string.translate));
                mIvFanyiPress.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showPopupMenu() {
        if (mView == null) {
            mView = new FrameLayout(this);
            mView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mView.setBackgroundColor(UIUtils.getColor(R.color.white));

            TextView tv = new TextView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, UIUtils.dip2px(45));
            tv.setLayoutParams(params);
            tv.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            tv.setPadding(UIUtils.dip2px(20), 0, 0, 0);
            tv.setTextColor(UIUtils.getColor(R.color.gray0));
            tv.setTextSize(14);
            tv.setText(UIUtils.getString(R.string.select_qr_code_from_ablum));
            mView.addView(tv);

            tv.setOnClickListener(v -> {
                mPopupWindow.dismiss();
                Intent intent = new Intent(ScanActivity.this, ImageGridActivity.class);
                startActivityForResult(intent, IMAGE_PICKER);
            });
        }
        mPopupWindow = PopupWindowUtils.getPopupWindowAtLocation(mView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, getWindow().getDecorView().getRootView(), Gravity.BOTTOM, 0, 0);
        mPopupWindow.setOnDismissListener(() -> PopupWindowUtils.makeWindowLight(ScanActivity.this));
        PopupWindowUtils.makeWindowDark(this);
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    private void handleResult(String result) {
        LogUtils.sf("扫描结果:" + result);
        vibrate();
        mZxingview.startSpot();

    }


    private void loadError(Throwable throwable) {
        LogUtils.sf(throwable.getLocalizedMessage());
        UIUtils.showToast(throwable.getLocalizedMessage());
    }

}

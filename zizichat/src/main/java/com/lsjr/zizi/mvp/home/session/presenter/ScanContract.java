package com.lsjr.zizi.mvp.home.session.presenter;

import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.ymz.baselibrary.mvp.BasePresenter;

public interface ScanContract {

    interface ScanView{

    }

    class ScanPresenter extends BasePresenter<ScanView>  {
        String mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        String mLoginNickName = ConfigApplication.instance().mLoginUser.getNickName();


        public ScanPresenter(ScanView mvpView) {
            super(mvpView);
        }
    }
}

package com.lsjr.zizi.mvp.home.zichat.presenter;

import android.support.v7.widget.RecyclerView;

import com.lsjr.zizi.base.BaseContract;
import com.ymz.baselibrary.mvp.BasePresenter;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/10/12 16:55
 */

public interface SeachUserList {
    interface IView extends BaseContract.View{
        RecyclerView getRvView();
    }

    class GroupListPresenter extends BasePresenter<SeachUserList.IView> {
        public GroupListPresenter(SeachUserList.IView mvpView) {
            super(mvpView);
        }


    }
}

package com.lsjr.zizi.base;

import android.support.annotation.StringRes;

import com.ymz.baselibrary.mvp.BasePresenter;


public interface BaseContract {
    // 基本的界面职责
    interface View {
        // 公共的：显示一个字符串错误
        void showError(@StringRes int str);

        // 公共的：显示进度条
        void showLoading();


    }

    // 基本的Presenter职责
    abstract class Presenter<P extends View> extends BasePresenter<View> {
        public Presenter(View mvpView) {
            super(mvpView);
        }

        // 共用的开始触发
       public abstract void start();

        // 共用的销毁触发
        public abstract void destroy();
    }

    // 基本的一个列表的View的职责
    interface RecyclerView<T extends Presenter, ViewMode> extends View {
        // 界面端只能刷新整个数据集合，不能精确到每一条数据更新
        // void onDone(List<User> users);

        // 拿到一个适配器，然后自己自主的进行刷新
        //RecyclerAdapter<ViewMode> getRecyclerAdapter();

        // 当适配器数据更改了的时候触发
        void onAdapterDataChanged();
    }
}
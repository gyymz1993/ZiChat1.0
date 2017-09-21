package com.lsjr.zizi.base;

import com.ymz.baselibrary.mvp.BaseMvpFragment;
import com.ymz.baselibrary.mvp.BasePresenter;

/**
 * @author: gyymz1993
 * 创建时间：2017/5/3 21:56
 *
 *   最基本的MVP 模式基类
 **/
public abstract class MvpFragment<P extends BasePresenter> extends BaseMvpFragment<P> {

    @Override
    protected void initTitle() {
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initData() {

    }

}

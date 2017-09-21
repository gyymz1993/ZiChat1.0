package com.lsjr.zizi.base;

import android.os.Build;

import com.barlibrary.ImmersionBar;
import com.githang.statusbar.OSUtils;
import com.githang.statusbar.StatusBarCompat;
import com.lsjr.zizi.R;
import com.ymz.baselibrary.mvp.BaseMvpActivity;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.SystemBarHelper;
import com.ymz.baselibrary.utils.UIUtils;

/**
  * @author: gyymz1993
  * 创建时间：2017/5/3 21:53
  * @version  最基本的MVP 模式基类
  *
 **/
public abstract class MvpActivity<P extends BasePresenter> extends BaseMvpActivity<P> {

    protected ImmersionBar mImmersionBar;
    @Override
    protected void initTitle() {
        if (isSupportStatusBarDarkFont()){
            StatusBarCompat.setStatusBarColor(this, UIUtils.getColor(R.color.status_color));
        }else {
            SystemBarHelper.tintStatusBar(this, UIUtils.getColor(R.color.status_color));
        }
        //初始化沉浸式
        if (isImmersionBarEnabled()){
            initImmersionBar();
        }

        setTopLeftButton(R.drawable.ic_back).
                setTitleTextColor(UIUtils.getColor(R.color.top_text_color)).
                setBackgroundColor(UIUtils.getColor(R.color.top_theme_color));
    }


    protected void initImmersionBar() {
        //在BaseActivity里初始化
        mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.init();
    }

    /**
     * 是否可以使用沉浸式
     * Is immersion bar enabled boolean.
     *
     * @return the boolean
     */
    protected boolean isImmersionBarEnabled() {
        return false;
    }

    /**
     * 判断手机支不支持状态栏字体变色
     * Is support status bar dark font boolean.
     *
     * @return the boolean
     */
    public static boolean isSupportStatusBarDarkFont() {
        if (OSUtils.isMIUI6Later() || OSUtils.isFlymeOS4Later()
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            return true;
        } else
            return false;
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initData() {

    }

    @Override
    protected void onDestroy() {
        ImmersionBar.with(this).destroy();
        super.onDestroy();
    }

    @Override
    public int getFragmentContentId() {
        return 0;
    }

}

package com.lsjr.zizi.mvp.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.lsjr.zizi.mvp.contrl.FragmentController;


/**
 * 创建人：gyymz1993
 * 创建时间：2017/5/29/3:32
 **/
public class HomeAadapter  extends FragmentPagerAdapter{

    private int [] mSelectors;
    private String[] titles=new String[]{"消息","通讯录","发现","话题"};
    public HomeAadapter(FragmentManager fm,int [] selectors) {
        super(fm);
        this.mSelectors=selectors;
    }


    @Override public CharSequence getPageTitle(int position) {
        return titles[position % 4];
    }


    @Override public int getCount() {
        return mSelectors.length;
    }


    @Override public Fragment getItem(int position) {
        return FragmentController.getInstance().getFragment(position);
    }


}

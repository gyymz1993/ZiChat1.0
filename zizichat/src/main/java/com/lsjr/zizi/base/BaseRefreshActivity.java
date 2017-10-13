package com.lsjr.zizi.base;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.refreshview.XRefreshView;
import com.andview.refreshview.XRefreshViewFooter;

import com.lsjr.zizi.R;
import com.lsjr.zizi.view.CustomGifHeader;
import com.ymz.baselibrary.mvp.BasePresenter;

import butterknife.BindView;

import static com.ymz.baselibrary.utils.UIUtils.getContext;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/7/21 15:14
 *
 *  所有  刷新Recycleview刷新的Base
 *
 */

public abstract class BaseRefreshActivity<P extends BasePresenter> extends SwipeBackActivity<P> {

    @BindView(R.id.recyclerview)
    public RecyclerView recyclerView;
    @BindView(R.id.xrefreshview)
    public XRefreshView xRefreshView;
    public int pager = 1;
    public static final int ON_REFRESH = 1;
    public static final int ON_LOAD = 2;
    public int pullStatus;

    @Override
    protected int getLayoutId() {
        return R.layout.ab_ac_refresh;
    }

    @Override
    protected void initView() {
        xRefreshView.setAutoRefresh(false);
        xRefreshView.setAutoLoadMore(true);
        xRefreshView.setPinnedTime(1000);
        xRefreshView.stopLoadMore(false);
        xRefreshView.setPullLoadEnable(true);
        xRefreshView.setMoveForHorizontal(true);
        xRefreshView.setMoveForHorizontal(true);
        xRefreshView.setAutoLoadMore(true);
        CustomGifHeader header = new CustomGifHeader(getApplicationContext());
        xRefreshView.setCustomHeaderView(header);

        initRecycleView();

    }

    public void initRecycleView() {
        ABaseRefreshAdapter baseRefreshAdapter = getBaseRefreshAdapter();
        baseRefreshAdapter.setCustomLoadMoreView(new XRefreshViewFooter(getApplicationContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()) {
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        baseRefreshAdapter.setHeaderView(getHeadView(), recyclerView);

        recyclerView.setAdapter(baseRefreshAdapter);
    }


    public abstract ABaseRefreshAdapter getBaseRefreshAdapter();

    public abstract View getHeadView();


}

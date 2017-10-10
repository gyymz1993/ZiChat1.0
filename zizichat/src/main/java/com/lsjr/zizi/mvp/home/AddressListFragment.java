package com.lsjr.zizi.mvp.home;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpFragment;
import com.lsjr.zizi.mvp.home.zichat.presenter.AddressListFrag;
import com.lsjr.zizi.view.CharIndexView;
import com.ymz.baselibrary.utils.UIUtils;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/10/9 10:06
 */

public class AddressListFragment extends MvpFragment<AddressListFrag.Presenter> implements AddressListFrag.IView {
    @BindView(R.id.rv_main)
    RecyclerView rvMain;
    @BindView(R.id.iv_main)
    CharIndexView ivMain;
    @BindView(R.id.tv_index)
    TextView tvIndex;

    @Override
    protected AddressListFrag.Presenter createPresenter() {
        AddressListFrag.Presenter presenter = new AddressListFrag.Presenter(this);
        presenter.loadData(getContext());
        return presenter;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.frag_address_list;
    }

    @Override
    protected void afterCreate(Bundle bundle) {

        ivMain.setOnCharIndexChangedListener(new CharIndexView.OnCharIndexChangedListener() {
            @Override
            public void onCharIndexChanged(char currentIndex) {
                for (int i=0; i<mvpPresenter.getContactList().size(); i++) {
                    if (mvpPresenter.getContactList().get(i).getFirstChar() == currentIndex) {
                        mvpPresenter.getManager().scrollToPositionWithOffset(i, 0);
                        return;
                    }
                }
            }

            @Override
            public void onCharIndexSelected(String currentIndex) {
                if (currentIndex == null) {
                    tvIndex.setVisibility(View.INVISIBLE);
                } else {
                    tvIndex.setVisibility(View.VISIBLE);
                    tvIndex.setText(currentIndex);
                }
            }
        });
    }

    @Override
    public View getHeadView(){
        return UIUtils.inflate(R.layout.header_rv_contacts);
    }

    @Override
    public void showError(int str) {

    }

    @Override
    public void showLoading() {

    }



    @Override
    public RecyclerView getRvView() {
        return rvMain;
    }

}

package com.lsjr.zizi.mvp.home.session;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.andview.listener.OnItemClickListener;
import com.andview.refreshview.XRefreshView;
import com.andview.refreshview.XRefreshViewFooter;
import com.lsjr.bean.ArrayResult;
import com.lsjr.callback.ChatArrayCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.db.User;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.zichat.presenter.SeachUserList;
import com.lsjr.zizi.view.CustomGifHeader;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.zhy.autolayout.AutoLinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;

import static com.ymz.baselibrary.utils.UIUtils.getContext;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/1 16:13
 */

public class SeachFriendActivity extends MvpActivity<SeachUserList.GroupListPresenter>
        implements View.OnClickListener,SeachUserList.IView {
    @BindView(R.id.et_search)
    EditText etSearch;
    @BindView(R.id.iv_clear)
    ImageView ivClear;
    @BindView(R.id.ll_search)
    LinearLayout llSearch;
    @BindView(R.id.ll_none)
    LinearLayout llNone;



    @BindView(R.id.id_fd_list)
    RecyclerView idFdList;

    @BindView(R.id.xrefreshview)
    public XRefreshView xRefreshView;


    protected InputMethodManager imm;
    private String mKeyWord;// 关键字(keyword)
    private int mSex;// 城市Id(cityId)
    private int mMinAge;// 行业Id(industryId)
    private int mMaxAge;// 职能Id(fnId)
    private int mShowTime;// 日期(days)

    FriendAdapter friendAdapter;
    private List<User> datas=new ArrayList<>();
    XRefreshViewFooter xRefreshViewFooter;


    @Override
    protected SeachUserList.GroupListPresenter createPresenter() {
        return new SeachUserList.GroupListPresenter(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_search_friend_;
    }


    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("搜索好友");
        //setTopLeftButton(R.drawable.ic_back);
    }

    @Override
    protected void initView() {
        super.initView();
        xRefreshView.setAutoRefresh(false);
        xRefreshView.setPinnedTime(1000);
        xRefreshView.stopLoadMore(true);
        xRefreshView.setPullLoadEnable(true);
        xRefreshView.setMoveForHorizontal(true);
        xRefreshView.setAutoLoadMore(true);
        idFdList.setHasFixedSize(true);
        CustomGifHeader header = new CustomGifHeader(getApplicationContext());
        xRefreshView.setCustomHeaderView(header);
        xRefreshView.setXRefreshViewListener(new XRefreshView.SimpleXRefreshListener() {
            @Override
            public void onRefresh(boolean isPullDown) {
                pullStatus = ON_REFRESH;
                seachFriend();
            }

            @Override
            public void onLoadMore(boolean isSilence) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pullStatus = ON_LOAD;
                        seachFriend();
                    }
                }, 2000);

            }
        });
        friendAdapter=new FriendAdapter(this,datas,R.layout.item_friend);
        xRefreshViewFooter=new XRefreshViewFooter(getApplicationContext());
        friendAdapter.setCustomLoadMoreView(xRefreshViewFooter);
        idFdList.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        idFdList.setAdapter(friendAdapter);

        friendAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(BaseRecyclerHolder baseRecyclerHolder, int position, Object item) {
                Intent intent = new Intent(SeachFriendActivity.this, BasicInfoActivity.class);
                intent.putExtra(AppConfig.EXTRA_USER_ID, datas.get(position).getUserId());
                startActivity(intent);
            }
        });

        xRefreshView.setVisibility(View.GONE);
        etSearch.setOnClickListener(this);
        ivClear.setOnClickListener(this);
        llSearch.setOnClickListener(this);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent() != null) {
            mKeyWord = getIntent().getStringExtra("key_word");
            mSex = getIntent().getIntExtra("sex", 0);
            mMinAge = getIntent().getIntExtra("min_age", 0);
            mMaxAge = getIntent().getIntExtra("max_age", 0);
            mShowTime = getIntent().getIntExtra("show_time", 0);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        etSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    llSearch.setVisibility(View.VISIBLE);
                    ivClear.setVisibility(View.VISIBLE);
                } else {
                    ivClear.setVisibility(View.GONE);
                    llSearch.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_search:
                //T_.showToastReal("点击搜索");
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                mKeyWord = etSearch.getText().toString();
                if (TextUtils.isEmpty(mKeyWord)){
                    T_.showToastReal("请输入搜索条件");
                    return;
                }
                Pattern pattern = Pattern.compile("[0-9a-zA-Z\u4E00-\u9FA5]+");
                Matcher matcher = pattern.matcher(mKeyWord);
                if (!matcher.matches() ) {
                    T_.showToastReal("有特殊字符");
                    return;
                }

                if (null != mKeyWord && mKeyWord.length() > 0) {
                    //getData();
                    pullStatus = ON_REFRESH;
                    seachFriend();
                } else {
                    T_.showToastReal("搜索条件不能为空");
                    //  Toast.makeText(SearchAddFriendActivity.this, "搜索内容不能为空...", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.iv_clear:
                etSearch.setText("");
                break;
        }
    }


    public final int ON_REFRESH = 1;
    public final int ON_LOAD = 2;
    public int pullStatus=0;
    private  int  mPageIndex = 0;
    public void seachFriend() {
        if (pullStatus==ON_REFRESH){
            mPageIndex=0;
        }else {
            mPageIndex++;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("pageIndex", mPageIndex+"");
        params.put("pageSize", String.valueOf(AppConfig.PAGE_SIZE));
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        if (!TextUtils.isEmpty(mKeyWord)) {
            params.put("nickname", mKeyWord);
        }else {
            T_.showToastReal("请输入搜索条件");
            return ;
        }

        if (mSex != 0) {
            params.put("sex", String.valueOf(mSex));
        }
        if (mMinAge != 0) {
            params.put("minAge", String.valueOf(mMinAge));
        }
        if (mMaxAge != 0) {
            params.put("maxAge", String.valueOf(mMaxAge));
        }
        params.put("active", String.valueOf(mShowTime));
        HttpUtils.getInstance().postServiceData(AppConfig.USER_NEAR, params, new ChatArrayCallBack<User>(User.class) {

            @Override
            protected void onXError(String exception) {
                L_.e("搜索失败" + exception);
                if (idFdList!=null){
                    idFdList.setVisibility(View.GONE);
                }
                if (llNone!=null){
                    llNone.setVisibility(View.VISIBLE);
                }
            }

            @Override
            protected void onSuccess(ArrayResult<User> result) {
                L_.e("搜索成功");
                boolean success = ResultCode.defaultParser(result, true);
                if (success) {
                    List<User> data = result.getData();
                    if (data.size() > 0 ) {
                        L_.e("----------"+data.size());
                        idFdList.setVisibility(View.VISIBLE);
                        xRefreshView.setVisibility(View.VISIBLE);
                        llNone.setVisibility(View.GONE);
                    } else {
                        idFdList.setVisibility(View.GONE);
                        llNone.setVisibility(View.VISIBLE);
                    }
                    endNetRequse(data);
                }
            }
        });

    }


    public void endNetRequse(List<User> data) {
        L_.e("endNetRequse"+"--------pullStatus"+pullStatus+";;;;;;;;;;"+data.size());
        //*加载更多*//*
        if (pullStatus == ON_LOAD) {
            datas.addAll(data);
            friendAdapter.notifyDataSetChanged(datas);
        } else if (pullStatus == ON_REFRESH) {
            datas=data;
            xRefreshView.stopRefresh();
            xRefreshView.setLoadComplete(false);
            friendAdapter.notifyDataSetChanged(datas);
        }
        if (data.size() < 50) {
                /*再无数据*/
            xRefreshView.setLoadComplete(true);
        } else {
            xRefreshView.stopLoadMore(true);
        }
        pullStatus = 0;
    }


    @Override
    public void showError(int str) {

    }

    @Override
    public void showLoading() {

    }

    @Override
    public RecyclerView getRvView() {
        return null;
    }



    public class FriendAdapter extends ABaseRefreshAdapter<User> {
        public FriendAdapter(Context context, List<User> datas, int itemLayoutId) {
            super(context, datas, itemLayoutId);
        }

        @Override
        protected void convert(com.andview.adapter.BaseRecyclerHolder var1, User user, int var3) {
            AutoLinearLayout llNewFriend;
            ImageView  ivNewFriend =  var1.getView(R.id.ivNewFriend);
            TextView tvNewFriendUnread =  var1.getView(R.id.tvNewFriendUnread);
            AvatarHelper.getInstance().displayAvatar(user, ivNewFriend, true);
            tvNewFriendUnread.setText(user.getNickName());
        }
    }

}

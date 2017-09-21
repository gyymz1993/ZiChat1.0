package com.lsjr.zizi.mvp.home.session;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.chat.bean.PhoneInfo;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.mvp.home.ContactsFragment;
import com.lsjr.zizi.mvp.home.decor.IndexStickyViewDecoration;
import com.lsjr.zizi.mvp.home.session.adapter.ContactAdapter;
import com.lsjr.zizi.mvp.home.session.adapter.PhoneContactAdapter;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ymz.baselibrary.view.PermissionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import cn.ittiger.indexlist.IndexStickyView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/2 9:56
 */

public class PhoneContactActivity extends MvpActivity {

    private static final int REQUEST_CODE_CONTACTS = 1;
    private static Pattern IS_PHONE = Pattern.compile("^(1(3|4|5|7|8))\\d{9}$");

    //@BindView(R.id.id_contacts)
   // RecyclerView idContacts;
   // PhoneContactAdapter adapter;

    ContactAdapter contactAdapter;
    private List<Friend> friends;
    private String mLoginUserId;
    @BindView(R.id.id_contacts)
    IndexStickyView idContacts;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mRefreshLayout;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_contacte;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initPermissions();
        super.onCreate(savedInstanceState);
        List<PhoneInfo> phoneNumberFromMobile = getPhoneNumberFromMobile(getApplicationContext());
        for (int i=0;i<phoneNumberFromMobile.size();i++){
            L_.e(phoneNumberFromMobile.get(i).toString());
        }
        contactAdapter=new ContactAdapter(phoneNumberFromMobile);
        //adapter=new PhoneContactAdapter(getApplicationContext(),phoneNumberFromMobile,R.layout.itme_phone_contact);
        idContacts.setAdapter(contactAdapter);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshLayout.setRefreshing(true);
                mRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //loadData();
                       // contactAdapter.notifyDataSetChanged();
                        mRefreshLayout.setRefreshing(false);
                    }
                }, 1000);
            }
        });
        idContacts.addItemDecoration(new IndexStickyViewDecoration(getApplicationContext()));
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        setTitleText("本地联系人");
        setTopLeftButton(R.drawable.ic_back).
                setTitleTextColor(UIUtils.getColor(R.color.white)).
                setBackgroundColor(UIUtils.getColor(R.color.colorPrimary));
    }

    private void initPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS
        }, new PermissionListener() {
            @Override
            public void onGranted() {
                T_.showToastReal("授权");
            }

            @Override
            public void onDenied(List<String> deniedPermissions) {
            }
        });
    }


    public  List<PhoneInfo> getPhoneNumberFromMobile(Context context) {
        List<PhoneInfo> list = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String photo = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
            String number1 = number.replaceAll(" ","");
            L_.e("--------->a", number1.length() + "");
            L_.e("---------->b", number1);
            if (number1.length() == 11 && IS_PHONE.matcher(number1.trim()).matches()) {
                PhoneInfo phoneInfo = new PhoneInfo(name.trim(), number1.trim(), photo);
                list.add(phoneInfo);
            }
        }
        cursor.close();
        return list;
    }

}

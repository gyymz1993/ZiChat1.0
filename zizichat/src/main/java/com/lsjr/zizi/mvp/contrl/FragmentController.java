package com.lsjr.zizi.mvp.contrl;

import android.support.v4.app.Fragment;


import com.lsjr.zizi.mvp.home.ContactsFragment;
import com.lsjr.zizi.mvp.home.MeFragment;
import com.lsjr.zizi.mvp.home.MessageFragment;
import com.lsjr.zizi.mvp.home.WordFragment;

import java.util.ArrayList;


/**
 * 主界面Fragment控制器
 */
public class FragmentController {


    private ArrayList<Fragment> fragments;
    private static FragmentController controller;
    private MessageFragment mAddressListFragment=null;
    //private FindFragment mMessageFragment;
    private WordFragment wordFragment;
    private MeFragment meFragment;
    private ContactsFragment contactsFragment;

    public static FragmentController getInstance() {
        if (controller == null) {
            controller = new FragmentController();
        }
        return controller;
    }

    public static void onDestroy() {
        controller = null;
    }

    private FragmentController() {
        initFragment();
    }

    private void initFragment() {
        if (fragments == null) {
            fragments = new ArrayList<>();
        }
        if (mAddressListFragment == null) {
            mAddressListFragment = new MessageFragment();
        }
        if (wordFragment == null) {
            wordFragment = new WordFragment();
        }
        if (meFragment == null) {
            meFragment = new MeFragment();
        }
        if (contactsFragment == null) {
            contactsFragment = new ContactsFragment();
        }
        fragments.add(mAddressListFragment);
        fragments.add(contactsFragment);
        fragments.add(wordFragment);
        fragments.add(meFragment);
    }


    public Fragment getFragment(int position) {
        return fragments.get(position);
    }

    public void clean(){
        for (int i=0;i<fragments.size();i++){

            fragments.remove(i);
            if (fragments.get(i) instanceof MessageFragment) {
                if (mAddressListFragment == null) {
                    mAddressListFragment = null;
                }
            }
            if (fragments.get(i) instanceof WordFragment) {
                if (wordFragment == null) {
                    wordFragment = null;
                }
            }

            if (fragments.get(i) instanceof MeFragment) {
                if (meFragment == null) {
                    meFragment = null;
                }
            }

            if (fragments.get(i) instanceof ContactsFragment) {
                if (contactsFragment == null) {
                    contactsFragment = null;
                }
            }
            controller=null;
        }
    }
}
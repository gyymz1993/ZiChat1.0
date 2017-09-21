package com.lsjr.zizi.mvp.home.zichat.presenter;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.alibaba.fastjson.JSON;
import com.andview.myrvview.LQRRecyclerView;
import com.lqr.adapter.LQRAdapterForRecyclerView;
import com.lqr.adapter.LQRHeaderAndFooterAdapter;
import com.lqr.adapter.LQRViewHolderForRecyclerView;
import com.lsjr.bean.ObjectResult;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Area;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.zichat.CreatNewGroupActivity;
import com.lsjr.zizi.util.LogUtils;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ymz.baselibrary.widget.NavigationBarView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/15 14:36
 */

public interface CreateNewGroup {

    interface ICreateGroupView{

        NavigationBarView getBtnToolbarSend();

        LQRRecyclerView getRvContacts();

        LQRRecyclerView getRvSelectedContacts();

        EditText getEtKey();

        View getHeaderView();

        void addFriendSuccess(List<Friend> mSelectedData);

        void createRoomSuccess(String userId,String roomJid, String roomName,String roomDesc);
    }

    class CreateGroupPresenter extends BasePresenter<ICreateGroupView> {
        String mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        String mLoginNickName = ConfigApplication.instance().mLoginUser.getNickName();
        private CreatNewGroupActivity activity;
        public CreateGroupPresenter(CreatNewGroupActivity activity , ICreateGroupView mvpView) {
            super(mvpView);
            this.activity=activity;
        }

        public CreateGroupPresenter(ICreateGroupView mvpView) {
            super(mvpView);
        }

        private String mGroupName = "";
        private List<Friend> mData = new ArrayList<>();
        private List<Friend> mSelectedData = new ArrayList<>();
        private LQRHeaderAndFooterAdapter mAdapter;
        private LQRAdapterForRecyclerView<Friend> mSelectedAdapter;

        public CreateGroupPresenter(CreatNewGroupActivity context) {
            super(context);
        }

        public void loadContacts() {
            loadAllFriend();
            setAdapter();
            setSelectedAdapter();
        }


        public void createGroupChat(String roomJid,final String roomName, String roomSubject, final String roomDesc) {
            String nickName = ConfigApplication.instance().mLoginUser.getNickName();
            if (TextUtils.isEmpty(roomJid)) {
                return;
            }
            Map<String, String> params = new HashMap<String, String>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            params.put("jid", roomJid);
            params.put("name", roomName);
            params.put("desc", roomDesc);
            params.put("countryId", String.valueOf(Area.getDefaultCountyId()));// 国家Id
            Area area = Area.getDefaultProvince();
            if (area != null) {
                params.put("provinceId", String.valueOf(area.getId()));// 省份Id
            }
            area = Area.getDefaultCity();
            if (area != null) {
                params.put("cityId", String.valueOf(area.getId()));// 城市Id
                area = Area.getDefaultDistrict(area.getId());
                if (area != null) {
                    params.put("areaId", String.valueOf(area.getId()));// 城市Id
                }
            }
            double latitude = 100;
            double longitude = 100;
            if (latitude != 0)
                params.put("latitude", String.valueOf(latitude));
            if (longitude != 0)
                params.put("longitude", String.valueOf(longitude));

            List<String> inviteUsers = new ArrayList<String>();
            // 邀请好友
            for (int i = 0; i < mSelectedData.size(); i++) {
                if (mSelectedData.get(i) == null) {
                    continue;
                }
                String userId = mSelectedData.get(i).getUserId();
                inviteUsers.add(userId);
            }
            params.put("text", JSON.toJSONString(inviteUsers));
            //showProgressDialogWithText("请等待");
            HttpUtils.getInstance().postServiceData(AppConfig.ROOM_ADD, params, new ChatObjectCallBack<MucRoom>(MucRoom.class) {

                @Override
                protected void onXError(String exception) {
                    //dismissProgressDialog();
                    T_.showToastReal(exception);
                }

                @Override
                protected void onSuccess(ObjectResult<MucRoom> result) {
                   // dismissProgressDialog();
                    boolean parserResult = ResultCode.defaultParser(result, true);
                    if (parserResult && result.getData() != null) {
                        //createRoomSuccess(result.getData().getId(), roomJid, roomName, roomDesc);
                        mvpView.createRoomSuccess(result.getData().getId(), roomJid, roomName, roomDesc);
                    }
                }
            });


        }

        public void addFriendforRoom(String mRoomId) {
            if (mSelectedData.size() < 1) {
                L_.e(mSelectedData.get(0).getNickName());
                return;
            }
            Map<String, String> params = new HashMap<String, String>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            params.put("roomId", mRoomId);
            List<String> inviteUsers = new ArrayList<String>();
            // 邀请好友
            for (int i = 0; i < mSelectedData.size(); i++) {
                if (mSelectedData.get(i) == null) {
                    continue;
                }
                String userId = mSelectedData.get(i).getUserId();
                inviteUsers.add(userId);
            }
            params.put("text", JSON.toJSONString(inviteUsers));
            //showProgressDialogWithText("请等待");
            HttpUtils.getInstance().postServiceData(AppConfig.ROOM_MEMBER_UPDATE, params, new ChatObjectCallBack<Void>(Void.class) {

                @Override
                protected void onXError(String exception) {
                    // dismissProgressDialog();
                    T_.showToastReal(exception);
                }

                @Override
                protected void onSuccess(ObjectResult<Void> result) {
                    boolean parserResult = ResultCode.defaultParser(result, true);
                    if (parserResult) {
                        //inviteFriendSuccess();
                        mvpView.addFriendSuccess(mSelectedData);
                    }
                }
            });

        }

        private void loadAllFriend() {
            //得到所有联系人
            mData = FriendDao.getInstance().getAllContacts(mLoginUserId);
        }

        private void setAdapter() {
            if (mAdapter == null) {
                LQRAdapterForRecyclerView adapter = new LQRAdapterForRecyclerView<Friend>(UIUtils.getContext(), mData, R.layout.item_contact) {
                    @Override
                    public void convert(LQRViewHolderForRecyclerView helper, Friend item, int position) {
                        helper.setText(R.id.tvName, item.getNickName()).setViewVisibility(R.id.cb, View.VISIBLE);
                        ImageView ivHeader = helper.getView(R.id.ivHeader);
                        //Glide.with(UIUtils.getContext()).load(item.get_id()).centerCrop().into(ivHeader);
                        AvatarHelper.getInstance().displayAvatar(item, ivHeader, true);
                        CheckBox cb = helper.getView(R.id.cb);
                        //如果添加群成员的话，需要判断是否已经在群中
                        if (activity.mSelectedTeamMemberAccounts != null &&
                                activity.mSelectedTeamMemberAccounts.contains(item.getUserId())){
                            cb.setChecked(true);
                            helper.setEnabled(R.id.cb, false).setEnabled(R.id.root, false);
                        } else {
                            helper.setEnabled(R.id.cb, true).setEnabled(R.id.root, true);
                            //没有在已有群中的联系人，根据当前的选中结果判断
                            cb.setChecked(mSelectedData.contains(item) ? true : false);
                        }

                        String str = "";
                        //得到当前字母
                        String currentLetter = item.getRemarkName() + "";
                        if (position == mData.size() - 1) {
                            helper.setViewVisibility(R.id.vLine, View.GONE);
                        }

                        //根据str是否为空决定字母栏是否显示
                        if (TextUtils.isEmpty(str)) {
                            helper.setViewVisibility(R.id.tvIndex, View.GONE);
                        } else {
                            helper.setViewVisibility(R.id.tvIndex, View.VISIBLE);
                            helper.setText(R.id.tvIndex, str);
                        }
                    }
                };
                adapter.addHeaderView(getView().getHeaderView());
                mAdapter = adapter.getHeaderAndFooterAdapter();
                getView().getRvContacts().setAdapter(mAdapter);

                ((LQRAdapterForRecyclerView) mAdapter.getInnerAdapter()).setOnItemClickListener((lqrViewHolder, viewGroup, view, i) -> {
                    //选中或反选
                    Friend friend = mData.get(i - 1);
                    if (mSelectedData.contains(friend)) {
                        mSelectedData.remove(friend);
                    } else {
                        mSelectedData.add(friend);
                    }
                    mSelectedAdapter.notifyDataSetChangedWrapper();
                    mAdapter.notifyDataSetChanged();
                    if (mSelectedData.size() > 0) {
                       // getView().getBtnToolbarSend().setEnabled(true);
                        getView().getBtnToolbarSend().getRightTextView().setVisibility(View.VISIBLE);
                        getView().getBtnToolbarSend().setRightText(UIUtils.getString(R.string.sure_with_count, mSelectedData.size()));
                    } else {
                      // getView().getBtnToolbarSend().setEnabled(false);
                        getView().getBtnToolbarSend().getRightTextView().setVisibility(View.GONE);
                        //getView().getBtnToolbarSend().setRightText(UIUtils.getString(R.string.sure));
                    }
                });
            }
        }

        private void setSelectedAdapter() {
            if (mSelectedAdapter == null) {
                mSelectedAdapter = new LQRAdapterForRecyclerView<Friend>(UIUtils.getContext(), mSelectedData, R.layout.item_selected_contact) {
                    @Override
                    public void convert(LQRViewHolderForRecyclerView helper, Friend item, int position) {
                        ImageView ivHeader = helper.getView(R.id.ivHeader);
                        AvatarHelper.getInstance().displayAvatar(item, ivHeader, true);
                    }
                };
                getView().getRvSelectedContacts().setAdapter(mSelectedAdapter);
            }
        }

        public void addGroupMembers() {
            ArrayList<String> selectedIds = new ArrayList<>(mSelectedData.size());
            for (int i = 0; i < mSelectedData.size(); i++) {
                Friend friend = mSelectedData.get(i);
                selectedIds.add(friend.getUserId());
            }
            Intent data = new Intent();
            data.putStringArrayListExtra("selectedIds", selectedIds);
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }

        public void createGroup() {
           // mSelectedData.add(0, DBManager.getInstance().getFriendById(UserCache.getId()));
            int size = mSelectedData.size();
            if (size == 0)
                return;

            List<String> selectedIds = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Friend friend = mSelectedData.get(i);
                selectedIds.add(friend.getUserId());
            }
            mGroupName = "";
            if (size > 3) {
                for (int i = 0; i < 3; i++) {
                    Friend friend = mSelectedData.get(i);
                    mGroupName += friend.getNickName() + "、";
                }
            } else {
                for (Friend friend : mSelectedData) {
                    mGroupName += friend.getNickName() + "、";
                }
            }
            mGroupName = mGroupName.substring(0, mGroupName.length() - 1);
           // mContext.showWaitingDialog(UIUtils.getString(R.string.please_wait));
        }

        private void loadError(Throwable throwable) {
            LogUtils.sf(throwable.getLocalizedMessage());
        }

    }
}

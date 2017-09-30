package com.lsjr.zizi.mvp.home;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.lsjr.bean.ObjectResult;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.callback.StringCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpFragment;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.bean.UploadAvatar;
import com.lsjr.zizi.chat.broad.MsgBroadcast;
import com.lsjr.zizi.chat.dao.UserDao;
import com.lsjr.zizi.chat.db.User;
import com.lsjr.zizi.chat.helper.LoginHelper;
import com.lsjr.zizi.chat.utils.StringUtils;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.zichat.AddressListActivity;
import com.lsjr.zizi.mvp.home.zichat.DepartureSelectorActivity;
import com.lsjr.zizi.mvp.home.zichat.UpdateSourceActivity;
import com.lsjr.zizi.mvp.home.zichat.dialog.DialogUtils;
import com.lsjr.zizi.mvp.utils.PhotoUtils;
import com.lsjr.zizi.util.CameraUtil;
import com.lsjr.zizi.view.OptionItemView;
import com.ymz.baselibrary.AppCache;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.GetFileSizeUtil;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ymz.baselibrary.widget.ItemView;
import com.zhy.autolayout.AutoLinearLayout;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/15 16:11
 */

public class MeFragment extends MvpFragment {

    @BindView(R.id.llMyInfo)
    AutoLinearLayout infoLl;

    @BindView(R.id.id_al_avatar)
    AutoLinearLayout autollAvatar;
    @BindView(R.id.tvName)
    TextView mTvName;
    @BindView(R.id.tvAccount)
    TextView mTvAccount;
    @BindView(R.id.oivSetting)
    OptionItemView oivSetting;
    @BindView(R.id.login_out)
    TextView login_out;
    @BindView(R.id.oivAliasName)
    ItemView oivAliasName;
    @BindView(R.id.ivAvatar)
    CircleImageView mAvatarImg;

    @BindView(R.id.id_setting)
    ItemView itSetting;
    @BindView(R.id.id_sex_set)
    ItemView idSexSet;

    User mLoginUser;


    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_me;
    }

    @Override
    protected void initView() {
        mLoginUser = ConfigApplication.instance().mLoginUser;
        long cacheSize = GetFileSizeUtil.getFileSize(new File(AppCache.getInstance().mAppDir));
        //T_.showToastReal("缓存大小"+	GetFileSizeUtil.formatFileSize(cacheSize)+"M");
        oivSetting.setRightText("缓存大小"+	GetFileSizeUtil.formatFileSize(cacheSize)+"M");
        String loginUserId = mLoginUser.getUserId();
        AvatarHelper.getInstance().displayAvatar(ConfigApplication.instance().mLoginUser, mAvatarImg, true);
        //mTvName.setText(ConfigApplication.instance().mLoginUser.getNickName());
        oivAliasName.setRightText(ConfigApplication.instance().mLoginUser.getNickName());
        idSexSet.setRightText(ConfigApplication.instance().mLoginUser.getSex()==0?"男":"女");
        mTvAccount.setText(ConfigApplication.instance().mLoginUser.getTelephone());
        mAvatarImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectAvatarDialog();
            }
        });

        autollAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        infoLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectAvatarDialog();
            }
        });

        login_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExitDialog();
            }
        });

        oivAliasName.setItemOnclickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), UpdateSourceActivity.class);
                Bundle bundle=new Bundle();
                bundle.putString("updateContent","修改用户名");
                bundle.putInt("key",5);
                intent.putExtras(bundle);
                startActivityForResult(intent, 100);
            }
        });
        oivAliasName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // showRemarkDialog();
                Intent intent = new Intent(getContext(), UpdateSourceActivity.class);
                Bundle bundle=new Bundle();
                bundle.putString("updateContent","修改用户名");
                bundle.putInt("key",5);
                intent.putExtras(bundle);
                startActivityForResult(intent, 100);

//                Intent intent = new Intent(BasicInfoActivity.this, UpdateSourceActivity.class);
//                Bundle bundle=new Bundle();
//                bundle.putString("updateContent","修改昵称");
//                bundle.putSerializable("Friend",mFriend);
//                bundle.putInt("key",5);
//                intent.putExtras(bundle);
//                startActivityForResult(intent, 100);
            }
        });


        itSetting.setItemOnclickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity(DepartureSelectorActivity.class);
                //openActivity(AddressListActivity.class);
            }
        });

        idSexSet.setItemOnclickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // openActivity(GeoCoderActivity.class);
                //showListDialog();
                showSexDialog();
            }
        });
    }

    public void showSexDialog(){
        Dialog dialog = new Dialog(getActivity(), R.style.dialog);
        dialog.setContentView(R.layout.dialog_sex_selector);
        Window sex_window = dialog.getWindow();
        sex_window.setGravity(Gravity.CENTER | Gravity.CENTER);
        Button btn_cancel = (Button) dialog.findViewById(R.id.btn_cancel);
        Button bt_boy = (Button) dialog.findViewById(R.id.bt_boy);
        Button bt_gril = (Button) dialog.findViewById(R.id.bt_gril);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        bt_boy.setOnClickListener(v -> {
            dialog.dismiss();
            //getDataSex("1");
            if (mLoginUser.getSex()==0)return;
            mLoginUser.setSex(0);
            updateData(ConfigApplication.instance().mLoginUser.getNickName());
           // idSexSet.setRightText("男");
        });
        bt_gril.setOnClickListener(v -> {
            dialog.dismiss();
           // getDataSex("2");
            if (mLoginUser.getSex()==1)return;
            mLoginUser.setSex(1);
            updateData(ConfigApplication.instance().mLoginUser.getNickName());
            //idSexSet.setRightText("女");
        });
        dialog.show();
    }


    /**
     * 显示列表的弹出窗
     */
    private void showListDialog() {
        DialogUtils dialogUtils = DialogUtils.newInstance(new DialogUtils.OnCallDialog() {
            @Override
            public Dialog getDialog(Context context) {
                return null;
            }
        }, true);
        dialogUtils.show(getFragmentManager(),"tag");
//        DialogHelper.showListDialog(getFragmentManager(), titleList, languanges,
//                (DialogHelper.IDialogResultListener<Integer>)
//                        result -> showToast(languanges[result]),
//                true);
    }

    private void showRemarkDialog() {
        final EditText editText = new EditText(getActivity());
        editText.setMaxLines(2);
        editText.setLines(2);
        editText.setText(ConfigApplication.instance().mLoginUser.getNickName());
        editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin=UIUtils.dip2px(10);
        editText.setLayoutParams(layoutParams);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.set_remark_name).setView(editText)
                .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = editText.getText().toString();
                        if (input.equals(mLoginUser.getNickName())||!StringUtils.isNickName(input)) {// 备注名没变
                            T_.showToastReal("备注名没变");
                            L_.e("不符合昵称"+input);
                            return;
                        }
                        L_.e("修改备注名字"+input);
                        updateData(input);
                    }
                }).setNegativeButton(getString(R.string.cancel), null);
        builder.create().show();
    }

    private void updateData(String input) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        if (!mLoginUser.getNickName().equals(input)&&!TextUtils.isEmpty(input)) {
            params.put("nickname", input);
        }
        params.put("sex", String.valueOf(mLoginUser.getSex()));
        params.put("birthday", String.valueOf(mLoginUser.getBirthday()));
        params.put("countryId", String.valueOf(mLoginUser.getCountryId()));
        params.put("provinceId", String.valueOf(mLoginUser.getProvinceId()));
        params.put("cityId", String.valueOf(mLoginUser.getCityId()));
        params.put("areaId", String.valueOf(mLoginUser.getAreaId()));
        showProgressDialogWithText("更新中");
        HttpUtils.getInstance().postServiceData(AppConfig.USER_UPDATE, params, new ChatObjectCallBack<Void>(Void.class) {

            @Override
            protected void onXError(String exception) {
                dismissProgressDialog();
            }

            @Override
            protected void onSuccess(ObjectResult<Void> result) {
                dismissProgressDialog();
                boolean success = ResultCode.defaultParser( result, true);
                if (success) {
                    ConfigApplication.instance().mLoginUser.setNickName(input);
                    UserDao.getInstance().updateNickName(mLoginUser.getUserId(), input);// 更新数据库
                   // mTvName.setText(input);
                    oivAliasName.setRightText(ConfigApplication.instance().mLoginUser.getNickName());
                    idSexSet.setRightText(ConfigApplication.instance().mLoginUser.getSex()==0?"男":"女");
                } else {

                }
            }
        });

    }


    private void showExitDialog() {
        new AlertDialog.Builder(getActivity()).setTitle(R.string.app_name).setMessage("您确定要退出登陆吗？").setNegativeButton("取消", null)
                .setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MsgBroadcast.broadcastMsgUiUpdate(UIUtils.getContext());
                        LoginHelper.broadcastLogout(UIUtils.getContext());
                    }
                }).create().show();
    }

    public void jumpToActivityAndClearTask(Class activity) {
        Intent intent = new Intent(getActivity(), activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void showSelectAvatarDialog() {
        String[] items = new String[] { "拍照", "相册" };
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle("选择方式").setSingleChoiceItems(items, 0,
                (dialog, which) -> {
                    if (which == 0) {
                        takePhoto();
                    } else {
                        selectPhoto();
                    }
                    dialog.dismiss();
                });
        builder.setCancelable(false);
        builder.show();

    }

    private static final int REQUEST_CODE_CAPTURE_CROP_PHOTO = 1;
    private static final int REQUEST_CODE_PICK_CROP_PHOTO = 2;
    private static final int REQUEST_CODE_CROP_PHOTO = 3;
    private static final int STORAGE_PERMISSIONS_REQUEST_CODE = 0x04;

    private Uri imageUri;
    private Uri cropImageUri;
    // 选择头像的数据
    private File mCurrentFile;
    File fileUri=new File(AppCache.getInstance().getPublicFilePath(1));
    private File fileCropUri = new File(Environment.getExternalStorageDirectory().getPath() + "/crop_photo.jpg");

    private void takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri =  FileProvider.getUriForFile(getActivity(), "com.lsjr.zizi.fileProvider", fileUri);
        } else {
            imageUri = CameraUtil.getOutputMediaFileUri(getActivity(), CameraUtil.MEDIA_TYPE_IMAGE);
        }
        PhotoUtils.takePicture(getActivity(), imageUri, REQUEST_CODE_CAPTURE_CROP_PHOTO);
    }

    private void selectPhoto() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSIONS_REQUEST_CODE);
        } else {
            PhotoUtils.openPic(getActivity(), REQUEST_CODE_PICK_CROP_PHOTO);
        }
    }

  //  @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
      //  super.onActivityResult(requestCode, resultCode, data);
        L_.e(requestCode+"*************"+requestCode+"--------------->");
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAPTURE_CROP_PHOTO) {// 拍照返回再去裁减
                if (imageUri != null) {
                    cropImageUri = Uri.fromFile(fileCropUri);
                    PhotoUtils.cropImageUri(getActivity(), imageUri, cropImageUri, 1, 1, 300, 300, REQUEST_CODE_CROP_PHOTO);

//                    Uri o = mNewPhotoUri;
//                    mNewPhotoUri = CameraUtil.getOutputMediaFileUri(getActivity(), CameraUtil.MEDIA_TYPE_IMAGE);
//                    mCurrentFile = new File(mNewPhotoUri.getPath());
                    //    CameraUtil.cropImage(getActivity(), o, mNewPhotoUri, REQUEST_CODE_CROP_PHOTO, 1, 1, 300, 300);
                } else {
                    T_.showToastReal("选择失败");
                }
            } else if (requestCode == REQUEST_CODE_PICK_CROP_PHOTO) {// 选择一张图片,然后立即调用裁减
                if (data != null && data.getData() != null) {
                    cropImageUri = Uri.fromFile(fileCropUri);
                    Uri newUri = Uri.parse(PhotoUtils.getPath(getActivity(), data.getData()));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        newUri = FileProvider.getUriForFile(getActivity(), "com.lsjr.zizi.fileProvider", new File(newUri.getPath()));
                    PhotoUtils.cropImageUri(getActivity(), newUri, cropImageUri, 1, 1, 300, 300, REQUEST_CODE_CROP_PHOTO);
                    //CameraUtil.cropImage(getActivity(), o, imageUri, REQUEST_CODE_CROP_PHOTO, 1, 1, 300, 300);
                } else {
                    T_.showToastReal("选择失败");
                }
            } else if (requestCode == REQUEST_CODE_CROP_PHOTO) {
//                Bitmap bitmap = PhotoUtils.getBitmapFromUri(cropImageUri, getContext());
//                if (bitmap != null) {
//                    mAvatarImg.setImageBitmap(bitmap);
//                }
                if (cropImageUri != null) {
                    mCurrentFile = new File(cropImageUri.getPath());
                    if (mCurrentFile != null && mCurrentFile.exists()) {
                        L_.e("上传图片路径------>"+mCurrentFile.getAbsolutePath());
                        uploadAvatar(mCurrentFile);
                    }
                } else {
                    T_.showToastReal("选择失败");
                }
            }else if (requestCode == 100){
                if (data==null)return;
                String updateStr= data.getStringExtra("udaptes");
                if (TextUtils.isEmpty(updateStr))return;
                oivAliasName.setRightText(updateStr);
            }

        }
    }


    private void uploadAvatar(File file) {
        if (!file.exists()) {// 文件不存在
            return;
        }
        // 显示正在上传的ProgressDialog
       // showProgressDialogWithText("上传头像中");
        Map<String,String> params=new HashMap<>();
        String loginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        params.put("userId", loginUserId);

        HttpUtils.getInstance().uploadFileWithParts(AppConfig.AVATAR_UPLOAD_URL, params, file, new StringCallBack() {


            @Override
            protected void onXError(String exception) {

            }

            @Override
            protected void onSuccess(String response) {
                L_.e("onSuccess---------"+response);
                UploadAvatar result = JSON.parseObject(response, UploadAvatar.class);
                L_.e("onSuccess---------"+result.getResultCode());
                UploadAvatar.DataBean data = result.getData();
                L_.e(data.toString());
                // Glide.with(UIUtils.getContext()).load(data.getOUrl()).into(ivAvatar);
                if (result.getResultCode()==1){
                    //data.getoUrl();
                    L_.e("显示照片-------》");
                   // UploadAvatar.Data data = result.getData();
                    L_.e(data.toString());
                   // Glide.with(UIUtils.getContext()).load(data.getTUrl()).into(mAvatarImg);
                    AvatarHelper.getInstance().displayAvatar(ConfigApplication.instance().mLoginUser, mAvatarImg, true);
                    //ImageLoader.getInstance().show(data.getoUrl(),mAvatarImg);
                }

            }
        });

    }


    @Override
    protected void afterCreate(Bundle savedInstanceState) {

    }
}

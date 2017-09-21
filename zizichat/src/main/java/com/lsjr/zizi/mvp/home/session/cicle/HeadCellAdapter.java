package com.lsjr.zizi.mvp.home.session.cicle;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.dao.MyPhotoDao;
import com.lsjr.zizi.chat.db.MyPhoto;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.session.FriendCircleActivity;
import com.lsjr.zizi.mvp.home.session.SendFriendCircleActivity;
import com.ymz.baselibrary.utils.UIUtils;
import com.ys.uilibrary.base.RVBaseCell;
import com.ys.uilibrary.base.RVBaseViewHolder;

import java.util.List;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/16 14:40
 */

public class HeadCellAdapter extends RVBaseCell {

    public final static int TYPE_HEAD = 0;
    List<MyPhoto>  mPhotos;
    public HeadCellAdapter(Object o) {
        super(o);
        mPhotos = MyPhotoDao.getInstance().getPhotos(ConfigApplication.instance().getLoginUserId());

    }


    @Override
    public int getItemType() {
        return TYPE_HEAD;
    }

    @Override
    public RVBaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View headView = LayoutInflater.from(parent.getContext()).inflate(R.layout.head_circle, parent, false);
        return new RVBaseViewHolder(headView);
    }

    @Override
    public void onBindViewHolder(RVBaseViewHolder holder, int position) {
        ImageView avatarImg= (ImageView) holder.getView(R.id.ig_icon_head);
        //ImageView imageView= (ImageView) holder.getView(R.id.id_ig_show);
        //AvatarHelper.getInstance().displayAvatar(ConfigApplication.instance().getLoginUserId(), avatarImg, false);
       // ImageUtils.doBlur()
        //ImageLoader.getInstance().showImage(ConfigApplication.instance().getLoginUserId(), avatarImg,R.drawable.icon_head);
        //setCoverPhotos(imageView);

        Friend friend=new Friend();
        friend.setUserId(ConfigApplication.instance().getLoginUserId());
        friend.setNickName(ConfigApplication.instance().mLoginUser.getNickName());
        AvatarHelper.getInstance().displayAvatar(friend, avatarImg, true);



        ImageView igBakc= (ImageView) holder.getView(R.id.id_back);
        ImageView igTalk= (ImageView) holder.getView(R.id.id_talk);
        igBakc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTopBarClickLisetener!=null){
                    onTopBarClickLisetener.onBack();
                }
            }
        });
        igTalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTopBarClickLisetener!=null){
                    onTopBarClickLisetener.onTalk();
                }
            }
        });
        igTalk.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (onTopBarClickLisetener!=null){
                    onTopBarClickLisetener.onPhonto();
                }
                return true;
            }
        });
        //AvatarHelper.getInstance().displayAvatar(friend, imageView, false);

        //AvatarHelper.getInstance().displayAvatarBg(ConfigApplication.instance().getLoginUserId(), avatarImg, true);
        //AvatarHelper.getInstance().displayAvatarBg(ConfigApplication.instance().getLoginUserId(), imageView, false);

    }

    public void setOnTopBarClickLisetener(HeadCellAdapter.onTopBarClickLisetener onTopBarClickLisetener) {
        this.onTopBarClickLisetener = onTopBarClickLisetener;
    }

    onTopBarClickLisetener onTopBarClickLisetener;
    public interface  onTopBarClickLisetener{
        void  onBack();
        void  onTalk();

        void onPhonto();
    }

    private void setCoverPhotos(ImageView mCoverImg) {
        if (mPhotos == null || mPhotos.size() <= 0) {
            return;
        }
        String[] coverPhotos = new String[mPhotos.size()];
        for (int i = 0; i < mPhotos.size(); i++) {
            coverPhotos[i] = mPhotos.get(i).getOriginalUrl();
        }
       // mCoverImg.setImageResource(coverPhotos);
    }

}

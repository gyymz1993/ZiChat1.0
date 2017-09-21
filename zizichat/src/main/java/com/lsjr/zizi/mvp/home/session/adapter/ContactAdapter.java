package com.lsjr.zizi.mvp.home.session.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lsjr.zizi.R;
import com.lsjr.zizi.bean.UserInfo;
import com.lsjr.zizi.chat.bean.PhoneInfo;
import com.lsjr.zizi.mvp.circledemo.bean.PhotoInfo;

import java.util.List;

import cn.ittiger.indexlist.adapter.IndexStickyViewAdapter;

public class ContactAdapter extends IndexStickyViewAdapter<PhoneInfo> {

        public ContactAdapter(List<PhoneInfo> list) {
            super(list);
        }

        @Override
        public RecyclerView.ViewHolder onCreateIndexViewHolder(ViewGroup parent) {

            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.indexsticky_item_index, parent, false);
            return new IndexViewHolder(view);
        }

        @Override
        public RecyclerView.ViewHolder onCreateContentViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contacts, parent, false);
            return new ContentViewHolder(view);
        }

        @Override
        public void onBindIndexViewHolder(RecyclerView.ViewHolder holder, int position, String indexName) {

            IndexViewHolder indexViewHolder = (IndexViewHolder) holder;
            indexViewHolder.mTextView.setText(indexName);
        }

        @Override
        public void onBindContentViewHolder(RecyclerView.ViewHolder holder, int position, PhoneInfo userInfo) {
            // 设置头像
            //final Friend friend = friends.get(position);
            ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
            // 昵称
            contentViewHolder.nick_name_tv.setText(userInfo.getName());
            // 个性签名
            //contentViewHolder.des_tv.setText(friend.getDescription());
            contentViewHolder.id_root_ry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }


    class IndexViewHolder extends RecyclerView.ViewHolder {
        TextView mTextView;

        public IndexViewHolder(View itemView) {

            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.tv_index);
        }
    }

    class ContentViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout id_root_ry;
        TextView catagoryTitleTv;
        ImageView avatar_img;
        TextView nick_name_tv;
        TextView des_tv;

        public ContentViewHolder(View itemView) {
            super(itemView);
            id_root_ry = (RelativeLayout) itemView.findViewById(R.id.id_root_ry);
            catagoryTitleTv = (TextView) itemView.findViewById(R.id.catagory_title);
            avatar_img = (ImageView) itemView.findViewById(R.id.avatar_img);
            nick_name_tv = (TextView) itemView.findViewById(R.id.nick_name_tv);
            des_tv = (TextView) itemView.findViewById(R.id.des_tv);
        }
    }


}

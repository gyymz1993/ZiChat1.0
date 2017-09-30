package com.lsjr.zizi.mvp.circledemo.mvp.contract;

import com.lsjr.zizi.mvp.circledemo.bean.CircleItem;
import com.lsjr.zizi.mvp.circledemo.bean.CommentConfig;
import com.lsjr.zizi.mvp.circledemo.bean.CommentItem;
import com.lsjr.zizi.mvp.circledemo.bean.FavortItem;

import java.util.List;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2016/7/15 18:05
 */
public interface CircleContract {

    interface View extends BaseView{
        void update2DeleteCircle(String circleId);
        void update2AddFavorite(int circlePosition, FavortItem addItem);
        void update2DeleteFavort(int circlePosition, String favortId);
        void update2AddComment(int circlePosition, CommentItem addItem);
        void update2DeleteComment(int circlePosition, String commentId);
        void updateEditTextBodyVisible(int visibility, CommentConfig commentConfig);
        void update2loadData(int loadType, List<CircleItem> datas);
    }

    interface Presenter extends BasePresenter{
        void loadData(int loadType);
        void deleteCircle(final String circleId);
        void addFavort(final int circlePosition);
        void deleteFavort(final int circlePosition, final String favortId);
        void deleteComment(final int circlePosition, final String commentId);

    }
}

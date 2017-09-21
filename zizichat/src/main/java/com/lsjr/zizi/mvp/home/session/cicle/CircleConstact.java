package com.lsjr.zizi.mvp.home.session.cicle;

import com.lsjr.zizi.chat.bean.Comment;
import com.lsjr.zizi.chat.bean.PublicMessage;
import com.lsjr.zizi.mvp.circledemo.bean.CommentConfig;

import java.util.List;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/28 17:17
 */

public class CircleConstact {
    static  CircleConstact circleConstact=new CircleConstact();
    private CircleConstact(){

    }
    public static CircleConstact getCircleConstact() {
        return circleConstact;
    }

    public PublicMessage currentPublicMessage;
    private int position;
    private List<Comment> currentComments;
    CommentConfig config;

    private Comment currentComment;

    public List<Comment> getCurrentComments() {
        return currentComments;
    }

    public void setCurrentComments(List<Comment> currentComments) {
        this.currentComments = currentComments;
    }

    public Comment getCurrentComment() {
        return currentComment;
    }

    public void setCurrentComment(Comment currentComment) {
        this.currentComment = currentComment;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setCurrentPublicMessage(PublicMessage currentPublicMessage) {
        this.currentPublicMessage = currentPublicMessage;
    }

    public PublicMessage getCurrentPublicMessage() {
        return currentPublicMessage;
    }

    public CommentConfig getConfig() {
        return config;
    }

    public void setConfig(CommentConfig config) {
        this.config = config;
    }


    /*
    *
    *  CommentConfig config = new CommentConfig();
                                    config.circlePosition = circlePosition;
                                    config.commentPosition = commentPosition;
                                    config.commentType = CommentConfig.Type.REPLY;
                                    config.replyUser = commentItem.getUser();
                                    presenter.showEditTextBody(config);
    * */

}

package com.lsjr.zizi.mvp.home.session.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.lsjr.zizi.R;
import com.lsjr.zizi.mvp.upload.FreshImaCallBack;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.List;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/24 14:47
 */

public class SelectPhotoAdapter extends ABaseRefreshAdapter<String> {

    private List<String> mData;
    private final static int maxImageSize = 9;
    private FreshImaCallBack freshImgCallBack;//针对三种操作逻辑所自定义的回调
    public SelectPhotoAdapter(Context context, List<String> datas, int itemLayoutId) {
        super(context, datas, itemLayoutId);
        mData=datas;
    }

    @Override
    protected void convert(BaseRecyclerHolder convertView, String filePaht, int position) {
      //  convertView = LayoutInflater.from(context).inflate(R.layout.item_gridview, null);

        L_.e("convert------->"+mData.size());
        LinearLayout rootLy=convertView.getView(R.id.id_root_ly);
        SimpleDraweeView sdvItemShowImg = convertView.getView(R.id.sdvItemShowImg);
        ImageView ivDeleteImg = convertView.getView(R.id.ivDeleteImg);
        ImageView ivItemAdd =  convertView.getView(R.id.ivItemAdd);
        FrameLayout rlItemShow = convertView.getView(R.id.rlItemShow);
        ivItemAdd.setVisibility(View.VISIBLE);
        rlItemShow.setVisibility(View.VISIBLE);

        if (mData.size()>=maxImageSize){
            showImg(convertView,filePaht,position);
        }else {
            if (position == mData.size() - 1) {
                rlItemShow.setVisibility(View.GONE);
                ivItemAdd.setVisibility(View.VISIBLE);
            } else {
                if (mData.size() > 1) {
                    showImg(convertView,filePaht,position);
                }
            }
        }

        //放在外面用于更新position
        sdvItemShowImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                freshImgCallBack.previewImag(position);//预览图片
            }
        });
        ivDeleteImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                freshImgCallBack.updateGvIgShow(position);//更新数据
            }
        });
        ivItemAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                freshImgCallBack.openGallery();//打开相册放在里面即可
            }
        });

        LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(UIUtils.WHD()[0]/4,UIUtils.WHD()[0]/4);
        layoutParams.gravity= Gravity.CENTER;
        rootLy.setLayoutParams(layoutParams);
    }



    //显示图片
    private void showImg(BaseRecyclerHolder convertView,String filePaht,int position) {
        SimpleDraweeView sdvItemShowImg = convertView.getView(R.id.sdvItemShowImg);
       // convertView.getView(R.id.ivItemAdd).setVisibility(View.GONE);
        convertView.getView(R.id.ivItemAdd).setVisibility(View.GONE);
        if (position == mData.size() - 1) {
            convertView.getView(R.id.rlItemShow).setVisibility(View.GONE);
        }else {
            convertView.getView(R.id.rlItemShow).setVisibility(View.VISIBLE);
            //设置图片
            ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse("file://" + filePaht))
                    .setProgressiveRenderingEnabled(true)
                    .setResizeOptions(new ResizeOptions(100, 100))
                    .build();
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(request)
                    .setAutoPlayAnimations(true)
                    .setTapToRetryEnabled(true)
                    .setOldController(sdvItemShowImg.getController())
                    .build();
            sdvItemShowImg.setController(controller);
        }

    }


    /**
     * 设置回调
     *
     * @param callBack freshImgCallBack
     */
    public void setImgShowFresh(FreshImaCallBack callBack) {
        freshImgCallBack = callBack;
    }
}

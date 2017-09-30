package com.lsjr.zizi.mvp.circledemo.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.View;

import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.Praise;
import com.lsjr.zizi.mvp.circledemo.bean.FavortItem;
import com.lsjr.zizi.mvp.circledemo.spannable.CircleMovementMethod;
import com.lsjr.zizi.mvp.circledemo.spannable.SpannableClickable;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.List;

/**
 * Created by yiwei on 16/7/9.
 */
public class PraiseListView extends android.support.v7.widget.AppCompatTextView{


    private int itemColor;
    private int itemSelectorColor;
    private List<Praise> datas;
    private OnItemClickListener onItemClickListener;

    public OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public PraiseListView(Context context) {
        super(context);
    }

    public PraiseListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
    }

    public PraiseListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.PraiseListView, 0, 0);
        try {
            //textview的默认颜色
            itemColor = typedArray.getColor(R.styleable.PraiseListView_item_color, getResources().getColor(R.color.praise_item_default));
            itemSelectorColor = typedArray.getColor(R.styleable.PraiseListView_item_selector_color, getResources().getColor(R.color.praise_item_selector_default));

        }finally {
            typedArray.recycle();
        }
    }

    public List<Praise> getDatas() {
        return datas;
    }
    public void setDatas(List<Praise> datas) {
        this.datas = datas;
        L_.e("--------------setDatas"+datas.size());
        notifyDataSetChanged();
    }


    public void notifyDataSetChanged(){
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if(datas != null && datas.size() > 0){
            //添加点赞图标
            builder.append(setImageSpan());
            Praise item = null;
            for (int i=0; i<datas.size(); i++){
                item = datas.get(i);
                if(item != null){
                    L_.e("=-=========点赞用户"+item.getNickName());
                    builder.append(setClickableSpan(item.getNickName(), i));
                    if(i != datas.size()-1){
                        builder.append(", ");
                    }
                }
            }
        }
        setText(builder);
        setMovementMethod(new CircleMovementMethod(itemSelectorColor));
    }


    private SpannableString setImageSpan(){
        String text = "  ";
        SpannableString imgSpanText = new SpannableString(text);
        imgSpanText.setSpan(new ImageSpan(UIUtils.getContext(), R.drawable.icon_praise, DynamicDrawableSpan.ALIGN_BASELINE),
                0 , 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return imgSpanText;
    }

    @NonNull
    private SpannableString setClickableSpan(String textStr, final int position) {
        SpannableString subjectSpanText = new SpannableString(textStr);
        subjectSpanText.setSpan(new SpannableClickable(itemColor){
                                    @Override
                                    public void onClick(View widget) {
                                        if(onItemClickListener!=null){
                                            onItemClickListener.onClick(position);
                                        }
                                    }
                                }, 0, subjectSpanText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return subjectSpanText;
    }


    public static interface OnItemClickListener{
        public void onClick(int position);
    }
}

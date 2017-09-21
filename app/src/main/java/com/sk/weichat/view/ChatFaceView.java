package com.sk.weichat.view;

import java.util.ArrayList;

import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;

import com.sk.weichat.R;
import com.sk.weichat.util.SmileyParser;

/**
 * 表情界面
 * 
 * @author Administrator
 * 
 */
public class ChatFaceView extends RelativeLayout {
	private Context mContext;
	private ViewPager mViewPager;
	private RadioGroup mFaceRadioGroup;// 切换不同组表情的RadioGroup
	private LinearLayout mPointsLayout;// 标志滑动到第几页的点的布局

	private ImageView[] mPoints;
	private PagerListener mPagerListener;

	private boolean mHasGif;

	public ChatFaceView(Context context) {
		super(context);
		init(context);
	}

	public ChatFaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(attrs);
		init(context);
	}

	public ChatFaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAttrs(attrs);
		init(context);
	}

	private void initAttrs(AttributeSet attrs) {
		if (attrs == null) {
			return;
		}
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ChatFaceView);// TypedArray是一个数组容器
		mHasGif = a.getBoolean(R.styleable.ChatFaceView_hasGif, true);
		a.recycle();
	}

	private int mScreenWidth;

	@SuppressWarnings("deprecation")
	private void init(Context context) {
		mContext = context;
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display d = wm.getDefaultDisplay();
		mScreenWidth = d.getWidth();

		LayoutInflater.from(mContext).inflate(R.layout.chat_face_view, this);
		mViewPager = (ViewPager) findViewById(R.id.view_pager);
		mFaceRadioGroup = (RadioGroup) findViewById(R.id.face_btn_layout);
		mPointsLayout = (LinearLayout) findViewById(R.id.chat_face_point_loy);
		mFaceRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.default_face) {
					switchViewPager1();
				} else if (checkedId == R.id.moya_face_gif) {
					switchViewPager2();
				}
			}
		});
		mFaceRadioGroup.check(R.id.default_face);

		if (!mHasGif) {
			mFaceRadioGroup.setVisibility(View.GONE);
		}
	}

	public void setHasGift(boolean hasGift) {
		mHasGif = hasGift;
		if (!mHasGif) {
			mFaceRadioGroup.setVisibility(View.GONE);
		}
	}

	private static int dip_To_px(Context context, int dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

	/**
	 * 初始化ViewPager，默认表情 2013-3-18
	 */
	private void switchViewPager1() {
		int[][] resId = SmileyParser.Smilies.getIds();
		String[][] strArray = SmileyParser.Smilies.getTexts();
		mPoints = new ImageView[resId.length];// 有几页表情
		mPointsLayout.removeAllViews();
		for (int i = 0; i < mPoints.length; i++) {
			ImageView point = new ImageView(mContext);
			if (i == 0) {
				point.setBackgroundResource(R.drawable.im_tab_press);
			} else {
				point.setBackgroundResource(R.drawable.im_tab_normal);
			}
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dip_To_px(mContext, 5), dip_To_px(mContext, 5));
			params.setMargins(0, 0, dip_To_px(mContext, 5), 2);
			point.setLayoutParams(params);
			mPointsLayout.addView(point);
			mPoints[i] = point;
		}
		mPagerListener = new PagerListener(mPoints);
		mViewPager.setOnPageChangeListener(mPagerListener);
		List<View> views = new ArrayList<View>();
		for (int i = 0; i < resId.length; i++) {
			View contentView = LayoutInflater.from(mContext).inflate(R.layout.emotion_gridview, null);
			GridView gridView = (GridView) contentView.findViewById(R.id.gridView);
			gridView.setSelector(R.drawable.chat_face_bg);
			EmotionImgAdapter adapter = new EmotionImgAdapter(mContext, resId[i], 1);
			gridView.setAdapter(adapter);
			gridView.setOnItemClickListener(new EmotionItemClick(i, strArray, 1));
			views.add(contentView);
		}
		mViewPager.setAdapter(new EmotionPagerAdapter(views));

	}

	/**
	 * 磨牙 Gif wjm
	 */

	public void switchViewPager2() {
		int[][] resId = SmileyParser.Gifs.getIds();
		String[][] strArray = SmileyParser.Gifs.getTexts();

		mPoints = new ImageView[resId.length];
		mPointsLayout.removeAllViews();
		for (int i = 0; i < mPoints.length; i++) {
			ImageView point = new ImageView(mContext);
			if (i == 0) {
				point.setBackgroundResource(R.drawable.im_tab_press);
			} else {
				point.setBackgroundResource(R.drawable.im_tab_normal);
			}
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dip_To_px(mContext, 5), dip_To_px(mContext, 5));
			params.setMargins(0, 0, dip_To_px(mContext, 5), 2);
			point.setLayoutParams(params);
			mPointsLayout.addView(point);
			mPoints[i] = point;
		}
		mPagerListener = new PagerListener(mPoints);
		mViewPager.setOnPageChangeListener(mPagerListener);
		List<View> views = new ArrayList<View>();
		for (int i = 0; i < resId.length; i++) {
			View contentView = LayoutInflater.from(mContext).inflate(R.layout.chat_face_gridview, null);
			GridView gridView = (GridView) contentView.findViewById(R.id.gridView);
			gridView.setSelector(R.drawable.chat_face_bg);
			EmotionImgAdapter adapter = new EmotionImgAdapter(mContext, resId[i], 2);
			gridView.setAdapter(adapter);
			gridView.setColumnWidth(20);
			gridView.setOnItemClickListener(new EmotionItemClick(i, strArray, 2));
			views.add(contentView);
		}
		mViewPager.setAdapter(new EmotionPagerAdapter(views));
	}

	public static interface EmotionClickListener {
		public void onNormalFaceClick(SpannableString ss);

		public void onGifFaceClick(String resName);
	}

	private EmotionClickListener mEmotionClickListener;

	public void setEmotionClickListener(EmotionClickListener listener) {
		mEmotionClickListener = listener;
	}

	/**
	 * 点击单个表情图片
	 * 
	 * @author xk 2013-3-8
	 */
	class EmotionItemClick implements OnItemClickListener {
		int index;
		int type;
		String[][] strArray;

		public EmotionItemClick(int index, String[][] strArray, int type) {
			this.index = index;
			this.strArray = strArray;
			this.type = type;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			String insertText = strArray[index][position];
			int insertResId = ((Integer) parent.getAdapter().getItem(position)).intValue();
			if (type == 1) {
				if (mEmotionClickListener != null) {
					SpannableString ss = new SpannableString(insertText);
					Drawable d = mContext.getResources().getDrawable(insertResId);
					d.setBounds(0, 0, (int) (d.getIntrinsicWidth() / 1.5), (int) (d.getIntrinsicHeight() / 1.5));// 设置表情图片的显示大小
					ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
					ss.setSpan(span, 0, insertText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					mEmotionClickListener.onNormalFaceClick(ss);
				}
			} else if (type == 2) {
				if (mEmotionClickListener != null) {
					mEmotionClickListener.onGifFaceClick(insertText);
				}
			}
		}

	}

	/**
	 * 对ViewPager设置Listener
	 * 
	 * @author xk 2013-3-8
	 */
	class PagerListener implements OnPageChangeListener {
		ImageView[] imgArray;

		public PagerListener(ImageView[] array) {
			imgArray = array;
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int arg0) {
			for (int i = 0; i < imgArray.length; i++) {
				imgArray[i].setBackgroundResource(i == arg0 ? R.drawable.im_tab_press : R.drawable.im_tab_normal);
			}
		}

	}

	class EmotionImgAdapter extends BaseAdapter {
		private LayoutInflater inflater;
		private int[] resId;
		int switchID;

		public EmotionImgAdapter(Context context, int[] resId, int switchID) {
			inflater = LayoutInflater.from(context);
			this.resId = resId;
			this.switchID = switchID;
		}

		@Override
		public int getCount() {
			return resId.length;
		}

		@Override
		public Integer getItem(int position) {
			return resId[position];
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (null == convertView) {
				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.item_emotion, parent, false);
				holder.img = (ImageView) convertView.findViewById(R.id.img);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			if (switchID == 2) {
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(mScreenWidth / 4, mScreenWidth / 4);
				holder.img.setLayoutParams(layoutParams);
				holder.img.setBackgroundResource(resId[position]);
			} else {
				holder.img.setImageResource(resId[position]);
			}

			return convertView;
		}

		class ViewHolder {
			ImageView img;
		}
	}

	class EmotionPagerAdapter extends PagerAdapter {

		private List<View> views;

		public EmotionPagerAdapter(List<View> views) {
			this.views = views;
		}

		@Override
		public int getCount() {
			return views.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			((ViewPager) container).removeView(views.get(position));
		}

		@Override
		public Object instantiateItem(View container, int position) {
			((ViewGroup) container).addView(views.get(position));
			return views.get(position);
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

	}

}

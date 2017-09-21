package com.lsjr.zizi.loader;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.db.User;
import com.lsjr.zizi.util.ImageUtils;
import com.ymz.baselibrary.utils.CacheUtils;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.Random;

/**
 * 用户头像的上传和获取
 *
 *
 */
public class AvatarHelper {

	private static AvatarHelper instance;
	public static AvatarHelper getInstance() {
		if (instance == null) {
			synchronized (AvatarHelper.class) {
				if (instance == null) {
					instance = new AvatarHelper();
				}
			}
		}
		return instance;
	}




	private int defaultHead(){
		int max = 7;
		int min = 1;
		Random random = new Random();
		int s = random.nextInt(max)%(max-min+1) + min;
		switch (s){
			case 1:
				return R.drawable.head_one;
			case 2:
				return R.drawable.head_two;
			case 3:
				return R.drawable.head_three;
			case 4:
				return R.drawable.head_four;
			case 5:
				return R.drawable.head_five;
			case 6:
				return R.drawable.head_six;
			case 7:
				return R.drawable.head_eight;

		}
		return R.drawable.head_one;
	}


	public void displayAvatar(String userName,String userId, final ImageView imageView, final boolean isThumb) {
		final String url = getAvatarUrl(userId, isThumb);
		//http://192.168.100.10:8092/avatar/t/11/11.jpg
		//http://192.168.100.10/avatar/t/11/11.jpg
		L_.e(url);
		if (TextUtils.isEmpty(url)) {
			return;
		}
		showImage(userName, userId,url, imageView, R.drawable.defaultpic);

	}

	public void displayAvatar(String userId, final ImageView imageView, final boolean isThumb) {
		final String url = getAvatarUrl(userId, isThumb);
		//http://192.168.100.10:8092/avatar/t/11/11.jpg
		//http://192.168.100.10/avatar/t/11/11.jpg
		L_.e(url);
		if (TextUtils.isEmpty(url)) {
			return;
		}
		showImage(null, userId,url, imageView, R.drawable.defaultpic);

	}

    public void displayAvatar(Friend friend, final ImageView imageView, final boolean isThumb) {
        final String url = getAvatarUrl(friend.getUserId(), isThumb);
        if (TextUtils.isEmpty(url)) {
            return;
        }
        String name = friend.getRemarkName();
        if (!TextUtils.isEmpty(name)) {
            friend.setNickName(name);
        }
        showImage(friend.getNickName(), friend.getUserId(),url, imageView, R.drawable.defaultpic);

    }

    public void displayAvatar(User friend, final ImageView imageView, final boolean isThumb) {
        final String url = getAvatarUrl(friend.getUserId(), isThumb);
        if (TextUtils.isEmpty(url)) {
            return;
        }
        showImage(friend.getNickName(), friend.getUserId(),url, imageView, R.drawable.defaultpic);

    }

	public void displayAvatarBg(String userId, final ImageView imageView, final boolean isThumb) {
		final String url = getAvatarUrl(userId, isThumb);
		if (TextUtils.isEmpty(url)) {
			return;
		}
		showImage(url, imageView);
	}


	//    .placeholder(R.mipmap.default_img) .signature( new StringSignature("01"))//增加签名
	private void showImage(String name,String userId,String url, ImageView imageView, int errorResId) {
		//String name=ConfigApplication.instance().mLoginUser.getNickName();
		//String userId = ConfigApplication.instance().mLoginUser.getUserId();
		//String defaultAvatar = RongGenerate.generateDefaultAvatar(name, userId);
       //然后使用方法decodeByteArray（）方法解析编码，生成Bitmap对象。
       // Bitmap bitmap = getCacheAvatarImage(defaultAvatar);
		//Bitmap bitmap = ImageUtils.getLoacalBitmap(defaultAvatar);
		//BitmapDrawable bd= new BitmapDrawable(bitmap);
		//L_.e("showImage------------------>"+bd);
		Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(true).
                error(R.drawable.head_one).diskCacheStrategy(DiskCacheStrategy.NONE).dontAnimate().into(imageView);
	}

    private Bitmap getCacheAvatarImage(String filePath){
        Bitmap bitmap = CacheUtils.getInstance().getBitmap(filePath);
        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = ImageUtils.getLoacalBitmap(filePath);
            if (bitmap != null) {
                CacheUtils.getInstance().put(filePath, bitmap);
            }
        }
        return bitmap;
    }


	private void showImage(String url, ImageView imageView) {
		Glide.with(UIUtils.getContext()).load(url).skipMemoryCache(false).
				diskCacheStrategy(DiskCacheStrategy.SOURCE).dontAnimate().centerCrop().into(imageView);
	}


	public static String getAvatarUrl(String userId, boolean isThumb) {
		if (TextUtils.isEmpty(userId)) {
			return null;
		}
		int userIdInt = -1;
		try {
			userIdInt = Integer.parseInt(userId);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		if (userIdInt == -1 || userIdInt == 0) {
			return null;
		}

		int dirName = userIdInt % 10000;
		String url;
		if (isThumb) {
			url = AppConfig.AVATAR_THUMB_PREFIX + "/" + dirName + "/" + userId + ".jpg";
		} else {
			url = AppConfig.AVATAR_ORIGINAL_PREFIX + "/" + dirName + "/" + userId + ".jpg";
		}
		return url;
	}


}

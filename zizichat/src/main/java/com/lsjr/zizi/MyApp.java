package com.lsjr.zizi;

import android.app.Activity;
import android.net.Uri;
import android.support.multidex.MultiDexApplication;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.lqr.emoji.LQREmotionKit;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.loader.ImageLoader;
import com.lqr.imagepicker.view.CropImageView;
import com.lsjr.bean.ArrayResult;
import com.lsjr.callback.ChatArrayCallBack;
import com.lsjr.callback.HttpSubscriber;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.bean.PublicMessage;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.ymz.baselibrary.AppCache;
import com.ymz.baselibrary.BaseApplication;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;


/**
 * ━━━━━━神兽出没━━━━━━
 * 　　　┏┓　　　┏┓
 * 　　┏┛┻━━━┛┻┓
 * 　　┃　　　　　　　┃
 * 　　┃　　　━　　　┃
 * 　　┃　┳┛　┗┳　┃
 * 　　┃　　　　　　　┃
 * 　　┃　　　┻　　　┃
 * 　　┃　　　　　　　┃
 * 　　┗━┓　　　┏━┛
 * 　　　　┃　　　┃  神兽保佑
 * 　　　　┃　　　┃  代码无bug
 * 　　　　┃　　　┗━━━┓
 * 　　　　┃　　　　　　　┣┓
 * 　　　　┃　　　　　　　┏┛
 * 　　　　┗┓┓┏━┳┓┏┛
 * 　　　　　┃┫┫　┃┫┫
 * 　　　　　┗┻┛　┗┻┛
 * ━━━━━━感觉萌萌哒━━━━━━
 *
 * @描述 BaseApp的拓展，用于设置其他第三方的初始化
 */
public class MyApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
       // initImagePicker();
        BaseApplication.instance().initialize(this);
        ConfigApplication.instance().initialize(this);
        //DcodeService.initialize(this);
       // ConfigApplication.instance().initialize(this);
        Fresco.initialize(this);
        BaseApplication.instance().registerUiScreen(750,1334);
        //BaseApplication.instance().initLeakCanary();
        AppCache.getInstance().initCreateAppDir();
        initImagePicker();
        //初始化表情控件
        LQREmotionKit.init(this, (context, path, imageView) -> Glide.with(context).load(path).centerCrop()
                .skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).into(imageView));

        // test();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .threadPriority(Thread.NORM_PRIORITY - 2).threadPoolSize(4).build();
        // Initialize ImageLoader with configuration.
        com.nostra13.universalimageloader.core.ImageLoader.getInstance().init(config);

        //AppService.startService(this);
       // downOnlineMsg();
    }



    /**
     * 离线消息
     */
    private void downOnlineMsg() {
        L_.e("downOnlineMsg");
        HashMap<String, String> params = new HashMap<>();
       // params.put("access_token", ConfigApplication.instance().mAccessToken);
        params.put("from", "3");
        params.put("to", "4");
        //direction=1（receiver=发送方，sender=接收方）
//        params.put("startTime",(System.currentTimeMillis()-60 * 60 * 24 )+"");
//        params.put("endTime", System.currentTimeMillis()+"");
//        params.put("pageIndex", "1");
//        params.put("pageSize", "50");
        HttpUtils.getInstance().postServiceData(AppConfig.ONLINE_MSG, params, new ChatArrayCallBack<PublicMessage>(PublicMessage.class) {

            @Override
            protected void onXError(String exception) {
                T_.showToastReal(exception);
            }

            @Override
            protected void onSuccess(ArrayResult<PublicMessage> result) {
                L_.e(result.toString());
                boolean success = ResultCode.defaultParser( result, true);
                //mPullToRefreshListView.onRefreshComplete();
            }
        });

    }





    /**
     * 初始化仿微信控件ImagePicker
     */
    private void initImagePicker() {
        ImagePicker imagePicker = ImagePicker.getInstance();
        imagePicker.setImageLoader(new ImageLoader() {
            @Override
            public void displayImage(Activity activity, String path, ImageView imageView, int width, int height) {
                Glide.with(getApplicationContext()).load(Uri.parse("file://" + path).toString()).centerCrop().into(imageView);
            }

            @Override
            public void clearMemoryCache() {

            }
        });   //设置图片加载器
        imagePicker.setShowCamera(true);  //显示拍照按钮
        imagePicker.setCrop(true);        //允许裁剪（单选才有效）
        imagePicker.setSaveRectangle(true); //是否按矩形区域保存
        imagePicker.setSelectLimit(9);    //选中数量限制
        imagePicker.setStyle(CropImageView.Style.RECTANGLE);  //裁剪框的形状
        imagePicker.setFocusWidth(800);   //裁剪框的宽度。单位像素（圆形自动取宽高最小值）
        imagePicker.setFocusHeight(800);  //裁剪框的高度。单位像素（圆形自动取宽高最小值）
        imagePicker.setOutPutX(1000);//保存文件的宽度。单位像素
        imagePicker.setOutPutY(1000);//保存文件的高度。单位像素
    }

    String aitTicketUrl = "http://ws.51book.com:8000/ltips/services/getAvailableFlightWithPriceAndCommisionServiceRestful1.0/getAvailableFlightWithPriceAndCommision";

    /*
       * 加密算法
       */
    public static String encode(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            byte[] result = digest.digest(text.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                int number = b & 0xff;
                String hex = Integer.toHexString(number);
                if (hex.length() == 1) {
                    sb.append("0" + hex);
                } else {
                    sb.append(hex);
                }
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    public void test(){
        //aitTicketUrl="http://ws.51book.com:8000/ltips/services/getAvailableFlightWithPriceAndCommisionService1.0?wsdl";
        Map<String, String> map = new HashMap<>();
        map.put("agencyCode", "ZZWL88"); //公司账号id
        // map.put("group", "2017-06-25");
        // map.put("officeNo", "WUH");
        map.put("pidNo", "BJP");
        map.put("orgAirportCode", "PEK"); //出发城市三字码
        map.put("dstAirportCode", "SHA"); //抵达城市三字码
        map.put("airlineCode", "CA"); //航空公司两字码
        map.put("onlyAvailableSeat", "1");
        map.put("onlyNormalCommision", "1");
        map.put("onlyOnWorkingCommision", "1");
        map.put("onlySelfPNR", "1");
        map.put("orgAirportCode", "PEK");
        map.put("date", "2017-10-01");
        map.put("depTime", ""); //起飞时间	String	否	格式：“HHSS”
        map.put("direct", "D"); //是否直飞	String	否	D为直飞（可经停不中转）
        map.put("sign", encode("ZZWL88" + "SHA" +
                "1" + "1" + "1" + "1" + "PEK" + "#jGhn*$$"));

        HttpUtils.getInstance().postServiceData(aitTicketUrl, map, new HttpSubscriber() {
            @Override
            protected void onXError(String exception) {

            }

            @Override
            protected void onFailure(String msg) {

            }

            @Override
            protected void onSuccess(String response) {

            }
        });
    }

}

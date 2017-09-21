package com.sk.weichat.util;


public class HeadUtil {

	private static final String[] IMAGE_HEAD = { "http://t11.baidu.com/it/u=3152599756,1043471390&fm=56",
			"http://t11.baidu.com/it/u=3122060977,2586459116&fm=56", "http://t12.baidu.com/it/u=2765799854,136034624&fm=56",
			"http://t12.baidu.com/it/u=2785619621,148693455&fm=56", "http://t12.baidu.com/it/u=2752039340,114156826&fm=56",
			"http://t12.baidu.com/it/u=1846256132,3722614394&fm=56", "http://t10.baidu.com/it/u=1203974490,2298671380&fm=56",
			"http://t10.baidu.com/it/u=2421093975,3240953536&fm=56", "http://t10.baidu.com/it/u=2330821068,3152004504&fm=56",
			"http://t12.baidu.com/it/u=650515163,789500416&fm=56", "http://t10.baidu.com/it/u=2141311539,3870874150&fm=56", };

	public static String getHeadUrl(int index) {
		return IMAGE_HEAD[index % IMAGE_HEAD.length];
	}
}

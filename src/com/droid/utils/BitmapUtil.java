package com.droid.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;
import android.util.LruCache;

import com.droid.network.HttpClient;
import com.example.android.bitmapfun.util.DiskLruCache;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.CoreConnectionPNames;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * 使用 内存硬盘 双重缓存
 * 
 * @author shenhui
 * 
 */

public class BitmapUtil {

	private static final String TAG = "BitmapUtil";
	private static Context mContext;
	private static BitmapUtil instance;

	private static LruCache<String, Bitmap> memoryCache;
	private static DiskLruCache mDiskCache;
	private static final long DISK_CACHE_SIZE = 1024 * 1024 * 80; // 80MB
	private static final int MEMORY_CACHE_SIZE = 1024 * 1024 * 20; // 20MB
	private static final String DISK_CACHE_SUBDIR = "diskCache";

	private BitmapUtil(Context context) {
		mContext = context;
		// init memoryCache
		memoryCache = new LruCache<String, Bitmap>(MEMORY_CACHE_SIZE);
		// init DiskCache
		File cacheDir = new File(mContext.getCacheDir(), DISK_CACHE_SUBDIR);
		mDiskCache = DiskLruCache
				.openCache(mContext, cacheDir, DISK_CACHE_SIZE);
	}

	public static synchronized BitmapUtil getInstance(Context context) {
		if (null == instance) {
			instance = new BitmapUtil(context);
		}
		return instance;
	}

	/**
	 * 得到指定大小的 bitmap
	 * @param data
	 * @param width
	 * @param height
	 * @return
	 */
	public Bitmap getBitmap(byte[] data, int width, int height) {
		Bitmap bitmap = null;
		Options opts = new Options();
		opts.inJustDecodeBounds = true;
		opts.inSampleSize = calculateInSampleSize(opts, width, height);
		opts.inJustDecodeBounds = false;
		bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
		return bitmap;
	}

	/**
	 * 计算缩放比例
	 * @param options
	 * @param reqWidth
	 * 目标宽
	 * @param reqHeight
	 * 目标高
	 * @return
	 */
	private int calculateInSampleSize(Options options,
			int reqWidth, int reqHeight) {

		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		if (reqWidth == 0 || reqHeight == 0) {
			return inSampleSize;
		}
		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}

		Log.d("", "原图尺寸：" + width + "x" + height + ",实际尺寸：" + reqWidth + "x"
				+ reqHeight + ",inSampleSize = " + inSampleSize);
		return inSampleSize;
	}

	/**
	 * 直接从网络 获取 图片 不缓存
	 * todo : 还未测试
	 * @param url
	 * @return
	 * @throws org.apache.http.conn.ConnectTimeoutException
	 * @throws java.io.IOException
	 */

	public static Bitmap getBitmap(String url) {
		Bitmap bitmap = null;
//		byte[] data = HttpUtils.getBinary(url, null, null);
        try {
            URL mUrl =new URL(url);
            byte[] data = HttpClient.connect(mUrl, HttpClient.HTTP_POST,
                    null,
                    30000,
                    10000);
            if (data != null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

		return bitmap;
	}

	/**
	 * 该方法为静态方法，从网络下载图片，如果硬盘有数据 ，优先从硬盘缓存获取<br>
	 * 并加入到内存缓存和 硬盘缓存<br>
	 * 存在网络下载，只能运行在工作线程<br>
	 * 
	 * @param context
	 * @param url
	 * @param isLurCache
	 *            是否加入到内存缓存，后台更新的时候不需要加入到内存缓存
	 * @return
	 */
	public static Bitmap getBitmap(Context context, String url,
			boolean isLurCache) {
		Bitmap bitmap = null;
		getInstance(context);
		bitmap = instance.getBitmapFromDisk(url);
		if (bitmap != null) {
			Log.d(TAG, "sp:from disk bitmap" + bitmap);
			instance.addToCache(url, bitmap, isLurCache, true);
			return bitmap;
		}
		bitmap = instance.getBitmapFromNet(url, 0, 0);
		if (bitmap != null) {
			instance.addToCache(url, bitmap, isLurCache, true);
		}
		return bitmap;
	}

	public Bitmap getBitmapFromMemory(String url) {
		String key = MD5Util.getMD5String(url);
		Bitmap bitmap = memoryCache.get(key);
		return bitmap;
	}

	public Bitmap getBitmapFromDisk(String url) {

		if (url != null) {
			String key = MD5Util.getMD5String(url).substring(8, 17) + ".png";
			// Log.d("info", "tvRcommendKey="+key);
			return mDiskCache.get(key);
		} else {

			return null;
		}

	}

	public Bitmap getBitmapFromNet(String url, int width, int height) {
		Bitmap bitmap = null;
		DefaultHttpClient client = new DefaultHttpClient();
		client.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT,
				60000);
		client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(5,
				false));
		HttpGet get = new HttpGet();
		int retryCount = 0;
		try {
			get.setURI(new URI(url));
			HttpResponse response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {
				bitmap = BitmapFactory.decodeStream(response.getEntity()
						.getContent());
			}
			if (bitmap == null) {
				Log.e(TAG, "getBitmapFromNet 下载失败 " + retryCount + " 次, url = "
						+ url);
			}
		} catch (Exception e) {
		} finally {
			get.abort();
			client.getConnectionManager().shutdown();
		}
		return bitmap;
	}

	public void addToCache(String url, Bitmap bitmap, boolean lruCache,
			boolean diskLruCache) {
		if (url == null || bitmap == null) {
			return;
		}
		String key = MD5Util.getMD5String(url);
		if (lruCache) {
			memoryCache.put(key, bitmap);
		}
		if (diskLruCache) {
			mDiskCache.put(key + ".png", bitmap);
		}
	}
}

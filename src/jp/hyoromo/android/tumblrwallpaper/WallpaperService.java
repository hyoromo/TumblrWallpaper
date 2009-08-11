package jp.hyoromo.android.tumblrwallpaper;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;

/**
 * 画像情報を取得して壁紙に設定するサービス。
 * @author hyoromo
 *
 */
public class WallpaperService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart (Intent intent, int startId) {
		try {
			// 画像パス指定して背景に設定
			Bitmap bmp = BitmapFactory.decodeFile("/sdcard/DCIM/Camera/test2.jpg");
			setWallpaper(bmp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

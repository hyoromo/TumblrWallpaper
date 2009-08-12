package jp.hyoromo.android.tumblrwallpaper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;

/**
 * 画像情報を取得して壁紙に設定するサービス。
 * @author hyoromo
 */
public class WallpaperService extends Service {

	private static final String IMAGE_PATH = "PATH";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onStart (Intent intent, int startId) {
		try {
			// 指定URLより画像を取得
			String imagePath = "";
			Bundle extras = intent.getExtras();
			if (extras != null) {
				imagePath = extras.getString(IMAGE_PATH);
			} else {
				// デフォルトの壁紙設定
				clearWallpaper();
			}
			URL url = new URL(imagePath);
			InputStream is = url.openStream();
			Bitmap bmp = BitmapFactory.decodeStream(is);
			setWallpaper(bmp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

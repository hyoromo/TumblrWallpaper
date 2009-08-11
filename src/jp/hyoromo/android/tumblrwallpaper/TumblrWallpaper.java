package jp.hyoromo.android.tumblrwallpaper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Tumblr から画像情報を取得して、HOMEの壁紙として設定させるアプリ。
 * メイン処理は WallpaperService で行っている。
 * @author hyoromo
 *
 */
public class TumblrWallpaper extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// サービスをインテントで起動。
		Intent intent = new Intent(TumblrWallpaper.this, WallpaperService.class);
		startService(intent);
		// サービスを起動させたら Activity は終了させておく。
		finish();
	}
}
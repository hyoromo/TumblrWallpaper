package jp.hyoromo.android.tumblrwallpaper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

/**
 * Tumblr から画像情報を取得して、HOMEの壁紙として設定させるアプリ。
 * メイン処理は WallpaperService で行っている。
 * @author hyoromo
 */
public class TumblrWallpaper extends Activity implements OnClickListener {

	private static final int BUTTON_MAX = 3;
	private static final String IMAGE_PATH = "PATH";
	 
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// レイアウトの生成
		LinearLayout layout = new LinearLayout(this);
		layout.setBackgroundColor(R.color.background);
		layout.setOrientation(LinearLayout.VERTICAL);
		setContentView(layout);

		// 壁紙候補一覧をボタンで表示
		ImageButton []button = new ImageButton[BUTTON_MAX];
		for (int i = 0; i < button.length; i++) {
			// ボタン配置
			button[i] = new ImageButton(this);
			setDraw(button[i], i);
			button[i].setOnClickListener(this);
			button[i].setLayoutParams(new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT
			));
			layout.addView(button[i]);
		}
	}

	/**
	 * 画像情報設定
	 * @param v ImageButton
	 * @param i
	 */
	private void setDraw(View v, int i) {
		URL url;
		try {
			// サイズ指定は 250 又は 400 のどちらかを指定する。今は 250 を指定。
			// ダミーデータとして格納。URL を解析して 20 ピクチャー表示させる予定。
			if (i == 0) {
				url = new URL("http://7.media.tumblr.com/XQTdQMQu9r0xsc2gGaZXiNG2o1_400.jpg");
			} else if (i == 1) {
				url = new URL("http://13.media.tumblr.com/Jcnh0ZA0Fr0dhj76TKCgAVt4o1_400.jpg");				
			} else {
				url = new URL("http://3.media.tumblr.com/4HHYyjXOQr0iyz34DvvtV9gso1_400.png");
			}
			InputStream is = url.openStream();
			Drawable draw = DrawableContainer.createFromStream(is, "");
			v.setBackgroundDrawable(draw);
			// Tagにファイルパスを設定。本来の目的にそぐわない操作だとは思う。
			v.setTag(url.toExternalForm());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onClick(View v) {
		// サービスをインテントで起動。
		Intent intent = new Intent(TumblrWallpaper.this, WallpaperService.class);
		intent.putExtra(IMAGE_PATH, (String)v.getTag());
		startService(intent);
		// サービスを起動させたら Activity は終了させておく。
		finish();
	}
}
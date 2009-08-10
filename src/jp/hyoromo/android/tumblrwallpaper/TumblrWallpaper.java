package jp.hyoromo.android.tumblrwallpaper;

import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;

/**
 * Tumblr から画像情報を取得して、HOMEの壁紙として設定させるアプリです。
 * @author hyoromo
 *
 */
public class TumblrWallpaper extends Activity implements OnClickListener {
	static final String TAG = "TumblrWallpaper";
	private FrameLayout layout;
	private BroadcastReceiver wallpaperReceiver;
	private Button wallpaperChangerButton;
	private boolean wallpaperFlag;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // レイアウトの生成
        layout = new FrameLayout(this);
        layout.setBackgroundColor(R.color.background);
        setContentView(layout);
        
        // 壁紙変更ボタンを生成
        wallpaperChangerButton = new Button(this);
        wallpaperChangerButton.setText(R.string.wallpaperChangerButton_name);
        wallpaperChangerButton.setOnClickListener(this);
        wallpaperChangerButton.setLayoutParams(new FrameLayout.LayoutParams(
    			FrameLayout.LayoutParams.WRAP_CONTENT,
    			FrameLayout.LayoutParams.WRAP_CONTENT
    	));
        layout.addView(wallpaperChangerButton);
        
        // インテントレシーバの登録
        wallpaperReceiver = new WallpaperIntentReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        registerReceiver(wallpaperReceiver, filter);
        
        // デフォルト壁紙の指定
        wallpaperFlag = false;
        setDefaultWallpaper();
    }
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(wallpaperReceiver);
	}

	@Override
	public void onClick(View v) {
		if (v == wallpaperChangerButton) {
			// 壁紙変更
			try {
				Bitmap bmp = BitmapFactory.decodeFile("/sdcard/DCIM/Camera/test2.jpg");
				setWallpaper(bmp);
			} catch (IOException e) {
				
			}
		}
	}
	
	// レシーバ
	public class WallpaperIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// アプリを終了させる
			finish();
		}
	}
	
	// デフォルト壁紙の指定
	private void setDefaultWallpaper() {
		if (!wallpaperFlag) {
			Drawable wallpaper = peekWallpaper();
			if (wallpaper == null) {
				try {
					clearWallpaper();
				} catch (IOException e) {
					layout.setBackgroundDrawable(wallpaper);
				}
				wallpaperFlag = true;
			}
		}
	}
}
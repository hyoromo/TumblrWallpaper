package jp.hyoromo.android.tumblrwallpaper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URL;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * 画像情報を取得して壁紙に設定するサービス。
 * @author hyoromo
 */
public class WallpaperService extends Service {

    private static final String TAG = "TumblrWallpaper";
    private static final String IMAGE_PATH = "PATH";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart (Intent intent, int startId) {
        try {
            String imagePath = "";
            Bundle extras = intent.getExtras();
            if (extras != null) {
                imagePath = extras.getString(IMAGE_PATH);
            } else {
                // デフォルトの壁紙設定
                clearWallpaper();
            }
            // 指定URLより画像を取得
            URL url = new URL(imagePath);
            InputStream is = url.openStream();
            Bitmap bmp = BitmapFactory.decodeStream(is);

            // 端末の幅と高さ
            int hw = getWallpaperDesiredMinimumWidth();
            int hh = getWallpaperDesiredMinimumHeight();
            Log.d(TAG, "hardX:" + hw + "/hardY:" + hh);

            // スケール
            bmp = scaleBitmap(bmp, hw, hh);

            // 指定壁紙設定
            setWallpaper(bmp);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** スケールビットマップを取得します */
    public Bitmap scaleBitmap (Bitmap bmp, int width, int height) {
        Point point = saleSize(bmp, width, height);
        bmp = Bitmap.createScaledBitmap(bmp, point.x, point.y, true);
        return bmp;
    }

    /** スケールサイズを取得します */
    public Point saleSize(Bitmap bmp, int width, int height) {
        Point point = new Point(0, 0);
        int changeX = 0;
        int changeY = 0;

        // 画像の幅と高さ
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        Log.d(TAG, "bmpX:" + bmpWidth + "/bmpY:" + bmpHeight);

        changeX = width - bmpWidth;
        changeY = height - bmpHeight;
        BigDecimal scale = new BigDecimal("0");
        // 差がマイナスであることを最優先に確認します
        if (changeX < 0 || changeY < 0) {
            if (changeX < changeY) {
                scale = scaleData(bmpWidth, changeX);
            } else {
                scale = scaleData(bmpHeight, changeY);
            }
        } else {
            if (changeX < changeY) {
                scale = scaleData(bmpWidth, changeX);
            } else {
                scale = scaleData(bmpHeight, changeY);
            }
        }
        point.x = scale.multiply(new BigDecimal(Integer.toString(bmpWidth))).intValue();
        point.y = scale.multiply(new BigDecimal(Integer.toString(bmpHeight))).intValue();
        Log.d(TAG, "scaleX:" + point.x + "/scaleY:" + point.y);

        return point;
    }
    
    /** スケール値を取得します */
    public BigDecimal scaleData (int bmpSize, int changeSize) {
        BigDecimal afterSize = new BigDecimal(bmpSize + changeSize);
        BigDecimal scale = afterSize.divide(new BigDecimal(Integer.toString(bmpSize)), 4, BigDecimal.ROUND_CEILING);
        Log.d(TAG, "scale:" + scale);

        return scale;
    }
}

package jp.hyoromo.android.tumblrwallpaper;

import java.math.BigDecimal;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

/**
 * Bitmapを指定サイズにスケールします。
 * 
 * @author hyoromo
 */
public class ScaleBitmap {

    private static final String TAG = "ScaleBitmap";

    /**
     * スケールビットマップを取得します
     */
    public static Bitmap getScaleBitmap(Bitmap bmp, int width, int height) {
        if (bmp != null) {
            Point point = saleSize(bmp, width, height);
            bmp = Bitmap.createScaledBitmap(bmp, point.x, point.y, true);
        }
        return bmp;
    }

    /**
     * スケールサイズを取得します
     */
    private static Point saleSize(Bitmap bmp, int width, int height) {
        Point point = new Point(0, 0);
        int changeY = 0;

        // 画像の幅と高さ
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        Log.d(TAG, "bmpX:" + bmpWidth + "/bmpY:" + bmpHeight);

        changeY = height - bmpHeight;
        BigDecimal scale = new BigDecimal("0");
        // 差がマイナスであることを最優先に確認します
        scale = scaleData(bmpHeight, changeY);

        point.x = scale.multiply(new BigDecimal(Integer.toString(bmpWidth))).intValue();
        point.y = scale.multiply(new BigDecimal(Integer.toString(bmpHeight))).intValue();
        Log.d(TAG, "scaleX:" + point.x + "/scaleY:" + point.y);

        return point;
    }

    /**
     * スケール値を取得します
     */
    private static BigDecimal scaleData(int bmpSize, int changeSize) {
        BigDecimal afterSize = new BigDecimal(bmpSize + changeSize);
        BigDecimal scale = afterSize.divide(new BigDecimal(Integer.toString(bmpSize)), 4,
                BigDecimal.ROUND_CEILING);
        Log.d(TAG, "scale:" + scale);

        return scale;
    }
}
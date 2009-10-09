package jp.hyoromo.android.tumblrwallpaper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;

/**
 * Bitmapを指定サイズにスケールします。
 * 
 * @author hyoromo
 */
public class BitmapUtil {

    private static final String TAG = "ScaleBitmap";
    private static final int SLEEP_TIME = 250;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 100;

    /**
     * Bitmapを取得してきます
     * @param urlStr : 画像URL
     * @param count : 同一画像URLで何回再取得したかの回数
     * @param sleepTime : Bitmap取得前のスリープ時間。初回は0で、失敗してからスリープするようにする。
     * @return
     */
    public static Bitmap getBitmap(final String urlStr, int count, final int sleepTime, final ProgressDialog progressDialog) {
        Bitmap bmp = null;
        BitmapFactory.Options bm_opt;

        try {
            URL url = new URL(urlStr);
            InputStream is = new BufferedInputStream(url.openStream(), DEFAULT_BUFFER_SIZE);
            bm_opt = new BitmapFactory.Options();

            // 取得失敗時は少し待機してから再取得する
            Thread.sleep((SLEEP_TIME + sleepTime) * count);
            bmp = BitmapFactory.decodeStream(is, null, bm_opt);

            int dataSize = is.read();
            is.close();

            // 取得できなかった場合は再取得処理
            if (bmp == null && count < 4) {
                Log.v(TAG, Integer.valueOf(count) + ":" + urlStr);
                int time = 0;
                // 読み込み漏らしサイズ量でsleep時間を増やす
                if (dataSize > 200) {
                    time = SLEEP_TIME * 3;
                } else if (dataSize > 150) {
                    time = SLEEP_TIME * 2;
                } else if (dataSize > 100) {
                    time = SLEEP_TIME;
                }
                bmp = getBitmap(urlStr, ++count, time, null);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            String newStr = "";
            if (Pattern.compile(".+_500\\..+").matcher(urlStr).matches()) {
                newStr = urlStr.replaceAll("_500.", "_400.");
            } else if (Pattern.compile(".+_400\\..+").matcher(urlStr).matches()) {
                newStr = urlStr.replaceAll("_400.", "_250.");
            } else if (Pattern.compile(".+_250\\..+").matcher(urlStr).matches()) {
                newStr = urlStr.replaceAll("_250.", "_100.");
            } else {
                // 【未実装】壁紙貼り付け失敗ダイアログ表示
                e.printStackTrace();
            }
            bmp = getBitmap(newStr, count, 0, null);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (progressDialog != null) {
            setProgressBarCount(progressDialog);
        }

        return bmp;
    }

    private static void setProgressBarCount(ProgressDialog progressDialog) {
        progressDialog.incrementProgressBy(5);
    }

    /**
     * スケールビットマップを取得します
     * @param bmp : スケール対象bitmap
     * @param width : 画面の幅
     * @param height : 画面の高さ
     * @return
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
     * @param bmp : スケール対象bitmap
     * @param width : 画面の幅
     * @param height : 画面の高さ
     * @return
     */
    private static Point saleSize(Bitmap bmp, int width, int height) {
        Point point = new Point(0, 0);
        int changeY = 0;

        // 画像の幅と高さ
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();

        changeY = height - bmpHeight;
        BigDecimal scale = new BigDecimal("0");
        // 差がマイナスであることを最優先に確認します
        scale = scaleData(bmpHeight, changeY);

        point.x = scale.multiply(new BigDecimal(Integer.toString(bmpWidth))).intValue();
        point.y = scale.multiply(new BigDecimal(Integer.toString(bmpHeight))).intValue();

        return point;
    }

    /**
     * スケール値を取得します
     * @param bmpSize : 画像のサイズ(現在は高さを基準にしている)
     * @param changeSize : 画面のサイズ(今はｒｙ)
     * @return
     */
    private static BigDecimal scaleData(int bmpSize, int changeSize) {
        BigDecimal afterSize = new BigDecimal(bmpSize + changeSize);
        BigDecimal scale = afterSize.divide(new BigDecimal(Integer.toString(bmpSize)), 4,
                BigDecimal.ROUND_CEILING);

        return scale;
    }
}
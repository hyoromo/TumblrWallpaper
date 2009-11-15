package jp.hyoromo.android.tumblrwallpaper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

/**
 * Bitmapを指定サイズにスケールします。
 * 
 * @author hyoromo
 */
public final class BitmapUtil {

    private static final String TAG = "ScaleBitmap";

    /**
     * Bitmapを取得してきます
     * @param urlStr : 画像URL
     * @return
     */
    public static final Bitmap getBitmap(final String urlStr, final ProgressDialog progressDialog) {
        Bitmap bmp = null;
        byte[] line = new byte[1024];
        int byteSize = 0;

        try {
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();
            InputStream is = con.getInputStream();
            
            // バイト単位での読込
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((byteSize = is.read(line)) > 0) {
                out.write(line, 0, byteSize);
            }

            byte[] byteArray = out.toByteArray();
            bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            is.close();
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
            bmp = getBitmap(newStr, null);
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
    public static final Bitmap getScaleBitmap(Bitmap bmp, int width, int height) {
        if (bmp != null) {
            Point point = saleSize(bmp, width, height);
            bmp = Bitmap.createScaledBitmap(bmp, point.x, point.y, true);
            /* 2.0対応するための対処法
            Bitmap backBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            // bmp = backBmp.createBitmap(bmp, 0, 0, point.x, point.y);
            Canvas canvas = new Canvas(backBmp);
            canvas.drawBitmap(bmp, 0, 0, null);
            BitmapDrawable drawable = new BitmapDrawable(backBmp);
            drawable.draw(canvas);
            bmp = drawable.getBitmap();
            // drawable.draw(canvas);
             */
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

        // 画像の幅と高さ
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();

        int changeX = width - bmpWidth;
        int changeY = height - bmpHeight;
        BigDecimal scale = new BigDecimal("0");
        // 差がマイナスであることを最優先に確認します
        scale = scaleData(bmpHeight, changeY);
        // scale = scaleData(bmpWidth, changeX);

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
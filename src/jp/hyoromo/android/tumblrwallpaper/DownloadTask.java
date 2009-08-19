package jp.hyoromo.android.tumblrwallpaper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;

/**
 * 非同期でWebから画像情報を取得してボタンに貼り付けます。
 * @author hyoromo
 */
public class DownloadTask extends AsyncTask<String, Integer, Drawable> {

    private View view;

    public DownloadTask(View v) {
        view = v;
    }
    
    public Drawable downloadImage(String uri) {
        URL url = null;
        try {
            url = new URL(uri);
            InputStream is = url.openStream();
            Drawable draw = Drawable.createFromStream(is, "");
            view.setTag(url.toExternalForm());
            is.close();
            return draw;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 独自スレッドでバックグラウンド処理
     * @param result
     */
    protected Drawable doInBackground(String... uri) {
        return downloadImage(uri[0]);
    }
    
    /**
     * 画面描画できるmainスレッドで実行したい処理
     */
    protected void onPostExecute(Drawable draw) {
        view.setBackgroundDrawable(draw);
    }
}
package jp.hyoromo.android.tumblrwallpaper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.View;

/**
 * 非同期でWebから画像情報を取得してボタンに貼り付けます。
 * 
 * @author hyoromo
 */
public class DownloadTask extends AsyncTask<String, Integer, Drawable> {

    private View mView;
    private Handler mHandler;

    public DownloadTask(View view, Handler handler) {
        mView = view;
        mHandler = handler;
    }

    public Drawable downloadImage(String uri) {
        URL url = null;
        final View view = mView;
        try {
            url = new URL(uri);
            InputStream is = url.openStream();
            final Drawable draw = Drawable.createFromStream(is, "");
            view.setTag(url.toExternalForm());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    view.setBackgroundDrawable(draw);
                }
            });
            is.close();
            // return draw;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 独自スレッドでバックグラウンド処理
     */
    protected Drawable doInBackground(String... uri) {
        return downloadImage(uri[0]);
    }

    /**
     * 画面描画できるmainスレッドで実行したい処理
     */
    protected void onPostExecute(Drawable draw) {
    }
}
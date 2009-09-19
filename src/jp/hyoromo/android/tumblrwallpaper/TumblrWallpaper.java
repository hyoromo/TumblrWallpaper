package jp.hyoromo.android.tumblrwallpaper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * Tumblr から画像情報を取得して、HOMEの壁紙として設定させるアプリ。 メイン処理は WallpaperService で行っている。
 * 
 * @author hyoromo
 */
public class TumblrWallpaper extends ListActivity {

    private static final String TAG = "TumblrWallpaper";
    private static final int BUTTON_MAX = 5;
    private static final int SLEEP_TIME = 250;
    private static ProgressDialog mProgressDialog;
    private static Context mContext;
    private static Activity mActivity;

    // private static Activity mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // ログ収集開始
        // Debug.startMethodTracing();
        setContentView(R.layout.main);

        // 初期設定
        mContext = getApplicationContext();
        mActivity = this;
        setNameDialog().show();

        // ログ収集終了
        // Debug.stopMethodTracing();
    }

    /**
     * 取得URL設定ダイアログ
     */
    private Dialog setNameDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View entryView = factory.inflate(R.layout.dialog_entry, null);
        final EditText edit = (EditText) entryView.findViewById(R.id.username_edit);
        edit.setText(getPreferences("name", ""));

        return new AlertDialog.Builder(this)
            .setIcon(R.drawable.icon)
            .setTitle(R.string.load_alert_name_dialog_title)
            .setView(entryView).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String mes1 = getResources().getString(R.string.load_progress_dialog_mes1);
                        String mes2 = getResources().getString(R.string.load_progress_dialog_mes2);
                        showPrrogressDialog(mes1, mes2);
                        String editStr = edit.getText().toString();
                        String url = "http://" + editStr + ".tumblr.com/";
                        new ImageTask().execute(url);
                        setPreferences("name", editStr);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        }).create();
    }

    /**
     * プリファレンス情報設定
     */
    private void setPreferences(String keyname, String key) {
        SharedPreferences settings = getSharedPreferences("TumblrWallpaper", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(keyname, key);
        editor.commit();
    }

    /**
     * プリファレンス情報取得
     */
    private String getPreferences(String keyname, String key) {
        SharedPreferences settings = getSharedPreferences("TumblrWallpaper", MODE_PRIVATE);
        return settings.getString(keyname, key);
    }

    /*
     * Load時のプログレスダイアログ表示
     */
    private void showPrrogressDialog(String mes1, String mes2) {
        mProgressDialog = ProgressDialog.show(mActivity, mes1, mes2, true);
    }

    /**
     * 非同期で画像データ取得し、同期を取って画面上に表示させる
     */
    class ImageTask extends AsyncTask<String, Void, IconicAdapter> {
        @Override
        protected IconicAdapter doInBackground(String... params) {
            // 画像URLを取得
            final String[] imageStr = getImage(params[0]);
            final IconicAdapter adapter = new IconicAdapter(imageStr);

            return adapter;
        }

        @Override
        protected void onPostExecute(IconicAdapter adapter) {
            setListAdapter(adapter);
            mProgressDialog.dismiss();
        }
    }

    /**
     * Tumblrから画像取得
     */
    private String[] getImage(String tumblrUrl) {
        String[] imageStr = new String[BUTTON_MAX];
        try {
            URL url = new URL(tumblrUrl);
            HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
            Pattern p = Pattern
                    .compile("http\\:\\/\\/\\d+.media\\.tumblr\\.com\\/(?!avatar_)[\\-_\\.\\!\\~\\*\\'\\(\\)a-zA-Z0-9\\;\\/\\?\\:@&=\\$\\,\\%\\#]+\\.(jpg|jpeg|png|gif|bmp)");
            urlCon.setRequestMethod("GET");
            BufferedReader urlIn = new BufferedReader(
                    new InputStreamReader(urlCon.getInputStream()));
            String str;
            int count = 0;
            while ((str = urlIn.readLine()) != null && count < BUTTON_MAX) {
                Matcher m = p.matcher(str);
                if (m.find()) {
                    imageStr[count] = m.group().replaceAll("400", "250");
                    Log.d(TAG, imageStr[count]);
                    count++;
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageStr;
    }

    /**
     * ArrayAdapter を拡張したクラス。 画像を一覧表示させている。
     */
    public class IconicAdapter extends ArrayAdapter<Object> {
        String[] mItems;
        private final Bitmap[] mBitmap;

        IconicAdapter(String[] items) {
            super(mContext, R.layout.row, items);
            mItems = items;
            mBitmap = new Bitmap[BUTTON_MAX];

            for (int i = 0; i < BUTTON_MAX; i++) {
                mBitmap[i] = getBitmap(mItems[i], 0, 0);
            }
        }

        /**
         * 画面に表示される毎に呼び出される
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                row = inflater.inflate(R.layout.row, null);
                holder = new ViewHolder();

                // 画像イメージを作成
                holder.img = (ImageView) row.findViewById(R.id.image);

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            holder.img.setImageBitmap(mBitmap[position]);
            holder.img.setTag(mItems[position]);

            return row;
        }

        /*
         * 10件目以降は非同期で取得してくる予定 // 画像をダウンロードして表示する private void downloadAndUpdateImage(int position,
         * ImageButton v) { DownloadTask task = new DownloadTask(v, mHandler);
         * task.execute(mItems[position]); }
         */
    }

    static class ViewHolder {
        ImageView img;
    }

    private Bitmap getBitmap(final String urlStr, int count, int sleepTime) {
        Bitmap bmp = null;
        try {
            int DEFAULT_BUFFER_SIZE = 1024 * 100;
            URL url = new URL(urlStr);
            InputStream is = new BufferedInputStream(url.openStream(), DEFAULT_BUFFER_SIZE);

            // 取得失敗時は少し待機してから再取得する
            Thread.sleep((SLEEP_TIME + sleepTime) * count);
            bmp = BitmapFactory.decodeStream(is);

            int dataSize = is.read();
            Log.v(TAG, urlStr + " : " + Integer.toString(dataSize) + " : " + bmp);
            is.close();

            // 取得できなかった場合は再取得処理
            if (bmp == null && count < 4) {
                int time = 0;
                // 読み込み漏らしサイズ量でsleep時間を増やす
                if (dataSize > 200) {
                    time = SLEEP_TIME * 3;
                } else if (dataSize > 150) {
                    time = SLEEP_TIME * 2;
                } else if (dataSize > 100) {
                    time = SLEEP_TIME;
                }
                bmp = getBitmap(urlStr, ++count, time);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return bmp;
    }

    /**
     * Listがクリックされたら選択画像を壁紙設定する
     */
    public void onListItemClick(ListView parent, View v, int position, long id) {
        // 端末の幅と高さ
        final int hw = getWallpaperDesiredMinimumWidth();
        final int hh = getWallpaperDesiredMinimumHeight();
        Log.d(TAG, "bmpX:" + hw + "/bmpY:" + hh);

        // 画像を取得してスケール
        ViewHolder holder = (ViewHolder) v.getTag();
        final String str = ((String) holder.img.getTag()).replaceAll("250", "500");

        /*
         * 250px size の画像を引き延ばすと汚いのでやめた BitmapDrawable draw = (BitmapDrawable)
         * holder.img.getDrawable(); final Bitmap bmp = ScaleBitmap.getScaleBitmap(draw.getBitmap(),
         * hw, hh);
         */

        String mes1 = getResources().getString(R.string.wallpaper_progress_dialog_mes1);
        String mes2 = getResources().getString(R.string.wallpaper_progress_dialog_mes2);
        showPrrogressDialog(mes1, mes2);

        // 壁紙設定
        new Thread(new Runnable() {
            public void run() {
                try {
                    final Bitmap bmp = ScaleBitmap.getScaleBitmap(getBitmap(str, 0, 0), hw, hh);
                    setWallpaper(bmp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mProgressDialog.dismiss();
                // 壁紙設定後に Activity を終了させる。
                finish();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");

        mActivity = null;
        mContext = null;
        mProgressDialog = null;
    }
}
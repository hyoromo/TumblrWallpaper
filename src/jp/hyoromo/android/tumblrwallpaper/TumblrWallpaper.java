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
    private static final int BUTTON_MAX = 10;
    private static final int LIST_MAX = 10;
    private static final int PAGE_MAX = 5;
    private static final int SLEEP_TIME = 250;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 100;
    private static ProgressDialog mProgressDialog;
    private static Context mContext;
    private static Activity mActivity;
    private static ListData []mListData;
    int titleId;
    int mesId;

    // private static Activity mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        // 初期設定
        mContext = getApplicationContext();
        mActivity = this;
        mListData = new ListData[LIST_MAX];

        showAccountNameDialog().show();
    }

    /**
     * アカウント入力ダイアログ表示
     */
    private Dialog showAccountNameDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View entryView = factory.inflate(R.layout.dialog_entry, null);
        final EditText edit = (EditText) entryView.findViewById(R.id.username_edit);

        // アカウント名がプリファレンスにあればエディタに設定
        edit.setText(getPreferences("name", ""));

        return new AlertDialog.Builder(this).setIcon(R.drawable.icon).setTitle(R.string.load_alert_name_dialog_title)
                .setView(entryView).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // ロード中ダイアログ表示
                        String mes1 = getResources().getString(R.string.load_progress_dialog_mes1);
                        String mes2 = getResources().getString(R.string.load_progress_dialog_mes2);
                        showPrrogressDialog(mes1, mes2);

                        // 非同期で画像取得
                        String editStr = edit.getText().toString();
                        String url = "http://" + editStr + ".tumblr.com/page/";
                        new ImageTask().execute(url);

                        // アカウント名をプリファレンスに保存
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

    /**
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
            IconicAdapter adapter = null;
            try {
                setListData(params[0], 1);
                adapter = new IconicAdapter();
            } catch (IOException e) {
                titleId = R.string.err1_alert_dialog_title;
                mesId = R.string.err1_alert_dialog_mes;
            } catch (RuntimeException e) {
                titleId = R.string.err2_alert_dialog_title;
                mesId = R.string.err2_alert_dialog_mes;
            }
            return adapter;
        }

        @Override
        protected void onPostExecute(IconicAdapter adapter) {
            mProgressDialog.dismiss();
            if (adapter != null) {
                setListAdapter(adapter);
            } else {
                showAlertDialog().show();
            }
        }
    }

    /**
     * Tumblrからの情報を設定
     */
    private void setListData(String tumblrUrl, int page) throws IOException, RuntimeException {
        int count = 0;
        DownloadBitmapThread []mBitmapThreads = new DownloadBitmapThread[BUTTON_MAX];
        while (count < BUTTON_MAX && page < PAGE_MAX) {
            try {
                URL url = new URL(tumblrUrl + Integer.toString(page++));
                HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
                Pattern p = Pattern
                        .compile("http\\:\\/\\/\\d+.media\\.tumblr\\.com\\/(?!avatar_)[\\-_\\.\\!\\~\\*\\'\\(\\)a-zA-Z0-9\\;\\/\\?\\:@&=\\$\\,\\%\\#]+\\.(jpg|jpeg|png|gif|bmp)");
                urlCon.setRequestMethod("GET");
                BufferedReader urlIn = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));

                String str;
                while ((str = urlIn.readLine()) != null && count < BUTTON_MAX) {
                    Matcher m = p.matcher(str);
                    if (m.find()) {
                        // 画像URL取得
                        mListData[count] = new ListData();
                        mListData[count].position = count;
                        String urlStr = "";
                        urlStr = m.group().replaceAll("_500.", "_250.");
                        urlStr = m.group().replaceAll("_400.", "_250.");
                        mListData[count].url = urlStr;

                        // Bitmap情報を別スレッドで取得
                        mBitmapThreads[count] = new DownloadBitmapThread(count, 0);
                        mBitmapThreads[count].start();

                        count++;
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        // Bitmap取得スレッドが全て終わるまで待機
        for (int threadCount = 0; threadCount < BUTTON_MAX; threadCount++) {
            if (mBitmapThreads[threadCount] != null) {
                try {
                    mBitmapThreads[threadCount].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBitmapThreads[threadCount] = null;
            }
        }
    }

    /**
     * 警告ダイアログを表示
     */
    public AlertDialog showAlertDialog() {
        return new AlertDialog.Builder(this)
        .setIcon(R.drawable.alert_dialog_icon)
        .setTitle(titleId)
        .setMessage(mesId)
        .setPositiveButton("了解", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                showAccountNameDialog().show();
            }
        })
        .create();
    }

    public class DownloadBitmapThread extends Thread {
        private final int mCount;
        private final int mSleepTime;

        DownloadBitmapThread(int count, int sleepTime) {
            mCount = count;
            mSleepTime = sleepTime;
        }

        public void run() {
            mListData[mCount].bitmap = getBitmap(mListData[mCount].url, mCount, mSleepTime);
        }
    }

    private Bitmap getBitmap(final String urlStr, int count, int sleepTime) {
        Bitmap bmp = null;
        try {
            URL url = new URL(urlStr);
            InputStream is = new BufferedInputStream(url.openStream(), DEFAULT_BUFFER_SIZE);

            // 取得失敗時は少し待機してから再取得する
            Thread.sleep((SLEEP_TIME + sleepTime) * count);
            bmp = BitmapFactory.decodeStream(is);

            int dataSize = is.read();
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
            bmp = getBitmap(newStr, count, 0);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return bmp;
    }

    /**
     * ArrayAdapter を拡張したクラス。 画像を一覧表示させている。
     */
    public class IconicAdapter extends ArrayAdapter<Object> {

        IconicAdapter() {
            super(mContext, R.layout.row, mListData);
        }

        /**
         * 画面に表示される毎に呼び出される
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            if (mListData[position] != null) {
                // 画像イメージを作成
                if (row == null || row.getTag() == null) {
                    LayoutInflater inflater = LayoutInflater.from(mContext);
                    row = inflater.inflate(R.layout.row, null);

                    mListData[position].img = (ImageView) row.findViewById(R.id.image);
                    row.setTag(mListData[position].img);
                } else {
                    // 画像イメージを読み込み
                    mListData[position].img = (ImageView) row.getTag();
                }

                if (mListData[position] != null && mListData[position].img != null) {
                    mListData[position].img.setImageBitmap(mListData[position].bitmap);
                    mListData[position].img.setTag(mListData[position].url);
                } else {
                    mListData[position].img.setImageBitmap(null);
                    mListData[position].img.setTag("");
                }
            } else {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                row = inflater.inflate(R.layout.row, null);
            }
            return row;
        }

        /*
         * 10件目以降は非同期で取得してくる予定 // 画像をダウンロードして表示する private void downloadAndUpdateImage(int position, ImageButton v) {
         * DownloadTask task = new DownloadTask(v, mHandler); task.execute(mItems[position]); }
         */
    }

    /**
     * Listがクリックされたら選択画像を壁紙設定する
     */
    public void onListItemClick(ListView parent, View v, int position, long id) {
        // 端末の幅と高さ
        final int hw = getWallpaperDesiredMinimumWidth();
        final int hh = getWallpaperDesiredMinimumHeight();

        // 画像を取得してスケール
        final String str = mListData[position].url.replaceAll("_250.", "_500.");

        String mes1 = getResources().getString(R.string.wallpaper_progress_dialog_mes1);
        String mes2 = getResources().getString(R.string.wallpaper_progress_dialog_mes2);
        showPrrogressDialog(mes1, mes2);

        // 壁紙設定
        new Thread(new Runnable() {
            public void run() {
                try {
                    final Bitmap bmp = ScaleBitmap.getScaleBitmap(getBitmap(str, 0, 0), hw, hh);
                    if (bmp != null) {
                        setWallpaper(bmp);
                    } else {
                        // Bitmap取得に失敗したときはデフォルト壁紙を設定
                        clearWallpaper();
                    }
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

        mListData = null;
        mActivity = null;
        mContext = null;
        mProgressDialog = null;
    }
}
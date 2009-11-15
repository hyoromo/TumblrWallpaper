package jp.hyoromo.android.tumblrwallpaper;

import java.io.BufferedReader;
import java.io.IOException;
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
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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
    private static final int MAX_PROGRESS = 100;
    private static ProgressDialog mProgressDialog;
    private static Dialog mDialog;
    private static Context mContext;
    private static Activity mActivity;
    private static ListData []mListData;
    private static AsyncTask mAsyncTask;
    private static DownloadBitmapThread []mBitmapThreads;
    private static Thread mBitmapScaleThread;
    private static Bitmap mWallpaperBitmap;
    private static int titleId;
    private static int mesId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        // 初期設定
        mContext = getApplicationContext();
        mActivity = this;

        String reloadCheck = String.valueOf(getPreferences("reload", "false"));
        // 初期ダイアログ表示
        if ("false".equals(reloadCheck)) {
            mDialog = showAccountNameDialog();
            mDialog.show();
        }
        // 初期ダイアログをスキップ
        else {
            String accountName = getPreferences("name", "");
            loadThreadStart(accountName, reloadCheck);
        }
    }

    /**
     * アカウント入力ダイアログ表示
     * @return
     */
    private Dialog showAccountNameDialog() {
        LayoutInflater factory = LayoutInflater.from(mContext);
        final View entryView = factory.inflate(R.layout.dialog_entry, null);
        final EditText edit = (EditText) entryView.findViewById(R.id.username_edit);
        final CheckBox check = (CheckBox) entryView.findViewById(R.id.reaccount_use);

        // アカウント名がプリファレンスにあればエディタに設定
        edit.setText(getPreferences("name", ""));
        check.setChecked(Boolean.valueOf(getPreferences("reload", "true")));

        // キーハンドリング
        edit.setOnKeyListener(new View.OnKeyListener(){
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                 // Enterキーハンドリング
                if (KeyEvent.KEYCODE_ENTER == keyCode) {
                    // 押したときに改行を挿入防止処理
                    if (KeyEvent.ACTION_DOWN == event.getAction()) {
                        return true;
                    }
                     // 離したときにダイアログ上の[読込]処理を実行
                    else if (KeyEvent.ACTION_UP == event.getAction()) {
                        if (edit != null && edit.length() != 0) {
                            // 非同期で画像取得
                            String accountName = edit.getText().toString();
                            String reloadCheck = String.valueOf(check.isChecked());
                            loadThreadStart(accountName, reloadCheck);
                            mDialog.dismiss();
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        // 初期ダイアログ作成
        return new AlertDialog.Builder(mActivity)
            .setIcon(R.drawable.icon)
            .setTitle(R.string.load_alert_name_dialog_title)
            .setView(entryView)
            .setCancelable(false)
            .setPositiveButton(R.string.load_alert_name_dialog_button1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // 非同期で画像取得
                    String accountName = edit.getText().toString();
                    String reloadCheck = String.valueOf(check.isChecked());
                    loadThreadStart(accountName, reloadCheck);
                }
            }).setNegativeButton(R.string.load_alert_name_dialog_button2, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                }
            }).create();
    }

    /**
     * プリファレンス情報設定
     * @param key
     * @param value
     */
    private void setPreferences(String key, String value) {
        SharedPreferences settings = getSharedPreferences("TumblrWallpaper", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * プリファレンス情報取得
     * @param key
     * @param value : プリファレンスから取得失敗した時に設定される値
     * @return
     */
    private String getPreferences(String key, String value) {
        SharedPreferences settings = getSharedPreferences("TumblrWallpaper", MODE_PRIVATE);
        return settings.getString(key, value);
    }

    /**
     * Load時のスレッド開始処理
     * @param accountName : アカウント名
     * @param reloadCheck : 「次回から自動読み込み」フラグ
     */
    private void loadThreadStart(String accountName, String reloadCheck) {
        // 非同期で画像取得
        String url = "http://" + accountName + ".tumblr.com/page/";
        mAsyncTask = new ImageTask().execute(url);

        setAccountInfo(accountName, reloadCheck);
    }

    /**
     * アカウント情報をプリファレンスに保存
     * @param accountName : アカウント名
     * @param reloadCheck : 「次回から自動読み込み」フラグ
     */
    private void setAccountInfo(String accountName, String reloadCheck) {
        setPreferences("name", accountName);
        setPreferences("reload", reloadCheck);
    }

    /**
     * 非同期で画像データ取得し、同期を取って画面上に表示させる
     * @author hyoromo
     */
    private class ImageTask extends AsyncTask<String, Void, IconicAdapter> {
        @Override
        protected void onPreExecute() {
            // ロード中はプログレスバーダイアログ表示
            showPrrogressBarDialog();
        }

        @Override
        protected final IconicAdapter doInBackground(String... params) {
            mListData = new ListData[LIST_MAX];
            IconicAdapter adapter = null;
            try {
                setListData(params[0], 1);
                mProgressDialog.incrementProgressBy(5);
                adapter = new IconicAdapter();
                mProgressDialog.incrementProgressBy(5);
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
        protected final void onPostExecute(IconicAdapter adapter) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (adapter != null) {
                setListAdapter(adapter);
            }
            // 画像取得に失敗
            else {
                showAlertDialog().show();
            }
            mAsyncTask = null;
        }
    }

    /**
     * プログレスバーダイアログ表示
     * @param title
     * @param mes
     */
    private void showPrrogressBarDialog() {
        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle(R.string.load_progress_bar_dialog_title);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(MAX_PROGRESS);
        // 初期ダイアログ表示
        mProgressDialog.setButton(getText(R.string.load_progress_bar_dialog_button1), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                clearThreadData();
                mProgressDialog.dismiss();
                mDialog = showAccountNameDialog();
                mDialog.show();
            }
        });
        // アプリ終了
        mProgressDialog.setButton2(getText(R.string.load_progress_bar_dialog_button2), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        });
        mProgressDialog.show();

        // バックボタンが押されたとき
        mProgressDialog.setOnCancelListener(new OnCancelListener(){
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
       });
    }

    /**
     * プログレスダイアログ表示
     * @param title
     * @param mes
     */
    private void showPrrogressDialog(String title, String mes) {
        mProgressDialog = ProgressDialog.show(mActivity, title, mes, true);
    }

    /**
     * Tumblrからの情報を設定
     * @param tumblrUrl
     * @param page : URLのページ数。最初は1から。
     * @throws IOException
     * @throws RuntimeException
     */
    private void setListData(String tumblrUrl, int page) throws IOException, RuntimeException {
        int count = 0;
        int barCountDown = 40;
        mBitmapThreads = new DownloadBitmapThread[BUTTON_MAX];
        while (count < BUTTON_MAX && page < PAGE_MAX) {
            try {
                URL url = new URL(tumblrUrl + Integer.toString(page++));
                HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
                Pattern p = Pattern
                        .compile("http\\:\\/\\/\\d+.media\\.tumblr\\.com\\/(?!avatar_)[\\-_\\.\\!\\~\\*\\'\\(\\)a-zA-Z0-9\\;\\/\\?\\:@&=\\$\\,\\%\\#]+\\.(jpg|jpeg|png|gif|bmp)");
                urlCon.setRequestMethod("GET");
                BufferedReader urlIn = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
                if (barCountDown > 10) {
                    mProgressDialog.incrementProgressBy(10);
                    barCountDown -= 10;
                }

                String str;
                while ((str = urlIn.readLine()) != null && count < BUTTON_MAX) {
                    Matcher m = p.matcher(str);
                    if (m.find()) {
                        // 画像URL取得
                        mListData[count] = new ListData();
                        mListData[count].position = count;
                        String urlStr = "";
                        // 一覧表示サイズを250pxに合わせる
                        urlStr = m.group().replaceAll("_500.", "_250.");
                        urlStr = m.group().replaceAll("_400.", "_250.");
                        mListData[count].url = urlStr;

                        // Bitmap情報を別スレッドで取得
                        mBitmapThreads[count] = new DownloadBitmapThread(count, 0);
                        mBitmapThreads[count].start();

                        count++;
                    }
                    if (barCountDown != 0) {
                        mProgressDialog.incrementProgressBy(1);
                        barCountDown--;
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        // 一件も画像を取得できなかった場合はエラー
        if (mBitmapThreads[0] == null) {
            throw new RuntimeException();
        }

        // 残っているカウントダウン分をProgressBarに加算
        mProgressDialog.incrementProgressBy(barCountDown);

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
     * @return
     */
    private AlertDialog showAlertDialog() {
        return new AlertDialog.Builder(mActivity)
        .setIcon(R.drawable.alert_dialog_icon)
        .setTitle(titleId)
        .setMessage(mesId)
        .setPositiveButton(R.string.err_alert_dialog_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mDialog = showAccountNameDialog();
                mDialog.show();
            }
        })
        .create();
    }

    /**
     * 画像取得するためのスレッド
     * @author hyoromo
     */
    private class DownloadBitmapThread extends Thread {
        private final int mCount;

        DownloadBitmapThread(int count, int sleepTime) {
            mCount = count;
        }

        public void run() {
            try {
                mListData[mCount].bitmap = BitmapUtil.getBitmap(mListData[mCount].url, mProgressDialog);
            } catch (NullPointerException e) {
                // スレッド異常なため処理をスローさせる
            }
        }
    }

    /**
     * ArrayAdapterを拡張したクラス。 画像を一覧表示させている。
     * @author hyoromo
     */
    private class IconicAdapter extends ArrayAdapter<Object> {

        IconicAdapter() {
            super(mContext, R.layout.row, mListData);
        }

        /**
         * 画面に表示される毎に呼び出される
         * @param position : 表示する対象Listの一覧を上から数えたときの番号
         * @param convertView : 表示する対象ListのView
         * @param parent : 知らん
         */
        final public View getView(int position, View convertView, ViewGroup parent) {
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
     * @param parent : 知らん
     * @param v : 選択されたListのView
     * @param position : 選択されたListの上から数えたときの番号
     * @param id : 知らん
     */
    public final void onListItemClick(ListView parent, View v, final int position, long id) {
        // 選択ダイアログが空の場合
        if (mListData[position] == null) {
            showNullRowSelDialog().show();
        } else {
            mBitmapScaleThread = new Thread(new Runnable() {
                public void run() {
                    // 端末の幅と高さ
                    int hw = getWallpaperDesiredMinimumWidth();
                    int hh = getWallpaperDesiredMinimumHeight();

                    // 画像を取得してスケール
                    String str = mListData[position].url.replaceAll("_250.", "_500.");
                    mWallpaperBitmap = BitmapUtil.getScaleBitmap(BitmapUtil.getBitmap(str, null), hw, hh);
                }
            });
            mBitmapScaleThread.start();

            showCheckWallpaperDialog().show();
        }
    }

    /**
     * 壁紙貼り付け確認アラートダイアログ
     * @return
     */
    private AlertDialog showCheckWallpaperDialog() {
        return new AlertDialog.Builder(mActivity)
        .setIcon(R.drawable.icon)
        .setTitle(R.string.check_wallpaper_dialog_title)
        .setMessage(R.string.check_wallpaper_dialog_mes)
        .setPositiveButton(R.string.check_wallpaper_dialog_button1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // 壁紙設定
                mAsyncTask = new Wallpaper().execute();
            }
        })
        .setNegativeButton(R.string.check_wallpaper_dialog_button2, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        })
        .create();
    }

    /**
     * 壁紙設定
     */
    private class Wallpaper extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            String mes1 = getResources().getString(R.string.wallpaper_progress_dialog_mes1);
            String mes2 = getResources().getString(R.string.wallpaper_progress_dialog_mes2);
            showPrrogressDialog(mes1, mes2);
        }

        @Override
        protected final Void doInBackground(String... params) {
            try {
                mBitmapScaleThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                if (mWallpaperBitmap != null) {
                    setWallpaper(mWallpaperBitmap);
                } else {
                    // Bitmap取得に失敗したときはデフォルト壁紙を設定
                    clearWallpaper();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected final void onPostExecute(Void parm) {
            mProgressDialog.dismiss();
            mAsyncTask = null;
            // 壁紙設定後に Activity を終了させる。
            finish();
        }
    }

    /**
     * 空リスト選択ダイアログを表示
     * @return
     */
    private AlertDialog showNullRowSelDialog() {
        return new AlertDialog.Builder(mActivity)
        .setIcon(R.drawable.alert_dialog_icon)
        .setTitle(R.string.null_row_sel_dialog_title)
        .setMessage(R.string.null_row_sel_dialog_mes)
        .setPositiveButton(R.string.err_alert_dialog_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        })
        .create();
    }

    /**
     * menuボタン作成
     */
    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * meny押下時のイベント
     */
    @Override
    public final boolean onOptionsItemSelected(MenuItem items) {
        switch (items.getItemId()) {
        case R.id.menu_reload_setting:
            mDialog = showReloadSettingDialog();
            mDialog.show();
            return true;
        }
        return false;
    }

    /**
     * アカウント入力ダイアログ表示
     * @return
     */
    private Dialog showReloadSettingDialog() {
        LayoutInflater factory = LayoutInflater.from(mContext);
        final View entryView = factory.inflate(R.layout.dialog_entry, null);
        final EditText edit = (EditText) entryView.findViewById(R.id.username_edit);
        final CheckBox check = (CheckBox) entryView.findViewById(R.id.reaccount_use);

        // アカウント名がプリファレンスにあればエディタに設定
        edit.setText(getPreferences("name", ""));
        check.setChecked(Boolean.valueOf(getPreferences("reload", "true")));

        // キーハンドリング
        edit.setOnKeyListener(new View.OnKeyListener(){
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                 // Enterキーハンドリング
                if (KeyEvent.KEYCODE_ENTER == keyCode) {
                    // 押したときに改行を挿入防止処理
                    if (KeyEvent.ACTION_DOWN == event.getAction()) {
                        return true;
                    }
                     // 離したときにダイアログ上の[再読込/再設定]処理を実行
                    else if (KeyEvent.ACTION_UP == event.getAction()) {
                        if (edit != null && edit.length() != 0) {
                            mDialog.dismiss();
                            String accountName = edit.getText().toString();
                            String reloadCheck = String.valueOf(check.isChecked());
                            loadThreadStart(accountName, reloadCheck);
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        // 設定ダイアログを作成
        return new AlertDialog.Builder(mActivity)
                .setIcon(R.drawable.icon)
                .setTitle(R.string.load_alert_name_dialog_title)
                .setView(entryView)
                .setPositiveButton(R.string.reload_setting_dialog_button1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String accountName = edit.getText().toString();
                        String reloadCheck = String.valueOf(check.isChecked());
                        loadThreadStart(accountName, reloadCheck);
                    }
                })
                .setNeutralButton(R.string.reload_setting_dialog_button2, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String accountName = edit.getText().toString();
                        String reloadCheck = String.valueOf(check.isChecked());
                        setAccountInfo(accountName, reloadCheck);
                    }
                })
                .setNegativeButton(R.string.reload_setting_dialog_button3, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // 処理なし
                    }
                })
                .create();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        clearThreadData();
        mActivity = null;
        mContext = null;
        mDialog = null;
        mProgressDialog = null;
    }

    /**
     * スレッド関係のクラス変数をクリア
     */
    private void clearThreadData() {
        mWallpaperBitmap = null;
        if (mBitmapThreads != null) {
            for (int i = 0; i < mBitmapThreads.length; i++) {
                if (mBitmapThreads[i] != null && mBitmapThreads[i].isAlive() == true) {
                    mBitmapThreads[i].interrupt();
                    mBitmapThreads[i] = null;
                }
            }
            mBitmapThreads = null;
        }
        mBitmapScaleThread = null;
        if (mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mAsyncTask.cancel(true);
        }
        mAsyncTask = null;
        mListData = null;
    }
}
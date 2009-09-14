package jp.hyoromo.android.tumblrwallpaper;

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
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
    private Handler mHandler = new Handler();
    private AsyncTask<?, ?, ?> mTask;
    private ProgressDialog mProgressDialog;
    private Activity mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // ログ収集開始
        // Debug.startMethodTracing();
        setContentView(R.layout.main);

        // 初期設定
        mContext = this;
        setNameDialog().show();

        // ログ収集終了
        // Debug.stopMethodTracing();
    }


    private Dialog setNameDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View entryView = factory.inflate(R.layout.dialog_entry, null);

        return new AlertDialog.Builder(this)
        .setIcon(R.drawable.icon)
        .setTitle("Set tumblr user name.")
        .setView(entryView)
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mProgressDialog = ProgressDialog.show(mContext, "Loading",
                        "Image downloading...", true);
                EditText edit = (EditText) entryView.findViewById(R.id.username_edit);
                String url = "http://" + edit.getText().toString() + ".tumblr.com/";
                mTask = new ImageTask().execute(url);
            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        })
        .create();
    }

    class ImageTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            // 画像URLを取得
            final String[] imageStr = getImage(params[0]);
            final IconicAdapter adapter = new IconicAdapter(mContext, imageStr, mHandler);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // リスト作成
                    setListAdapter(adapter);
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            mProgressDialog.dismiss();
        }
    }

    /** Tumblrから画像取得 */
    private String[] getImage(String tumblrUrl) {
        // 未実装（正規表現を使って画像を取得してくる予定
        String[] imageStr = new String[BUTTON_MAX];
        try {
            URL url = new URL(tumblrUrl);
            HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
            Pattern p = Pattern
                    .compile("http\\:\\/\\/\\d+.media\\.tumblr\\.com\\/(?!avatar_)[\\-_\\.\\!\\~\\*\\'\\(\\)a-zA-Z0-9\\;\\/\\?\\:@&=\\$\\,\\%\\#]+\\.(jpg|jpeg|png|gif|bmp)");
            urlCon.setRequestMethod("GET");
            BufferedReader urlIn = new BufferedReader(new InputStreamReader(
                    urlCon.getInputStream()));
            String str;
            int count = 0;
            while ((str = urlIn.readLine()) != null) {
                Matcher m = p.matcher(str);
                if (m.find()) {
                    Log.d(TAG, m.group());
                    imageStr[count] = m.group();
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

    public void onListItemClick(ListView parent, View v, int position, long id) {
        // 端末の幅と高さ
        int hw = getWallpaperDesiredMinimumWidth();
        int hh = getWallpaperDesiredMinimumHeight();
        Log.d(TAG, "bmpX:" + hw + "/bmpY:" + hh);

        // 画像を取得してスケール
        ViewHolder holder = (ViewHolder) v.getTag();
        BitmapDrawable draw = (BitmapDrawable) holder.img.getDrawable();
         final Bitmap bmp = ScaleBitmap.getScaleBitmap(draw.getBitmap(), hw, hh);

         mProgressDialog = ProgressDialog.show(this, "Wallpaper Setting",
                 "The image is put on the wallpaper...", true);
        // 壁紙設定
        new Thread(new Runnable() {
            public void run() {
                try {
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

    /**
     * ArrayAdapter を拡張したクラス。 画像を一覧表示させている。
     */
    public class IconicAdapter extends ArrayAdapter<Object> {
        Activity mContext;
        String[] mItems;
        Handler mHandler;
        LayoutInflater mInflater;
        Bitmap[] mBitmap;

        IconicAdapter(Activity context, String[] items, Handler handler) {
            super(context, R.layout.row, items);
            mContext = context;
            mItems = items;
            mHandler = handler;
            mInflater = LayoutInflater.from(mContext);

            mBitmap = new Bitmap[BUTTON_MAX];
            for (int i = 0; i < BUTTON_MAX; i++) {
                try {
                    URL url = new URL(mItems[i]);
                    final InputStream is = url.openStream();
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBitmap[i] = BitmapFactory.decodeStream(is);
                    Log.d(TAG, "No." + Integer.valueOf(i) + "width:" + mBitmap[i].getWidth() + "height:" + mBitmap[i].getHeight());
                    is.close();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        /** 画面に表示される毎に呼び出される */
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewHolder holder;

            if (row == null) {
                row = mInflater.inflate(R.layout.row, null);
                holder = new ViewHolder();

                // 画像イメージを作成
                holder.img = (ImageView) row.findViewById(R.id.image);

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            holder.img.setImageBitmap(mBitmap[position]);
            holder.img.setTag(mItems[position]);
            //Log.d(TAG, "No." + Integer.valueOf(position) + "width:" + mBitmap[position].getWidth() + "height:" + mBitmap[position].getHeight());

            return row;
        }

/* 10件目以降は非同期で取得してくる予定
        // 画像をダウンロードして表示する
        private void downloadAndUpdateImage(int position, ImageButton v) {
            DownloadTask task = new DownloadTask(v, mHandler);
            task.execute(mItems[position]);
        }
*/
    }
    static class ViewHolder {
        ImageView img;
    }
}
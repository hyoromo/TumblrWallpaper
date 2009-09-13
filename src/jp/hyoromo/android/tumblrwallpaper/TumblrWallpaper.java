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
import android.app.ListActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
 
/**
* Tumblr から画像情報を取得して、HOMEの壁紙として設定させるアプリ。
* メイン処理は WallpaperService で行っている。
* @author hyoromo
*/
public class TumblrWallpaper extends ListActivity {
 
    private static final String TAG = "TumblrWallpaper";
    private static final int BUTTON_MAX = 10;
    private Handler mHandler = new Handler();
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // ログ収集開始
        //Debug.startMethodTracing();

        setContentView(R.layout.main);
        // 画像URLを取得
        String[] imageStr = getImage();
        // リスト作成
        setListAdapter(new IconicAdapter(this, imageStr, mHandler));

        // ログ収集終了
        //Debug.stopMethodTracing();
    }
 
    /**
    * 画像情報設定
    * @param v ImageButton
    * @param i
    */
    private void setDraw(View v, String imageStr) {
        URL url;
        try {
            url = new URL(imageStr);
            InputStream is = url.openStream();
            Drawable draw = Drawable.createFromStream(is, "");
            v.setBackgroundDrawable(draw);
            v.setTag(url.toExternalForm());
            is.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
    }

    /**
    * Tumblrから画像取得
    */
    private String[] getImage() {
        // 未実装（正規表現を使って画像を取得してくる予定
        String []imageStr = new String[10];
        try {
            URL url = new URL("http://hyoromo.tumblr.com/");
            HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
            Pattern p = Pattern.compile("http\\:\\/\\/\\d+.media\\.tumblr\\.com\\/(?!avatar_)[\\-_\\.\\!\\~\\*\\'\\(\\)a-zA-Z0-9\\;\\/\\?\\:@&=\\$\\,\\%\\#]+\\.(jpg|jpeg|png|gif|bmp)");
            urlCon.setRequestMethod("GET");
            BufferedReader urlIn = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        e.printStackTrace();
        }
        return imageStr;
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
        // 端末の幅と高さ
        int hw = getWallpaperDesiredMinimumWidth();
        int hh = getWallpaperDesiredMinimumHeight();

        // 画像を取得してスケール
        ViewHolder holder = (ViewHolder) v.getTag();
        BitmapDrawable draw = (BitmapDrawable) holder.img.getDrawable();
        Bitmap bmp = draw.getBitmap();
        bmp = ScaleBitmap.getScaleBitmap(bmp, hw, hh);

        // 壁紙設定
        try {
            setWallpaper(bmp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 壁紙設定後に Activity を終了させる。
        finish();
    }


    /**
     * ArrayAdapter を拡張したクラス。
     * 画像を一覧表示させている。
     */
    public class IconicAdapter extends ArrayAdapter {
        Activity mContext;
        String[] mItems;
        Handler mHandler;
        LayoutInflater mInflater;
        Drawable[] mDraw;
        Bitmap[] mBitmap;

        IconicAdapter(Activity context, String[] items, Handler handler) {
            super(context, R.layout.row, items);
            mContext = context;
            mItems = items;
            mHandler = handler;
            mInflater = LayoutInflater.from(mContext);

            mDraw = new Drawable[BUTTON_MAX];
            mBitmap = new Bitmap[BUTTON_MAX];
            for (int i = 0; i < BUTTON_MAX; i++) {
                try {
                    URL url = new URL(mItems[i]);
                    InputStream is = url.openStream();
                    //mBitmap[i] = BitmapFactory.decodeStream(is);
                    mBitmap[i] = BitmapFactory.decodeStream(is);
                    mDraw[i] = Drawable.createFromStream(is, "");
                    is.close();
                    //return draw;
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

            return row;
        }

        // 画像をダウンロードして表示する
        private void downloadAndUpdateImage(int position, ImageButton v) {
            DownloadTask task = new DownloadTask(v, mHandler);
            task.execute(mItems[position]);
        }
    }

    static class ViewHolder {
        ImageView img;
    }
}
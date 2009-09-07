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
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
 
/**
* Tumblr から画像情報を取得して、HOMEの壁紙として設定させるアプリ。
* メイン処理は WallpaperService で行っている。
* @author hyoromo
*/
public class TumblrWallpaper extends Activity implements OnClickListener {
 
    private static final String TAG = "TumblrWallpaper";
    private static final int BUTTON_MAX = 10;
    private static final String IMAGE_PATH = "PATH";
    private int []id = {
            R.id.button00,
            R.id.button01,
            R.id.button02,
            R.id.button03,
            R.id.button04,
            R.id.button05,
            R.id.button06,
            R.id.button07,
            R.id.button08,
            R.id.button09
    };
    private ImageButton []button = new ImageButton[BUTTON_MAX];
    private Handler mHandler = new Handler();
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ログ収集開始
        //Debug.startMethodTracing();

        setContentView(R.layout.main);
        // 画像URLを取得
        String[] imageStr = getImage();
        // ボタン初期設定
        for (int i = 0; i < BUTTON_MAX; i++) {
            button[i] = (ImageButton) this.findViewById(id[i]);
            if (i < 1) {
                // 通常画像設定
                setDraw(button[i], imageStr[i]);
            } else {
                // 非同期で画像設定
                //downloadAndUpdateImage(button[i], imageStr[i]);
            }
            button[i].setOnClickListener(this);
        }
        downloadAndUpdateImage(button, imageStr);

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
        // サイズ指定は 250 又は 400 のどちらかを指定する。今は 250 を指定。
        // ダミーデータとして格納。URL を解析して 20 ピクチャー表示させる予定。
        /*
        String []imageStr = {
                "http://14.media.tumblr.com/tumblr_kocridVHHs1qz5eiyo1_400.jpg",
                "http://16.media.tumblr.com/tumblr_kogypbo2sN1qzxpv5o1_400.jpg",
                "http://13.media.tumblr.com/Jcnh0ZA0Fr2xnlx5DV7skgn8o1_400.jpg",
                "http://13.media.tumblr.com/Jcnh0ZA0Fr0dhj76TKCgAVt4o1_400.jpg",
                "http://3.media.tumblr.com/4HHYyjXOQr0iyz34DvvtV9gso1_400.png",
                "http://15.media.tumblr.com/tumblr_kp8ztnEwQm1qzxpv5o1_400.jpg",
                "http://18.media.tumblr.com/tumblr_kow1hbjvDV1qzxpv5o1_400.jpg",
                "http://19.media.tumblr.com/tumblr_kos3lwxqA21qzxpv5o1_400.jpg",
                "http://17.media.tumblr.com/tumblr_kor2o8ja961qzxpv5o1_400.png",
                "http://14.media.tumblr.com/tumblr_kor2mtHwaM1qzxpv5o1_400.jpg"
        };
        */

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
    
    @Override
    public void onClick(View v) {
        // サービスをインテントで起動。
        Intent intent = new Intent(TumblrWallpaper.this, WallpaperService.class);
        intent.putExtra(IMAGE_PATH, (String)v.getTag());
        startService(intent);
        // サービスを起動させたら Activity は終了させておく。
        finish();
    }
 
    // 画像をダウンロードして表示する
    private void downloadAndUpdateImage(View []v, String []imageStr) {
        DownloadTask task = new DownloadTask(v, mHandler);
        task.execute(imageStr);
    }
}
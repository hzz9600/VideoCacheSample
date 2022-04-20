package com.hz.videocache;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.preload.PreloadHelper;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    HttpProxyCacheServer cacheServer;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer = new MediaPlayer();

        if (cacheServer == null) {
            cacheServer = new HttpProxyCacheServer(this.getApplicationContext());
        }
    }

//    String url = "https://vd2.bdstatic.com/mda-ndhguzmmzz9hkztg/cae_h264_delogo/1650328165572422632/mda-ndhguzmmzz9hkztg.mp4";//12f
    //    String url = "https://vd3.bdstatic.com/mda-nck9gvjnys5duwbc/cae_h264_delogo/1647845706366501738/mda-nck9gvjnys5duwbc.mp4";//5f
    String url = "https://ksv-video-publish-m3u8.cdn.bcebos.com/d30ff91063ee24746b752a65903eda95dbfae750.m3u8";

    public void clickCache(View view) {
//        String url = "https://vd3.bdstatic.com/mda-mme9x7eeygvncdpk/sc/cae_h264/1639596540411766788/mda-mme9x7eeygvncdpk.mp4";

        //设置预加载缓存大小单位字节，默认 256KB -> 256*1024
        PreloadHelper.getInstance().setPreloadSize(512*1024);
        //加载制定url链接，url为源地址(非代理url)
        PreloadHelper.getInstance().load(cacheServer,url);
        cacheServer.registerCacheListener(new CacheListener() {
            @Override
            public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {
                Log.i(TAG, "onCacheAvailable: "+url+"  "+percentsAvailable);
            }
        },url);

    }

    public void clickStart(View view) {
        start(cacheServer.getProxyUrl(url));
    }

    public void start(String path) {
        try {
            SurfaceView surfaceView = new SurfaceView(this);
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

                }
            });
//            mediaPlayer.setSurface(surfaceView.getHolder().getSurface());
            FrameLayout view = findViewById(R.id.video_container);
            view.addView(surfaceView);

            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.setDisplay(surfaceView.getHolder());
                    mediaPlayer.start();
                }
            });

            long start = System.currentTimeMillis();
            mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mediaPlayer, int w, int h) {
                    Log.i(TAG, "onVideoSizeChanged:" + w + "-" + h+" time:"+(System.currentTimeMillis()-start));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
        mediaPlayer.release();
        mediaPlayer = null;
    }
}
package com.dsigner.dskey;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.dsigner.dskey.core.BootVideoManager;
import com.dsigner.dskey.core.DeviceKeyManager;
import com.dsigner.dskey.core.MediaDownloader;
import com.google.firebase.database.*;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ImageView imageView;
    private LinearLayout generatorLayout, codeOverlay;
    private TextView tvKey, tvOverlayCode;

    private ExoPlayer player;
    private String deviceKey;

    // 🔑 CONTROLE CORRETO DE ESTADO
    private String currentPlayingUrl = null;
    private boolean isDownloading = false;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.playerView);
        imageView = findViewById(R.id.imageView);
        generatorLayout = findViewById(R.id.generatorLayout);
        codeOverlay = findViewById(R.id.codeOverlay);
        tvKey = findViewById(R.id.tvKey);
        tvOverlayCode = findViewById(R.id.tvOverlayCode);

        deviceKey = DeviceKeyManager.getOrCreate(this);
        tvKey.setText(deviceKey);
        tvOverlayCode.setText(deviceKey);

        requestPermission();
        BootVideoManager.getDir();

        player = new ExoPlayer.Builder(this).build();
        playerView.setUseController(false);
        playerView.setPlayer(player);

        // ▶️ SEMPRE toca o local primeiro
        playLocalIfExists();

        // 🌐 LISTENER SEMPRE ATIVO QUANDO TEM INTERNET
        if (hasInternet()) {
            startRealtimeListener();
        }

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        showOverlay();
                    }
                });

        findViewById(android.R.id.content).setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) showOverlay();
            return true;
        });
    }

    // 🔥 LISTENER REALTIME CORRETO
    private void startRealtimeListener() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("midia")
                .child(deviceKey);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {

                if (!snap.exists()) return;

                String tipo = snap.child("tipo").getValue(String.class);
                String url  = snap.child("url").getValue(String.class);

                if (tipo == null || url == null) return;

                // 🖼️ IMAGEM
                if ("image".equals(tipo)) {
                    if (url.equals(currentPlayingUrl)) return;

                    currentPlayingUrl = url;
                    showImageOnline(url);
                    return;
                }

                // 🎥 VÍDEO
                if (!"video".equals(tipo)) return;

                // SE JÁ ESTÁ TOCANDO ESSE VÍDEO → NÃO FAZ NADA
                if (url.equals(currentPlayingUrl)) return;

                // ▶️ CONTINUA TOCANDO O ATUAL, MAS BAIXA O NOVO
                if (isDownloading) return;
                isDownloading = true;

                new Thread(() -> {
                    File dest = BootVideoManager.getVideo();
                    boolean ok = MediaDownloader.download(url, dest);

                    runOnUiThread(() -> {
                        isDownloading = false;
                        if (ok) {
                            currentPlayingUrl = url;
                            playLocalVideo();
                        }
                    });
                }).start();
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    // ▶️ TOCA LOCAL SE EXISTIR
    private void playLocalIfExists() {
        if (BootVideoManager.hasVideo()) {
            playLocalVideo();
        } else {
            showGenerator();
        }
    }

    private void playLocalVideo() {
        generatorLayout.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        codeOverlay.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);

        player.setMediaItem(MediaItem.fromUri(
                BootVideoManager.getVideo().toURI().toString()
        ));
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.prepare();
        player.play();
    }

    private void showImageOnline(String url) {
        playerView.setVisibility(View.GONE);
        generatorLayout.setVisibility(View.GONE);
        codeOverlay.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);

        Glide.with(this).load(url).into(imageView);
    }

    private void showGenerator() {
        playerView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        generatorLayout.setVisibility(View.VISIBLE);
    }

    private void showOverlay() {
        codeOverlay.setVisibility(View.VISIBLE);
        handler.postDelayed(() ->
                codeOverlay.setVisibility(View.GONE), 10_000);
    }

    private boolean hasInternet() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}

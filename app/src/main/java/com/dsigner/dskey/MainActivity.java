package com.dsigner.dskey;

import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.dsigner.dskey.core.DeviceKeyManager;
import com.dsigner.dskey.offline.*;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    PlayerView playerView;
    ImageView imageView;
    TextView tvKey;
    TextView tvOverlayCode;
    View codeOverlay;
    View generatorLayout;

    ExoPlayer player;
    String key;

    Handler handler = new Handler();
    Runnable hideOverlayRunnable;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.playerView);
        imageView  = findViewById(R.id.imageView);
        tvKey      = findViewById(R.id.tvKey);
        tvOverlayCode = findViewById(R.id.tvOverlayCode);
        codeOverlay = findViewById(R.id.codeOverlay);
        generatorLayout = findViewById(R.id.generatorLayout);

        key = DeviceKeyManager.getOrCreate(this);
        tvKey.setText(key);
        tvOverlayCode.setText(key);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setUseController(false);

        // ▶️ Tenta rodar OFFLINE primeiro
        playOffline();

        // 📡 Firebase online
        if (NetworkUtils.hasInternet(this)) {
            listenFirebase();
        }

        // 🔙 BOTÃO VOLTAR (ANDROID MODERNO)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showCodeOverlay();
            }
        });

        // 👆 TOQUE NA TELA
        findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                showCodeOverlay();
            }
            return true;
        });
    }

    // =========================
    // MOSTRAR CÓDIGO POR 10s
    // =========================
    private void showCodeOverlay() {
        codeOverlay.setVisibility(View.VISIBLE);

        if (hideOverlayRunnable != null) {
            handler.removeCallbacks(hideOverlayRunnable);
        }

        hideOverlayRunnable = () -> codeOverlay.setVisibility(View.GONE);
        handler.postDelayed(hideOverlayRunnable, 10_000);
    }

    // =========================
    // OFFLINE
    // =========================
    private void playOffline() {
        JSONObject o = BootMediaManager.read(this);
        if (o == null) {
            showGenerator();
            return;
        }

        File media = BootMediaManager.getMediaFile(this);
        if (!media.exists()) {
            showGenerator();
            return;
        }

        String tipo = o.optString("tipo");

        hideGenerator();
        player.stop();
        playerView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);

        if ("video".equals(tipo)) {
            playerView.setVisibility(View.VISIBLE);
            player.setMediaItem(MediaItem.fromUri(media.toURI().toString()));
            player.setRepeatMode(Player.REPEAT_MODE_ALL);
            player.prepare();
            player.play();
        } else if ("image".equals(tipo)) {
            imageView.setVisibility(View.VISIBLE);
            Glide.with(this).load(media).into(imageView);
        }
    }

    // =========================
    // FIREBASE
    // =========================
    private void listenFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("midia")
                .child(key);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                String tipo = s.child("tipo").getValue(String.class);
                String url  = s.child("url").getValue(String.class);
                if (tipo == null || url == null) return;

                JSONObject old = BootMediaManager.read(MainActivity.this);
                if (old != null && url.equals(old.optString("url"))) {
                    return;
                }

                File mediaFile = BootMediaManager.getMediaFile(MainActivity.this);

                new Thread(() -> {
                    try {
                        BootMediaManager.clearMedia(MainActivity.this);
                        MediaDownloader.download(url, mediaFile);
                        BootMediaManager.save(MainActivity.this, tipo, url);
                        runOnUiThread(() -> playOffline());
                    } catch (Exception ignored) {}
                }).start();
            }

            @Override public void onCancelled(DatabaseError e) {}
        });
    }

    private void showGenerator() {
        generatorLayout.setVisibility(View.VISIBLE);
        tvKey.setVisibility(View.VISIBLE);
        playerView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
    }

    private void hideGenerator() {
        generatorLayout.setVisibility(View.GONE);
        tvKey.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}

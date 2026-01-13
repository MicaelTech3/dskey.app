package com.dsigner.dskey;

import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.dsigner.dskey.core.DeviceKeyManager;
import com.dsigner.dskey.offline.BootMediaManager;
import com.dsigner.dskey.offline.MediaDownloader;
import com.dsigner.dskey.offline.NetworkUtils;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ImageView imageView;
    private LinearLayout generatorLayout;
    private LinearLayout codeOverlay;
    private TextView tvKey;
    private TextView tvOverlayCode;

    private ExoPlayer player;
    private String deviceKey;

    private final Handler handler = new Handler();
    private Runnable hideOverlayRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔗 VIEWS
        playerView = findViewById(R.id.playerView);
        imageView = findViewById(R.id.imageView);
        generatorLayout = findViewById(R.id.generatorLayout);
        codeOverlay = findViewById(R.id.codeOverlay);
        tvKey = findViewById(R.id.tvKey);
        tvOverlayCode = findViewById(R.id.tvOverlayCode);

        // 🔑 CÓDIGO FIXO DO DISPOSITIVO
        deviceKey = DeviceKeyManager.getOrCreate(this);
        tvKey.setText(deviceKey);
        tvOverlayCode.setText(deviceKey);

        // ▶️ PLAYER
        player = new ExoPlayer.Builder(this).build();
        playerView.setUseController(false);
        playerView.setPlayer(player);

        // ▶️ OFFLINE PRIMEIRO
        playOffline();

        // 🌐 FIREBASE
        if (NetworkUtils.hasInternet(this)) {
            listenFirebase();
        }

        // 🔙 BOTÃO VOLTAR (CONTROLE)
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

    // ================== OFFLINE ==================
    private void playOffline() {
        JSONObject json = BootMediaManager.read(this);
        File media = BootMediaManager.getMediaFile(this);

        // ❌ SEM MÍDIA
        if (json == null || !media.exists()) {
            player.stop();
            playerView.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);

            generatorLayout.setVisibility(View.VISIBLE);
            codeOverlay.setVisibility(View.GONE);
            tvKey.setVisibility(View.VISIBLE);
            return;
        }

        // ✅ COM MÍDIA
        generatorLayout.setVisibility(View.GONE);
        tvKey.setVisibility(View.GONE);
        codeOverlay.setVisibility(View.GONE);

        player.stop();
        playerView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);

        String tipo = json.optString("tipo");

        if ("video".equals(tipo)) {
            playerView.setVisibility(View.VISIBLE);
            player.setMediaItem(MediaItem.fromUri(media.toURI().toString()));
            player.setRepeatMode(Player.REPEAT_MODE_ALL);
            player.prepare();
            player.play();
        } else {
            imageView.setVisibility(View.VISIBLE);
            Glide.with(this).load(media).into(imageView);
        }
    }

    // ================== FIREBASE ==================
    private void listenFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("midia")
                .child(deviceKey);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                String tipo = s.child("tipo").getValue(String.class);
                String url = s.child("url").getValue(String.class);
                if (tipo == null || url == null) return;

                JSONObject old = BootMediaManager.read(MainActivity.this);
                if (old != null && url.equals(old.optString("url"))) return;

                File file = BootMediaManager.getMediaFile(MainActivity.this);

                new Thread(() -> {
                    try {
                        BootMediaManager.clear(MainActivity.this);
                        MediaDownloader.download(url, file);
                        BootMediaManager.save(MainActivity.this, tipo, url, file);
                        runOnUiThread(() -> playOffline());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    // ================== OVERLAY ==================
    private void showCodeOverlay() {
        if (generatorLayout.getVisibility() == View.VISIBLE) return;

        codeOverlay.setVisibility(View.VISIBLE);

        if (hideOverlayRunnable != null) {
            handler.removeCallbacks(hideOverlayRunnable);
        }

        hideOverlayRunnable = () -> codeOverlay.setVisibility(View.GONE);
        handler.postDelayed(hideOverlayRunnable, 10_000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
    }
}

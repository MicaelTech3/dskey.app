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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    // ── PLAYLIST ──────────────────────────────────────────────────
    private List<JSONObject> playlistItems = new ArrayList<>();
    private int playlistIndex = 0;
    private Runnable playlistRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView       = findViewById(R.id.playerView);
        imageView        = findViewById(R.id.imageView);
        generatorLayout  = findViewById(R.id.generatorLayout);
        codeOverlay      = findViewById(R.id.codeOverlay);
        tvKey            = findViewById(R.id.tvKey);
        tvOverlayCode    = findViewById(R.id.tvOverlayCode);

        deviceKey = DeviceKeyManager.getOrCreate(this);
        tvKey.setText(deviceKey);
        tvOverlayCode.setText(deviceKey);

        player = new ExoPlayer.Builder(this).build();
        playerView.setUseController(false);
        playerView.setPlayer(player);

        // Listener para avançar playlist de vídeo quando terminar
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED && !playlistItems.isEmpty()) {
                    advancePlaylist();
                }
            }
        });

        playOffline();

        if (NetworkUtils.hasInternet(this)) {
            listenFirebase();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showCodeOverlay();
            }
        });

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

        if (json == null) {
            showGeneratorScreen();
            return;
        }

        String tipo = json.optString("tipo");

        if ("playlist".equals(tipo)) {
            JSONArray items = json.optJSONArray("items");
            if (items == null || items.length() == 0) {
                showGeneratorScreen();
                return;
            }
            // Monta lista local com os arquivos já baixados
            List<JSONObject> loaded = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                try {
                    JSONObject item = items.getJSONObject(i);
                    File f = BootMediaManager.getPlaylistFile(this, i);
                    if (f.exists()) {
                        item.put("localPath", f.getAbsolutePath());
                        loaded.add(item);
                    }
                } catch (Exception ignored) {}
            }
            if (loaded.isEmpty()) {
                showGeneratorScreen();
                return;
            }
            startPlaylist(loaded);
        } else {
            // Mídia única (imagem ou vídeo)
            File media = BootMediaManager.getMediaFile(this);
            if (!media.exists()) {
                showGeneratorScreen();
                return;
            }
            stopPlaylist();
            showMedia(tipo, media.toURI().toString(), 0, false);
        }
    }

    // ================== PLAYLIST ==================
    private void startPlaylist(List<JSONObject> items) {
        stopPlaylist();
        playlistItems = items;
        playlistIndex = 0;
        showPlaylistItem();
    }

    private void showPlaylistItem() {
        if (playlistItems.isEmpty()) return;
        if (playlistIndex >= playlistItems.size()) playlistIndex = 0;

        JSONObject item = playlistItems.get(playlistIndex);
        String tipo     = item.optString("type", "image");
        String path     = item.optString("localPath", "");
        int duration    = item.optInt("duration", 10); // segundos
        boolean loop    = item.optBoolean("loop", false);

        // Mostra a mídia
        showMedia(tipo, "file://" + path, duration, loop);

        // Para imagens/gif agenda o próximo item
        if (!"video".equals(tipo)) {
            playlistRunnable = this::advancePlaylist;
            handler.postDelayed(playlistRunnable, duration * 1000L);
        }
        // Para vídeo, o avanço acontece no onPlaybackStateChanged (STATE_ENDED)
    }

    private void advancePlaylist() {
        if (playlistRunnable != null) {
            handler.removeCallbacks(playlistRunnable);
            playlistRunnable = null;
        }
        playlistIndex++;
        if (playlistIndex >= playlistItems.size()) playlistIndex = 0;
        showPlaylistItem();
    }

    private void stopPlaylist() {
        if (playlistRunnable != null) {
            handler.removeCallbacks(playlistRunnable);
            playlistRunnable = null;
        }
        playlistItems.clear();
        playlistIndex = 0;
    }

    // ================== EXIBE MÍDIA ==================
    private void showMedia(String tipo, String uri, int duration, boolean loop) {
        generatorLayout.setVisibility(View.GONE);
        tvKey.setVisibility(View.GONE);

        player.stop();
        playerView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);

        if ("video".equals(tipo)) {
            playerView.setVisibility(View.VISIBLE);
            player.setMediaItem(MediaItem.fromUri(uri));
            player.setRepeatMode(loop ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
            player.prepare();
            player.play();
        } else {
            // image ou gif
            imageView.setVisibility(View.VISIBLE);
            Glide.with(this).load(uri).into(imageView);
        }
    }

    private void showGeneratorScreen() {
        stopPlaylist();
        player.stop();
        playerView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        generatorLayout.setVisibility(View.VISIBLE);
        codeOverlay.setVisibility(View.GONE);
        tvKey.setVisibility(View.VISIBLE);
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
                if (tipo == null) return;

                if ("playlist".equals(tipo)) {
                    // Lê o array items do Firebase
                    Object rawItems = s.child("items").getValue();
                    if (rawItems == null) return;

                    try {
                        JSONArray items = new JSONArray(
                                new com.google.gson.Gson().toJson(rawItems)
                        );
                        if (items.length() == 0) return;

                        // Verifica se mudou comparando com o cache
                        JSONObject old = BootMediaManager.read(MainActivity.this);
                        if (old != null && "playlist".equals(old.optString("tipo"))) {
                            JSONArray oldItems = old.optJSONArray("items");
                            if (oldItems != null && oldItems.toString().equals(items.toString())) return;
                        }

                        // Baixa todos os arquivos da playlist
                        new Thread(() -> {
                            try {
                                BootMediaManager.clearAll(MainActivity.this);
                                List<JSONObject> downloaded = new ArrayList<>();

                                for (int i = 0; i < items.length(); i++) {
                                    JSONObject item = items.getJSONObject(i);
                                    String url = item.optString("url", "");
                                    if (url.isEmpty()) continue;

                                    File dest = BootMediaManager.getPlaylistFile(MainActivity.this, i);
                                    MediaDownloader.download(url, dest);
                                    item.put("localPath", dest.getAbsolutePath());
                                    downloaded.add(item);
                                }

                                BootMediaManager.savePlaylist(MainActivity.this, items);
                                runOnUiThread(() -> startPlaylist(downloaded));

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    // Mídia única
                    String url = s.child("url").getValue(String.class);
                    if (url == null) return;

                    JSONObject old = BootMediaManager.read(MainActivity.this);
                    if (old != null && url.equals(old.optString("url"))) return;

                    File file = BootMediaManager.getMediaFile(MainActivity.this);
                    String finalTipo = tipo;

                    new Thread(() -> {
                        try {
                            BootMediaManager.clearAll(MainActivity.this);
                            MediaDownloader.download(url, file);
                            BootMediaManager.save(MainActivity.this, finalTipo, url, file);
                            runOnUiThread(() -> playOffline());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
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
        stopPlaylist();
        if (player != null) player.release();
    }
}
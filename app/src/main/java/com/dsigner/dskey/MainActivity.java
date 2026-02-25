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

    // ── Views ─────────────────────────────────────────────────────────────────
    private PlayerView playerView;
    private ImageView imageView;
    private LinearLayout generatorLayout;
    private LinearLayout codeOverlay;
    private LinearLayout emptyPlaylistLayout;
    private TextView tvKey;
    private TextView tvOverlayCode;

    // ── Player ────────────────────────────────────────────────────────────────
    private ExoPlayer player;
    private String deviceKey;

    // ── Slideshow ─────────────────────────────────────────────────────────────
    private final Handler slideshowHandler = new Handler();
    private Runnable slideshowRunnable;
    private List<PlaylistItem> playlistItems = new ArrayList<>();
    private int currentSlide = 0;

    // ── Overlay ───────────────────────────────────────────────────────────────
    private final Handler handler = new Handler();
    private Runnable hideOverlayRunnable;

    // ── Modelo ────────────────────────────────────────────────────────────────
    static class PlaylistItem {
        String url, type;
        int duration;
        boolean loop;
        PlaylistItem(String url, String type, int duration, boolean loop) {
            this.url = url; this.type = type;
            this.duration = duration; this.loop = loop;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView          = findViewById(R.id.playerView);
        imageView           = findViewById(R.id.imageView);
        generatorLayout     = findViewById(R.id.generatorLayout);
        codeOverlay         = findViewById(R.id.codeOverlay);
        emptyPlaylistLayout = findViewById(R.id.emptyPlaylistLayout);
        tvKey               = findViewById(R.id.tvKey);
        tvOverlayCode       = findViewById(R.id.tvOverlayCode);

        deviceKey = DeviceKeyManager.getOrCreate(this);
        tvKey.setText(deviceKey);
        tvOverlayCode.setText(deviceKey);

        player = new ExoPlayer.Builder(this).build();
        playerView.setUseController(false);
        playerView.setPlayer(player);

        playOffline();
        if (NetworkUtils.hasInternet(this)) listenFirebase();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { showCodeOverlay(); }
        });

        findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) showCodeOverlay();
            return true;
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VISIBILIDADE
    // ══════════════════════════════════════════════════════════════════════════
    private void hideAll() {
        player.stop();
        playerView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        generatorLayout.setVisibility(View.GONE);
        emptyPlaylistLayout.setVisibility(View.GONE);
        tvKey.setVisibility(View.GONE);
    }

    private void showGenerator() {
        stopSlideshow();
        hideAll();
        generatorLayout.setVisibility(View.VISIBLE);
        tvKey.setVisibility(View.VISIBLE);
    }

    private void showEmptyPlaylist() {
        stopSlideshow();
        hideAll();
        emptyPlaylistLayout.setVisibility(View.VISIBLE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  OFFLINE
    // ══════════════════════════════════════════════════════════════════════════
    private void playOffline() {
        stopSlideshow();
        JSONObject json = BootMediaManager.read(this);
        if (json == null) { showGenerator(); return; }

        String tipo = json.optString("tipo");

        if ("playlist".equals(tipo)) {
            JSONArray arr = json.optJSONArray("items");
            if (arr == null || arr.length() == 0) { showEmptyPlaylist(); return; }
            playPlaylistFromJson(arr);
        } else if ("empty_playlist".equals(tipo)) {
            showEmptyPlaylist();
        } else {
            File media = BootMediaManager.getMediaFile(this);
            if (!media.exists()) { showGenerator(); return; }
            playSingleMedia(tipo, media);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MÍDIA ÚNICA — aceita qualquer formato de imagem, mp4 e mov
    // ══════════════════════════════════════════════════════════════════════════
    private void playSingleMedia(String tipo, File media) {
        stopSlideshow();
        hideAll();

        if (isVideoType(tipo)) {
            playerView.setVisibility(View.VISIBLE);
            player.setMediaItem(MediaItem.fromUri(media.toURI().toString()));
            player.setRepeatMode(Player.REPEAT_MODE_ALL);
            player.prepare();
            player.play();
        } else {
            // Glide suporta: jpg, jpeg, png, gif, webp, bmp, heic, etc.
            imageView.setVisibility(View.VISIBLE);
            Glide.with(this).load(media).into(imageView);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PLAYLIST / SLIDESHOW
    // ══════════════════════════════════════════════════════════════════════════
    private void playPlaylistFromJson(JSONArray arr) {
        playlistItems.clear();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject item = arr.getJSONObject(i);
                String url  = item.optString("url", "");
                String type = item.optString("type", "image");
                int dur     = item.optInt("duration", 10);
                boolean lp  = item.optBoolean("loop", false);
                if (!url.isEmpty()) playlistItems.add(new PlaylistItem(url, type, dur, lp));
            } catch (Exception ignored) {}
        }

        if (playlistItems.isEmpty()) { showEmptyPlaylist(); return; }

        hideAll();
        currentSlide = 0;
        showSlide(0);
    }

    private void showSlide(int index) {
        if (playlistItems.isEmpty()) { showEmptyPlaylist(); return; }
        index = index % playlistItems.size();
        currentSlide = index;
        PlaylistItem item = playlistItems.get(index);
        stopSlideshow();

        if (isVideoType(item.type)) {
            imageView.setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);
            player.stop();
            player.setMediaItem(MediaItem.fromUri(item.url));
            player.setRepeatMode(item.loop ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
            player.prepare();
            player.play();

            final int next = index + 1;
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED) {
                        player.removeListener(this);
                        runOnUiThread(() -> showSlide(next));
                    }
                }
            });
        } else {
            // Imagem / GIF / WebP / PNG / JPG / BMP / HEIC — Glide cuida de tudo
            playerView.setVisibility(View.GONE);
            player.stop();
            imageView.setVisibility(View.VISIBLE);
            Glide.with(this).load(item.url).into(imageView);

            int durationMs = Math.max(1, item.duration) * 1000;
            final int next = index + 1;
            slideshowRunnable = () -> showSlide(next);
            slideshowHandler.postDelayed(slideshowRunnable, durationMs);
        }
    }

    /** mp4 e mov são vídeo; gif, jpg, png, webp, bmp, heic são imagem */
    private boolean isVideoType(String type) {
        if (type == null) return false;
        String t = type.toLowerCase();
        return t.equals("video") || t.equals("mp4") || t.equals("mov");
    }

    private void stopSlideshow() {
        if (slideshowRunnable != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
            slideshowRunnable = null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FIREBASE
    // ══════════════════════════════════════════════════════════════════════════
    private void listenFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("midia").child(deviceKey);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                String tipo = s.child("tipo").getValue(String.class);
                if (tipo == null) return;

                // ── PLAYLIST ──────────────────────────────────────────────────
                if ("playlist".equals(tipo)) {
                    DataSnapshot itemsSnap = s.child("items");

                    if (!itemsSnap.exists() || !itemsSnap.hasChildren()) {
                        BootMediaManager.clear(MainActivity.this);
                        BootMediaManager.saveEmptyPlaylist(MainActivity.this);
                        runOnUiThread(() -> showEmptyPlaylist());
                        return;
                    }

                    JSONArray arr = new JSONArray();
                    try {
                        for (DataSnapshot is : itemsSnap.getChildren()) {
                            JSONObject obj = new JSONObject();
                            obj.put("url",  is.child("url").getValue(String.class));
                            obj.put("type", is.child("type").getValue(String.class));
                            Object dur = is.child("duration").getValue();
                            obj.put("duration", dur != null ? Integer.parseInt(dur.toString()) : 10);
                            Object lp = is.child("loop").getValue();
                            obj.put("loop", lp != null && Boolean.parseBoolean(lp.toString()));
                            arr.put(obj);
                        }
                    } catch (Exception e) { e.printStackTrace(); return; }

                    BootMediaManager.clear(MainActivity.this);
                    BootMediaManager.savePlaylist(MainActivity.this, arr);
                    final JSONArray finalArr = arr;
                    runOnUiThread(() -> playPlaylistFromJson(finalArr));
                    return;
                }

                // ── STOP ──────────────────────────────────────────────────────
                if ("stop".equals(tipo)) {
                    BootMediaManager.clear(MainActivity.this);
                    runOnUiThread(() -> showGenerator());
                    return;
                }

                // ── MÍDIA ÚNICA ───────────────────────────────────────────────
                String url = s.child("url").getValue(String.class);
                if (url == null) return;

                JSONObject old = BootMediaManager.read(MainActivity.this);
                if (old != null && url.equals(old.optString("url"))) return;

                File file = BootMediaManager.getMediaFile(MainActivity.this);
                new Thread(() -> {
                    try {
                        BootMediaManager.clear(MainActivity.this);
                        MediaDownloader.download(url, file);
                        BootMediaManager.save(MainActivity.this, tipo, url, file);
                        runOnUiThread(() -> playOffline());
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  OVERLAY
    // ══════════════════════════════════════════════════════════════════════════
    private void showCodeOverlay() {
        if (generatorLayout.getVisibility() == View.VISIBLE) return;
        if (emptyPlaylistLayout.getVisibility() == View.VISIBLE) return;
        codeOverlay.setVisibility(View.VISIBLE);
        if (hideOverlayRunnable != null) handler.removeCallbacks(hideOverlayRunnable);
        hideOverlayRunnable = () -> codeOverlay.setVisibility(View.GONE);
        handler.postDelayed(hideOverlayRunnable, 10_000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSlideshow();
        if (player != null) player.release();
    }
}
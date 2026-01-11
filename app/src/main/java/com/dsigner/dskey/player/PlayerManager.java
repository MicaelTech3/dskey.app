package com.dsigner.dskey.player;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.io.File;

public class PlayerManager {

    private final ExoPlayer player;

    public PlayerManager(Context c, PlayerView view) {
        player = new ExoPlayer.Builder(c).build();
        view.setUseController(false);
        view.setPlayer(player);
    }

    public void play(File video) {
        MediaItem item = MediaItem.fromUri(Uri.fromFile(video));
        player.setMediaItem(item);
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.prepare();
        player.play();
    }
}

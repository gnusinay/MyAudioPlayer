package com.mercury.gnusin.myaudioplayer.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


public interface AudioPlayerInterface {

    String CHANGE_STATE_AUDIO_PLAYER_EVENT = "change_state_audio_player_event";
    String ERROR_AUDIO_PLAYER_EVENT = "error_audio_player_event";

    @IntDef({PlayerState.PLAY, PlayerState.PAUSE, PlayerState.STOP, PlayerState.CONNECT, PlayerState.DISCONNECT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerState {
        int PLAY = 0;
        int PAUSE = 1;
        int STOP = 2;
        int CONNECT = 3;
        int DISCONNECT = 4;
    }

    void playNewTrack(Context context);

    void playContinue();

    void pause();

    void stop();

    @PlayerState
    int getPlayerState();

    Bitmap getPlayingTrackCover(Context context);

    void init(Context context);

    void release(Context context);
}

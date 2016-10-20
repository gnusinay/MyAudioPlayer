package com.mercury.gnusin.myaudioplayer.model;

import android.graphics.Bitmap;
import android.os.Binder;


public class AudioPlayerBinder extends Binder {
    private AudioPlayerService service;

    public AudioPlayerBinder(AudioPlayerService service) {
        this.service = service;
    }

    public @AudioPlayerService.State int getPlayerState() {
        return service.getPlayerState();
    }

    public void playNewTrack() {
        service.playNewTrack();
    }

    public void playContinue() {
        service.playContinue();
    }

    public void pause() {
        service.pause();
    }

    public void stop() {
        service.stop();
    }

    public Bitmap getPlayingTrackCover() {
        return service.getPlayingTrackCover();
    }
}

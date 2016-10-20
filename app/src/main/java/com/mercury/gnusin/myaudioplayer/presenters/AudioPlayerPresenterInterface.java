package com.mercury.gnusin.myaudioplayer.presenters;


import android.content.Context;

import com.mercury.gnusin.myaudioplayer.view.AudioPlayerViewInterface;

public interface AudioPlayerPresenterInterface {

    void bindView(AudioPlayerViewInterface  view);

    void unbindView();

    void playPause();

    void stop();
}

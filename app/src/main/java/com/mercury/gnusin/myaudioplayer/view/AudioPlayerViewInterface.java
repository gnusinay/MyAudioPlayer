package com.mercury.gnusin.myaudioplayer.view;

import android.content.Context;
import android.graphics.Bitmap;
import com.mercury.gnusin.myaudioplayer.model.AudioPlayerInterface;



public interface AudioPlayerViewInterface {


    void showErrorMessage(String message);

    void setCover(Bitmap cover);

    void changeUIByPlayerState(@AudioPlayerInterface.PlayerState int playerState);

    Context getContext();

    void stopApp();
}

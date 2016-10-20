package com.mercury.gnusin.myaudioplayer.presenters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
import com.mercury.gnusin.myaudioplayer.R;
import com.mercury.gnusin.myaudioplayer.model.AudioPlayerInterface;
import com.mercury.gnusin.myaudioplayer.view.AudioPlayerViewInterface;
import java.util.ArrayList;
import java.util.List;


public class AudioPlayerPresenter implements AudioPlayerPresenterInterface{

    private AudioPlayerViewInterface boundView;
    private AudioPlayerInterface audioPlayer;
    private List<BroadcastReceiver> eventReceiverList = new ArrayList<>();


    public AudioPlayerPresenter(AudioPlayerViewInterface boundView, AudioPlayerInterface audioPlayer) {
        this.boundView = boundView;
        this.audioPlayer = audioPlayer;
    }

    @Override
    public void bindView(AudioPlayerViewInterface view) {
        boundView = view;
        registrationEventReceivers(boundView.getContext());
        audioPlayer.init(boundView.getContext());
    }

    @Override
    public void unbindView() {
        for (BroadcastReceiver receiver : eventReceiverList) {
            LocalBroadcastManager.getInstance(boundView.getContext()).unregisterReceiver(receiver);
        }
        audioPlayer.release(boundView.getContext());
        boundView = null;
    }

    @Override
    public void playPause() {
        switch (audioPlayer.getPlayerState()) {
            case AudioPlayerInterface.PlayerState.PLAY:
                audioPlayer.pause();
                    break;
            case AudioPlayerInterface.PlayerState.PAUSE:
                audioPlayer.playContinue();
                    break;
            case AudioPlayerInterface.PlayerState.STOP:
                audioPlayer.playNewTrack(boundView.getContext());
                break;
            case AudioPlayerInterface.PlayerState.CONNECT:
                audioPlayer.playNewTrack(boundView.getContext());
                break;
        }
    }

    @Override
    public void stop() {
        audioPlayer.stop();
    }

    private void registrationEventReceivers(Context context) {
        BroadcastReceiver changeStatePlayerEvent = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (boundView != null) {
                    @AudioPlayerInterface.PlayerState int playerState = audioPlayer.getPlayerState();
                    boundView.changeUIByPlayerState(playerState);
                    if (playerState == AudioPlayerInterface.PlayerState.DISCONNECT) {
                        boundView.stopApp();
                    } else {
                        if (playerState != AudioPlayerInterface.PlayerState.STOP) {
                            Bitmap cover = audioPlayer.getPlayingTrackCover(boundView.getContext());
                            if (cover != null) {
                                boundView.setCover(cover);
                            } else {
                                boundView.setCover(BitmapFactory.decodeResource(context.getResources(), R.mipmap.cover_default));
                            }
                        }
                    }
                }
            }
        };
        eventReceiverList.add(changeStatePlayerEvent);
        LocalBroadcastManager.getInstance(context).registerReceiver(changeStatePlayerEvent, new IntentFilter(AudioPlayerInterface.CHANGE_STATE_AUDIO_PLAYER_EVENT));

        BroadcastReceiver errorPlayerEvent = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (boundView != null) {
                    String errorMessage = intent.getStringExtra("error");
                    boundView.showErrorMessage(String.format(boundView.getContext().getResources().getString(R.string.run_audio_player_error_message), errorMessage));
                }
            }
        };
        eventReceiverList.add(errorPlayerEvent);
        LocalBroadcastManager.getInstance(context).registerReceiver(errorPlayerEvent, new IntentFilter(AudioPlayerInterface.ERROR_AUDIO_PLAYER_EVENT));
    }
}

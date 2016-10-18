package com.mercury.gnusin.myaudioplayer.middlelayer;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mercury.gnusin.myaudioplayer.R;
import com.mercury.gnusin.myaudioplayer.model.AudioPlayerBinder;
import com.mercury.gnusin.myaudioplayer.model.AudioPlayerService;
import com.mercury.gnusin.myaudioplayer.view.PlayerControlActivity;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;


public class AudioPlayerPresenter {

    @IntDef ({PlayerState.UNREADY, PlayerState.READY, PlayerState.PLAY, PlayerState.PAUSE, PlayerState.STOP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerState {
        int UNREADY = 0;
        int READY = 1;
        int PLAY = 2;
        int PAUSE = 3;
        int STOP = 4;
    }

    private PlayerControlActivity activity;
    private @PlayerState int playerState = PlayerState.UNREADY;
    private AudioPlayerBinder audioPlayerBinder;
    private ServiceConnection serviceConnection;
    private List<BroadcastReceiver> eventReceiverList = new ArrayList<>();


    public AudioPlayerPresenter(final Context context) {
        context.startService(new Intent(context, AudioPlayerService.class));
        registrationEventReceivers(context);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                audioPlayerBinder = (AudioPlayerBinder) service;
                Intent intent = new Intent(MiddleLayerEvents.CONNECT_SERVICE);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                Log.d("AGn", "Service READY");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Intent intent = new Intent(MiddleLayerEvents.DISCONNECT_SERVICE);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                Log.d("AGn", "Service UNREADY");
            }
        };
    }


    public void playPause() {
        if (playerState == PlayerState.UNREADY) {
            startAudioPlayerService(activity);
        }

        switch (playerState) {
            case PlayerState.READY:
                    playNewTrack();
                    break;
            case PlayerState.PLAY:
                    pause();
                    break;
            case PlayerState.PAUSE:
                    playContinue();
                    break;
            case PlayerState.STOP:
                    playNewTrack();
                    break;
        }
    }

    public void stop() {
        changePlayerState(PlayerState.STOP);
        stopAudioPlayerService(activity);
    }

    public void bindActivity(PlayerControlActivity activity) {
        if (activity != null) {
            this.activity = activity;
            activity.bindService(new Intent(activity, AudioPlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
            //activity.changeUIByState(playerState);
        }
    }

    public void unbindActivity() {
        activity = null;

        /*if (activity != null) {
            //activity.unbindService(serviceConnection);

            for (BroadcastReceiver receiver : eventReceiverList) {
                LocalBroadcastManager.getInstance(activity).unregisterReceiver(receiver);
            }

            activity = null;
        }*/
    }

    public void release(Context context) {
        context.unbindService(serviceConnection);
    }

    private void startAudioPlayerService(final Context context) {
        if (playerState == PlayerState.UNREADY) {


            //Intent intent = new Intent(context, AudioPlayerService.class);
            //context.startService(intent);

        }
    }

    private void stopAudioPlayerService(Context context) {
        if (playerState != PlayerState.UNREADY) {
            Intent intent = new Intent(context, AudioPlayerService.class);
            context.unbindService(serviceConnection);
            context.stopService(intent);

            changePlayerState(PlayerState.UNREADY);
        }
    }


    private void playNewTrack() {
        changePlayerState(PlayerState.PLAY);
        audioPlayerBinder.playNewTrack();
    }

    private void playContinue() {
        changePlayerState(PlayerState.PLAY);
        audioPlayerBinder.playContinue();
    }

    private void pause() {
        changePlayerState(PlayerState.PAUSE);
        audioPlayerBinder.pause();
    }

    private void changePlayerState(@PlayerState int playerState) {
        this.playerState = playerState;
        activity.changeUIByState(playerState);
    }

    private void registrationEventReceivers(Context context) {
        BroadcastReceiver connectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                activity.changeUIByState(convertStateServerToStateUI(audioPlayerBinder.getPlayerState()));
                //changePlayerState(PlayerState.UNREADY);
                //activity.showErrorMessage(R.string.unready_audio_player_error_message);
            }
        };
        eventReceiverList.add(connectReceiver);
        LocalBroadcastManager.getInstance(context).registerReceiver(connectReceiver, new IntentFilter(MiddleLayerEvents.DISCONNECT_SERVICE));

        BroadcastReceiver disconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //changePlayerState(PlayerState.UNREADY);
                activity.changeUIByState(PlayerControlActivity.StateUI.NO_BIND);
                activity.showErrorMessage(R.string.unready_audio_player_error_message);
            }
        };
        eventReceiverList.add(disconnectReceiver);
        LocalBroadcastManager.getInstance(context).registerReceiver(disconnectReceiver, new IntentFilter(MiddleLayerEvents.DISCONNECT_SERVICE));
    }

    public void throwError() {

    }

    private @PlayerControlActivity.StateUI int convertStateServerToStateUI(@AudioPlayerService.State int playerState) {
        switch (playerState) {
            case AudioPlayerService.State.PLAY:
                return PlayerControlActivity.StateUI.PLAY;
            case AudioPlayerService.State.PAUSE:
                return PlayerControlActivity.StateUI.PAUSE;
            case AudioPlayerService.State.STOP:
                return PlayerControlActivity.StateUI.STOP;
            default: return PlayerControlActivity.StateUI.NO_BIND;
        }
    }


}

package com.mercury.gnusin.myaudioplayer.middlelayer;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mercury.gnusin.myaudioplayer.R;
import com.mercury.gnusin.myaudioplayer.model.AudioPlayerBinder;
import com.mercury.gnusin.myaudioplayer.model.AudioPlayerService;
import com.mercury.gnusin.myaudioplayer.view.PlayerControlActivity;

import java.util.ArrayList;
import java.util.List;


public class AudioPlayerPresenter {


    private PlayerControlActivity boundActivity;
    private AudioPlayerBinder audioPlayerBinder;
    private ServiceConnection serviceConnection;
    private List<BroadcastReceiver> eventReceiverList = new ArrayList<>();
    private boolean isStartService = false;
    //private


    public AudioPlayerPresenter(PlayerControlActivity activity) {
        boundActivity = activity;

    }

    private void startAudioPlayerService (final Context context) {
        context.startService(new Intent(context, AudioPlayerService.class));

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
        if (isStartService == false) {
            startAudioPlayerService(boundActivity);
        }

        switch (audioPlayerBinder.getPlayerState()) {
            case AudioPlayerService.State.PLAY:
                    pause();
                    break;
            case AudioPlayerService.State.PAUSE:
                    playContinue();
                    break;
            case AudioPlayerService.State.STOP:
                    playNewTrack();
                    break;
        }
    }

    public void stop() {
        audioPlayerBinder.stop();
        boundActivity.stopService(new Intent(boundActivity, AudioPlayerService.class));
        unbindActivity();
    }

    public void bindActivity(PlayerControlActivity activity) {
        if (activity != null) {
            boundActivity = activity;
            registrationEventReceivers(boundActivity);
            boundActivity.bindService(new Intent(activity, AudioPlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void unbindActivity() {
        if (boundActivity != null) {
            boundActivity.unbindService(serviceConnection);

            for (BroadcastReceiver receiver : eventReceiverList) {
                LocalBroadcastManager.getInstance(boundActivity).unregisterReceiver(receiver);
            }

            boundActivity.changeUIByState(PlayerControlActivity.StateUI.NO_BIND);

            boundActivity = null;
        }
    }


    private void playNewTrack() {
        boundActivity.changeUIByState(PlayerControlActivity.StateUI.PLAY);
        audioPlayerBinder.playNewTrack();
    }

    private void playContinue() {
        boundActivity.changeUIByState(PlayerControlActivity.StateUI.PLAY);
        audioPlayerBinder.playContinue();
    }

    private void pause() {
        boundActivity.changeUIByState(PlayerControlActivity.StateUI.PAUSE);
        audioPlayerBinder.pause();
    }

    private void registrationEventReceivers(Context context) {
        BroadcastReceiver connectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boundActivity.changeUIByState(convertStateServerToStateUI(audioPlayerBinder.getPlayerState()));
            }
        };
        eventReceiverList.add(connectReceiver);
        LocalBroadcastManager.getInstance(context).registerReceiver(connectReceiver, new IntentFilter(MiddleLayerEvents.CONNECT_SERVICE));

        BroadcastReceiver disconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boundActivity.changeUIByState(PlayerControlActivity.StateUI.NO_BIND);
                boundActivity.showErrorMessage(R.string.unready_audio_player_error_message);
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

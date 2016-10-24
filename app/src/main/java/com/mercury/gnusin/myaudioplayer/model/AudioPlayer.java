package com.mercury.gnusin.myaudioplayer.model;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;


public class AudioPlayer implements AudioPlayerInterface {

    private AudioPlayerService.ServiceBinder serviceBinder;

    private ServiceConnection serviceConnection;

    private @PlayerState
    int playerState = PlayerState.DISCONNECT;

    private BroadcastReceiver audioServiceReceiver;

    private LocalBroadcastManager broadcastManager;

    public AudioPlayer(Context context) {
        broadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void init(Context context) {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceBinder = (AudioPlayerService.ServiceBinder) service;

                switch (serviceBinder.getService().getStateService()) {
                    case AudioPlayerService.ServiceState.UNDEFINED:
                        changeState(PlayerState.CONNECT);
                        break;
                    case AudioPlayerService.ServiceState.PLAY:
                        changeState(PlayerState.PLAY);
                        break;
                    case AudioPlayerService.ServiceState.PAUSE:
                        changeState(PlayerState.PAUSE);
                        break;
                    case AudioPlayerService.ServiceState.STOP:
                        changeState(PlayerState.STOP);
                        break;
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                changeState(PlayerState.DISCONNECT);
            }
        };

        audioServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case AudioPlayerService.CHANGE_STATE_AUDIO_SERVICE_EVENT:
                        switch (serviceBinder.getService().getStateService()) {
                            case AudioPlayerService.ServiceState.UNDEFINED:
                                changeState(PlayerState.CONNECT);
                                break;
                            case AudioPlayerService.ServiceState.PLAY:
                                changeState(PlayerState.PLAY);
                                break;
                            case AudioPlayerService.ServiceState.PAUSE:
                                changeState(PlayerState.PAUSE);
                                break;
                            case AudioPlayerService.ServiceState.STOP:
                                changeState(PlayerState.STOP);
                                break;
                        }
                        break;
                    case AudioPlayerService.ERROR_AUDIO_SERVICE_EVENT:
                        Intent playerIntent = new Intent(ERROR_AUDIO_PLAYER_EVENT);
                        playerIntent.putExtra("error", intent.getStringExtra("error"));
                        broadcastManager.sendBroadcast(playerIntent);
                        break;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioPlayerService.CHANGE_STATE_AUDIO_SERVICE_EVENT);
        intentFilter.addAction(AudioPlayerService.ERROR_AUDIO_SERVICE_EVENT);
        broadcastManager.registerReceiver(audioServiceReceiver, intentFilter);

        context.bindService(new Intent(context, AudioPlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void playNewTrack(Context context) {
        context.startService(new Intent(context, AudioPlayerService.class));
    }

    @Override
    public void playContinue() {
        serviceBinder.getService().playContinue();
        changeState(PlayerState.PLAY);
    }

    @Override
    public void pause() {
        serviceBinder.getService().pause();
        changeState(PlayerState.PAUSE);
    }

    @Override
    public void stop() {
        serviceBinder.getService().stop();
        changeState(PlayerState.STOP);
    }

    @Override
    public @PlayerState
    int getPlayerState() {
        return playerState;
    }

    @Override
    public Bitmap getPlayingTrackCover(Context context) {
        return serviceBinder.getService().getTrackCover(context);
    }

    @Override
    public void release(Context context) {
        context.unbindService(serviceConnection);
        broadcastManager.unregisterReceiver(audioServiceReceiver);
    }

    private void changeState(@PlayerState int state) {
        playerState = state;
        broadcastManager.sendBroadcast(new Intent(CHANGE_STATE_AUDIO_PLAYER_EVENT));
    }
}

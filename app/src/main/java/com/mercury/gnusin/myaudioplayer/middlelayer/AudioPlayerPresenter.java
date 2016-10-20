package com.mercury.gnusin.myaudioplayer.middlelayer;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mercury.gnusin.myaudioplayer.R;
import com.mercury.gnusin.myaudioplayer.model.AudioPlayerBinder;
import com.mercury.gnusin.myaudioplayer.model.AudioPlayerService;
import com.mercury.gnusin.myaudioplayer.model.ModelEvents;
import com.mercury.gnusin.myaudioplayer.view.PlayerControlActivity;

import java.util.ArrayList;
import java.util.List;


public class AudioPlayerPresenter {

    private PlayerControlActivity boundActivity;
    private AudioPlayerBinder audioPlayerBinder;
    private ServiceConnection serviceConnection;
    private List<BroadcastReceiver> eventReceiverList = new ArrayList<>();


    public AudioPlayerPresenter(final Context context) {
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

    public void playPause() {
        switch (audioPlayerBinder.getPlayerState()) {
            case AudioPlayerService.State.PLAY:
                    audioPlayerBinder.pause();
                    break;
            case AudioPlayerService.State.PAUSE:
                    audioPlayerBinder.playContinue();
                    break;
            case AudioPlayerService.State.STOP:
                    audioPlayerBinder.playNewTrack();
                    break;
        }
    }

    public void stop() {
        audioPlayerBinder.stop();
        boundActivity.stopService(new Intent(boundActivity, AudioPlayerService.class));
    }

    private void registrationEventReceivers(Context context) {
        final BroadcastReceiver connectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (boundActivity != null) {
                    boundActivity.changeUIByState(convertStateServerToStateUI(audioPlayerBinder.getPlayerState()));
                    Bitmap cover = audioPlayerBinder.getPlayingTrackCover();
                    if (cover != null) {
                        boundActivity.setCover(cover);
                    }
                }
            }
        };
        eventReceiverList.add(connectReceiver);
        LocalBroadcastManager.getInstance(context).registerReceiver(connectReceiver, new IntentFilter(MiddleLayerEvents.CONNECT_SERVICE));

        BroadcastReceiver disconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (boundActivity != null) {
                    boundActivity.changeUIByState(PlayerControlActivity.StateUI.NO_BIND);
                    boundActivity.showErrorMessage(boundActivity.getResources().getString(R.string.init_audio_player_error_message));
                }
            }
        };
        eventReceiverList.add(disconnectReceiver);
        LocalBroadcastManager.getInstance(context).registerReceiver(disconnectReceiver, new IntentFilter(MiddleLayerEvents.DISCONNECT_SERVICE));

        BroadcastReceiver changeStatePlayerEvent = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (boundActivity != null) {
                    @AudioPlayerService.State int playerState = audioPlayerBinder.getPlayerState();
                    boundActivity.changeUIByState(convertStateServerToStateUI(playerState));
                    if (playerState == AudioPlayerService.State.PLAY) {
                        Bitmap cover = audioPlayerBinder.getPlayingTrackCover();
                        if (cover != null) {
                            boundActivity.setCover(cover);
                        }
                    }
                }
            }
        };
        eventReceiverList.add(changeStatePlayerEvent);
        LocalBroadcastManager.getInstance(context).registerReceiver(changeStatePlayerEvent, new IntentFilter(ModelEvents.CHANGE_STATE_EVENT));

        BroadcastReceiver errorPlayerEvent = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (boundActivity != null) {
                    String errorMessage = intent.getStringExtra("error");
                    boundActivity.showErrorMessage(String.format(boundActivity.getResources().getString(R.string.run_audio_player_error_message), errorMessage));
                }
            }
        };
        eventReceiverList.add(errorPlayerEvent);
        LocalBroadcastManager.getInstance(context).registerReceiver(errorPlayerEvent, new IntentFilter(ModelEvents.ERROR_EVENT));
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

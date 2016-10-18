package com.mercury.gnusin.myaudioplayer.model;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.IntDef;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.mercury.gnusin.myaudioplayer.view.PlayerControlActivity;
import com.mercury.gnusin.myaudioplayer.R;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    @IntDef({State.PLAY, State.PAUSE, State.STOP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
        int PLAY = 0;
        int PAUSE = 1;
        int STOP = 2;
    }

    private @AudioPlayerService.State int playerState = State.STOP;

    private final String audioFilePath = "android.resource://com.mercury.gnusin.myaudioplayer/raw/basta";

    private MediaPlayer mediaPlayer;

    private Notification notification;

    public @State int getPlayerState() {
        return playerState;
    }

    public void playContinue() {
        mediaPlayer.start();
        playerState = State.PLAY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("AGn", "onCreate() service");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);

        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                                                                0,
                                                                new Intent(getApplicationContext(), PlayerControlActivity.class),
                                                                PendingIntent.FLAG_UPDATE_CURRENT);

        notification = new NotificationCompat.Builder(getApplicationContext())
                .setTicker("Ticker")
                .setContentTitle("Title")
                .setContentText("Text")
                .setSmallIcon(R.mipmap.audio_player)
                .setContentIntent(pendingIntent)
                .build();
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AGn", "onStartCommand() service");
        return START_NOT_STICKY;
    }

    public void playNewTrack() {
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(audioFilePath));
            notification.tickerText = audioFilePath.substring(audioFilePath.lastIndexOf("/") + 1, audioFilePath.length());
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace(); // TODO fire event
        }
    }

    public void pause() {
        mediaPlayer.pause();
        playerState = State.PAUSE;
    }

    public void stop() {
        Log.d("AGn", "stop() service");
        mediaPlayer.stop();
        playerState = State.STOP;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("AGn", "onBind() service");
        return new AudioPlayerBinder(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("AGn", "onUnbind() service");
        return super.onUnbind(intent);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
        playerState = State.PLAY;
    }

    @Override
    public void onDestroy() {
        Log.d("AGn", "onDestroy() service");
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            stopForeground(true);
        }
        super.onDestroy();

    }
}

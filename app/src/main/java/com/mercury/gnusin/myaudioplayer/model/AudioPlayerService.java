package com.mercury.gnusin.myaudioplayer.model;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import com.mercury.gnusin.myaudioplayer.R;
import com.mercury.gnusin.myaudioplayer.view.PlayerControlActivity_;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_STOP = "action_stop";



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

    //private MediaSessionManager  mediaSessionManager;

    //private MediaSessionCompat mediaSessionCompat;

    private Notification notification;

    private Uri playingTrack;

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
        playerState = State.PLAY;
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(ModelEvents.CHANGE_STATE_EVENT));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }



    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AGn", "onStartCommand() service");

        if (playingTrack == null) {
            Intent errorIntent = new Intent(ModelEvents.ERROR_EVENT);
            errorIntent.putExtra("error", getResources().getString(R.string.no_set_track_error_message));
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(errorIntent);
        } else {
            try {
                //mediaSessionCompat = new MediaSessionCompat();

                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setOnErrorListener(this);


                mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

                mediaPlayer.setDataSource(getApplicationContext(), playingTrack);
                mediaPlayer.prepare();

                Intent notificationIntent = new Intent(getApplicationContext(), PlayerControlActivity_.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle(getResources().getString(R.string.title_notification))
                        .setContentText(String.format(getResources().getString(R.string.text_notification), playingTrack.getPath().substring(playingTrack.getPath().lastIndexOf("/") + 1, playingTrack.getPath().length())))
                        .setSmallIcon(R.mipmap.audio_player)
                        .setLargeIcon(getPlayingTrackCover())
                        .setContentIntent(pendingIntent)
                        //.setStyle(new NotificationCompat.MediaStyle()
                        .build();
                notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

                startForeground(1, notification);
            } catch (IOException e) {
                Intent errorIntent = new Intent(ModelEvents.ERROR_EVENT);
                errorIntent.putExtra("error", e.toString());
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(errorIntent);
            }
        }

        return START_NOT_STICKY;
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

    public void playNewTrack() {
        playingTrack = Uri.parse(audioFilePath);
        startService(new Intent(getApplicationContext(), AudioPlayerService.class));
    }

    public void playContinue() {
        mediaPlayer.start();
        playerState = State.PLAY;
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(ModelEvents.CHANGE_STATE_EVENT));
    }

    public void pause() {
        mediaPlayer.pause();
        playerState = State.PAUSE;
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(ModelEvents.CHANGE_STATE_EVENT));
    }

    public void stop() {
        Log.d("AGn", "stop() service");
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        playerState = State.STOP;
        stopForeground(true);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(ModelEvents.CHANGE_STATE_EVENT));
    }

    public @State int getPlayerState() {
        return playerState;
    }

    public Bitmap getPlayingTrackCover() {
        if (playingTrack != null) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(getApplicationContext(), playingTrack);

            byte[] buffer = retriever.getEmbeddedPicture();

            if (buffer != null) {
                return BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
            }
        }

        return null;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(ModelEvents.ERROR_EVENT));
        return false;
    }

    @Override
    public void onDestroy() {
        Log.d("AGn", "onDestroy() service");
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}

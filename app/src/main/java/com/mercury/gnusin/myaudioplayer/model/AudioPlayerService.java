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
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import com.mercury.gnusin.myaudioplayer.R;
import com.mercury.gnusin.myaudioplayer.view.PlayerControlActivity_;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    @IntDef({ServiceState.PLAY, ServiceState.PAUSE, ServiceState.STOP, ServiceState.UNDEFINED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ServiceState {
        int PLAY = 0;
        int PAUSE = 1;
        int STOP = 2;
        int UNDEFINED = 3;
    }

    class ServiceBinder extends Binder {
        AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    public static final String START_PLAY_TRACK_EVENT = "start_play_track_event";
    public static final String ERROR_AUDIO_SERVICE_EVENT = "error_audio_service_event";

    private MediaPlayer mediaPlayer;

    private ServiceBinder binder =  new ServiceBinder();

    private Notification notification;

    private String audioFileUri = "android.resource://com.mercury.gnusin.myaudioplayer/raw/africa_with_cover";

    private @ServiceState
    int stateService = ServiceState.UNDEFINED;

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
        stateService = ServiceState.PLAY;
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(START_PLAY_TRACK_EVENT));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Uri trackUri = Uri.parse(audioFileUri);
            if (trackUri == null) {
                Intent errorIntent = new Intent(ERROR_AUDIO_SERVICE_EVENT);
                errorIntent.putExtra("error", getResources().getString(R.string.no_set_track_error_message));
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(errorIntent);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnErrorListener(this);


            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            mediaPlayer.setDataSource(getApplicationContext(), trackUri);
            mediaPlayer.prepare();

            Intent notificationIntent = new Intent(getApplicationContext(), PlayerControlActivity_.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            notification = new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle(getResources().getString(R.string.title_notification))
                    .setContentText(String.format(getResources().getString(R.string.text_notification), trackUri.getPath().substring(trackUri.getPath().lastIndexOf("/") + 1, trackUri.getPath().length())))
                    .setSmallIcon(R.mipmap.audio_player)
                    .setLargeIcon(getTrackCover(getApplicationContext()))
                    .setContentIntent(pendingIntent)
                    //.setStyle(new NotificationCompat.MediaStyle()
                    .build();
            notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

            startForeground(1, notification);
        } catch (IOException e) {
            Intent errorIntent = new Intent(ERROR_AUDIO_SERVICE_EVENT);
            errorIntent.putExtra("error", e.toString());
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(errorIntent);
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void playContinue() {
        mediaPlayer.start();
        stateService = ServiceState.PLAY;
    }

    public void pause() {
        mediaPlayer.pause();
        stateService = ServiceState.PAUSE;
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
        stateService = ServiceState.STOP;
    }

    public @ServiceState
    int getStateService() {
        return stateService;
    }

    public Bitmap getTrackCover(Context context) {
        Uri uri = Uri.parse(audioFileUri);
        if (uri != null) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, uri);

            byte[] buffer = retriever.getEmbeddedPicture();

            if (buffer != null) {
                return BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
            }
        }

        return null;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(ERROR_AUDIO_SERVICE_EVENT));
        return false;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}

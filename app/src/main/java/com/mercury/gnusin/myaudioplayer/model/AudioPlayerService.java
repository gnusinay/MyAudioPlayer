package com.mercury.gnusin.myaudioplayer.model;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    public static final String CHANGE_STATE_AUDIO_SERVICE_EVENT = "change_state_audio_service_event";
    public static final String ERROR_AUDIO_SERVICE_EVENT = "error_audio_service_event";

    private static final String ACTION_PLAY = "action_play";
    private static final String ACTION_PAUSE = "action_pause";
    private static final String ACTION_STOP = "action_stop";


    private MediaPlayer mediaPlayer;

    private BroadcastReceiver actionNotificationReceiver;

    private ServiceBinder binder =  new ServiceBinder();

    private Notification notification;

    private String audioFileUri = "android.resource://com.mercury.gnusin.myaudioplayer/raw/africa_with_cover";

    private @ServiceState int stateService = ServiceState.UNDEFINED;


    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
        stateService = ServiceState.PLAY;
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(CHANGE_STATE_AUDIO_SERVICE_EVENT));

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
            } else {


                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setOnErrorListener(this);


                mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

                mediaPlayer.setDataSource(getApplicationContext(), trackUri);
                mediaPlayer.prepare();

                notification = buildNotification(trackUri);

                actionNotificationReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        switch (intent.getAction()) {
                            case ACTION_PLAY:
                                playContinue();
                                break;
                            case ACTION_PAUSE:
                                pause();
                                break;
                            case ACTION_STOP:
                                stop();
                                break;
                        }
                    }
                };
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ACTION_PLAY);
                intentFilter.addAction(ACTION_PAUSE);
                intentFilter.addAction(ACTION_STOP);
                registerReceiver(actionNotificationReceiver, intentFilter);

                startForeground(1, notification);
            }
        } catch(IOException e){
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
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(CHANGE_STATE_AUDIO_SERVICE_EVENT));
    }

    public void pause() {
        mediaPlayer.pause();
        stateService = ServiceState.PAUSE;
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(CHANGE_STATE_AUDIO_SERVICE_EVENT));
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
        unregisterReceiver(actionNotificationReceiver);
        stateService = ServiceState.STOP;
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(CHANGE_STATE_AUDIO_SERVICE_EVENT));
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

    private Notification buildNotification(Uri trackUri) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);

        PendingIntent playIntent = PendingIntent.getBroadcast(this, 100, new Intent(ACTION_PLAY), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.addAction(new NotificationCompat.Action(R.mipmap.play_button_notification, "Play", playIntent));

        PendingIntent pauseIntent = PendingIntent.getBroadcast(this, 100, new Intent(ACTION_PAUSE), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.addAction(new NotificationCompat.Action(R.mipmap.pause_button_notification, "Pause", pauseIntent));

        PendingIntent stopIntent = PendingIntent.getBroadcast(this, 100, new Intent(ACTION_STOP), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.addAction(new NotificationCompat.Action(R.mipmap.stop_button_notification, "Stop", stopIntent));

        Intent notificationIntent = new Intent(getApplicationContext(), PlayerControlActivity_.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);


        notificationBuilder
                .setContentTitle(getResources().getString(R.string.title_notification))
                .setContentText(String.format(getResources().getString(R.string.text_notification), trackUri.getPath().substring(trackUri.getPath().lastIndexOf("/") + 1, trackUri.getPath().length())))
                .setSmallIcon(R.mipmap.audio_player)
                .setLargeIcon(getTrackCover(getApplicationContext()))
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.MediaStyle());

        Notification notif = notificationBuilder.build();
        notif.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        return notif;
    }
}

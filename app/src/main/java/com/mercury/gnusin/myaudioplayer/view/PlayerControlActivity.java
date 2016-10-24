package com.mercury.gnusin.myaudioplayer.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.mercury.gnusin.myaudioplayer.R;
import com.mercury.gnusin.myaudioplayer.model.AudioPlayer;
import com.mercury.gnusin.myaudioplayer.model.AudioPlayerInterface;
import com.mercury.gnusin.myaudioplayer.presenters.AudioPlayerPresenter;
import com.mercury.gnusin.myaudioplayer.presenters.AudioPlayerPresenterInterface;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SeekBarProgressChange;
import org.androidannotations.annotations.ViewById;


@EActivity(R.layout.a_player_control)
public class PlayerControlActivity extends AppCompatActivity implements AudioPlayerViewInterface {

    @ViewById(R.id.play_pause_button)
    ImageButton playPauseButton;

    @ViewById(R.id.stop_button)
    ImageButton stopButton;

    @ViewById(R.id.album_cover)
    ImageView albumCover;

    @ViewById(R.id.volume_state_icon)
    ImageView volumeStateIcon;

    @ViewById(R.id.volume_bar)
    SeekBar volumeBar;


    private AudioPlayerPresenterInterface playerPresenter;

    private AudioManager audioManager;

    private BroadcastReceiver volumeChangeReceiver;

    @AfterViews
    void init() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        volumeBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumeBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        volumeStateIcon.setImageResource(getVolumeStateIcon());

        playerPresenter = (AudioPlayerPresenter) getLastCustomNonConfigurationInstance();
        if (playerPresenter == null) {
            playerPresenter = new AudioPlayerPresenter(this, new AudioPlayer(getApplicationContext()));
        }

        volumeChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                volumeBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                volumeStateIcon.setImageResource(getVolumeStateIcon());
            }
        };
        registerReceiver(volumeChangeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));

        playerPresenter.bindView(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            int newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - 1;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
            volumeBar.setProgress(newVolume);
            volumeStateIcon.setImageResource(getVolumeStateIcon());
            return true;
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            int newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + 1;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
            volumeBar.setProgress(newVolume);
            volumeStateIcon.setImageResource(getVolumeStateIcon());
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return playerPresenter;
    }

    @Click(R.id.play_pause_button)
    void playPauseButtonClick() {
        playerPresenter.playPause();
    }

    @Click(R.id.stop_button)
    void stopButtonClick() {
        playerPresenter.stop();
    }

    @SeekBarProgressChange(R.id.volume_bar)
    void volumeBarProgressChange(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            volumeStateIcon.setImageResource(getVolumeStateIcon());
        }
    }

    @Override
    public void changeUIByPlayerState(@AudioPlayerInterface.PlayerState int playerState) {
        switch (playerState) {
            case AudioPlayerInterface.PlayerState.CONNECT:
                playPauseButton.setImageResource(R.mipmap.play_button);
                stopButton.setImageResource(R.mipmap.stop_button);
                albumCover.setImageResource(R.mipmap.cover_default);
                break;
            case AudioPlayerInterface.PlayerState.PLAY:
                playPauseButton.setImageResource(R.mipmap.pause_button);
                stopButton.setImageResource(R.mipmap.stop_button);
                break;
            case AudioPlayerInterface.PlayerState.PAUSE:
                playPauseButton.setImageResource(R.mipmap.play_button);
                stopButton.setImageResource(R.mipmap.stop_button);
                break;
            case AudioPlayerInterface.PlayerState.STOP:
                playPauseButton.setImageResource(R.mipmap.play_button);
                stopButton.setImageResource(R.mipmap.stop_button);
                albumCover.setImageResource(R.mipmap.cover_default);
                break;
        }
    }

    @Override
    public void setCover(Bitmap cover) {
        albumCover.setImageBitmap(cover);
    }

    @Override
    public void showErrorMessage(String message) {
        Snackbar.make(findViewById(R.id.a_player_control), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    private int getVolumeStateIcon() {
        if (volumeBar.getProgress() == 0) {
            return R.mipmap.volume_off;
        }

        if (volumeBar.getProgress() <= 3) {
            return R.mipmap.volume_min;
        }

        if (volumeBar.getProgress() <= 7) {
            return R.mipmap.volume_middle;
        }

        return R.mipmap.volume_max;
    }

    @Override
    public void stopApp() {
        finish();
    }

    @Override
    protected void onDestroy() {
        playerPresenter.unbindView();
        unregisterReceiver(volumeChangeReceiver);
        super.onDestroy();
    }
}

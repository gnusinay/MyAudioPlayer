package com.mercury.gnusin.myaudioplayer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.support.annotation.IntDef;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.mercury.gnusin.myaudioplayer.R;
import com.mercury.gnusin.myaudioplayer.middlelayer.AudioPlayerPresenter;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SeekBarProgressChange;
import org.androidannotations.annotations.ViewById;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@EActivity(R.layout.a_player_control)
public class PlayerControlActivity extends AppCompatActivity {

    @IntDef({StateUI.NO_BIND, StateUI.PLAY, StateUI.PAUSE, StateUI.STOP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StateUI {
        int NO_BIND = 0;
        int PLAY = 1;
        int PAUSE = 2;
        int STOP = 3;
    }

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


    private AudioPlayerPresenter playerPresenter;

    @AfterViews
    void init() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        volumeBar.setMax(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumeBar.setProgress(am.getStreamVolume(AudioManager.STREAM_MUSIC));
        volumeStateIcon.setImageResource(getVolumeStateIcon());

        changeUIByState(StateUI.NO_BIND);

        playerPresenter = (AudioPlayerPresenter) getLastCustomNonConfigurationInstance();
        if (playerPresenter == null) {
            playerPresenter = new AudioPlayerPresenter(getApplicationContext());
        }
        playerPresenter.bindActivity(this);
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
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            volumeStateIcon.setImageResource(getVolumeStateIcon());
        }
    }

    public void changeUIByState(@StateUI int stateUI) {
        switch (stateUI) {
            case StateUI.NO_BIND:
                playPauseButton.setImageResource(R.mipmap.play_button);
                stopButton.setImageResource(R.mipmap.stop_button);
                albumCover.setImageResource(R.mipmap.cover_default);
                break;
            case StateUI.PLAY:
                playPauseButton.setImageResource(R.mipmap.pause_button);
                stopButton.setImageResource(R.mipmap.stop_button);
                break;
            case StateUI.PAUSE:
                playPauseButton.setImageResource(R.mipmap.play_button);
                stopButton.setImageResource(R.mipmap.stop_button);
                break;
            case StateUI.STOP:
                playPauseButton.setImageResource(R.mipmap.play_button);
                stopButton.setImageResource(R.mipmap.stop_button);
                albumCover.setImageResource(R.mipmap.cover_default);
                break;
        }
    }

    public void setCover(Bitmap cover) {
        albumCover.setImageBitmap(cover);
    }

    public void showErrorMessage(String message) {
        Snackbar.make(findViewById(R.id.a_player_control), message, Snackbar.LENGTH_SHORT).show();
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
    protected void onDestroy() {
        playerPresenter.unbindActivity();
        super.onDestroy();
    }
}

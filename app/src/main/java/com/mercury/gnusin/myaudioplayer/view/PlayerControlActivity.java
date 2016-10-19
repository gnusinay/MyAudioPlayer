package com.mercury.gnusin.myaudioplayer.view;

import android.support.annotation.IntDef;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageButton;
import com.mercury.gnusin.myaudioplayer.R;
import com.mercury.gnusin.myaudioplayer.middlelayer.AudioPlayerPresenter;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
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

    private AudioPlayerPresenter playerPresenter;

    @AfterViews
    void init() {
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

    public void changeUIByState(@StateUI int stateUI) {
        switch (stateUI) {
            case StateUI.NO_BIND:
                playPauseButton.setImageResource(R.mipmap.play_button);
                stopButton.setImageResource(R.mipmap.stop_button);
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
                break;
        }
    }

    public void showErrorMessage(String message) {
        Snackbar.make(findViewById(R.id.a_player_control), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        playerPresenter.unbindActivity();
        super.onDestroy();
    }
}

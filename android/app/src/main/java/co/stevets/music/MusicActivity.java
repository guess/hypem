package co.stevets.music;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import co.stevets.music.fragments.FavouriteFragment;
import co.stevets.music.fragments.LoadFragment;
import co.stevets.music.fragments.NowPlayingFragment;
import co.stevets.music.fragments.ShuffleFragment;
import co.stevets.music.models.MusicProvider;
import co.stevets.music.network.MusicService;
import co.stevets.music.utils.Common;


public class MusicActivity extends Activity implements NowPlayingFragment.OnNowPlayingInteraction,
        ShuffleFragment.OnSongsDownloadedListener {

    private static final String TAG = "MusicActivity";

    private static final String DOWNLOAD = "download";

    private Common mApp;

    private SlidingUpPanelLayout mSlidingPanel;

    private MediaController mController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate. savedInstanceState=" + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        if (savedInstanceState == null) {
            // Switch to the now playing fragment
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new LoadFragment())
                    .commitAllowingStateLoss();
        }
        mApp = (Common) getApplicationContext();

        // Start the music service
        startService(new Intent(this, MusicService.class));

        mSlidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingPanel.setPanelSlideListener(new SlidingListener());

        // Initialize the media controller
        mController = new MediaController(this, mApp.getSessionToken());
    }


    @Override
    protected void onResume() {
        super.onResume();
        MusicProvider provider = mApp.getProvider();
        if (provider == null || !provider.isInitialized()) {
            startDownload();
        }
    }


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopService(new Intent(this, MusicService.class));
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        if (mSlidingPanel != null &&
                (mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ||
                        mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }


    // ******** Communication with fragments:


    @Override
    public void onButtonPressed(int action) {
        Log.d(TAG, "onButtonPressed. action=" + action);

        if (mController == null || mController.getPlaybackState() == null) {
            Log.d(TAG, "onButtonPressed. Cancelled action!");
            return;
        }

        switch (action) {
            case PLAY_PAUSE:
                if (mController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
                    mController.getTransportControls().pause();
                } else {
                    mController.getTransportControls().play();
                }
                break;
            case PREV:
                mController.getTransportControls().skipToPrevious();
                break;
            case NEXT:
                Log.d(TAG, "Request to play next song");
                mController.getTransportControls().skipToNext();
                break;
            case FAV:
                mController.getTransportControls()
                        .sendCustomAction(Common.ACTION_FAV, null);
                break;
            default:
                // Not sure what this action is
        }
    }


    private void startPlayer() {
        Log.d(TAG, "startPlayer");
        if (mApp.getProvider() == null || !mApp.getProvider().isInitialized()) {
            Log.d(TAG, "startPlayer. Songs not downloaded!");
            return;
        }

        mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

        // Switch to the now playing fragment
        getFragmentManager().beginTransaction()
                .replace(R.id.container, new FavouriteFragment())
                .replace(R.id.now_playing, new NowPlayingFragment())
                .commitAllowingStateLoss();
    }


    public void startDownload() {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, new LoadFragment())
                .add(new ShuffleFragment(), DOWNLOAD)
                .commit();
    }


    @Override
    public void onSongsDownloaded() {
        Log.d(TAG, "onSongsDownloaded");
        startPlayer();
    }


    // ********* Sliding panel:

    class SlidingListener implements SlidingUpPanelLayout.PanelSlideListener {

        @Override
        public void onPanelSlide(View view, float v) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                if (v > 0.8) {
                    actionBar.hide();
                } else {
                    actionBar.show();
                }
            }
        }

        @Override
        public void onPanelCollapsed(View view) {
            Log.d(TAG, "onPanelCollapsed");
            // Show play/pause button
        }

        @Override
        public void onPanelExpanded(View view) {
            Log.d(TAG, "onPanelExpanded");
            // Hide play/pause button
        }

        @Override
        public void onPanelAnchored(View view) {
            Log.d(TAG, "onPanelAnchored");
        }

        @Override
        public void onPanelHidden(View view) {
            Log.d(TAG, "onPanelHidden");
        }
    }
}

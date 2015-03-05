package co.stevets.music;

import android.app.Activity;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import co.stevets.music.fragments.LoadFragment;
import co.stevets.music.fragments.NowPlayingFragment;
import co.stevets.music.fragments.SongDownloadFragment;
import co.stevets.music.models.MusicProvider;
import co.stevets.music.network.MusicService;
import co.stevets.music.utils.Common;


public class MusicActivity extends Activity implements NowPlayingFragment.OnNowPlayingInteraction,
        SongDownloadFragment.OnSongsDownloadedListener {

    private static final String TAG = "MusicActivity";

    private static final String DOWNLOAD = "download";

    private Common mApp;

    private MediaController mController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate. savedInstanceState=" + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new LoadFragment())
                    .commit();
        }
        mApp = (Common) getApplicationContext();

        // Start the music service
        startService(new Intent(this, MusicService.class));

        // Initialize the media controller
        mController = new MediaController(this, mApp.getSessionToken());
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicProvider provider = mApp.getProvider();
        if (provider == null || !provider.isInitialized()) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new LoadFragment())
                    .add(new SongDownloadFragment(), DOWNLOAD)
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopService(new Intent(this, MusicService.class));
        super.onDestroy();
    }

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
                Toast.makeText(mApp, "Added to favourites", Toast.LENGTH_SHORT).show();
                break;
            default:
                // Not sure what this action is
        }
    }

    @Override
    public void onSongsDownloaded() {
        Log.d(TAG, "onSongsDownloaded");

        if (mApp.getProvider() == null || !mApp.getProvider().isInitialized()) {
            Toast.makeText(mApp, "Something went wrong.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Switch to the now playing fragment
        getFragmentManager().beginTransaction()
                .replace(R.id.container, new NowPlayingFragment())
                .commitAllowingStateLoss();
    }
}

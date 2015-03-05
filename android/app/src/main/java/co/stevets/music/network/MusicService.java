package co.stevets.music.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import co.stevets.music.R;
import co.stevets.music.models.MusicProvider;
import co.stevets.music.utils.Common;
import co.stevets.music.utils.QueueHelper;


public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "MusicService";

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;

    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    private MediaSession mSession;
    private MediaPlayer mMediaPlayer;

    // "Now playing" queue:
    private List<MediaSession.QueueItem> mPlayingQueue;
    private int mCurrentIndexOnQueue;

    // Current local media player state
    private int mState = PlaybackState.STATE_NONE;

    // Wifi lock that we hold when streaming files from the internet, in order
    // to prevent the device from shutting off the Wifi radio
    private WifiManager.WifiLock mWifiLock;

    public MediaNotification mMediaNotification;

    enum AudioFocus {
        NoFocusNoDuck,  // we don't have audio focus, and can't duck
        NoFocusCanDuck, // we don't have focus, but can play at a low volume (i.e., ducking)
        Focused         // we have full audio focus
    }

    // Music catalog manager
    private MusicProvider mMusicProvider;

    // Type of audio focus we have:
    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
    private AudioManager mAudioManager;

    // Indicates if we should start playing immediately after we gain focus.
    private boolean mPlayOnFocusGain;

    // Media session token
    private MediaSession.Token mToken;

    // Handler to send updates on song position
    private Handler mHandler = new Handler();
    private Intent mSeekIntent;
    // TODO: Move to Common
    public static final String BROADCAST_SEEK_PROGRESS = "co.stevets.music.seekprogress";
    public static final String POSITION = "position";
    public static final String DURATION = "duration";


    public MusicService() {
    }


    /*
     * (non-Javadoc)
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        Common app = (Common) getApplicationContext();

        mPlayingQueue = new ArrayList<>();

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "HypeShuffle_Lock");

        // Get the music catalog metadata provider
        mMusicProvider = app.getProvider();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Start a new MediaSession
        mSession = app.getSession();
        mToken = mSession.getSessionToken();
        mSession.setCallback(new MediaSessionCallback());
        updatePlaybackState(null);

        mSeekIntent = new Intent(BROADCAST_SEEK_PROGRESS);

        mMediaNotification = new MediaNotification(this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setupHandler();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // Service is being killed, so make sure we release our resources
        handleStopRequest(null);

        // Clean up the handler
        mHandler.removeCallbacks(mPositionUpdater);

        // In particular, always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        mSession.release();
    }


    private void setupHandler() {
        mHandler.removeCallbacks(mPositionUpdater);
        mHandler.postDelayed(mPositionUpdater, 100);
    }

    private Runnable mPositionUpdater = new Runnable() {
        @Override
        public void run() {
            logMediaPosition();
            mHandler.postDelayed(mPositionUpdater, 100);
        }
    };

    private void logMediaPosition() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int position = mMediaPlayer.getCurrentPosition();
            int max = mMediaPlayer.getDuration();

            mSeekIntent.putExtra(POSITION, position);
            mSeekIntent.putExtra(DURATION, max);
            sendBroadcast(mSeekIntent);
        }
    }


    public MediaSession.Token getSessionToken() {
        return mToken;
    }


    // *********  MediaSession.Callback implementation:
    
    private final class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public void onPlay() {
            Log.d(TAG, "play");

            if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
                mPlayingQueue = QueueHelper.getRandomQueue(mMusicProvider);
                mSession.setQueue(mPlayingQueue);
                mSession.setQueueTitle(getString(R.string.random_queue_title));
                // start playing from the beginning of the queue
                mCurrentIndexOnQueue = 0;
            }

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            Log.d(TAG, "OnSkipToQueueItem:" + queueId);
            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {

                // set the current index on queue from the music Id:
                mCurrentIndexOnQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, queueId);

                // play the music
                handlePlayRequest();
            }
        }

        @Override
        public void onPause() {
            Log.d(TAG, "pause. current state=" + mState);
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            Log.d(TAG, "stop. current state=" + mState);
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "skipToNext");
            mCurrentIndexOnQueue++;
            if (mPlayingQueue != null && mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                mState = PlaybackState.STATE_PLAYING;
                handlePlayRequest();
            } else {
                Log.e(TAG, "skipToNext: cannot skip to next. next Index=" +
                        mCurrentIndexOnQueue + " queue length=" +
                        (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "skipToPrevious");

            mCurrentIndexOnQueue--;
            if (mPlayingQueue != null && mCurrentIndexOnQueue < 0) {
                // Skipping to previous when in first song restarts the first song.
                mCurrentIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                mState = PlaybackState.STATE_PLAYING;
                handlePlayRequest();
            } else {
                Log.e(TAG, "skipToPrevious: cannot skip to previous. previous Index=" +
                        mCurrentIndexOnQueue + " queue length=" +
                        (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onSeekTo(long pos) {
            Log.d(TAG, "seekTo " + pos/1000 + " seconds");
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            if (Common.ACTION_FAV.equals(action)) {
                Log.i(TAG, "onCustomAction: favorite for current track");
                MediaMetadata track = getCurrentPlayingMusic();
                if (track != null) {
                    String mediaId = track.getString(Common.METADATA_MEDIA_ID);
                    mMusicProvider.setFavorite(mediaId, !mMusicProvider.isFavorite(mediaId));
                }
                updatePlaybackState(null);
            } else {
                Log.e(TAG, "Unsupported action: " + action);
            }

        }
    }


    // *********  MediaPlayer listeners:

    /*
     * Called when media player is done playing current song.
     * @see android.media.MediaPlayer.OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        Log.d(TAG, "onCompletion from MediaPlayer");
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
            // Restart the playing queue when it gets to the end:
            mCurrentIndexOnQueue++;
            if (mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            handlePlayRequest();
        } else {
            // If there is nothing to play, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    /*
     * Called when media player is done preparing.
     * @see android.media.MediaPlayer.OnPreparedListener
     */
    @Override
    public void onPrepared(MediaPlayer player) {
        Log.d(TAG, "onPrepared from MediaPlayer");
        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        configMediaPlayerState();
    }

    /**
     * Called when there's an error playing media. When this happens, the media
     * player goes to the Error state. We warn the user about the error and
     * reset the media player.
     *
     * @see android.media.MediaPlayer.OnErrorListener
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Media player error: what=" + what + ", extra=" + extra);
        handleStopRequest("MediaPlayer error " + what + " (" + extra + ")");
        return true; // true indicates we handled the error
    }


    // *********  OnAudioFocusChangeListener listener:


    /**
     * Called by AudioManager on audio focus changes.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "onAudioFocusChange. focusChange=" + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AudioFocus.Focused;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mState == PlaybackState.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            Log.e(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
        }

        configMediaPlayerState();
    }


    // *********  private methods:

    /**
     * Handle a request to play music
     */
    private void handlePlayRequest() {
        Log.d(TAG, "handlePlayRequest: mState=" + mState);

        mPlayOnFocusGain = true;
        tryToGetAudioFocus();

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        // Actually play the song
        if (mState == PlaybackState.STATE_PAUSED) {
            // If we're paused, just continue playback and restore the
            // 'foreground service' state.
            configMediaPlayerState();
        } else {
            // If we're stopped or playing a song,
            // just go ahead to the new song and (re)start playing
            playCurrentSong();
        }
    }


    /**
     * Handle a request to pause music
     */
    private void handlePauseRequest() {
        Log.d(TAG, "handlePauseRequest: mState=" + mState);

        if (mState == PlaybackState.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            mState = PlaybackState.STATE_PAUSED;
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            // while paused, retain the MediaPlayer but give up audio focus
            relaxResources(false);
            giveUpAudioFocus();
        }
        updatePlaybackState(null);
    }

    /**
     * Handle a request to stop music
     */
    private void handleStopRequest(String withError) {
        Log.d(TAG, "handleStopRequest: mState=" + mState + " error=" + withError);
        mState = PlaybackState.STATE_STOPPED;

        // let go of all resources...
        relaxResources(true);
        giveUpAudioFocus();
        updatePlaybackState(withError);

        mMediaNotification.stopNotification();

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *            be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        Log.d(TAG, "relaxResources. releaseMediaPlayer=" + releaseMediaPlayer);
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    private void configMediaPlayerState() {
        Log.d(TAG, "configAndStartMediaPlayer. mAudioFocus=" + mAudioFocus);
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackState.STATE_PLAYING) {
                handlePauseRequest();
            }
        } else {  // we have audio focus:
            if (mAudioFocus == AudioFocus.NoFocusCanDuck) {
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
            } else {
                mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (!mMediaPlayer.isPlaying()) {
                    Log.d(TAG, "configAndStartMediaPlayer startMediaPlayer.");
                    mMediaPlayer.start();
                }
                mPlayOnFocusGain = false;
                mState = PlaybackState.STATE_PLAYING;
            }
        }
        updatePlaybackState(null);
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded() {
        Log.d(TAG, "createMediaPlayerIfNeeded. needed? " + (mMediaPlayer==null));
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    /**
     * Starts playing the current song in the playing queue.
     */
    void playCurrentSong() {
        MediaMetadata track = getCurrentPlayingMusic();
        if (track == null) {
            Log.e(TAG, "playSong:  ignoring request to play next song, because cannot" +
                    " find it." +
                    " currentIndex=" + mCurrentIndexOnQueue +
                    " playQueue.size=" + (mPlayingQueue==null?"null": mPlayingQueue.size()));
            return;
        }
        String source = track.getString(Common.METADATA_SOURCE);
        Log.d(TAG, "playSong:  current (" + mCurrentIndexOnQueue + ") in playingQueue. " +
                " musicId=" + track.getString(Common.METADATA_MEDIA_ID) +
                " source=" + source);

        mState = PlaybackState.STATE_STOPPED;
        relaxResources(false); // release everything except MediaPlayer

        try {
            createMediaPlayerIfNeeded();

            mState = PlaybackState.STATE_BUFFERING;

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Log.d(TAG, "****** playCurrentSong: about to call setDataSource. If no" +
                    "'finished' log message shows up right after this, it's because the media " +
                    "player is stuck in a deadlock. This is a known issue. In the meantime, you " +
                    "will need to restart the device.");
            try {
                mMediaPlayer.setDataSource(source);
            } finally {
                Log.d(TAG, "****** playCurrentSong: setDataSource finished, no deadlock :-)");
            }

            // Starts preparing the media player in the background. When
            // it's done, it will call our OnPreparedListener (that is,
            // the onPrepared() method on this class, since we set the
            // listener to 'this'). Until the media player is prepared,
            // we *cannot* call start() on it!
            mMediaPlayer.prepareAsync();

            // If we are streaming from the internet, we want to hold a
            // Wifi lock, which prevents the Wifi radio from going to
            // sleep while the song is playing.
            mWifiLock.acquire();

            updatePlaybackState(null);
            updateMetadata();

        } catch (IOException ex) {
            Log.e(TAG, "IOException playing song: " + ex);
            updatePlaybackState(ex.getMessage());
        }
    }



    private void updateMetadata() {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            Log.e(TAG, "Can't retrieve current metadata.");
            mState = PlaybackState.STATE_ERROR;
            updatePlaybackState(getResources().getString(R.string.error_no_metadata));
            return;
        }
        MediaSession.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
        String mediaId = queueItem.getDescription().getMediaId();
        MediaMetadata track = mMusicProvider.getMusic(mediaId);
        String trackId = track.getString(Common.METADATA_MEDIA_ID);
        if (!mediaId.equals(trackId)) {
            throw new IllegalStateException("track ID (" + trackId + ") " +
                    "should match mediaId (" + mediaId + ")");
        }
        Log.d(TAG, "Updating metadata for MusicID= " + mediaId);
        mSession.setMetadata(track);
    }


    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     *
     */
    private void updatePlaybackState(String error) {
        Log.d(TAG, "updatePlaybackState, setting session playback state to " + mState);
        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            position = mMediaPlayer.getCurrentPosition();
        }
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(getAvailableActions());

        setCustomAction(stateBuilder);

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            mState = PlaybackState.STATE_ERROR;
        }
        stateBuilder.setState(mState, position, 1.0f, SystemClock.elapsedRealtime());

        mSession.setPlaybackState(stateBuilder.build());

        if (mState == PlaybackState.STATE_PLAYING || mState == PlaybackState.STATE_PAUSED) {
            mMediaNotification.startNotification();
        }
    }

    private void setCustomAction(PlaybackState.Builder stateBuilder) {
        MediaMetadata currentMusic = getCurrentPlayingMusic();
        if (currentMusic != null) {
            // Set appropriate "Favorite" icon on Custom action:
            String mediaId = currentMusic.getString(Common.METADATA_MEDIA_ID);
            int favoriteIcon = android.R.drawable.btn_star_big_off;
            if (mMusicProvider.isFavorite(mediaId)) {
                favoriteIcon = android.R.drawable.btn_star_big_on;
            }
            Log.d(TAG, "updatePlaybackState, setting Favorite custom action of music " +
                    mediaId + " current favorite=" + mMusicProvider.isFavorite(mediaId));
            stateBuilder.addCustomAction(Common.ACTION_FAV, getString(R.string.favorite),
                    favoriteIcon);
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH;
        if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
            return actions;
        }
        if (mState == PlaybackState.STATE_PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }
        if (mCurrentIndexOnQueue > 0) {
            actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    private MediaMetadata getCurrentPlayingMusic() {
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            MediaSession.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            if (item != null) {
                Log.d(TAG, "getCurrentPlayingMusic for musicId=" + item.getDescription().getMediaId());
                return mMusicProvider.getMusic(item.getDescription().getMediaId());
            }
        }
        return null;
    }

    /**
     * Try to get the system audio focus.
     */
    void tryToGetAudioFocus() {
        Log.d(TAG, "tryToGetAudioFocus");
        if (mAudioFocus != AudioFocus.Focused) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AudioFocus.Focused;
            }
        }

    }

    /**
     * Give up the audio focus.
     */
    void giveUpAudioFocus() {
        Log.d(TAG, "giveUpAudioFocus");
        if (mAudioFocus == AudioFocus.Focused) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AudioFocus.NoFocusNoDuck;
            }
        }
    }

}

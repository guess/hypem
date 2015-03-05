package co.stevets.music.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import co.stevets.music.R;
import co.stevets.music.network.MusicService;
import co.stevets.music.utils.Common;


public class NowPlayingFragment extends Fragment {

    private static final String TAG = "NowPlayingFragment";

    // UI elements
    private SeekBar mSeekBar;           // Seek bar showing current location of song
    private ImageView mImage;           // Album artwork
    private TextView mTitle;            // Title of the song
    private TextView mArtist;           // Name of the artist
    private ImageButton mPlayPause;     // Play & pause button
    private ImageButton mFav;           // Favourite button
    private ProgressBar mProgress;      // Progress bar when the song is buffering

    // Communication with the main Music Activity
    private OnNowPlayingInteraction mListener;

    // Common application utilities
    private Common mApp;

    // Connection with the media player
    private MediaSession.Token mSessionToken;   // Token needed to interact with media session
    private MediaController mController;        // Controlling audio playback
    private PlaybackState mPlaybackState;       // The current state of the media player
    private MediaMetadata mMetadata;            // The metadata of the current song in focus


    public NowPlayingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApp = (Common) getActivity().getApplicationContext();

        // Get the current token of this media session
        updateSessionToken();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        final View view = inflater.inflate(R.layout.fragment_now_playing, container, false);

        // Album artwork
        mImage = (ImageView) view.findViewById(R.id.image);

        // Song title
        mTitle = (TextView) view.findViewById(R.id.title);

        // Song artist
        mArtist = (TextView) view.findViewById(R.id.artist);

        // Next button
        view.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed(OnNowPlayingInteraction.NEXT);
            }
        });

        // Previous button
        view.findViewById(R.id.previous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed(OnNowPlayingInteraction.PREV);
            }
        });

        // Play and pause button
        mPlayPause = (ImageButton) view.findViewById(R.id.play_pause);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed(OnNowPlayingInteraction.PLAY_PAUSE);
            }
        });

        // Favourite button
        mFav = (ImageButton) view.findViewById(R.id.fav);
        mFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // The activity will notify the service to mark the song as a favourite
                // The UI will update when it gets pinged that the playback state has changed.
                onButtonPressed(OnNowPlayingInteraction.FAV);
            }
        });

        // Progress bar to show song buffering
        mProgress = (ProgressBar) view.findViewById(R.id.progress);

        // Seek bar that shows the song's current position
        mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);

        // Automatically play the first song
        onButtonPressed(OnNowPlayingInteraction.PLAY_PAUSE);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mApp.registerReceiver(mReceiver, new IntentFilter(MusicService.BROADCAST_SEEK_PROGRESS));
    }

    @Override
    public void onPause() {
        mApp.unregisterReceiver(mReceiver);
        super.onPause();
    }

    // ********* Interaction with the media player:

    /**
     * Update the state based on a change on the session token.
     * Called when we are running for the first time or when the media session owner has
     * destroyed the session.
     */
    private void updateSessionToken() {
        MediaSession.Token freshToken = mApp.getSessionToken();
        if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCb);
            }
            mSessionToken = freshToken;
            mController = new MediaController(getActivity(), mSessionToken);
            mController.registerCallback(mCb);
        }
    }

    private final MediaController.Callback mCb = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            mPlaybackState = state;
            Log.d(TAG, "Received new playback state: " + state);
            updatePlaybackState();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mMetadata = metadata;
            Log.d(TAG, "Received new metadata: " + metadata);
            updateMetadata();
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Log.d(TAG, "Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }
    };


    // ********* UI updates:

    /**
     * Get updates with the position and duration of the current song.
     * This is used to update the seek bar as the song plays.
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int position = intent.getIntExtra(MusicService.POSITION, 0);
            int max = intent.getIntExtra(MusicService.DURATION, 0);
            mSeekBar.setMax(max);
            mSeekBar.setProgress(position);
        }
    };

    /**
     * Update the fragment's UI using the metadata of the currently playing song.
     */
    private void updateMetadata() {
        Log.d(TAG, "updateNotificationMetadata. mMetadata=" + mMetadata);
        if (mMetadata == null || mPlaybackState == null) {
            return;
        }

        MediaDescription description = mMetadata.getDescription();

        // Update title
        mTitle.setText(description.getTitle());

        // Update artist
        String artist = mMetadata.getString(Common.METADATA_ARTIST);
        mArtist.setText(artist);

        // Update favourite
        updateFavouriteFlag();

        // Update image
        Picasso.with(getActivity())
                .load(description.getIconUri())
                .placeholder(android.R.drawable.gallery_thumb)
                .into(mImage);
    }

    /**
     * Update the fragment's UI using the media player's current playback state.
     */
    private void updatePlaybackState() {
        Log.d(TAG, "updatePlaybackState. mPlaybackState=" + mPlaybackState);
        if (mPlaybackState == null) {
            Log.d(TAG, "updatePlaybackState. cancelling update!");
            return;
        }

        // Show the play or pause button depending on the current state (i.e., playing or stopped)
        if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
            mPlayPause.setImageResource(R.drawable.pause_light);
        } else {
            mPlayPause.setImageResource(R.drawable.play_light);
        }

        // Show the progress bar if the song is buffering
        if (mPlaybackState.getState() == PlaybackState.STATE_BUFFERING) {
            mProgress.setVisibility(View.VISIBLE);
            mSeekBar.setVisibility(View.INVISIBLE);
        } else {
            mProgress.setVisibility(View.INVISIBLE);
            mSeekBar.setVisibility(View.VISIBLE);
        }

        // When a song has been added/removed from the favourites list it will be notified
        // as a playback state change.
        updateFavouriteFlag();
    }

    /**
     * Set the favourite flag.
     */
    private void updateFavouriteFlag() {
        if (mMetadata == null || mPlaybackState == null) {
            return;
        }

        String id = mMetadata.getDescription().getMediaId();
        if (mApp.getProvider().isFavorite(id)) {
            mFav.setImageResource(R.drawable.fav_active);
        } else {
            mFav.setImageResource(R.drawable.fav);
        }
    }


    // ********* Communication with the main music activity:

    public void onButtonPressed(int action) {
        if (mListener != null) {
            mListener.onButtonPressed(action);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnNowPlayingInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnNowPlayingInteraction");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnNowPlayingInteraction {
        // Possible actions
        public static final int PLAY_PAUSE  = 1;
        public static final int PREV        = 2;
        public static final int NEXT        = 3;
        public static final int FAV         = 4;

        public void onButtonPressed(int action);
    }

}

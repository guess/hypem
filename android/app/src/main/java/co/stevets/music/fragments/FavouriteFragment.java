package co.stevets.music.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.database.Cursor;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;

import co.stevets.music.R;
import co.stevets.music.utils.Common;
import co.stevets.music.views.adapters.FavouriteAdapter;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link NowPlayingFragment.OnNowPlayingInteraction}
 * interface.
 */
public class FavouriteFragment extends Fragment implements AbsListView.OnItemClickListener {

    private static final String TAG = "FavouriteFragment";

    private NowPlayingFragment.OnNowPlayingInteraction mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private CursorAdapter mAdapter;

    private Common mApp;

    // Connection with the media player
    private MediaSession.Token mSessionToken;   // Token needed to interact with media session
    private MediaController mController;        // Controlling audio playback

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FavouriteFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApp = (Common) getActivity().getApplicationContext();

        // Get the current token of this media session
        updateSessionToken();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favourite, container, false);

        // Get the cursor of song favourites from the database
        Cursor cursor = mApp.getProvider().getFavourites();
        mAdapter = new FavouriteAdapter(mApp, cursor);

        // Set the adapter
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (NowPlayingFragment.OnNowPlayingInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onButtonPressed(10);
        }
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
            // Playback state changed:
            // Reload list in case the user added or removed a song from favourites
            Cursor cursor = mApp.getProvider().getFavourites();
            mAdapter.changeCursor(cursor);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Log.d(TAG, "Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }
    };

}

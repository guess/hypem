package co.stevets.music.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import java.util.List;

import co.stevets.music.managers.MusicDatabase;
import co.stevets.music.models.Song;
import co.stevets.music.network.ApiClient;
import co.stevets.music.utils.Common;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class ShuffleFragment extends Fragment {

    private int mNextPage = 1;
    private boolean mIsDownloadInProgress = false;

    private Common mApp;

    private OnSongsDownloadedListener mListener;

    public ShuffleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp = (Common) getActivity().getApplicationContext();
        MusicDatabase db = new MusicDatabase(mApp);
        db.clearSongs();
        downloadData(mNextPage);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSongsDownloadedListener) activity;
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


    private void downloadData(final int pageNumber) {
        if (!mIsDownloadInProgress) {
            mIsDownloadInProgress = true;

            ApiClient.getMusicApiClient().getPopular(pageNumber, new Callback<List<Song>>() {

                @Override
                public void success(List<Song> songs, Response response) {
                    consumeApiData(songs);
                }

                @Override
                public void failure(RetrofitError error) {
                    consumeApiData(null);
                }
            });
        }
    }


    private void consumeApiData(List<Song> songs) {
        if (songs != null) {
            // Add the found songs to our media catalog
            mApp.getProvider().addMediaCatalog("popular", songs);

            // Keep track of what page to download next
            mNextPage++;
        }

        mIsDownloadInProgress = false;

        // For now only download the first page
        if (mNextPage <= 1) {
            downloadData(mNextPage);
        } else {
            mListener.onSongsDownloaded();
        }
    }


    public interface OnSongsDownloadedListener {
        public void onSongsDownloaded();
    }

}

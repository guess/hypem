package co.stevets.music.models;


import android.media.MediaMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import co.stevets.music.utils.Common;

/**
 * Music provider to manage the songs in memory.
 * In the future these will be stored on disk.
 */
public class MusicProvider {

    public static final String TAG = "MusicProvider";

    // Categorized caches for music track data:
    private final HashMap<String, List<MediaMetadata>> mMusicListByPage;
    private final HashMap<String, MediaMetadata> mMusicListById;
    private final HashSet<String> mFavoriteTracks;

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED;
    }

    private State mCurrentState = State.NON_INITIALIZED;


    public MusicProvider() {
        mMusicListByPage = new HashMap<>();
        mMusicListById = new HashMap<>();
        mFavoriteTracks = new HashSet<>();
    }

    /**
     * Get the Hype Machine pages in the media catalog.
     * @return  Iterator over the list of pages.
     */
    public Iterable<String> getPages() {
        if (mCurrentState != State.INITIALIZED) {
            return new ArrayList<>(0);
        }
        return mMusicListByPage.keySet();
    }

    /**
     * Get songs from a specified page.
     * @return  An iterator over the list of songs on the specified page.
     */
    public Iterable<MediaMetadata> getMusicsByPage(String page) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByPage.containsKey(page)) {
            return new ArrayList<>();
        }
        return mMusicListByPage.get(page);
    }

    /**
     * Get song with the specified ID.
     * @param mediaId   Song ID
     * @return The media metadata of the specified song.
     */
    public MediaMetadata getMusic(String mediaId) {
        return mMusicListById.get(mediaId);
    }

    /**
     * Add or remove a song from favourites.
     * @param mediaId   Song ID
     * @param favorite  True to add a song to favourites, false to remove it from favourites.
     */
    public void setFavorite(String mediaId, boolean favorite) {
        if (favorite) {
            mFavoriteTracks.add(mediaId);
        } else {
            mFavoriteTracks.remove(mediaId);
        }
    }

    /**
     * Check if the a song is a favourite.
     * @param musicId   Song ID
     * @return          True if the song is a favourite, false otherwise.
     */
    public boolean isFavorite(String musicId) {
        return mFavoriteTracks.contains(musicId);
    }

    /**
     * Check if music provider has been initialized.
     * @return True if the music provider has been initialized, false otherwise.
     */
    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    /**
     * Add songs from a Hype Machine page into the media catalog.
     * @param page      Name of the Hype Machine page.
     * @param songs     List of songs on the page.
     */
    public void addMediaCatalog(final String page, final List<Song> songs) {
        mCurrentState = State.INITIALIZING;
        List<MediaMetadata> list = mMusicListByPage.get(page);
        if (list == null) {
            list = new ArrayList<>();
        }
        for (Song song : songs) {
            MediaMetadata item = getMetadata(song);
            list.add(item);
            mMusicListById.put(item.getDescription().getMediaId(), item);
        }
        mMusicListByPage.put(page, list);
        mCurrentState = State.INITIALIZED;
    }

    /**
     * Convert a Song object into MediaMetadata object. Used by the Media Player.
     * @param song  Song
     * @return      MediaMetadata version of the song.
     */
    private MediaMetadata getMetadata(Song song) {
        // Since we don't have a unique ID in the server, we fake one using the hashcode of
        // the music source. In a real world app, this could come from the server.
        String id = String.valueOf(song.hashCode());

        return new MediaMetadata.Builder()
                .putString(Common.METADATA_MEDIA_ID, id)
                .putString(Common.METADATA_SOURCE, song.getUrl())
                .putString(Common.METADATA_ARTIST, song.getArtist())
                .putLong(Common.METADATA_DURATION, song.getTime())
                .putString(Common.METADATA_ALBUM_ART_URL, song.getThumbUrlLarge())
                .putString(Common.METADATA_TITLE, song.getTitle())
                .build();
    }

}

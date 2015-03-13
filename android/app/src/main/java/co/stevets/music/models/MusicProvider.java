package co.stevets.music.models;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaMetadata;

import java.util.ArrayList;
import java.util.List;

import co.stevets.music.managers.MusicDatabase;
import co.stevets.music.utils.Common;

/**
 * Music provider to manage the songs in memory.
 * In the future these will be stored on disk.
 */
public class MusicProvider {

    public static final String TAG = "MusicProvider";

    private Context mContext;

    // Categorized caches for music track data:
    //private final HashMap<String, MediaMetadata> mMusicListById;

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED;
    }

    private State mCurrentState = State.NON_INITIALIZED;


    public MusicProvider(Context context) {
        mContext = context;
        //mMusicListById = new HashMap<>();
    }

    /**
     * Get song with the specified ID.
     * @param mediaId   Song ID
     * @return The media metadata of the specified song.
     */
    public MediaMetadata getSong(String mediaId) {
        MediaMetadata metadata = null;

        MusicDatabase helper = new MusicDatabase(mContext);
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor cursor = db.query(
                Common.TABLE_SONG,
                Common.SONG_PROJECTION,
                Common.SONG_MEDIA_ID + "=?",
                new String[]{ mediaId },
                null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst() && !cursor.isAfterLast()) {
                metadata = getMetadata(cursor);
            }
            cursor.close();
        }

        db.close();

        return metadata;
    }

    public List<MediaMetadata> getRandomSongs() {
        List<MediaMetadata> songs = new ArrayList<>();

        MusicDatabase db = new MusicDatabase(mContext);
        Cursor cursor = db.getRandomSongs();
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                songs.add(getMetadata(cursor));
                cursor.moveToNext();
            }
            cursor.close();
        }

        return songs;
    }

    /**
     * Add or remove a song from favourites.
     * @param mediaId   Song ID
     * @param favorite  True to add a song to favourites, false to remove it from favourites.
     */
    public void setFavorite(String mediaId, boolean favorite) {
        MusicDatabase helper = new MusicDatabase(mContext);
        if (favorite) {
            helper.setFavourite(mediaId, 1);
        } else {
            helper.setFavourite(mediaId, 0);
        }
    }

    public Cursor getFavourites() {
        MusicDatabase helper = new MusicDatabase(mContext);
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.query(
                Common.TABLE_SONG,
                Common.SONG_PROJECTION,
                Common.SONG_FAVOURITE + "=?",
                new String[]{Integer.toString(1)},
                null, null, null);
    }

    /**
     * Check if the a song is a favourite.
     * @param musicId   Song ID
     * @return          True if the song is a favourite, false otherwise.
     */
    public boolean isFavorite(String musicId) {
        //return mFavoriteTracks.contains(musicId);
        MusicDatabase helper = new MusicDatabase(mContext);
        return helper.isFavourite(musicId);
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
        MusicDatabase db = new MusicDatabase(mContext);
        for (Song song : songs) {
            db.addOrUpdateSong(song);
        }
        mCurrentState = State.INITIALIZED;
    }

    private MediaMetadata getMetadata(Cursor cursor) {
        return new MediaMetadata.Builder()
                .putString(Common.METADATA_MEDIA_ID,
                        cursor.getString(cursor.getColumnIndex(Common.SONG_MEDIA_ID)))
                .putString(Common.METADATA_SOURCE,
                        cursor.getString(cursor.getColumnIndex(Common.SONG_URL)))
                .putString(Common.METADATA_ARTIST,
                        cursor.getString(cursor.getColumnIndex(Common.SONG_ARTIST)))
                .putLong(Common.METADATA_DURATION,
                        cursor.getLong(cursor.getColumnIndex(Common.SONG_DURATION)))
                .putString(Common.METADATA_ALBUM_ART_URL,
                        cursor.getString(cursor.getColumnIndex(Common.SONG_THUMB_URL_LARGE)))
                .putString(Common.METADATA_TITLE,
                        cursor.getString(cursor.getColumnIndex(Common.SONG_TITLE)))
                .build();
    }

    /**
     * Convert a Song object into MediaMetadata object. Used by the Media Player.
     * @param song  Song
     * @return      MediaMetadata version of the song.
     */
    private MediaMetadata getMetadata(Song song) {
        return new MediaMetadata.Builder()
                .putString(Common.METADATA_MEDIA_ID, song.getMediaId())
                .putString(Common.METADATA_SOURCE, song.getUrl())
                .putString(Common.METADATA_ARTIST, song.getArtist())
                .putLong(Common.METADATA_DURATION, song.getDuration())
                .putString(Common.METADATA_ALBUM_ART_URL, song.getThumbUrlLarge())
                .putString(Common.METADATA_TITLE, song.getTitle())
                .build();
    }

}

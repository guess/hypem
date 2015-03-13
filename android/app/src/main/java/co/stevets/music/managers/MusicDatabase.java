package co.stevets.music.managers;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import co.stevets.music.models.Song;
import co.stevets.music.utils.Common;

public class MusicDatabase extends SQLiteOpenHelper {

    private static final String TAG = "MusicDatabase";

    private SQLiteDatabase mDatabase;

    public MusicDatabase(Context context) {
        super(context, Common.DATABASE_NAME, null, Common.DATABASE_VERSION);
    }

    /**
     * Returns a writable instance of the database. Provides an additional
     * null check for additional stability.
     */
    private synchronized SQLiteDatabase getDatabase() {
        if (mDatabase==null)
            mDatabase = this.getWritableDatabase();
        return mDatabase;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Construct a table for song items
        String CREATE_SONG_TABLE = "CREATE TABLE " + Common.TABLE_SONG + "("
                + Common.SONG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Common.SONG_MEDIA_ID + " TEXT UNIQUE,"
                + Common.SONG_ARTIST + " TEXT,"
                + Common.SONG_TITLE + " TEXT,"
                + Common.SONG_URL + " TEXT,"
                + Common.SONG_THUMB_URL + " TEXT,"
                + Common.SONG_THUMB_URL_MED + " TEXT,"
                + Common.SONG_THUMB_URL_LARGE + " TEXT,"
                + Common.SONG_THUMB_URL_ARTIST + " TEXT,"
                + Common.SONG_DATE_POSTED + " INTEGER,"
                + Common.SONG_POSTED_COUNT + " INTEGER,"
                + Common.SONG_POST_URL + " TEXT,"
                + Common.SONG_LOVED_COUNT + " INTEGER,"
                + Common.SONG_DURATION + " TEXT,"
                + Common.SONG_FAVOURITE + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_SONG_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 1) {
            // Wipe older tables if existed
            db.execSQL("DROP TABLE IF EXISTS " + Common.TABLE_SONG);
            // Create tables again
            onCreate(db);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            getDatabase().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.finalize();
    }

    /**
     * Insert a song into the database. If it already exists, update it.
     * Note: this does not change the favourite value for a song if it already exists.
     * @param song  Song to insert or update
     */
    public void addOrUpdateSong(Song song) {
        SQLiteDatabase db = getDatabase();

        // Define content values for each field
        ContentValues values = new ContentValues();
        values.put(Common.SONG_MEDIA_ID, song.getMediaId());
        values.put(Common.SONG_ARTIST, song.getArtist());
        values.put(Common.SONG_TITLE, song.getTitle());
        values.put(Common.SONG_URL, song.getUrl());
        values.put(Common.SONG_THUMB_URL, song.getThumbUrl());
        values.put(Common.SONG_THUMB_URL_MED, song.getThumbUrlMedium());
        values.put(Common.SONG_THUMB_URL_LARGE, song.getThumbUrlLarge());
        values.put(Common.SONG_THUMB_URL_ARTIST, song.getThumbUrlArtist());
        values.put(Common.SONG_DATE_POSTED, song.getDatePosted());
        values.put(Common.SONG_POSTED_COUNT, song.getPostedCount());
        values.put(Common.SONG_POST_URL, song.getPostUrl());
        values.put(Common.SONG_LOVED_COUNT, song.getLovedCount());
        values.put(Common.SONG_DURATION, song.getDuration());

        try {
            db.insertOrThrow(Common.TABLE_SONG, null, values);
            Log.d(TAG, "addSong. Added: " + song.getArtist() + " - " + song.getTitle());
        } catch (SQLiteException e) {
            String where = Common.SONG_MEDIA_ID + "=?";
            String[] whereArgs = new String[] { song.getMediaId() };
            db.update(Common.TABLE_SONG, values, where, whereArgs);
            Log.d(TAG, "addSong. Updated: " + song.getArtist() + " - " + song.getTitle());
        }
    }

    /**
     * Add or remove a song from favourites in the database.
     * @param mediaId       ID of the song to update the favourite value of
     * @param favourite     1 to set favourite, 0 to remove favourite
     */
    public void setFavourite(String mediaId, int favourite) {
        SQLiteDatabase db = getDatabase();

        // Set the favourite value
        ContentValues values = new ContentValues();
        values.put(Common.SONG_FAVOURITE, favourite);

        // Update the database entry
        String where = Common.SONG_MEDIA_ID + "=?";
        String[] whereArgs = new String[] { mediaId };
        int value = db.update(Common.TABLE_SONG, values, where, whereArgs);
        Log.d(TAG,
                (value > 0)
                        ? "setFavourite. Successfully set favourite."
                        : "setFavourite. Failed to set favourite."
        );
    }

    /**
     * Check if a song is a favourite.
     * @param mediaId   ID of the song
     * @return True if the song is a favourite, false otherwise.
     */
    public boolean isFavourite(String mediaId) {
        boolean isFav = false;
        SQLiteDatabase db = getDatabase();

        Cursor cursor = db.query(
                Common.TABLE_SONG,
                new String[] { Common.SONG_ID, Common.SONG_MEDIA_ID, Common.SONG_FAVOURITE },
                Common.SONG_MEDIA_ID + "=?",
                new String[] { mediaId },
                null, null, null
        );

        if (cursor != null ) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast() && cursor.getInt(cursor.getColumnIndex(Common.SONG_FAVOURITE)) > 0) {
                isFav = true;
            }
            cursor.close();
        }

        return isFav;
    }

    public Cursor getRandomSongs() {
        SQLiteDatabase db = getDatabase();

        Cursor cursor = db.query(
                Common.TABLE_SONG + " ORDER BY RANDOM()",
                Common.SONG_PROJECTION,
                null, null, null, null, null
        );

        return cursor;
    }

    /**
     * Remove songs that are not set as a favourite.
     */
    public void clearSongs() {
        SQLiteDatabase db = getDatabase();

        // Delete songs that are not set as favourite
        int records = db.delete(
                Common.TABLE_SONG,
                Common.SONG_FAVOURITE + " = ?",
                new String[] { String.valueOf(0) }
        );
        Log.d(TAG, "clearSongs. Removed " + records + " songs from the database.");
    }
}

package co.stevets.music.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.provider.BaseColumns;

import co.stevets.music.models.MusicProvider;


public class Common extends Application {

    // Song metadata
    public static final String METADATA_SOURCE          = "__SOURCE__";
    public static final String METADATA_FAVOURITE       = "__FAVOURITE__";
    public static final String METADATA_MEDIA_ID        = MediaMetadata.METADATA_KEY_MEDIA_ID;
    public static final String METADATA_ARTIST          = MediaMetadata.METADATA_KEY_ARTIST;
    public static final String METADATA_TITLE           = MediaMetadata.METADATA_KEY_TITLE;
    public static final String METADATA_DURATION        = MediaMetadata.METADATA_KEY_DURATION;
    public static final String METADATA_ALBUM_ART_URL   = MediaMetadata.METADATA_KEY_ALBUM_ART_URI;

    // Media player actions
    public static final String ACTION_PAUSE = "co.stevets.music.pause";
    public static final String ACTION_PLAY  = "co.stevets.music.play";
    public static final String ACTION_PREV  = "co.stevets.music.prev";
    public static final String ACTION_NEXT  = "co.stevets.music.next";
    public static final String ACTION_FAV   = "co.stevets.music.fav";

    // Database
    public static final int DATABASE_VERSION    = 1;
    public static final String DATABASE_NAME    = "music_database";
    public static final String TABLE_SONG       = "songs";

    // Database columns
    public static final String SONG_ID                  = BaseColumns._ID;
    public static final String SONG_MEDIA_ID            = "media_id";
    public static final String SONG_ARTIST              = "artist";
    public static final String SONG_TITLE               = "title";
    public static final String SONG_DURATION            = "duration";
    public static final String SONG_THUMB_URL           = "thumb_url";
    public static final String SONG_THUMB_URL_MED       = "thumb_url_med";
    public static final String SONG_THUMB_URL_LARGE     = "thumb_url_large";
    public static final String SONG_THUMB_URL_ARTIST    = "thumb_artist";
    public static final String SONG_URL                 = "url";
    public static final String SONG_FAVOURITE           = "favourite";
    public static final String SONG_LOVED_COUNT         = "loved_count";
    public static final String SONG_DATE_POSTED         = "date_posted";
    public static final String SONG_POSTED_COUNT        = "posted_count";
    public static final String SONG_POST_URL            = "post_url";

    public static final String[] SONG_PROJECTION = {
            SONG_ID, SONG_MEDIA_ID, SONG_ARTIST, SONG_TITLE, SONG_DURATION, SONG_THUMB_URL,
            SONG_THUMB_URL_MED, SONG_THUMB_URL_LARGE, SONG_THUMB_URL_ARTIST, SONG_URL,
            SONG_FAVOURITE, SONG_LOVED_COUNT, SONG_DATE_POSTED, SONG_POSTED_COUNT, SONG_POST_URL
    };


    // Context.
    private Context mContext;

    // Music catalog manager
    private MusicProvider mMusicProvider;

    // Shared preferences.
    private SharedPreferences mSharedPreferences;

    private MediaSession mSession;


    @Override
    public void onCreate() {
        super.onCreate();

        // Context
        mContext = getApplicationContext();

        // Music catalog
        mMusicProvider = new MusicProvider(mContext);

        // Media session
        mSession = new MediaSession(this, "MusicService");

        // Shared preferences
        mSharedPreferences = this.getSharedPreferences("co.stevets.music", Context.MODE_PRIVATE);
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    /**
     * Converts milliseconds to hh:mm:ss format.
     */
    public String convertMillisToMinsSecs(long milliseconds) {

        int secondsValue = (int) (milliseconds / 1000) % 60 ;
        int minutesValue = (int) ((milliseconds / (1000*60)) % 60);
        int hoursValue  = (int) ((milliseconds / (1000*60*60)) % 24);

        String seconds = "";
        String minutes = "";
        String hours = "";

        if (secondsValue < 10) {
            seconds = "0" + secondsValue;
        } else {
            seconds = "" + secondsValue;
        }

        if (minutesValue < 10) {
            minutes = "0" + minutesValue;
        } else {
            minutes = "" + minutesValue;
        }

        if (hoursValue < 10) {
            hours = "0" + hoursValue;
        } else {
            hours = "" + hoursValue;
        }

        String output = "";
        if (hoursValue!=0) {
            output = hours + ":" + minutes + ":" + seconds;
        } else {
            output = minutes + ":" + seconds;
        }

        return output;
    }

    public MusicProvider getProvider() {
        return mMusicProvider;
    }

    public MediaSession getSession() {
        return mSession;
    }

    public MediaSession.Token getSessionToken() {
        return mSession.getSessionToken();
    }

}

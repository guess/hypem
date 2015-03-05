package co.stevets.music.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaSession;

import co.stevets.music.models.MusicProvider;


public class Common extends Application {

    // Song metadata
    public static final String METADATA_SOURCE          = "__SOURCE__";
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
        mMusicProvider = new MusicProvider();

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

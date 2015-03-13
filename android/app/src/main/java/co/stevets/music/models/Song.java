
package co.stevets.music.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Song objects we are getting from the server.
 */
public class Song {

    @SerializedName("time")
    @Expose
    private int duration;

    @Expose
    private String url;

    @SerializedName("date_posted")
    @Expose
    private int datePosted;

    @SerializedName("loved_count")
    @Expose
    private int lovedCount;

    @SerializedName("thumb_url_medium")
    @Expose
    private String thumbUrlMedium;

    @SerializedName("thumb_url_artist")
    @Expose
    private String thumbUrlArtist;

    @Expose
    private String title;

    @SerializedName("post_url")
    @Expose
    private String postUrl;

    @Expose
    private String artist;

    @SerializedName("posted_count")
    @Expose
    private int postedCount;

    @SerializedName("thumb_url_large")
    @Expose
    private String thumbUrlLarge;

    @SerializedName("thumb_url")
    @Expose
    private String thumbUrl;

    @SerializedName("media_id")
    @Expose
    private String mediaId;

    /**
     * 
     * @return
     *     The time of the song in milliseconds
     */
    public int getDuration() {
        return duration;
    }

    /**
     * 
     * @param duration
     *     The time of the song in milliseconds
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * 
     * @return
     *     The url of the song
     */
    public String getUrl() {
        return url;
    }

    /**
     * 
     * @param url
     *     The url of the song
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 
     * @return
     *     The date the song was posted, in milliseconds
     */
    public int getDatePosted() {
        return datePosted;
    }

    /**
     * 
     * @param datePosted
     *     The date the song was posted, in milliseconds
     */
    public void setDatePosted(int datePosted) {
        this.datePosted = datePosted;
    }

    /**
     * 
     * @return
     *     The loved count of the song
     */
    public int getLovedCount() {
        return lovedCount;
    }

    /**
     * 
     * @param lovedCount
     *     The loved count of the song
     */
    public void setLovedCount(int lovedCount) {
        this.lovedCount = lovedCount;
    }

    /**
     * 
     * @return
     *     The url of a medium-sized thumbnail
     */
    public String getThumbUrlMedium() {
        return thumbUrlMedium;
    }

    /**
     * 
     * @param thumbUrlMedium
     *     The url of a medium-sized thumbnail
     */
    public void setThumbUrlMedium(String thumbUrlMedium) {
        this.thumbUrlMedium = thumbUrlMedium;
    }

    /**
     * 
     * @return
     *     The url of a thumbnail of the song's artist
     */
    public String getThumbUrlArtist() {
        return thumbUrlArtist;
    }

    /**
     * 
     * @param thumbUrlArtist
     *     The url of a thumbnail of the song's artist
     */
    public void setThumbUrlArtist(String thumbUrlArtist) {
        this.thumbUrlArtist = thumbUrlArtist;
    }

    /**
     * 
     * @return
     *     The title of the song
     */
    public String getTitle() {
        return title;
    }

    /**
     * 
     * @param title
     *     The title of the song
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 
     * @return
     *     The url of the blog posted that featured this song
     */
    public String getPostUrl() {
        return postUrl;
    }

    /**
     * 
     * @param postUrl
     *     The url of the blog posted that featured this song
     */
    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    /**
     * 
     * @return
     *     The artist of the song
     */
    public String getArtist() {
        return artist;
    }

    /**
     * 
     * @param artist
     *     The artist of the song
     */
    public void setArtist(String artist) {
        this.artist = artist;
    }

    /**
     * 
     * @return
     *     The number of blog posts that featured this song
     */
    public int getPostedCount() {
        return postedCount;
    }

    /**
     * 
     * @param postedCount
     *     The number of blog posts that featured this song
     */
    public void setPostedCount(int postedCount) {
        this.postedCount = postedCount;
    }

    /**
     * 
     * @return
     *     The url of a large-sized thumbnail
     */
    public String getThumbUrlLarge() {
        return thumbUrlLarge;
    }

    /**
     * 
     * @param thumbUrlLarge
     *     The url of a large-sized thumbnail
     */
    public void setThumbUrlLarge(String thumbUrlLarge) {
        this.thumbUrlLarge = thumbUrlLarge;
    }

    /**
     * 
     * @return
     *     The url of the thumbnail
     */
    public String getThumbUrl() {
        return thumbUrl;
    }

    /**
     * 
     * @param thumbUrl
     *     The url of the thumbnail
     */
    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    /**
     *
     * @return
     *     The media ID of the song
     */
    public String getMediaId() {
        return mediaId;
    }

    /**
     *
     * @param mediaId
     *     The media ID of the song
     */
    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

}

/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.stevets.music.network;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.util.Log;
import android.util.SparseArray;

import com.squareup.picasso.Picasso;

import java.io.IOException;

import co.stevets.music.R;
import co.stevets.music.utils.Common;

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
public class MediaNotification extends BroadcastReceiver {
    private static final String TAG = "MediaNotification";

    private static final int NOTIFICATION_ID = 416;     // Running through the 6 with my woes

    // Used to associate buttons with actions
    private final SparseArray<PendingIntent> mIntents = new SparseArray<>();

    // Music service associated with the notification
    private final MusicService mService;

    // Controlling the media player
    private MediaSession.Token mSessionToken;
    private MediaController mController;
    private MediaController.TransportControls mTransportControls;

    // Information about the song that is currently playing
    private PlaybackState mPlaybackState;
    private MediaMetadata mMetadata;

    // Notification builder
    private Notification.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private Notification.Action mPlayPauseAction;
    private int mNotificationColor;

    // Used to make sure we don't create multiple notifications
    private boolean mStarted = false;


    public MediaNotification(MusicService service) {
        mService = service;
        updateSessionToken();
        mNotificationColor = getNotificationColor();

        mNotificationManager = (NotificationManager) mService
                .getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = mService.getPackageName();
        mIntents.put(R.drawable.ic_pause_white_24dp, PendingIntent.getBroadcast(mService, 100,
                new Intent(Common.ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
        mIntents.put(R.drawable.ic_play_arrow_white_24dp, PendingIntent.getBroadcast(mService, 100,
                new Intent(Common.ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
        mIntents.put(R.drawable.ic_skip_previous_white_24dp, PendingIntent.getBroadcast(mService, 100,
                new Intent(Common.ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
        mIntents.put(R.drawable.ic_skip_next_white_24dp, PendingIntent.getBroadcast(mService, 100,
                new Intent(Common.ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT));
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!mStarted) {
            mController.registerCallback(mCb);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Common.ACTION_NEXT);
            filter.addAction(Common.ACTION_PAUSE);
            filter.addAction(Common.ACTION_PLAY);
            filter.addAction(Common.ACTION_PREV);
            mService.registerReceiver(this, filter);

            mMetadata = mController.getMetadata();
            mPlaybackState = mController.getPlaybackState();

            mStarted = true;

            // The notification must be updated after setting started to true
            updateNotificationMetadata();
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        Log.d(TAG, "stopNotification");
        mStarted = false;
        mController.unregisterCallback(mCb);
        try {
            mService.unregisterReceiver(this);
        } catch (IllegalArgumentException ex) {
            // Ignore if the receiver is not registered.
        }
        mService.stopForeground(true);
    }


    // ********* Interacting with the media player:

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session.
     */
    private void updateSessionToken() {
        MediaSession.Token freshToken = mService.getSessionToken();
        if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCb);
            }
            mSessionToken = freshToken;
            mController = new MediaController(mService, mSessionToken);
            mTransportControls = mController.getTransportControls();
            if (mStarted) {
                mController.registerCallback(mCb);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "Received intent with action " + action);
        switch (action) {
            case Common.ACTION_PAUSE:
                mTransportControls.pause();
                break;
            case Common.ACTION_PLAY:
                mTransportControls.play();
                break;
            case Common.ACTION_NEXT:
                mTransportControls.skipToNext();
                break;
            case Common.ACTION_PREV:
                mTransportControls.skipToPrevious();
                break;
        }
    }

    private final MediaController.Callback mCb = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            mPlaybackState = state;
            Log.d(TAG, "Received new playback state: " + state);
            updateNotificationPlaybackState();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mMetadata = metadata;
            Log.d(TAG, "Received new metadata: " + metadata);
            updateNotificationMetadata();
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Log.d(TAG, "Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }
    };


    // ********* Update the UI:

    /**
     * Update the notification UI when the song's metadata changes.
     * This is usually called when the song changes.
     */
    private void updateNotificationMetadata() {
        Log.d(TAG, "updateNotificationMetadata. mMetadata=" + mMetadata);
        if (mMetadata == null || mPlaybackState == null) {
            return;
        }

        updatePlayPauseAction();

        mNotificationBuilder = new Notification.Builder(mService);
        int playPauseActionIndex = 0;

        // If skip to previous action is enabled
        // The button is not displayed if there is no previous song
        if ((mPlaybackState.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) {
            mNotificationBuilder
                    .addAction(R.drawable.ic_skip_previous_white_24dp,
                            mService.getString(R.string.label_previous),
                            mIntents.get(R.drawable.ic_skip_previous_white_24dp));
            playPauseActionIndex = 1;
        }

        // Draw the play and pause button
        mNotificationBuilder.addAction(mPlayPauseAction);

        // If skip to next action is enabled
        // The button is not displayed if there is no song to play next
        if ((mPlaybackState.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) {
            mNotificationBuilder.addAction(R.drawable.ic_skip_next_white_24dp,
                    mService.getString(R.string.label_next),
                    mIntents.get(R.drawable.ic_skip_next_white_24dp));
        }

        MediaDescription description = mMetadata.getDescription();
        Bitmap art = description.getIconBitmap();
        if (art == null && description.getIconUri() != null) {
            String artUrl = description.getIconUri().toString();

            // Set the default image placeholder for now
            art = BitmapFactory.decodeResource(mService.getResources(), android.R.drawable.gallery_thumb);

            // Download the image in the background
            if (artUrl != null) {
                fetchBitmapFromURLAsync(artUrl);
            }
        }

        mNotificationBuilder
                .setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(playPauseActionIndex)  // only show play/pause in compact view
                        .setMediaSession(mSessionToken))
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .setUsesChronometer(true)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(art);

        updateNotificationPlaybackState();

        mService.startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    /**
     * Update the notification UI when the playback state changes.
     */
    private void updateNotificationPlaybackState() {
        Log.d(TAG, "updateNotificationPlaybackState. mPlaybackState=" + mPlaybackState);

        if (mPlaybackState == null || !mStarted) {
            Log.d(TAG, "updateNotificationPlaybackState. cancelling notification!");
            mService.stopForeground(true);
            return;
        }

        if (mNotificationBuilder == null) {
            Log.d(TAG, "updateNotificationPlaybackState. there is no notificationBuilder. " +
                    "Ignoring request to update state!");
            return;
        }

        // Setting the song's timer
        if (mPlaybackState.getPosition() >= 0) {
            Log.d(TAG, "updateNotificationPlaybackState. updating playback position to " +
                    (System.currentTimeMillis() - mPlaybackState.getPosition()) / 1000 + " seconds");
            mNotificationBuilder
                    .setWhen(System.currentTimeMillis() - mPlaybackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
            mNotificationBuilder.setShowWhen(true);
        } else {
            Log.d(TAG, "updateNotificationPlaybackState. hiding playback position");
            mNotificationBuilder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        updatePlayPauseAction();

        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    /**
     * Update the notification UI for the play or pause button and the associated
     * actions when you click on the respective buttons.
     */
    private void updatePlayPauseAction() {
        Log.d(TAG, "updatePlayPauseAction");
        String label;
        int icon;

        // Set the play or pause button
        if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
            label = mService.getString(R.string.label_pause);
            icon = R.drawable.ic_pause_white_24dp;
        } else {
            label = mService.getString(R.string.label_play);
            icon = R.drawable.ic_play_arrow_white_24dp;
        }

        // Set the action associated with the buttons
        if (mPlayPauseAction == null) {
            mPlayPauseAction = new Notification.Action(icon, label, mIntents.get(icon));
        } else {
            mPlayPauseAction.icon = icon;
            mPlayPauseAction.title = label;
            mPlayPauseAction.actionIntent = mIntents.get(icon);
        }
    }

    /**
     * Get the notification colour.
     * @return  The application's primary colour.
     */
    protected int getNotificationColor() {
        int notificationColor = 0;
        String packageName = mService.getPackageName();
        try {
            Context packageContext = mService.createPackageContext(packageName, 0);
            ApplicationInfo applicationInfo =
                    mService.getPackageManager().getApplicationInfo(packageName, 0);
            packageContext.setTheme(applicationInfo.theme);
            Resources.Theme theme = packageContext.getTheme();
            TypedArray ta = theme.obtainStyledAttributes(
                    new int[] {android.R.attr.colorPrimary});
            notificationColor = ta.getColor(0, Color.DKGRAY);
            ta.recycle();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return notificationColor;
    }

    /**
     * Load the album artwork on a separate thread.
     * @param source    The URL of the album artwork.
     */
    public void fetchBitmapFromURLAsync(final String source) {
        Log.d(TAG, "getBitmapFromURLAsync: starting asynctask to fetch: " + source);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = Picasso.with(mService).load(source).get();
                    if (mMetadata != null) {
                        String currentSource = mMetadata.getDescription().getIconUri().toString();
                        // If the media is still the same, update the notification:
                        if (mNotificationBuilder != null && currentSource.equals(source)) {
                            Log.d(TAG, "getBitmapFromURLAsync: set bitmap to " + source);
                            mNotificationBuilder.setLargeIcon(bitmap);
                            mNotificationManager.notify(NOTIFICATION_ID,
                                    mNotificationBuilder.build());
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "getBitmapFromURLAsync: " + source + ". Error: " + e);
                }
            }
        }).start();
    }

}

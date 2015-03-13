package co.stevets.music.utils;

import android.media.MediaMetadata;
import android.media.session.MediaSession;

import java.util.ArrayList;
import java.util.List;

import co.stevets.music.models.MusicProvider;

/**
 * The queue helper helps work with data needed for the Media Player.
 */
public class QueueHelper {

    private static final String TAG = "QueueHelper";

    /**
     * Get the music index on the queue.
     * @param queue     The queue of songs
     * @param mediaId   The song ID
     * @return  The music index on the queue.
     */
    public static final int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue,
                                                 String mediaId) {
        int index = 0;
        for (MediaSession.QueueItem item: queue) {
            if (mediaId.equals(item.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Get the music index on the queue.
     * @param queue     The queue of songs
     * @param queueId   The item's queue ID
     * @return  The music index on the queue.
     */
    public static final int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue,
                                                 long queueId) {
        int index = 0;
        for (MediaSession.QueueItem item: queue) {
            if (queueId == item.getQueueId()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Create a random queue.
     * @param musicProvider Music provider
     * @return  A random queue of media session queue items.
     */
    public static final List<MediaSession.QueueItem> getRandomQueue(MusicProvider musicProvider) {
        Iterable<MediaMetadata> tracks = musicProvider.getRandomSongs();
        return convertToQueue(tracks);
    }

    /**
     * Check if there is a song in the queue at a specified index.
     * @param index Index of song
     * @param queue Queue holding songs
     * @return  True if there is a song in the index of the queue, false otherwise
     */
    public static final boolean isIndexPlayable(int index, List<MediaSession.QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }

    /**
     * Convert an iterator over media metadata to a random queue.
     * @param tracks    Iterator of tracks
     * @return  A random queue of tracks.
     */
    private static final List<MediaSession.QueueItem> convertToQueue(Iterable<MediaMetadata> tracks) {
        List<MediaSession.QueueItem> queue = new ArrayList<>();
        int count = 0;
        for (MediaMetadata track : tracks) {
            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            MediaSession.QueueItem item = new MediaSession.QueueItem(
                    track.getDescription(), count++);
            queue.add(item);
        }
        return queue;

    }

}

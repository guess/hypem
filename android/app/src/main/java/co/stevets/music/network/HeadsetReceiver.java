package co.stevets.music.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.util.Log;

import co.stevets.music.utils.Common;

/**
 * Broadcast receiver that handles headphone plug/unplug events.
 */
public class HeadsetReceiver extends BroadcastReceiver {

    private static final String TAG = "HeadsetReceiver";

    // Name of the extra received from the intent
    private static final String STATE = "state";

    // Possible states
    private static final int UNPLUGGED  = 0;
    private static final int PLUGGED_IN = 1;

    public HeadsetReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        Common app = (Common) context.getApplicationContext();
        MediaController controller = new MediaController(context, app.getSessionToken());
        MediaController.TransportControls controls = controller.getTransportControls();

        if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            Log.d(TAG, "action_audio_becoming_noisy");
            controls.pause();
        }

        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra(STATE, -1);
            Log.d(TAG, "action_audio_becoming_noisy. State: " + state);
            switch (state) {
                case UNPLUGGED:
                    controls.pause();
                    break;
                case PLUGGED_IN:
                    controls.play();
                    break;
                default:
                    // Unknown state
            }
        }
    }
}

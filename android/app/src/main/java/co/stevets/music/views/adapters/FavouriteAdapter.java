package co.stevets.music.views.adapters;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import co.stevets.music.R;
import co.stevets.music.utils.Common;


public class FavouriteAdapter extends CursorAdapter {

    private static final String TAG = "FavouriteAdapter";

    // Holder class used to efficiently recycle view positions
    private static final class ViewHolder {
        public TextView title;
        public TextView artist;
        public ImageView image;
    }

    public FavouriteAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_favourite_item, parent, false);

        // Add views to the view holder
        ViewHolder holder = new ViewHolder();
        holder.title = (TextView) view.findViewById(R.id.title);
        holder.artist = (TextView) view.findViewById(R.id.artist);
        holder.image = (ImageView) view.findViewById(R.id.image);

        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();

        // Set the title
        String title = cursor.getString(cursor.getColumnIndex(Common.SONG_TITLE));
        holder.title.setText(title);

        // Set the artist
        String artist = cursor.getString(cursor.getColumnIndex(Common.SONG_ARTIST));
        holder.artist.setText(artist);

        // Load the screen cap image on a background thread
        Picasso.with(context)
                .load(cursor.getString(cursor.getColumnIndex(Common.SONG_THUMB_URL)))
                .placeholder(android.R.drawable.gallery_thumb)
                .into(holder.image);
    }
}

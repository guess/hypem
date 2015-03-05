package co.stevets.music.network;

import java.util.List;

import co.stevets.music.models.Song;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;


public class ApiClient {
    private static MusicApiInterface sMusicService;

    public static MusicApiInterface getMusicApiClient() {
        if (sMusicService == null) {
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint("http://music.stevets.co")
                    .build();

            sMusicService = restAdapter.create(MusicApiInterface.class);
        }

        return sMusicService;
    }

    public interface MusicApiInterface {
        @GET("/popular/3day/{num}")
        void getPopular(@Path("num") int num, Callback<List<Song>> callback);
        @GET("/latest/all/{num}")
        void getLatest(@Path("num") int num);
    }
}

package com.example.jennatillotson.musicfind;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ArtistListActivity extends AppCompatActivity {

    public static final String TASTEDIVE_URL =
            "https://tastedive.com/api/similar?k=304641-MusicHWP-05RY388Z&type=music&q=";

    private ProgressBar spinner;
    private ListView artistList;
    private TextView noResultView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_list);

        artistList = findViewById(R.id.artist_list);
        spinner = findViewById(R.id.progressSpinner);
        noResultView = findViewById(R.id.no_results);


        Intent intent = getIntent();
        String searchText = intent.getStringExtra(ArtistSearch.SEARCH_TEXT);

        new GetArtistsTask().execute(searchText);
    }


    @SuppressLint("StaticFieldLeak")
    private class GetArtistsTask extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String ... strings) {
            List<String> artists = new ArrayList<>();

            String artistData = null;

            try {
                URL url = new URL(TASTEDIVE_URL + URLEncoder.encode(strings[0], "UTF-8"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                try {
                    artistData = streamToString(conn.getInputStream());
                } finally {
                    conn.disconnect();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (artistData != null) {
                try {
                    JSONObject json = new JSONObject(artistData);
                    JSONObject similar = json.getJSONObject("Similar");
                    JSONArray similarList = similar.getJSONArray("Results");

                    String artistName;

                    for (int i = 0; i < similarList.length(); i++) {
                        JSONObject artist = (JSONObject) similarList.get(i);
                        artistName = artist.getString("Name");
                        artists.add(artistName);
                    }

                    if (artists.size() > 0) {
                        //add the person they searched to the list
                        JSONArray searched = similar.getJSONArray("Info");
                        JSONObject originalArtist = (JSONObject) searched.get(0);
                        artistName = originalArtist.getString("Name");
                        artists.add(0, artistName);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return artists;
        }

        @Override
        protected void onPostExecute(List<String> artists) {
            artistList.setAdapter(new ArtistAdapter(artists));

            //Hide loading spinner
            spinner.setVisibility(View.GONE);

            if (artists.size() == 0) {
                noResultView.setVisibility(View.VISIBLE);
            }

        }
    }

    private static String streamToString(InputStream in) throws IOException {
        StringBuilder data = new StringBuilder();
        byte[] buffer = new byte[1000];
        int len = in.read(buffer);
        while (len != -1) {
            data.append(new String(buffer, 0, len));
            len = in.read(buffer);
        }
        in.close();
        return data.toString();
    }

    class ArtistAdapter extends BaseAdapter {

        private List<String> artists;

        public ArtistAdapter(List<String> artists) {
            this.artists = artists;
        }

        @Override
        public int getCount() {
            return artists.size();
        }

        @Override
        public Object getItem(int i) {
            return artists.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.artist_list_item, viewGroup, false);
            }

            final String artistName = (String) getItem(i);
            TextView name = view.findViewById(R.id.artistListName);
            name.setText(artistName);

            name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent detailIntent = new Intent(view.getContext(), ArtistDetailActivity.class);
                    detailIntent.putExtra("ARTIST_NAME", artistName);
                    startActivity(detailIntent);
                }
            });

            return view;
        }
    }

}

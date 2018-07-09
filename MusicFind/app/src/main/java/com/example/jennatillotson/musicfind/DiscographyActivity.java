package com.example.jennatillotson.musicfind;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DiscographyActivity Class
 * Called when the user clicks "View discography" button on artist detail
 */

public class DiscographyActivity extends AppCompatActivity {

    //API URLs
    public static final String MUSICBRAINZ_URL_BEFORE = "http://musicbrainz.org/ws/2/artist/";
    public static final String MUSICBRAINZ_URL_AFTER = "?inc=release-groups&fmt=json";
    public static final String COVERARTARCHIVE_URL = "http://coverartarchive.org/release-group/";

    //View elements
    private ListView discogList;
    private ProgressBar spinner;

    //Define tasks so they can be referenced
    private GetArtworkTask atask = null;
    private GetDiscographyTask dtask = null;

    //So we can check if the activity is running
    private boolean running;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discog_list);

        running = true;
        discogList = findViewById(R.id.discog_list);
        spinner = findViewById(R.id.progressSpinner);

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");

        dtask = new GetDiscographyTask();
        dtask.execute(id);
    }

    @Override
    protected void onDestroy() {
        //Cancel tasks and set running to false
        running = false;
        dtask.cancel(true);
        atask.cancel(true);
        super.onDestroy();
    }

    /**
     * Inner class Release
     *
     * Creates a Release Item out of the JSON data
     */
    private static class Release {
        private String id;
        private String title;
        private String type;
        private String date;
        private String artwork;

        //created so it can be called for sorting purposes
        public String getDate() {
            return this.date;
        }
    }

    /**
     * GetDiscographyTask
     * Extends AsyncTask
     *
     * Gets all the releases for the artist
     * Sets adapter onPostExecute
     *
     * Takes the artist id as parameter
     * Returns list of releases from the artist
     */
    @SuppressLint("StaticFieldLeak")
    private class GetDiscographyTask extends AsyncTask<String, Void, List<Release>> {

        @Override
        protected List<Release> doInBackground(String... strings) {
            List<Release> releases = new ArrayList<>();

            String releaseData = null;
            try {
                URL url = new URL(MUSICBRAINZ_URL_BEFORE + strings[0] + MUSICBRAINZ_URL_AFTER);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();

                try {
                    releaseData = streamToString(connection.getInputStream());
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (releaseData != null) {
                try {
                    JSONObject json = new JSONObject(releaseData);
                    JSONArray releaseGroups = json.getJSONArray("release-groups");

                    for (int i = 0; i < releaseGroups.length(); i++) {
                        JSONObject release = (JSONObject) releaseGroups.get(i);

                        //get title, type, date, id
                        String title = release.getString("title");
                        String type = release.getString("primary-type");
                        String date = release.getString("first-release-date");
                        String id = release.getString("id");

                        //Create Release object and add it to list
                        Release newRelease = new Release();
                        newRelease.id = id;
                        newRelease.title = title;
                        newRelease.type = type;
                        newRelease.date = date;

                        releases.add(newRelease);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            //Sort the releases from newest to oldest
            Collections.sort(releases, new Comparator<Release>() {
                @Override
                public int compare(Release one, Release two) {
                    return -one.getDate().compareTo(two.getDate());
                }
            });

            return releases;
        }

        @Override
        protected void onPostExecute(List<Release> releases) {
            //set adapter and hide loading spinner
            discogList.setAdapter(new ReleaseAdapter(releases));
            spinner.setVisibility(View.GONE);
        }
    }


    /**
     * GetArtworkTask
     * Extends AsyncTask
     *
     * Called for each item in the list, searches a separate API
     * For a link to album artwork and sets it to release.artwork
     *
     * Takes the Release object as a parameter
     */
    @SuppressLint("StaticFieldLeak")
    private class GetArtworkTask extends AsyncTask<Release, Void, String> {

        @Override
        protected String doInBackground(Release...releases) {
            if (running) {
                String artwork = null;
                URL url = null;
                try {
                    url = new URL(COVERARTARCHIVE_URL + releases[0].id);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    try {
                        artwork = streamToString(connection.getInputStream());
                    } finally {
                        connection.disconnect();
                    }
                } catch (FileNotFoundException fnfe) {
                    Log.d("file not found", url.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String imgSrc = null;
                if (artwork != null) {
                    JSONObject jsonArtwork = null;
                    try {
                        jsonArtwork = new JSONObject(artwork);
                        JSONArray images = jsonArtwork.getJSONArray("images");

                        //Gets an array of sources
                        for (int j = 0; j < images.length(); j++) {
                            JSONObject obj = (JSONObject) images.get(j);
                            Boolean front = obj.getBoolean("front");

                            //We only want the front cover image so look for that
                            if (front) {
                                imgSrc = obj.getString("image");
                                releases[0].artwork = imgSrc;
                                break;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return imgSrc;
            }
            return null;
        }
    }


    /**
     * ReleaseAdapter
     * Extends BaseAdapter
     *
     * Loads the Release data into the ListView
     */
    class ReleaseAdapter extends BaseAdapter {

        private List<Release> releases;

        private ReleaseAdapter(List<Release> releases) {
            this.releases = releases;
        }

        @Override
        public int getCount() {
            return releases.size();
        }

        @Override
        public Release getItem(int i) {
            return releases.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.discog_list_item, viewGroup, false);
            }

            Release release = getItem(i);

            TextView title = view.findViewById(R.id.release_title);
            TextView type = view.findViewById(R.id.release_type);
            TextView date = view.findViewById(R.id.release_date);
            ImageView img = view.findViewById(R.id.release_img);

            title.setText(release.title);
            type.setText(release.type);
            date.setText(release.date);
            img.setImageResource(R.drawable.artwork_placeholder_b);


            //if still running, it starts GetArtworkTask to get the artwork link
            //Loads the link into the ImageView using Glide
            if (running) {
                atask = new GetArtworkTask();
                atask.execute(release);
                if (release.artwork != null) {
                    Glide.with(view.getContext()).load(release.artwork).placeholder(R.drawable.artwork_placeholder_b).into(img);
                }
            }
            return view;
        }
    }


    /**
     * Converts InputStream to a String
     *
     * @param in the InputStream
     * @return InputStream converted to a String
     * @throws IOException
     */
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
}

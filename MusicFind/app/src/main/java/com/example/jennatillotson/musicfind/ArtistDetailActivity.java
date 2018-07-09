package com.example.jennatillotson.musicfind;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ms.square.android.expandabletextview.ExpandableTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public class ArtistDetailActivity extends AppCompatActivity {

    public static final String AUDIODB_URL="http://www.theaudiodb.com/api/v1/json/1/search.php?s=";
    public static final String SEARCH_TEXT = "com.example.jennatillotson.musicfind.MESSAGE";

    TextView artistView;
    ImageView picView;
    TextView genreView;
    TextView styleView;
    TextView moodView;
    LinearLayout linksLayout;
    ProgressBar spinner;
    String artistName;
    Button searchBtn;
    Button discogBtn;
    View separator;
    TextView about;

    boolean is_favorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        Intent intent = getIntent();

        //Get info from intent extras
        boolean from_favorite = intent.getBooleanExtra("from_favorite", false);
        artistName = intent.getStringExtra("ARTIST_NAME");

        if (from_favorite) {
            is_favorite = true; //set true because we came from favorites list
        } else {
            new GetFromFavoritesTask().execute(artistName);
        }

        //Find all relevant views
        artistView = findViewById(R.id.artistName);
        picView = findViewById(R.id.artistImage);
        genreView = findViewById(R.id.artistGenre);
        styleView = findViewById(R.id.artistStyle);
        moodView = findViewById(R.id.artistMood);
        linksLayout = findViewById(R.id.links);
        spinner = findViewById(R.id.progressSpinner);
        searchBtn = findViewById(R.id.newSearch);
        discogBtn = findViewById(R.id.discography);
        separator = findViewById(R.id.separator);
        about = findViewById(R.id.about);

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent searchIntent = new Intent(getBaseContext(), ArtistListActivity.class);
                searchIntent.putExtra(SEARCH_TEXT, artistName);
                startActivity(searchIntent);
            }
        });

        //get information to put into views
        new GetDetailsTask().execute(artistName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_fav, menu);

        MenuItem favItem = menu.findItem(R.id.action_fav);
        if (is_favorite) {
            favItem.setIcon(R.drawable.ic_favorite_remove);
        } else {
            favItem.setIcon(R.drawable.ic_favorite_add);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        DatabaseConnector db = new DatabaseConnector(getBaseContext());

        if (is_favorite) {
            db.deleteFavoriteByName(artistName);
            is_favorite = false;
            item.setIcon(R.drawable.ic_favorite_add);
            Toast.makeText(getApplicationContext(), artistName + " removed from favorites.", Toast.LENGTH_LONG).show();
        } else {
            db.insertFavorite(artistName);
            is_favorite = true;
            item.setIcon(R.drawable.ic_favorite_remove);
            Toast.makeText(getApplicationContext(), artistName + " added to favorites.", Toast.LENGTH_LONG).show();
        }

        return true;
    }


    /**
     * AsyncTask: GetFromFavoritesTask
     *
     * Checks DB to see if the artist they are viewing is
     * already saved as a favorite
     */
    @SuppressLint("StaticFieldLeak")
    class GetFromFavoritesTask extends AsyncTask<String, Void, Boolean> {
        DatabaseConnector db = new DatabaseConnector(getBaseContext());

        @Override
        protected Boolean doInBackground(String... strings) {
            db.open();
            return db.isFavorited(strings[0]);
        }

        @Override
        protected void onPostExecute(Boolean favorited) {
            //Set the boolean according to the results
            is_favorite = favorited;
            db.close();

        }
    }

    /**
     * AsyncTask: GetDetailsTask
     *
     * Gets all the artist detail from the API and
     * puts it into the corresponding views
     */
    @SuppressLint("StaticFieldLeak")
    class GetDetailsTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {

            String details = null;

            try {
                URL url = new URL(AUDIODB_URL + URLEncoder.encode(strings[0], "UTF-8"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                try {
                    details = streamToString(conn.getInputStream());
                } finally {
                    conn.disconnect();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return details;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject json = new JSONObject(result);

                if (json.toString().equals("{\"artists\":null}")) {
                    spinner.setVisibility(View.GONE);

                    TextView noResultView = findViewById(R.id.no_results);
                    noResultView.setVisibility(View.VISIBLE);
                    return;
                }

                JSONArray list = json.getJSONArray("artists");
                JSONObject artist = (JSONObject) list.get(0);

                //artist name
                String name = artist.getString("strArtist");
                artistView.setText(name);

                //Photo
                //This is messy because the empty values are sometimes null
                //and sometimes an empty string which doesn't load anything
                String pic = " " + artist.getString("strArtistBanner");
                if (pic.trim().length() <= 4) {
                    pic = " " + artist.getString("strArtistLogo");
                }
                if (pic.trim().length() > 4 ) {
                    Glide.with(getBaseContext()).load(pic.trim()).into(picView);
                }

                //genre
                String genreStr = artist.getString("strGenre");
                if (genreStr.equals("null") || genreStr.equals("")) {
                    genreView.setVisibility(View.GONE);
                } else {
                    genreView.setText("Genre: " + genreStr);
                }

                //style
                String styleStr = artist.getString("strStyle");
                if (styleStr.equals("null") || styleStr.equals("")) {
                    styleView.setVisibility(View.GONE);
                } else {
                    styleView.setText("Style: " + styleStr);
                }

                //mood
                String moodStr = artist.getString("strMood");
                if (moodStr.equals("null") || moodStr.equals("")) {
                    moodView.setVisibility(View.GONE);
                } else {
                    moodView.setText("Mood: " + moodStr);
                }

                //artist bio
                String bioStr = artist.getString("strBiographyEN");
                ExpandableTextView expTextView = findViewById(R.id.expand_text_view);
                expTextView.setText(bioStr);

                //website link
                final String webStr = artist.getString("strWebsite");
                if (!webStr.isEmpty()) {
                    ImageView webSiteImage = new ImageView(getBaseContext());
                    webSiteImage.setImageResource(R.drawable.website_icon);
                    webSiteImage.setPadding(10,0,10,0);
                    linksLayout.addView(webSiteImage);

                    webSiteImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Uri uri = Uri.parse("http://" + webStr);
                            try {
                                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(launchBrowser);
                            } catch(ActivityNotFoundException e) {
                                Toast.makeText(getBaseContext(), "No application can handle this request."
                                        + " Please install a web browser",  Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    });
                }

                //facebook icon
                final String fbStr = artist.getString("strFacebook");
                if (!fbStr.isEmpty()) {
                    ImageView fbImage = new ImageView(getBaseContext());
                    fbImage.setImageResource(R.drawable.ic_facebook);
                    fbImage.setPadding(10,0,10,0);
                    linksLayout.addView(fbImage);

                    fbImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Uri uri = Uri.parse("http://" + fbStr);
                            try {
                                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(launchBrowser);
                            } catch(ActivityNotFoundException e) {
                                Toast.makeText(getBaseContext(), "No application can handle this request."
                                        + " Please install a web browser",  Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    });
                }

                //twitter icon
                final String twitterStr = artist.getString("strTwitter");
                if (!twitterStr.isEmpty()) {
                    ImageView twitImage = new ImageView(getBaseContext());
                    twitImage.setImageResource(R.drawable.ic_twitter);
                    twitImage.setPadding(10,0,10,0);
                    linksLayout.addView(twitImage);

                    twitImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Uri uri = Uri.parse("http://" + twitterStr);
                            try {
                                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(launchBrowser);
                            } catch(ActivityNotFoundException e) {
                                Toast.makeText(getBaseContext(), "No application can handle this request."
                                        + " Please install a web browser",  Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    });
                }

                //Show hidden elements
                searchBtn.setVisibility(View.VISIBLE);
                separator.setVisibility(View.VISIBLE);
                about.setVisibility(View.VISIBLE);

                //Hide loading spinner
                spinner.setVisibility(View.GONE);

                //MusicBrainz ID
                final String musicBrainsID = artist.getString("strMusicBrainzID");
                if (!musicBrainsID.isEmpty()) {
                    discogBtn.setVisibility(View.VISIBLE);

                    discogBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(getBaseContext(), DiscographyActivity.class);
                            intent.putExtra("id", musicBrainsID);
                            startActivity(intent);
                        }
                    });
                }

            } catch (JSONException e) {
                Log.d("why", "WHY THE FUCK ISN'T THIS WORKING");
                e.printStackTrace();
            }
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

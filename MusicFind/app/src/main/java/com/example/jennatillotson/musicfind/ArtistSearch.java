package com.example.jennatillotson.musicfind;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class ArtistSearch extends AppCompatActivity {

    public static final String SEARCH_TEXT = "com.example.jennatillotson.musicfind.MESSAGE";

    private EditText searchText;
    private ListView artistList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_search);
    }

    /** called when the user taps the search button */
    public void search(View view) {
        Intent searchIntent = new Intent(this, ArtistListActivity.class);
        EditText editText = (EditText) findViewById(R.id.search_text);
        String message = editText.getText().toString();
        searchIntent.putExtra(SEARCH_TEXT, message);
        startActivity(searchIntent);
    }

    // create the Activity's menu from a menu resource XML file
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_favorites, menu);
        return true;
    }

    // handle choice from options menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about) {
            Intent viewAbout = new Intent(ArtistSearch.this, AboutPage.class);
            startActivity(viewAbout);
        } else if (item.getItemId() == R.id.viewFavorites) {
            Intent viewFavorites = new Intent(ArtistSearch.this, FavoritesList.class);
            startActivity(viewFavorites);
        }
        return super.onOptionsItemSelected(item);
    }
}

package com.example.jennatillotson.musicfind;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class FavoritesList extends AppCompatActivity {

    private CursorAdapter favoritesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorites_list);


        ListView favsList = findViewById(R.id.favorites_list);
        favsList.setOnItemClickListener(viewFavoriteListener);

        //display message on empty list
        TextView emptyText = (TextView) View.inflate(this,
                R.layout.favorites_list_empty_item, null);
        emptyText.setVisibility(View.GONE);

        ((ViewGroup) favsList.getParent()).addView(emptyText);
        favsList.setEmptyView(emptyText);

        // map each favorite's name to a TextView in the ListView layout
        String[] from = new String[]{"name"};
        int[] to = new int[]{R.id.artistListName};

        favoritesAdapter = new SimpleCursorAdapter(FavoritesList.this,
                R.layout.artist_list_item,null, from, to, 0);

        favsList.setAdapter(favoritesAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new GetFavoritesTask().execute((Object[]) null);
    }

    @Override
    protected void onStop() {
        Cursor cursor = favoritesAdapter.getCursor();

        if (cursor != null)
            cursor.close();

        favoritesAdapter.changeCursor(null);
        super.onStop();
    }

    OnItemClickListener viewFavoriteListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent viewArtist = new Intent(FavoritesList.this, ArtistDetailActivity.class);
            viewArtist.putExtra("from_favorite", true);

            //Get artist name from row and send it over
            TextView artistName = view.findViewById(R.id.artistListName);
            viewArtist.putExtra("ARTIST_NAME", artistName.getText().toString());
            startActivity(viewArtist);
        }
    };


    @SuppressLint("StaticFieldLeak")
    private class GetFavoritesTask extends AsyncTask<Object,Object,Cursor> {
        DatabaseConnector dbConnector = new DatabaseConnector(FavoritesList.this);

        @Override
        protected Cursor doInBackground(Object... objects) {
            dbConnector.open();
            return dbConnector.getAllFavorites();
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            favoritesAdapter.changeCursor(cursor);
            dbConnector.close();
        }
    }
}

package com.example.jennatillotson.musicfind;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jennatillotson on 4/16/18.
 */

public class DatabaseConnector {

    private static final String DATABASE_NAME = "FavoriteArtists";
    private SQLiteDatabase database;
    private DatabaseOpenHelper databaseOpenHelper;

    public DatabaseConnector(Context context) {
        databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, 1);
    }

    public void open() throws SQLException {
        database = databaseOpenHelper.getWritableDatabase();
    }

    public void close() {
        if (database != null) {
            database.close();
        }
    }

    public void insertFavorite(String name) {
        ContentValues newFavorite = new ContentValues();
        newFavorite.put("name", name);

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
        Date date = new Date();
        newFavorite.put("saved_on", dateFormat.format(date));

        open();
        database.insert("favorites", null, newFavorite);
        close();
    }

    public Cursor getAllFavorites() {
        return database.query("favorites", new String[] {"_id", "name", "saved_on"},
                null, null, null, null, "name");
    }

    public Cursor getOneFavorite(long id) {
        return database.query("favorites", null, "_id=" + id,
                null, null, null, null);
    }

    public boolean isFavorited(String name) {
        Cursor query = database.query("favorites", null, "name='" + name + "'",
                null, null, null, null);

        return query.getCount() > 0;
    }

    public void deleteFavorite(long id) {
        open();
        database.delete("favorites", "_id=" + id, null);
        close();
    }

    public void deleteFavoriteByName(String name) {
        open();
        database.delete("favorites", "name='" + name + "'", null);
        close();
    }


    private class DatabaseOpenHelper extends SQLiteOpenHelper {

        public DatabaseOpenHelper(Context context, String name,
                                  SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            String createQuery = "CREATE TABLE favorites" +
                "(_id integer primary key autoincrement," +
                "name TEXT, saved_on TEXT);";

            db.execSQL(createQuery);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }
    }

}

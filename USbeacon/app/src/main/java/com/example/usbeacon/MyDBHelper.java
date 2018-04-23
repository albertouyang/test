package com.example.usbeacon;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 良柏 on 2018/4/20.
 */

public class MyDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "lottery.db";

    public static final int VERSION = 1;

    public static final String TABLE_NAME = "people";

    public static final String KEY = "_id";
    public static final String PERSON = "IMEI";

    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            PERSON + " INTEGER NOT NULL)";

    private static SQLiteDatabase database;

    public MyDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, null, version);
    }

    public static SQLiteDatabase getDatabase(Context context){
        if (database == null || !database.isOpen()){
            database = new MyDBHelper(context, DATABASE_NAME, null, VERSION).getWritableDatabase();
        }
        return database;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        if (newVersion > oldVersion){
//
//        }
//        onCreate(db);
    }

    public void insert(String person){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(PERSON, person);

        db.insert(TABLE_NAME, null, cv);
    }

//    public void modify(SQLiteDatabase db, String id, String person){
//        ContentValues cv = new ContentValues();
//        cv.put(PERSON, person);
//
//        db.update(TABLE_NAME, cv, KEY + "=?", new String[] {id});
//    }
//
//    public void delete(SQLiteDatabase db, String id){
//        db.delete(TABLE_NAME, KEY + "=?", new String[]{id});
//    }
//    public Cursor getAll(SQLiteDatabase db){
//        return db.query(TABLE_NAME, new String[] {KEY, PERSON}, null, null, null, null, null);
//    }
}

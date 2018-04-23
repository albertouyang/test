package com.example.usbeacon;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by 良柏 on 2018/4/20.
 */

public class ItemDAO {
    public static final String TABLE_NAME = "people";

    public static final String KEY = "ID";
    public static final String PERSON = "IMEI";


    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            PERSON + " INTEGER NOT NULL)";

    private SQLiteDatabase db;

    public ItemDAO(Context context){
        db = MyDBHelper.getDatabase(context);
    }

    public void close(){
        db.close();
    }

//    public Item insert(Item item){
//        ContentValues cv = new ContentValues();
//    }
}

package com.XECUREVoIP.chat.ChatUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.Nullable;

import com.XECUREVoIP.security.SecurityUtils;

public class DelChatRoomDBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "DelChatRooms.db";
    public static final String TABLE_NAME = "chatroom_table";
    public static final String COL_ID = "ID";
    public static final String COL_ENTRY_ID = "ENTRY_ID";
    public static final String COL_EXCHANGED = "EXCHANGED";
    public static final String COL_ACCEPT = "ACEEPT";
    public static final String COL_KEY = "SECURE_KEY";

    public DelChatRoomDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME +" (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ENTRY_ID TEXT," +
                "EXCHANGED BOOLEAN," +
                "ACEEPT BOOLEAN," +
                "SECURE_KEY TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);

    }

    public long insertData(String entryId,
                           boolean exchanged,
                           boolean accept,
                           String key){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        //encrypt
        SecurityUtils utils = new SecurityUtils(Build.ID);
        long id = -1;
        try {
            if (entryId != null)
                entryId = utils.encrypt(entryId);
            contentValues.put(COL_ENTRY_ID, entryId);

            contentValues.put(COL_EXCHANGED, exchanged);

            contentValues.put(COL_ACCEPT, accept);

            contentValues.put(COL_KEY, key);

            id = db.insert(TABLE_NAME, null, contentValues);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return id;
    }

    public Cursor getAllData(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME, null);

        return res;
    }

    public long updateData(String id,
                           String entryId,
                           boolean exchanged,
                           boolean accept,
                           String key){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        //encrypt
        SecurityUtils utils = new SecurityUtils(Build.ID);
        long result = -1;
        try {
            if (entryId != null)
                entryId = utils.encrypt(entryId);
            contentValues.put(COL_ENTRY_ID, entryId);

            contentValues.put(COL_EXCHANGED, exchanged);

            contentValues.put(COL_ACCEPT, accept);

            contentValues.put(COL_KEY, key);

            result = db.update(TABLE_NAME, contentValues, "ID = ?", new String[]{id});
        }catch (Exception e){
            e.printStackTrace();
        }

        return result;
    }
    public Integer deleteData(Long id){
        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete(TABLE_NAME, "ID=?", new String[]{id.toString()});
    }
}

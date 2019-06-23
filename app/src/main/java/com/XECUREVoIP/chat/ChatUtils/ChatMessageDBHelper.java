package com.XECUREVoIP.chat.ChatUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.Nullable;

import com.XECUREVoIP.security.SecurityUtils;

public class ChatMessageDBHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "message_table";
    public static final String COL_ID = "ID";
    public static final String COL_MSG_ID = "MSG_ID";
    public static final String COL_SENDER_ID = "SENDER_ID";
    public static final String COL_BODY = "BODY";
    public static final String COL_DATE = "DATE";
    public static final String COL_SENT = "SENT";
    public static final String COL_READ = "READ";
    public static final String COL_DELIVER = "DELIVER";

    public ChatMessageDBHelper(@Nullable Context context, String name) {
        super(context, name + ".db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME +" (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "MSG_ID TEXT," +
                "SENDER_ID TEXT," +
                "BODY TEXT," +
                "DATE TEXT," +
                "SENT BOOLEAN," +
                "READ BOOLEAN," +
                "DELIVER BOOLEAN)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);

    }

    public long insertData(String msgId,
                           String senderId,
                           String body,
                           String date,
                           boolean sent,
                           boolean read,
                           boolean deliver){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        //encrypt
        SecurityUtils utils = new SecurityUtils(Build.ID);
        long id = -1;
        try {
            contentValues.put(COL_MSG_ID, msgId);
            contentValues.put(COL_SENDER_ID, senderId);
            contentValues.put(COL_BODY, body);
            contentValues.put(COL_DATE, date);
            contentValues.put(COL_SENT, sent);
            contentValues.put(COL_READ, read);
            contentValues.put(COL_DELIVER, deliver);

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

    public Cursor getDataFromNumber(String strNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        //encrypt
        SecurityUtils utils = new SecurityUtils(Build.ID);
        try {
            strNumber = utils.encrypt(strNumber);
        }catch (Exception e){
            e.printStackTrace();
        }
        Cursor res = db.rawQuery("select * from " + TABLE_NAME + " where " + COL_SENT + " = ? OR " + COL_READ + " = ?", new String[]{strNumber, strNumber});
        if (res.getCount() == 0)
            return null;
        res.moveToNext();
        return res;
    }

    public long updateData(String id,
                           String msgId,
                           String senderId,
                           String body,
                           String date,
                           boolean sent,
                           boolean read,
                           boolean deliver){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        //encrypt
        SecurityUtils utils = new SecurityUtils(Build.ID);
        long result = -1;
        try {
            contentValues.put(COL_MSG_ID, msgId);
            contentValues.put(COL_SENDER_ID, senderId);
            contentValues.put(COL_BODY, body);
            contentValues.put(COL_DATE, date);
            contentValues.put(COL_SENT, sent);
            contentValues.put(COL_READ, read);
            contentValues.put(COL_DELIVER, deliver);

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

package com.XECUREVoIP.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.Nullable;

import com.XECUREVoIP.XecureContact;
import com.XECUREVoIP.XecureNumberOrAddress;
import com.XECUREVoIP.security.SecurityUtils;

import java.util.ArrayList;

public class ContactDBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Contacts.db";
    public static final String TABLE_NAME = "contact_table";
    public static final String COL_ID = "ID";
    public static final String COL_LAST_NAME = "LAST_NAME";
    public static final String COL_FIRST_NAME = "FIRST_NAME";
    public static final String COL_EMAIL = "EMAIL";
    public static final String COL_SIP_ACCOUNT = "SIP_ACCOUNT";
    public static final String COL_NUMBER00 = "NUMBER00";
    public static final String COL_NUMBER01 = "NUMBER01";
    public static final String COL_PHOTO_URI = "PHOTO_URI";
    public static final String COL_COMPANY = "COMPANY";
    public static final String COL_DEPARTMENT = "DEPARTMENT";
    public static final String COL_SUB_DEPARTMENT = "SUB_DEPARTMENT";
    public static final String COL_SHARE = "SHARE";

    public ContactDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME +" (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "LAST_NAME TEXT," +
                "FIRST_NAME TEXT," +
                "EMAIL TEXT," +
                "SIP_ACCOUNT TEXT," +
                "NUMBER00 TEXT," +
                "NUMBER01 TEXT," +
                "PHOTO_URI TEXT," +
                "COMPANY TEXT," +
                "DEPARTMENT TEXT," +
                "SUB_DEPARTMENT TEXT," +
                "SHARE BOOLEAN)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);

    }

    public long insertData(String strLastName,
                           String strFirstName,
                           String strEmail,
                           String strSipAccount,
                           String strNumber00,
                           String strNumber01,
                           String strPhoto,
                           String strCompany,
                           String strDepartment,
                           String strSubDepartment,
                           boolean bShare){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        //encrypt
        SecurityUtils utils = new SecurityUtils(Build.ID);
        long id = -1;
        try {
            if (strLastName != null)
                strLastName = utils.encrypt(strLastName);
            contentValues.put(COL_LAST_NAME, strLastName);
            if (strFirstName != null)
                strFirstName = utils.encrypt(strFirstName);
            contentValues.put(COL_FIRST_NAME, strFirstName);
            if (strEmail != null)
                strEmail = utils.encrypt(strEmail);
            contentValues.put(COL_EMAIL, strEmail);
            if (strSipAccount != null)
                strSipAccount= utils.encrypt(strSipAccount);
            contentValues.put(COL_SIP_ACCOUNT, strSipAccount);
            if (strNumber00 != null)
                strNumber00 = utils.encrypt(strNumber00);
            contentValues.put(COL_NUMBER00, strNumber00);
            if (strNumber01 != null)
                strNumber01 = utils.encrypt(strNumber01);
            contentValues.put(COL_NUMBER01, strNumber01);
            if (strPhoto != null)
                strPhoto = utils.encrypt(strPhoto);
            contentValues.put(COL_PHOTO_URI, strPhoto);
            if (strCompany != null)
                strCompany = utils.encrypt(strCompany);
            contentValues.put(COL_COMPANY, strCompany);
            if (strDepartment != null)
                strDepartment = utils.encrypt(strDepartment);
            contentValues.put(COL_DEPARTMENT, strDepartment);
            if (strSubDepartment != null)
                strSubDepartment = utils.encrypt(strSubDepartment);
            contentValues.put(COL_SUB_DEPARTMENT, strSubDepartment);
            contentValues.put(COL_SHARE, bShare);

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
        Cursor res = db.rawQuery("select * from " + TABLE_NAME + " where " + COL_NUMBER00 + " = ? OR " + COL_NUMBER01 + " = ?", new String[]{strNumber, strNumber});
        if (res.getCount() == 0)
            return null;
        res.moveToNext();
        return res;
    }

    public long updateData(String id,
                           String strLastName,
                           String strFirstName,
                           String strEmail,
                           String strSipAccount,
                           String strNumber00,
                           String strNumber01,
                           String strPhoto,
                           String strCompany,
                           String strDepartment,
                           String strSubDepartment,
                           boolean bShare){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        //encrypt
        SecurityUtils utils = new SecurityUtils(Build.ID);
        long result = -1;
        try {
            if (strLastName != null)
                strLastName = utils.encrypt(strLastName);
            contentValues.put(COL_LAST_NAME, strLastName);
            if (strFirstName != null)
                strFirstName = utils.encrypt(strFirstName);
            contentValues.put(COL_FIRST_NAME, strFirstName);
            if (strEmail != null)
                strEmail = utils.encrypt(strEmail);
            contentValues.put(COL_EMAIL, strEmail);
            if (strSipAccount != null)
                strSipAccount= utils.encrypt(strSipAccount);
            contentValues.put(COL_SIP_ACCOUNT, strSipAccount);
            if (strNumber00 != null)
                strNumber00 = utils.encrypt(strNumber00);
            contentValues.put(COL_NUMBER00, strNumber00);
            if (strNumber01 != null)
                strNumber01 = utils.encrypt(strNumber01);
            contentValues.put(COL_NUMBER01, strNumber01);
            if (strPhoto != null)
                strPhoto = utils.encrypt(strPhoto);
            contentValues.put(COL_PHOTO_URI, strPhoto);
            if (strCompany != null)
                strCompany = utils.encrypt(strCompany);
            contentValues.put(COL_COMPANY, strCompany);
            if (strDepartment != null)
                strDepartment = utils.encrypt(strDepartment);
            contentValues.put(COL_DEPARTMENT, strDepartment);
            if (strSubDepartment != null)
                strSubDepartment = utils.encrypt(strSubDepartment);
            contentValues.put(COL_SUB_DEPARTMENT, strSubDepartment);
            contentValues.put(COL_SHARE, bShare);

            result = db.update(TABLE_NAME, contentValues, "ID = ?", new String[]{id});
        }catch (Exception e){
            e.printStackTrace();
        }

        return result;
    }

    public boolean isNewContact(String strFirstName, String strLastName){
        //encrypt
        SecurityUtils utils = new SecurityUtils(Build.ID);
        try {
            strFirstName = utils.encrypt(strFirstName);
            strLastName = utils.encrypt(strLastName);
        }catch (Exception e){
            e.printStackTrace();
        }

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME + " where " + COL_LAST_NAME + " = ? AND " + COL_FIRST_NAME + " = ?", new String[]{strLastName, strFirstName});
        if (res.getCount() == 0)
            return true;
        return false;
    }
    public Integer deleteData(Long id){
        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete(TABLE_NAME, "ID=?", new String[]{id.toString()});
    }

    public boolean isExisting(XecureContact contact) {
        SecurityUtils utils = new SecurityUtils(Build.ID);
        try {
            String strFirstName = utils.encrypt(contact.getFirstName());
            String strLastName = utils.encrypt(contact.getLastName());
            String strEmail = utils.encrypt(contact.getEmail());
            ArrayList<String> arrayAdress = new ArrayList<String>();
            ArrayList<String> arrayNumber = new ArrayList<String>();
            for (XecureNumberOrAddress noa : contact.getNumbersOrAddresses()){
                if (noa.isSIPAddress())
                    arrayAdress.add(noa.getValue());
                else
                    arrayNumber.add(noa.getValue());
            }
            String strSipAccount = null;
            String strPhoneNumber00 = null;
            String strPhoneNumber01 = null;
            if (arrayAdress.size() == 1)
                strSipAccount = utils.encrypt(arrayAdress.get(0));
            if (arrayNumber.size() == 1)
                strPhoneNumber00 = utils.encrypt(arrayNumber.get(0));
            else if (arrayNumber.size() == 2){
                strPhoneNumber00 = utils.encrypt(arrayNumber.get(0));
                strPhoneNumber01 = utils.encrypt(arrayNumber.get(1));
            }
            String strCompany = utils.encrypt(contact.getCompany());
            String strDepartment = utils.encrypt(contact.getDepartment());
            String strSubDepartment = utils.encrypt(contact.getSubDepartment());
            SQLiteDatabase db = this.getWritableDatabase();
            Cursor res = db.rawQuery("select * from " +
                            TABLE_NAME + " where " +
                            COL_LAST_NAME + " = ? AND " +
                            COL_FIRST_NAME + " = ? AND " +
                            COL_EMAIL + " = ? AND " +
                            COL_SIP_ACCOUNT + " = ? AND " +
                            COL_NUMBER00 + " = ? AND " +
                            COL_NUMBER01 + " = ? AND " +
                            COL_COMPANY + " = ? AND " +
                            COL_DEPARTMENT + " = ? AND " +
                            COL_SUB_DEPARTMENT + " = ?",
                    new String[]{
                        strLastName,
                        strFirstName,
                        strEmail,
                        strSipAccount,
                        strPhoneNumber00,
                        strPhoneNumber01,
                        strCompany,
                        strDepartment,
                        strSubDepartment
                    });
            if (res.getCount() > 0)
                return true;
            res = db.rawQuery("select * from " +
                            TABLE_NAME + " where " +
                            COL_LAST_NAME + " = ? AND " +
                            COL_FIRST_NAME + " = ? AND " +
                            COL_EMAIL + " = ? AND " +
                            COL_SIP_ACCOUNT + " = ? AND " +
                            COL_NUMBER00 + " = ? AND " +
                            COL_NUMBER01 + " = ? AND " +
                            COL_COMPANY + " = ? AND " +
                            COL_DEPARTMENT + " = ? AND " +
                            COL_SUB_DEPARTMENT + " = ?",
                    new String[]{
                            strLastName,
                            strFirstName,
                            strEmail,
                            strSipAccount,
                            strPhoneNumber01,
                            strPhoneNumber00,
                            strCompany,
                            strDepartment,
                            strSubDepartment
                    });
            if (res.getCount() > 0)
                return true;
        }catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public Cursor getUnSharedData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME + " where " + COL_SHARE + " = ?", new String[]{"0"});

        return res;
    }
}

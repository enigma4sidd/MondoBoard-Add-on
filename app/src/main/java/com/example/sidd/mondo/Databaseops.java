package com.example.sidd.mondo;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.sql.SQLException;

import  com.example.sidd.mondo.Table.TableInfo;

public class Databaseops extends SQLiteOpenHelper {
    public static final int database_version = 1;

    public String tabname;

    public Databaseops(Context context) {

        super(context, TableInfo.database_name, null, database_version);
        Log.d("Database Operations", "Database Created");

    }


    @Override
    public void onCreate(SQLiteDatabase sdb) {

        String CREATE_QUERY = "CREATE TABLE " + TableInfo.table_name + "( " + TableInfo.routename + " TEXT , " + TableInfo.lat + " REAL, " + TableInfo.lon + " REAL );";

        sdb.execSQL(CREATE_QUERY);

        Log.d("Database Operations", "Table Created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }

    public void putInfo(Databaseops dop, String routename, Double lat, Double lon) {

        SQLiteDatabase SQ = dop.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(TableInfo.routename, routename);
        cv.put(TableInfo.lat, lat);
        cv.put(TableInfo.lon, lon);
        long k = SQ.insert(TableInfo.table_name,null , cv);

        Log.d("Database Operations", "One row inserted");
    }

    public Cursor getInfo(Databaseops dop, String function) {
        SQLiteDatabase SQ = dop.getReadableDatabase();
        String col;
        Cursor CR;
        if (function=="getData")
      col =   TableInfo.routename;
        else     col = TableInfo.table_name;
      String[] colarray = {col};
        if (function=="getData")
        {CR = SQ.query(TableInfo.table_name, colarray, TableInfo.routename + "=" +TableInfo.routename, null, null, null, null);}
        else
         CR= SQ.rawQuery("SELECT "+TableInfo.routename +" as _id FROM " +TableInfo.table_name, null);
       String cursorvalue =  DatabaseUtils.dumpCursorToString(CR);
        return CR;

    }

    public static boolean CheckIsDataAlreadyInDBorNot(Databaseops dp, String fieldValue) {
        SQLiteDatabase sqldb = dp.getReadableDatabase();
        Cursor c=sqldb.rawQuery("SELECT * FROM myroute WHERE "+TableInfo.routename+"= "+"'"+ fieldValue+"'", null);
        if(c.moveToFirst())
        {
           return true;
        }
        else
        {
           return false;
        }
    }

}
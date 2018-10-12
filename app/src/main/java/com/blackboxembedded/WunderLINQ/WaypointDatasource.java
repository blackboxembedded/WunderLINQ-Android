package com.blackboxembedded.WunderLINQ;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class WaypointDatasource {
    final String TAG = "WunderLINQ";
    private SQLiteDatabase db;
    private WaypointDatabase dbHelper;
    String sqlTable = "records";

    public WaypointDatasource(Context context) {
        dbHelper = new WaypointDatabase(context);
    }

    public void open() throws SQLException {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    //Get records in database
    public List<WaypointRecord> getAllRecords() {
        List<WaypointRecord> pageList = new ArrayList<>();

        Cursor c = db.query(sqlTable, new String[] {"_id", "date", "data", "label"}, null, null, null, null,  "date DESC");

        if (c != null) {
            c.moveToFirst();
        }
        while (!c.isAfterLast()) {
            WaypointRecord record = cursorToRecord(c);
            pageList.add(record);
            c.moveToNext();
        }

        c.close();
        return pageList;
    }
    public Cursor getAllRecordsCursor() {

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(sqlTable, new String[] {"date", "data", "label"}, null, null, null, null,  "date DESC");
        if (c != null) {
            c.moveToFirst();
        }

        return c;
    }
    // Add record to database
    void addRecord(WaypointRecord record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("date", record.getDate());
        values.put("data", record.getData());
        values.put("label", record.getLabel());

        // Inserting Row
        db.insert(sqlTable, null, values);
        db.close();
    }
    // Add label to record
    void addLabel(long record, String label) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("label",label);
        db.update(sqlTable, cv, "_id = ?", new String[]{String.valueOf(record)});
        db.close();
    }
    // Remove record from database
    void removeRecord(WaypointRecord record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(sqlTable, "_id=?", new String[] { Long.toString(record.getID()) });
        db.close();
    }
    // Return record from database
    WaypointRecord returnRecord(String id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.query(sqlTable, new String[] {"_id", "date", "data", "label"}, "_id=?", new String[] {id}, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        WaypointRecord record = cursorToRecord(c);
        db.close();
        return record;
    }

    private WaypointRecord cursorToRecord(Cursor cursor) {
        WaypointRecord record = new WaypointRecord();
        record.setID(cursor.getLong(0));
        record.setDate(cursor.getString(cursor.getColumnIndex("date")));
        record.setData(cursor.getString(cursor.getColumnIndex("data")));
        record.setLabel(cursor.getString(cursor.getColumnIndex("label")));
        return record;
    }

}

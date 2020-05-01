/*
WunderLINQ Client Application
Copyright (C) 2020  Keith Conger, Black Box Embedded, LLC

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.blackboxembedded.WunderLINQ;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class WaypointDatasource {
    final String TAG = "WptDataSource";
    private SQLiteDatabase db;
    private WaypointDatabase dbHelper;
    private String sqlTable = "records";

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
        SQLiteDatabase db = dbHelper.getReadableDatabase();
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
        db.close();
        return pageList;
    }
    // Add record to database
    public void addRecord(WaypointRecord record) {
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

    public Cursor getAllRecordsCursor() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(sqlTable, new String[] {"date", "data", "label"}, null, null, null, null,  "date DESC");
        if (c != null) {
            c.moveToFirst();
        }
        db.close();
        return c;
    }

    private WaypointRecord cursorToRecord(Cursor cursor) {
        WaypointRecord record = new WaypointRecord();
        record.setID(cursor.getInt(0));
        record.setDate(cursor.getString(cursor.getColumnIndex("date")));
        record.setData(cursor.getString(cursor.getColumnIndex("data")));
        record.setLabel(cursor.getString(cursor.getColumnIndex("label")));
        return record;
    }
}

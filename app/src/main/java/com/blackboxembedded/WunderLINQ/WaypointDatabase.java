package com.blackboxembedded.WunderLINQ;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class WaypointDatabase extends SQLiteAssetHelper {

    final String TAG = "WunderLINQ";
    private static final String DATABASE_NAME = "waypoints.db";
    private static final int DATABASE_VERSION = 3;

    public WaypointDatabase(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgrade();

    }

}

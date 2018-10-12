package com.blackboxembedded.WunderLINQ;

public class WaypointRecord {
    //private variables
    long _id;
    String _date;
    String _data;
    String _label;

    // Empty constructor
    public WaypointRecord(){

    }
    // Constructor
    public WaypointRecord(String date, String data, String label) {
        this._date = date;
        this._data = data;
        this._label = label;
    }

    public long getID(){
        return this._id;
    }

    public void setID(long id) {
        this._id = id;
    }

    public String getDate(){
        return this._date;
    }

    public void setDate(String date){
        this._date = date;
    }

    public void setData(String data){
        this._data = data;
    }
    public String getData(){
        return this._data;
    }

    public void setLabel(String label){
        this._label = label;
    }
    public String getLabel(){
        return this._label;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return _date + "    " + _data ;
    }
}

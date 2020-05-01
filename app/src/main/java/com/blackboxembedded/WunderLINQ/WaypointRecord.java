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

public class WaypointRecord {

    public final static String TAG = "WaypointRecord";

    int _id;
    String _date;
    String _data;
    String _label;

    public WaypointRecord(){

    }

    public WaypointRecord(String date, String data, String label) {
        this._date = date;
        this._data = data;
        this._label = label;
    }

    public int getID(){
        return this._id;
    }

    public void setID(int id) {
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

    @Override
    public String toString() {
        return _date + "    " + _data ;
    }

    @Override
    public int hashCode() {
        return _id;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof WaypointRecord)) {
            return false;
        }
        WaypointRecord otherPoint = (WaypointRecord)other;
        return otherPoint._id == this._id;
    }
}

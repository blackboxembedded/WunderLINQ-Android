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

public class PermissionRecord {

    int _id;
    String _label;
    Boolean _enabled;

    public PermissionRecord(){
    }

    public PermissionRecord(int id, String label, Boolean enabled) {
        this._id = id;
        this._label = label;
        this._enabled = enabled;
    }

    public void setID(int id){
        this._id = id;
    }
    public int getID(){
        return this._id;
    }

    public void setLabel(String label){
        this._label = label;
    }
    public String getLabel(){
        return this._label;
    }

    public void setEnabled(Boolean enabled){
        this._enabled = enabled;
    }
    public Boolean getEnabled(){
        return this._enabled;
    }

    @Override
    public String toString() {
        return _label ;
    }
}

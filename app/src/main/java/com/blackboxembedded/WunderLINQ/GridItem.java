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

import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;

import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;

public class GridItem {
    private final static String TAG = "GridItem";

    private  MotorcycleData.DataType dataType;

    private String label;
    private String value;
    @ColorInt
    private Integer valueColor;
    private Drawable icon;

    public GridItem(Drawable icon, String label, String value, Integer valueColor,  MotorcycleData.DataType type) {
        this.icon = icon;
        this.label = label;
        this.value = value;
        this.valueColor = valueColor;
        this.dataType = type;
    }

    public String getLabel() {
        return label;
    }
    public String getValue() {
        return value;
    }
    public Integer getValueColor() {
        return valueColor;
    }
    public Drawable getIcon() {
        return icon;
    }
    public  MotorcycleData.DataType dataType() {
        return dataType;
    }



    public static GridItem getCellData( MotorcycleData.DataType dataPoint){
        Object[] retObj =  MotorcycleData.getCombinedData(dataPoint);

        String dataVal = (String) retObj[0];
        String label = (String) retObj[1];
        Drawable icon = (Drawable) retObj[2];
        @ColorInt Integer valueColor = (Integer) retObj[3];

        return new GridItem (  icon, label, dataVal, valueColor, dataPoint);
    }
}
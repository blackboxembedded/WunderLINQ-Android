package com.blackboxembedded.WunderLINQ.TaskList;


import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;

import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;

public class GridItem {
    private final static String TAG = "GridItem";

    private  MotorcycleData.DataType dataType;

    private String label;
    private String value;
    @ColorInt
    private Integer valueColor;
    private Drawable icon;

    public GridItem(Drawable icon, String label, String value) {
        this.icon = icon;
        this.label = label;
        this.value = value;
        this.valueColor = R.attr.buttonTextColor;
    }
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
        String label = (String) retObj[1]; // Data.getLabel(dataPoint);/ getString(R.string.blank_field); // (!Data.getValue(dataPoint).equals("")) ? Data.getValue(dataPoint) : getString(R.string.blank_field))
        Drawable icon = (Drawable) retObj[2]; //Data.getIcon(dataPoint);
        @ColorInt Integer valueColor = (Integer) retObj[3];  // Data.getValueColor(dataPoint, dataVal);

        return new GridItem (  icon, label, dataVal, valueColor, dataPoint);
    }
}
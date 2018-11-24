package com.blackboxembedded.WunderLINQ;

/**
 * Created by keithconger on 9/5/17.
 */

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class TaskListView extends ArrayAdapter<String>{

    private final Activity context;
    private final List<String> label;
    private final List<Drawable> icon;
    private final boolean itsDark;
    public TaskListView(Activity context,
                        List<String> label, List<Drawable> icon, boolean itsDark) {
        super(context, R.layout.list_task, label);
        this.context = context;
        this.label = label;
        this.icon = icon;
        this.itsDark = itsDark;

    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.list_task, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(R.id.tv_label);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.iv_icon);

        txtTitle.setText(label.get(position));
        if (itsDark){
            txtTitle.setTextColor(Color.WHITE);
        } else {
            txtTitle.setTextColor(Color.BLACK);
        }
        imageView.setImageDrawable(icon.get(position));
        return rowView;
    }
}

package com.blackboxembedded.WunderLINQ;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class WaypointListView extends ArrayAdapter {
    private final Activity context;
    private final List<WaypointRecord> label;

    public WaypointListView(Activity context,
                            List<WaypointRecord> label) {
        super(context, R.layout.item_waypoint, label);
        this.context = context;
        this.label = label;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.item_waypoint, null, true);
        TextView txtTitle = rowView.findViewById(R.id.tv_label);

        WaypointRecord record = label.get(position);
        if (!record.getLabel().equals("")){
            txtTitle.setText(record.getLabel());
        } else {
            txtTitle.setText(record.getDate());
        }

        return rowView;
    }
}

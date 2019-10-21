package com.blackboxembedded.WunderLINQ;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class TripListView extends ArrayAdapter {
    private final Activity context;
    private final List<String> tripList;
    public TripListView(Activity context,
                        List<String> tripList) {
        super(context, R.layout.list_trip, tripList);
        this.context = context;
        this.tripList = tripList;

    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.list_trip, null, true);
        TextView txtTitle = rowView.findViewById(R.id.tv_label);

        txtTitle.setText(tripList.get(position));
        return rowView;
    }
}

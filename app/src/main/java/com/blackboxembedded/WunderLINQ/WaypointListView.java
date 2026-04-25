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

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

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

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        View rowView = view;
        ViewHolder holder;
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.item_waypoint, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = rowView.findViewById(R.id.tv_label);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        WaypointRecord record = label.get(position);
        if (!record.getLabel().isEmpty()){
            holder.txtTitle.setText(record.getLabel());
        } else {
            holder.txtTitle.setText(record.getDate());
        }

        return rowView;
    }

    static class ViewHolder {
        TextView txtTitle;
    }
}

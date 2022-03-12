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
package com.blackboxembedded.WunderLINQ.TaskList.Activities;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.blackboxembedded.WunderLINQ.AppInfo;
import com.blackboxembedded.WunderLINQ.R;

import java.util.List;

public class AppListView extends ArrayAdapter<AppInfo> {

    private final Activity context;
    private final List<AppInfo> apps;

    public AppListView(Activity context,
                       List<AppInfo> apps) {
        super(context, R.layout.item_contact, apps);
        this.context = context;
        this.apps = apps;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.item_contact, null, true);
        TextView txtTitle = rowView.findViewById(R.id.tv_label);
        ImageView imageView = rowView.findViewById(R.id.iv_icon);

        txtTitle.setText(apps.get(position).label);
        if (apps.get(position).icon != null) {
            imageView.setImageDrawable(apps.get(position).icon);
            imageView.setImageTintMode(null);
        }

        return rowView;
    }
}

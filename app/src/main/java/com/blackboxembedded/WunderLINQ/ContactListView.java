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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ContactListView extends ArrayAdapter<String>{

    private final Activity context;
    private final ArrayList<String> label;
    private final ArrayList<Drawable> icon;

    public ContactListView(Activity context,
                           ArrayList<String> label, ArrayList<Drawable> icon) {
        super(context, R.layout.item_contact, label);
        this.context = context;
        this.label = label;
        this.icon = icon;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.item_contact, null, true);
        TextView txtTitle = rowView.findViewById(R.id.tv_label);
        ImageView imageView = rowView.findViewById(R.id.iv_icon);

        txtTitle.setText(label.get(position));
        if (icon.get(position) != null) {
            imageView.setImageDrawable(icon.get(position));
            imageView.setImageTintMode(null);
        }

        return rowView;
    }
}

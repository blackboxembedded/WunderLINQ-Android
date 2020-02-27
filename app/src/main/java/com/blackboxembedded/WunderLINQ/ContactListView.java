package com.blackboxembedded.WunderLINQ;

/**
 * Created by keithconger on 9/5/17.
 */

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
        imageView.setImageDrawable(icon.get(position));
        if( imageView.getDrawable().getConstantState() != context.getResources().getDrawable( R.drawable.ic_default_contact).getConstantState()){
            imageView.setImageTintMode(null);
        }

        return rowView;
    }
}

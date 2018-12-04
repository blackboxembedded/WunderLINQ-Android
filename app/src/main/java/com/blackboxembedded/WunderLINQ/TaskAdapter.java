package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class TaskAdapter extends BaseAdapter {

    public final static String TAG = "TaskAdapter";

    private Context mContext;
    private final List<String> label;
    private final List<Drawable> icon;
    private final boolean itsDark;

    public TaskAdapter(Context context, List<String> label, List<Drawable> icon, boolean itsDark) {
        this.mContext = context;
        this.label = label;
        this.icon = icon;
        this.itsDark = itsDark;
    }

    public int getCount() {
        return label.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View gridView;
        if (convertView == null) {
            gridView = new View(mContext);
            gridView = inflater.inflate(R.layout.grid_task, null);
            // set image
            ImageView imageView = (ImageView) gridView.findViewById(R.id.gridImageView);
            imageView.setImageDrawable(icon.get(position));
            // set text
            TextView textView = (TextView) gridView.findViewById(R.id.gridTextView);
            textView.setText(label.get(position));
            if (itsDark){
                textView.setTextColor(Color.WHITE);
            } else {
                textView.setTextColor(Color.BLACK);
            }
        } else {
            gridView = (View) convertView;
        }
        return gridView;
    }
}

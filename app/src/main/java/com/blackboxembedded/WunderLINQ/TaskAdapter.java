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
            gridView = inflater.inflate(R.layout.grid_task, null);
            ImageView imageView = gridView.findViewById(R.id.gridImageView);
            TextView textView = gridView.findViewById(R.id.gridTextView);

            textView.setText(label.get(position));
            if (itsDark){
                imageView.setImageDrawable(icon.get(position));
                textView.setTextColor(Color.WHITE);
            } else {
                Drawable temp = icon.get(position);
                temp.setTintList(mContext.getResources().getColorStateList(R.color.task_text_color_light));
                imageView.setImageDrawable(temp);
                textView.setTextColor(mContext.getResources().getColorStateList(R.color.task_text_color_light));
            }
        } else {
            gridView = convertView;
        }

        return gridView;
    }
}

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
package com.blackboxembedded.WunderLINQ.TaskList;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blackboxembedded.WunderLINQ.R;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.RecyclerViewHolder> {

    private ArrayList<TaskItem> dataSource = new ArrayList<TaskItem>();
    public interface AdapterCallback{
        void onItemClicked(Integer menuPosition);
    }
    private AdapterCallback callback;

    private Context context;

    public int selected = 0;

    public TaskAdapter(Context context, ArrayList<TaskItem> dataArgs, AdapterCallback callback){
        this.context = context;
        this.dataSource = dataArgs;
        this.callback = callback;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task,parent,false);
        RecyclerViewHolder recyclerViewHolder = new RecyclerViewHolder(view);
        return recyclerViewHolder;
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder
    {
        ConstraintLayout taskContainer;
        TextView taskItem;
        ImageView taskIcon;

        public RecyclerViewHolder(View view) {
            super(view);
            taskContainer = view.findViewById(R.id.task_container);
            taskItem = view.findViewById(R.id.task_text);
            taskIcon = view.findViewById(R.id.task_icon);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, final int position) {

        TaskItem data_provider = dataSource.get(position);
        holder.taskIcon.setImageResource(data_provider.getImage());
        holder.taskIcon.setImageResource(data_provider.getImage());
        holder.taskItem.setText(data_provider.getText());

        holder.taskContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                selected = position;
                if(callback != null) {
                    callback.onItemClicked(position);

                    int highlightColor = PreferenceManager.getDefaultSharedPreferences(context).getInt("prefHighlightColor", R.color.colorAccent);
                    GradientDrawable shape = new GradientDrawable();
                    shape.setShape(GradientDrawable.OVAL);
                    shape.setColor(highlightColor);
                    shape.setStroke(5, highlightColor);
                    holder.taskIcon.setBackground(shape);
                }
            }
        });

        if(selected == position){
            int highlightColor = PreferenceManager.getDefaultSharedPreferences(context).getInt("prefHighlightColor", R.color.colorAccent);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(highlightColor);
            shape.setStroke(5, highlightColor);
            holder.taskIcon.setBackground(shape);
        } else {
            holder.taskIcon.setBackgroundColor(context.getColor(R.color.clear));
        }
    }

    @Override
    public int getItemCount() {
        return dataSource.size();
    }

}

class TaskItem {
    private String text;
    private int image;

    public TaskItem(int image, String text) {
        this.image = image;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public int getImage() {
        return image;
    }
}
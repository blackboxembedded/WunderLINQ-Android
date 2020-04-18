package com.blackboxembedded.WunderLINQ.TaskList;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.blackboxembedded.WunderLINQ.R;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.RecyclerViewHolder> {

    private ArrayList<MenuItem> dataSource = new ArrayList<MenuItem>();
    public interface AdapterCallback{
        void onItemClicked(Integer menuPosition);
    }
    private AdapterCallback callback;

    private Context context;

    public int selected = 0;

    public TaskAdapter(Context context, ArrayList<MenuItem> dataArgs, AdapterCallback callback){
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
        RelativeLayout menuContainer;
        TextView menuItem;
        ImageView menuIcon;

        public RecyclerViewHolder(View view) {
            super(view);
            menuContainer = view.findViewById(R.id.menu_container);
            menuItem = view.findViewById(R.id.menu_item);
            menuIcon = view.findViewById(R.id.menu_icon);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, final int position) {

        MenuItem data_provider = dataSource.get(position);
        holder.menuIcon.setImageResource(data_provider.getImage());
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            holder.menuIcon.setRotation(270.0f);
            holder.menuItem.setText("");
        } else {
            holder.menuIcon.setImageResource(data_provider.getImage());
            holder.menuItem.setText(data_provider.getText());
        }

        holder.menuContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                selected = position;
                if(callback != null) {
                    callback.onItemClicked(position);
                }
            }
        });

        if(selected == position){
            holder.menuIcon.setBackground(context.getDrawable(R.drawable.icon_highlight));
        } else {
            holder.menuIcon.setBackgroundColor(context.getColor(R.color.clear));
        }
    }

    @Override
    public int getItemCount() {
        return dataSource.size();
    }

    private Drawable getRotateDrawable(final Drawable d, final float angle) {
        final Drawable[] arD = { d };
        return new LayerDrawable(arD) {
            @Override
            public void draw(final Canvas canvas) {
                canvas.save();
                canvas.rotate(angle, d.getIntrinsicWidth() / 2, d.getIntrinsicHeight() / 2);
                super.draw(canvas);
                canvas.restore();
            }
        };
    }
}

class MenuItem {
    private String text;
    private int image;

    public MenuItem(int image, String text) {
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
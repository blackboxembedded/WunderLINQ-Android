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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HWSettingsRecyclerViewAdapter extends RecyclerView.Adapter<HWSettingsRecyclerViewAdapter.ViewHolder>  {
    private ArrayList<ActionItem> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    HWSettingsRecyclerViewAdapter(Context context, ArrayList<ActionItem> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_hwsettings, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String label = mData.get(position).getLabel();
        String key = mData.get(position).getKey();
        holder.actionLabelTV.setText(label);
        holder.actionKeyTV.setText(key);
        if(key.equals("")){
            holder.actionLabelTV.setTextSize(32);
            holder.actionKeyTV.setVisibility(View.GONE);
        } else {
            holder.actionLabelTV.setTextSize(21);
            holder.actionKeyTV.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        LinearLayout actionLL;
        TextView actionLabelTV;
        TextView actionKeyTV;

        ViewHolder(View itemView) {
            super(itemView);
            actionLL = itemView.findViewById(R.id.llAction);
            actionLabelTV = itemView.findViewById(R.id.tvActionLabel);
            actionKeyTV = itemView.findViewById(R.id.tvActionKey);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    int getActionID(int id) {
        return mData.get(id).getID();
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

}

class ActionItem {
    private int id;
    private String label;
    private String key;

    public ActionItem(int id, String label, String key) {
        this.id = id;
        this.label = label;
        this.key = key;
    }

    public int getID() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getKey() {
        return key;
    }
}
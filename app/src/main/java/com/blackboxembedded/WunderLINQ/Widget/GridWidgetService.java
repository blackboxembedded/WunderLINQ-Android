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
package com.blackboxembedded.WunderLINQ.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.blackboxembedded.WunderLINQ.MainActivity;
import com.blackboxembedded.WunderLINQ.R;

public class GridWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new GridRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class GridRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context context;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public GridRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        if (intent != null && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
    }

    @Override
    public void onDestroy() {
        WidgetProvider.data.clear();
    }

    @Override
    public int getCount() {
        return WidgetProvider.data.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_grid_item);
        rv.setTextViewText(R.id.grid_item_label, WidgetProvider.labels.get(position));
        rv.setTextViewText(R.id.grid_item_text, WidgetProvider.data.get(position));
        rv.setImageViewBitmap(R.id.grid_item_icon, drawableToBitmap(WidgetProvider.icons.get(position)));

        // Create an Intent to launch your main activity (or any other activity)
        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Make it possible to distinguish the individual on-click
        // action of a given item
        rv.setOnClickFillInIntent(R.id.grid_item_layout, launchIntent);
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public Bitmap drawableToBitmap(Drawable drawable) {
        // Set fallback size if width/height not specified
        int targetWidth = 40;
        int targetHeight = 40;

        Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            // Just scale to fill
            drawable.setBounds(0, 0, targetWidth, targetHeight);
        } else {
            // Calculate aspect-ratio-preserving scale and offset
            float widthRatio = (float) targetWidth / intrinsicWidth;
            float heightRatio = (float) targetHeight / intrinsicHeight;
            float scale = Math.min(widthRatio, heightRatio); // scale to fit

            int scaledWidth = Math.round(intrinsicWidth * scale);
            int scaledHeight = Math.round(intrinsicHeight * scale);

            int dx = (targetWidth - scaledWidth) / 2;
            int dy = (targetHeight - scaledHeight) / 2;

            drawable.setBounds(dx, dy, dx + scaledWidth, dy + scaledHeight);
        }

        drawable.draw(canvas);
        return bitmap;
    }

}
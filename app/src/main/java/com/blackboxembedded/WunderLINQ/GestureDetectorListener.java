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
import androidx.core.view.GestureDetectorCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class GestureDetectorListener implements OnTouchListener {
    private final GestureDetectorCompat gestureDetector;
    public int cell;

    public GestureDetectorListener(Context context) {
        gestureDetector = new GestureDetectorCompat(context, new GestureListener());
    }

    public void onPressLong() {
    }

    public void onSwipeLeft() {
    }

    public void onSwipeRight() {
    }

    public void onSwipeUp() {
    }

    public void onSwipeDown() {
    }

    public boolean onTouch(View v, MotionEvent event) {
        if(v.getTag() != null){
            cell = Integer.parseInt(v.getTag().toString());
        }
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends OnSwipeListener {

        @Override
        public void onLongPress(MotionEvent e) {
            onPressLong();
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSwipe(OnSwipeListener.Direction direction) {
            if (direction==Direction.up){
                onSwipeUp();
            }
            if (direction==Direction.down){
                onSwipeDown();
            }
            if (direction==Direction.left){
                onSwipeLeft();
            }
            if (direction==Direction.right){
                onSwipeRight();
            }
            return true;
        }
    }
}

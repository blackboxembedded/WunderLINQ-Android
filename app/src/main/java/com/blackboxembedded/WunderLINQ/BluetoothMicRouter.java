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

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public final class BluetoothMicRouter {
    private static final String TAG = "BluetoothMicRouter";
    private static final long LEGACY_SCO_TIMEOUT_MS = 4000; // fail-safe

    private final Context appContext;
    private final AudioManager audioManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable private BroadcastReceiver scoReceiver;
    @Nullable private Runnable pendingCallback;
    @Nullable private Runnable scoTimeout;

    private boolean usingBtMic = false;
    private boolean waitingForSco = false;

    public BluetoothMicRouter(@NonNull Context ctx) {
        this.appContext = ctx.getApplicationContext();
        this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Attempts to route input to a Bluetooth microphone if one is present.
     * Calls {@code onInputReady.run()}:
     *  - immediately if routing succeeds (API 31+) or no BT mic is present (fallback to built-in),
     *  - after SCO connects on legacy devices (<=30), or after a timeout (fallback).
     */
    public void routeToBluetoothIfPresentThen(@NonNull Runnable onInputReady) {
        if (audioManager == null) {
            Log.w(TAG, "AudioManager null; continuing without BT mic");
            runOnMain(onInputReady);
            return;
        }

        // API 31+ modern routing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
                Log.w(TAG, "BLUETOOTH_CONNECT not granted; continuing without BT routing");
                runOnMain(onInputReady);
                return;
            }

            try {
                AudioDeviceInfo bt = findBluetoothInputApi31Plus();
                if (bt != null) {
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    boolean ok = audioManager.setCommunicationDevice(bt);
                    usingBtMic = ok;
                    Log.d(TAG, "setCommunicationDevice -> " + ok + " (" + deviceLabel(bt) + ")");
                    // Either way, start now (if ok=true it's BT, else built-in)
                    runOnMain(onInputReady);
                    return;
                }
            } catch (SecurityException se) {
                Log.w(TAG, "setCommunicationDevice denied", se);
            } catch (Throwable t) {
                Log.w(TAG, "API31+ routing error", t);
            }

            // No BT mic found; continue with built-in
            runOnMain(onInputReady);
            return;
        }

        // Legacy (<=30): SCO
        if (audioManager.isBluetoothScoAvailableOffCall()) {
            try {
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                // Some OEMs require this deprecated toggle for SCO mic path pre-31:
                try {
                    audioManager.setBluetoothScoOn(true);
                } catch (Throwable ignored) { }

                waitingForSco = true;
                registerScoReceiver(onInputReady);

                audioManager.startBluetoothSco();
                Log.d(TAG, "startBluetoothSco() called; waiting for SCO connect");

                // Fail-safe timeout: start anyway after X ms if SCO never connects
                scoTimeout = () -> {
                    Log.w(TAG, "SCO connect timeout; continuing without BT mic");
                    waitingForSco = false;
                    runAndClearPending(onInputReady);
                };
                mainHandler.postDelayed(scoTimeout, LEGACY_SCO_TIMEOUT_MS);
                return;
            } catch (Throwable t) {
                Log.w(TAG, "Legacy SCO routing error; continuing without BT mic", t);
            }
        }

        // No SCO available; continue with built-in
        runOnMain(onInputReady);
    }

    /** Undo routing and restore audio mode. Safe to call multiple times. */
    public void clearRouting() {
        // Cancel timeout/pending
        if (scoTimeout != null) {
            mainHandler.removeCallbacks(scoTimeout);
            scoTimeout = null;
        }
        pendingCallback = null;

        // Unregister receiver
        if (scoReceiver != null) {
            try { appContext.unregisterReceiver(scoReceiver); } catch (Throwable ignored) {}
            scoReceiver = null;
        }

        // Clear routing
        if (audioManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.clearCommunicationDevice();
                } else {
                    if (waitingForSco || usingBtMic) {
                        try { audioManager.stopBluetoothSco(); } catch (Throwable ignored) {}
                        try {
                            audioManager.setBluetoothScoOn(false);
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "clearRouting error", t);
            }

            try {
                audioManager.setMode(AudioManager.MODE_NORMAL);
            } catch (Throwable ignored) {}
        }

        usingBtMic = false;
        waitingForSco = false;
    }

    /** True if we successfully routed to a BT mic. */
    public boolean isUsingBtMic() { return usingBtMic; }

    /** True while <=30 is waiting for SCO connection. */
    public boolean isWaitingForSco() { return waitingForSco; }

    // ---------- Internals ----------

    @Nullable
    private AudioDeviceInfo findBluetoothInputApi31Plus() {
        // Look through input devices and find first BT-capable mic
        for (AudioDeviceInfo dev : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            int type = dev.getType();
            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && type == AudioDeviceInfo.TYPE_BLE_HEADSET)) {
                return dev;
            }
        }
        return null;
    }

    private void registerScoReceiver(@NonNull Runnable onInputReady) {
        if (scoReceiver != null) return;

        pendingCallback = onInputReady;

        IntentFilter f = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        scoReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (!AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(intent.getAction())) return;
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    Log.d(TAG, "SCO connected");
                    usingBtMic = true;
                    waitingForSco = false;

                    // Cancel timeout, run callback
                    if (scoTimeout != null) {
                        mainHandler.removeCallbacks(scoTimeout);
                        scoTimeout = null;
                    }
                    runAndClearPending(null);

                } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    Log.d(TAG, "SCO disconnected");
                }
            }
        };
        appContext.registerReceiver(scoReceiver, f);
    }

    private void runAndClearPending(@Nullable Runnable fallback) {
        Runnable cb = pendingCallback != null ? pendingCallback : fallback;
        pendingCallback = null;
        if (cb != null) runOnMain(cb);
    }

    private void runOnMain(@NonNull Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else mainHandler.post(r);
    }

    private static String deviceLabel(AudioDeviceInfo dev) {
        String addr = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28+
            try { addr = dev.getAddress(); } catch (Throwable ignored) {}
        }
        CharSequence pn = dev.getProductName(); // avail since API 23
        String name = (pn != null) ? pn.toString() : "unknown";

        // On API < 28 we won't have an address; include id/name instead.
        return "type=" + dev.getType()
                + " id=" + dev.getId()
                + " name=" + name
                + ((addr != null && !addr.isEmpty()) ? " addr=" + addr : "");
    }
}

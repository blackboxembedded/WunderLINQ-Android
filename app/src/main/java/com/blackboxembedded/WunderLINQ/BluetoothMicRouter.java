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
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioFocusRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public final class BluetoothMicRouter {
    private static final String TAG = "BluetoothMicRouter";
    private static final long LEGACY_SCO_TIMEOUT_MS = 4000;

    private final Context appContext;
    private final AudioManager audioManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable private BroadcastReceiver scoReceiver;
    @Nullable private Runnable scoTimeout;
    @Nullable private Runnable pendingCallback;

    // Audio focus (26+) or legacy focus
    @Nullable private AudioFocusRequest focusRequest;
    @Nullable private AudioManager.OnAudioFocusChangeListener legacyFocusCb;

    private boolean usingBtMic = false;
    private boolean waitingForSco = false;

    public BluetoothMicRouter(@NonNull Context ctx) {
        appContext = ctx.getApplicationContext();
        audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Route to a Bluetooth mic if available, then run onInputReady.
     * Always calls the callback (falls back to built-in mic if routing fails).
     */
    public void routeToBluetoothIfPresentThen(@NonNull Runnable onInputReady) {
        if (audioManager == null) { runOnMain(onInputReady); return; }

        // 1) Ask for voice-comm focus and set COMM mode (helps a lot of stacks)
        requestVoiceCommFocus();
        try { audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION); } catch (Throwable ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 2) API 31+: pick a communication device (BLE preferred, then SCO)
            AudioDeviceInfo selected = pickBtCommDevice31Plus();
            boolean ok = false;
            if (selected != null) {
                ok = audioManager.setCommunicationDevice(selected);
                usingBtMic = ok;
                Log.d(TAG, "setCommunicationDevice -> " + ok + " (" + deviceLabel(selected) + ")");
            } else {
                Log.d(TAG, "No BT communication devices currently listed");
            }

            // 3) If that didn’t stick, try SCO as a fallback (some stacks need SCO handshake)
            if (!ok && audioManager.isBluetoothScoAvailableOffCall()) {
                startScoThen(onInputReady, /*retrySetCommDevice=*/true);
                return;
            }

            // 4) Log current comm device and continue
            AudioDeviceInfo cur = audioManager.getCommunicationDevice();
            Log.d(TAG, "Current COMM device: " + (cur == null ? "null" : deviceLabel(cur)));
            runOnMain(onInputReady);
            return;
        }

        // 5) Legacy (<=30): use SCO if available, else continue
        if (audioManager.isBluetoothScoAvailableOffCall()) {
            startScoThen(onInputReady, /*retrySetCommDevice=*/false);
        } else {
            runOnMain(onInputReady);
        }
    }

    /** Undo routing and restore defaults. Safe to call multiple times. */
    public void clearRouting() {
        if (scoTimeout != null) {
            mainHandler.removeCallbacks(scoTimeout);
            scoTimeout = null;
        }
        pendingCallback = null;

        if (scoReceiver != null) {
            try { appContext.unregisterReceiver(scoReceiver); } catch (Throwable ignored) {}
            scoReceiver = null;
        }

        if (audioManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.clearCommunicationDevice();
                } else {
                    try { audioManager.stopBluetoothSco(); } catch (Throwable ignored) {}
                    try { /*noinspection deprecation*/ audioManager.setBluetoothScoOn(false); } catch (Throwable ignored) {}
                }
            } catch (Throwable t) {
                Log.w(TAG, "clearRouting error", t);
            }
            try { audioManager.setMode(AudioManager.MODE_NORMAL); } catch (Throwable ignored) {}
        }

        abandonVoiceCommFocus();

        usingBtMic = false;
        waitingForSco = false;
    }

    public boolean isUsingBtMic() { return usingBtMic; }
    public boolean isWaitingForSco() { return waitingForSco; }

    // ---------- Internals ----------

    private void startScoThen(@NonNull Runnable onInputReady, boolean retrySetCommDeviceAfterConnected) {
        try { /*noinspection deprecation*/ audioManager.setBluetoothScoOn(true); } catch (Throwable ignored) {}
        waitingForSco = true;
        registerScoReceiver(onInputReady, retrySetCommDeviceAfterConnected);
        try {
            audioManager.startBluetoothSco();
            Log.d(TAG, "startBluetoothSco… waiting for SCO_AUDIO_STATE_CONNECTED");
        } catch (Throwable t) {
            Log.w(TAG, "startBluetoothSco failed; continuing without BT mic", t);
            waitingForSco = false;
            runOnMain(onInputReady);
            return;
        }

        // Fail-safe timeout
        scoTimeout = () -> {
            Log.w(TAG, "SCO timeout; continuing");
            waitingForSco = false;
            runOnMain(onInputReady);
        };
        mainHandler.postDelayed(scoTimeout, LEGACY_SCO_TIMEOUT_MS);
    }

    private void registerScoReceiver(@NonNull Runnable onInputReady, boolean retrySetCommDeviceAfterConnected) {
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

                    if (scoTimeout != null) {
                        mainHandler.removeCallbacks(scoTimeout);
                        scoTimeout = null;
                    }

                    // On API 31+, retry setting COMM device now that SCO is active
                    if (retrySetCommDeviceAfterConnected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        AudioDeviceInfo pick = pickBtCommDevice31Plus(/*preferSco=*/true);
                        if (pick != null) {
                            boolean ok2 = audioManager.setCommunicationDevice(pick);
                            Log.d(TAG, "setCommunicationDevice(after SCO) -> " + ok2 + " (" + deviceLabel(pick) + ")");
                            usingBtMic = usingBtMic || ok2;
                        }
                        AudioDeviceInfo cur = audioManager.getCommunicationDevice();
                        Log.d(TAG, "Current COMM device: " + (cur == null ? "null" : deviceLabel(cur)));
                    }

                    runAndClearPending();
                } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    Log.d(TAG, "SCO disconnected");
                }
            }
        };
        appContext.registerReceiver(scoReceiver, f);
    }

    @Nullable
    private AudioDeviceInfo pickBtCommDevice31Plus() {
        return pickBtCommDevice31Plus(false);
    }

    @Nullable
    private AudioDeviceInfo pickBtCommDevice31Plus(boolean preferSco) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null;
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "BLUETOOTH_CONNECT not granted; cannot list comm devices");
            return null;
        }
        try {
            List<AudioDeviceInfo> comm = audioManager.getAvailableCommunicationDevices();
            if (comm == null || comm.isEmpty()) return null;

            AudioDeviceInfo ble = null, sco = null;
            for (AudioDeviceInfo d : comm) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && d.getType() == AudioDeviceInfo.TYPE_BLE_HEADSET) ble = d;
                if (d.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) sco = d;
            }
            if (preferSco) return (sco != null) ? sco : ble;
            return (ble != null) ? ble : sco;
        } catch (Throwable t) {
            Log.w(TAG, "pickBtCommDevice31Plus error", t);
            return null;
        }
    }

    private void requestVoiceCommFocus() {
        if (audioManager == null) return;
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .setAudioAttributes(attrs)
                        .setOnAudioFocusChangeListener(fc -> {})
                        .build();
                int res = audioManager.requestAudioFocus(focusRequest);
                Log.d(TAG, "requestAudioFocus(26+) -> " + res);
            } else {
                legacyFocusCb = fc -> {};
                @SuppressWarnings("deprecation")
                int res = audioManager.requestAudioFocus(
                        legacyFocusCb,
                        AudioManager.STREAM_VOICE_CALL,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                );
                Log.d(TAG, "requestAudioFocus(legacy) -> " + res);
            }
        } catch (Throwable t) {
            Log.w(TAG, "requestVoiceCommFocus failed", t);
        }
    }

    private void abandonVoiceCommFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (focusRequest != null) {
                    audioManager.abandonAudioFocusRequest(focusRequest);
                    focusRequest = null;
                }
            } else if (legacyFocusCb != null) {
                @SuppressWarnings("deprecation")
                int res = audioManager.abandonAudioFocus(legacyFocusCb);
                legacyFocusCb = null;
                Log.d(TAG, "abandonAudioFocus(legacy) -> " + res);
            }
        } catch (Throwable ignored) {}
    }

    private void runAndClearPending() {
        Runnable cb = pendingCallback;
        pendingCallback = null;
        if (cb != null) runOnMain(cb);
    }

    private void runOnMain(@NonNull Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else mainHandler.post(r);
    }

    private static String deviceLabel(@NonNull AudioDeviceInfo dev) {
        String addr = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { addr = dev.getAddress(); } catch (Throwable ignored) {}
        }
        CharSequence pn = dev.getProductName();
        String name = (pn != null) ? pn.toString() : "unknown";
        return "type=" + dev.getType()
                + " id=" + dev.getId()
                + " name=" + name
                + ((addr != null && !addr.isEmpty()) ? " addr=" + addr : "");
    }
}

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class DashActivity extends AppCompatActivity implements View.OnTouchListener {

    public final static String TAG = "DashActivity";
    private SharedPreferences sharedPrefs;
    private SVGImageView dashboardView;
    private SVG svg;
    private SvgFileResolver svgFileResolver;
    private GestureDetectorListener gestureDetector;
    private CountDownTimer cTimer = null;
    private boolean timerRunning = false;
    private boolean dashUpdateRunning = false;

    String pressureFormat = "0";
    String temperatureFormat = "0";
    String distanceFormat = "0";
    String consumptionFormat = "0";
    String pressureUnit = "bar";
    String temperatureUnit = "C";
    String distanceUnit = "km";
    String heightUnit = "m";
    String distanceTimeUnit = "KMH";
    String consumptionUnit = "L/100";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());

        View view = findViewById(R.id.layout_dash);

        gestureDetector = new GestureDetectorListener(this){
            @Override
            public void onPressLong() {
            }

            @Override
            public void onSwipeUp() {
            }

            @Override
            public void onSwipeDown() {
            }

            @Override
            public void onSwipeLeft() {
                goForward();
            }

            @Override
            public void onSwipeRight() {
                goBack();
            }
        };

        view.setOnTouchListener(this);

        showActionBar();

        dashboardView = findViewById(R.id.mainView);
        svgFileResolver = new SvgFileResolver();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        getSupportActionBar().show();
        startTimer();
        updateUnits();
        updateDashboard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e){
            Log.d(TAG,e.toString());
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        getSupportActionBar().show();
        startTimer();
        gestureDetector.onTouch(v, event);
        return true;
    }

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setCustomView(v);

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.dash_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    goBack();
                    break;
                case R.id.action_forward:
                    goForward();
                    break;
            }
        }
    };

    //Go to next screen - Quick Tasks
    private void goForward(){
        Intent backIntent = new Intent(this, MusicActivity.class);
        startActivity(backIntent);
    }

    //Go back to last screen - Motorcycle Data
    private void goBack(){
        Intent backIntent = new Intent(this, MainActivity.class);
        startActivity(backIntent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                goBack();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                goForward();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    //start timer function
    void startTimer() {
        if(!timerRunning) {
            cTimer = new CountDownTimer(10000, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    getSupportActionBar().hide();
                    timerRunning = false;
                }
            };
            timerRunning = true;
            cTimer.start();
        }
    }

    //cancel timer
    void cancelTimer() {
        if(cTimer!=null)
            cTimer.cancel();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                updateDashboard();
            }
        }
    };

    public void updateUnits(){
        pressureFormat = sharedPrefs.getString("prefPressureF", "0");
        if (pressureFormat.contains("1")) {
            // KPa
            pressureUnit = "KPa";
        } else if (pressureFormat.contains("2")) {
            // Kg-f
            pressureUnit = "Kgf";
        } else if (pressureFormat.contains("3")) {
            // Psi
            pressureUnit = "psi";
        }
        temperatureFormat = sharedPrefs.getString("prefTempF", "0");
        if (temperatureFormat.contains("1")) {
            // F
            temperatureUnit = "F";
        }
        distanceFormat = sharedPrefs.getString("prefDistance", "0");
        if (distanceFormat.contains("1")) {
            distanceUnit = "mls";
            heightUnit = "ft";
            distanceTimeUnit = "MPH";
        }
        consumptionFormat = sharedPrefs.getString("prefConsumption", "0");
        if (consumptionFormat.contains("1")) {
            consumptionUnit = "mpg";
        } else if (consumptionFormat.contains("2")) {
            consumptionUnit = "mpg";
        } else if (consumptionFormat.contains("3")) {
            consumptionUnit = "kmL";
        }
    }

    public void updateDashboard(){
        if (!dashUpdateRunning) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    dashUpdateRunning = true;
                    try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(DashActivity.this.getAssets().open("gstft-dashboard.svg"));
                        NodeList nodeslist = doc.getElementsByTagName("tspan");
                        for (int i = 0; i < nodeslist.getLength(); i++) {
                            Node node = nodeslist.item(i);
                            NamedNodeMap att = node.getAttributes();
                            int h = 0;
                            while (h < att.getLength()) {
                                Node tspan = att.item(h);
                                switch (tspan.getNodeValue()) {
                                    case "tspanAmbientTemp-Label":
                                        node.setTextContent(getResources().getString(R.string.dash_ambient_label) + " ");
                                        break;
                                    case "tspanEngineTemp-Label":
                                        node.setTextContent(getResources().getString(R.string.dash_engine_label) + " ");
                                        break;
                                    case "tspanRange-Label":
                                        node.setTextContent(getResources().getString(R.string.dash_range_label) + " ");
                                        break;
                                    case "tspanRDCF-Label":
                                        node.setTextContent(getResources().getString(R.string.dash_rdcf_label) + " ");
                                        break;
                                    case "tspanRDCR-Label":
                                        node.setTextContent(getResources().getString(R.string.dash_rdcr_label) + " ");
                                        break;
                                    case "tspanSpeed-Label":
                                        node.setTextContent(distanceTimeUnit);
                                        break;
                                    case "tspanSpeed":
                                        if (Data.getSpeed() != null) {
                                            double speed = Data.getSpeed();
                                            if (distanceFormat.contains("1")) {
                                                speed = Utils.kmToMiles(speed);
                                            }
                                            node.setTextContent(String.valueOf(Math.round(speed)));
                                        } else {
                                            node.setTextContent("-");
                                        }
                                        break;
                                    case "tspanGear":
                                        if (Data.getGear() != null) {
                                            node.setTextContent(Data.getGear());
                                            String style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#fcc914;");
                                            if (Data.getGear().equals("N")) {
                                                //Change color to green
                                                style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#03ae1e;");
                                            }
                                            att.item(h + 3).setNodeValue(style);
                                        } else {
                                            node.setTextContent("-");
                                        }
                                        break;
                                    case "tspanAmbientTemp":
                                        if (Data.getAmbientTemperature() != null) {
                                            double ambientTemp = Data.getAmbientTemperature();
                                            if (temperatureFormat.contains("1")) {
                                                // F
                                                temperatureUnit = "F";
                                                ambientTemp = Utils.celsiusToFahrenheit(ambientTemp);
                                            }
                                            node.setTextContent(Math.round(ambientTemp) + temperatureUnit);
                                        } else {
                                            node.setTextContent("-");
                                        }
                                        break;
                                    case "tspanEngineTemp":
                                        if (Data.getEngineTemperature() != null) {
                                            double engineTemp = Data.getEngineTemperature();
                                            if (temperatureFormat.contains("1")) {
                                                // F
                                                temperatureUnit = "F";
                                                engineTemp = Utils.celsiusToFahrenheit(engineTemp);
                                            }
                                            node.setTextContent(Math.round(engineTemp) + temperatureUnit);
                                            String style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#eef1f0;");
                                            if (Data.getEngineTemperature() >= 104.0) {
                                                style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#e20505;");
                                            }
                                            att.item(h + 3).setNodeValue(style);
                                        } else {
                                            node.setTextContent("-");
                                        }
                                        break;
                                    case "tspanRange":
                                        if (Data.getFuelRange() != null) {
                                            double fuelrange = Data.getFuelRange();
                                            if (distanceFormat.contains("1")) {
                                                distanceUnit = "mls";
                                                fuelrange = Utils.kmToMiles(fuelrange);
                                            }
                                            node.setTextContent(Utils.oneDigit.format(fuelrange) + distanceUnit);
                                            String style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#eef1f0;");
                                            if (FaultStatus.getfuelFaultActive()) {
                                                style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#e20505;");
                                            }
                                            att.item(h + 3).setNodeValue(style);
                                        } else {
                                            node.setTextContent("-");
                                        }
                                        break;
                                    case "tspanClock":
                                        if (Data.getTime() != null) {
                                            SimpleDateFormat dateformat = new SimpleDateFormat("h:mm", Locale.getDefault());
                                            if (!sharedPrefs.getString("prefTime", "0").equals("0")) {
                                                dateformat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                            }
                                            node.setTextContent(dateformat.format(Data.getTime()));
                                        }
                                        break;
                                    case "tspanRDCF":
                                        if (Data.getFrontTirePressure() != null) {
                                            double rdcFront = Data.getFrontTirePressure();
                                            if (pressureFormat.contains("1")) {
                                                // KPa
                                                pressureUnit = "KPa";
                                                rdcFront = Utils.barTokPa(rdcFront);
                                            } else if (pressureFormat.contains("2")) {
                                                // Kg-f
                                                pressureUnit = "Kgf";
                                                rdcFront = Utils.barTokgf(rdcFront);
                                            } else if (pressureFormat.contains("3")) {
                                                // Psi
                                                pressureUnit = "psi";
                                                rdcFront = Utils.barToPsi(rdcFront);
                                            }
                                            node.setTextContent(Utils.oneDigit.format(rdcFront) + pressureUnit);
                                            String style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#eef1f0;");
                                            if (FaultStatus.getfrontTirePressureCriticalActive()) {
                                                style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#e20505;");
                                            } else if (FaultStatus.getfrontTirePressureWarningActive()) {
                                                style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#fcc914;");
                                            }
                                            att.item(h + 3).setNodeValue(style);
                                        } else {
                                            node.setTextContent("-");
                                        }
                                        break;
                                    case "tspanRDCR":
                                        if (Data.getRearTirePressure() != null) {
                                            double rdcRear = Data.getRearTirePressure();
                                            if (pressureFormat.contains("1")) {
                                                // KPa
                                                pressureUnit = "KPa";
                                                rdcRear = Utils.barTokPa(rdcRear);
                                            } else if (pressureFormat.contains("2")) {
                                                // Kg-f
                                                pressureUnit = "Kgf";
                                                rdcRear = Utils.barTokgf(rdcRear);
                                            } else if (pressureFormat.contains("3")) {
                                                // Psi
                                                pressureUnit = "psi";
                                                rdcRear = Utils.barToPsi(rdcRear);
                                            }
                                            node.setTextContent(Utils.oneDigit.format(rdcRear) + pressureUnit);
                                            String style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#eef1f0;");
                                            if (FaultStatus.getrearTirePressureCriticalActive()) {
                                                style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#e20505;");
                                            } else if (FaultStatus.getrearTirePressureWarningActive()) {
                                                style = att.item(h + 3).getNodeValue().replaceAll("fill:([^<]*);", "fill:#fcc914;");
                                            }
                                            att.item(h + 3).setNodeValue(style);
                                        } else {
                                            node.setTextContent("-");
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                h += 1;  // To get The Next Attribute.
                            }
                        }

                        NodeList glist = doc.getElementsByTagName("g");
                        for (int i = 0; i < glist.getLength(); i++) {
                            Node gnode = glist.item(i);
                            NamedNodeMap att = gnode.getAttributes();
                            int h = 0;
                            while (h < att.getLength()) {
                                Node tspan = att.item(h);
                                switch (tspan.getNodeValue()) {
                                    case "triplog":
                                        if (((MyApplication) DashActivity.this.getApplication()).getTripRecording()) {
                                            att.item(h + 2).setNodeValue("display:inline");
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "camera":
                                        if (((MyApplication) DashActivity.this.getApplication()).getVideoRecording()) {
                                            att.item(h + 2).setNodeValue("display:inline");
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "warning":
                                        ArrayList<String> faultListData = FaultStatus.getallActiveDesc();
                                        if (!faultListData.isEmpty()) {
                                            att.item(h + 2).setNodeValue("display:inline");
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "fuel":
                                        if (FaultStatus.getfuelFaultActive()) {
                                            att.item(h + 2).setNodeValue("display:inline");
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm333":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 333) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm666":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 666) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm1000":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 1000) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm1333":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 1333) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm1666":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 1666) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm2000":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 2000) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    default:
                                        break;
                                    case "rpm2333":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 2333) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm2666":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 2666) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm3000":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 3000) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm3333":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 3333) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm3666":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 3666) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm4000":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 4000) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm4333":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 4333) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm4666":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 4666) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm5000":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 5000) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm5333":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 5333) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm5666":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 5666) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm6000":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 6000) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm6333":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 6333) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm6666":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 6666) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm7000":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 7000) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm7333":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 7333) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm7666":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 7666) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm8000":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 8000) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm8333":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 8333) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm8666":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 8666) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm9000":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 9000) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm9333":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 9333) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm9666":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 9666) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                    case "rpm10000":
                                        if (Data.getRPM() != null) {
                                            if (Data.getRPM() >= 10000) {
                                                att.item(h + 2).setNodeValue("display:inline");
                                            } else {
                                                att.item(h + 2).setNodeValue("display:none");
                                            }
                                        } else {
                                            att.item(h + 2).setNodeValue("display:none");
                                        }
                                        break;
                                }
                                h += 1;  // To get The Next Attribute.
                            }
                        }
                        TransformerFactory transformerFactory = TransformerFactory.newInstance();
                        Transformer transformer = transformerFactory.newTransformer();
                        DOMSource dSource = new DOMSource(doc);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        StreamResult result = new StreamResult(outputStream);
                        transformer.transform(dSource, result);
                        InputStream xml = new ByteArrayInputStream(outputStream.toByteArray());
                        svg = SVG.getFromInputStream(xml);
                        svg.registerExternalFileResolver(svgFileResolver);
                        dashUpdateRunning = false;
                    } catch (IOException | ParserConfigurationException | SAXException | TransformerException e) {
                        Log.d(TAG, "Exception updating dashboard: " + e.toString());
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //your code or your request that you want to run on uiThread
                            dashboardView.setSVG(svg);
                        }
                    });
                }
            }).start();
        }
    }
}

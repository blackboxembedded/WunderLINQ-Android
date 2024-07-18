package com.blackboxembedded.WunderLINQ.SVGDashboards;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Faults;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.Utils;
import com.caverock.androidsvg.SVG;

import org.w3c.dom.Document;
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

public class ADVDashboard {
    private final static String TAG = "ADVDashboard";

    private final static String SVGfilename = "adv-dashboard.svg";

    private static SharedPreferences sharedPrefs;

    private static String pressureFormat = "0";
    private static String temperatureFormat = "0";
    private static String distanceFormat = "0";
    private static String consumptionFormat = "0";
    private static String pressureUnit = "bar";
    private static String temperatureUnit = "C";
    private static String distanceUnit = "km";
    private static String heightUnit = "m";
    private static String distanceTimeUnit = "KMH";
    private static String consumptionUnit = "L/100";

    private static boolean twelveK = false;
    private static boolean tenK = false;

    public static SVG updateDashboard(int infoLine){
        try {
            // Read SVG File
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(MyApplication.getContext().getAssets().open(SVGfilename));

            // Read Settings
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
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
            if (sharedPrefs.getString("prefRPMMax", "0").equals("0")){
                tenK = true;
            } else if (sharedPrefs.getString("prefRPMMax", "0").equals("1")){
                twelveK = true;
            }

            //Speed Label
            doc.getElementById("speedUnit").setTextContent(distanceTimeUnit);

            //Icons
            //Trip Icon
            if(MyApplication.getTripRecording()){
                doc.getElementById("iconTrip").setAttribute("style","display:inline");
            }
            //Camera Icon
            if (MyApplication.getVideoRecording()) {
                doc.getElementById("iconVideo").setAttribute("style","display:inline");
            }
            //Fault Icon
            ArrayList<String> faultListData = Faults.getallActiveDesc();
            if (!faultListData.isEmpty()) {
                doc.getElementById("iconFault").setAttribute("style","display:inline");
            }
            //Fuel Icon
            if (Faults.getfuelFaultActive()) {
                doc.getElementById("iconFuel").setAttribute("style","display:inline");
            }
            //Bluetooth Icon
            if (BluetoothLeService.isConnected()){
                doc.getElementById("iconBT").setAttribute("style","display:inline");
            }
            //Values
            //Clock
            if (Data.getTime() != null) {
                SimpleDateFormat dateformat = new SimpleDateFormat("h:mm", Locale.getDefault());
                if (!sharedPrefs.getString("prefTime", "0").equals("0")) {
                    dateformat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                }
                doc.getElementById("clock").setTextContent(dateformat.format(Data.getTime()));
            }
            //Speed
            String speedSource = sharedPrefs.getString("prefDashSpeedSource", "0");
            Double speed = null;
            if (speedSource.contains("0")) {
                if (Data.getSpeed() != null) {
                    speed = Data.getSpeed();
                }
            } else if (speedSource.contains("1")) {
                if (Data.getRearSpeed() != null) {
                    speed = Data.getRearSpeed();
                }
            } else if (speedSource.contains("2")) {
                if (Data.getLastLocation() != null) {
                    speed = (Data.getLastLocation().getSpeed() * 3.6);
                }
            }
            if (speed != null){
                if (distanceFormat.contains("1")) {
                    speed = Utils.kmToMiles(speed);
                }
                String speedValue = String.valueOf(Math.round(speed));
                if (speed < 10) {
                    speedValue = String.format("%02d", Math.round(speed));
                }
                doc.getElementById("speed").setTextContent(speedValue);
            }
            //Gear
            if (Data.getGear() != null) {
                doc.getElementById("gear").setTextContent(Data.getGear());
                if (Data.getGear().equals("N")){
                    doc.getElementById("gear").setAttribute("class",
                            doc.getElementById("gear").getAttribute("class").replaceAll("st34", "st14")
                    );
                }
            }

            //Data Label
            switch (infoLine){
                case 1://Altitude
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_altitude_label));
                    break;
                case 2://Trip1
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_trip1_label));
                    break;
                case 3://Trip2
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_trip2_label));
                    break;
                case 4://Range
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_range_label));
                    break;
                default:
                    break;
            }
            //Data Value
            switch (infoLine){
                case 1://Altitude
                    if (Data.getLastLocation() != null){
                        double altitude = Data.getLastLocation().getAltitude();
                        if (distanceFormat.contains("1")) {
                            altitude = Utils.mToFeet(altitude);
                        }
                        doc.getElementById("dataValue").setTextContent(String.valueOf(Math.round(altitude)));
                    }
                    break;
                case 2://Trip1
                    if (Data.getTripOne() != null) {
                        if(Data.getTripOne() != null) {
                            double trip1 = Data.getTripOne();
                            if (distanceFormat.contains("1")) {
                                trip1 = Utils.kmToMiles(trip1);
                            }
                            doc.getElementById("dataValue").setTextContent(Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(trip1));
                        }
                    }
                    break;
                case 3://Trip2
                    if (Data.getTripTwo() != null) {
                        if(Data.getTripTwo() != null) {
                            double trip2 = Data.getTripTwo();
                            if (distanceFormat.contains("1")) {
                                trip2 = Utils.kmToMiles(trip2);
                            }
                            doc.getElementById("dataValue").setTextContent(Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(trip2));
                        }
                    }
                    break;
                case 4://Range
                    if(Data.getFuelRange() != null){
                        double fuelrange = Data.getFuelRange();
                        if (distanceFormat.contains("1")) {
                            fuelrange = Utils.kmToMiles(fuelrange);
                        }
                        doc.getElementById("dataValue").setTextContent(String.valueOf(Math.round(fuelrange)));
                    }
                    break;
                default:
                    break;
            }
            //Data Unit
            switch (infoLine){
                case 1://Altitude
                    doc.getElementById("dataUnit").setTextContent(heightUnit);
                    break;
                case 2: case 3: case 4://Trip1/2 Range
                    doc.getElementById("dataUnit").setTextContent(distanceUnit);
                    break;
                default:
                    break;
            }

            //Compass
            if(Data.getBearing() != null){
                doc.getElementById("compass").setAttribute("transform",
                        "translate(200,340) scale(3.0) rotate(" + Data.getBearing() + ",250,246)"
                );
            }


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource dSource = new DOMSource(doc);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(dSource, result);
            InputStream xml = new ByteArrayInputStream(outputStream.toByteArray());
            return SVG.getFromInputStream(xml);

        } catch (IOException | ParserConfigurationException | SAXException | TransformerException | NullPointerException e) {
            Log.d(TAG, "Exception updating dashboard: " + e.toString());
        }
        return null;
    }
}

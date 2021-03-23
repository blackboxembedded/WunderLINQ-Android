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
package com.blackboxembedded.WunderLINQ.SVGDashboards;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blackboxembedded.WunderLINQ.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.Data;
import com.blackboxembedded.WunderLINQ.FaultStatus;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils;
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

public class SportDashboard {

    private final static String TAG = "SportDashboard";

    private final static String SVGfilename = "sport-dashboard.svg";

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
            //RPM Digits For GS
            if (tenK){
                doc.getElementById("rpmDialDigit1").setTextContent("2");
                doc.getElementById("rpmDialDigit2").setTextContent("3");
                doc.getElementById("rpmDialDigit3").setTextContent("4");
                doc.getElementById("rpmDialDigit4").setTextContent("5");
                doc.getElementById("rpmDialDigit5").setTextContent("6");
                doc.getElementById("rpmDialDigit6").setTextContent("7");
                doc.getElementById("rpmDialDigit7").setTextContent("8");
                doc.getElementById("rpmDialDigit8").setTextContent("9");
                doc.getElementById("rpmDialDigit9").setTextContent("10");
            } else if(twelveK){
                doc.getElementById("rpmDialDigit1").setTextContent("2");
                doc.getElementById("rpmDialDigit2").setTextContent("4");
                doc.getElementById("rpmDialDigit3").setTextContent("6");
                doc.getElementById("rpmDialDigit4").setTextContent("7");
                doc.getElementById("rpmDialDigit5").setTextContent("8");
                doc.getElementById("rpmDialDigit6").setTextContent("9");
                doc.getElementById("rpmDialDigit7").setTextContent("10");
                doc.getElementById("rpmDialDigit8").setTextContent("11");
                doc.getElementById("rpmDialDigit9").setTextContent("12");
            }

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
            ArrayList<String> faultListData = FaultStatus.getallActiveDesc();
            if (!faultListData.isEmpty()) {
                doc.getElementById("iconFault").setAttribute("style","display:inline");
            }
            //Fuel Icon
            if (FaultStatus.getfuelFaultActive()) {
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
            String speedValue = null;
            if (speedSource.contains("0")) {
                if (Data.getSpeed() != null) {
                    double speed = Data.getSpeed();
                    if (distanceFormat.contains("1")) {
                        speed = Utils.kmToMiles(speed);
                    }
                    speedValue = String.valueOf(Math.round(speed));
                }
            } else if (speedSource.contains("1")) {
                if (Data.getRearSpeed() != null) {
                    double speed = Data.getRearSpeed();
                    if (distanceFormat.contains("1")) {
                        speed = Utils.kmToMiles(speed);
                    }
                    speedValue = String.valueOf(Math.round(speed));
                }
            } else if (speedSource.contains("2")) {
                if (Data.getLastLocation() != null) {
                    double speed = (Data.getLastLocation().getSpeed() * 3.6);
                    if (distanceFormat.contains("1")) {
                        speed = Utils.kmToMiles(speed);
                    }
                    speedValue = String.valueOf(Math.round(speed));
                }
            }
            if (speedValue != null){
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
            //Lean Angle
            if (Data.getLeanAngleBike() != null) {
                doc.getElementById("angle").setTextContent(String.format("%02d", Math.abs(Math.round(Data.getLeanAngleBike()))));
            } else if (Data.getLeanAngle() != null) {
                doc.getElementById("angle").setTextContent(String.format("%02d", Math.abs(Math.round(Data.getLeanAngle()))));
            }
            //Left Max Angle
            if (Data.getLeanAngleBikeMaxL() != null) {
                doc.getElementById("angleMaxL").setTextContent(String.valueOf(Math.round(Data.getLeanAngleBikeMaxL())));
            } else if (Data.getLeanAngleMaxL() != null) {
                doc.getElementById("angleMaxL").setTextContent(String.valueOf(Math.round(Data.getLeanAngleMaxL())));
            }
            //Right Max Angle
            if (Data.getLeanAngleBikeMaxR() != null) {
                doc.getElementById("angleMaxR").setTextContent(String.valueOf(Math.round(Data.getLeanAngleBikeMaxR())));
            } else if (Data.getLeanAngleMaxR() != null) {
                doc.getElementById("angleMaxR").setTextContent(String.valueOf(Math.round(Data.getLeanAngleMaxR())));
            }
            //Data Label
            switch (infoLine){
                case 1://Trip1
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_trip1_label));
                    break;
                case 2://Trip2
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_trip2_label));
                    break;
                case 3://Range
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_range_label));
                    break;
                default:
                    break;
            }
            //Data Value
            switch (infoLine){
                case 1://Trip1
                    if (Data.getTripOne() != null) {
                        if(Data.getTripOne() != null) {
                            double trip1 = Data.getTripOne();
                            if (distanceFormat.contains("1")) {
                                trip1 = Utils.kmToMiles(trip1);
                            }
                            doc.getElementById("dataValue").setTextContent(Utils.oneDigit.format(trip1));
                        }
                    }
                    break;
                case 2://Trip2
                    if (Data.getTripTwo() != null) {
                        if(Data.getTripTwo() != null) {
                            double trip2 = Data.getTripTwo();
                            if (distanceFormat.contains("1")) {
                                trip2 = Utils.kmToMiles(trip2);
                            }
                            doc.getElementById("dataValue").setTextContent(Utils.oneDigit.format(trip2));
                        }
                    }
                    break;
                case 3://Range
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
                case 1: case 2: case 3://Trip1/2 Range
                    doc.getElementById("dataUnit").setTextContent(distanceUnit);
                    break;
                default:
                    break;
            }
            //RPM Dial
            if (Data.getRPM() != null) {
                if (tenK) {
                    if (Data.getRPM() >= 0) {
                        doc.getElementById("rpmTick1").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 154) {
                        doc.getElementById("rpmTick2").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 308) {
                        doc.getElementById("rpmTick3").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 462) {
                        doc.getElementById("rpmTick4").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 616) {
                        doc.getElementById("rpmTick5").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 770) {
                        doc.getElementById("rpmTick6").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 924) {
                        doc.getElementById("rpmTick7").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1078) {
                        doc.getElementById("rpmTick8").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1232) {
                        doc.getElementById("rpmTick9").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1386) {
                        doc.getElementById("rpmTick10").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1540) {
                        doc.getElementById("rpmTick11").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1694) {
                        doc.getElementById("rpmTick12").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1848) {
                        doc.getElementById("rpmTick13").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2000) {
                        doc.getElementById("rpmTick14").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2083) {
                        doc.getElementById("rpmTick15").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2167) {
                        doc.getElementById("rpmTick16").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2250) {
                        doc.getElementById("rpmTick17").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2334) {
                        doc.getElementById("rpmTick18").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2417) {
                        doc.getElementById("rpmTick19").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2501) {
                        doc.getElementById("rpmTick20").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2584) {
                        doc.getElementById("rpmTick21").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2668) {
                        doc.getElementById("rpmTick22").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2751) {
                        doc.getElementById("rpmTick23").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2835) {
                        doc.getElementById("rpmTick24").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2918) {
                        doc.getElementById("rpmTick25").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3000) {
                        doc.getElementById("rpmTick26").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3077) {
                        doc.getElementById("rpmTick27").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3154) {
                        doc.getElementById("rpmTick28").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3231) {
                        doc.getElementById("rpmTick29").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3308) {
                        doc.getElementById("rpmTick30").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3385) {
                        doc.getElementById("rpmTick31").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3462) {
                        doc.getElementById("rpmTick32").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3539) {
                        doc.getElementById("rpmTick33").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3616) {
                        doc.getElementById("rpmTick34").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3693) {
                        doc.getElementById("rpmTick35").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3770) {
                        doc.getElementById("rpmTick36").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3847) {
                        doc.getElementById("rpmTick37").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3924) {
                        doc.getElementById("rpmTick38").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4000) {
                        doc.getElementById("rpmTick39").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4077) {
                        doc.getElementById("rpmTick40").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4154) {
                        doc.getElementById("rpmTick41").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4231) {
                        doc.getElementById("rpmTick42").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4308) {
                        doc.getElementById("rpmTick43").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4385) {
                        doc.getElementById("rpmTick44").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4462) {
                        doc.getElementById("rpmTick45").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4539) {
                        doc.getElementById("rpmTick46").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4616) {
                        doc.getElementById("rpmTick47").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4693) {
                        doc.getElementById("rpmTick48").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4770) {
                        doc.getElementById("rpmTick49").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4847) {
                        doc.getElementById("rpmTick50").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4924) {
                        doc.getElementById("rpmTick51").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5000) {
                        doc.getElementById("rpmTick52").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5083) {
                        doc.getElementById("rpmTick53").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5166) {
                        doc.getElementById("rpmTick54").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5249) {
                        doc.getElementById("rpmTick55").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5332) {
                        doc.getElementById("rpmTick56").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5415) {
                        doc.getElementById("rpmTick57").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5498) {
                        doc.getElementById("rpmTick58").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5581) {
                        doc.getElementById("rpmTick59").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5664) {
                        doc.getElementById("rpmTick60").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5747) {
                        doc.getElementById("rpmTick61").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5830) {
                        doc.getElementById("rpmTick62").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5913) {
                        doc.getElementById("rpmTick63").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6000) {
                        doc.getElementById("rpmTick64").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6077) {
                        doc.getElementById("rpmTick65").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6154) {
                        doc.getElementById("rpmTick66").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6231) {
                        doc.getElementById("rpmTick67").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6308) {
                        doc.getElementById("rpmTick68").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6385) {
                        doc.getElementById("rpmTick69").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6462) {
                        doc.getElementById("rpmTick70").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6539) {
                        doc.getElementById("rpmTick71").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6616) {
                        doc.getElementById("rpmTick72").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6693) {
                        doc.getElementById("rpmTick73").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6770) {
                        doc.getElementById("rpmTick74").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6847) {
                        doc.getElementById("rpmTick75").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6924) {
                        doc.getElementById("rpmTick76").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7000) {
                        doc.getElementById("rpmTick77").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7083) {
                        doc.getElementById("rpmTick78").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7166) {
                        doc.getElementById("rpmTick79").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7249) {
                        doc.getElementById("rpmTick80").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7332) {
                        doc.getElementById("rpmTick81").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7415) {
                        doc.getElementById("rpmTick82").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7498) {
                        doc.getElementById("rpmTick83").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7581) {
                        doc.getElementById("rpmTick84").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7664) {
                        doc.getElementById("rpmTick85").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7747) {
                        doc.getElementById("rpmTick86").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7830) {
                        doc.getElementById("rpmTick87").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7913) {
                        doc.getElementById("rpmTick88").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8000) {
                        doc.getElementById("rpmTick89").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8083) {
                        doc.getElementById("rpmTick90").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8166) {
                        doc.getElementById("rpmTick91").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8249) {
                        doc.getElementById("rpmTick92").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8332) {
                        doc.getElementById("rpmTick93").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8415) {
                        doc.getElementById("rpmTick94").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8498) {
                        doc.getElementById("rpmTick95").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8581) {
                        doc.getElementById("rpmTick96").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8664) {
                        doc.getElementById("rpmTick97").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8747) {
                        doc.getElementById("rpmTick98").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8830) {
                        doc.getElementById("rpmTick99").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8913) {
                        doc.getElementById("rpmTick100").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9000) {
                        doc.getElementById("rpmTick101").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9066) {
                        doc.getElementById("rpmTick102").setAttribute("style", "display:inline");
                    }
                    // Needle
                    if ((Data.getRPM() >= 0) && (Data.getRPM() <= 249)){
                        doc.getElementById("rpmNeedle0").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 250) && (Data.getRPM() <=499)){
                        doc.getElementById("rpmNeedle1").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 500) && (Data.getRPM() <= 749)){
                        doc.getElementById("rpmNeedle2").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 750) && (Data.getRPM() <= 999)){
                        doc.getElementById("rpmNeedle3").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1000) && (Data.getRPM() <= 1249)){
                        doc.getElementById("rpmNeedle4").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1250) && (Data.getRPM() <= 1499)){
                        doc.getElementById("rpmNeedle5").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1500) && (Data.getRPM() <= 1749)){
                        doc.getElementById("rpmNeedle6").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1750) && (Data.getRPM() <= 1999)){
                        doc.getElementById("rpmNeedle7").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2000) && (Data.getRPM() <= 2166)) {
                        doc.getElementById("rpmNeedle8").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2167) && (Data.getRPM() <= 2332)) {
                        doc.getElementById("rpmNeedle9").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2333) && (Data.getRPM() <= 2499)) {
                        doc.getElementById("rpmNeedle10").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2500) && (Data.getRPM() <= 2600)) {
                        doc.getElementById("rpmNeedle11").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2601) && (Data.getRPM() <= 2700)) {
                        doc.getElementById("rpmNeedle12").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2701) && (Data.getRPM() <= 2800)) {
                        doc.getElementById("rpmNeedle13").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2801) && (Data.getRPM() <= 2900)) {
                        doc.getElementById("rpmNeedle14").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3000) && (Data.getRPM() <= 3166)) {
                        doc.getElementById("rpmNeedle15").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3167) && (Data.getRPM() <= 3332)) {
                        doc.getElementById("rpmNeedle16").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3333) && (Data.getRPM() <= 3499)) {
                        doc.getElementById("rpmNeedle17").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3500) && (Data.getRPM() <= 3600)) {
                        doc.getElementById("rpmNeedle18").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3601) && (Data.getRPM() <= 3700)) {
                        doc.getElementById("rpmNeedle19").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3701) && (Data.getRPM() <= 3800)) {
                        doc.getElementById("rpmNeedle20").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3801) && (Data.getRPM() <= 3900)) {
                        doc.getElementById("rpmNeedle21").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3901) && (Data.getRPM() <= 4000)) {
                        doc.getElementById("rpmNeedle22").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4001) && (Data.getRPM() <= 4124)) {
                        doc.getElementById("rpmNeedle23").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4125) && (Data.getRPM() <= 4249)) {
                        doc.getElementById("rpmNeedle24").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4250) && (Data.getRPM() <= 4374)) {
                        doc.getElementById("rpmNeedle25").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4375) && (Data.getRPM() <= 4499)) {
                        doc.getElementById("rpmNeedle26").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4500) && (Data.getRPM() <= 4674)) {
                        doc.getElementById("rpmNeedle27").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4750) && (Data.getRPM() <= 4874)) {
                        doc.getElementById("rpmNeedle28").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4875) && (Data.getRPM() <= 4999)) {
                        doc.getElementById("rpmNeedle29").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5000) && (Data.getRPM() <= 5124)) {
                        doc.getElementById("rpmNeedle30").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5125) && (Data.getRPM() <= 5249)) {
                        doc.getElementById("rpmNeedle31").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5250) && (Data.getRPM() <= 5374)) {
                        doc.getElementById("rpmNeedle32").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5375) && (Data.getRPM() <= 5499)) {
                        doc.getElementById("rpmNeedle33").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5500) && (Data.getRPM() <= 5624)) {
                        doc.getElementById("rpmNeedle34").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5625) && (Data.getRPM() <= 5749)) {
                        doc.getElementById("rpmNeedle35").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5750) && (Data.getRPM() <= 5874)) {
                        doc.getElementById("rpmNeedle36").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5875) && (Data.getRPM() <= 6000)) {
                        doc.getElementById("rpmNeedle37").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6001) && (Data.getRPM() <= 6142)) {
                        doc.getElementById("rpmNeedle38").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6143) && (Data.getRPM() <= 6285)) {
                        doc.getElementById("rpmNeedle39").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6286) && (Data.getRPM() <= 6428)) {
                        doc.getElementById("rpmNeedle40").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6429) && (Data.getRPM() <= 6571)) {
                        doc.getElementById("rpmNeedle41").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6572) && (Data.getRPM() <= 6714)) {
                        doc.getElementById("rpmNeedle42").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6715) && (Data.getRPM() <= 6857)) {
                        doc.getElementById("rpmNeedle43").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6858) && (Data.getRPM() <= 6999)) {
                        doc.getElementById("rpmNeedle44").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7000) && (Data.getRPM() <= 7142)) {
                        doc.getElementById("rpmNeedle45").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7143) && (Data.getRPM() <= 7285)) {
                        doc.getElementById("rpmNeedle46").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7286) && (Data.getRPM() <= 7428)) {
                        doc.getElementById("rpmNeedle47").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7429) && (Data.getRPM() <= 7571)) {
                        doc.getElementById("rpmNeedle48").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7572) && (Data.getRPM() <= 7714)) {
                        doc.getElementById("rpmNeedle49").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7715) && (Data.getRPM() <= 7857)) {
                        doc.getElementById("rpmNeedle50").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7858) && (Data.getRPM() <= 7999)) {
                        doc.getElementById("rpmNeedle51").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8000) && (Data.getRPM() <= 8142)) {
                        doc.getElementById("rpmNeedle52").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8143) && (Data.getRPM() <= 8285)) {
                        doc.getElementById("rpmNeedle53").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8286) && (Data.getRPM() <= 8428)) {
                        doc.getElementById("rpmNeedle54").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8429) && (Data.getRPM() <= 8571)) {
                        doc.getElementById("rpmNeedle55").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8572) && (Data.getRPM() <= 8714)) {
                        doc.getElementById("rpmNeedle56").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8715) && (Data.getRPM() <= 8857)) {
                        doc.getElementById("rpmNeedle57").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8858) && (Data.getRPM() <= 8999)) {
                        doc.getElementById("rpmNeedle58").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9000) && (Data.getRPM() <= 9143)) {
                        doc.getElementById("rpmNeedle59").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9144) && (Data.getRPM() <= 9287)) {
                        doc.getElementById("rpmNeedle60").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9288) && (Data.getRPM() <= 9431)) {
                        doc.getElementById("rpmNeedle61").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9432) && (Data.getRPM() <= 9575)) {
                        doc.getElementById("rpmNeedle62").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9576) && (Data.getRPM() <= 9719)) {
                        doc.getElementById("rpmNeedle63").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9720) && (Data.getRPM() <= 9863)) {
                        doc.getElementById("rpmNeedle64").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9864) && (Data.getRPM() <= 9999)) {
                        doc.getElementById("rpmNeedle65").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10000) {
                        doc.getElementById("rpmNeedle66").setAttribute("style", "display:inline");
                    }
                } else if (twelveK) {
                    if (Data.getRPM() >= 0) {
                        doc.getElementById("rpmTick1").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 154) {
                        doc.getElementById("rpmTick2").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 308) {
                        doc.getElementById("rpmTick3").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 462) {
                        doc.getElementById("rpmTick4").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 616) {
                        doc.getElementById("rpmTick5").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 770) {
                        doc.getElementById("rpmTick6").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 924) {
                        doc.getElementById("rpmTick7").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1078) {
                        doc.getElementById("rpmTick8").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1232) {
                        doc.getElementById("rpmTick9").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1386) {
                        doc.getElementById("rpmTick10").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1540) {
                        doc.getElementById("rpmTick11").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1694) {
                        doc.getElementById("rpmTick12").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1848) {
                        doc.getElementById("rpmTick13").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2000) {
                        doc.getElementById("rpmTick14").setAttribute("style", "display:inline");
                    }

                    if (Data.getRPM() >= 2167) {
                        doc.getElementById("rpmTick15").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2167) {
                        doc.getElementById("rpmTick16").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2334) {
                        doc.getElementById("rpmTick17").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2501) {
                        doc.getElementById("rpmTick18").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2668) {
                        doc.getElementById("rpmTick19").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2835) {
                        doc.getElementById("rpmTick20").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3002) {
                        doc.getElementById("rpmTick21").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3169) {
                        doc.getElementById("rpmTick22").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3503) {
                        doc.getElementById("rpmTick23").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3670) {
                        doc.getElementById("rpmTick24").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3837) {
                        doc.getElementById("rpmTick25").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4000) {
                        doc.getElementById("rpmTick26").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4154) {
                        doc.getElementById("rpmTick27").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4308) {
                        doc.getElementById("rpmTick28").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4462) {
                        doc.getElementById("rpmTick29").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4616) {
                        doc.getElementById("rpmTick30").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4770) {
                        doc.getElementById("rpmTick31").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4924) {
                        doc.getElementById("rpmTick32").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5078) {
                        doc.getElementById("rpmTick33").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5232) {
                        doc.getElementById("rpmTick34").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5386) {
                        doc.getElementById("rpmTick35").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5540) {
                        doc.getElementById("rpmTick36").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5694) {
                        doc.getElementById("rpmTick37").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5848) {
                        doc.getElementById("rpmTick38").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6000) {
                        doc.getElementById("rpmTick39").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6077) {
                        doc.getElementById("rpmTick40").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6154) {
                        doc.getElementById("rpmTick41").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6231) {
                        doc.getElementById("rpmTick42").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6308) {
                        doc.getElementById("rpmTick43").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6385) {
                        doc.getElementById("rpmTick44").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6462) {
                        doc.getElementById("rpmTick45").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6539) {
                        doc.getElementById("rpmTick46").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6616) {
                        doc.getElementById("rpmTick47").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6693) {
                        doc.getElementById("rpmTick48").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6770) {
                        doc.getElementById("rpmTick49").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6847) {
                        doc.getElementById("rpmTick50").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6924) {
                        doc.getElementById("rpmTick51").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7000) {
                        doc.getElementById("rpmTick52").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7083) {
                        doc.getElementById("rpmTick53").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7166) {
                        doc.getElementById("rpmTick54").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7249) {
                        doc.getElementById("rpmTick55").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7332) {
                        doc.getElementById("rpmTick56").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7415) {
                        doc.getElementById("rpmTick57").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7498) {
                        doc.getElementById("rpmTick58").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7581) {
                        doc.getElementById("rpmTick59").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7664) {
                        doc.getElementById("rpmTick60").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7747) {
                        doc.getElementById("rpmTick61").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7830) {
                        doc.getElementById("rpmTick62").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7913) {
                        doc.getElementById("rpmTick63").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8000) {
                        doc.getElementById("rpmTick64").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8077) {
                        doc.getElementById("rpmTick65").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8154) {
                        doc.getElementById("rpmTick66").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8231) {
                        doc.getElementById("rpmTick67").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8308) {
                        doc.getElementById("rpmTick68").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8385) {
                        doc.getElementById("rpmTick69").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8462) {
                        doc.getElementById("rpmTick70").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8539) {
                        doc.getElementById("rpmTick71").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8616) {
                        doc.getElementById("rpmTick72").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8693) {
                        doc.getElementById("rpmTick73").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8770) {
                        doc.getElementById("rpmTick74").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8847) {
                        doc.getElementById("rpmTick75").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8924) {
                        doc.getElementById("rpmTick76").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9000) {
                        doc.getElementById("rpmTick77").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9083) {
                        doc.getElementById("rpmTick78").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9166) {
                        doc.getElementById("rpmTick79").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9249) {
                        doc.getElementById("rpmTick80").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9332) {
                        doc.getElementById("rpmTick81").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9415) {
                        doc.getElementById("rpmTick82").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9498) {
                        doc.getElementById("rpmTick83").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9581) {
                        doc.getElementById("rpmTick84").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9664) {
                        doc.getElementById("rpmTick85").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9747) {
                        doc.getElementById("rpmTick86").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9830) {
                        doc.getElementById("rpmTick87").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9913) {
                        doc.getElementById("rpmTick88").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10000) {
                        doc.getElementById("rpmTick89").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10083) {
                        doc.getElementById("rpmTick90").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10166) {
                        doc.getElementById("rpmTick91").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10249) {
                        doc.getElementById("rpmTick92").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10332) {
                        doc.getElementById("rpmTick93").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10415) {
                        doc.getElementById("rpmTick94").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10498) {
                        doc.getElementById("rpmTick95").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10581) {
                        doc.getElementById("rpmTick96").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10664) {
                        doc.getElementById("rpmTick97").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10747) {
                        doc.getElementById("rpmTick98").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10830) {
                        doc.getElementById("rpmTick99").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10913) {
                        doc.getElementById("rpmTick100").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11000) {
                        doc.getElementById("rpmTick101").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11066) {
                        doc.getElementById("rpmTick102").setAttribute("style", "display:inline");
                    }
                    // Needle
                    if ((Data.getRPM() >= 0) && (Data.getRPM() <= 249)){
                        doc.getElementById("rpmNeedle0").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 250) && (Data.getRPM() <=499)){
                        doc.getElementById("rpmNeedle1").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 500) && (Data.getRPM() <= 749)){
                        doc.getElementById("rpmNeedle2").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 750) && (Data.getRPM() <= 999)){
                        doc.getElementById("rpmNeedle3").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1000) && (Data.getRPM() <= 1249)){
                        doc.getElementById("rpmNeedle4").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1250) && (Data.getRPM() <= 1499)){
                        doc.getElementById("rpmNeedle5").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1500) && (Data.getRPM() <= 1749)){
                        doc.getElementById("rpmNeedle6").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1750) && (Data.getRPM() <= 1999)){
                        doc.getElementById("rpmNeedle7").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2000) && (Data.getRPM() <= 2286)) {
                        doc.getElementById("rpmNeedle8").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2287) && (Data.getRPM() <= 2573)) {
                        doc.getElementById("rpmNeedle9").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2574) && (Data.getRPM() <= 2860)) {
                        doc.getElementById("rpmNeedle10").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2861) && (Data.getRPM() <= 3147)) {
                        doc.getElementById("rpmNeedle11").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3148) && (Data.getRPM() <= 3434)) {
                        doc.getElementById("rpmNeedle12").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3435) && (Data.getRPM() <= 3721)) {
                        doc.getElementById("rpmNeedle13").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3722) && (Data.getRPM() <= 3999)) {
                        doc.getElementById("rpmNeedle14").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4000) && (Data.getRPM() <= 4249)) {
                        doc.getElementById("rpmNeedle15").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4250) && (Data.getRPM() <= 4499)) {
                        doc.getElementById("rpmNeedle16").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4500) && (Data.getRPM() <= 4749)) {
                        doc.getElementById("rpmNeedle17").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4750) && (Data.getRPM() <= 4999)) {
                        doc.getElementById("rpmNeedle18").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5000) && (Data.getRPM() <= 5249)) {
                        doc.getElementById("rpmNeedle19").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5250) && (Data.getRPM() <= 5499)) {
                        doc.getElementById("rpmNeedle20").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5500) && (Data.getRPM() <= 5749)) {
                        doc.getElementById("rpmNeedle21").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5750) && (Data.getRPM() <= 6000)) {
                        doc.getElementById("rpmNeedle22").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6001) && (Data.getRPM() <= 6124)) {
                        doc.getElementById("rpmNeedle23").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6125) && (Data.getRPM() <= 6249)) {
                        doc.getElementById("rpmNeedle24").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6250) && (Data.getRPM() <= 6374)) {
                        doc.getElementById("rpmNeedle25").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6375) && (Data.getRPM() <= 6499)) {
                        doc.getElementById("rpmNeedle26").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6500) && (Data.getRPM() <=6674)) {
                        doc.getElementById("rpmNeedle27").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6750) && (Data.getRPM() <= 6874)) {
                        doc.getElementById("rpmNeedle28").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6875) && (Data.getRPM() <= 6999)) {
                        doc.getElementById("rpmNeedle29").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7000) && (Data.getRPM() <= 7124)) {
                        doc.getElementById("rpmNeedle30").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7125) && (Data.getRPM() <= 7249)) {
                        doc.getElementById("rpmNeedle31").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7250) && (Data.getRPM() <= 7374)) {
                        doc.getElementById("rpmNeedle32").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7375) && (Data.getRPM() <= 7499)) {
                        doc.getElementById("rpmNeedle33").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7500) && (Data.getRPM() <= 7624)) {
                        doc.getElementById("rpmNeedle34").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7625) && (Data.getRPM() <= 7749)) {
                        doc.getElementById("rpmNeedle35").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7750) && (Data.getRPM() <= 7874)) {
                        doc.getElementById("rpmNeedle36").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7875) && (Data.getRPM() <= 8000)) {
                        doc.getElementById("rpmNeedle37").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8001) && (Data.getRPM() <= 6142)) {
                        doc.getElementById("rpmNeedle38").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6143) && (Data.getRPM() <= 6285)) {
                        doc.getElementById("rpmNeedle39").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6286) && (Data.getRPM() <= 6428)) {
                        doc.getElementById("rpmNeedle40").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6429) && (Data.getRPM() <= 6571)) {
                        doc.getElementById("rpmNeedle41").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6572) && (Data.getRPM() <= 6714)) {
                        doc.getElementById("rpmNeedle42").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6715) && (Data.getRPM() <= 6857)) {
                        doc.getElementById("rpmNeedle43").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6858) && (Data.getRPM() <= 6999)) {
                        doc.getElementById("rpmNeedle44").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9000) && (Data.getRPM() <= 9142)) {
                        doc.getElementById("rpmNeedle45").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9143) && (Data.getRPM() <= 9285)) {
                        doc.getElementById("rpmNeedle46").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9286) && (Data.getRPM() <= 9428)) {
                        doc.getElementById("rpmNeedle47").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9429) && (Data.getRPM() <= 9571)) {
                        doc.getElementById("rpmNeedle48").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9572) && (Data.getRPM() <= 9714)) {
                        doc.getElementById("rpmNeedle49").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9715) && (Data.getRPM() <= 9857)) {
                        doc.getElementById("rpmNeedle50").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9858) && (Data.getRPM() <= 9999)) {
                        doc.getElementById("rpmNeedle51").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10000) && (Data.getRPM() <= 10142)) {
                        doc.getElementById("rpmNeedle52").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10143) && (Data.getRPM() <= 10285)) {
                        doc.getElementById("rpmNeedle53").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10286) && (Data.getRPM() <= 10428)) {
                        doc.getElementById("rpmNeedle54").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10429) && (Data.getRPM() <= 10571)) {
                        doc.getElementById("rpmNeedle55").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10572) && (Data.getRPM() <= 10714)) {
                        doc.getElementById("rpmNeedle56").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10715) && (Data.getRPM() <= 10857)) {
                        doc.getElementById("rpmNeedle57").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10858) && (Data.getRPM() <= 10999)) {
                        doc.getElementById("rpmNeedle58").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11000) && (Data.getRPM() <= 11143)) {
                        doc.getElementById("rpmNeedle59").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11144) && (Data.getRPM() <= 11287)) {
                        doc.getElementById("rpmNeedle60").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11288) && (Data.getRPM() <= 11431)) {
                        doc.getElementById("rpmNeedle61").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11432) && (Data.getRPM() <= 11575)) {
                        doc.getElementById("rpmNeedle62").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11576) && (Data.getRPM() <= 11719)) {
                        doc.getElementById("rpmNeedle63").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11720) && (Data.getRPM() <= 11863)) {
                        doc.getElementById("rpmNeedle64").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11864) && (Data.getRPM() <= 11999)) {
                        doc.getElementById("rpmNeedle65").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12000) {
                        doc.getElementById("rpmNeedle66").setAttribute("style", "display:inline");
                    }
                } else {
                    if (Data.getRPM() >= 0) {
                        doc.getElementById("rpmTick1").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 154) {
                        doc.getElementById("rpmTick2").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 308) {
                        doc.getElementById("rpmTick3").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 462) {
                        doc.getElementById("rpmTick4").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 616) {
                        doc.getElementById("rpmTick5").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 770) {
                        doc.getElementById("rpmTick6").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 924) {
                        doc.getElementById("rpmTick7").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1078) {
                        doc.getElementById("rpmTick8").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1232) {
                        doc.getElementById("rpmTick9").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1386) {
                        doc.getElementById("rpmTick10").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1540) {
                        doc.getElementById("rpmTick11").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1694) {
                        doc.getElementById("rpmTick12").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1848) {
                        doc.getElementById("rpmTick13").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2000) {
                        doc.getElementById("rpmTick14").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2167) {
                        doc.getElementById("rpmTick15").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2334) {
                        doc.getElementById("rpmTick16").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2501) {
                        doc.getElementById("rpmTick17").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2668) {
                        doc.getElementById("rpmTick18").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2835) {
                        doc.getElementById("rpmTick19").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3002) {
                        doc.getElementById("rpmTick20").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3169) {
                        doc.getElementById("rpmTick21").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3336) {
                        doc.getElementById("rpmTick22").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3503) {
                        doc.getElementById("rpmTick23").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3670) {
                        doc.getElementById("rpmTick24").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3837) {
                        doc.getElementById("rpmTick25").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4000) {
                        doc.getElementById("rpmTick26").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4308) {
                        doc.getElementById("rpmTick27").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4616) {
                        doc.getElementById("rpmTick28").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4924) {
                        doc.getElementById("rpmTick29").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5232) {
                        doc.getElementById("rpmTick30").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5540) {
                        doc.getElementById("rpmTick31").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5848) {
                        doc.getElementById("rpmTick32").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6156) {
                        doc.getElementById("rpmTick33").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6464) {
                        doc.getElementById("rpmTick34").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6772) {
                        doc.getElementById("rpmTick35").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7080) {
                        doc.getElementById("rpmTick36").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7388) {
                        doc.getElementById("rpmTick37").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7696) {
                        doc.getElementById("rpmTick38").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8000) {
                        doc.getElementById("rpmTick39").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8077) {
                        doc.getElementById("rpmTick40").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8154) {
                        doc.getElementById("rpmTick41").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8231) {
                        doc.getElementById("rpmTick42").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8308) {
                        doc.getElementById("rpmTick43").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8385) {
                        doc.getElementById("rpmTick44").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8462) {
                        doc.getElementById("rpmTick45").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8539) {
                        doc.getElementById("rpmTick46").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8616) {
                        doc.getElementById("rpmTick47").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8693) {
                        doc.getElementById("rpmTick48").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8770) {
                        doc.getElementById("rpmTick49").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8847) {
                        doc.getElementById("rpmTick50").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8924) {
                        doc.getElementById("rpmTick51").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9000) {
                        doc.getElementById("rpmTick52").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9083) {
                        doc.getElementById("rpmTick53").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9166) {
                        doc.getElementById("rpmTick54").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9249) {
                        doc.getElementById("rpmTick55").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9332) {
                        doc.getElementById("rpmTick56").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9415) {
                        doc.getElementById("rpmTick57").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9498) {
                        doc.getElementById("rpmTick58").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9581) {
                        doc.getElementById("rpmTick59").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9664) {
                        doc.getElementById("rpmTick60").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9747) {
                        doc.getElementById("rpmTick61").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9830) {
                        doc.getElementById("rpmTick62").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9913) {
                        doc.getElementById("rpmTick63").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10000) {
                        doc.getElementById("rpmTick64").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10077) {
                        doc.getElementById("rpmTick65").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10154) {
                        doc.getElementById("rpmTick66").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10231) {
                        doc.getElementById("rpmTick67").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10308) {
                        doc.getElementById("rpmTick68").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10385) {
                        doc.getElementById("rpmTick69").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10462) {
                        doc.getElementById("rpmTick70").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10539) {
                        doc.getElementById("rpmTick71").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10616) {
                        doc.getElementById("rpmTick72").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10693) {
                        doc.getElementById("rpmTick73").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10770) {
                        doc.getElementById("rpmTick74").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10847) {
                        doc.getElementById("rpmTick75").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10924) {
                        doc.getElementById("rpmTick76").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11000) {
                        doc.getElementById("rpmTick77").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11083) {
                        doc.getElementById("rpmTick78").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11166) {
                        doc.getElementById("rpmTick79").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11249) {
                        doc.getElementById("rpmTick80").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11332) {
                        doc.getElementById("rpmTick81").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11415) {
                        doc.getElementById("rpmTick82").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11498) {
                        doc.getElementById("rpmTick83").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11581) {
                        doc.getElementById("rpmTick84").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11664) {
                        doc.getElementById("rpmTick85").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11747) {
                        doc.getElementById("rpmTick86").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11830) {
                        doc.getElementById("rpmTick87").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11913) {
                        doc.getElementById("rpmTick88").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12000) {
                        doc.getElementById("rpmTick89").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12083) {
                        doc.getElementById("rpmTick90").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12166) {
                        doc.getElementById("rpmTick91").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12249) {
                        doc.getElementById("rpmTick92").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12332) {
                        doc.getElementById("rpmTick93").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12415) {
                        doc.getElementById("rpmTick94").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12498) {
                        doc.getElementById("rpmTick95").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12581) {
                        doc.getElementById("rpmTick96").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12664) {
                        doc.getElementById("rpmTick97").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12747) {
                        doc.getElementById("rpmTick98").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12830) {
                        doc.getElementById("rpmTick99").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12913) {
                        doc.getElementById("rpmTick100").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 13000) {
                        doc.getElementById("rpmTick101").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 13133) {
                        doc.getElementById("rpmTick102").setAttribute("style", "display:inline");
                    }
                    //Needle
                    if ((Data.getRPM() >= 0) && (Data.getRPM() <= 249)){
                        doc.getElementById("rpmNeedle0").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 250) && (Data.getRPM() <=499)){
                        doc.getElementById("rpmNeedle1").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 500) && (Data.getRPM() <= 749)){
                        doc.getElementById("rpmNeedle2").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 750) && (Data.getRPM() <= 999)){
                        doc.getElementById("rpmNeedle3").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1000) && (Data.getRPM() <= 1249)){
                        doc.getElementById("rpmNeedle4").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1250) && (Data.getRPM() <= 1499)){
                        doc.getElementById("rpmNeedle5").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1500) && (Data.getRPM() <= 1749)){
                        doc.getElementById("rpmNeedle6").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 1750) && (Data.getRPM() <= 1999)){
                        doc.getElementById("rpmNeedle7").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2000) && (Data.getRPM() <= 2332)) {
                        doc.getElementById("rpmNeedle8").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2333) && (Data.getRPM() <= 2665)) {
                        doc.getElementById("rpmNeedle9").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 2666) && (Data.getRPM() <= 2998)) {
                        doc.getElementById("rpmNeedle10").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3000) && (Data.getRPM() <= 3249)) {
                        doc.getElementById("rpmNeedle11").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3250) && (Data.getRPM() <= 3499)) {
                        doc.getElementById("rpmNeedle12").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3500) && (Data.getRPM() <= 3749)) {
                        doc.getElementById("rpmNeedle13").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 3750) && (Data.getRPM() <= 3999)) {
                        doc.getElementById("rpmNeedle14").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4000) && (Data.getRPM() <= 4499)) {
                        doc.getElementById("rpmNeedle15").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 4500) && (Data.getRPM() <= 4999)) {
                        doc.getElementById("rpmNeedle16").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5000) && (Data.getRPM() <= 5499)) {
                        doc.getElementById("rpmNeedle17").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 5500) && (Data.getRPM() <= 5999)) {
                        doc.getElementById("rpmNeedle18").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6000) && (Data.getRPM() <= 6499)) {
                        doc.getElementById("rpmNeedle19").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 6500) && (Data.getRPM() <= 6999)) {
                        doc.getElementById("rpmNeedle20").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7000) && (Data.getRPM() <= 7499)) {
                        doc.getElementById("rpmNeedle21").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 7500) && (Data.getRPM() <= 8000)) {
                        doc.getElementById("rpmNeedle22").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8001) && (Data.getRPM() <= 8124)) {
                        doc.getElementById("rpmNeedle23").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8125) && (Data.getRPM() <= 8249)) {
                        doc.getElementById("rpmNeedle24").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8250) && (Data.getRPM() <= 8374)) {
                        doc.getElementById("rpmNeedle25").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8375) && (Data.getRPM() <= 8499)) {
                        doc.getElementById("rpmNeedle26").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8500) && (Data.getRPM() <= 8674)) {
                        doc.getElementById("rpmNeedle27").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8750) && (Data.getRPM() <= 8874)) {
                        doc.getElementById("rpmNeedle28").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 8875) && (Data.getRPM() <= 8999)) {
                        doc.getElementById("rpmNeedle29").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9000) && (Data.getRPM() <= 9124)) {
                        doc.getElementById("rpmNeedle30").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9125) && (Data.getRPM() <= 9249)) {
                        doc.getElementById("rpmNeedle31").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9250) && (Data.getRPM() <= 9374)) {
                        doc.getElementById("rpmNeedle32").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9375) && (Data.getRPM() <= 9499)) {
                        doc.getElementById("rpmNeedle33").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9500) && (Data.getRPM() <= 9624)) {
                        doc.getElementById("rpmNeedle34").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9625) && (Data.getRPM() <= 9749)) {
                        doc.getElementById("rpmNeedle35").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9750) && (Data.getRPM() <= 9874)) {
                        doc.getElementById("rpmNeedle36").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 9875) && (Data.getRPM() <= 10000)) {
                        doc.getElementById("rpmNeedle37").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10001) && (Data.getRPM() <= 10142)) {
                        doc.getElementById("rpmNeedle38").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10143) && (Data.getRPM() <= 10285)) {
                        doc.getElementById("rpmNeedle39").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10286) && (Data.getRPM() <= 10428)) {
                        doc.getElementById("rpmNeedle40").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10429) && (Data.getRPM() <= 10571)) {
                        doc.getElementById("rpmNeedle41").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10572) && (Data.getRPM() <= 10714)) {
                        doc.getElementById("rpmNeedle42").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10715) && (Data.getRPM() <= 10857)) {
                        doc.getElementById("rpmNeedle43").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 10858) && (Data.getRPM() <= 10999)) {
                        doc.getElementById("rpmNeedle44").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11000) && (Data.getRPM() <= 11142)) {
                        doc.getElementById("rpmNeedle45").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11143) && (Data.getRPM() <= 11285)) {
                        doc.getElementById("rpmNeedle46").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11286) && (Data.getRPM() <= 11428)) {
                        doc.getElementById("rpmNeedle47").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11429) && (Data.getRPM() <= 11571)) {
                        doc.getElementById("rpmNeedle48").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11572) && (Data.getRPM() <= 11714)) {
                        doc.getElementById("rpmNeedle49").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11715) && (Data.getRPM() <= 11857)) {
                        doc.getElementById("rpmNeedle50").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 11858) && (Data.getRPM() <= 11999)) {
                        doc.getElementById("rpmNeedle51").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 12000) && (Data.getRPM() <= 12142)) {
                        doc.getElementById("rpmNeedle52").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 12143) && (Data.getRPM() <= 12285)) {
                        doc.getElementById("rpmNeedle53").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 12286) && (Data.getRPM() <= 12428)) {
                        doc.getElementById("rpmNeedle54").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 12429) && (Data.getRPM() <= 12571)) {
                        doc.getElementById("rpmNeedle55").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 12572) && (Data.getRPM() <= 12714)) {
                        doc.getElementById("rpmNeedle56").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 12715) && (Data.getRPM() <= 12857)) {
                        doc.getElementById("rpmNeedle57").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 12858) && (Data.getRPM() <= 12999)) {
                        doc.getElementById("rpmNeedle58").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 13000) && (Data.getRPM() <= 13285)) {
                        doc.getElementById("rpmNeedle59").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 13286) && (Data.getRPM() <= 13571)) {
                        doc.getElementById("rpmNeedle60").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 13572) && (Data.getRPM() <= 13857)) {
                        doc.getElementById("rpmNeedle61").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 13858) && (Data.getRPM() <= 14143)) {
                        doc.getElementById("rpmNeedle62").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 14144) && (Data.getRPM() <= 14429)) {
                        doc.getElementById("rpmNeedle63").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 14430) && (Data.getRPM() <= 14715)) {
                        doc.getElementById("rpmNeedle64").setAttribute("style", "display:inline");
                    }
                    if ((Data.getRPM() >= 14716) && (Data.getRPM() <= 14999)) {
                        doc.getElementById("rpmNeedle65").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 15000) {
                        doc.getElementById("rpmNeedle66").setAttribute("style", "display:inline");
                    }
                }

            }

            // Lean Angle Dial
            if (Data.getLeanAngleBike() != null) {
                if((Data.getLeanAngleBike() <= 5.5) && (Data.getLeanAngleBike() >= -5.5)){
                    doc.getElementById("angle0").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() <= -5.5) && (Data.getLeanAngleBike() >= -11.0)){
                    doc.getElementById("angle1L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() <= -11.0) && (Data.getLeanAngleBike() >= -16.5)){
                    doc.getElementById("angle2L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() <= -16.5) && (Data.getLeanAngleBike() >= -22.0)){
                    doc.getElementById("angle3L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() <= -22.0) && (Data.getLeanAngleBike() >= -27.5)){
                    doc.getElementById("angle4L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() <= -27.5) && (Data.getLeanAngleBike() >= -33.0)){
                    doc.getElementById("angle5L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() <= -33.0) && (Data.getLeanAngleBike() >= -38.5)){
                    doc.getElementById("angle6L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() <= -38.5) && (Data.getLeanAngleBike() >= -44.0)){
                    doc.getElementById("angle7L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() <= -44.0) && (Data.getLeanAngleBike() >= -49.5)){
                    doc.getElementById("angle8L").setAttribute("style", "display:inline");
                }
                if(Data.getLeanAngleBike() <= -49.5){
                    doc.getElementById("angle9L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() >= 5.5) && (Data.getLeanAngleBike() <= 11.0)){
                    doc.getElementById("angle1R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() >= 11.0) && (Data.getLeanAngleBike() <= 16.5)){
                    doc.getElementById("angle2R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() >= 16.5) && (Data.getLeanAngleBike() <= 22.0)){
                    doc.getElementById("angle3R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() >= 22.0) && (Data.getLeanAngleBike() <= 27.5)){
                    doc.getElementById("angle4R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() >= 27.5) && (Data.getLeanAngleBike() <= 33.0)){
                    doc.getElementById("angle5R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() >= 33.0) && (Data.getLeanAngleBike() <= 38.5)){
                    doc.getElementById("angle6R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() >= 38.5) && (Data.getLeanAngleBike() <= 44.0)){
                    doc.getElementById("angle7R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngleBike() >= 44.0) && (Data.getLeanAngleBike() <= 9.5)){
                    doc.getElementById("angle8R").setAttribute("style", "display:inline");
                }
                if(Data.getLeanAngleBike() >= 49.5){
                    doc.getElementById("angle9R").setAttribute("style", "display:inline");
                }
            } else if (Data.getLeanAngle() != null) {
                if((Data.getLeanAngle() <= 5.5) && (Data.getLeanAngle() >= -5.5)){
                    doc.getElementById("angle0").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() <= -5.5) && (Data.getLeanAngle() >= -11.0)){
                    doc.getElementById("angle1L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() <= -11.0) && (Data.getLeanAngle() >= -16.5)){
                    doc.getElementById("angle2L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() <= -16.5) && (Data.getLeanAngle() >= -22.0)){
                    doc.getElementById("angle3L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() <= -22.0) && (Data.getLeanAngle() >= -27.5)){
                    doc.getElementById("angle4L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() <= -27.5) && (Data.getLeanAngle() >= -33.0)){
                    doc.getElementById("angle5L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() <= -33.0) && (Data.getLeanAngle() >= -38.5)){
                    doc.getElementById("angle6L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() <= -38.5) && (Data.getLeanAngle() >= -44.0)){
                    doc.getElementById("angle7L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() <= -44.0) && (Data.getLeanAngle() >= -49.5)){
                    doc.getElementById("angle8L").setAttribute("style", "display:inline");
                }
                if(Data.getLeanAngle() <= -49.5){
                    doc.getElementById("angle9L").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() >= 5.5) && (Data.getLeanAngle() <= 11.0)){
                    doc.getElementById("angle1R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() >= 11.0) && (Data.getLeanAngle() <= 16.5)){
                    doc.getElementById("angle2R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() >= 16.5) && (Data.getLeanAngle() <= 22.0)){
                    doc.getElementById("angle3R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() >= 22.0) && (Data.getLeanAngle() <= 27.5)){
                    doc.getElementById("angle4R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() >= 27.5) && (Data.getLeanAngle() <= 33.0)){
                    doc.getElementById("angle5R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() >= 33.0) && (Data.getLeanAngle() <= 38.5)){
                    doc.getElementById("angle6R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() >= 38.5) && (Data.getLeanAngle() <= 44.0)){
                    doc.getElementById("angle7R").setAttribute("style", "display:inline");
                }
                if((Data.getLeanAngle() >= 44.0) && (Data.getLeanAngle() <= 49.5)){
                    doc.getElementById("angle8R").setAttribute("style", "display:inline");
                }
                if(Data.getLeanAngle() >= 49.5){
                    doc.getElementById("angle9R").setAttribute("style", "display:inline");
                }
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

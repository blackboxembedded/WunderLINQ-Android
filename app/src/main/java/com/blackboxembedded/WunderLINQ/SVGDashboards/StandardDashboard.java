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

public class StandardDashboard {

    public final static String TAG = "StdDashboard";

    private final static String SVGfilename = "standard-dashboard.svg";

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
    private static boolean fifteenK = false;

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
            if (sharedPrefs.getString("prefRPMMax", "0").equals("1")){
                twelveK = true;
            } else if (sharedPrefs.getString("prefRPMMax", "0").equals("2")){
                fifteenK = true;
            }

            //Labels
            //Ambient Temp Label
            doc.getElementById("ambientTempLabel").setTextContent(MyApplication.getContext().getResources().getString(R.string.dash_ambient_label) + ": ");
            //Engine Temp Label
            doc.getElementById("engineTempLabel").setTextContent(MyApplication.getContext().getResources().getString(R.string.dash_engine_label) + ": ");
            //Engine Temp Label
            doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getResources().getString(R.string.dash_range_label) + ": ");
            //Engine Temp Label
            doc.getElementById("rdcFLabel").setTextContent(MyApplication.getContext().getResources().getString(R.string.dash_rdcf_label) + ": ");
            //Engine Temp Label
            doc.getElementById("rdcRLabel").setTextContent(MyApplication.getContext().getResources().getString(R.string.dash_rdcr_label) + ": ");
            //Speed Label
            doc.getElementById("speedLabel").setTextContent(distanceTimeUnit);
            //RPM Digits For Sport Bikes
            if (twelveK) {
                doc.getElementById("rpmDialDigit1").setTextContent("2");
                doc.getElementById("rpmDialDigit2").setTextContent("4");
                doc.getElementById("rpmDialDigit3").setTextContent("5");
                doc.getElementById("rpmDialDigit4").setTextContent("6");
                doc.getElementById("rpmDialDigit5").setTextContent("7");
                doc.getElementById("rpmDialDigit6").setTextContent("8");
                doc.getElementById("rpmDialDigit7").setTextContent("9");
                doc.getElementById("rpmDialDigit8").setTextContent("10");
                doc.getElementById("rpmDialDigit9").setTextContent("11");
                doc.getElementById("rpmDialDigit10").setTextContent("12");
            } else if (fifteenK){
                doc.getElementById("rpmDialDigit1").setTextContent("2");
                doc.getElementById("rpmDialDigit2").setTextContent("4");
                doc.getElementById("rpmDialDigit3").setTextContent("6");
                doc.getElementById("rpmDialDigit4").setTextContent("8");
                doc.getElementById("rpmDialDigit5").setTextContent("9");
                doc.getElementById("rpmDialDigit6").setTextContent("10");
                doc.getElementById("rpmDialDigit7").setTextContent("11");
                doc.getElementById("rpmDialDigit8").setTextContent("12");
                doc.getElementById("rpmDialDigit9").setTextContent("13");
                doc.getElementById("rpmDialDigit10").setTextContent("15");
            }

            //Values
            //Data Label
            switch (infoLine){
                case 1://Trip1
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_trip1_label) + ": ");
                    break;
                case 2://Trip2
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_trip2_label) + ": ");
                    break;
                case 3://Range
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_range_label) + ": ");
                    break;
                case 4://Altitude
                    doc.getElementById("dataLabel").setTextContent(MyApplication.getContext().getString(R.string.dash_altitude_label) + ": ");
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
                            doc.getElementById("dataValue").setTextContent(Utils.oneDigit.format(trip1) + " " + distanceUnit);
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
                            doc.getElementById("dataValue").setTextContent(Utils.oneDigit.format(trip2) + " " + distanceUnit);
                        }
                    }
                    break;
                case 3://Range
                    if(Data.getFuelRange() != null){
                        double fuelrange = Data.getFuelRange();
                        if (distanceFormat.contains("1")) {
                            fuelrange = Utils.kmToMiles(fuelrange);
                        }
                        doc.getElementById("dataValue").setTextContent(String.valueOf(Math.round(fuelrange)) + " " + distanceUnit);
                        if (FaultStatus.getfuelFaultActive()) {
                            doc.getElementById("dataValue").setAttribute("style",
                                    doc.getElementById("dataValue").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#e20505;")
                            );
                        }
                    }
                    break;
                case 4://Altitude
                    if (Data.getLastLocation() != null){
                        double altitude = Data.getLastLocation().getAltitude();
                        if (distanceFormat.contains("1")) {
                            altitude = Utils.mToFeet(altitude);
                        }
                        doc.getElementById("dataValue").setTextContent(String.valueOf(Math.round(altitude) + " " + heightUnit));
                    }
                    break;
                default:
                    break;
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
                    doc.getElementById("gear").setAttribute("style",
                            doc.getElementById("gear").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#03ae1e;")
                    );

                }
            }
            //Ambient Temp
            if (Data.getAmbientTemperature() != null) {
                double ambientTemp = Data.getAmbientTemperature();
                if (temperatureFormat.contains("1")) {
                    // F
                    temperatureUnit = "F";
                    ambientTemp = Utils.celsiusToFahrenheit(ambientTemp);
                }
                doc.getElementById("ambientTemp").setTextContent(Math.round(ambientTemp) + temperatureUnit);
            }
            //Engine Temp
            if (Data.getEngineTemperature() != null) {
                double engineTemp = Data.getEngineTemperature();
                if (temperatureFormat.contains("1")) {
                    // F
                    temperatureUnit = "F";
                    engineTemp = Utils.celsiusToFahrenheit(engineTemp);
                }
                doc.getElementById("engineTemp").setTextContent(Math.round(engineTemp) + temperatureUnit);
                if (Data.getEngineTemperature() >= 104.0) {
                    doc.getElementById("engineTemp").setAttribute("style",
                            doc.getElementById("engineTemp").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#e20505;")
                    );
                }
            }
            //Clock
            if (Data.getTime() != null) {
                SimpleDateFormat dateformat = new SimpleDateFormat("h:mm", Locale.getDefault());
                if (!sharedPrefs.getString("prefTime", "0").equals("0")) {
                    dateformat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                }
                doc.getElementById("clock").setTextContent(dateformat.format(Data.getTime()));
            }
            //RDC Front
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
                doc.getElementById("rdcF").setTextContent(Utils.oneDigit.format(rdcFront) + pressureUnit);
                if (FaultStatus.getfrontTirePressureCriticalActive()) {
                    doc.getElementById("rdcF").setAttribute("style",
                            doc.getElementById("rdcF").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#e20505;")
                    );
                } else if (FaultStatus.getfrontTirePressureWarningActive()) {
                    doc.getElementById("rdcF").setAttribute("style",
                            doc.getElementById("rdcF").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#fcc914;")
                    );
                }
            }
            //RDC Rear
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
                doc.getElementById("rdcR").setTextContent(Utils.oneDigit.format(rdcRear) + pressureUnit);
                if (FaultStatus.getrearTirePressureCriticalActive()) {
                    doc.getElementById("rdcR").setAttribute("style",
                            doc.getElementById("rdcR").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#e20505;")
                    );

                } else if (FaultStatus.getrearTirePressureWarningActive()) {
                    doc.getElementById("rdcR").setAttribute("style",
                            doc.getElementById("rdcR").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#fcc914;")
                    );
                }
            }

            //Icons
            //Trip Icon
            if (MyApplication.getTripRecording()) {
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

            //RPM Gauge
            if (Data.getRPM() != null) {
                if(twelveK) {
                    if (Data.getRPM() >= 666) {
                        doc.getElementById("rpm333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1333) {
                        doc.getElementById("rpm666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2000) {
                        doc.getElementById("rpm1000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2666) {
                        doc.getElementById("rpm1333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3333) {
                        doc.getElementById("rpm1666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4000) {
                        doc.getElementById("rpm2000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4333) {
                        doc.getElementById("rpm2333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4666) {
                        doc.getElementById("rpm2666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5000) {
                        doc.getElementById("rpm3000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5333) {
                        doc.getElementById("rpm3333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5666) {
                        doc.getElementById("rpm3666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6000) {
                        doc.getElementById("rpm4000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6333) {
                        doc.getElementById("rpm4333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6666) {
                        doc.getElementById("rpm4666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7000) {
                        doc.getElementById("rpm5000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7333) {
                        doc.getElementById("rpm5333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7666) {
                        doc.getElementById("rpm5666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8000) {
                        doc.getElementById("rpm6000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8333) {
                        doc.getElementById("rpm6333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8666) {
                        doc.getElementById("rpm6666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9000) {
                        doc.getElementById("rpm7000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9333) {
                        doc.getElementById("rpm7333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9666) {
                        doc.getElementById("rpm7666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10000) {
                        doc.getElementById("rpm8000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10333) {
                        doc.getElementById("rpm8333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10666) {
                        doc.getElementById("rpm8666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11000) {
                        doc.getElementById("rpm9000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11333) {
                        doc.getElementById("rpm9333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11666) {
                        doc.getElementById("rpm9666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12000) {
                        doc.getElementById("rpm10000").setAttribute("style", "display:inline");
                    }
                }else if (fifteenK) {
                    if (Data.getRPM() >= 666) {
                        doc.getElementById("rpm333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1333) {
                        doc.getElementById("rpm666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2000) {
                        doc.getElementById("rpm1000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2666) {
                        doc.getElementById("rpm1333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3333) {
                        doc.getElementById("rpm1666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4000) {
                        doc.getElementById("rpm2000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4666) {
                        doc.getElementById("rpm2333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5333) {
                        doc.getElementById("rpm2666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6000) {
                        doc.getElementById("rpm3000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6666) {
                        doc.getElementById("rpm3333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7333) {
                        doc.getElementById("rpm3666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8000) {
                        doc.getElementById("rpm4000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8333) {
                        doc.getElementById("rpm4333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8666) {
                        doc.getElementById("rpm4666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9000) {
                        doc.getElementById("rpm5000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9333) {
                        doc.getElementById("rpm5333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9666) {
                        doc.getElementById("rpm5666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10000) {
                        doc.getElementById("rpm6000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10333) {
                        doc.getElementById("rpm6333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10666) {
                        doc.getElementById("rpm6666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11000) {
                        doc.getElementById("rpm7000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11333) {
                        doc.getElementById("rpm7333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 11666) {
                        doc.getElementById("rpm7666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12000) {
                        doc.getElementById("rpm8000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12333) {
                        doc.getElementById("rpm8333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 12666) {
                        doc.getElementById("rpm8666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 13000) {
                        doc.getElementById("rpm9000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 13666) {
                        doc.getElementById("rpm9333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 14333) {
                        doc.getElementById("rpm9666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 15000) {
                        doc.getElementById("rpm10000").setAttribute("style", "display:inline");
                    }
                } else {
                    if (Data.getRPM() >= 333) {
                        doc.getElementById("rpm333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 666) {
                        doc.getElementById("rpm666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1000) {
                        doc.getElementById("rpm1000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1333) {
                        doc.getElementById("rpm1333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 1666) {
                        doc.getElementById("rpm1666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2000) {
                        doc.getElementById("rpm2000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2333) {
                        doc.getElementById("rpm2333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 2666) {
                        doc.getElementById("rpm2666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3000) {
                        doc.getElementById("rpm3000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3333) {
                        doc.getElementById("rpm3333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 3666) {
                        doc.getElementById("rpm3666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4000) {
                        doc.getElementById("rpm4000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4333) {
                        doc.getElementById("rpm4333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 4666) {
                        doc.getElementById("rpm4666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5000) {
                        doc.getElementById("rpm5000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5333) {
                        doc.getElementById("rpm5333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 5666) {
                        doc.getElementById("rpm5666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6000) {
                        doc.getElementById("rpm6000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6333) {
                        doc.getElementById("rpm6333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 6666) {
                        doc.getElementById("rpm6666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7000) {
                        doc.getElementById("rpm7000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7333) {
                        doc.getElementById("rpm7333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 7666) {
                        doc.getElementById("rpm7666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8000) {
                        doc.getElementById("rpm8000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8333) {
                        doc.getElementById("rpm8333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 8666) {
                        doc.getElementById("rpm8666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9000) {
                        doc.getElementById("rpm9000").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9333) {
                        doc.getElementById("rpm9333").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 9666) {
                        doc.getElementById("rpm9666").setAttribute("style", "display:inline");
                    }
                    if (Data.getRPM() >= 10000) {
                        doc.getElementById("rpm10000").setAttribute("style", "display:inline");
                    }
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

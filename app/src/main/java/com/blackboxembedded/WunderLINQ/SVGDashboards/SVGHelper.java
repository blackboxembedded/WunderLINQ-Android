package com.blackboxembedded.WunderLINQ.SVGDashboards;

import static android.content.Context.WINDOW_SERVICE;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;


import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Faults;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.Utils;
//import com.caverock.androidsvg.SVG;

import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;

import org.w3c.dom.Document;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class SVGHelper {
    private final static String TAG = "SVGHelper";
    public SVGSettings s = getSvgSettings();


    public SVGSettings getSvgSettings() {
        s = new SVGSettings();

        try {
            // Read Settings
            s.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
            s.pressureFormat = s.sharedPrefs.getString("prefPressureF", "0");
            if (s.pressureFormat.contains("1")) {
                // KPa
                s.pressureUnit = "KPa";
            } else if (s.pressureFormat.contains("2")) {
                // Kg-f
                s.pressureUnit = "Kgf";
            } else if (s.pressureFormat.contains("3")) {
                // Psi
                s.pressureUnit = "psi";
            }
            s.temperatureFormat = s.sharedPrefs.getString("prefTempF", "0");
            if (s.temperatureFormat.contains("1")) {
                // F
                s.temperatureUnit = "F";
            }
            s.distanceFormat = s.sharedPrefs.getString("prefDistance", "0");
            if (s.distanceFormat.contains("1")) {
                s.distanceUnit = "mls";
                s.heightUnit = "ft";
                s.distanceTimeUnit = "MPH";
            }
            s.consumptionFormat = s.sharedPrefs.getString("prefConsumption", "0");
            if (s.consumptionFormat.contains("1")) {
                s.consumptionUnit = "mpg";
            } else if (s.consumptionFormat.contains("2")) {
                s.consumptionUnit = "mpg";
            } else if (s.consumptionFormat.contains("3")) {
                s.consumptionUnit = "kmL";
            }
            String maxRPM = s.sharedPrefs.getString("prefRPMMax", "0");
            switch (maxRPM) {
                case "0":
                    s.tenK = true;
                    break;
                case "1":
                    s.twelveK = true;
                    break;
                case "2":
                    s.fifteenK = true;
                    break;
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception getting SVG Settings: " + e.toString());
        }
        return s;
    }

    public static String svgFilename(String dashName) {
        if (dashName == null || dashName.isEmpty()) {
            dashName = "standard-dashboard";
        }

        if (isDevicePortrait()) {
            dashName  += "-portrait.svg";
        } else {
            dashName += ".svg";
        }

        return dashName;
    }

    public static boolean isDevicePortrait() {
        boolean portrait = false;

        try {
            WindowManager wm = (WindowManager) MyApplication.getContext().getSystemService(WINDOW_SERVICE);

            if (wm != null) {
                int rotation = wm.getDefaultDisplay().getRotation();

                // Determine the screen orientation based on the rotation value
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                    portrait = true;
                }
            }
        } catch (Exception e) {
            Log.e("IsPortrait Error", e.toString());
        }

        return portrait;
    }

    public String getDataLabel(int infoLine) {
        String dataLbl = "-";

        try {
            switch (infoLine) {
                case 1://Range
                    dataLbl = MyApplication.getContext().getString(R.string.dash_range_label);
                    break;
                case 2://Trip1
                    dataLbl = MyApplication.getContext().getString(R.string.dash_trip1_label);
                    break;
                case 3://Trip2
                    dataLbl = MyApplication.getContext().getString(R.string.dash_trip2_label);
                    break;
                case 4://Altitude
                    dataLbl = MyApplication.getContext().getString(R.string.dash_altitude_label);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception getting data labels: " + e.toString());
        }

        return dataLbl + ": ";
    }

    public String getDataValue(int infoLine) {
        String dataVal = "-";

        try {
            switch (infoLine) {
                case 1://Range
                    if (MotorcycleData.getFuelRange() != null) {
                        double fuelRange = MotorcycleData.getFuelRange();
                        if (s.distanceFormat.contains("1")) {
                            fuelRange = Utils.kmToMiles(fuelRange);
                        }
                        dataVal = (Utils.toZeroDecimalString(fuelRange)) + " " + s.distanceUnit;
                    }
                    break;
                case 2://Trip1
                    if (MotorcycleData.getTripOne() != null) {
                        if (MotorcycleData.getTripOne() != null) {
                            double trip1 = MotorcycleData.getTripOne();
                            if (s.distanceFormat.contains("1")) {
                                trip1 = Utils.kmToMiles(trip1);
                            }
                            dataVal = Utils.toOneDecimalString(trip1) + " " + s.distanceUnit;
                        }
                    }
                    break;
                case 3://Trip2
                    if (MotorcycleData.getTripTwo() != null) {
                        if (MotorcycleData.getTripTwo() != null) {
                            double trip2 = MotorcycleData.getTripTwo();
                            if (s.distanceFormat.contains("1")) {
                                trip2 = Utils.kmToMiles(trip2);
                            }
                            dataVal = Utils.toOneDecimalString(trip2) + " " + s.distanceUnit;
                        }
                    }
                    break;
                case 4://Altitude
                    if (MotorcycleData.getLastLocation() != null) {
                        double altitude = MotorcycleData.getLastLocation().getAltitude();
                        if (s.distanceFormat.contains("1")) {
                            altitude = Utils.mToFeet(altitude);
                        }
                        dataVal = ((Utils.toZeroDecimalString(altitude) + " " + s.heightUnit));
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception getting data values: " + e.toString());
        }

        return dataVal;
    }

    public String getSpeedValue() {
        String speedValue = "00";
        String speedSource = s.sharedPrefs.getString("prefDashSpeedSource", "0");
        Double speed = null;

        try {
            if (speedSource.contains("0")) {
                if (MotorcycleData.getSpeed() != null) {
                    speed = MotorcycleData.getSpeed();
                }
            } else if (speedSource.contains("1")) {
                if (MotorcycleData.getRearSpeed() != null) {
                    speed = MotorcycleData.getRearSpeed();
                }
            } else if (speedSource.contains("2")) {
                if (MotorcycleData.getLastLocation() != null) {
                    speed = (MotorcycleData.getLastLocation().getSpeed() * 3.6);
                }
            }
            if (speed != null) {
                if (s.distanceFormat.contains("1")) {
                    speed = Utils.kmToMiles(speed);
                }
                speedValue = (Utils.toZeroDecimalString(speed));
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception getting speed value: " + e.toString());
        }


        return speedValue;
    }

    public String getAmbientTemp() {
        String val = "-";

        try {
            //Ambient Temp
            if (MotorcycleData.getAmbientTemperature() != null) {
                double ambientTemp = MotorcycleData.getAmbientTemperature();
                if (s.temperatureFormat.contains("1")) {
                    // F
                    s.temperatureUnit = "F";
                    ambientTemp = Utils.celsiusToFahrenheit(ambientTemp);
                }
                val = (Utils.toZeroDecimalString(ambientTemp) + s.temperatureUnit);
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception getting ambient temp: " + e.toString());
        }

        return val;
    }

    public void setupIcons(Document doc) {
        try {
            org.w3c.dom.Element e;
            //Icons
            //Trip Icon
            e = doc.getElementById("iconTrip");
            if (e != null) {
                if (MyApplication.getTripRecording()) {
                    e.setAttribute("style", "display:inline");
                } else {
                    e.setAttribute("style", "display:none");
                }
            }

            //Camera Icon
            e = doc.getElementById("iconVideo");
            if (e != null) {
                if (MyApplication.getVideoRecording()) {
                    e.setAttribute("style", "display:inline");
                } else {
                    e.setAttribute("style", "display:none");
                }
            }

            //Fault Icon
            e = doc.getElementById("iconFault");
            if (e != null) {
                ArrayList<String> faultListData = Faults.getAllActiveDesc();
                if (!faultListData.isEmpty()) {
                    e.setAttribute("style", "display:inline");
                } else {
                    e.setAttribute("style", "display:none");
                }
            }

            //Fuel Icon
            e = doc.getElementById("iconFuel");
            if (e != null) {
                if (Faults.getFuelFaultActive()) {
                    e.setAttribute("style", "display:inline");
                } else {
                    e.setAttribute("style", "display:none");
                }
            }

            //Bluetooth Icon
            e = doc.getElementById("iconBT");
            if (e != null) {
                if (BluetoothLeService.isConnected()) {
                    e.setAttribute("style", "display:inline");
                } else {
                    e.setAttribute("style", "display:none");
                }
            }

            e = null;
        } catch (Exception e) {
            Log.d(TAG, "Exception setting up icons: " + e.toString());
        }

        return;
    }

    public void setupRpmDialStandard(Document doc) {
        //RPM Digits For Sport Bikes
        try {
            if (s.twelveK) {
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
            } else if (s.fifteenK) {
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
        } catch (Exception e) {
            Log.d(TAG, "Exception setting up tach dial: " + e.toString());
        }

        return;
    }

    public void setupTachStandard(Document doc) {
        try {
            //RPM Gauge
            if (MotorcycleData.getRPM() != null) {
                if (s.twelveK) {
                    if (MotorcycleData.getRPM() >= 666) {
                        doc.getElementById("rpm333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1333) {
                        doc.getElementById("rpm666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2000) {
                        doc.getElementById("rpm1000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2666) {
                        doc.getElementById("rpm1333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3333) {
                        doc.getElementById("rpm1666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4000) {
                        doc.getElementById("rpm2000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4333) {
                        doc.getElementById("rpm2333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4666) {
                        doc.getElementById("rpm2666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5000) {
                        doc.getElementById("rpm3000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5333) {
                        doc.getElementById("rpm3333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5666) {
                        doc.getElementById("rpm3666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6000) {
                        doc.getElementById("rpm4000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6333) {
                        doc.getElementById("rpm4333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6666) {
                        doc.getElementById("rpm4666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7000) {
                        doc.getElementById("rpm5000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7333) {
                        doc.getElementById("rpm5333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7666) {
                        doc.getElementById("rpm5666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8000) {
                        doc.getElementById("rpm6000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8333) {
                        doc.getElementById("rpm6333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8666) {
                        doc.getElementById("rpm6666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9000) {
                        doc.getElementById("rpm7000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9333) {
                        doc.getElementById("rpm7333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9666) {
                        doc.getElementById("rpm7666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10000) {
                        doc.getElementById("rpm8000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10333) {
                        doc.getElementById("rpm8333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10666) {
                        doc.getElementById("rpm8666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11000) {
                        doc.getElementById("rpm9000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11333) {
                        doc.getElementById("rpm9333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11666) {
                        doc.getElementById("rpm9666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12000) {
                        doc.getElementById("rpm10000").setAttribute("style", "display:inline");
                    }
                } else if (s.fifteenK) {
                    if (MotorcycleData.getRPM() >= 666) {
                        doc.getElementById("rpm333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1333) {
                        doc.getElementById("rpm666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2000) {
                        doc.getElementById("rpm1000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2666) {
                        doc.getElementById("rpm1333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3333) {
                        doc.getElementById("rpm1666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4000) {
                        doc.getElementById("rpm2000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4666) {
                        doc.getElementById("rpm2333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5333) {
                        doc.getElementById("rpm2666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6000) {
                        doc.getElementById("rpm3000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6666) {
                        doc.getElementById("rpm3333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7333) {
                        doc.getElementById("rpm3666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8000) {
                        doc.getElementById("rpm4000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8333) {
                        doc.getElementById("rpm4333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8666) {
                        doc.getElementById("rpm4666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9000) {
                        doc.getElementById("rpm5000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9333) {
                        doc.getElementById("rpm5333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9666) {
                        doc.getElementById("rpm5666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10000) {
                        doc.getElementById("rpm6000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10333) {
                        doc.getElementById("rpm6333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10666) {
                        doc.getElementById("rpm6666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11000) {
                        doc.getElementById("rpm7000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11333) {
                        doc.getElementById("rpm7333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11666) {
                        doc.getElementById("rpm7666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12000) {
                        doc.getElementById("rpm8000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12333) {
                        doc.getElementById("rpm8333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12666) {
                        doc.getElementById("rpm8666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 13000) {
                        doc.getElementById("rpm9000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 13666) {
                        doc.getElementById("rpm9333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 14333) {
                        doc.getElementById("rpm9666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 15000) {
                        doc.getElementById("rpm10000").setAttribute("style", "display:inline");
                    }
                } else {
                    if (MotorcycleData.getRPM() >= 333) {
                        doc.getElementById("rpm333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 666) {
                        doc.getElementById("rpm666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1000) {
                        doc.getElementById("rpm1000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1333) {
                        doc.getElementById("rpm1333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1666) {
                        doc.getElementById("rpm1666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2000) {
                        doc.getElementById("rpm2000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2333) {
                        doc.getElementById("rpm2333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2666) {
                        doc.getElementById("rpm2666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3000) {
                        doc.getElementById("rpm3000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3333) {
                        doc.getElementById("rpm3333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3666) {
                        doc.getElementById("rpm3666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4000) {
                        doc.getElementById("rpm4000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4333) {
                        doc.getElementById("rpm4333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4666) {
                        doc.getElementById("rpm4666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5000) {
                        doc.getElementById("rpm5000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5333) {
                        doc.getElementById("rpm5333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5666) {
                        doc.getElementById("rpm5666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6000) {
                        doc.getElementById("rpm6000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6333) {
                        doc.getElementById("rpm6333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6666) {
                        doc.getElementById("rpm6666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7000) {
                        doc.getElementById("rpm7000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7333) {
                        doc.getElementById("rpm7333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7666) {
                        doc.getElementById("rpm7666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8000) {
                        doc.getElementById("rpm8000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8333) {
                        doc.getElementById("rpm8333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8666) {
                        doc.getElementById("rpm8666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9000) {
                        doc.getElementById("rpm9000").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9333) {
                        doc.getElementById("rpm9333").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9666) {
                        doc.getElementById("rpm9666").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10000) {
                        doc.getElementById("rpm10000").setAttribute("style", "display:inline");
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception setting up standard tach: " + e.toString());
        }
    }

    public void setupGear(Document doc) {
        try {
            var e = doc.getElementById("gear");
            if (e != null) {
                //Gear
                String gear = MotorcycleData.getGear();

                if (gear != null) {
                    if (gear.equals("N")) {
                        e.setAttribute("style",
                                e.getAttribute("style").replaceAll("fill:([^<]*);", "fill:#03ae1e;")
                        );
                    }
                } else {
                    gear = "-";
                }
                e.setTextContent(gear);
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception getting Gear: " + e.toString());
        }
    }

    public void setupRearRDC(Document doc) {
        try {
            //RDC Rear
            if (MotorcycleData.getRearTirePressure() != null) {
                double rdcRear = MotorcycleData.getRearTirePressure();
                if (s.pressureFormat.contains("1")) {
                    // KPa
                    s.pressureUnit = "KPa";
                    rdcRear = Utils.barTokPa(rdcRear);
                } else if (s.pressureFormat.contains("2")) {
                    // Kg-f
                    s.pressureUnit = "Kgf";
                    rdcRear = Utils.barToKgF(rdcRear);
                } else if (s.pressureFormat.contains("3")) {
                    // Psi
                    s.pressureUnit = "psi";
                    rdcRear = Utils.barToPsi(rdcRear);
                }
                doc.getElementById("rdcR").setTextContent(Utils.toOneDecimalString(rdcRear) + s.pressureUnit);
                if (Faults.getRearTirePressureCriticalActive()) {
                    doc.getElementById("rdcR").setAttribute("style",
                            doc.getElementById("rdcR").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#e20505;")
                    );

                } else if (Faults.getRearTirePressureWarningActive()) {
                    doc.getElementById("rdcR").setAttribute("style",
                            doc.getElementById("rdcR").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#fcc914;")
                    );
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception getting rear pressure: " + e.toString());
        }
        return;
    }

    public void setupFrontRDC(Document doc) {
        try {
            //RDC Front
            if (MotorcycleData.getFrontTirePressure() != null) {
                double rdcFront = MotorcycleData.getFrontTirePressure();
                if (s.pressureFormat.contains("1")) {
                    // KPa
                    s.pressureUnit = "KPa";
                    rdcFront = Utils.barTokPa(rdcFront);
                } else if (s.pressureFormat.contains("2")) {
                    // Kg-f
                    s.pressureUnit = "Kgf";
                    rdcFront = Utils.barToKgF(rdcFront);
                } else if (s.pressureFormat.contains("3")) {
                    // Psi
                    s.pressureUnit = "psi";
                    rdcFront = Utils.barToPsi(rdcFront);
                }
                doc.getElementById("rdcF").setTextContent(Utils.toOneDecimalString(rdcFront) + s.pressureUnit);
                if (Faults.getFrontTirePressureCriticalActive()) {
                    doc.getElementById("rdcF").setAttribute("style",
                            doc.getElementById("rdcF").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#e20505;")
                    );
                } else if (Faults.getFrontTirePressureWarningActive()) {
                    doc.getElementById("rdcF").setAttribute("style",
                            doc.getElementById("rdcF").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#fcc914;")
                    );
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception getting front pressure: " + e.toString());
        }
        return;
    }

    public void setupClock(Document doc) {
        try {
            //Clock
            if (MotorcycleData.getTime() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm", Locale.getDefault());
                if (!s.sharedPrefs.getString("prefTime", "0").equals("0")) {
                    dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                }
                doc.getElementById("clock").setTextContent(dateFormat.format(MotorcycleData.getTime()));
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception getting clock: " + e.toString());
        }

        return;

    }

    public void setupEngineTemp(Document doc) {
        try {
            //Engine Temp
            if (MotorcycleData.getEngineTemperature() != null) {
                double engineTemp = MotorcycleData.getEngineTemperature();
                if (s.temperatureFormat.contains("1")) {
                    // F
                    s.temperatureUnit = "F";
                    engineTemp = Utils.celsiusToFahrenheit(engineTemp);
                }
                doc.getElementById("engineTemp").setTextContent(Utils.toZeroDecimalString(engineTemp) + s.temperatureUnit);
                if (MotorcycleData.getEngineTemperature() >= 104.0) {
                    doc.getElementById("engineTemp").setAttribute("style",
                            doc.getElementById("engineTemp").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#e20505;")
                    );
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception getting engine temp: " + e.toString());
        }
        return;
    }

    public void setupAmbientTemp(Document doc) {
        try {
            //Ambient Temp
            doc.getElementById("ambientTemp").setTextContent(this.getAmbientTemp());
        } catch (Exception e) {
            Log.d(TAG, "Exception setting ambient temp: " + e.toString());
        }

        return;
    }

    public void setupSpeedo(Document doc) {
        try {
            doc.getElementById("speed").setTextContent(this.getSpeedValue());


            //Speed Label
            doc.getElementById("speedUnit").setTextContent(s.distanceTimeUnit);
        } catch (Exception e) {
            Log.d(TAG, "Exception setting Speedo: " + e.toString());
        }

        return;
    }

    public void setupStandardLabels(Document doc) {
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Exception setting standard labels: " + e.toString());
        }

        return;
    }

    public void setupCustomData(Document doc, int infoLine) {
        try {
            //Data Label
            doc.getElementById("dataLabel").setTextContent(this.getDataLabel(infoLine));

            doc.getElementById("dataValue").setTextContent(this.getDataValue(infoLine));

            //Data Value
            if (infoLine == 1) {
                if (MotorcycleData.getFuelRange() != null) {
                    if (Faults.getFuelFaultActive()) {
                        doc.getElementById("dataValue").setAttribute("style",
                                doc.getElementById("dataValue").getAttribute("style").replaceAll("fill:([^<]*);", "fill:#e20505;")
                        );
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception setting custom data line: " + e.toString());
        }
    }

    public void setupRpmDialSport(Document doc) {
        try {
            //RPM Digits For GS
            if (s.tenK) {
                doc.getElementById("rpmDialDigit1").setTextContent("2");
                doc.getElementById("rpmDialDigit2").setTextContent("3");
                doc.getElementById("rpmDialDigit3").setTextContent("4");
                doc.getElementById("rpmDialDigit4").setTextContent("5");
                doc.getElementById("rpmDialDigit5").setTextContent("6");
                doc.getElementById("rpmDialDigit6").setTextContent("7");
                doc.getElementById("rpmDialDigit7").setTextContent("8");
                doc.getElementById("rpmDialDigit8").setTextContent("9");
                doc.getElementById("rpmDialDigit9").setTextContent("10");
            } else if (s.twelveK) {
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
        } catch (Exception e) {
            Log.d(TAG, "Exception setting sport tach dial: " + e.toString());
        }

        return;
    }

    public void setupTachSport(Document doc) {
        try {
            //RPM Dial
            if (MotorcycleData.getRPM() != null) {
                if (s.tenK) {
                    if (MotorcycleData.getRPM() >= 0) {
                        doc.getElementById("rpmTick1").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 154) {
                        doc.getElementById("rpmTick2").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 308) {
                        doc.getElementById("rpmTick3").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 462) {
                        doc.getElementById("rpmTick4").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 616) {
                        doc.getElementById("rpmTick5").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 770) {
                        doc.getElementById("rpmTick6").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 924) {
                        doc.getElementById("rpmTick7").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1078) {
                        doc.getElementById("rpmTick8").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1232) {
                        doc.getElementById("rpmTick9").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1386) {
                        doc.getElementById("rpmTick10").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1540) {
                        doc.getElementById("rpmTick11").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1694) {
                        doc.getElementById("rpmTick12").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1848) {
                        doc.getElementById("rpmTick13").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2000) {
                        doc.getElementById("rpmTick14").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2083) {
                        doc.getElementById("rpmTick15").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2167) {
                        doc.getElementById("rpmTick16").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2250) {
                        doc.getElementById("rpmTick17").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2334) {
                        doc.getElementById("rpmTick18").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2417) {
                        doc.getElementById("rpmTick19").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2501) {
                        doc.getElementById("rpmTick20").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2584) {
                        doc.getElementById("rpmTick21").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2668) {
                        doc.getElementById("rpmTick22").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2751) {
                        doc.getElementById("rpmTick23").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2835) {
                        doc.getElementById("rpmTick24").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2918) {
                        doc.getElementById("rpmTick25").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3000) {
                        doc.getElementById("rpmTick26").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3077) {
                        doc.getElementById("rpmTick27").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3154) {
                        doc.getElementById("rpmTick28").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3231) {
                        doc.getElementById("rpmTick29").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3308) {
                        doc.getElementById("rpmTick30").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3385) {
                        doc.getElementById("rpmTick31").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3462) {
                        doc.getElementById("rpmTick32").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3539) {
                        doc.getElementById("rpmTick33").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3616) {
                        doc.getElementById("rpmTick34").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3693) {
                        doc.getElementById("rpmTick35").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3770) {
                        doc.getElementById("rpmTick36").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3847) {
                        doc.getElementById("rpmTick37").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3924) {
                        doc.getElementById("rpmTick38").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4000) {
                        doc.getElementById("rpmTick39").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4077) {
                        doc.getElementById("rpmTick40").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4154) {
                        doc.getElementById("rpmTick41").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4231) {
                        doc.getElementById("rpmTick42").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4308) {
                        doc.getElementById("rpmTick43").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4385) {
                        doc.getElementById("rpmTick44").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4462) {
                        doc.getElementById("rpmTick45").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4539) {
                        doc.getElementById("rpmTick46").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4616) {
                        doc.getElementById("rpmTick47").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4693) {
                        doc.getElementById("rpmTick48").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4770) {
                        doc.getElementById("rpmTick49").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4847) {
                        doc.getElementById("rpmTick50").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4924) {
                        doc.getElementById("rpmTick51").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5000) {
                        doc.getElementById("rpmTick52").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5083) {
                        doc.getElementById("rpmTick53").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5166) {
                        doc.getElementById("rpmTick54").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5249) {
                        doc.getElementById("rpmTick55").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5332) {
                        doc.getElementById("rpmTick56").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5415) {
                        doc.getElementById("rpmTick57").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5498) {
                        doc.getElementById("rpmTick58").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5581) {
                        doc.getElementById("rpmTick59").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5664) {
                        doc.getElementById("rpmTick60").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5747) {
                        doc.getElementById("rpmTick61").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5830) {
                        doc.getElementById("rpmTick62").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5913) {
                        doc.getElementById("rpmTick63").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6000) {
                        doc.getElementById("rpmTick64").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6077) {
                        doc.getElementById("rpmTick65").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6154) {
                        doc.getElementById("rpmTick66").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6231) {
                        doc.getElementById("rpmTick67").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6308) {
                        doc.getElementById("rpmTick68").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6385) {
                        doc.getElementById("rpmTick69").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6462) {
                        doc.getElementById("rpmTick70").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6539) {
                        doc.getElementById("rpmTick71").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6616) {
                        doc.getElementById("rpmTick72").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6693) {
                        doc.getElementById("rpmTick73").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6770) {
                        doc.getElementById("rpmTick74").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6847) {
                        doc.getElementById("rpmTick75").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6924) {
                        doc.getElementById("rpmTick76").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7000) {
                        doc.getElementById("rpmTick77").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7083) {
                        doc.getElementById("rpmTick78").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7166) {
                        doc.getElementById("rpmTick79").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7249) {
                        doc.getElementById("rpmTick80").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7332) {
                        doc.getElementById("rpmTick81").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7415) {
                        doc.getElementById("rpmTick82").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7498) {
                        doc.getElementById("rpmTick83").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7581) {
                        doc.getElementById("rpmTick84").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7664) {
                        doc.getElementById("rpmTick85").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7747) {
                        doc.getElementById("rpmTick86").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7830) {
                        doc.getElementById("rpmTick87").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7913) {
                        doc.getElementById("rpmTick88").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8000) {
                        doc.getElementById("rpmTick89").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8083) {
                        doc.getElementById("rpmTick90").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8166) {
                        doc.getElementById("rpmTick91").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8249) {
                        doc.getElementById("rpmTick92").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8332) {
                        doc.getElementById("rpmTick93").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8415) {
                        doc.getElementById("rpmTick94").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8498) {
                        doc.getElementById("rpmTick95").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8581) {
                        doc.getElementById("rpmTick96").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8664) {
                        doc.getElementById("rpmTick97").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8747) {
                        doc.getElementById("rpmTick98").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8830) {
                        doc.getElementById("rpmTick99").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8913) {
                        doc.getElementById("rpmTick100").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9000) {
                        doc.getElementById("rpmTick101").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9066) {
                        doc.getElementById("rpmTick102").setAttribute("style", "display:inline");
                    }
                    // Needle
                    if ((MotorcycleData.getRPM() >= 0) && (MotorcycleData.getRPM() <= 249)) {
                        doc.getElementById("rpmNeedle0").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 250) && (MotorcycleData.getRPM() <= 499)) {
                        doc.getElementById("rpmNeedle1").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 500) && (MotorcycleData.getRPM() <= 749)) {
                        doc.getElementById("rpmNeedle2").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 750) && (MotorcycleData.getRPM() <= 999)) {
                        doc.getElementById("rpmNeedle3").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1000) && (MotorcycleData.getRPM() <= 1249)) {
                        doc.getElementById("rpmNeedle4").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1250) && (MotorcycleData.getRPM() <= 1499)) {
                        doc.getElementById("rpmNeedle5").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1500) && (MotorcycleData.getRPM() <= 1749)) {
                        doc.getElementById("rpmNeedle6").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1750) && (MotorcycleData.getRPM() <= 1999)) {
                        doc.getElementById("rpmNeedle7").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2000) && (MotorcycleData.getRPM() <= 2166)) {
                        doc.getElementById("rpmNeedle8").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2167) && (MotorcycleData.getRPM() <= 2332)) {
                        doc.getElementById("rpmNeedle9").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2333) && (MotorcycleData.getRPM() <= 2499)) {
                        doc.getElementById("rpmNeedle10").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2500) && (MotorcycleData.getRPM() <= 2600)) {
                        doc.getElementById("rpmNeedle11").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2601) && (MotorcycleData.getRPM() <= 2700)) {
                        doc.getElementById("rpmNeedle12").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2701) && (MotorcycleData.getRPM() <= 2800)) {
                        doc.getElementById("rpmNeedle13").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2801) && (MotorcycleData.getRPM() <= 2900)) {
                        doc.getElementById("rpmNeedle14").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3000) && (MotorcycleData.getRPM() <= 3166)) {
                        doc.getElementById("rpmNeedle15").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3167) && (MotorcycleData.getRPM() <= 3332)) {
                        doc.getElementById("rpmNeedle16").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3333) && (MotorcycleData.getRPM() <= 3499)) {
                        doc.getElementById("rpmNeedle17").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3500) && (MotorcycleData.getRPM() <= 3600)) {
                        doc.getElementById("rpmNeedle18").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3601) && (MotorcycleData.getRPM() <= 3700)) {
                        doc.getElementById("rpmNeedle19").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3701) && (MotorcycleData.getRPM() <= 3800)) {
                        doc.getElementById("rpmNeedle20").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3801) && (MotorcycleData.getRPM() <= 3900)) {
                        doc.getElementById("rpmNeedle21").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3901) && (MotorcycleData.getRPM() <= 4000)) {
                        doc.getElementById("rpmNeedle22").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4001) && (MotorcycleData.getRPM() <= 4124)) {
                        doc.getElementById("rpmNeedle23").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4125) && (MotorcycleData.getRPM() <= 4249)) {
                        doc.getElementById("rpmNeedle24").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4250) && (MotorcycleData.getRPM() <= 4374)) {
                        doc.getElementById("rpmNeedle25").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4375) && (MotorcycleData.getRPM() <= 4499)) {
                        doc.getElementById("rpmNeedle26").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4500) && (MotorcycleData.getRPM() <= 4674)) {
                        doc.getElementById("rpmNeedle27").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4750) && (MotorcycleData.getRPM() <= 4874)) {
                        doc.getElementById("rpmNeedle28").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4875) && (MotorcycleData.getRPM() <= 4999)) {
                        doc.getElementById("rpmNeedle29").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5000) && (MotorcycleData.getRPM() <= 5124)) {
                        doc.getElementById("rpmNeedle30").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5125) && (MotorcycleData.getRPM() <= 5249)) {
                        doc.getElementById("rpmNeedle31").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5250) && (MotorcycleData.getRPM() <= 5374)) {
                        doc.getElementById("rpmNeedle32").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5375) && (MotorcycleData.getRPM() <= 5499)) {
                        doc.getElementById("rpmNeedle33").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5500) && (MotorcycleData.getRPM() <= 5624)) {
                        doc.getElementById("rpmNeedle34").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5625) && (MotorcycleData.getRPM() <= 5749)) {
                        doc.getElementById("rpmNeedle35").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5750) && (MotorcycleData.getRPM() <= 5874)) {
                        doc.getElementById("rpmNeedle36").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5875) && (MotorcycleData.getRPM() <= 6000)) {
                        doc.getElementById("rpmNeedle37").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6001) && (MotorcycleData.getRPM() <= 6142)) {
                        doc.getElementById("rpmNeedle38").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6143) && (MotorcycleData.getRPM() <= 6285)) {
                        doc.getElementById("rpmNeedle39").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6286) && (MotorcycleData.getRPM() <= 6428)) {
                        doc.getElementById("rpmNeedle40").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6429) && (MotorcycleData.getRPM() <= 6571)) {
                        doc.getElementById("rpmNeedle41").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6572) && (MotorcycleData.getRPM() <= 6714)) {
                        doc.getElementById("rpmNeedle42").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6715) && (MotorcycleData.getRPM() <= 6857)) {
                        doc.getElementById("rpmNeedle43").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6858) && (MotorcycleData.getRPM() <= 6999)) {
                        doc.getElementById("rpmNeedle44").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7000) && (MotorcycleData.getRPM() <= 7142)) {
                        doc.getElementById("rpmNeedle45").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7143) && (MotorcycleData.getRPM() <= 7285)) {
                        doc.getElementById("rpmNeedle46").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7286) && (MotorcycleData.getRPM() <= 7428)) {
                        doc.getElementById("rpmNeedle47").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7429) && (MotorcycleData.getRPM() <= 7571)) {
                        doc.getElementById("rpmNeedle48").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7572) && (MotorcycleData.getRPM() <= 7714)) {
                        doc.getElementById("rpmNeedle49").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7715) && (MotorcycleData.getRPM() <= 7857)) {
                        doc.getElementById("rpmNeedle50").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7858) && (MotorcycleData.getRPM() <= 7999)) {
                        doc.getElementById("rpmNeedle51").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8000) && (MotorcycleData.getRPM() <= 8142)) {
                        doc.getElementById("rpmNeedle52").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8143) && (MotorcycleData.getRPM() <= 8285)) {
                        doc.getElementById("rpmNeedle53").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8286) && (MotorcycleData.getRPM() <= 8428)) {
                        doc.getElementById("rpmNeedle54").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8429) && (MotorcycleData.getRPM() <= 8571)) {
                        doc.getElementById("rpmNeedle55").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8572) && (MotorcycleData.getRPM() <= 8714)) {
                        doc.getElementById("rpmNeedle56").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8715) && (MotorcycleData.getRPM() <= 8857)) {
                        doc.getElementById("rpmNeedle57").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8858) && (MotorcycleData.getRPM() <= 8999)) {
                        doc.getElementById("rpmNeedle58").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9000) && (MotorcycleData.getRPM() <= 9143)) {
                        doc.getElementById("rpmNeedle59").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9144) && (MotorcycleData.getRPM() <= 9287)) {
                        doc.getElementById("rpmNeedle60").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9288) && (MotorcycleData.getRPM() <= 9431)) {
                        doc.getElementById("rpmNeedle61").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9432) && (MotorcycleData.getRPM() <= 9575)) {
                        doc.getElementById("rpmNeedle62").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9576) && (MotorcycleData.getRPM() <= 9719)) {
                        doc.getElementById("rpmNeedle63").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9720) && (MotorcycleData.getRPM() <= 9863)) {
                        doc.getElementById("rpmNeedle64").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9864) && (MotorcycleData.getRPM() <= 9999)) {
                        doc.getElementById("rpmNeedle65").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10000) {
                        doc.getElementById("rpmNeedle66").setAttribute("style", "display:inline");
                    }
                } else if (s.twelveK) {
                    if (MotorcycleData.getRPM() >= 0) {
                        doc.getElementById("rpmTick1").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 154) {
                        doc.getElementById("rpmTick2").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 308) {
                        doc.getElementById("rpmTick3").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 462) {
                        doc.getElementById("rpmTick4").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 616) {
                        doc.getElementById("rpmTick5").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 770) {
                        doc.getElementById("rpmTick6").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 924) {
                        doc.getElementById("rpmTick7").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1078) {
                        doc.getElementById("rpmTick8").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1232) {
                        doc.getElementById("rpmTick9").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1386) {
                        doc.getElementById("rpmTick10").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1540) {
                        doc.getElementById("rpmTick11").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1694) {
                        doc.getElementById("rpmTick12").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1848) {
                        doc.getElementById("rpmTick13").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2000) {
                        doc.getElementById("rpmTick14").setAttribute("style", "display:inline");
                    }

                    if (MotorcycleData.getRPM() >= 2167) {
                        doc.getElementById("rpmTick15").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2167) {
                        doc.getElementById("rpmTick16").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2334) {
                        doc.getElementById("rpmTick17").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2501) {
                        doc.getElementById("rpmTick18").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2668) {
                        doc.getElementById("rpmTick19").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2835) {
                        doc.getElementById("rpmTick20").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3002) {
                        doc.getElementById("rpmTick21").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3169) {
                        doc.getElementById("rpmTick22").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3503) {
                        doc.getElementById("rpmTick23").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3670) {
                        doc.getElementById("rpmTick24").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3837) {
                        doc.getElementById("rpmTick25").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4000) {
                        doc.getElementById("rpmTick26").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4154) {
                        doc.getElementById("rpmTick27").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4308) {
                        doc.getElementById("rpmTick28").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4462) {
                        doc.getElementById("rpmTick29").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4616) {
                        doc.getElementById("rpmTick30").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4770) {
                        doc.getElementById("rpmTick31").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4924) {
                        doc.getElementById("rpmTick32").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5078) {
                        doc.getElementById("rpmTick33").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5232) {
                        doc.getElementById("rpmTick34").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5386) {
                        doc.getElementById("rpmTick35").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5540) {
                        doc.getElementById("rpmTick36").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5694) {
                        doc.getElementById("rpmTick37").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5848) {
                        doc.getElementById("rpmTick38").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6000) {
                        doc.getElementById("rpmTick39").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6077) {
                        doc.getElementById("rpmTick40").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6154) {
                        doc.getElementById("rpmTick41").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6231) {
                        doc.getElementById("rpmTick42").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6308) {
                        doc.getElementById("rpmTick43").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6385) {
                        doc.getElementById("rpmTick44").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6462) {
                        doc.getElementById("rpmTick45").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6539) {
                        doc.getElementById("rpmTick46").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6616) {
                        doc.getElementById("rpmTick47").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6693) {
                        doc.getElementById("rpmTick48").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6770) {
                        doc.getElementById("rpmTick49").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6847) {
                        doc.getElementById("rpmTick50").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6924) {
                        doc.getElementById("rpmTick51").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7000) {
                        doc.getElementById("rpmTick52").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7083) {
                        doc.getElementById("rpmTick53").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7166) {
                        doc.getElementById("rpmTick54").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7249) {
                        doc.getElementById("rpmTick55").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7332) {
                        doc.getElementById("rpmTick56").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7415) {
                        doc.getElementById("rpmTick57").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7498) {
                        doc.getElementById("rpmTick58").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7581) {
                        doc.getElementById("rpmTick59").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7664) {
                        doc.getElementById("rpmTick60").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7747) {
                        doc.getElementById("rpmTick61").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7830) {
                        doc.getElementById("rpmTick62").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7913) {
                        doc.getElementById("rpmTick63").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8000) {
                        doc.getElementById("rpmTick64").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8077) {
                        doc.getElementById("rpmTick65").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8154) {
                        doc.getElementById("rpmTick66").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8231) {
                        doc.getElementById("rpmTick67").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8308) {
                        doc.getElementById("rpmTick68").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8385) {
                        doc.getElementById("rpmTick69").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8462) {
                        doc.getElementById("rpmTick70").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8539) {
                        doc.getElementById("rpmTick71").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8616) {
                        doc.getElementById("rpmTick72").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8693) {
                        doc.getElementById("rpmTick73").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8770) {
                        doc.getElementById("rpmTick74").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8847) {
                        doc.getElementById("rpmTick75").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8924) {
                        doc.getElementById("rpmTick76").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9000) {
                        doc.getElementById("rpmTick77").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9083) {
                        doc.getElementById("rpmTick78").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9166) {
                        doc.getElementById("rpmTick79").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9249) {
                        doc.getElementById("rpmTick80").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9332) {
                        doc.getElementById("rpmTick81").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9415) {
                        doc.getElementById("rpmTick82").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9498) {
                        doc.getElementById("rpmTick83").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9581) {
                        doc.getElementById("rpmTick84").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9664) {
                        doc.getElementById("rpmTick85").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9747) {
                        doc.getElementById("rpmTick86").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9830) {
                        doc.getElementById("rpmTick87").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9913) {
                        doc.getElementById("rpmTick88").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10000) {
                        doc.getElementById("rpmTick89").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10083) {
                        doc.getElementById("rpmTick90").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10166) {
                        doc.getElementById("rpmTick91").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10249) {
                        doc.getElementById("rpmTick92").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10332) {
                        doc.getElementById("rpmTick93").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10415) {
                        doc.getElementById("rpmTick94").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10498) {
                        doc.getElementById("rpmTick95").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10581) {
                        doc.getElementById("rpmTick96").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10664) {
                        doc.getElementById("rpmTick97").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10747) {
                        doc.getElementById("rpmTick98").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10830) {
                        doc.getElementById("rpmTick99").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10913) {
                        doc.getElementById("rpmTick100").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11000) {
                        doc.getElementById("rpmTick101").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11066) {
                        doc.getElementById("rpmTick102").setAttribute("style", "display:inline");
                    }
                    // Needle
                    if ((MotorcycleData.getRPM() >= 0) && (MotorcycleData.getRPM() <= 249)) {
                        doc.getElementById("rpmNeedle0").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 250) && (MotorcycleData.getRPM() <= 499)) {
                        doc.getElementById("rpmNeedle1").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 500) && (MotorcycleData.getRPM() <= 749)) {
                        doc.getElementById("rpmNeedle2").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 750) && (MotorcycleData.getRPM() <= 999)) {
                        doc.getElementById("rpmNeedle3").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1000) && (MotorcycleData.getRPM() <= 1249)) {
                        doc.getElementById("rpmNeedle4").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1250) && (MotorcycleData.getRPM() <= 1499)) {
                        doc.getElementById("rpmNeedle5").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1500) && (MotorcycleData.getRPM() <= 1749)) {
                        doc.getElementById("rpmNeedle6").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1750) && (MotorcycleData.getRPM() <= 1999)) {
                        doc.getElementById("rpmNeedle7").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2000) && (MotorcycleData.getRPM() <= 2286)) {
                        doc.getElementById("rpmNeedle8").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2287) && (MotorcycleData.getRPM() <= 2573)) {
                        doc.getElementById("rpmNeedle9").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2574) && (MotorcycleData.getRPM() <= 2860)) {
                        doc.getElementById("rpmNeedle10").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2861) && (MotorcycleData.getRPM() <= 3147)) {
                        doc.getElementById("rpmNeedle11").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3148) && (MotorcycleData.getRPM() <= 3434)) {
                        doc.getElementById("rpmNeedle12").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3435) && (MotorcycleData.getRPM() <= 3721)) {
                        doc.getElementById("rpmNeedle13").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3722) && (MotorcycleData.getRPM() <= 3999)) {
                        doc.getElementById("rpmNeedle14").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4000) && (MotorcycleData.getRPM() <= 4249)) {
                        doc.getElementById("rpmNeedle15").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4250) && (MotorcycleData.getRPM() <= 4499)) {
                        doc.getElementById("rpmNeedle16").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4500) && (MotorcycleData.getRPM() <= 4749)) {
                        doc.getElementById("rpmNeedle17").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4750) && (MotorcycleData.getRPM() <= 4999)) {
                        doc.getElementById("rpmNeedle18").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5000) && (MotorcycleData.getRPM() <= 5249)) {
                        doc.getElementById("rpmNeedle19").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5250) && (MotorcycleData.getRPM() <= 5499)) {
                        doc.getElementById("rpmNeedle20").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5500) && (MotorcycleData.getRPM() <= 5749)) {
                        doc.getElementById("rpmNeedle21").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5750) && (MotorcycleData.getRPM() <= 6000)) {
                        doc.getElementById("rpmNeedle22").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6001) && (MotorcycleData.getRPM() <= 6124)) {
                        doc.getElementById("rpmNeedle23").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6125) && (MotorcycleData.getRPM() <= 6249)) {
                        doc.getElementById("rpmNeedle24").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6250) && (MotorcycleData.getRPM() <= 6374)) {
                        doc.getElementById("rpmNeedle25").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6375) && (MotorcycleData.getRPM() <= 6499)) {
                        doc.getElementById("rpmNeedle26").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6500) && (MotorcycleData.getRPM() <= 6674)) {
                        doc.getElementById("rpmNeedle27").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6750) && (MotorcycleData.getRPM() <= 6874)) {
                        doc.getElementById("rpmNeedle28").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6875) && (MotorcycleData.getRPM() <= 6999)) {
                        doc.getElementById("rpmNeedle29").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7000) && (MotorcycleData.getRPM() <= 7124)) {
                        doc.getElementById("rpmNeedle30").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7125) && (MotorcycleData.getRPM() <= 7249)) {
                        doc.getElementById("rpmNeedle31").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7250) && (MotorcycleData.getRPM() <= 7374)) {
                        doc.getElementById("rpmNeedle32").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7375) && (MotorcycleData.getRPM() <= 7499)) {
                        doc.getElementById("rpmNeedle33").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7500) && (MotorcycleData.getRPM() <= 7624)) {
                        doc.getElementById("rpmNeedle34").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7625) && (MotorcycleData.getRPM() <= 7749)) {
                        doc.getElementById("rpmNeedle35").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7750) && (MotorcycleData.getRPM() <= 7874)) {
                        doc.getElementById("rpmNeedle36").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7875) && (MotorcycleData.getRPM() <= 8000)) {
                        doc.getElementById("rpmNeedle37").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8001) && (MotorcycleData.getRPM() <= 6142)) {
                        doc.getElementById("rpmNeedle38").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6143) && (MotorcycleData.getRPM() <= 6285)) {
                        doc.getElementById("rpmNeedle39").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6286) && (MotorcycleData.getRPM() <= 6428)) {
                        doc.getElementById("rpmNeedle40").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6429) && (MotorcycleData.getRPM() <= 6571)) {
                        doc.getElementById("rpmNeedle41").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6572) && (MotorcycleData.getRPM() <= 6714)) {
                        doc.getElementById("rpmNeedle42").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6715) && (MotorcycleData.getRPM() <= 6857)) {
                        doc.getElementById("rpmNeedle43").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6858) && (MotorcycleData.getRPM() <= 6999)) {
                        doc.getElementById("rpmNeedle44").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9000) && (MotorcycleData.getRPM() <= 9142)) {
                        doc.getElementById("rpmNeedle45").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9143) && (MotorcycleData.getRPM() <= 9285)) {
                        doc.getElementById("rpmNeedle46").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9286) && (MotorcycleData.getRPM() <= 9428)) {
                        doc.getElementById("rpmNeedle47").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9429) && (MotorcycleData.getRPM() <= 9571)) {
                        doc.getElementById("rpmNeedle48").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9572) && (MotorcycleData.getRPM() <= 9714)) {
                        doc.getElementById("rpmNeedle49").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9715) && (MotorcycleData.getRPM() <= 9857)) {
                        doc.getElementById("rpmNeedle50").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9858) && (MotorcycleData.getRPM() <= 9999)) {
                        doc.getElementById("rpmNeedle51").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10000) && (MotorcycleData.getRPM() <= 10142)) {
                        doc.getElementById("rpmNeedle52").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10143) && (MotorcycleData.getRPM() <= 10285)) {
                        doc.getElementById("rpmNeedle53").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10286) && (MotorcycleData.getRPM() <= 10428)) {
                        doc.getElementById("rpmNeedle54").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10429) && (MotorcycleData.getRPM() <= 10571)) {
                        doc.getElementById("rpmNeedle55").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10572) && (MotorcycleData.getRPM() <= 10714)) {
                        doc.getElementById("rpmNeedle56").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10715) && (MotorcycleData.getRPM() <= 10857)) {
                        doc.getElementById("rpmNeedle57").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10858) && (MotorcycleData.getRPM() <= 10999)) {
                        doc.getElementById("rpmNeedle58").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11000) && (MotorcycleData.getRPM() <= 11143)) {
                        doc.getElementById("rpmNeedle59").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11144) && (MotorcycleData.getRPM() <= 11287)) {
                        doc.getElementById("rpmNeedle60").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11288) && (MotorcycleData.getRPM() <= 11431)) {
                        doc.getElementById("rpmNeedle61").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11432) && (MotorcycleData.getRPM() <= 11575)) {
                        doc.getElementById("rpmNeedle62").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11576) && (MotorcycleData.getRPM() <= 11719)) {
                        doc.getElementById("rpmNeedle63").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11720) && (MotorcycleData.getRPM() <= 11863)) {
                        doc.getElementById("rpmNeedle64").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11864) && (MotorcycleData.getRPM() <= 11999)) {
                        doc.getElementById("rpmNeedle65").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12000) {
                        doc.getElementById("rpmNeedle66").setAttribute("style", "display:inline");
                    }
                } else {
                    if (MotorcycleData.getRPM() >= 0) {
                        doc.getElementById("rpmTick1").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 154) {
                        doc.getElementById("rpmTick2").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 308) {
                        doc.getElementById("rpmTick3").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 462) {
                        doc.getElementById("rpmTick4").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 616) {
                        doc.getElementById("rpmTick5").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 770) {
                        doc.getElementById("rpmTick6").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 924) {
                        doc.getElementById("rpmTick7").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1078) {
                        doc.getElementById("rpmTick8").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1232) {
                        doc.getElementById("rpmTick9").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1386) {
                        doc.getElementById("rpmTick10").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1540) {
                        doc.getElementById("rpmTick11").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1694) {
                        doc.getElementById("rpmTick12").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 1848) {
                        doc.getElementById("rpmTick13").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2000) {
                        doc.getElementById("rpmTick14").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2167) {
                        doc.getElementById("rpmTick15").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2334) {
                        doc.getElementById("rpmTick16").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2501) {
                        doc.getElementById("rpmTick17").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2668) {
                        doc.getElementById("rpmTick18").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 2835) {
                        doc.getElementById("rpmTick19").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3002) {
                        doc.getElementById("rpmTick20").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3169) {
                        doc.getElementById("rpmTick21").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3336) {
                        doc.getElementById("rpmTick22").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3503) {
                        doc.getElementById("rpmTick23").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3670) {
                        doc.getElementById("rpmTick24").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 3837) {
                        doc.getElementById("rpmTick25").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4000) {
                        doc.getElementById("rpmTick26").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4308) {
                        doc.getElementById("rpmTick27").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4616) {
                        doc.getElementById("rpmTick28").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 4924) {
                        doc.getElementById("rpmTick29").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5232) {
                        doc.getElementById("rpmTick30").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5540) {
                        doc.getElementById("rpmTick31").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 5848) {
                        doc.getElementById("rpmTick32").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6156) {
                        doc.getElementById("rpmTick33").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6464) {
                        doc.getElementById("rpmTick34").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 6772) {
                        doc.getElementById("rpmTick35").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7080) {
                        doc.getElementById("rpmTick36").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7388) {
                        doc.getElementById("rpmTick37").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 7696) {
                        doc.getElementById("rpmTick38").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8000) {
                        doc.getElementById("rpmTick39").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8077) {
                        doc.getElementById("rpmTick40").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8154) {
                        doc.getElementById("rpmTick41").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8231) {
                        doc.getElementById("rpmTick42").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8308) {
                        doc.getElementById("rpmTick43").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8385) {
                        doc.getElementById("rpmTick44").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8462) {
                        doc.getElementById("rpmTick45").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8539) {
                        doc.getElementById("rpmTick46").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8616) {
                        doc.getElementById("rpmTick47").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8693) {
                        doc.getElementById("rpmTick48").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8770) {
                        doc.getElementById("rpmTick49").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8847) {
                        doc.getElementById("rpmTick50").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 8924) {
                        doc.getElementById("rpmTick51").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9000) {
                        doc.getElementById("rpmTick52").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9083) {
                        doc.getElementById("rpmTick53").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9166) {
                        doc.getElementById("rpmTick54").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9249) {
                        doc.getElementById("rpmTick55").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9332) {
                        doc.getElementById("rpmTick56").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9415) {
                        doc.getElementById("rpmTick57").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9498) {
                        doc.getElementById("rpmTick58").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9581) {
                        doc.getElementById("rpmTick59").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9664) {
                        doc.getElementById("rpmTick60").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9747) {
                        doc.getElementById("rpmTick61").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9830) {
                        doc.getElementById("rpmTick62").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 9913) {
                        doc.getElementById("rpmTick63").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10000) {
                        doc.getElementById("rpmTick64").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10077) {
                        doc.getElementById("rpmTick65").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10154) {
                        doc.getElementById("rpmTick66").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10231) {
                        doc.getElementById("rpmTick67").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10308) {
                        doc.getElementById("rpmTick68").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10385) {
                        doc.getElementById("rpmTick69").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10462) {
                        doc.getElementById("rpmTick70").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10539) {
                        doc.getElementById("rpmTick71").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10616) {
                        doc.getElementById("rpmTick72").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10693) {
                        doc.getElementById("rpmTick73").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10770) {
                        doc.getElementById("rpmTick74").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10847) {
                        doc.getElementById("rpmTick75").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 10924) {
                        doc.getElementById("rpmTick76").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11000) {
                        doc.getElementById("rpmTick77").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11083) {
                        doc.getElementById("rpmTick78").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11166) {
                        doc.getElementById("rpmTick79").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11249) {
                        doc.getElementById("rpmTick80").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11332) {
                        doc.getElementById("rpmTick81").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11415) {
                        doc.getElementById("rpmTick82").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11498) {
                        doc.getElementById("rpmTick83").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11581) {
                        doc.getElementById("rpmTick84").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11664) {
                        doc.getElementById("rpmTick85").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11747) {
                        doc.getElementById("rpmTick86").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11830) {
                        doc.getElementById("rpmTick87").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 11913) {
                        doc.getElementById("rpmTick88").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12000) {
                        doc.getElementById("rpmTick89").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12083) {
                        doc.getElementById("rpmTick90").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12166) {
                        doc.getElementById("rpmTick91").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12249) {
                        doc.getElementById("rpmTick92").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12332) {
                        doc.getElementById("rpmTick93").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12415) {
                        doc.getElementById("rpmTick94").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12498) {
                        doc.getElementById("rpmTick95").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12581) {
                        doc.getElementById("rpmTick96").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12664) {
                        doc.getElementById("rpmTick97").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12747) {
                        doc.getElementById("rpmTick98").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12830) {
                        doc.getElementById("rpmTick99").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 12913) {
                        doc.getElementById("rpmTick100").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 13000) {
                        doc.getElementById("rpmTick101").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 13133) {
                        doc.getElementById("rpmTick102").setAttribute("style", "display:inline");
                    }
                    //Needle
                    if ((MotorcycleData.getRPM() >= 0) && (MotorcycleData.getRPM() <= 249)) {
                        doc.getElementById("rpmNeedle0").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 250) && (MotorcycleData.getRPM() <= 499)) {
                        doc.getElementById("rpmNeedle1").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 500) && (MotorcycleData.getRPM() <= 749)) {
                        doc.getElementById("rpmNeedle2").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 750) && (MotorcycleData.getRPM() <= 999)) {
                        doc.getElementById("rpmNeedle3").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1000) && (MotorcycleData.getRPM() <= 1249)) {
                        doc.getElementById("rpmNeedle4").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1250) && (MotorcycleData.getRPM() <= 1499)) {
                        doc.getElementById("rpmNeedle5").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1500) && (MotorcycleData.getRPM() <= 1749)) {
                        doc.getElementById("rpmNeedle6").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 1750) && (MotorcycleData.getRPM() <= 1999)) {
                        doc.getElementById("rpmNeedle7").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2000) && (MotorcycleData.getRPM() <= 2332)) {
                        doc.getElementById("rpmNeedle8").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2333) && (MotorcycleData.getRPM() <= 2665)) {
                        doc.getElementById("rpmNeedle9").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 2666) && (MotorcycleData.getRPM() <= 2998)) {
                        doc.getElementById("rpmNeedle10").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3000) && (MotorcycleData.getRPM() <= 3249)) {
                        doc.getElementById("rpmNeedle11").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3250) && (MotorcycleData.getRPM() <= 3499)) {
                        doc.getElementById("rpmNeedle12").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3500) && (MotorcycleData.getRPM() <= 3749)) {
                        doc.getElementById("rpmNeedle13").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 3750) && (MotorcycleData.getRPM() <= 3999)) {
                        doc.getElementById("rpmNeedle14").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4000) && (MotorcycleData.getRPM() <= 4499)) {
                        doc.getElementById("rpmNeedle15").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 4500) && (MotorcycleData.getRPM() <= 4999)) {
                        doc.getElementById("rpmNeedle16").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5000) && (MotorcycleData.getRPM() <= 5499)) {
                        doc.getElementById("rpmNeedle17").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 5500) && (MotorcycleData.getRPM() <= 5999)) {
                        doc.getElementById("rpmNeedle18").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6000) && (MotorcycleData.getRPM() <= 6499)) {
                        doc.getElementById("rpmNeedle19").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 6500) && (MotorcycleData.getRPM() <= 6999)) {
                        doc.getElementById("rpmNeedle20").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7000) && (MotorcycleData.getRPM() <= 7499)) {
                        doc.getElementById("rpmNeedle21").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 7500) && (MotorcycleData.getRPM() <= 8000)) {
                        doc.getElementById("rpmNeedle22").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8001) && (MotorcycleData.getRPM() <= 8124)) {
                        doc.getElementById("rpmNeedle23").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8125) && (MotorcycleData.getRPM() <= 8249)) {
                        doc.getElementById("rpmNeedle24").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8250) && (MotorcycleData.getRPM() <= 8374)) {
                        doc.getElementById("rpmNeedle25").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8375) && (MotorcycleData.getRPM() <= 8499)) {
                        doc.getElementById("rpmNeedle26").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8500) && (MotorcycleData.getRPM() <= 8674)) {
                        doc.getElementById("rpmNeedle27").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8750) && (MotorcycleData.getRPM() <= 8874)) {
                        doc.getElementById("rpmNeedle28").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 8875) && (MotorcycleData.getRPM() <= 8999)) {
                        doc.getElementById("rpmNeedle29").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9000) && (MotorcycleData.getRPM() <= 9124)) {
                        doc.getElementById("rpmNeedle30").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9125) && (MotorcycleData.getRPM() <= 9249)) {
                        doc.getElementById("rpmNeedle31").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9250) && (MotorcycleData.getRPM() <= 9374)) {
                        doc.getElementById("rpmNeedle32").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9375) && (MotorcycleData.getRPM() <= 9499)) {
                        doc.getElementById("rpmNeedle33").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9500) && (MotorcycleData.getRPM() <= 9624)) {
                        doc.getElementById("rpmNeedle34").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9625) && (MotorcycleData.getRPM() <= 9749)) {
                        doc.getElementById("rpmNeedle35").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9750) && (MotorcycleData.getRPM() <= 9874)) {
                        doc.getElementById("rpmNeedle36").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 9875) && (MotorcycleData.getRPM() <= 10000)) {
                        doc.getElementById("rpmNeedle37").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10001) && (MotorcycleData.getRPM() <= 10142)) {
                        doc.getElementById("rpmNeedle38").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10143) && (MotorcycleData.getRPM() <= 10285)) {
                        doc.getElementById("rpmNeedle39").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10286) && (MotorcycleData.getRPM() <= 10428)) {
                        doc.getElementById("rpmNeedle40").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10429) && (MotorcycleData.getRPM() <= 10571)) {
                        doc.getElementById("rpmNeedle41").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10572) && (MotorcycleData.getRPM() <= 10714)) {
                        doc.getElementById("rpmNeedle42").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10715) && (MotorcycleData.getRPM() <= 10857)) {
                        doc.getElementById("rpmNeedle43").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 10858) && (MotorcycleData.getRPM() <= 10999)) {
                        doc.getElementById("rpmNeedle44").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11000) && (MotorcycleData.getRPM() <= 11142)) {
                        doc.getElementById("rpmNeedle45").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11143) && (MotorcycleData.getRPM() <= 11285)) {
                        doc.getElementById("rpmNeedle46").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11286) && (MotorcycleData.getRPM() <= 11428)) {
                        doc.getElementById("rpmNeedle47").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11429) && (MotorcycleData.getRPM() <= 11571)) {
                        doc.getElementById("rpmNeedle48").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11572) && (MotorcycleData.getRPM() <= 11714)) {
                        doc.getElementById("rpmNeedle49").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11715) && (MotorcycleData.getRPM() <= 11857)) {
                        doc.getElementById("rpmNeedle50").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 11858) && (MotorcycleData.getRPM() <= 11999)) {
                        doc.getElementById("rpmNeedle51").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 12000) && (MotorcycleData.getRPM() <= 12142)) {
                        doc.getElementById("rpmNeedle52").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 12143) && (MotorcycleData.getRPM() <= 12285)) {
                        doc.getElementById("rpmNeedle53").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 12286) && (MotorcycleData.getRPM() <= 12428)) {
                        doc.getElementById("rpmNeedle54").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 12429) && (MotorcycleData.getRPM() <= 12571)) {
                        doc.getElementById("rpmNeedle55").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 12572) && (MotorcycleData.getRPM() <= 12714)) {
                        doc.getElementById("rpmNeedle56").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 12715) && (MotorcycleData.getRPM() <= 12857)) {
                        doc.getElementById("rpmNeedle57").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 12858) && (MotorcycleData.getRPM() <= 12999)) {
                        doc.getElementById("rpmNeedle58").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 13000) && (MotorcycleData.getRPM() <= 13285)) {
                        doc.getElementById("rpmNeedle59").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 13286) && (MotorcycleData.getRPM() <= 13571)) {
                        doc.getElementById("rpmNeedle60").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 13572) && (MotorcycleData.getRPM() <= 13857)) {
                        doc.getElementById("rpmNeedle61").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 13858) && (MotorcycleData.getRPM() <= 14143)) {
                        doc.getElementById("rpmNeedle62").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 14144) && (MotorcycleData.getRPM() <= 14429)) {
                        doc.getElementById("rpmNeedle63").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 14430) && (MotorcycleData.getRPM() <= 14715)) {
                        doc.getElementById("rpmNeedle64").setAttribute("style", "display:inline");
                    }
                    if ((MotorcycleData.getRPM() >= 14716) && (MotorcycleData.getRPM() <= 14999)) {
                        doc.getElementById("rpmNeedle65").setAttribute("style", "display:inline");
                    }
                    if (MotorcycleData.getRPM() >= 15000) {
                        doc.getElementById("rpmNeedle66").setAttribute("style", "display:inline");
                    }
                }

            }

        } catch (Exception e) {
            Log.d(TAG, "Exception setting sport tach: " + e.toString());
        }

        return;
    }

    public void setupInclinometer(Document doc) {

        try {
            //Compass
            Double leanAngle = MotorcycleData.getLeanAngleDevice();

            String centerRadius = ", 540, 1540)";
            String angle = "0";
            String angleMaxL = "...";
            String angleMaxR = "...";

            //Lean Angle
            if (MotorcycleData.getLeanAngleBike() != null) {
                // Use bike value if available
                leanAngle = MotorcycleData.getLeanAngleBike();
                //Left Max Angle
                if (MotorcycleData.getLeanAngleBikeMaxL() != null) {
                    angleMaxL = Utils.toZeroDecimalString(MotorcycleData.getLeanAngleBikeMaxL());
                }
                //Right Max Angle
                if (MotorcycleData.getLeanAngleBikeMaxR() != null) {
                    angleMaxR = Utils.toZeroDecimalString(MotorcycleData.getLeanAngleBikeMaxR());
                }
            } else {
                // Fallback to device sensor reading
                //Left Max Angle
                if (MotorcycleData.getLeanAngleDeviceMaxL() != null) {
                    angleMaxL = Utils.toZeroDecimalString(MotorcycleData.getLeanAngleDeviceMaxL());
                }
                //Right Max Angle
                if (MotorcycleData.getLeanAngleDeviceMaxR() != null) {
                    angleMaxR = Utils.toZeroDecimalString(MotorcycleData.getLeanAngleDeviceMaxR());
                }
            }

            if (leanAngle != null) {
                if (leanAngle >  60) {
                    leanAngle = 60.0;
                } else if (leanAngle < -60) {
                    leanAngle = -60.0;
                }
                leanAngle *= 1.5;
                angle = leanAngle.toString();
            }

            if (isDevicePortrait()) {
                centerRadius = ",540, 1540)";
            }
            setText(doc, "angle", Utils.toZeroDecimalString(Math.abs(leanAngle)));
            setText(doc, "angleMaxL", angleMaxL);
            setText(doc, "angleMaxR", angleMaxR);
            doc.getElementById("needle").setAttribute("transform",
                    "rotate(" + angle + centerRadius);

        } catch (Exception e) {
            Log.d(TAG, "Exception Setting Up Compass: " + e.toString());
        }
    }


    public void setupCompass(Document doc) {
        try {
            setText(doc, "txtCardinalLeft", "W");
            setText(doc, "txtCardinalRight", "E");
            //Compass
            String centerRadius = ",960,1080)";
            String bearing = "0";

            if (MotorcycleData.getBearing() != null) {
                bearing = String.valueOf(MotorcycleData.getBearing() * -1);
                if (isDevicePortrait()) {
                    centerRadius = ",528,960)";
                }
            }
            doc.getElementById("compass").setAttribute("transform",
                    "rotate(" + bearing + centerRadius);

        } catch (Exception e) {
            Log.d(TAG, "Exception Setting Up Compass: " + e.toString());
        }
        return;
    }

    private static boolean setText(Document doc, String id, String text) {
        try {
            org.w3c.dom.Element e = doc.getElementById(id);
            if (e != null) {
                e.setTextContent(text);
                return true;
            }
        } catch (Exception E) {
            Log.d(TAG, "Exception Setting Text: " + E.toString());
        }
        return false;
    }


}

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
package com.blackboxembedded.WunderLINQ.hardware.WLQ;

import android.content.Context;
import android.content.ContextWrapper;

import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;

import java.util.ArrayList;

public class Faults extends ContextWrapper {

    // Motorcycle faults
    private static boolean absSelfDiagActive = false;
    private static String absSelfDiagDesc = "";

    private static boolean absDeactivatedActive = false;
    private static String absDeactivatedDesc = "";

    private static boolean absErrorActive = false;
    private static String absErrorDesc = "";

    private static boolean ascSelfDiagActive = false;
    private static String ascSelfDiagDesc = "";

    private static boolean ascInterventionActive = false;
    private static String ascInterventionDesc = "";

    private static boolean ascDeactivatedActive = false;
    private static String ascDeactivatedDesc = "";

    private static boolean ascErrorActive = false;
    private static String ascErrorDesc = "";

    private static boolean fuelFaultActive = false;
    private static String fuelFaultDesc = "";

    private static boolean frontTirePressureWarningActive = false;
    private static String frontTirePressureWarningDesc = "";

    private static boolean rearTirePressureWarningActive = false;
    private static String rearTirePressureWarningDesc = "";

    private static boolean frontTirePressureCriticalNotificationActive = false;
    private static boolean frontTirePressureCriticalActive = false;
    private static String frontTirePressureCriticalDesc = "";

    private static boolean rearTirePressureCriticalNotificationActive = false;
    private static boolean rearTirePressureCriticalActive = false;
    private static String rearTirePressureCriticalDesc = "";

    private static boolean addFrontLightOneActive = false;
    private static String addFrontLightOneDesc = "";

    private static boolean addFrontLightTwoActive = false;
    private static String addFrontLightTwoDesc = "";

    private static boolean daytimeRunningActive = false;
    private static String daytimeRunningDesc = "";

    private static boolean frontLeftSignalActive = false;
    private static String frontLeftSignalDesc = "";

    private static boolean frontRightSignalActive = false;
    private static String frontRightSignalDesc = "";

    private static boolean rearLeftSignalActive = false;
    private static String rearLeftSignalDesc = "";

    private static boolean rearRightSignalActive = false;
    private static String rearRightSignalDesc = "";

    private static boolean frontParkingLightOneActive = false;
    private static String frontParkingLightOneDesc = "";

    private static boolean frontParkingLightTwoActive = false;
    private static String frontParkingLightTwoDesc = "";

    private static boolean lowBeamActive = false;
    private static String lowBeamDesc = "";

    private static boolean highBeamActive = false;
    private static String highBeamDesc = "";

    private static boolean rearLightActive = false;
    private static String rearLightDesc = "";

    private static boolean brakeLightActive = false;
    private static String brakeLightDesc = "";

    private static boolean licenseLightActive = false;
    private static String licenseLightDesc = "";

    private static boolean rearFogLightActive = false;
    private static String rearFogLightDesc = "";

    private static boolean addDippedLightActive = false;
    private static String addDippedLightDesc = "";

    private static boolean addBrakeLightActive = false;
    private static String addBrakeLightDesc = "";

    private static boolean frontLampOneLightActive = false;
    private static String frontLampOneLightDesc = "";

    private static boolean frontLampTwoLightActive = false;
    private static String frontLampTwoLightDesc = "";

    private static boolean iceWarningActive = false;
    private static String iceWarningDesc = "";

    private static boolean generalFlashingYellowActive = false;
    private static String generalFlashingYellowDesc = "";

    private static boolean generalShowsYellowActive = false;
    private static String generalShowsYellowDesc = "";

    private static boolean generalFlashingRedNotificationActive = false;
    private static boolean generalFlashingRedActive = false;
    private static String generalFlashingRedDesc = "";

    private static boolean generalShowsRedNotificationActive = false;
    private static boolean generalShowsRedActive = false;
    private static String generalShowsRedDesc = "";

    private static boolean oilLowActive = false;
    private static String oilLowDesc = "";

    private static boolean serviceActive = false;
    private static String serviceDesc = "";

    public Faults(Context base) {
        super(base);
        // Motorcycle faults
        absSelfDiagDesc = MyApplication.getContext().getResources().getString(R.string.fault_ABSSLF);
        absDeactivatedDesc = MyApplication.getContext().getResources().getString(R.string.fault_ABSDAC);
        absErrorDesc = MyApplication.getContext().getResources().getString(R.string.fault_ABSERR);
        ascSelfDiagDesc = MyApplication.getContext().getResources().getString(R.string.fault_ASCSLF);
        ascInterventionDesc = MyApplication.getContext().getResources().getString(R.string.fault_ASCINT);
        ascDeactivatedDesc = MyApplication.getContext().getResources().getString(R.string.fault_ASCDAC);
        ascErrorDesc = MyApplication.getContext().getResources().getString(R.string.fault_ASCERR);
        fuelFaultDesc = MyApplication.getContext().getResources().getString(R.string.fault_FUELF);
        frontTirePressureWarningDesc = MyApplication.getContext().getResources().getString(R.string.fault_TIREFWF);
        rearTirePressureWarningDesc = MyApplication.getContext().getResources().getString(R.string.fault_TIRERWF);
        frontTirePressureCriticalDesc = MyApplication.getContext().getResources().getString(R.string.fault_TIREFCF);
        rearTirePressureCriticalDesc = MyApplication.getContext().getResources().getString(R.string.fault_TIRERCF);
        addFrontLightOneDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPAFL1);
        addFrontLightTwoDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPAFL2);
        daytimeRunningDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPDAY);
        frontLeftSignalDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPFLI);
        frontRightSignalDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPFRI);
        rearLeftSignalDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPRLI);
        rearRightSignalDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPRRI);
        frontParkingLightOneDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPFPK1);
        frontParkingLightTwoDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPFPK2);
        lowBeamDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPLOW);
        highBeamDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPHI);
        rearLightDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPREAR);
        brakeLightDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPBRK);
        licenseLightDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPLIC);
        rearFogLightDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPRFOG);
        addDippedLightDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPADDD);
        addBrakeLightDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPADDB);
        frontLampOneLightDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPFL1);
        frontLampTwoLightDesc = MyApplication.getContext().getResources().getString(R.string.fault_LAMPFL2);
        iceWarningDesc = MyApplication.getContext().getResources().getString(R.string.fault_ICEWARN);
        generalFlashingYellowDesc = MyApplication.getContext().getResources().getString(R.string.fault_GENWARNFSYLW);
        generalShowsYellowDesc = MyApplication.getContext().getResources().getString(R.string.fault_GENWARNSHYLW);
        generalFlashingRedDesc = MyApplication.getContext().getResources().getString(R.string.fault_GENWARNFSRED);
        generalShowsRedDesc = MyApplication.getContext().getResources().getString(R.string.fault_GENWARNSHRED);
        oilLowDesc = MyApplication.getContext().getResources().getString(R.string.fault_OILLOW);
        serviceDesc = MyApplication.getContext().getResources().getString(R.string.fault_SERVICE);
    }

    public static ArrayList<String> getallActiveDesc() {
        ArrayList<String> allActiveDesc = new ArrayList<String>();

        // Motorcycle faults
        if(absSelfDiagActive){
            allActiveDesc.add(absSelfDiagDesc);
        }
        if(absDeactivatedActive){
            allActiveDesc.add(absDeactivatedDesc);
        }
        if(absErrorActive){
            allActiveDesc.add(absErrorDesc);
        }
        if(ascSelfDiagActive){
            allActiveDesc.add(ascSelfDiagDesc);
        }
        if(ascInterventionActive){
            allActiveDesc.add(ascInterventionDesc);
        }
        if(ascDeactivatedActive){
            allActiveDesc.add(ascDeactivatedDesc);
        }
        if(ascErrorActive){
            allActiveDesc.add(ascErrorDesc);
        }
        if(fuelFaultActive){
            allActiveDesc.add(fuelFaultDesc);
        }
        if(frontTirePressureWarningActive){
            allActiveDesc.add(frontTirePressureWarningDesc);
        }
        if(rearTirePressureWarningActive){
            allActiveDesc.add(rearTirePressureWarningDesc);
        }
        if(frontTirePressureCriticalActive){
            allActiveDesc.add(frontTirePressureCriticalDesc);
        }
        if(rearTirePressureCriticalActive){
            allActiveDesc.add(rearTirePressureCriticalDesc);
        }
        if(addFrontLightOneActive){
            allActiveDesc.add(addFrontLightOneDesc);
        }
        if(addFrontLightTwoActive){
            allActiveDesc.add(addFrontLightTwoDesc);
        }
        if(daytimeRunningActive){
            allActiveDesc.add(daytimeRunningDesc);
        }
        if(frontLeftSignalActive){
            allActiveDesc.add(frontLeftSignalDesc);
        }
        if(frontRightSignalActive){
            allActiveDesc.add(frontRightSignalDesc);
        }
        if(rearLeftSignalActive){
            allActiveDesc.add(rearLeftSignalDesc);
        }
        if(rearRightSignalActive){
            allActiveDesc.add(rearRightSignalDesc);
        }
        if(frontParkingLightOneActive){
            allActiveDesc.add(frontParkingLightOneDesc);
        }
        if(frontParkingLightTwoActive){
            allActiveDesc.add(frontParkingLightTwoDesc);
        }
        if(lowBeamActive){
            allActiveDesc.add(lowBeamDesc);
        }
        if(highBeamActive){
            allActiveDesc.add(highBeamDesc);
        }
        if(rearLightActive){
            allActiveDesc.add(rearLightDesc);
        }
        if(brakeLightActive){
            allActiveDesc.add(brakeLightDesc);
        }
        if(licenseLightActive){
            allActiveDesc.add(licenseLightDesc);
        }
        if(rearFogLightActive){
            allActiveDesc.add(rearFogLightDesc);
        }
        if(addDippedLightActive){
            allActiveDesc.add(addDippedLightDesc);
        }
        if(addBrakeLightActive){
            allActiveDesc.add(addBrakeLightDesc);
        }
        if(frontLampOneLightActive){
            allActiveDesc.add(frontLampOneLightDesc);
        }
        if(frontLampTwoLightActive){
            allActiveDesc.add(frontLampTwoLightDesc);
        }
        if(iceWarningActive){
            allActiveDesc.add(iceWarningDesc);
        }
        if(generalFlashingYellowActive){
            allActiveDesc.add(generalFlashingYellowDesc);
        }
        if(generalShowsYellowActive){
            allActiveDesc.add(generalShowsYellowDesc);
        }
        if(generalFlashingRedActive){
            allActiveDesc.add(generalFlashingRedDesc);
        }
        if(generalShowsRedActive){
            allActiveDesc.add(generalShowsRedDesc);
        }
        if(oilLowActive){
            allActiveDesc.add(oilLowDesc);
        }
        if(serviceActive){
            allActiveDesc.add(serviceDesc);
        }

        return allActiveDesc;
    }

    // Motorcycle faults
    public static void setAbsSelfDiagActive(boolean absSelfDiagActive){
        Faults.absSelfDiagActive = absSelfDiagActive;
    }
    public static boolean getabsSelfDiagActive() {
        return absSelfDiagActive;
    }
    public static String getabsSelfDiagDesc() {
        return absSelfDiagDesc;
    }

    public static void setAbsDeactivatedActive(boolean absDeactivatedActive){
        Faults.ascDeactivatedActive = absDeactivatedActive;
    }
    public static boolean getabsDeactivatedActive() {
        return absDeactivatedActive;
    }
    public static String getabsDeactivatedDesc() {
        return absDeactivatedDesc;
    }

    public static void setabsErrorActive(boolean absErrorActive){
        Faults.absErrorActive = absErrorActive;
    }
    public static boolean getabsErrorActive() {
        return absErrorActive;
    }
    public static String getabsErrorDesc() {
        return absErrorDesc;
    }

    public static void setAscSelfDiagActive(boolean ascSelfDiagActive){
        Faults.ascSelfDiagActive = ascSelfDiagActive;
    }
    public static boolean getascSelfDiagActive() {
        return ascSelfDiagActive;
    }
    public static String getascSelfDiagDesc() {
        return ascSelfDiagDesc;
    }

    public static void setAscInterventionActive(boolean ascInterventionActive){
        Faults.ascInterventionActive = ascInterventionActive;
    }
    public static boolean getascInterventionActive() {
        return ascInterventionActive;
    }
    public static String getascInterventionDesc() {
        return ascInterventionDesc;
    }

    public static void setAscDeactivatedActive(boolean ascDeactivatedActive){
        Faults.ascDeactivatedActive = ascDeactivatedActive;
    }
    public static boolean getascDeactivatedActive() {
        return ascDeactivatedActive;
    }
    public static String getascDeactivatedDesc() {
        return ascDeactivatedDesc;
    }

    public static void setascErrorActive(boolean ascErrorActive){
        Faults.ascErrorActive = ascErrorActive;
    }
    public static boolean getascErrorActive() {
        return ascErrorActive;
    }
    public static String getascErrorDesc() {
        return ascErrorDesc;
    }

    public static void setfuelFaultActive(boolean fuelFaultActive){
        Faults.fuelFaultActive = fuelFaultActive;
    }
    public static boolean getfuelFaultActive() {
        return fuelFaultActive;
    }
    public static String getfuelFaultDesc() {
        return fuelFaultDesc;
    }

    public static void setfrontTirePressureWarningActive(boolean frontTirePressureWarningActive){
        Faults.frontTirePressureWarningActive = frontTirePressureWarningActive;
    }
    public static boolean getfrontTirePressureWarningActive() {
        return frontTirePressureWarningActive;
    }
    public static String getfrontTirePressureWarningDesc() {
        return frontTirePressureWarningDesc;
    }

    public static void setrearTirePressureWarningActive(boolean rearTirePressureWarningActive){
        Faults.rearTirePressureWarningActive = rearTirePressureWarningActive;
    }
    public static boolean getrearTirePressureWarningActive() {
        return rearTirePressureWarningActive;
    }
    public static String getrearTirePressureWarningDesc() {
        return rearTirePressureWarningDesc;
    }

    public static void setfrontTirePressureCriticalActive(boolean frontTirePressureCriticalActive){
        Faults.frontTirePressureCriticalActive = frontTirePressureCriticalActive;
    }
    public static boolean getfrontTirePressureCriticalActive() {
        return frontTirePressureCriticalActive;
    }
    public static String getfrontTirePressureCriticalDesc() {
        return frontTirePressureCriticalDesc;
    }
    public static void setfrontTirePressureCriticalNotificationActive(boolean frontTirePressureCriticalNotificationActive){
        Faults.frontTirePressureCriticalNotificationActive = frontTirePressureCriticalNotificationActive;
    }
    public static boolean getfrontTirePressureCriticalNotificationActive() {
        return frontTirePressureCriticalNotificationActive;
    }

    public static void setrearTirePressureCriticalActive(boolean rearTirePressureCriticalActive){
        Faults.rearTirePressureCriticalActive = rearTirePressureCriticalActive;
    }
    public static boolean getrearTirePressureCriticalActive() {
        return rearTirePressureCriticalActive;
    }
    public static String getrearTirePressureCriticalDesc() {
        return rearTirePressureCriticalDesc;
    }
    public static void setrearTirePressureCriticalNotificationActive(boolean rearTirePressureCriticalNotificationActive){
        Faults.rearTirePressureCriticalNotificationActive = rearTirePressureCriticalNotificationActive;
    }
    public static boolean getrearTirePressureCriticalNotificationActive() {
        return rearTirePressureCriticalNotificationActive;
    }

    public static void setAddFrontLightOneActive(boolean addFrontLightOneActive){
        Faults.addFrontLightOneActive = addFrontLightOneActive;
    }
    public static boolean getaddFrontLightOneActive() {
        return addFrontLightOneActive;
    }
    public static String getaddFrontLightOneDesc() {
        return addFrontLightOneDesc;
    }

    public static void setAddFrontLightTwoActive(boolean addFrontLightTwoActive){
        Faults.addFrontLightTwoActive = addFrontLightTwoActive;
    }
    public static boolean getaddFrontLightTwoActive() {
        return addFrontLightTwoActive;
    }
    public static String getaddFrontLightTwoDesc() {
        return addFrontLightTwoDesc;
    }

    public static void setDaytimeRunningActive(boolean daytimeRunningActive){
        Faults.daytimeRunningActive = daytimeRunningActive;
    }
    public static boolean getdaytimeRunningActive() {
        return daytimeRunningActive;
    }
    public static String getdaytimeRunninDesc() {
        return daytimeRunningDesc;
    }

    public static void setfrontLeftSignalActive(boolean frontLeftSignalActive){
        Faults.frontLeftSignalActive = frontLeftSignalActive;
    }
    public static boolean getfrontLeftSignalActive() {
        return frontLeftSignalActive;
    }
    public static String getfrontLeftSignalDesc() {
        return frontLeftSignalDesc;
    }

    public static void setfrontRightSignalActive(boolean frontRightSignalActive){
        Faults.frontRightSignalActive = frontRightSignalActive;
    }
    public static boolean getfrontRightSignalActive() {
        return frontRightSignalActive;
    }
    public static String getfrontRightSignalDesc() {
        return frontRightSignalDesc;
    }

    public static void setrearLeftSignalActive(boolean rearLeftSignalActive){
        Faults.rearLeftSignalActive = rearLeftSignalActive;
    }
    public static boolean getrearLeftSignalActive() {
        return rearLeftSignalActive;
    }
    public static String getrearLeftSignalDesc() {
        return rearLeftSignalDesc;
    }

    public static void setrearRightSignalActive(boolean rearRightSignalActive){
        Faults.rearRightSignalActive = rearRightSignalActive;
    }
    public static boolean getrearRightSignalActive() {
        return rearRightSignalActive;
    }
    public static String getrearRightSignalDesc() {
        return rearRightSignalDesc;
    }

    public static void setFrontParkingLightOneActive(boolean frontParkingLightOneActive){
        Faults.frontParkingLightOneActive = frontParkingLightOneActive;
    }
    public static boolean getfrontParkingLightOneActive() {
        return frontParkingLightOneActive;
    }
    public static String getfrontParkingLightOneDesc() {
        return frontParkingLightOneDesc;
    }

    public static void setFrontParkingLightTwoActive(boolean frontParkingLightTwoActive){
        Faults.frontParkingLightTwoActive = frontParkingLightTwoActive;
    }
    public static boolean getfrontParkingLightTwoActive() {
        return frontParkingLightTwoActive;
    }
    public static String getfrontParkingLightTwoDesc() {
        return frontParkingLightTwoDesc;
    }

    public static void setLowBeamActive(boolean lowBeamActive){
        Faults.lowBeamActive = lowBeamActive;
    }
    public static boolean getlowBeamActive() {
        return lowBeamActive;
    }
    public static String getLowBeamDesc() {
        return lowBeamDesc;
    }

    public static void setHighBeamActive(boolean highBeamActive){
        Faults.highBeamActive = highBeamActive;
    }
    public static boolean gethighBeamActive() {
        return highBeamActive;
    }
    public static String getHighBeamDesc() {
        return highBeamDesc;
    }

    public static void setRearLightActive(boolean rearLightActive){
        Faults.rearLightActive = rearLightActive;
    }
    public static boolean getrearLightActive() {
        return rearLightActive;
    }
    public static String getRearLightDescDesc() {
        return rearLightDesc;
    }

    public static void setBrakeLightActive(boolean brakeLightActive){
        Faults.brakeLightActive = brakeLightActive;
    }
    public static boolean getBrakeLightActive() {
        return brakeLightActive;
    }
    public static String getBrakeLightDescDesc() {
        return brakeLightDesc;
    }

    public static void setLicenseLightActive(boolean licenseLightActive){
        Faults.licenseLightActive = licenseLightActive;
    }
    public static boolean getLicenseLightActive() {
        return licenseLightActive;
    }
    public static String getLicenseLightDescDesc() {
        return licenseLightDesc;
    }

    public static void setRearFogLightActive(boolean rearFogLightActive){
        Faults.rearFogLightActive = rearFogLightActive;
    }
    public static boolean getRearFogLightActive() {
        return rearFogLightActive;
    }
    public static String getRearFogLightDescDesc() {
        return rearFogLightDesc;
    }

    public static void setAddDippedLightActive(boolean addDippedLightActive){
        Faults.addDippedLightActive = addDippedLightActive;
    }
    public static boolean getAddDippedLightActive() {
        return addDippedLightActive;
    }
    public static String getAddDippedLightDescDesc() {
        return addDippedLightDesc;
    }

    public static void setAddBrakeLightActive(boolean addBrakeLightActive){
        Faults.addBrakeLightActive = addBrakeLightActive;
    }
    public static boolean getAddBrakeLightActive() {
        return addBrakeLightActive;
    }
    public static String getAddBrakeLightDescDesc() {
        return addBrakeLightDesc;
    }

    public static void setFrontLampOneLightActive(boolean frontLampOneLightActive){
        Faults.frontLampOneLightActive = frontLampOneLightActive;
    }
    public static boolean getFrontLampOneLightActive() {
        return frontLampOneLightActive;
    }
    public static String getFrontLampOneLightDescDesc() {
        return frontLampOneLightDesc;
    }

    public static void setFrontLampTwoLightActive(boolean frontLampTwoLightActive){
        Faults.frontLampTwoLightActive = frontLampTwoLightActive;
    }
    public static boolean getFrontLampvLightActive() {
        return frontLampTwoLightActive;
    }
    public static String getFrontLampTwoLightDescDesc() {
        return frontLampTwoLightDesc;
    }

    public static void seticeWarnActive(boolean iceWarningActive){
        Faults.iceWarningActive = iceWarningActive;
    }
    public static boolean geticeWarningActive() {
        return iceWarningActive;
    }
    public static String geticeWarningDesc() {
        return iceWarningDesc;
    }

    public static void setGeneralFlashingYellowActive(boolean generalFlashingYellowActive){
        Faults.generalFlashingYellowActive = generalFlashingYellowActive;
    }
    public static boolean getgeneralFlashingYellowActive() {
        return generalFlashingYellowActive;
    }
    public static String getGeneralFlashingYellowDesc() {
        return generalFlashingYellowDesc;
    }

    public static void setGeneralShowsYellowActive(boolean generalShowsYellowActive){
        Faults.generalShowsYellowActive = generalShowsYellowActive;
    }
    public static boolean getgeneralShowsYellowActive() {
        return generalShowsYellowActive;
    }
    public static String getGeneralShowsYellowDesc() {
        return generalShowsYellowDesc;
    }

    public static void setGeneralFlashingRedActive(boolean generalFlashingRedActive){
        Faults.generalFlashingRedActive = generalFlashingRedActive;
    }
    public static boolean getgeneralFlashingRedActive() {
        return generalFlashingRedActive;
    }
    public static String getGeneralFlashingRedDesc() {
        return generalFlashingRedDesc;
    }
    public static void setGeneralFlashingRedNotificationActive(boolean generalFlashingRedNotificationActive){
        Faults.generalFlashingRedNotificationActive = generalFlashingRedNotificationActive;
    }
    public static boolean getgeneralFlashingRedNotificationActive() {
        return generalFlashingRedNotificationActive;
    }

    public static void setGeneralShowsRedActive(boolean generalShowsRedActive){
        Faults.generalShowsRedActive = generalShowsRedActive;
    }
    public static boolean getgeneralShowsRedActive() {
        return generalShowsRedActive;
    }
    public static String getGeneralShowsRedDesc() {
        return generalShowsRedDesc;
    }
    public static void setGeneralShowsRedNotificationActive(boolean generalShowsRedNotificationActive){
        Faults.generalShowsRedNotificationActive = generalShowsRedNotificationActive;
    }
    public static boolean getgeneralShowsRedNotificationActive() {
        return generalShowsRedNotificationActive;
    }

    public static void setOilLowActive(boolean oilLowActive){
        Faults.oilLowActive = oilLowActive;
    }
    public static boolean getOilLowActive() {
        return oilLowActive;
    }
    public static String getOilLowDesc() {
        return oilLowDesc;
    }

    public static void setServiceActive(boolean serviceActive){
        Faults.serviceActive = serviceActive;
    }
    public static boolean getServiceActive() {
        return serviceActive;
    }
    public static String getServiceDesc() {
        return serviceDesc;
    }

    // Utility functions
    public static void clear(){
        // Motorcycle Faults
        absSelfDiagActive = false;
        absDeactivatedActive = false;
        absErrorActive = false;
        ascSelfDiagActive = false;
        ascInterventionActive = false;
        ascDeactivatedActive = false;
        ascErrorActive = false;
        fuelFaultActive = false;
        frontTirePressureWarningActive = false;
        rearTirePressureWarningActive = false;
        frontTirePressureCriticalActive = false;
        rearTirePressureCriticalActive = false;
        addFrontLightOneActive = false;
        addFrontLightTwoActive = false;
        daytimeRunningActive = false;
        frontLeftSignalActive = false;
        frontRightSignalActive = false;
        rearLeftSignalActive = false;
        rearRightSignalActive = false;
        frontParkingLightOneActive = false;
        frontParkingLightTwoActive = false;
        lowBeamActive = false;
        highBeamActive = false;
        rearLightActive = false;
        brakeLightActive = false;
        licenseLightActive = false;
        rearFogLightActive = false;
        addDippedLightActive = false;
        addBrakeLightActive = false;
        frontLampOneLightActive = false;
        frontLampTwoLightActive = false;
        iceWarningActive = false;
        generalFlashingYellowActive = false;
        generalShowsYellowActive = false;
        generalFlashingRedActive = false;
        generalShowsRedActive = false;
        oilLowActive = false;
        serviceActive = false;
    }
}


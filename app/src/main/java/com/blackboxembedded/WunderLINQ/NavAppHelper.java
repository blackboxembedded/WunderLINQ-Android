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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;

public class NavAppHelper {

    static public void open(Activity activity){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String navApp = sharedPrefs.getString("prefNavApp", "1");
        Intent navIntent = new Intent(android.content.Intent.ACTION_MAIN);
        String url = "google.navigation://?free=1&mode=d&entry=fnls";
        switch (navApp){
            default: case "1": //Android Default or Google Maps
                //Nothing to do
                break;
            case "2": //Google Maps
                navIntent.setPackage("com.google.android.apps.maps");
                break;
            case "3": //Locus Maps
                url = "http://link.locusmap.eu";
                navIntent.setPackage("menion.android.locus.pro");
                navIntent.setData(Uri.parse(url));
                if(!isCallable(activity, navIntent)){
                    navIntent.setPackage("menion.android.locus");
                }
                break;
            case "4": //Waze
                url = "https://waze.com/ul";
                navIntent.setData(Uri.parse(url));
                break;
            case "5": //Maps.me
                url = "mapsme://?back_url=wunderlinq://datagrid";
                navIntent.setData(Uri.parse(url));
                break;
            case "6": //OsmAnd
                url = "http://osmand.net/go";
                navIntent.setData(Uri.parse(url));
                break;
            case "7": //Mapfactor Navigator
                navIntent.setPackage("com.mapfactor.navigator");
                url = "http://maps.google.com/maps";
                navIntent.setData(Uri.parse(url));
                break;
            case "8": //Sygic
                //https://www.sygic.com/developers/professional-navigation-sdk/android/api-examples/custom-url
                url = "com.sygic.aura://";
                navIntent.setData(Uri.parse(url));
                break;
            case "9": //Kurviger 2
                navIntent = activity.getPackageManager().getLaunchIntentForPackage("gr.talent.kurviger");
                break;
            case "10": //TomTom GO
                navIntent = activity.getPackageManager().getLaunchIntentForPackage("com.tomtom.gplay.navapp");
                break;
            case "11": //BMW ConnectedRide
                String discoveredBMWApp = installedApps(activity,"com.bmw.ConnectedRide");
                if (!discoveredBMWApp.equals("")) {
                    navIntent = activity.getPackageManager().getLaunchIntentForPackage(discoveredBMWApp);
                }
                break;
            case "12": //Calimoto
                String discoveredCalimotoApp = installedApps(activity, "com.calimoto.calimoto");
                if (!discoveredCalimotoApp.equals("")) {
                    navIntent = activity.getPackageManager().getLaunchIntentForPackage(discoveredCalimotoApp);
                }
                break;
            case "13": //Kurviger 1 Pro
                navIntent = activity.getPackageManager().getLaunchIntentForPackage("gr.talent.kurviger.pro");
                break;
            case "14": //CoPilot GPS
                url = "copilot://&EnableCustomButton=true&AppLaunchBundleID=com.blackboxembedded.WunderLINQ";
                navIntent.setData(Uri.parse(url));
                break;
            case "15": //Yandex
                url = "yandexnavi://";
                navIntent.setData(Uri.parse(url));
                break;
            case "16": //Cartograph
                url = "cartograph://?backurl=wunderlinq://datagrid";
                navIntent.setData(Uri.parse(url));
                break;
            case "17": //Organic Maps
                url = "om://?backurl=wunderlinq://datagrid";
                navIntent.setData(Uri.parse(url));
                break;
            case "18": //Cruiser
                navIntent = activity.getPackageManager().getLaunchIntentForPackage("gr.talent.cruiser");
                break;
            case "19": //OruxMaps
                navIntent = activity.getPackageManager().getLaunchIntentForPackage("com.orux.oruxmapsDonate");
                break;
        }
        try {
            if (navIntent != null) {
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    if (activity.isInMultiWindowMode()) {
                        navIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    }
                }
                activity.startActivity(navIntent);
            }
        } catch ( ActivityNotFoundException ex  ) {
        }
    }

    static public void navigateTo(Activity activity, Location start, Location end){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String navApp = sharedPrefs.getString("prefNavApp", "1");
        Intent homeNavIntent = new Intent(android.content.Intent.ACTION_VIEW);
        String navUrl = "google.navigation:q=" + String.valueOf(end.getLatitude()) + "," + String.valueOf(end.getLongitude()) + "&navigate=yes";
        switch (navApp){
            default: case "1": case "11": case "12": //Android Default, BMW ConnectedRide, Calimoto
                //Nothing to do
                break;
            case "2": //Google Maps
                homeNavIntent.setPackage("com.google.android.apps.maps");
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "3": //Locus Maps
                homeNavIntent.setPackage("menion.android.locus.pro");
                homeNavIntent.setData(Uri.parse(navUrl));
                if(!isCallable(activity, homeNavIntent)){
                    homeNavIntent.setPackage("menion.android.locus");
                }
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "4": //Waze
                navUrl = "https://www.waze.com/ul?ll=" + String.valueOf(end.getLatitude()) + "," + String.valueOf(end.getLongitude()) + "&navigate=yes&zoom=17";
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "5": //Maps.me
                navUrl = "mapsme://route?sll=" + String.valueOf(start.getLatitude()) + "," + String.valueOf(start.getLongitude()) + "&saddr=" + MyApplication.getContext().getString(R.string.trip_view_waypoint_start_label) + "&dll=" + String.valueOf(end.getLatitude()) + "," + String.valueOf(end.getLongitude()) + "&daddr=" + MyApplication.getContext().getString(R.string.trip_view_waypoint_start_label) + "&type=vehicle&back_url=wunderlinq://datagrid";
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "6": //OsmAnd
                OsmAndHelper osmAndHelper = new OsmAndHelper(activity, 1001, null);
                osmAndHelper.navigate("Start",start.getLatitude(),start.getLongitude(),"Destination",end.getLatitude(),end.getLongitude(),"motorcycle", true, true);
                break;
            case "7": //Mapfactor Navigator
                homeNavIntent.setPackage("com.mapfactor.navigator");
                navUrl = "http://maps.google.com/maps?f=d&daddr=@"  + String.valueOf(end.getLatitude()) + "," + String.valueOf(end.getLongitude()) + "&navigate=yes";
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "8": //Sygic
                //https://www.sygic.com/developers/professional-navigation-sdk/android/api-examples/custom-url
                navUrl = "com.sygic.aura://coordinate|"  + String.valueOf(end.getLongitude()) + "|" + String.valueOf(end.getLatitude()) + "|drive";
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "9": //Kurviger 2
                homeNavIntent.setPackage("gr.talent.kurviger");
                navUrl = "https://kurviger.de/en?point="  + String.valueOf(end.getLatitude()) + "," + String.valueOf(end.getLongitude()) + "&vehicle=motorycycle"
                        + "weighting=fastest";
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "10": //TomTom GO
                homeNavIntent.setPackage("com.tomtom.gplay.navapp");
                navUrl = "geo:" + String.valueOf(end.getLatitude()) + "," + String.valueOf(end.getLongitude());
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "13": //Kurviger 1 Pro
                homeNavIntent.setPackage("gr.talent.kurviger.pro");
                navUrl = "https://kurviger.de/en?point="  + String.valueOf(end.getLatitude()) + "," + String.valueOf(end.getLongitude()) + "&vehicle=motorycycle"
                        + "weighting=fastest";
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "14": //CoPilot GPS
                //https://developer.trimblemaps.com/copilot-navigation/v10-19/feature-guide/advanced-features/url-launch/#send-stops
                navUrl = "copilot://options?type=STOPS&stop=Start||||||" + String.valueOf(start.getLatitude()) + "|" + String.valueOf(start.getLongitude()) + "&stop=Stop||||||" + String.valueOf(end.getLatitude()) + "|" + String.valueOf(end.getLongitude())
                        + "&EnableCustomButton=true&AppLaunchBundleID=com.blackboxembedded.WunderLINQ";
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "15": //Yandex
                navUrl = "yandexnavi://build_route_on_map?lat_to=" + String.valueOf(end.getLatitude()) + "&lon_to=" + String.valueOf(end.getLongitude()) ;
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "16": //Cartograph
                navUrl = "cartograph://route?geo="+String.valueOf(end.getLatitude()) + "," + String.valueOf(end.getLongitude()) + "&back_url=wunderlinq://datagrid";
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "17": //Organic Maps
                navUrl = "om://route?sll=" + String.valueOf(start.getLatitude()) + "," + String.valueOf(start.getLongitude()) + "&saddr=" + MyApplication.getContext().getString(R.string.trip_view_waypoint_start_label) + "&dll=" + String.valueOf(end.getLatitude()) + "," + String.valueOf(end.getLongitude()) + "&daddr=" + MyApplication.getContext().getString(R.string.trip_view_waypoint_end_label) + "&type=vehicle&backurl=wunderlinq://datagrid";
                homeNavIntent.setData(Uri.parse(navUrl));
                break;
            case "18": //Cruiser
                homeNavIntent = new Intent("com.devemux86.intent.action.NAVIGATION");
                homeNavIntent.setPackage("gr.talent.cruiser");
                homeNavIntent.putExtra("LATITUDE", new double[]{start.getLatitude(), end.getLatitude()});
                homeNavIntent.putExtra("LONGITUDE", new double[]{start.getLongitude(), end.getLongitude()});
                homeNavIntent.putExtra("NAME", new String[]{MyApplication.getContext().getString(R.string.trip_view_waypoint_start_label), MyApplication.getContext().getString(R.string.trip_view_waypoint_end_label)});
                break;
            case "19": //OruxMaps
                homeNavIntent = new Intent("com.oruxmaps.VIEW_MAP_ONLINE");
                homeNavIntent.putExtra("targetLat", new double[]{start.getLatitude(), end.getLatitude()});
                homeNavIntent.putExtra("targetLon", new double[]{start.getLongitude(), end.getLongitude()});
                homeNavIntent.putExtra("targetName", new String[]{MyApplication.getContext().getString(R.string.trip_view_waypoint_start_label), MyApplication.getContext().getString(R.string.trip_view_waypoint_end_label)});
                homeNavIntent.putExtra("navigatetoindex", 0); //index of the wpt. you want to start
                break;
        }
        if (!navApp.equals("6")) { // If NOT OsmAnd
            try {
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    if (activity.isInMultiWindowMode()) {
                        homeNavIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    }
                }
                activity.startActivity(homeNavIntent);
            } catch (ActivityNotFoundException ex) {
                // Add Alert
            }
        }
    }
    
    static public void viewWaypoint(Activity activity, Location waypoint, String label){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String navApp = sharedPrefs.getString("prefNavApp", "1");
        Intent navIntent = new Intent(android.content.Intent.ACTION_VIEW);
        String navUrl = "geo:0,0?q=" + String.valueOf(waypoint.getLatitude()) + "," + String.valueOf(waypoint.getLongitude()) + "(" + label + ")";
        switch (navApp){
            default: case "1": case "11": case "12": //Android Default, BMW ConnectedRide, Calimoto
                //Nothing to do
                break;
            case "2": //Google Maps
                navIntent.setPackage("com.google.android.apps.maps");
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "3": //Locus Maps
                navIntent.setPackage("menion.android.locus.pro");
                navIntent.setData(Uri.parse(navUrl));
                if(!isCallable(activity, navIntent)){
                    navIntent.setPackage("menion.android.locus");
                }
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "4": //Waze
                navUrl = "https://www.waze.com/ul?ll=" + String.valueOf(waypoint.getLatitude()) + "," + String.valueOf(waypoint.getLongitude()) + "&zoom=10";
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "5": //Maps.me
                navUrl = "mapsme://map?ll=" + String.valueOf(waypoint.getLatitude()) + "," + String.valueOf(waypoint.getLongitude()) + "&n=" + label + "&back_url=wunderlinq://datagrid";
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "6": //OsmAnd
                //navUrl = "osmand.navigation:q=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes";
                OsmAndHelper osmAndHelper = new OsmAndHelper(activity, 1001, null);
                osmAndHelper.showLocation(waypoint.getLatitude(),waypoint.getLongitude());
                break;
            case "7": //Mapfactor Navigator
                navIntent.setPackage("com.mapfactor.navigator");
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "8": //Sygic
                //https://www.sygic.com/developers/professional-navigation-sdk/android/api-examples/custom-url
                navUrl = "com.sygic.aura://coordinate|"  + String.valueOf(waypoint.getLongitude()) + "|" + String.valueOf(waypoint.getLatitude()) + "|show";
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "9": //Kurviger 2
                navIntent.setPackage("gr.talent.kurviger");
                navUrl = "https://kurviger.de/en?point="  + String.valueOf(waypoint.getLatitude()) + "," + String.valueOf(waypoint.getLongitude()) + "&locale=en" +"&vehicle=motorycycle"
                        + "weighting=fastest" + "use_miles=true";
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "10": //TomTom GO
                navIntent.setPackage("com.tomtom.gplay.navapp");
                navUrl = "geo:" + String.valueOf(waypoint.getLatitude()) + "," + String.valueOf(waypoint.getLongitude());
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "13": //Kurviger 1 Pro
                navIntent.setPackage("gr.talent.kurviger.pro");
                navUrl = "https://kurviger.de/en?point="  + String.valueOf(waypoint.getLatitude()) + "," + String.valueOf(waypoint.getLongitude()) + "&locale=en" +"&vehicle=motorycycle"
                        + "weighting=fastest" + "use_miles=true";
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "14": //CoPilot GPS
                navUrl = "copilot://mydestination?type=LOCATION&action=VIEW&lat=" + String.valueOf(waypoint.getLatitude()) + "&long=" + String.valueOf(waypoint.getLongitude())
                        + "&EnableCustomButton=true&AppLaunchBundleID=com.blackboxembedded.WunderLINQ";
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "15": //Yandex
                navUrl = "yandexnavi://show_point_on_map?lat=" + String.valueOf(waypoint.getLatitude()) + "&lon=" + String.valueOf(waypoint.getLongitude()) + "&zoom=12&no-balloon=0&desc=" + label;
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "16": //Cartograph
                navUrl = "cartograph://view?geo="+String.valueOf(waypoint.getLatitude()) + "," + String.valueOf(waypoint.getLongitude()) + "&back_url=wunderlinq://datagrid";
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "17": //Organic Maps
                navUrl = "om://map?ll=" + String.valueOf(waypoint.getLatitude()) + "," + String.valueOf(waypoint.getLongitude()) + "&n=" + label + "&backurl=wunderlinq://datagrid";
                navIntent.setData(Uri.parse(navUrl));
                break;
            case "18": //Cruiser
                navIntent = new Intent("com.devemux86.intent.action.MAP_VIEW");
                navIntent.setPackage("gr.talent.cruiser");
                navIntent.putExtra("LATITUDE", waypoint.getLatitude());
                navIntent.putExtra("LONGITUDE", waypoint.getLongitude());
                navIntent.putExtra("NAME", label);
                break;
            case "19": //OruxMaps
                navIntent = new Intent("com.oruxmaps.VIEW_MAP_ONLINE");
                navIntent.putExtra("targetLat", new double[]{waypoint.getLatitude()});
                navIntent.putExtra("targetLon", new double[]{waypoint.getLongitude()});
                navIntent.putExtra("targetName", new String[]{label});
                break;
        }
        if (!navApp.equals("6")) { // If NOT OsmAnd
            try {
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    if (activity.isInMultiWindowMode()) {
                        navIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    }
                }
                activity.startActivity(navIntent);
            } catch (ActivityNotFoundException ex) {
                // Add Alert
            }
        }
    }

    static public void roadbook(Activity activity){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String roadbookApp = sharedPrefs.getString("prefRoadBookApp", "1");
        Intent roadbookIntent = new Intent(android.content.Intent.ACTION_VIEW);
        String url = "rabbitrally://app?back_url=wunderlinq://quicktasks";
        switch (roadbookApp){
            default: case "1": //Android Default or Google Maps
                //Nothing to do
                break;
        }
        try {
            roadbookIntent.setData(Uri.parse(url));
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                if (activity.isInMultiWindowMode()) {
                    roadbookIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
                }
            }
            activity.startActivity(roadbookIntent);
        } catch ( ActivityNotFoundException ex  ) {
        }
    }

    static private boolean isCallable(Activity activity, Intent intent) {
        List<ResolveInfo> list = activity.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    //Looks for a partial string and returns the first package name that matches
    static public String installedApps(Activity activity, String app) {
        List<PackageInfo> packList = activity.getPackageManager().getInstalledPackages(0);
        for (int i=0; i < packList.size(); i++)
        {
            PackageInfo packInfo = packList.get(i);
            if (  (packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
            {
                String packageName = packInfo.packageName;
                if (packageName.contains(app)){
                    return packageName;
                }
            }
        }
        return "";
    }
}

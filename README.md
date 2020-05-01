#WunderLINQ Android App

The WunderLINQ app is for use with the [WunderLINQ Hardware](https://www.wunderlinq.com)

The WunderLINQ is a combination of Plug-and-Play hardware that snaps into your existing BMW Motorcycle 
Navigation Prep and a companion app for your Android or iOS phone or tablet.  Together they allow you 
to control your mobile device and other connected devices like GoPros from your handlebar wheel.  
If your motorcycle also has the On Board Computer Pro option the WunderLINQ can also receive and 
decode performance and fault data.

## Build Instructions
1. Clone the project and open in Android Studio

2. Create an xml resource file called res/values/secrets.xml with your own Google Maps API key in it, like so:

    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <string name="google_maps_api_key" translatable="false">YOUR_API_KEY_HERE</string>
    </resources>

3. Build and Run
package com.blackboxembedded.WunderLINQ;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class ContactListActivity extends AppCompatActivity {

    public final static String TAG = "ContactList";

    private ListView contactList;

    private ActionBar actionBar;
    private ImageButton backButton;
    private TextView navbarTitle;

    private SharedPreferences sharedPrefs;

    static boolean itsDark = false;
    private long darkTimer = 0;
    private long lightTimer = 0;

    SensorManager sensorManager;
    Sensor lightSensor;

    private ArrayList<String> contacts;
    private ArrayList<String> phoneNumbers;
    private ArrayList<Drawable> photoId;

    private int lastPosition = 0;

    private static final int PERMISSION_REQUEST_READ_CONTACTS = 102;

    private static final String[] PROJECTION = new String[] {
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
    };

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_contact_list);

        contactList = findViewById(R.id.lv_contacts);
        contactList.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(ContactListActivity.this, TaskActivity.class);
                startActivity(backIntent);
            }
        });

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        String orientation = sharedPrefs.getString("prefOrientation", "0");
        if (!orientation.equals("0")){
            if(orientation.equals("1")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else if (orientation.equals("2")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }

        showActionBar();

        if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getString("prefNightModeCombo", "0").equals("1")){
            itsDark = true;
        } else {
            itsDark = false;
        }

        // Check Read Contacts permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.contacts_alert_title));
                builder.setMessage(getString(R.string.contacts_alert_body));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
                    }
                });
                builder.show();
            } else {
                // Android version is lesser than 6.0 or the permission is already granted.
                updateList();
                updateColors(itsDark);
            }
        }  else {
            // Android version is lesser than 6.0 or the permission is already granted.
            updateList();
            updateColors(itsDark);
        }

        // Sensor Stuff
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getString("prefNightModeCombo", "0").equals("1")){
            updateColors(true);
        } else {
            updateColors(false);
        }
        if (sharedPrefs.getBoolean("prefAutoNightMode", false)) {
            sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onStop() {
        super.onStop();
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                updateList();
                updateColors(itsDark);
            } else {
                //GO Back
                Intent backIntent = new Intent(ContactListActivity.this, TaskActivity.class);
                startActivity(backIntent);
            }
        }
    }

    // Listens for light sensor events
    private final SensorEventListener sensorEventListener
            = new SensorEventListener(){

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (sharedPrefs.getString("prefNightModeCombo", "0").equals("2")) {
                int delay = (Integer.parseInt(sharedPrefs.getString("prefAutoNightModeDelay", "30")) * 1000);
                if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                    float currentReading = event.values[0];
                    double darkThreshold = 20.0;  // Light level to determine darkness
                    if (currentReading < darkThreshold) {
                        lightTimer = 0;
                        if (darkTimer == 0) {
                            darkTimer = System.currentTimeMillis();
                        } else {
                            long currentTime = System.currentTimeMillis();
                            long duration = (currentTime - darkTimer);
                            if ((duration >= delay) && (!itsDark)) {
                                itsDark = true;
                                Log.d(TAG, "Its dark");
                                // Update colors
                                updateColors(true);
                            }
                        }
                    } else {
                        darkTimer = 0;
                        if (lightTimer == 0) {
                            lightTimer = System.currentTimeMillis();
                        } else {
                            long currentTime = System.currentTimeMillis();
                            long duration = (currentTime - lightTimer);
                            if ((duration >= delay) && (itsDark)) {
                                itsDark = false;
                                Log.d(TAG, "Its light");
                                // Update colors
                                updateColors(false);
                            }
                        }
                    }
                }
            }
        }
    };

    public void updateColors(boolean itsDark){
        ((MyApplication) this.getApplication()).setitsDark(itsDark);
        LinearLayout lLayout = findViewById(R.id.layout_contact_list);
        if (itsDark) {
            //Set Brightness to default
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = -1;
            getWindow().setAttributes(layoutParams);

            lLayout.setBackgroundColor(getResources().getColor(R.color.black));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
            navbarTitle.setTextColor(getResources().getColor(R.color.white));
            backButton.setColorFilter(getResources().getColor(R.color.white));
        } else {
            if (sharedPrefs.getBoolean("prefBrightnessOverride", false)) {
                //Set Brightness to 100%
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = 1;
                getWindow().setAttributes(layoutParams);
            }

            lLayout.setBackgroundColor(getResources().getColor(R.color.white));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));
            navbarTitle.setTextColor(getResources().getColor(R.color.black));
            backButton.setColorFilter(getResources().getColor(R.color.black));
        }
        updateList();
    }

    public void updateList(){
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.

            String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
            /*
            if (!sharedPrefs.getString("prefContactSorting", "0").equals("0")) {
                sortOrder = ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME + " COLLATE LOCALIZED ASC";
            }
            */
            ContentResolver cr = getContentResolver();
            Cursor cursor;
            if (sharedPrefs.getString("prefContactsFilter","1").contains("1")){
                cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI.buildUpon()
                        .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "1")
                        .build(), PROJECTION, "starred=?", new String[] {"1"}, sortOrder);
            } else {
                cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI.buildUpon()
                        .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "1")
                        .build(), PROJECTION, null, null, sortOrder);
            }

            if (cursor != null) {
                try {
                    HashSet<String> normalizedNumbersAlreadyFound = new HashSet<>();
                    contacts = new ArrayList<>();
                    phoneNumbers = new ArrayList<>();
                    photoId = new ArrayList<>();
                    final int contactIdIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID);
                    final int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    final int phoneTypeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                    final int phoneNumIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    final int photoURIIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI);
                    final int lastNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
                    final int givenNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
                    final int normalizedNumIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);

                    long contactId;
                    String displayName, phoneType, phoneNumber,photoURI, lastName, givenName, normalNum;
                    while (cursor.moveToNext()) {
                        contactId = cursor.getLong(contactIdIndex);
                        displayName = cursor.getString(displayNameIndex);
                        phoneType = cursor.getString(phoneTypeIndex);
                        phoneNumber = cursor.getString(phoneNumIndex);
                        photoURI = cursor.getString(photoURIIndex);
                        lastName = cursor.getString(lastNameIndex);
                        givenName = cursor.getString(givenNameIndex);
                        normalNum = cursor.getString(normalizedNumIndex);

                        if (phoneType != null && normalNum != null) {
                            if((phoneType.equals("1")) || phoneType.equals("2") || phoneType.equals("3")) {
                                if (normalizedNumbersAlreadyFound.add(normalNum.replaceAll("\\p{C}", ""))) {
                                    //Log.d(TAG, "Display Name: " + displayName + ", NN: " + normalNum + ", " + phoneType);
                                    contacts.add(displayName + " (" + typeIDtoString(Integer.parseInt(phoneType)) + ")");
                                    phoneNumbers.add(normalNum);
                                    Drawable photo;
                                    if (itsDark) {
                                        photo = getResources().getDrawable(R.drawable.ic_default_contact, getTheme());
                                        photo.setTint(Color.WHITE);
                                    } else {
                                        photo = getResources().getDrawable(R.drawable.ic_default_contact, getTheme());
                                        photo.setTint(Color.BLACK);
                                    }

                                    if (photoURI != null) {

                                        try {
                                            Bitmap photoBitmap = MediaStore.Images.Media
                                                    .getBitmap(getContentResolver(),
                                                            Uri.parse(photoURI));
                                            photo = new BitmapDrawable(getResources(), photoBitmap);

                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    photoId.add(photo);
                                }
                            }
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            ContactListView adapter = new
                    ContactListView(this, contacts, photoId, itsDark);
            contactList = findViewById(R.id.lv_contacts);
            contactList.setAdapter(adapter);
            contactList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {
                    lastPosition = position;
                    // Call Number
                    boolean callPerms = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (getApplication().checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(ContactListActivity.this);
                            builder.setTitle(getString(R.string.call_alert_title));
                            builder.setMessage(getString(R.string.call_alert_body));
                            builder.setPositiveButton(android.R.string.ok, null);
                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @TargetApi(23)
                                public void onDismiss(DialogInterface dialog) {
                                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
                                }
                            });
                            builder.show();
                        } else {
                            // Android version is lesser than 6.0 or the permission is already granted.
                            callPerms = true;
                        }
                    }  else {
                        // Android version is lesser than 6.0 or the permission is already granted.
                        callPerms = true;
                    }
                    if (callPerms) {
                        Intent callHomeIntent = new Intent(Intent.ACTION_CALL);
                        callHomeIntent.setData(Uri.parse("tel:" + phoneNumbers.get(position)));
                        startActivity(callHomeIntent);
                    }
                }

            });
        }
    }
    String typeIDtoString (int typeID){
        String type = "";
        switch (typeID){
            case 0:
                type = getResources().getString(R.string.contact_type_0);
                break;
            case 1:
                type = getResources().getString(R.string.contact_type_1);
                break;
            case 2:
                type = getResources().getString(R.string.contact_type_2);
                break;
            case 3:
                type = getResources().getString(R.string.contact_type_3);
                break;
            case 4:
                type = getResources().getString(R.string.contact_type_4);
                break;
            case 5:
                type = getResources().getString(R.string.contact_type_5);
                break;
            case 6:
                type = getResources().getString(R.string.contact_type_6);
                break;
            case 7:
                type = getResources().getString(R.string.contact_type_7);
                break;
            case 8:
                type = getResources().getString(R.string.contact_type_8);
                break;
            case 9:
                type = getResources().getString(R.string.contact_type_9);
                break;
            case 10:
                type = getResources().getString(R.string.contact_type_10);
                break;
            case 11:
                type = getResources().getString(R.string.contact_type_11);
                break;
            case 12:
                type = getResources().getString(R.string.contact_type_12);
                break;
            case 13:
                type = getResources().getString(R.string.contact_type_13);
                break;
            case 14:
                type = getResources().getString(R.string.contact_type_14);
                break;
            case 15:
                type = getResources().getString(R.string.contact_type_15);
                break;
            case 16:
                type = getResources().getString(R.string.contact_type_16);
                break;
            case 17:
                type = getResources().getString(R.string.contact_type_17);
                break;
            case 18:
                type = getResources().getString(R.string.contact_type_18);
                break;
            case 19:
                type = getResources().getString(R.string.contact_type_19);
                break;
            case 20:
                type = getResources().getString(R.string.contact_type_20);
                break;
        }
        return type;
    }

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.contactlist_title);

        backButton = findViewById(R.id.action_back);
        backButton.setOnClickListener(mClickListener);

        ImageButton forwardButton = findViewById(R.id.action_forward);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    // Go back
                    Intent backIntent = new Intent(ContactListActivity.this, TaskActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Keycode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Intent backIntent = new Intent(ContactListActivity.this, TaskActivity.class);
                startActivity(backIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if ((contactList.getSelectedItemPosition() == (contacts.size() - 1)) && lastPosition == (contacts.size() - 1) ){
                    contactList.setSelection(0);
                }
                lastPosition = contactList.getSelectedItemPosition();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (contactList.getSelectedItemPosition() == 0 && lastPosition == 0){
                    contactList.setSelection(contacts.size() - 1);
                }
                lastPosition = contactList.getSelectedItemPosition();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}

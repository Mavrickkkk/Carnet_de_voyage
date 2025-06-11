package com.example.asi_mobile_toz_gouix;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;


import org.osmdroid.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



public class MainActivity extends AppCompatActivity {

    public static HashMap<String, Object> locationData;

    private static ArrayList<Location> locations;

    private static LocationCallback locationCallback;

    private static FirebaseFirestore db;

   // private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static boolean isRunning;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationData = new HashMap<>();
        locations = new ArrayList<Location>();
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        db = FirebaseFirestore.getInstance();

        Fragment firstFragment = new FirstFragment();
        Fragment secondFragment = new secondFragment();
        Fragment thirdFragment = new ThirdFragment();
        setCurrentFragment(firstFragment);

        bottomNavigationView.setOnItemSelectedListener(item -> {
                    int id = item.getItemId();

                    if (id == R.id.home) {
                        setCurrentFragment(firstFragment);
                        return true;
                    } else if (id == R.id.profile) {
                        setCurrentFragment(secondFragment);
                        return true;
                    } else if (id == R.id.settings) {
                        setCurrentFragment(thirdFragment);
                        return true;
                    }

                    return false;
                }
        );
        // Callback pour mise à jour en direct
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {

                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    locations.add(location);
                    locationData.put(FirstFragment.getUuid().toString(), location);


                    // Envoi Firestore
                    isRunning=true;
                    db.collection(
                            Settings.Secure.getString(getContentResolver(),
                                    Settings.Secure.ANDROID_ID)).add(locationData);


                }
            }

            ;
        };

    }

    @Override
    public void onResume() {
        super.onResume();
        // map.onResume(); // maintenant dans le fragment
    }

    @Override
    public void onPause() {
        super.onPause();
        // map.onPause(); // maintenant dans le fragment
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    private boolean getRunning(){
        return isRunning;
    }

    static void setRunning(boolean newIsRunning){
        isRunning = newIsRunning;
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }

    public static LocationCallback getLocationCallback() {
        return locationCallback;
    }

    public static FirebaseFirestore getDb() {
        return db;
    }

    public static List<Location> getLocations(){return locations;}

    public static void setLocations(){locations = null;}
}

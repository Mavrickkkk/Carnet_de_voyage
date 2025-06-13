package com.example.asi_mobile_toz_gouix;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;


import org.osmdroid.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    public static HashMap<String, Object> locationData;

    private static ArrayList<Location> locations;

    private static LocationCallback locationCallback;

    private static FirebaseFirestore db;

    // private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static boolean isRunning;
    private static boolean infoSaved;
    private static UUID uuid;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);
        locationData = new HashMap<>();
        locations = new ArrayList<Location>();
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        db = FirebaseFirestore.getInstance();

        Fragment firstFragment = new FirstFragment();
        Fragment secondFragment = new SecondFragment();
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
                    Log.d("LocationCallback", "UUID: " + uuid.toString() + " | Location: " + location.toString());
                    saveLocationData(location);
                }
            }
        };

    }

    public void startTracking() {
        long time = 10000;

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                (long) time// priorité
                // intervalle en millisecondes
        ).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MainActivity", "Permission non accordée pour la localisation");
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("MainActivity", "Suivi de localisation démarré");
                        isRunning = true;
                    } else {
                        Log.e("MainActivity", "Échec du démarrage du suivi");
                        isRunning = false;
                    }
                });
    }

    public void stopTracking() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isRunning = false;
    }

    private void saveLocationData(Location location) {
        locations.add(location); //Liste servant à l'envoi du fichier gpx par mail
        String trajetId = uuid.toString();
        locationData.put(uuid.toString(), location); //Hashmap servant à remplir la base avec une clé valeur
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        if (!infoSaved) {
            createTrajetDocument(deviceId, trajetId);
            infoSaved = true;
        }
        db.collection(deviceId)
                .document(trajetId)
                .collection("localisations")
                .add(locationData)
                .addOnSuccessListener(documentReference ->
                Log.d("BDD", "Position ajoutée à " + trajetId)
                )
                .addOnFailureListener(e ->
                        Log.e("BDD", "Erreur lors de l'enregistrement : ", e)
                );
    }

    private void createTrajetDocument(String deviceId, String trajetId) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("created_at", System.currentTimeMillis());

        db.collection(deviceId)
                .document(trajetId)
                .set(meta)
                .addOnSuccessListener(aVoid -> Log.d("FIRESTORE", "Document trajetId créé"))
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Erreur création trajetId", e));
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

    public static UUID getTrackingUUID() {
        return uuid;
    }
    public static void generateNewUuid() {
        uuid = UUID.randomUUID();
        infoSaved = false;
    }

    public static boolean getRunning() {
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
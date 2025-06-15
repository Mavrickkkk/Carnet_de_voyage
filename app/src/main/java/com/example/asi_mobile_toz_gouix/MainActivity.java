package com.example.asi_mobile_toz_gouix;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import org.osmdroid.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static ArrayList<Location> locations;
    private static LocationCallback locationCallback;
    private static FirebaseFirestore db;
    private static boolean isRunning;
    private static boolean infoSaved;
    private static UUID uuid;
    private static long time = 10000; // Temps entre chaque envoi de localisation en ms (10s à la base)


    //Cœur de l'application

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);
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
                    Log.d("ÉTAPE 5", "Location Callback " + " | Location: " + location);
                    saveLocationData(location);
                }
            }
        };

    }


    //Tracking GPS

    public void startTracking() {

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                time// priorité
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
                        Log.d("ETAPE 4", "Suivi de localisation démarré");
                        isRunning = true;
                    } else {
                        Log.e("ERREUR", "Échec du démarrage du suivi");
                        isRunning = false;
                    }
                });
    }

    /**
     * Arrête le suivi de localisation
     */
    public void stopTracking() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isRunning = false;
    }


    //Interactif avec Firestore

    /**
     * Enregistre les données de localisation dans Firestore
     * avec device Id -> trajetIdLong qui fait référence à un Long
     *
     * @param location
     */
    private void saveLocationData(Location location) {
        registerDeviceIfNeeded();
        if(location != null)
            locations.add(location);

        String trajetId = uuid.toString();
        String deviceName = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("deviceName", null);
        if (deviceName == null) {
            Log.e("ERREUR", "Device name non trouvé, annulation de l'enregistrement");
            return;
        }

        if (!infoSaved) {
            createTrajetDocument(deviceName, trajetId);
            infoSaved = true;
        }
        db.collection("devices")
                .document(deviceName)
                .collection("trajets")
                .document(trajetId)
                .collection("localisations")
                .add(location)
                .addOnSuccessListener(documentReference ->
                        Log.d("ETAPE 7", "Position ajoutée à " + trajetId))
                .addOnFailureListener(e ->
                        Log.e("ERREUR", "Erreur lors de l'enregistrement", e));
    }

    /**
     * Enregistre le device si il n'est pas déjà enregistré
     */
    private void registerDeviceIfNeeded() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        db.collection("devices")
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        db.collection("devices")
                                .get()
                                .addOnSuccessListener(allDevices -> {
                                    int count = allDevices.size() + 1;
                                    String deviceName = "Téléphone " + count;

                                    Map<String, Object> deviceInfo = new HashMap<>();
                                    deviceInfo.put("deviceId", deviceId);
                                    deviceInfo.put("registeredAt", System.currentTimeMillis());

                                    db.collection("devices").document(deviceName)
                                            .set(deviceInfo)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("ETAPE BONUS", "Device enregistré : " + deviceName);
                                                getSharedPreferences("prefs", MODE_PRIVATE)
                                                        .edit()
                                                        .putString("deviceName", deviceName)
                                                        .apply();
                                            })
                                            .addOnFailureListener(e -> Log.e("ERREUR", "Erreur d'enregistrement du device", e));
                                });
                    } else {
                        DocumentSnapshot existingDoc = querySnapshot.getDocuments().get(0);
                        String deviceName = existingDoc.getId();

                        Log.d("ETAPE BONUS", "Device déjà enregistré : " + deviceName);
                        getSharedPreferences("prefs", MODE_PRIVATE)
                                .edit()
                                .putString("deviceName", deviceName)
                                .apply();
                    }
                })
                .addOnFailureListener(e -> Log.e("ERREUR", "Erreur lors de la vérification du device", e));
    }

    /**
     * Crée le document trajetId dans la collection des devices
     *
     * @param deviceName
     * @param trajetId
     */
    private void createTrajetDocument(String deviceName, String trajetId) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("created_at", System.currentTimeMillis());

        db.collection("devices")
                .document(deviceName)
                .collection("trajets")
                .document(trajetId)
                .set(meta)
                .addOnSuccessListener(aVoid -> Log.d("ETAPE 6", "Document trajetId créé"))
                .addOnFailureListener(e -> Log.e("ERREUR", "Erreur création trajetId", e));
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    /**
     * Génère un nouveau UUID
     */
    public static void generateNewUuid() {
        uuid = UUID.randomUUID();
        infoSaved = false;
    }


    //Getters

    /**
     * Retourne l'UUID du suivi de localisation
     *
     * @return uuid
     */
    public static UUID getTrackingUUID() {
        return uuid;
    }

    /**
     * Retourne l'état du suivi de localisation
     *
     * @return boolean
     */
    public static boolean getRunning() {
        return isRunning;
    }

    /**
     * Retourne la base de données Firestore
     *
     * @return db
     */
    public static FirebaseFirestore getDb() {
        return db;
    }

    /**
     * Retourne la liste des localisations
     *
     * @return locations
     */
    public static List<Location> getLocations() {
        return locations;
    }


    //Setters

    /**
     * Met à jour l'état du suivi de localisation
     *
     * @param newIsRunning
     */
    static void setRunning(boolean newIsRunning) {
        isRunning = newIsRunning;
    }

    /**
     * Met à jour le Fragment actuel
     *
     * @param fragment
     */
    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }

    /**
     * Met à jour la localisation
     */
    public static void setLocations() {
        locations = null;
    }

    /**
     * Fonction qui sert à modifier la durée du suivi de localisation en millisecondes (voir ThirdFragment)
     *
     * @param timeNew
     */
    public static void setTime(
            long timeNew) {
        time = (long) timeNew;
    }
}
package com.example.asi_mobile_toz_gouix;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
// import android.location.Location; // plus utilisé ici
// import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
// import androidx.core.app.ActivityCompat;
// import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
// import com.google.android.gms.location.FusedLocationProviderClient;
// import com.google.android.gms.location.LocationServices;
// import com.google.android.gms.location.LocationCallback;
// import com.google.android.gms.location.LocationResult;
// import com.google.android.gms.location.LocationRequest;
// import com.google.android.gms.location.Priority;
// import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
// import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
// import org.osmdroid.util.GeoPoint;
// import org.osmdroid.views.MapView;
// import org.osmdroid.views.overlay.Marker;


public class MainActivity extends AppCompatActivity {
    private static LocationCallback locationCallback;

    private static FirebaseFirestore db;

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static boolean isRunning;

    // La carte est maintenant dans le FirstFragment
    // private MapView map;

    // Déplacé dans FirstFragment
    // private FusedLocationProviderClient fusedLocationClient;
    // private GeoPoint userGeoPoint;
    // private LocationCallback locationCallback;
    // private static float time;
    // private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                    // Affichage sur carte
//                        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
//                        mapView.getOverlays().clear();
//                        Marker marker = new Marker(mapView);
//                        marker.setPosition(point);
//                        marker.setTitle("Ma position");
//                        mapView.getOverlays().add(marker);
//                        mapView.getController().setCenter(point);

                    // Envoi Firestore
                    isRunning=true;
                    db.collection(
                            Settings.Secure.getString(getContentResolver(),
                                    Settings.Secure.ANDROID_ID)).add(location);
//                                .addOnSuccessListener(documentReference -> Toast.makeText(getContext(),
//                                        "Position enregistrée", Toast.LENGTH_SHORT).show())
//                                .addOnFailureListener(e -> Toast.makeText(this,
//                                        "Erreur Firestore", Toast.LENGTH_SHORT).show());

                }
            }

            ;
        };

        // La carte est maintenant gérée dans FirstFragment
        // map = findViewById(R.id.map);
        // map.setTileSource(TileSourceFactory.MAPNIK);
        // map.setMultiTouchControls(true);

        // La localisation et Firestore sont déplacées
        // fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // requestPermissionsIfNecessary(new String[] {
        //         Manifest.permission.ACCESS_FINE_LOCATION,
        //         Manifest.permission.WRITE_EXTERNAL_STORAGE
        // });

        // this.db = FirebaseFirestore.getInstance();
        // Toast.makeText(this, "Initalisé" + Objects.nonNull(this.db), Toast.LENGTH_SHORT).show();

        // if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        //         == PackageManager.PERMISSION_GRANTED) {
        //     displayUserLocation();

        //     locationCallback = new LocationCallback(){
        //         @Override
        //         public void onLocationResult(LocationResult locationResult) {
        //             if (locationResult == null) return;
        //             Location location = locationResult.getLastLocation();

        //             if (location != null) {
        //                 Object parcelable = (Parcelable) location;
        //                 db.collection("CarnetDeVoyage").add(parcelable);
        //                 String message = "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude();
        //                 Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        //             }
        //         }
        //     };
        // }
    }

    // private void displayUserLocation() {
    //     if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    //             == PackageManager.PERMISSION_GRANTED) {
    //         fusedLocationClient.getLastLocation()
    //                 .addOnSuccessListener(this, location -> {
    //                     if (location != null) {
    //                         userGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
    //                         map.getController().setZoom(16.0);
    //                         map.getController().setCenter(userGeoPoint);

    //                         map.getOverlays().clear();
    //                         Marker marker = new Marker(map);
    //                         marker.setPosition(userGeoPoint);
    //                         marker.setTitle("Ma position");
    //                         map.getOverlays().add(marker);
    //                     } else {
    //                         Toast.makeText(this, "Position non trouvée. Activez la localisation GPS.", Toast.LENGTH_SHORT).show();
    //                     }
    //                 });
    //     }
    // }

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

        // Plus nécessaire ici : géré dans le fragment
        // ArrayList<String> permissionsToRequest = new ArrayList<>();
        // for (int i = 0; i < grantResults.length; i++) {
        //     permissionsToRequest.add(permissions[i]);
        // }
        // if (permissionsToRequest.toArray().length > 0) {
        //     ActivityCompat.requestPermissions(
        //             this,
        //             permissionsToRequest.toArray(new String[0]),
        //             REQUEST_PERMISSIONS_REQUEST_CODE);
        // }
    }

    // private void requestPermissionsIfNecessary(String[] permissions) {
    //     ArrayList<String> permissionsToRequest = new ArrayList<>();
    //     for (String permission : permissions) {
    //         if (ContextCompat.checkSelfPermission(this, permission)
    //                 != PackageManager.PERMISSION_GRANTED) {
    //             permissionsToRequest.add(permission);
    //         }
    //     }
    //     if (!permissionsToRequest.isEmpty()) {
    //         ActivityCompat.requestPermissions(
    //                 this,
    //                 permissionsToRequest.toArray(new String[0]),
    //                 REQUEST_PERMISSIONS_REQUEST_CODE);
    //     }
    // }

    // Plus utilisé dans cette classe
    // public void onClickDemarrer(View view) { ... }

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
}

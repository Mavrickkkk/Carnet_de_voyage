package com.example.asi_mobile_toz_gouix;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.UUID;

public class FirstFragment extends Fragment {
    private UUID uuid;
    private HashMap<String, Object> locationData;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1001;
    private boolean isTracking = false;
    private Button btnDemarrer;

    private static float time = 10000; // valeur par défaut, modifiable via setTime()

    public static void setTime(float timeGiven) {
        time = timeGiven;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);

        Context context = requireContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        // Initialisation carte
        mapView = view.findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Initialisation Firestore et Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Bouton
        btnDemarrer = view.findViewById(R.id.Btn_Demarrer);
        btnDemarrer.setOnClickListener(v -> onClickDemarrer(view));

        // Callback pour mise à jour en direct
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null ) return;

                Location location = locationResult.getLastLocation();
                if (location != null && isAdded()) {
                    locationData.put(uuid.toString(), location);
                    // Affichage sur carte
                    GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getOverlays().clear();
                    Marker marker = new Marker(mapView);
                    marker.setPosition(point);
                    marker.setTitle("Ma position");
                    mapView.getOverlays().add(marker);
                    mapView.getController().setCenter(point);

                    // Envoi Firestore
                    MainActivity.getDb().collection(Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.ANDROID_ID)).add(locationData);
                            /*.addOnSuccessListener(documentReference -> Toast.makeText(getContext(),
                                    "Position enregistrée", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(),
                                    "Erreur Firestore", Toast.LENGTH_SHORT).show());*/
                }
            }
        };

        // Permissions
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            displayUserLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }

        return view;
    }

    private void displayUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                            mapView.getController().setZoom(16.0);
                            mapView.getController().setCenter(point);

                            Marker marker = new Marker(mapView);
                            marker.setPosition(point);
                            marker.setTitle("Ma position");
                            mapView.getOverlays().add(marker);
                        } else {
                            Toast.makeText(this.getContext(), "Position non trouvée", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public void onClickDemarrer(View view) {
        time = 10000; // a modifier plus tard
        uuid = UUID.randomUUID();
        locationData = new HashMap<>();
        locationData.put(uuid.toString(),0);
        Log.d("UUID", uuid.toString());
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                (long) time// priorité
                // intervalle en millisecondes
        )
                .setMinUpdateIntervalMillis((long) time) // équivalent de setFastestInterval
                .build();
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Button Btn_Demarrer = view.findViewById(R.id.Btn_Demarrer);
        boolean isRunning = Btn_Demarrer.getText().toString().equals("Arrêter");

        if (isRunning) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            uuid = UUID.randomUUID();

            Btn_Demarrer.setText("Démarrer");
        }
        else {
            fusedLocationClient.requestLocationUpdates(locationRequest, MainActivity.getLocationCallback(), null).addOnCompleteListener(l ->{
                if(l.isSuccessful())
                    Btn_Demarrer.setText("Arrêter");
                else{
                    MainActivity.setRunning(false);
                }
            });
        }
    }
   /* private void onClickDemarrer() {
        if (!isTracking) {
            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, (long) time)
                    .setMinUpdateIntervalMillis(2000)
                    .build();

            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permission manquante", Toast.LENGTH_SHORT).show();
                return;
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            btnDemarrer.setText("Arrêter");
            isTracking = true;

        } else {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            btnDemarrer.setText("Démarrer");
            isTracking = false;
        }
    }*/

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            btnDemarrer.setText("Démarrer");
            isTracking = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                displayUserLocation();
            } else {
                Toast.makeText(getContext(), "Permission refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public static void setTime(long timeNew){time = (long)timeNew;}
}

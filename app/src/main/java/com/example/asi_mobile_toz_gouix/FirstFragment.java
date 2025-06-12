package com.example.asi_mobile_toz_gouix;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class FirstFragment extends Fragment {
    private Marker marker;
    private Button btnExportGpx;
    private EditText emailField;
    private static UUID uuid;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1001;
    private boolean isTracking = false;
    private Button btnDemarrer;

    private static float time = 10000; // valeur par défaut, modifiable via setTime()

    public static void setTime(float timeGiven) {
        time = timeGiven;
    }

    /**
     * Fonction qui sert à initialiser le fragment.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);

        Context context = requireContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        emailField = view.findViewById(R.id.emailField);
        btnExportGpx = view.findViewById(R.id.btnExportGpx);

        btnExportGpx.setOnClickListener(v -> exporterEtEnvoyerGPX());

        // Initialisation carte
        mapView = view.findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Initialisation Firestore et Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Bouton
        btnDemarrer = view.findViewById(R.id.Btn_Demarrer);
        btnDemarrer.setOnClickListener(v -> toggleTracking());
        //btnDemarrer.setOnClickListener(v -> onClickDemarrer(view));

        //Marker de l'utilisateur qui va se déplacer
        marker = new Marker(this.mapView);

        //Activité principale
        MainActivity activity = (MainActivity) requireActivity();

        // Permissions
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            displayUserLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }

        if (MainActivity.getRunning())
            btnDemarrer.setText("Arrêter");
        else
            btnDemarrer.setText("Démarrer");

        return view;
    }

    /**
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Mise a jour de la carte
        Handler monHandler = new Handler();
        Runnable monRunnable = new Runnable() {
            @Override
            public void run() {
                monHandler.postDelayed(this, 1000);
                showPosition(false);
            }
        };
        monHandler.post(monRunnable);
    }

    /**
     * Fonction qui sert à afficher la position de l'utilisateur sur la carte, la première fois quand la carte se charge.
     */
    private void displayUserLocation() {
            showPosition(true);
    }

    /*
     * Fonction qui sert à modifier l'état de suivi de localisation
     * Modifie l'état du bouton selon la variable isRunning
     */
    private void toggleTracking() {
        MainActivity activity = (MainActivity) requireActivity();
        if (MainActivity.getTrackingUUID() == null) {
            MainActivity.generateNewUuid();
        }
        if (activity.getRunning()) {
            btnExportGpx.setEnabled(true);
            activity.stopTracking();
            btnDemarrer.setText("Démarrer");
        } else {
            btnExportGpx.setEnabled(false);
            activity.startTracking();
            btnDemarrer.setText("Arrêter");
        }
        activity.setRunning(!activity.getRunning());
    }

    /**
     * Fonction qui sert à reprendre la carte
     */
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * Fonction qui sert à mettre en pause la carte
     */
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (isTracking) {
            toggleTracking();
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

    /**
     * Fonction qui sert à modifier la durée du suivi de localisation en millisecondes (voir ThirdFragment)
     * @param timeNew
     */
    public static void setTime(long timeNew){time = (long)timeNew;}

    /**
     * Fonction qui sert à exporter le fichier GPX et l'envoyer par mail
     */
    private void exporterEtEnvoyerGPX() {
        if (MainActivity.locationData == null || MainActivity.locationData.isEmpty()) {
            Toast.makeText(getContext(), "Aucun trajet à exporter", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = emailField.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(getContext(), "Veuillez entrer une adresse email", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File gpxFile = new File(requireContext().getCacheDir(), "trajet.gpx");
            FileWriter writer = new FileWriter(gpxFile);
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<gpx version=\"1.1\" creator=\"ASI_Mobile\">\n");
            writer.write("<trk><name>Trajet</name><trkseg>\n");

            for (Location obj : MainActivity.getLocations()) {
                if (obj != null) {
                    writer.write(String.format(Locale.US,
                            "<trkpt lat=\"%f\" lon=\"%f\"><ele>%f</ele></trkpt>\n",
                            obj.getLatitude(), obj.getLongitude(), obj.getAltitude()));
                }
            }

            writer.write("</trkseg></trk>\n</gpx>");
            writer.close();

            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", gpxFile);

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("application/gpx+xml");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Trajet GPX");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Voici le fichier GPX de votre trajet.");
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(emailIntent, "Envoyer le fichier GPX"));

        } catch (IOException e) {
            Toast.makeText(getContext(), "Erreur lors de l'export", Toast.LENGTH_SHORT).show();
            Log.e("GPX", "Erreur export GPX", e);
        }
        MainActivity.setLocations();  // met à null la liste de localisations. Pour envoi de mail.
    }

    public static UUID getUuid(){return uuid;}

    /**
     * Fonction qui sert a visualiser la position de l'utilisateur sur la carte.
     * @param first la seul fois ou il faut centré la carte au premier affichage
     */
    public void showPosition(boolean first){
        if(!isAdded()) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                            if (first) {
                                this.mapView.getController().setZoom(16.0);
                                this.mapView.getController().setCenter(point);
                                marker.setPosition(point);
                                marker.setTitle("Ma position");
                                this.mapView.getOverlays().add(marker);
                            }
                            else {
                                marker.setPosition(point);
                                marker.setTitle("Ma position");
                                this.mapView.getOverlays().add(marker);
                            }
                        } else {
                            Toast.makeText(this.getContext(), "Position non trouvée", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
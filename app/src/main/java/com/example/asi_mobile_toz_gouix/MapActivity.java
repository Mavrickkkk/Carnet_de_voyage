package com.example.asi_mobile_toz_gouix;

import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import org.osmdroid.views.MapView;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private MapView map;

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        Long trajetId = getIntent().getLongExtra("trajetIdLong",0);



        if (trajetId == null) {
            Log.e("MapActivity", "trajetId absent dans l'Intent");
            return;
        }

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        MainActivity.getDb()
                .collection("devices")
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnSuccessListener(deviceSnapshot -> {
                    if (deviceSnapshot.isEmpty()) {
                        Log.e("MapActivity", "Aucun device trouvé pour deviceId = " + deviceId);
                        return;
                    }

                    DocumentSnapshot deviceDoc = deviceSnapshot.getDocuments().get(0);

                    deviceDoc.getReference()
                            .collection("trajets")
                            .whereEqualTo("created_at", trajetId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (querySnapshot.isEmpty()) {
                                    Log.e("MapActivity", "Aucun trajet trouvé avec created_at = " + trajetId);
                                    return;
                                }

                                DocumentSnapshot trajetDoc = querySnapshot.getDocuments().get(0);

                                trajetDoc.getReference()
                                        .collection("localisations")
                                        .get()
                                        .addOnSuccessListener(pointSnapshots -> {
                                            List<GeoPoint> geoPoints = new ArrayList<>();

                                            for (DocumentSnapshot pointDoc : pointSnapshots.getDocuments()) {
                                                Double lat = pointDoc.getDouble("latitude");
                                                Double lon = pointDoc.getDouble("longitude");

                                                if (lat != null && lon != null) {
                                                    geoPoints.add(new GeoPoint(lat, lon));
                                                }
                                            }

                                            if (geoPoints.isEmpty()) {
                                                Toast.makeText(this, "Aucun point trouvé pour ce trajet.", Toast.LENGTH_LONG).show();
                                            } else {
                                                drawPolyline(geoPoints);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("MapActivity", "Erreur récupération des localisations", e);
                                        });

                            })
                            .addOnFailureListener(e -> {
                                Log.e("MapActivity", "Erreur lors de la requête sur les trajets", e);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("MapActivity", "Erreur lors de la requête sur les devices", e);
                });

    }

    /**
     * Dessine une ligne sur la carte avec les points fournis
     * @param geoPoints
     */
    private void drawPolyline(List<GeoPoint> geoPoints) {
        IMapController mapController = map.getController();
        mapController.setZoom(16.0);
        mapController.setCenter(geoPoints.get(0));

        Polyline polyline = new Polyline();
        polyline.setPoints(geoPoints);
        polyline.getOutlinePaint().setColor(Color.BLACK);
        polyline.getOutlinePaint().setStrokeWidth(6f);
        map.getOverlays().add(polyline);

        Marker startMarker = new Marker(map);
        startMarker.setPosition(geoPoints.get(0));
        startMarker.setTitle("Début du trajet");
        map.getOverlays().add(startMarker);

        map.invalidate();
        Log.d("MapActivity", "Ligne tracée avec " + geoPoints.size() + " points");
    }
}

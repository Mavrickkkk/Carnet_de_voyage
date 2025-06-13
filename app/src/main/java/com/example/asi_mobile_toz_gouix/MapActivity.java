package com.example.asi_mobile_toz_gouix;

import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;

import org.osmdroid.views.MapView;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        String uuidToShow = getIntent().getStringExtra("uuid");
        if (uuidToShow == null) {
            Log.e("MapActivity", "UUID absent dans l'Intent");
            return;
        }

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        MainActivity.getDb()
                .collection(deviceId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Regrouper les points par UUID
                    Map<String, List<GeoPoint>> uuidToPoints = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;

                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            String uuid = entry.getKey();
                            Object value = entry.getValue();

                            if (!(value instanceof Map)) continue;
                            Map<String, Object> locMap = (Map<String, Object>) value;

                            Object latObj = locMap.get("mLatitude");
                            Object lonObj = locMap.get("mLongitude");
                            if (latObj == null) latObj = locMap.get("latitude");
                            if (lonObj == null) lonObj = locMap.get("longitude");

                            if (latObj instanceof Number && lonObj instanceof Number) {
                                double lat = ((Number) latObj).doubleValue();
                                double lon = ((Number) lonObj).doubleValue();
                                GeoPoint point = new GeoPoint(lat, lon);

                                if (!uuidToPoints.containsKey(uuid)) {
                                    uuidToPoints.put(uuid, new ArrayList<>());
                                }
                                uuidToPoints.get(uuid).add(point);
                            }
                        }
                    }

                    List<GeoPoint> geoPoints = uuidToPoints.get(uuidToShow);
                    if (geoPoints == null || geoPoints.isEmpty()) {
                        Toast.makeText(this, "Aucun point trouvé pour ce trajet.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    drawPolyline(geoPoints);
                })
                .addOnFailureListener(e -> {
                    Log.e("MapActivity", "Erreur Firestore", e);
                    Toast.makeText(this, "Erreur lors de la récupération des données.", Toast.LENGTH_LONG).show();
                });
    }

    private void drawPolyline(List<GeoPoint> geoPoints) {
        IMapController mapController = map.getController();
        mapController.setZoom(16.0);
        mapController.setCenter(geoPoints.get(0));

        Polyline polyline = new Polyline();
        polyline.setPoints(geoPoints);
        polyline.setColor(Color.BLUE);
        polyline.setWidth(6f);
        map.getOverlays().add(polyline);

        Marker startMarker = new Marker(map);
        startMarker.setPosition(geoPoints.get(0));
        startMarker.setTitle("Début du trajet");
        map.getOverlays().add(startMarker);

        map.invalidate();
        Log.d("MapActivity", "Ligne tracée avec " + geoPoints.size() + " points");
    }
}




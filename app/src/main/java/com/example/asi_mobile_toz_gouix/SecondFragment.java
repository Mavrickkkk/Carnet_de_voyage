package com.example.asi_mobile_toz_gouix;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SecondFragment extends Fragment {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> trajetIdList = new ArrayList<>();
    private ArrayList<Long> trajetIdListLong = new ArrayList<>();
    private Spinner deviceSelector;

    /**
     * Appeler quand le Fragment est appelé
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

        View view = inflater.inflate(R.layout.fragment_second, container, false);
        listView = view.findViewById(R.id.listeTrajets);
        deviceSelector = view.findViewById(R.id.deviceSelector);
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, trajetIdList);
        listView.setAdapter(adapter);

        MainActivity.getDb()
                .collection("devices")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ArrayList<String> deviceNames = new ArrayList<>();
                    String currentDeviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID); //récupère l'id du device.
                    String defaultDeviceName = null;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        deviceNames.add(doc.getId());
                        String deviceIdInDb = doc.getString("deviceId");
                        if (deviceIdInDb != null && deviceIdInDb.equals(currentDeviceId)) {
                            defaultDeviceName = doc.getId();
                        }
                    }

                    ArrayAdapter<String> deviceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, deviceNames);
                    deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    deviceSelector.setAdapter(deviceAdapter);

                    // Sélectionner automatiquement l'appareil actuel
                    if (defaultDeviceName != null) {
                        int position = deviceNames.indexOf(defaultDeviceName);
                        deviceSelector.setSelection(position);
                        loadTrajetsForDevice(defaultDeviceName);
                    }

                })
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Erreur récupération des devices", e));

        // Quand l'utilisateur sélectionne un appareil
        deviceSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDeviceName = parent.getItemAtPosition(position).toString();
                loadTrajetsForDevice(selectedDeviceName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Quand on clique sur un trajet
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedTrajetId = trajetIdList.get(position);
            Intent intent = new Intent(getActivity(), MapActivity.class); //Appel la classe MapActivity qui gère l'affichage du trajet.
            intent.putExtra("trajetId", selectedTrajetId);
            intent.putExtra("trajetIdLong", trajetIdListLong.get(position)); // permet de récupérer le timeStamp de manoère propre
            startActivity(intent);
        });

        return view;
    }

    /**
     * Convertir timeStamp en date pour l'affichage dans la liste des trajets
     * @param timestamp
     * @return
     */
    public static String timestampToDate(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy 'à' HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Charge les trajets pour un device donné
     * @param deviceName
     */
    private void loadTrajetsForDevice(@NonNull String deviceName) {
        MainActivity.getDb()//récupère la db initialisé dans le MainActivity
                .collection("devices")
                .document(deviceName)
                .collection("trajets")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    trajetIdList.clear();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Object createdAtObj = document.get("created_at");
                        if (createdAtObj instanceof Number) {
                            long timestamp = ((Number) createdAtObj).longValue();
                            trajetIdList.add(timestampToDate(timestamp));
                            trajetIdListLong.add(timestamp);
                        } else {
                            trajetIdList.add("Pas de date ?!");
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e("FIRESTORE", "Erreur lors du chargement des trajets pour le device : " + deviceName, e)
                );
    }
}

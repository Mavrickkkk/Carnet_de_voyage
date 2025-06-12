package com.example.asi_mobile_toz_gouix;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.asi_mobile_toz_gouix.MainActivity;
import com.example.asi_mobile_toz_gouix.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class secondFragment extends Fragment {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> uuidList = new ArrayList<>();
    private HashMap<String, Location> uuidToLocation = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_second, container, false);
        listView = view.findViewById(R.id.listeTrajets);
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, uuidList);
        listView.setAdapter(adapter);

        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        MainActivity.getDb()
                .collection(deviceId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Map<String, Object> data = document.getData();
                        if (data == null) continue;

                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            String uuid = entry.getKey();
                            Map<String, Object> locationMap = (Map<String, Object>) entry.getValue();

                            Location location = new Location("");
                            location.setLatitude((double) locationMap.get("latitude"));
                            location.setLongitude((double) locationMap.get("longitude"));

                            uuidList.add(uuid);
                            uuidToLocation.put(uuid, location);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Erreur lors de la récupération", e));

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedUuid = uuidList.get(position);
            Location location = uuidToLocation.get(selectedUuid);

            if (location != null) {
                Intent intent = new Intent(getActivity(), MapActivity.class);
                intent.putExtra("latitude", location.getLatitude());
                intent.putExtra("longitude", location.getLongitude());
                startActivity(intent);
            }
        });

        return view;
    }
}

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_second, container, false);
        listView = view.findViewById(R.id.listeTrajets);
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, trajetIdList);
        listView.setAdapter(adapter);

        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        MainActivity.getDb()
                .collection(deviceId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    trajetIdList.clear();
                    Log.d("FIRESTORE", "Trajets trouvés : " + querySnapshot.size());
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String trajetId = document.getId();
                        Object createdAtObj = document.get("created_at");
                        if (createdAtObj != null && createdAtObj instanceof Number) {
                            long timestamp = ((Number) createdAtObj).longValue();
                            String formattedDate = timestampToDate(timestamp);
                            trajetIdList.add(formattedDate);
                        } else {
                            trajetIdList.add("Pas de date ?!");
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Erreur lors de la récupération des trajets", e));

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedTrajetId = trajetIdList.get(position);

            Intent intent = new Intent(getActivity(), MapActivity.class);
            intent.putExtra("trajetId", selectedTrajetId);
            startActivity(intent);
        });


        return view;
    }

    public static String timestampToDate(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy 'à' HH:mm", Locale.getDefault());
        return sdf.format(date);
    }
}

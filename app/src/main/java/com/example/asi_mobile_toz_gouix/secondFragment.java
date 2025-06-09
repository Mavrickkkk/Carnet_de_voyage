package com.example.asi_mobile_toz_gouix;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Map;

public class secondFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Location[] locations = new Location[20];
        MainActivity.getDb()
                .collection(Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        // Traite chaque document ici
                        // Exemple : récupération des données
                        Map<String, Object> data = document.getData();
                        Log.d("FIRESTORE", "Document: " + data.toString());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Erreur lors de la récupération", e);
                });
        return inflater.inflate(R.layout.fragment_second, container, false);

    }
}
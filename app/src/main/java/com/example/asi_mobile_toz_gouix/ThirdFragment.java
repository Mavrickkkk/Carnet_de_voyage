package com.example.asi_mobile_toz_gouix;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class ThirdFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_third, container, false);

        SeekBar seekBar =(SeekBar) view.findViewById(R.id.SliderTime);
        TextView sliderValueText = view.findViewById(R.id.sliderValueText);

        seekBar.setMax(40000);
        seekBar.setMin(1000);
        seekBar.setProgress(10000);

        sliderValueText.setText("Durée : 10000 ms");

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = Math.max(progress, 1000);
                sliderValueText.setText("Durée : " + value + " ms");
                MainActivity.setTime(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        return view;

    }

    }
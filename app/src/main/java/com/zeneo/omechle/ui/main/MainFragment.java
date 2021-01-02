package com.zeneo.omechle.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.zeneo.omechle.R;

public class MainFragment extends Fragment {

    private AdView adView;
    private AdView smallAdView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        adView = new AdView(context, "IMG_16_9_APP_INSTALL#YOUR_PLACEMENT_ID", AdSize.RECTANGLE_HEIGHT_250);
        smallAdView = new AdView(context, "IMG_16_9_APP_INSTALL#YOUR_PLACEMENT_ID", AdSize.BANNER_HEIGHT_90);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout adContainer = view.findViewById(R.id.banner_container);
        LinearLayout smallAdContainer = view.findViewById(R.id.small_banner_container);

        // Add the ad view to your activity layout
        adContainer.addView(adView);
        smallAdContainer.addView(smallAdView);

        // Request an ad
        adView.loadAd();
        smallAdView.loadAd();

        view.findViewById(R.id.text_button).setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(MainFragment.this);
            navController.navigate(R.id.action_mainFragment_to_textFragment);
        });
        view.findViewById(R.id.video_button).setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(MainFragment.this);
            navController.navigate(R.id.action_mainFragment_to_videoFragment);
        });
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
}
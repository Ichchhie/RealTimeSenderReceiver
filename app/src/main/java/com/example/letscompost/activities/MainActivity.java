package com.example.letscompost.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;

import com.example.letscompost.BuildConfig;
import com.example.letscompost.R;
import com.example.letscompost.realtime.RealTimeLocationReceiverActivity;
import com.example.letscompost.realtime.RealTimeLocationSenderActivity;
import com.example.letscompost.utils.Constants;
import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    String title = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initToolbar();
        initViews();
    }

    private void initFlavors() {
        if (BuildConfig.FLAVOR.equals(Constants.FLAVOR_REALTIME_SENDER)) {
            startActivity(new Intent(this, RealTimeLocationSenderActivity.class));
        }
        else if (BuildConfig.FLAVOR.equals(Constants.FLAVOR_REALTIME_RECEIVER)) {
            startActivity(new Intent(this, RealTimeLocationReceiverActivity.class));
        }
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name));
    }

    private void initViews() {
        title = getString(R.string.app_name);
        initFlavors();
    }


}
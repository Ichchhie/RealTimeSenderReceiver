package com.example.letscompost.realtime;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.baato.baatolibrary.models.LatLon;
import com.baato.baatolibrary.models.Place;
import com.baato.baatolibrary.models.PlaceAPIResponse;
import com.baato.baatolibrary.services.BaatoReverse;
import com.example.letscompost.R;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import java.util.Calendar;

/**
 * Click and put a marker at a specific location and then perform
 * reverse geocoding to retrieve and display the location's address
 */
public class RealTimeLocationSenderActivity extends AppCompatActivity implements LocationEngineListener {
    private MapView mapView;
    private TextView bottomInfoLayout,topInfoLayout;
    private MapboxMap mapboxMap;
    private Icon selectedMarkerIcon;
    private MarkerOptions marker;
    private String baato_access_token = "";
    DatabaseReference rootRef, usersRef;
    private LocationEngine locationEngine;
    private Location lastLocation, locationToSend;
    private CoordinatorLayout rootLayout;
    private static final int ONE_SECOND_INTERVAL = 1000;
    private static final int REQUEST_CODE_PERMISSION = 2;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    Handler handler;
    private Runnable runnableCode;
    AlertDialog dialog;
    String tracKName = "";
    boolean startTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);
        mapView = findViewById(R.id.mapView);
        bottomInfoLayout = findViewById(R.id.bottomInfoLayout);
        rootLayout = findViewById(R.id.rootLayout);
        bottomInfoLayout.setText(R.string.start_tracking);
        topInfoLayout = findViewById(R.id.topInfoLayout);
        topInfoLayout.setText("Sender App");
        //ask location permission
        checkLocationPermission();
        Mapbox.getInstance(this, null);
        //set your map style url here
        mapView.setStyleUrl(getString(R.string.base_url) + "styles/retro?key=" + baato_access_token);
        mapView.getMapAsync(mapboxMap ->
        {
            //remove mapbox attribute
            mapboxMap.getUiSettings().setAttributionEnabled(false);
            mapboxMap.getUiSettings().setLogoEnabled(false);

            //add your baato logo attribution here
            final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(250, 104);
            params.gravity = Gravity.BOTTOM | Gravity.LEFT;
            params.setMargins(12, 12, 12, 12);
            ImageView imageview = new ImageView(this);
            imageview.setImageResource(R.drawable.baato_logo);
            imageview.setLayoutParams(params);
            mapView.addView(imageview);

            //add your map style url here
            mapboxMap.setStyleUrl(getString(R.string.base_url) + "styles/retro?key=" + baato_access_token,
                    style -> {
                        this.mapboxMap = mapboxMap;
                        mapView.setVisibility(View.VISIBLE);
                        initClickedMarker(mapboxMap);
                        // For Location updates
                        initializeLocationEngine();
                        bottomInfoLayout.setVisibility(View.VISIBLE);
                    });
        });
        mapView.onCreate(savedInstanceState);

        bottomInfoLayout.setOnClickListener(view -> {
            //add intervals to send location
            if (bottomInfoLayout.getText().equals(getString(R.string.start_tracking))) {
                showAddQuestionDialog();
            } else {
                startTracking=false;
                bottomInfoLayout.setText(getString(R.string.start_tracking));
            }
        });

    }

    private void showAddQuestionDialog() {
        final View customLayout = getLayoutInflater().inflate(R.layout.layout_add_track, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setCancelable(true)
                .setView(customLayout)
                .setPositiveButton("ok", (dialogInterface, i) -> {
                    EditText etQuestion = customLayout.findViewById(R.id.etQuestion);
                    dialog.dismiss();
                    tracKName = etQuestion.getText().toString();
                    startTracking = true;
                    bottomInfoLayout.setText(tracKName + ": " + getString(R.string.stop_tracking));
                })
                .show();

        dialog = builder.create();
    }

    private void checkLocationPermission() {
        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{mPermission},
                        REQUEST_CODE_PERMISSION);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * LocationEngine listeners
     */

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        locationToSend = location;
        if (lastLocation == null) {
            // Move the navigationMap camera to the first Location
            moveCameraTo(new LatLng(location.getLatitude(), location.getLongitude()));

            // Allow navigationMap clicks now that we have the current Location
            Snackbar.make(rootLayout, "Please wait...", BaseTransientBottomBar.LENGTH_LONG).show();
        }
        // Cache for fetching the route later
        updateLocation(location);
    }

    private void updateLocation(Location location) {
        locationToSend = location;
        if (lastLocation == location)
            Toast.makeText(this, "same location", Toast.LENGTH_SHORT).show();
        else {
            lastLocation = location;
            if (startTracking)
                saveDataToFirebase();
            updateMarkerPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        locationEngine = locationEngineProvider.obtainLocationEngineBy(LocationEngine.Type.ANDROID);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.addLocationEngineListener(this);
        locationEngine.setInterval(10000);
        locationEngine.setFastestInterval(ONE_SECOND_INTERVAL);
        locationEngine.activate();
    }

    // Initialize, but don't show, a symbol for the marker icon which will represent a selected location.
    private void initClickedMarker(MapboxMap mapboxMap) {
        Drawable drawabled = ResourcesCompat.getDrawable(getResources(), R.drawable.mapbox_marker_icon_default, null);
        Bitmap dBitmap = BitmapUtils.getBitmapFromDrawable(drawabled);
        assert dBitmap != null;
        IconFactory mIconFactory = IconFactory.getInstance(this);
        selectedMarkerIcon = mIconFactory.fromBitmap(dBitmap);
    }

    //update tapped marker position
    private void updateMarkerPosition(LatLng point) {
        if (marker == null) {
            marker = new MarkerOptions().icon(selectedMarkerIcon).position(point);
            mapboxMap.addMarker(marker);
        } else if (!mapboxMap.getMarkers().isEmpty()) {
            Marker marker = mapboxMap.getMarkers().get(0);
            marker.setPosition(point);
            mapboxMap.updateMarker(marker);
        }
        moveCameraTo(point);
    }

    private void moveCameraTo(LatLng point) {
        double zoom = mapboxMap.getCameraPosition().zoom;
        if (zoom < 10)
            zoom = 13;
        else if (zoom < 13)
            zoom = 15;
        else if (zoom < 15)
            zoom = zoom + 1;
        else if (zoom < 18)
            zoom = zoom + 1;
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, zoom), 300);
    }

    private void saveDataToFirebase() {
        if (locationToSend == null)
            Log.d("apple", "saveDataToFirebase: null location to send");
        else {
            rootRef = FirebaseDatabase.getInstance().getReference();
            usersRef = rootRef.child("RealTimeStats");
            LocationData faq = new LocationData(Calendar.getInstance().getTimeInMillis(), locationToSend.getLatitude(), locationToSend.getLongitude(), tracKName);
            usersRef.push().setValue(faq);
        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

}

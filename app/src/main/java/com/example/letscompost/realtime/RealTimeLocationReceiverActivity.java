package com.example.letscompost.realtime;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import androidx.core.content.res.ResourcesCompat;

import com.baato.baatolibrary.models.LatLon;
import com.baato.baatolibrary.models.Place;
import com.baato.baatolibrary.models.PlaceAPIResponse;
import com.baato.baatolibrary.services.BaatoReverse;
import com.example.letscompost.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

/**
 * Click and put a marker at a specific location and then perform
 * reverse geocoding to retrieve and display the location's address
 */
public class RealTimeLocationReceiverActivity extends AppCompatActivity {
    private MapView mapView;
    private TextView bottomInfoLayout, topInfoLayout;
    private MapboxMap mapboxMap;
    private Icon selectedMarkerIcon;
    private MarkerOptions marker;
    private String baato_access_token = "";
    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);
        mapView = findViewById(R.id.mapView);
        bottomInfoLayout = findViewById(R.id.bottomInfoLayout);
        topInfoLayout = findViewById(R.id.topInfoLayout);
        topInfoLayout.setText("Receiver App");
        topInfoLayout.setBackgroundColor(getResources().getColor(R.color.colorRed));
        bottomInfoLayout.setBackgroundColor(getResources().getColor(R.color.colorRed));
        bottomInfoLayout.setText(R.string.start_tracking);
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
                        bottomInfoLayout.setVisibility(View.VISIBLE);
                    });
        });
        mapView.onCreate(savedInstanceState);
        bottomInfoLayout.setOnClickListener(view -> {
            if (bottomInfoLayout.getText().equals(getString(R.string.start_tracking))) {
                showAddQuestionDialog();
            } else {
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
                    dialog.dismiss();
                    EditText etQuestion = customLayout.findViewById(R.id.etQuestion);
                    String tracKName = etQuestion.getText().toString();
                    getLatestLocationOfTheSender(tracKName);
                    bottomInfoLayout.setText(tracKName + ": " + getString(R.string.stop_tracking));
                })
                .show();

        dialog = builder.create();
    }

    private void getLatestLocationOfTheSender(String tracKName) {
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        Query lastQuery = databaseReference.child("RealTimeStats").orderByChild("userId").equalTo(tracKName).limitToLast(1);

        lastQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChildren()) {
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        LocationData data = postSnapshot.getValue(LocationData.class);
                        updateMarkerPosition(new LatLng(data.latitude, data.longitude));
                        Log.d("TAF", "university key" + postSnapshot.getValue());
                    }
                } else
                    Toast.makeText(RealTimeLocationReceiverActivity.this, "nothing", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Initialize, but don't show, a symbol for the marker icon which will represent a selected location.
    private void initClickedMarker(MapboxMap mapboxMap) {
        Drawable drawabled = ResourcesCompat.getDrawable(getResources(), R.drawable.location, null);
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

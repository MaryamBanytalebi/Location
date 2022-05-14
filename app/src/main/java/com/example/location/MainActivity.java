package com.example.location;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    //    private Button btnCurrentLocation;
    public static final int REQUEST_CODE_LOCATION = 0;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap map;
    private Location myLocation;
    private double mLongitude;
    private double mLatitude;
    private ArrayList<LatLng> locations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

//        findViews();
//        setListeners();
        setUpMap();
        defaultLocation();

    }

//    private void setListeners() {
//        btnCurrentLocation.setOnClickListener(new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.M)
//            @Override
//            public void onClick(View view) {
//                if (hasPermission()) {
//                    requestLocation();
//                } else {
//                    requestLocationPermission();
//                }
//            }
//        });
//    }
//
//    private void findViews() {
//        btnCurrentLocation = findViewById(R.id.btn_current_location);
//    }

    private void setUpMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private boolean hasPermission() {
        boolean isFineLocation = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean isCoarseLocation = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return isFineLocation && isCoarseLocation;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestLocationPermission() {
        String[] permissions = new String[]{

                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        requestPermissions(permissions, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_LOCATION:
                if (grantResults == null || grantResults.length == 0)
                    return;
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    requestLocation();
                else {
                    Toast.makeText(this, "we do not have location permission",
                            Toast.LENGTH_SHORT)
                            .show();
                }
        }
    }

    @SuppressLint("MissingPermission")
    private void requestLocation() {

        if (!hasPermission())
            return;

        LocationRequest locationRequest = LocationRequest.create();
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                myLocation = locationResult.getLocations().get(0);
                Log.e("TAG", "onLocationResult: " + myLocation.getLatitude() + "," +
                        myLocation.getLongitude());
            }
        };

        locationRequest.setNumUpdates(1);
        locationRequest.setInterval(0);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        updateUI();
    }

    @SuppressLint("MissingPermission")
    private void updateUI() {
        map.setMyLocationEnabled(true);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng latLng = new LatLng(latitude, longitude);
            map.addMarker(new MarkerOptions().position(latLng).title("موقعیت شما"));
            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));

            mLongitude = longitude;
            mLatitude = latitude;
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(10)
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        setMarkerListener();
    }

    private void setMarkerListener() {
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                map.clear();
                MarkerOptions marker = new MarkerOptions();
                marker.position(latLng);
                marker.title("موقعیت انتخابی شما");
                mLongitude = marker.getPosition().longitude;
                mLatitude = marker.getPosition().latitude;
                map.addMarker(marker);

            }
        });
    }

    private void defaultLocation() {
        if (locations != null) {
            locations.add(new LatLng(35.759131, 51.333236));
            locations.add(new LatLng(35.757785, 51.327323));
            locations.add(new LatLng(35.757815, 51.320967));
        }
    }
}
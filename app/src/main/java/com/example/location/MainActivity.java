package com.example.location;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    //    private Button btnCurrentLocation;
    public static final int REQUEST_CODE_LOCATION = 0;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap map;
    private Location myLocation;
    private double mLongitude;
    private double mLatitude;
    private ArrayList<LatLng> locations = new ArrayList<>();
    private Location selectedLocation;
    private Marker selectedMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

//        findViews();
//        setListeners();
        defaultLocation();
        setUpMap();
        distance();

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
        addMarkerDefaultLocation();
        map.setMyLocationEnabled(true);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (location != null) {
            selectedLocation = location;
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng latLng = new LatLng(latitude, longitude);
            selectedMarker = map.addMarker(new MarkerOptions().position(latLng).title("myLocation"));
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
                if(selectedMarker != null){
                    selectedMarker.remove();
                }
                MarkerOptions marker = new MarkerOptions();
                marker.position(latLng);
                marker.title("selectedLocation");
                mLongitude = marker.getPosition().longitude;
                mLatitude = marker.getPosition().latitude;
                selectedLocation = new Location("selected");
                selectedLocation.setLatitude(mLatitude);
                selectedLocation.setLongitude(mLongitude);
                selectedMarker = map.addMarker(marker);
                drawPath(checkDistance());
            }
        });
    }

    private void drawPath(LatLng destination) {

    }

    private void defaultLocation() {
            locations.add(new LatLng(35.759131, 51.333236));
            locations.add(new LatLng(35.757785, 51.327323));
            locations.add(new LatLng(35.757815, 51.320967));
    }

    private void addMarkerDefaultLocation() {
        for (int i = 0; i < locations.size(); i++) {
            map.addMarker(new MarkerOptions().position(locations.get(i)));
        }
    }

    //
    private LatLng checkDistance() {
        float[] distances = new float[3];
        for (int i = 0; i < locations.size(); i++) {
            Location location = new Location("");
            location.setLatitude(locations.get(i).latitude);
            location.setLongitude(locations.get(i).longitude);
            distances[i] = selectedLocation.distanceTo(location);
        }

        int position = 0;
        for (int i = 0; i < distances.length; i++) {
            for (int j = i + 1; j < distances.length; j++) {
                if (distances[i] < distances[j]) {
                    position = i;
                }
            }
        }
        Log.d("TAG", "checkDistance: " + Arrays.toString(distances));
        Log.d("TAG", "checkDistance: " + position);
        return locations.get(0);

    }
    //

//    private double distance(double lat2, double lon2) {
//        double theta = mLongitude - lon2;
//        double dist = Math.sin(deg2rad(mLatitude)) * Math.sin(deg2rad(lat2))
//                + Math.cos(deg2rad(mLatitude)) * Math.cos(deg2rad(lat2))
//                * Math.cos(deg2rad(theta));
//        dist = Math.acos(dist);
//        dist = rad2deg(dist);
//        dist = dist * 60; // 60 nautical miles per degree of seperation
//        dist = dist * 1852; // 1852 meters per nautical mile
//        return (dist);
//    }
//
//    private double deg2rad(double deg) {
//        return (deg * Math.PI / 180.0);
//    }
//
//    private double rad2deg(double rad) {
//        return (rad * 180.0 / Math.PI);
//    }

    private float distance() {
        Location location1 = new Location("");
        location1.setLatitude(mLatitude);
        location1.setLongitude(mLongitude);

        Location location2 = new Location("");
        location2.setLatitude(35.759131);
        location2.setLongitude(51.333236);

        Location location3 = new Location("");
        location3.setLatitude(35.757785);
        location3.setLongitude(51.327323);

        Location location4 = new Location("");
        location4.setLatitude(35.757815);
        location4.setLongitude(51.320967);

        float distance1 = location1.distanceTo(location2);
        float distance2 = location1.distanceTo(location3);
        float distance3 = location1.distanceTo(location4);

        float minDistance;
        if (distance1 < distance2 && distance1 < distance3)
            minDistance = distance1;
        if (distance2 < distance3)
            minDistance = distance2;
        else minDistance = distance3;

        Log.e("TAG", "distance: " + minDistance);
        return minDistance;
    }
}
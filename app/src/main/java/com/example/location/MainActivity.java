package com.example.location;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import okhttp3.Request;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final int REQUEST_CODE_LOCATION = 0;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap map;
    private Location myLocation;
    private double mLongitude;
    private double mLatitude;
    private ArrayList<LatLng> locations = new ArrayList<>();
    private Location selectedLocation;
    private Marker selectedMarker;
    private Polyline polyline;
    private List<List<HashMap<String, String>>> routes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        defaultLocation();
        setUpMap();
    }

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
                if (selectedMarker != null) {
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

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(
                new LatLng(selectedLocation.getLatitude(), selectedLocation.getLongitude())
                , destination);

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
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

    private String getDirectionsUrl(LatLng origin, LatLng destination) {

        String strOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        String strDest = "destination=" + destination.latitude + "," + destination.longitude;
        String key = "key=AIzaSyB8eJ_2MFLjYYBxf9KYWHbqeoWRtkKz820";
        String parameters = strOrigin + "&" + strDest + "&" + key;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            inputStream = urlConnection.getInputStream();
            BufferedReader bufferReader = new BufferedReader(
                    new InputStreamReader(inputStream));
            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferReader.readLine()) != null) {
                stringBuffer.append(line);
            }
            data = stringBuffer.toString();
            bufferReader.close();

        } catch (Exception e) {
            Log.e("Exception on download", e.toString());
        } finally {
            inputStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";
            try {
                data = downloadUrl(url[0]);
                Log.d("DownloadTask", "DownloadTask : " + data);
            } catch (Exception e) {
                Log.e("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            Log.e("TAG", "doInBackground: " + Arrays.toString(jsonData));
            JSONObject jsonObject;

            try {
                jsonObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jsonObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points ;
            PolylineOptions lineOptions = null;

            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = routes.get(i);
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double latitude = Double.parseDouble(Objects.requireNonNull(point.get("lat")));
                    double lngitude = Double.parseDouble(Objects.requireNonNull(point.get("lng")));
                    LatLng position = new LatLng(latitude, lngitude);
                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(8);
                lineOptions.color(Color.RED);
            }

            if (lineOptions != null) {
                if (polyline != null) {
                    polyline.remove();
                }
                polyline = map.addPolyline(lineOptions);
            }
        }
    }
}
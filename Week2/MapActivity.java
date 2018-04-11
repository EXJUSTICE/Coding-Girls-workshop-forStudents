package com.xu.zombierunteach;




import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends AppCompatActivity implements
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    ArrayList<Marker> allMarkers;

    //Map Var
    GoogleMap googleMap;
    LocationManager locationManager;

    Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    double lat, lon;

    boolean firstZoom = false;
    boolean activated = false;
    boolean firstRun= true;
    boolean caught = false;

    int zombieID;
    //Check if user allowed permission already
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    TextView distanceShower;


    //The primary method of the activity, automatically called by Android. We override it in order to get it to do what we want.
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        distanceShower = findViewById(R.id.distanceshower);
        allMarkers = new ArrayList<Marker>();


    }





    //Required code for LocationListener,  LocationRequests serve to tell Android that we wish to use
    // GPS and GoogleMaps in order to track our location. In order to effectively track this location in GoogleMaps,
    //We need to connect to the Google Maps API.
    @Override
    public void onConnected(Bundle bundle) {
        checkLocationPermission();
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(100);
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {

            buildGoogleApiClient();
            mGoogleApiClient.connect();

        }
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }


        //TODO Upon connection, marker on current location, also used in checkArrival

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            lat = mLastLocation.getLatitude();
            lon = mLastLocation.getLongitude();
            LatLng loc = new LatLng(lat, lon);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
            /*
             */
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    //In theory, this method is called every time the User's location changes. In practice, it is called every few seconds automatically
    @Override
    public void onLocationChanged(Location location) {
        //Heres the code to track user location. Extracting Coordinates from location. One can add markers or do whatever when the user momves.

        mLastLocation = location;

        lat = mLastLocation.getLatitude();
        lon = mLastLocation.getLongitude();
        LatLng loc = new LatLng(lat, lon);


        //initial zoom
        if (firstZoom == false) {
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
            firstZoom = true;
        }



    }

    public void onConnectionFailed(ConnectionResult connectionResult) {
        buildGoogleApiClient();
    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    ;

    //Connect and disconnect to ApiClient during start/destroy
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {

            buildGoogleApiClient();
            mGoogleApiClient.connect();

        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    //This code gets called by GoogleMaps as soon as the Map is Ready. We want to put all map-related code here
    @Override
    public void onMapReady(GoogleMap map) {


        this.googleMap = map;
        checkLocationPermission();

        map.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        //Custom Map UI set up
        //disable zoom Controls
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);




        googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng zombielatlng) {
                spawnzombie(zombielatlng);
            }
        });


    }
    //spwns a zombie Marker on map
    public void spawnzombie(LatLng zombielatlng) {
        drawZombieMarker(zombielatlng, zombieID);
        zombieID += 1;
    }
    // A fallback check in case we do not have GoogleAPI for any reason (i.e. no internet)
    public boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int status = api.isGooglePlayServicesAvailable(this);
        //if we have a problem, return false
        if (status != ConnectionResult.SUCCESS) {
            if (api.isUserResolvableError(status)) {
                api.getErrorDialog(activity, status, 2404).show();
            }
            return false;

        }
        return true;

    }



    //Many Android methods, such as accessing the internet, or using GPS, require the permission of the user. We either request them in the manifest
    //Or do it here.
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //No permission allowed, force user to give one
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.CAMERA, Manifest.permission.WRITE_SETTINGS, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_LOCATION);
            return false;
        } else {
            return true;
        }

    }

    //DrawZombieMarker
    private void drawZombieMarker(LatLng point, int ID) {
        Marker temp;
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

        temp = googleMap.addMarker(markerOptions);
        temp.setTag(ID);
        allMarkers.add(temp);
        measureDistance(point,ID);

    }

    private void measureDistance(LatLng point, int ID){
        Location markerLocation = new Location(LocationManager.NETWORK_PROVIDER);
        markerLocation.setLatitude(point.latitude);
        markerLocation.setLongitude(point.longitude);
        float distance = mLastLocation.distanceTo(markerLocation);

        distanceShower.setText("Distance "+String.valueOf(distance) + " m" + "\n ID: "+ID);
    }

    //callback from RequestPermissions() method, handling the user's response to our requests
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                //if request is cancelled result arrays are empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted, so do everything related to locations
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        googleMap.setMyLocationEnabled(true);
                    }
                } else {
                    //permission denied
                    Toast.makeText(this, "permission denied, app functionality disabled", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        //Required CONNECT CALL TO ACTUALLY START FUSED LOCATION API

        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {

            buildGoogleApiClient();
            mGoogleApiClient.connect();

        }

        if (googleMap == null) {
            SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            fm.getMapAsync(this);
        }


    }
    //called when you minimize app
    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }






}

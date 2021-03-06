package com.sg.geofencing;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import android.Manifest;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class SGHomeActivity extends Activity/* implements*/ /*GoogleApiClient.ConnectionCallbacks,*/
        /*GoogleApiClient.OnConnectionFailedListener,*/
        /*OnMapReadyCallback,*/
       /* GoogleMap.OnMapClickListener*/
        /*GoogleMap.OnMarkerClickListener, LocationListener, ResultCallback */ {
    private GoogleApiClient mGoogleApiClient;
    public static final String TAG = "SGGeoFencing";
    public static final String MY_HOME_FENCE_ID = "my_home_fence";
    private MapFragment mMapFragment;
    private GoogleMap mGoogleMap;
    private TextView mLatitudeTv, mLongitudeTv;
    private Location mLastLocation;
    private static final int REQ_PERMISSION = 100;
    private static final int GEO_FENCE_RADIUS = 500;
    private Geofence mMyHomeFence;
    private LocationRequest mLocationRequest;
    private Marker mGeoFenceMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sghome);
        mLatitudeTv = (TextView) findViewById(R.id.lat);
        mLongitudeTv = (TextView) findViewById(R.id.lon);
        createGoogleApi();
        long oneDay = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        int[] transition = new int[]{
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
        };
        //My Home : 12.9220761,77.4854912
        mMyHomeFence = createGeoFence(MY_HOME_FENCE_ID, 12.9221894, 77.485419, GEO_FENCE_RADIUS, oneDay, transition);

        // Create GoogleApiClient instance
        initGMaps();
    }

    public static Intent makeNotificationIntent(Context geofenceService, String msg) {
        Log.d(TAG, msg);
        return new Intent(geofenceService, SGHomeActivity.class);
    }

    private void createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new MyConnectionCallbacks())
                    .addOnConnectionFailedListener(new MYOnConnectionFailedListener())
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    // Initialize GoogleMaps
    private void initGMaps() {
        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(new MyOnMapReadyCallback());
    }

    private Geofence createGeoFence(String fenceId, double latitude, double longitude, float radius, long expiryDurationInMillis, int[] transitionTypes) {
        int transition = 0;
        for (int i = 0; i < transitionTypes.length; i++) {
            if (i == 0) {
                transition = transitionTypes[0];
            } else {
                transition = transition | transitionTypes[i];
            }
        }
        Geofence fence = new Geofence.Builder()
                .setRequestId(fenceId).
                        setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(expiryDurationInMillis)
                .setTransitionTypes(transition)
                .build();
        return fence;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Call GoogleApiClient connection when starting the Activity
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect GoogleApiClient when stopping Activity
        mGoogleApiClient.disconnect();
    }

    // Get last known location
    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if (checkPermission()) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                Log.i(TAG, "LasKnown location. " +
                        "Long: " + mLastLocation.getLongitude() +
                        " | Lat: " + mLastLocation.getLatitude());
                writeLastLocation();
                startLocationUpdates();
            } else {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
            }
        } else askPermission();
    }


    // Defined in mili seconds.
    // This number in extremely low, and should be used only for debug
    private final int UPDATE_INTERVAL = 1000;
    private final int FASTEST_INTERVAL = 900;

    // Start location Updates
    private void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates()");
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if (checkPermission())
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, new MyLocationListener());
    }

    /* @Override
     public void onLocationChanged(Location location) {
         Log.d(TAG, "onLocationChanged [" + location + "]");
         mLastLocation = location;
         mLatitudeTv.setText(getString(R.string.latitude) + mLastLocation.getLatitude());
         mLongitudeTv.setText(getString(R.string.longitude) + mLastLocation.getLongitude());
         writeActualLocation(location);
     }
 */
    // Asks for permission
    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERMISSION);
    }

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }


    private void writeLastLocation() {
        writeActualLocation(mLastLocation);
    }


    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");
        if (geoFencePendingIntent != null)
            return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTrasitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.geofence: {
                startGeofence();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    // Start Geofence creation process
    private void startGeofence() {
        Log.i(TAG, "startGeofence()");
        if (mGeoFenceMarker != null) {
            Geofence geofence = mMyHomeFence;
            GeofencingRequest geofenceRequest = createGeofenceRequest(geofence);
            addGeofence(geofenceRequest);
        } else {
            Log.e(TAG, "Geofence marker is null");
        }
    }

    // Draw Geofence circle on GoogleMap
    private Circle geoFenceLimits;

    private void drawGeofence() {
        Log.d(TAG, "drawGeofence()");

        if (geoFenceLimits != null)
            geoFenceLimits.remove();

        CircleOptions circleOptions = new CircleOptions()
                .center(mGeoFenceMarker.getPosition())
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150))
                .radius(GEO_FENCE_RADIUS);
        geoFenceLimits = mGoogleMap.addCircle(circleOptions);
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d(TAG, "addGeofence");
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(new MyResultCallback());
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

   /* @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClickListener: " + marker.getPosition());

        return false;
    }
*/


    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    getLastKnownLocation();

                } else {
                    // Permission denied
                    permissionsDenied();
                }
                break;
            }
        }
    }

    // App cannot work without the permissions
    private void permissionsDenied() {
        Log.w(TAG, "permissionsDenied()");
    }

    private void writeActualLocation(Location location) {
        // ...
        markerLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private Marker locationMarker;

    // Create a Location Marker
    private void markerLocation(LatLng latLng) {
        Log.i(TAG, "markerLocation(" + latLng + ")");
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if (mGoogleMap != null) {
            // Remove the anterior marker
            if (locationMarker != null)
                locationMarker.remove();
            locationMarker = mGoogleMap.addMarker(markerOptions);
            float zoom = 14f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            mGoogleMap.animateCamera(cameraUpdate);
        }
    }


    // Create a marker for the geofence creation
    private void markerForGeofence(LatLng latLng) {
        Log.i(TAG, "markerForGeofence(" + latLng + ")");
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);
        if (mGoogleMap != null) {
            // Remove last geoFenceMarker
            if (mGeoFenceMarker != null)
                mGeoFenceMarker.remove();

            mGeoFenceMarker = mGoogleMap.addMarker(markerOptions);
        }
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged [" + location + "]");
            mLastLocation = location;
            mLatitudeTv.setText(getString(R.string.latitude) + mLastLocation.getLatitude());
            mLongitudeTv.setText(getString(R.string.longitude) + mLastLocation.getLongitude());
            writeActualLocation(location);
        }
    }

    private class MyMarkerClickListener implements GoogleMap.OnMarkerClickListener {

        @Override
        public boolean onMarkerClick(Marker marker) {
            Log.d(TAG, "onMarkerClickListener: " + marker.getPosition());
            return false;
        }
    }

    private class MyResultCallback implements ResultCallback {
        @Override
        public void onResult(@NonNull Result result) {
            Log.i(TAG, "onResult: " + result.getStatus());
            if (result.getStatus().isSuccess()) {
                drawGeofence();
            } else {
                // inform about fail
            }
        }
    }

    private class MyOnMapReadyCallback implements OnMapReadyCallback {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            Log.d(TAG, "onMapReady()");
            mGoogleMap = googleMap;
            mGoogleMap.setOnMapClickListener(new MyOnMapClickListener());
            mGoogleMap.setOnMarkerClickListener(new MyMarkerClickListener());
        }
    }

    private class MYOnConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {

        // GoogleApiClient.OnConnectionFailedListener fail
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.w(TAG, "onConnectionFailed()");
        }
    }

    private class MyConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        // GoogleApiClient.ConnectionCallbacks connected
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.i(TAG, "onConnected()");
            getLastKnownLocation();
        }

        // GoogleApiClient.ConnectionCallbacks suspended
        @Override
        public void onConnectionSuspended(int i) {
            Log.w(TAG, "onConnectionSuspended()");
        }
    }

    private class MyOnMapClickListener implements GoogleMap.OnMapClickListener {
        @Override
        public void onMapClick(LatLng latLng) {
            Log.d(TAG, "onMapClick(" + latLng + ")");
            markerForGeofence(latLng);

        }
    }
}


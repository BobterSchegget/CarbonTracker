package com.example.app1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class GPStracker implements LocationListener {

    // The maximum time before a location is updated (in milliseconds)
    public static final int MIN_TIME_MS = 2000;
    // The maximum distance that can be travelled before a location is updated (in meters)
    public static final int MIN_DISTANCE_M = 5;
    private final LocationManager lm;
    private LocationChangeListener locationChangeListener;
    public boolean isTracking = false;
    private final List<Location> trackedLocations = new ArrayList<>();
    Context context;

    public GPStracker(Context c, LocationChangeListener listener) {
        context = c;
        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.locationChangeListener = listener;
    }

    // Here an interface is defined to notify GPSActivity of a location change to modify the total distance covered
    public interface LocationChangeListener {
        void onLocationChanged(List<Location> trackedLocations);
    }

    public Location getLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show();
            return null;
        }
        boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(isGPSEnabled) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_M, this);
            return lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        else {
            Toast.makeText(context, "Please enable GPS", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    public void startTracking() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show();
        }
        boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!isGPSEnabled) {
            Toast.makeText(context, "Please enable GPS", Toast.LENGTH_LONG).show();
        }
        if (!isTracking) {
            isTracking = true;
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_M, this);
        }
    }

    public void stopTracking() {
        if(isTracking) {
            isTracking = false;
            lm.removeUpdates(this);
        }
    }

    // Returns the list of tracked locations
    public List<Location> getTrackedLocations() {
        return trackedLocations;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        // This method is called whenever there is a new location and will add the location to the list
        if(isTracking) {
            trackedLocations.add(location);

            // Notify GPSActivity that a new location has been added
            if (locationChangeListener != null) {
                locationChangeListener.onLocationChanged(trackedLocations);
            }
        }
    }

    @Override
    public void onFlushComplete(int requestCode) {
        LocationListener.super.onFlushComplete(requestCode);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }
}

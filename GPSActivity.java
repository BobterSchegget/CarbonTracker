package com.example.app1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class GPSActivity extends AppCompatActivity implements GPStracker.LocationChangeListener {

    // Declares all the view variables
    private GPStracker gpsTracker;
    private Button trackingButton;
    private Button returnButton;
    private ProgressBar progressBar;
    private TextView progressValueView;
    private TextView progressTextView;
    private TextView altProgressValueView;
    private TextView altProgressTextView;
    private ImageView carImage;
    private ImageView pubTransImage;

    // Declares all strings for saving variables
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String CARBON_SAVED = "carbonSaved";
    public static final String CARBON_PERCENTAGE = "carbonPercentage";
    public static final String CARBON_GOAL = "carbonGoal";
    public static final String CAR_FACTOR = "carFactor";
    public static final String PUBLIC_TRANSPORT_FACTOR = "publicTransportFactor";
    private static final String SELECTED_TRANSPORT_MODE = "selectedTransportMode";
    private static final String CAR_IMAGE_ALPHA = "carImageAlpha";
    private static final String PUB_TRANS_IMAGE_ALPHA = "pubTransImageAlpha";

    // Declares all other variables and set their default values
    private static double savedCarbonValue;
    private static double savedCarbonPercentage;
    private static double currentCarbonValue;
    private static double currentCarbonPercentage;
    private static double carbonGoal = 5;
    private static double carFactor = 0.15;
    private static double pubTransFactor = 0.04;
    private static String selectedTransportMode = "car";
    private static String selectedPerformanceMode = "kilograms";

    // Called upon opening the activity. Loads the variables and sets the views and the actions
    //  that happen whenever the user clicks on some views
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpsactivity);

        // Assigning all activity elements to the defined variables
        trackingButton = findViewById(R.id.startTrackingButton);
        returnButton = findViewById(R.id.returnButton);
        progressBar = findViewById(R.id.progressBar);
        progressValueView = findViewById(R.id.progressValue);
        progressTextView = findViewById(R.id.progressText);
        altProgressValueView = findViewById(R.id.altProgressValue);
        altProgressTextView = findViewById(R.id.altProgressText);
        carImage = findViewById(R.id.carImage);
        pubTransImage = findViewById(R.id.pubTransImage);

        // Load all variables and views with the saved data
        loadData();

        // Button to return to the main activity and close the GPSActivity
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Requests permission in order to access the phone's GPS data
        ActivityCompat.requestPermissions(GPSActivity.this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION},123);
        gpsTracker = new GPStracker(this, this);

        trackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTrackingMode();
            }
        });

        carImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTransportMode = "car";
                updateTransportMode();
            }
        });

        pubTransImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTransportMode = "public_transport";
                updateTransportMode();
            }
        });

        progressBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePerformanceMode();
            }
        });
    }

    // Implements the onLocationChanged method from the LocationChangeListener interface
    @Override
    public void onLocationChanged(List<Location> trackedLocations) {
        // Updates the distance text every time a new location is added
        updateCarbonEmissions(trackedLocations);
    }

    // Whenever the activity is closed, the data is saved and the tracker stops tracking
    @Override
    protected void onDestroy() {
        saveData();
        super.onDestroy();
        if (gpsTracker != null && gpsTracker.isTracking) {
            gpsTracker.stopTracking();
        }
    }

    // Sets tracking mode on or off, meaning that the GPStracker starts or stops tracking and saves the data on stopping
    private void toggleTrackingMode() {
        if (!gpsTracker.isTracking) {
            gpsTracker.startTracking();
            trackingButton.setText("Finish Tracking");
        } else {
            gpsTracker.stopTracking();
            trackingButton.setText("Start Tracking");
            updateCarbonEmissions(gpsTracker.getTrackedLocations());
            saveData();
        }
    }

    // Changes the desired transport mode and then updates the carbon emissions
    private void updateTransportMode() {
        if (selectedTransportMode.equals("car")) {
            carImage.setSelected(true);
            carImage.setAlpha(1.0f);
            pubTransImage.setSelected(false);
            pubTransImage.setAlpha(0.3f);
        } else {
            carImage.setSelected(false);
            carImage.setAlpha(0.3f);
            pubTransImage.setSelected(true);
            pubTransImage.setAlpha(1.0f);
        }
        updateCarbonEmissions(gpsTracker.getTrackedLocations());
    }

    private void updatePerformanceMode() {
        if (selectedPerformanceMode.equals("kilograms")) {
            selectedPerformanceMode = "percentage";
            progressValueView.setVisibility(View.INVISIBLE);
            progressTextView.setVisibility(View.INVISIBLE);
            altProgressValueView.setVisibility(View.VISIBLE);
            altProgressTextView.setVisibility(View.VISIBLE);
        } else {
            selectedPerformanceMode = "kilograms";
            progressValueView.setVisibility(View.VISIBLE);
            progressTextView.setVisibility(View.VISIBLE);
            altProgressValueView.setVisibility(View.INVISIBLE);
            altProgressTextView.setVisibility(View.INVISIBLE);
        }
    }

    // Updates the amount of carbon saved based on the saved locations of the GPStracker
    @SuppressLint("DefaultLocale")
    private void updateCarbonEmissions(List<Location> trackedLocations) {

        double deltaDistance;
        double deltaCarbonValue;

        deltaDistance = calculateTotalDistance(trackedLocations);
        deltaCarbonValue = calculateCarbonSaved(deltaDistance);
        currentCarbonValue = savedCarbonValue + deltaCarbonValue;
        currentCarbonPercentage = currentCarbonValue / carbonGoal;
        progressValueView.setText(String.format("%.3f", currentCarbonValue));
        altProgressValueView.setText(String.format("%.1f", currentCarbonPercentage));
        progressBar.setProgress((int) currentCarbonPercentage);
    }

    // Calculates the distance between two pairs of longitudes and latitudes
    // It uses the Haversine formula for this and then rounds the answer to three decimals
    // Source: https://www.geeksforgeeks.org/haversine-formula-to-find-distance-between-two-points-on-a-sphere/
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // Haversine formula for distance calculation
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Radius of the Earth in kilometers
        // Source https://nssdc.gsfc.nasa.gov/planetary/factsheet/earthfact.html
        double earthRadius = 6378.137;
        // Calculate the distance using the Haversine formula and round to 3 decimals
        BigDecimal distance = new BigDecimal(earthRadius * c).setScale(3, RoundingMode.HALF_UP);
        return distance.doubleValue();
    }

    // Used to calculate the total distance travelled from a list of locations
    // For each subsequent pair of locations the distance is calculated using the function
    // calculateDistance, then the sum of these distances is returned as the totalDistance
    public static double calculateTotalDistance(List<Location> locations) {
        double totalDistance = 0;

        if (locations.size() < 2) {
            return totalDistance;
        }

        for (int i = 0; i < locations.size() - 1; i++) {
            Location currentLocation = locations.get(i);
            Location nextLocation = locations.get(i + 1);

            double distance = calculateDistance(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    nextLocation.getLatitude(), nextLocation.getLongitude());
            totalDistance += distance;
        }

        return totalDistance;
    }

    public static double calculateCarbonSaved(double distance) {
        if (selectedTransportMode.equals("car")) {
            return distance * carFactor;
        } else {
            return distance * pubTransFactor;
        }
    }

    // Saves the variables so they can be loaded correctly whenever the app reopens
    public void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(CARBON_SAVED, String.valueOf(currentCarbonValue));
        editor.putString(CARBON_PERCENTAGE, String.valueOf(currentCarbonPercentage));
        editor.putString(CARBON_GOAL, String.valueOf(carbonGoal));
        editor.putString(CAR_FACTOR, String.valueOf(carFactor));
        editor.putString(PUBLIC_TRANSPORT_FACTOR, String.valueOf(pubTransFactor));
        editor.putString(SELECTED_TRANSPORT_MODE, selectedTransportMode);
        editor.putFloat(CAR_IMAGE_ALPHA, carImage.getAlpha());
        editor.putFloat(PUB_TRANS_IMAGE_ALPHA, pubTransImage.getAlpha());

        editor.apply();

        Toast.makeText(this, "Data saved", Toast.LENGTH_SHORT).show();
    }

    // Loads the variables and sets the values of the progress achieved in previous tracking sessions to the corresponding textviews
    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        try {
            savedCarbonValue = roundDouble(Double.parseDouble(sharedPreferences.getString(CARBON_SAVED, "0")), 3);
            savedCarbonPercentage = roundDouble(Double.parseDouble(sharedPreferences.getString(CARBON_PERCENTAGE, "0")), 1);
            carbonGoal = Double.parseDouble(sharedPreferences.getString(CARBON_GOAL, "5"));
            carFactor = Double.parseDouble(sharedPreferences.getString(CAR_FACTOR, "0.15"));
            pubTransFactor = Double.parseDouble(sharedPreferences.getString(PUBLIC_TRANSPORT_FACTOR, "0.04"));
            selectedTransportMode = sharedPreferences.getString(SELECTED_TRANSPORT_MODE, "car");
            carImage.setAlpha(sharedPreferences.getFloat(CAR_IMAGE_ALPHA, 1.0f));
            pubTransImage.setAlpha(sharedPreferences.getFloat(PUB_TRANS_IMAGE_ALPHA, 0.3f));
        } catch (NumberFormatException e) { // If an error is detecting when loading the data, the variables are reset to their default values
            savedCarbonValue = 0;
            savedCarbonPercentage = 0;
            carbonGoal = 5;
            carFactor = 0.15;
            pubTransFactor = 0.04;
            selectedTransportMode = "car";
            carImage.setAlpha(0.3f);
            pubTransImage.setAlpha(0.3f);
            Toast.makeText(this, "Error loading data. Data has been reset.", Toast.LENGTH_LONG).show();
        }
        // Set the loaded values to the corresponding TextViews
        progressValueView.setText(String.valueOf(savedCarbonValue));
        altProgressValueView.setText(String.valueOf(savedCarbonPercentage));

        Toast.makeText(this, "Data loaded", Toast.LENGTH_SHORT).show();
    }

    // Simple function to make the rounding of doubles easier
    private static double roundDouble(double value, int decimalPlaces) {
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }
}

package com.example.app1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText carbonGoalEditText;
    private EditText carFactorEditText;
    private EditText pubTransFactorEditText;
    private Button saveChangesButton;
    private Button resetButton;

    // Constants for SharedPreferences
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String CARBON_GOAL = "carbonGoal";
    public static final String CAR_FACTOR = "carFactor";
    public static final String PUBLIC_TRANSPORT_FACTOR = "publicTransportFactor";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        carbonGoalEditText = findViewById(R.id.carbonGoalEditText);
        carFactorEditText = findViewById(R.id.carFactorEditText);
        pubTransFactorEditText = findViewById(R.id.pubTransFactorEditText);
        saveChangesButton = findViewById(R.id.saveChangesButton);
        resetButton = findViewById(R.id.resetButton);

        // Load existing values from SharedPreferences
        loadSettings();

        // Save changes when the button is clicked
        saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                Intent intent = new Intent(SettingsActivity.this, GPSActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Display warning dialog when reset button is clicked
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetDialog();
            }
        });
    }

    // Load existing values from SharedPreferences and set them in EditText fields
    private void loadSettings() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
            carbonGoalEditText.setText(sharedPreferences.getString(CARBON_GOAL, String.valueOf(5.0f)));
            carFactorEditText.setText(sharedPreferences.getString(CAR_FACTOR, String.valueOf(0.15f)));
            pubTransFactorEditText.setText(sharedPreferences.getString(PUBLIC_TRANSPORT_FACTOR, String.valueOf(0.04f)));
        } catch (NumberFormatException e) { // If an error is detecting when loading the data, the variables are reset to their default values
            carbonGoalEditText.setText("5.0");
            carFactorEditText.setText("0.15");
            pubTransFactorEditText.setText("0.04");
            Toast.makeText(this, "Error loading data. Data has been reset.", Toast.LENGTH_LONG).show();
        }
    }

    // Save the entered values to SharedPreferences
    private void saveSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(CARBON_GOAL, String.valueOf(carbonGoalEditText.getText()));
        editor.putString(CAR_FACTOR, String.valueOf(carFactorEditText.getText()));
        editor.putString(PUBLIC_TRANSPORT_FACTOR, String.valueOf(pubTransFactorEditText.getText()));

        editor.apply();

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    // Display a warning dialog when reset button is clicked
    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to clear all data? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Clear all data and close the dialog
                    clearAllData();
                    dialog.dismiss();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Close the dialog without clearing data
                    dialog.dismiss();
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Clear all data from SharedPreferences
    private void clearAllData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();

        carbonGoalEditText.setText("5");
        carFactorEditText.setText("0.15");
        pubTransFactorEditText.setText("0.04");
    }
}

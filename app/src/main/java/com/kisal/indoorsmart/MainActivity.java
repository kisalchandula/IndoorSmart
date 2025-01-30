package com.kisal.indoorsmart;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.indoorsmart.R;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_ACTIVITY_RECOGNITION = 100;
    private static final int PERMISSION_LOCATION = 101;
    private static final int PICK_IMAGE_REQUEST = 102;

    private TextView azimuthView, positionView, stepView;
    private MapView canvasView; // Custom view for displaying the point on a canvas
    private final DecimalFormat d = new DecimalFormat("#.###");
    private float prevX = 0, prevY = 0;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        azimuthView = findViewById(R.id.azimuthView);
        positionView = findViewById(R.id.positionView);
        stepView = findViewById(R.id.stepView);
        canvasView = findViewById(R.id.positionCanvas);

        Button startServiceButton = findViewById(R.id.startServiceButton);
        Button uploadMapButton = findViewById(R.id.uploadMapButton);

        // Set button listeners
        startServiceButton.setOnClickListener(v -> onStartServiceClick());
        uploadMapButton.setOnClickListener(v -> onUploadMapClick());

        // Check permissions
        checkPermissions();

        // Register the broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorDataReceiver, new IntentFilter("SensorDataUpdate"));
    }

    private void checkPermissions() {
        // Check Activity Recognition permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSION_ACTIVITY_RECOGNITION);
        }

        // Check Location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
        }
    }

    private final BroadcastReceiver sensorDataReceiver = new BroadcastReceiver() {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        public void onReceive(Context context, Intent intent) {
            double azimuth = intent.getDoubleExtra("orientation", 0.0);
            float[] position = intent.getFloatArrayExtra("position");
            int steps = intent.getIntExtra("step", 0);

            azimuthView.setText("Heading: " + d.format(azimuth) + "°");
            stepView.setText("Steps: " + d.format(steps));

            if (position != null) {
                float x = position[0];
                float y = position[1];
                positionView.setText(String.format("Position (x, y): %.2f, %.2f", x, y));

                if (x != prevX || y != prevY) {
                    canvasView.updatePosition(x+1, y+1);
                    prevX = x;
                    prevY = y;
                }
            } else {
                positionView.setText("Position: Not available");
            }
        }
    };

    private void onStartServiceClick() {
        startStepSensorService();
        Toast.makeText(this, "Indoor Navigation Started", Toast.LENGTH_SHORT).show();
    }

    private void startStepSensorService() {
        Intent serviceIntent = new Intent(this, SensorFusionService.class);
        startService(serviceIntent);
    }

    @SuppressLint("IntentReset")
    private void onUploadMapClick() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Floor Map"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                // Set the floor map in the custom view
                canvasView.setFloorMapBitmap(bitmap);

                // Show an alert dialog prompting the user to touch the starting point
                showStartPointAlert();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showStartPointAlert() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Start Point")
                .setMessage("Please touch the starting point on the uploaded map.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> canvasView.enableStartPointSelection())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorDataReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ACTIVITY_RECOGNITION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Activity Recognition Permission Granted", Toast.LENGTH_SHORT).show();
        } else if (requestCode == PERMISSION_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location Permission Granted", Toast.LENGTH_SHORT).show();
        }
    }
}

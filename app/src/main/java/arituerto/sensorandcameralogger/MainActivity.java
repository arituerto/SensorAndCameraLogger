package arituerto.sensorandcameralogger;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    // TODO: Add GPS logging

    private SharedPreferences mainPreferences;

    private static final String TAG = "MainActivity";

    static final int CAMERA_SETTINGS_REQUEST = 2;
    static final int CPRO_SETTINGS_REQUEST = 3;

    // SENSORS
    private boolean mLogSensor;

    // CAMERA
    private boolean mLogCamera;

    // CPRO
    private boolean mLogCPRO;

    // LOGGING
    private String dataSetName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }

        // LOAD PREFERENCES
        mainPreferences = getSharedPreferences("mainPreferences", MODE_PRIVATE);
        mLogSensor = mainPreferences.getBoolean("sensorLogging", false);
        mLogCamera = mainPreferences.getBoolean("cameraLogging", false);
        mLogCPRO = mainPreferences.getBoolean("cproLogging", false);
        dataSetName = mainPreferences.getString("dataSetName", "test");

        EditText textEntry = (EditText) findViewById(R.id.inputDataSetName);
        textEntry.setText(dataSetName);

        Switch sensorSwitch = (Switch) findViewById(R.id.sensorsSwitch);
        sensorSwitch.setChecked(mLogSensor);
        sensorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mLogSensor = true;
                    Log.i(TAG, "Sensors Logging ON");
                } else {
                    mLogSensor = false;
                    Log.i(TAG, "Sensors Logging OFF");
                }
            }
        });

        Switch cproSwitch = (Switch) findViewById(R.id.cproSwitch);
        cproSwitch.setChecked(mLogCPRO);
        cproSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mLogCPRO = true;
                    Log.i(TAG, "CPRO Logging ON");
                } else {
                    mLogCPRO = false;
                    Log.i(TAG, "CPRO Logging OFF");
                }
            }
        });

        Switch cameraSwitch = (Switch) findViewById(R.id.cameraSwitch);
        cameraSwitch.setChecked(mLogCamera);
        cameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mLogCamera = true;
                    Log.i(TAG, "Camera Logging ON");
                } else {
                    mLogCamera = false;
                    Log.i(TAG, "Camera Logging OFF");
                }
            }
        });


        final Button sensorSettingsButton = (Button) findViewById(R.id.buttonSensorSettings);
        sensorSettingsButton.setOnClickListener(sensorSettingsClick);

        final Button cproSettingsButton = (Button) findViewById(R.id.buttonCPROSettings);
        cproSettingsButton.setOnClickListener(cproSettingsClick);

        final Button cameraSettingsButton = (Button) findViewById(R.id.buttonCameraSettings);
        cameraSettingsButton.setOnClickListener(cameraSettingsClick);

        final Button startButton = (Button) findViewById(R.id.buttonStartLogging);
        startButton.setOnClickListener(startClick);
    }

    // BUTTONS FUNCTIONS
    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            EditText textEntry = (EditText) findViewById(R.id.inputDataSetName);
            dataSetName = textEntry.getText().toString();

            mainPreferences.edit().putBoolean("sensorLogging", mLogSensor);
            mainPreferences.edit().putBoolean("cameraLogging", mLogCamera);
            mainPreferences.edit().putBoolean("cproLogging", mLogCPRO);
            mainPreferences.edit().putString("dataSetName", dataSetName);

            Intent intent = new Intent(MainActivity.this, LoggingActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener sensorSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Sensor Settings");
            Intent intent = new Intent(MainActivity.this, SensorSettingsActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener cameraSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Camera Settings");
            Intent intent = new Intent(MainActivity.this, CameraSettingsActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener cproSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "CPRO Settings");
            Intent intent = new Intent(MainActivity.this, CPROSettingsActivity.class);
            startActivity(intent);
        }
    };
}

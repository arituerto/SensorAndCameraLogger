package arituerto.sensorandcameralogger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // TODO: Save configuration for next runs.
    // TODO: Add GPS logging

    private static final String TAG = "MainActivity";

    static final int SENSORS_SETTINGS_REQUEST = 1;
    static final int CAMERA_SETTINGS_REQUEST = 2;

    // SENSORS
    private boolean mLogSensor;
    private boolean[] mSelectedSensorList;
    private int mSensorDelay = SensorManager.SENSOR_DELAY_UI;

    // CAMERA
    private boolean mLogCamera;
    private String mCameraId;
    private Size mImageSize;
    private int mFocusMode;

    // LOGGING
    private String dataSetName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }

        // TODO: Load and save the parameters. Do not start logging without parameters configures or loaded

        Switch sensorSwitch = (Switch) findViewById(R.id.sensorsSwitch);
        sensorSwitch.setChecked(false);
        mLogSensor = false;
        sensorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mLogSensor = true;
                    Log.i(TAG, "Sensors Logging ON");
                } else {
                    mLogSensor = false;
                    Log.i(TAG, "Sensors Logging ON");
                }
            }
        });

        Switch cameraSwitch = (Switch) findViewById(R.id.cameraSwitch);
        cameraSwitch.setChecked(false);
        mLogCamera = false;
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

        final Button cameraSettingsButton = (Button) findViewById(R.id.buttonCameraSettings);
        cameraSettingsButton.setOnClickListener(cameraSettingsClick);

        final Button startButton = (Button) findViewById(R.id.buttonStartLogging);
        startButton.setOnClickListener(startClick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SENSORS_SETTINGS_REQUEST) {

            if (resultCode == RESULT_OK) {
                Log.i(TAG, "Sensor Settings Received");
                Bundle bundle = data.getExtras();
                mSelectedSensorList = bundle.getBooleanArray("selectedSensors");
                mSensorDelay = bundle.getInt("sensorDelay");
            }

        } else if (requestCode == CAMERA_SETTINGS_REQUEST) {

            if (resultCode == RESULT_OK) {
                Log.i(TAG, "Camera Settings Received");
                Bundle bundle = data.getExtras();
                mCameraId = bundle.getString("selectedCamera");
                mImageSize = bundle.getSize("selectedSize");
                mFocusMode = bundle.getInt("selectedFocus");
            }

        }

    }

    // BUTTONS FUNCTIONS
    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (mLogSensor | mLogCamera) {

                EditText textEntry = (EditText) findViewById(R.id.inputDataSetName);
                dataSetName = textEntry.getText().toString();

                Intent intent = new Intent(MainActivity.this, LoggingActivity.class);
                Bundle outBundle = new Bundle();

                outBundle.putString("dataSetName", dataSetName);
                outBundle.putBoolean("LogSensor", mLogSensor);
                outBundle.putInt("SensorDelay", mSensorDelay);
                outBundle.putBooleanArray("SensorSelection", mSelectedSensorList);

                outBundle.putBoolean("LogCamera", mLogCamera);
                outBundle.putString("CameraId", mCameraId);
                outBundle.putSize("CameraSize", mImageSize);
                outBundle.putInt("CameraAF", mFocusMode);

                intent.putExtras(outBundle);

                startActivity(intent);
            }
        }
    };

    private View.OnClickListener sensorSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Sensor Settings");
            Intent intent = new Intent(MainActivity.this, SensorSettingsActivity.class);
            startActivityForResult(intent, SENSORS_SETTINGS_REQUEST);
        }
    };

    private View.OnClickListener cameraSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Camera Settings");
            Intent intent = new Intent(MainActivity.this, CameraSettingsActivity.class);
            startActivityForResult(intent, CAMERA_SETTINGS_REQUEST);
        }
    };
}

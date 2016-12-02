package arituerto.sensorandcameralogger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    // TODO: Save configuration for next runs.
    // TODO: Add GPS logging

    private static final String TAG = "MainActivity";

    static final int SENSORS_SETTINGS_REQUEST = 1;
    static final int CAMERA_SETTINGS_REQUEST = 2;
    static final int CPRO_SETTINGS_REQUEST = 3;

    // SENSORS
    private boolean mLogSensor;
    private boolean[] mSelectedSensorList;
    private int mSensorDelay;

    // CAMERA
    private boolean mLogCamera;
    private String mCameraId;
    private Size mImageSize;
    private int mFocusMode;
    private int mOutFormat;

    // CPRO
    private boolean mLogCPRO;
    private String mCPRO_Rmac;
    private String mCPRO_Lmac;
    private float mCPROfreq;
    private boolean mCPROAccelerometer;
    private boolean mCPROGyroscope;
    private boolean mCPROBarometer;
    private boolean mCPROMagnetometer;

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

        // TODO: Load and save the parameters. Do not start logging without parameters configures or loaded
        mLogSensor = true;
        mLogCamera = true;
        mLogCPRO = false;
        dataSetName = getResources().getString(R.string.basic_datasetName);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case (SENSORS_SETTINGS_REQUEST):
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Sensor Settings Received");
                    Bundle bundle = data.getExtras();
                    mSelectedSensorList = bundle.getBooleanArray("selectedSensors");
                    mSensorDelay = bundle.getInt("sensorDelay");
                }
                break;
            case (CAMERA_SETTINGS_REQUEST):
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Camera Settings Received");
                    Bundle bundle = data.getExtras();
                    mCameraId = bundle.getString("selectedCamera");
                    mImageSize = bundle.getSize("selectedSize");
                    mFocusMode = bundle.getInt("selectedFocus");
                    mOutFormat = bundle.getInt("selectedOutFormat");
                }
                break;
            case (CPRO_SETTINGS_REQUEST):
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "CPRO Settings Received");
                    Bundle bundle = data.getExtras();
                    mCPRO_Rmac = bundle.getString("CPRORmac");
                    mCPRO_Lmac = bundle.getString("CPROLmac");
                    mCPROfreq = bundle.getFloat("CPROfreq");
                    mCPROAccelerometer = bundle.getBoolean("CPROAccelerometer");
                    mCPROGyroscope = bundle.getBoolean("CPROGyroscope");
                    mCPROBarometer = bundle.getBoolean("CPROBarometer");
                    mCPROMagnetometer = bundle.getBoolean("CPROMagnetometer");
                }
                break;
        }

    }

    // BUTTONS FUNCTIONS
    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

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
            outBundle.putInt("OutputFormat", mOutFormat);

            outBundle.putBoolean("LogCPRO", mLogCPRO);
            outBundle.putString("CPRORmac", mCPRO_Rmac);
            outBundle.putString("CPROLmac", mCPRO_Lmac);
            outBundle.putFloat("CPROfreq", mCPROfreq);
            outBundle.putBoolean("CPROAccelerometer", mCPROAccelerometer);
            outBundle.putBoolean("CPROGyroscope", mCPROGyroscope);
            outBundle.putBoolean("CPROBarometer", mCPROBarometer);
            outBundle.putBoolean("CPROMagnetometer", mCPROMagnetometer);

            intent.putExtras(outBundle);

            startActivity(intent);
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

    private View.OnClickListener cproSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "CPRO Settings");
            Intent intent = new Intent(MainActivity.this, CPROSettingsActivity.class);
            startActivityForResult(intent, CPRO_SETTINGS_REQUEST);
        }
    };
}

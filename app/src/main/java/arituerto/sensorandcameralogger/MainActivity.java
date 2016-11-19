package arituerto.sensorandcameralogger;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // TODO: Save configuration for next runs.
    // TODO: Add GPS logging
    // TODO: Add EXIT button or option

    private static final String TAG = "MAIN";

    static final int SENSORS_SETTINGS_REQUEST = 1;
    static final int CAMERA_SETTINGS_REQUEST = 2;

    // SENSORS
    private boolean mLogSensor = true;
    private boolean[] mSelectedSensorList;
    private int mSensorDelay;

    // CAMERA
    private boolean mLogCamera = false;
    private String mCameraId;
    private Size mImageSize;
    private int mFocusMode;

    // LOGGING
    private String dataSetName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                mImageSize = bundle.getSize("selectedSize");
                mFocusMode = bundle.getInt("selectedFocus");
            }

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
}

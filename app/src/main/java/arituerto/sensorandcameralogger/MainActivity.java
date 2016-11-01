package arituerto.sensorandcameralogger;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // TODO: Save configuration for next runs.
    // TODO: Select the sensors delay (Now: SensorManager.SENSOR_DELAY_FASTEST)

    private static final String TAG = "MainActivity:: ";

    static final int SENSORS_SETTINGS_REQUEST = 1;
    static final int CAMERA_SETTINGS_REQUEST = 2;

    // SENSORS
    private SensorManager mSensorManager;
    private Map<String, Sensor> mSensorMap;
    private ArrayList<String> mNameSensorList;
    private boolean[] mSelectedSensorList;
    private Map<Sensor, Logger> mSensorLoggerMap;

    // CAMERA
    private CameraManager mCameraManager;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCameraCharacteristics;

    // Logging data
    private boolean mLoggingActive;
    private boolean mCameraLogging;
    private File loggingDir;
    private File imageDir;
    private String dataSetName;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, " onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SENSORS
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        mSensorLoggerMap = new HashMap<Sensor, Logger>();
        mNameSensorList = new ArrayList<String>();
        mSelectedSensorList = new boolean[sensorList.size()];
        mSensorMap = new HashMap<String, Sensor>();
        for (int i = 0; i < sensorList.size(); i++) {
            Sensor iSensor = sensorList.get(i);
            String sensorString = iSensor.getName() + "\n" + iSensor.getStringType();
            mSensorMap.put(sensorString, iSensor);
            mNameSensorList.add(sensorString);
            mSelectedSensorList[i] = true;
        }

        // CAMERA
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIds;
        try{
            cameraIds = mCameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            cameraIds = null;
        }
        if (cameraIds == null) {
            Log.i(TAG, "Camera not available");
            mCameraLogging = false;
        } else {
            mCameraLogging = true;
            for (int iCamera = 0; iCamera < cameraIds.length; iCamera++) {
                try {
                    mCameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraIds[iCamera]);
                    if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        mCameraId = cameraIds[iCamera];
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //VISUAL
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        final Button sensorSettingsButton = (Button) findViewById(R.id.buttonSensorSettings);
        sensorSettingsButton.setOnClickListener(sensorSettingsClick);
        final Button cameraSettingsButton = (Button) findViewById(R.id.buttonCameraSettings);
        cameraSettingsButton.setOnClickListener(cameraSettingsClick);
        final Button startButton = (Button) findViewById(R.id.buttonStartLogging);
        startButton.setOnClickListener(startClick);
        final Button stopButton = (Button) findViewById(R.id.buttonStopLogging);
        stopButton.setOnClickListener(stopClick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == SENSORS_SETTINGS_REQUEST) {
        if (resultCode == RESULT_OK) {
            Log.i(TAG, "Sensor Settings Received");
            Bundle bundle = data.getExtras();
            mSelectedSensorList = bundle.getBooleanArray("selectedSensors");
        }
    }

    }

    private void startSensorListeners() {
        for (int iSensor = 0; iSensor < mSelectedSensorList.length; iSensor++) {
            if (mSelectedSensorList[iSensor]) {
                mSensorManager.registerListener(this,
                        mSensorMap.get(mNameSensorList.get(iSensor)),
                        SensorManager.SENSOR_DELAY_FASTEST);
            }
        }
        Log.i(TAG, "Sensor Listeners ON");
    }

    private void stopSensorListeners() {
        mSensorManager.unregisterListener(this);
        Log.i(TAG, "Sensor Listeners OFF");
    }

    private void startLogging() {

        startSensorListeners();

        EditText textEntry = (EditText) findViewById(R.id.inputDataSetName);
        dataSetName = textEntry.getText().toString();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());

        // Create directory
        loggingDir = new File(Environment.getExternalStorageDirectory().getPath() +
                "/" + currentDateAndTime +
                "_" + Build.MANUFACTURER +
                "_" + Build.MODEL +
                "_" + dataSetName);

        try {
            loggingDir.mkdirs();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        imageDir = new File(loggingDir.getPath() + "/images");
        try {
            imageDir.mkdirs();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        String loggerFileName;
        for (int iSensor = 0; iSensor < mSelectedSensorList.length; iSensor++ ) {
            if (mSelectedSensorList[iSensor]) {

                Sensor sensor = mSensorMap.get(mNameSensorList.get(iSensor));

                String sensorTypeString = sensor.getStringType();
                String[] parts = sensorTypeString.split("\\.");
                loggerFileName = loggingDir.getPath() + "/sensor_" + parts[parts.length - 1].toUpperCase() + "_log.csv";

                // First line: Data description
                String csvFormat = "// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ns], EVENT_" + sensorTypeString + "_VALUES";
                try {
                    Logger logger = new Logger(loggerFileName);
                    mSensorLoggerMap.put(sensor, logger);
                    try {
                        logger.log(csvFormat);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void stopLogging() {
        for (Map.Entry<Sensor, Logger> iSensorLogger : mSensorLoggerMap.entrySet()) {
            try {
                iSensorLogger.getValue().close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        mSensorLoggerMap.clear();
        stopSensorListeners();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onSensorChanged(SensorEvent event) {
        if (mLoggingActive) {
            Sensor key = event.sensor;
            Logger sensorLogger = mSensorLoggerMap.get(key);
            String eventData = SystemClock.elapsedRealtimeNanos() + "," + event.timestamp;
            for (float i : event.values){
                eventData += "," + i;
            }
            try {
                sensorLogger.log(eventData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mLoggingActive) {
                Log.i(TAG, "Start Logging");
                startLogging();
                mLoggingActive = true;
                progressBar.setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "System is already Logging");
            }
        }
    };

    private View.OnClickListener stopClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mLoggingActive) {
                Log.i(TAG, "Stop Logging");
                stopLogging();
                mLoggingActive = false;
                progressBar.setVisibility(View.GONE);
            } else {
                Log.i(TAG, "System is not Logging");
            }
        }
    };

    private View.OnClickListener sensorSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Sensor Settings");
            Intent intent = new Intent(MainActivity.this, SensorSettingsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("allSensors", mNameSensorList);
            bundle.putBooleanArray("selectedSensors", mSelectedSensorList);
            intent.putExtras(bundle);
            startActivityForResult(intent, SENSORS_SETTINGS_REQUEST);
        }
    };

    private View.OnClickListener cameraSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Camera Settings");
            Intent intent = new Intent(MainActivity.this, SensorSettingsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("allSensors", mNameSensorList);
            bundle.putBooleanArray("selectedSensors", mSelectedSensorList);
            intent.putExtras(bundle);
            startActivityForResult(intent, SENSORS_SETTINGS_REQUEST);
        }
    };
}

package arituerto.sensorandcameralogger;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity:: ";

    // Variables for sensor reading
    private SensorManager mSensorManager;
    private List<Sensor> mSensorList;
    private Map<Sensor, Logger> mSensorLoggerMap;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;

    // Logging data
    private boolean mLoggingActive;
    private File loggingDir;
    private File imageDir;
    private String dataSetName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, " onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        mSensorLoggerMap = new HashMap<Sensor, Logger>();

        ListIterator<Sensor> iter = mSensorList.listIterator();
        while (iter.hasNext()) {
            mSensorManager.registerListener(this,iter.next(),1000);
        }

        String cameraIdList[];
        mCameraManager = (CameraManager)getSystemService(CAMERA_SERVICE);
        try {
            cameraIdList = mCameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.i(TAG, e.getMessage());
            cameraIdList = null;
        }

        if (cameraIdList != null) {
            for (int i = 0; i < cameraIdList.length; i++) {
                String cameraId = cameraIdList[i];
                Log.i(TAG, "CAMERAID - " + cameraId);
                try {
                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                    Log.i(TAG, "LENS FACING - " + characteristics.get(CameraCharacteristics.LENS_FACING).toString());
                } catch (Exception e) {
                    Log.i(TAG, e.getMessage());
                }
            }
        }

        final Button startButton = (Button) findViewById(R.id.buttonStartLogging);
        startButton.setOnClickListener(startClick);
        final Button stopButton = (Button) findViewById(R.id.buttonStopLogging);
        stopButton.setOnClickListener(stopClick);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }

    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (!mLoggingActive) {

                Log.i(TAG, "Start Logging");

                EditText textEntry = (EditText) findViewById(R.id.inputDataSetName);
                dataSetName = textEntry.getText().toString();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
                String currentDateAndTime = sdf.format(new Date());

                // Create directory
                loggingDir = new File(Environment.getExternalStorageDirectory().getPath() +
                        "/" + currentDateAndTime + "_" + dataSetName);
                try {
                    loggingDir.mkdirs();
                } catch (Exception e) {
                    Log.i(TAG, e.getMessage());
                }
                imageDir = new File(loggingDir.getPath() + "/images");
                try {
                    imageDir.mkdirs();
                } catch (Exception e) {
                    Log.i(TAG, e.getMessage());
                }

                ListIterator<Sensor> iter = mSensorList.listIterator();
                String loggerFileName = new String();
                while (iter.hasNext()) {
                    Sensor key = iter.next();

                    String sensorTypeString = key.getStringType();
                    // TODO: Get last part of the type string and change to capital letters
                    loggerFileName = loggingDir.getPath() + "/sensor_" + sensorTypeString + "_log.csv";

                    String csvFormat = "// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ns], EVENT_" + sensorTypeString + "_VALUES";
                    try {
                        Logger logger = new Logger(loggerFileName);
                        mSensorLoggerMap.put(key, logger);
                        try {
                            logger.log(csvFormat);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                mLoggingActive = true;
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
                Iterator<Map.Entry<Sensor,Logger>> iter = mSensorLoggerMap.entrySet().iterator();
                while (iter.hasNext()) {
                    try {
                        iter.next().getValue().close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                mSensorLoggerMap.clear();

                mLoggingActive = false;
            } else {
                Log.i(TAG, "System is not Logging");
            }
        }
    };

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onSensorChanged(SensorEvent event) {
        if (mLoggingActive) {
            Sensor key = event.sensor;
            Logger sensorLogger = mSensorLoggerMap.get(key);
            // TODO: TIME REFERENCE!!
            String eventData = System.nanoTime() + "," + event.timestamp;
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
}

package arituerto.sensorandcameralogger;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LoggingActivity extends AppCompatActivity implements SensorEventListener{

    private boolean mLoggingON;
    private File mLoggingDir;
    private String mDataSetName;

    private boolean mLogSensor;
    private boolean mLogSensorReady = false;
    private boolean mLogCamera;
    private boolean mLogCameraReady = false;
    private boolean mLogCPRO;
    private boolean mLogCPROReady = false;
    private boolean mLogGPS;
    private boolean mLogGPSReady = false;

    private SensorManager mSensorManager;
    List<Sensor> mSensorList;
    private boolean[] mSelectedSensorList;
    private Map<Sensor, Logger> mSensorLoggerMap;
    private int mSensorDelay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);

        // Get configuration data
        Bundle inBundle = this.getIntent().getExtras();

        mDataSetName = inBundle.getString("dataSetName");

        mLogSensor = inBundle.getBoolean("LogSensor");
        mSensorDelay = inBundle.getInt("sensorDelay");
        mSelectedSensorList = inBundle.getBooleanArray("Sensor");

//        mLogCamera = inBundle.getBoolean("LogCamera");
//        mLogCPRO = inBundle.getBoolean("LogCPRO");
//        mLogGPS = inBundle.getBoolean("LogGPS");

        // Create Looging directory
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());

        // CREATE LOGGING DIRECTORY
        mLoggingDir = new File(Environment.getExternalStorageDirectory().getPath() +
                "/" + currentDateAndTime +
                "_" + Build.MANUFACTURER +
                "_" + Build.MODEL +
                "_" + mDataSetName);
        try {
            mLoggingDir.mkdirs();
        } catch (SecurityException e) {
            e.printStackTrace();
            return;
        }

        if (mLogSensor) {
            startSensorListeners();
            createSensorLoggers();
            mLogSensorReady = true;
            startLogging();
        }
    }

    // START LOGGING IF EVERYTHING IS READY
    private void startLogging() {
        mLoggingON = (mLogSensorReady & mLogCameraReady & mLogCPROReady & mLogGPSReady);
    }

    // SENSOR FUNCTIONS
    private void startSensorListeners() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        assert (mSensorList.size() == mSelectedSensorList.length);
        for (int iSensor = 0; iSensor < mSelectedSensorList.length; iSensor++) {
            if (mSelectedSensorList[iSensor]) {
                mSensorManager.registerListener(this,
                        mSensorList.get(iSensor),
                        mSensorDelay);
            }
        }
    }

    private void createSensorLoggers(){

        String loggerFileName;

        for (int iSensor = 0; iSensor < mSelectedSensorList.length; iSensor++) {
            if (mSelectedSensorList[iSensor]) {

                Sensor sensor = mSensorList.get(iSensor);

                String sensorTypeString = sensor.getStringType();
                String[] parts = sensorTypeString.split("\\.");
                loggerFileName = mLoggingDir.getPath() + "/sensor_" + parts[parts.length - 1].toUpperCase() + "_log.csv";

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

    private void stopSensorListeners() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (mLoggingON) {
            Sensor key = event.sensor;
            Logger sensorLogger = mSensorLoggerMap.get(key);
            String eventData = SystemClock.elapsedRealtimeNanos() + "," + event.timestamp;
            for (float i : event.values) {
                eventData += "," + i;
            }
            try {
                sensorLogger.log(eventData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

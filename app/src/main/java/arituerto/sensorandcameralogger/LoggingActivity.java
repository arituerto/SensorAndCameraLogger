package arituerto.sensorandcameralogger;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBleService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoggingActivity extends AppCompatActivity implements SensorEventListener, ServiceConnection, CPROboardLog.CPROboardLogListener {

    private static String TAG = "LoggingActivity";

    private boolean mLoggingON;
    private long mStartLoggingTime;
    private File mLoggingDir;
    private String mDataSetName;

    private boolean mLogSensor;
    private boolean mLogCamera;
    private boolean mLogCPRO;

    private boolean sensorsReceiving = false;

    private SensorManager mSensorManager;
    List<Sensor> mSensorList;
    private boolean[] mSelectedSensorList;
    private Map<Sensor, Logger> mSensorLoggerMap = new HashMap<Sensor, Logger>();
    private int mSensorDelay;

    private int mCameraId;
    private Camera.Size mCameraSize;
    private String mCameraAF;
    private int mOutputFormat;
    private File mCameraLoggingDir;
    private Camera mCameraDevice;
    private Logger mCameraLogger;


    private String mCPRO_Rmac;
    private String mCPRO_Lmac;
    private MetaWearBleService.LocalBinder mServiceBinder;
    private CPROboardLog mRboard;
    private CPROboardLog mLboard;

    //PREVIEW SURFACE
    private SurfaceHolder mPreviewHolder;

    // STOP ACTIVITY
    boolean mDoubleBackToExitPressedOnce = false;

    // VISUAL
    private ProgressBar loggingSpinner;
    private TextView sensorsText;
    private TextView cameraText;
    private TextView cproRText;
    private TextView cproLText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);

        // VISUAL
        loggingSpinner = (ProgressBar) findViewById(R.id.progressBarLogging);
        loggingSpinner.setVisibility(View.INVISIBLE);

        cproRText = (TextView) findViewById(R.id.textCPROR);
        cameraText = (TextView) findViewById(R.id.textCamera);
        sensorsText = (TextView) findViewById(R.id.textSensors);
        cproLText = (TextView) findViewById(R.id.textCPROL);

        Button actionButton = (Button) findViewById(R.id.buttonLog);
        actionButton.setText("START");
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mLoggingON) {
                    Toast.makeText(getApplicationContext(), "LOGGING", Toast.LENGTH_LONG).show();
                    v.setVisibility(View.INVISIBLE);
                    loggingSpinner.setVisibility(View.VISIBLE);
                    startLogging();
                }
            }
        });

        // Get configuration data
        readPreferences();

        // Create Logging directory
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());
        mLoggingDir = new File(Environment.getExternalStorageDirectory().getPath() +
                "/" + currentDateAndTime +
                "_" + Build.MANUFACTURER +
                "_" + Build.MODEL +
                "_" + mDataSetName);
        try {
            mLoggingDir.mkdirs();
            Log.i(TAG, "logging dir created");
        } catch (SecurityException e) {
            e.printStackTrace();
            return;
        }

        if (mLogSensor) {
            sensorsText.setText("Sensors connecting");
            startSensorListenersAndLoggers();
        } else {
            sensorsText.setText("Sensors logging OFF");
        }

        if (mLogCPRO) {
            cproRText.setText("CPRO R connecting");
            cproLText.setText("CPRO L connecting");
            getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                    this, Context.BIND_AUTO_CREATE);
        } else {
            cproRText.setText("CPRO R logging OFF");
            cproLText.setText("CPRO L logging OFF");
        }

        // Camera
        startCamera();

        if (mLogCamera) {
            cameraText.setText("Camera connecting");
            // Create Image Logging directory
            mCameraLoggingDir = new File(mLoggingDir.getPath() + "/images_" + mCameraSize.width + "x" + mCameraSize.height);
            try {
                mCameraLoggingDir.mkdirs();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            // Create Camera Logger
            try {
                mCameraLogger = new Logger(mLoggingDir.getPath() + "/sensor_CAMERA_log.csv");
                try {
                    mCameraLogger.log("// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ns], IMG_NAME");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            cameraText.setText("Camera logging OFF");
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        stopLogging();
    }

    @Override
    public void onBackPressed() {
        if (mDoubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.mDoubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDoubleBackToExitPressedOnce=false;
            }
        }, 1000);
    }

    private void readPreferences() {

        // Main Preferences
        SharedPreferences mainPreferences = getSharedPreferences(MainActivity.SHRDPRFS_NAME, MODE_PRIVATE);
        mLogSensor = mainPreferences.getBoolean(MainActivity.SNSLOG, false);
        mLogCamera = mainPreferences.getBoolean(MainActivity.CAMLOG, false);
        mLogCPRO = mainPreferences.getBoolean(MainActivity.CPROLOG, false);
        mDataSetName = mainPreferences.getString(MainActivity.DTSTNAME, "test");

        // CAMERA SETTINGS
        SharedPreferences cameraPrefs = getSharedPreferences(CameraSettingsActivity.SHRDPRFS_NAME, MODE_PRIVATE);
        mCameraId = cameraPrefs.getInt(CameraSettingsActivity.CAMID, 0);
        mOutputFormat = cameraPrefs.getInt(CameraSettingsActivity.FORMAT, -1);
        int sizePos = cameraPrefs.getInt(CameraSettingsActivity.SIZE, -1);
        mCameraAF = cameraPrefs.getString(CameraSettingsActivity.FOCUS, null);try {
            Camera camera = Camera.open(mCameraId);
            mCameraSize = camera.getParameters().getSupportedPictureSizes().get(sizePos);
            camera.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // SENSOR SETTINGS
        SharedPreferences sensorsPrefs = getSharedPreferences(SensorSettingsActivity.SHRDPRFS_NAME, MODE_PRIVATE);
        mSelectedSensorList = SensorSettingsActivity.loadBooleanArray(SensorSettingsActivity.SNSSELECTION, sensorsPrefs);
        mSensorDelay = sensorsPrefs.getInt(SensorSettingsActivity.SNSDELAY, SensorManager.SENSOR_DELAY_NORMAL);

        // CPRO SETTINGS
        SharedPreferences cproPrefs = getSharedPreferences(CPROSettingsActivity.SHRDPRFS_NAME, MODE_PRIVATE);
        mCPRO_Rmac = cproPrefs.getString(CPROSettingsActivity.CPRORMAC, "D3:27:08:FD:69:78");
        mCPRO_Lmac = cproPrefs.getString(CPROSettingsActivity.CPROLMAC, "D0:72:37:14:3B:15");
    }

    // START/STOP LOGGING
    private void startLogging() {
        mStartLoggingTime = SystemClock.elapsedRealtimeNanos();
        mLoggingON = true;
//        if (mLogCamera) {
//            mCameraDevice.takePicture(null, null, mJpegCallback);
//        }
        if (mLogCPRO) {
            mRboard.activateLogging();
            mLboard.activateLogging();
        }
        Log.i(TAG, "Logging START");
    }

    private void stopLogging() {

        mLoggingON = false;

        if (mLogCPRO) {
            mRboard.deactivateLogging();
            mLboard.deactivateLogging();
        }

        writeSessionDescription();

        if (mLogSensor) {
            stopSensorLoggers();
            stopSensorListeners();
        }
        if (mLogCPRO) {
            mRboard.disconnect();
            mLboard.disconnect();
            getApplicationContext().unbindService(this);
        }
        if (mLogCamera) {
            try {
                mCameraLogger.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            closeCamera();
        }
        Log.i(TAG, "Logging STOP");
        Toast.makeText(getApplicationContext(), "LOGGING STOPPED", Toast.LENGTH_LONG).show();

    }

    // SENSOR FUNCTIONS
    private void startSensorListenersAndLoggers() {
        Log.i(TAG, "startSensorListenersAndLoggers");
        String loggerFileName;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (int iSensor = 0; iSensor < mSelectedSensorList.length; iSensor++) {
            if (mSelectedSensorList[iSensor]) {
                mSensorManager.registerListener(this,
                        mSensorList.get(iSensor),
                        mSensorDelay);
                Sensor sensor = mSensorList.get(iSensor);
                String sensorTypeString = SensorSettingsActivity.getSensorTypeName(sensor.getType());
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
        sensorsText.setText("Sensors connected");
    }

    private void stopSensorListeners() {
        Log.i(TAG, "stopSensorListeners");
        if (null != mSensorManager) {
            mSensorManager.unregisterListener(this);
        }
    }

    private void stopSensorLoggers() {
        Log.i(TAG, "stopSensorLoggers");
        for (Map.Entry<Sensor, Logger> iLogger : mSensorLoggerMap.entrySet()) {
            try {
                iLogger.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (! sensorsReceiving) {
            sensorsReceiving = true;
            sensorsText.setText("Sensors receiving");
        }
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

    @Override
    public void onCPROStateChanged(int state, String boardID) {

        TextView textView;
        if (boardID == mRboard.getBoardID()) {
            textView = cproRText;
        } else {
            textView = cproLText;
        }

        if (state == CPROboardLog.stateCreated) {
            textView.setText(boardID + " created");
        } else if (state == CPROboardLog.stateConnecting) {
            textView.setText(boardID + " connecting");
        } else if (state == CPROboardLog.stateConnected) {
            textView.setText(boardID + " connected");
        } else if (state == CPROboardLog.stateConnError) {
            textView.setText(boardID + " connection error");
        } else if (state == CPROboardLog.stateReceiving) {
            textView.setText(boardID + " receiving data");
        } else if (state == CPROboardLog.stateDisconnected) {
            textView.setText(boardID + " disconnected");
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        Log.i(TAG, "MetaWear Service Connected");

        mServiceBinder = (MetaWearBleService.LocalBinder) service;

        BluetoothManager bt = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        BluetoothDevice deviceR = bt.getAdapter().getRemoteDevice(mCPRO_Rmac);
        mRboard = new CPROboardLog("CPRO_R",
                mServiceBinder.getMetaWearBoard(deviceR),
                mLoggingDir,
                true,
                true,
                true,
                true,
                true);
        mRboard.registerListener(this);
        mRboard.connect();

        BluetoothDevice deviceL = bt.getAdapter().getRemoteDevice(mCPRO_Lmac);
        mLboard = new CPROboardLog("CPRO_L",
                mServiceBinder.getMetaWearBoard(deviceL),
                mLoggingDir,
                true,
                true,
                true,
                true,
                true);
        mLboard.registerListener(this);
        mLboard.connect();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    // CAMERA FUNCTIONS
    private void startCamera() {
        // Open Camera
        try {
            mCameraDevice = Camera.open(mCameraId);
            setPictureParameters();
            setPreviewParameters();

        } catch (RuntimeException e) {
            e.printStackTrace();
            cameraText.setText("Failed to acquire Camera");
        }

        // Setup Preview
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mPreviewHolder = surfaceView.getHolder();
        mPreviewHolder.addCallback(mPreviewHolderCallback);

        if (mLogCamera) {
            mCameraDevice.setPreviewCallback(mPreviewCallback);
        }
    }

    void setPictureParameters() {

        // SET PARAMETERS PICTURE

        Camera.Parameters camParams = mCameraDevice.getParameters();

        if ((camParams.getSupportedPictureFormats() != null) & (camParams.getSupportedPictureFormats().contains(mOutputFormat))) {
            camParams.setPictureFormat(mOutputFormat);
            mCameraDevice.setParameters(camParams);
            Log.i("startCamera", "Picture Output format set to " +
                    CameraSettingsActivity.getOutputFormatName(mCameraDevice.getParameters().getPictureFormat()));
        }

        if ((camParams.getSupportedFocusModes() != null) & (camParams.getSupportedFocusModes().contains(mCameraAF))) {
            camParams.setFocusMode(mCameraAF);
            mCameraDevice.setParameters(camParams);
            Log.i("startCamera", "Focus mode set to " +
                    CameraSettingsActivity.getAFModeName(mCameraDevice.getParameters().getFocusMode()));
        }

        if ((camParams.getSupportedPictureSizes() != null) & (camParams.getSupportedPictureSizes().contains(mCameraSize))) {
            camParams.setPictureSize(mCameraSize.width, mCameraSize.height);
            mCameraDevice.setParameters(camParams);
            Log.i("startCamera", "Picture Size set to " +
                    mCameraDevice.getParameters().getPictureSize().width + "x" +
                    mCameraDevice.getParameters().getPictureSize().height);
        }

    }

    void setPreviewParameters() {

        // SET PARAMETERS PICTURE

        Camera.Parameters camParams = mCameraDevice.getParameters();

        if ((camParams.getSupportedPreviewFormats() != null) & (camParams.getSupportedPreviewFormats().contains(mOutputFormat))) {
            camParams.setPreviewFormat(mOutputFormat);
            mCameraDevice.setParameters(camParams);
            Log.i("startCamera", "Preview Output format set to " +
                    CameraSettingsActivity.getOutputFormatName(mCameraDevice.getParameters().getPreviewFormat()));
        }

        if ((camParams.getSupportedFocusModes() != null) & (camParams.getSupportedFocusModes().contains(mCameraAF))) {
            camParams.setFocusMode(mCameraAF);
            mCameraDevice.setParameters(camParams);
            Log.i("startCamera", "Focus mode set to " +
                    CameraSettingsActivity.getAFModeName(mCameraDevice.getParameters().getFocusMode()));
        }

        if ((camParams.getSupportedPreviewSizes() != null) & (camParams.getSupportedPreviewSizes().contains(mCameraSize))) {
            camParams.setPreviewSize(mCameraSize.width, mCameraSize.height);
            mCameraDevice.setParameters(camParams);
            Log.i("startCamera", "Preview Size set to " +
                    mCameraDevice.getParameters().getPreviewSize().width + "x" +
                    mCameraDevice.getParameters().getPreviewSize().height);
        }

    }


    private SurfaceHolder.Callback mPreviewHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCameraDevice.setPreviewDisplay(holder);
                mCameraDevice.startPreview();
                cameraText.setText("Camera receiving images");
            } catch (IOException e) {
                Log.e(TAG, "Failed to start preview");
            }
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {}
    };

    private Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // Save image
            JpegSaver imgSave = new JpegSaver();
            imgSave.execute(data);
            if (mLoggingON) {
                mCameraDevice.startPreview();
                mCameraDevice.takePicture(null, null, mJpegCallback);
            }
        }
    };

    class JpegSaver extends AsyncTask<byte[], Void, Boolean> {
        @Override
        protected Boolean doInBackground(byte[]... params) {
            byte[] data = params[0];
            long systemTime = SystemClock.elapsedRealtimeNanos();
            // CREATE NAMES
            String imgName = "img_" + systemTime + ".jpg";
            String imgFileName = mCameraLoggingDir.getPath() + "/" + imgName;
            // LOG DATA
            String eventData = systemTime + "," + systemTime + "," + imgName;
            try {
                mCameraLogger.log(eventData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                FileOutputStream out = new FileOutputStream(imgFileName);
                out.write(data);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mLoggingON) {
                PreviewSaver saver = new PreviewSaver();
                saver.execute(data);
            }
        }
    };

    class PreviewSaver extends AsyncTask<byte[], Void, Boolean> {
        @Override
        protected Boolean doInBackground(byte[]... params) {
            byte[] data = params[0];
            long systemTime = SystemClock.elapsedRealtimeNanos();
            // CREATE NAMES
            String imgName = "img_" + systemTime + ".jpg";
            String imgFileName = mCameraLoggingDir.getPath() + "/" + imgName;
            // LOG DATA
            String eventData = systemTime + "," + systemTime + "," + imgName;
            try {
                mCameraLogger.log(eventData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            YuvImage im = new YuvImage(data,
                    mCameraDevice.getParameters().getPreviewFormat(),
                    mCameraDevice.getParameters().getPreviewSize().width,
                    mCameraDevice.getParameters().getPreviewSize().height,
                    null);
            Rect r = new Rect(0, 0,
                    mCameraDevice.getParameters().getPreviewSize().width,
                    mCameraDevice.getParameters().getPreviewSize().height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            im.compressToJpeg(r, 100, baos);
            try {
                FileOutputStream out = new FileOutputStream(imgFileName);
                out.write(baos.toByteArray());
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private void closeCamera() {

        Log.i(TAG, "closeCamera");

        if (null != mCameraDevice) {
            mCameraDevice.stopPreview();
            mCameraDevice.setPreviewCallback(null);
            mCameraDevice.release();
            mCameraDevice = null;
        }

    }

    // SESSION DESCRIPTION
    private void writeSessionDescription() {

        Log.i(TAG, "writeSessionDescription");

        String sessionDescriptionName = mLoggingDir.getPath() + "/sessionDescription.txt";

        FileOutputStream outputStream;

        try {
            outputStream = new FileOutputStream(sessionDescriptionName);

            String string;

            string = "DATA_SET_NAME, " + mDataSetName + System.lineSeparator();
            outputStream.write(string.getBytes());

            string = "DATA_SET_FOLDER, " + mLoggingDir + System.lineSeparator();
            outputStream.write(string.getBytes());

            double sessionTime = (SystemClock.elapsedRealtimeNanos() - mStartLoggingTime)/1000000000.0;
            string = "DATA_SET_TIME, " + (sessionTime) + " [s]" + System.lineSeparator();
            outputStream.write(string.getBytes());

            if (mLogCamera) {

                string = "CAMERA_RESOLUTION, " + mCameraSize.width + "x" + mCameraSize.height + System.lineSeparator();
                outputStream.write(string.getBytes());
                string = "CAMERA_AF_MODE, " + CameraSettingsActivity.getAFModeName(mCameraAF) + System.lineSeparator();
                outputStream.write(string.getBytes());
                string = "CAMERA_OUTPUT_FORMAT," + CameraSettingsActivity.getOutputFormatName(mOutputFormat) + System.lineSeparator();
                outputStream.write(string.getBytes());

                string = "CAMERA_N_IMAGES, " + (mCameraLogger.getnLogs() - 1) + " [" + ((float) (mCameraLogger.getnLogs() - 1) / sessionTime) + " Hz]" + System.lineSeparator();
                outputStream.write(string.getBytes());
            }

            if (mLogSensor) {
                switch (mSensorDelay) {
                    case (SensorManager.SENSOR_DELAY_UI):
                        string = "SENSOR_DELAY, SENSOR_DELAY_UI" + System.lineSeparator();
                        break;
                    case (SensorManager.SENSOR_DELAY_NORMAL):
                        string = "SENSOR_DELAY, SENSOR_DELAY_NORMAL" + System.lineSeparator();
                        break;
                    case (SensorManager.SENSOR_DELAY_GAME):
                        string = "SENSOR_DELAY, SENSOR_DELAY_GAME" + System.lineSeparator();
                        break;
                    case (SensorManager.SENSOR_DELAY_FASTEST):
                        string = "SENSOR_DELAY, SENSOR_DELAY_FASTEST" + System.lineSeparator();
                        break;
                }
                outputStream.write(string.getBytes());

                for (Map.Entry<Sensor, Logger> iSensorLogger : mSensorLoggerMap.entrySet()) {
                    string = "SENSOR_NAME, " + iSensorLogger.getKey().getName() + System.lineSeparator();
                    outputStream.write(string.getBytes());
                    string = "N_READINGS, " + (iSensorLogger.getValue().getnLogs() - 1) + " [" + ((float) (iSensorLogger.getValue().getnLogs() - 1) / sessionTime) + " Hz]" + System.lineSeparator();
                    outputStream.write(string.getBytes());
                }
            }

            if (mLogCPRO) {
                for (Map.Entry<String, Logger> iStringLogger : mRboard.getLoggersMap().entrySet()) {
                    string = "SENSOR_NAME, " + "CPRO_R_" + iStringLogger.getKey() + System.lineSeparator();
                    outputStream.write(string.getBytes());
                    string = "N_READINGS, " + (iStringLogger.getValue().getnLogs() - 1) + " [" + ((float) (iStringLogger.getValue().getnLogs() - 1) / sessionTime) + " Hz]" + System.lineSeparator();
                    outputStream.write(string.getBytes());
                }
                for (Map.Entry<String, Logger> iStringLogger : mLboard.getLoggersMap().entrySet()) {
                    string = "SENSOR_NAME, " + "CPRO_L_" + iStringLogger.getKey() + System.lineSeparator();
                    outputStream.write(string.getBytes());
                    string = "N_READINGS, " + (iStringLogger.getValue().getnLogs() - 1) + " [" + ((float) (iStringLogger.getValue().getnLogs() - 1) / sessionTime) + " Hz]" + System.lineSeparator();
                    outputStream.write(string.getBytes());
                }
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

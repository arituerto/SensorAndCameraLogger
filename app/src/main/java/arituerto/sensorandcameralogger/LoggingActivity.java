package arituerto.sensorandcameralogger;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
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

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoCameraPreview;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.mbientlab.metawear.MetaWearBleService;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoggingActivity
        extends AppCompatActivity
        implements ServiceConnection, CPROboardLog.CPROboardLogListener {

    private static String TAG = "LoggingActivity";

    private boolean mLoggingON;
    private long mStartLoggingTime;
    private File mLoggingDir;
    private String mDataSetName;

    private boolean mLogSensor;
    private boolean mLogCamera;
    private boolean mLogCPRO;
    private boolean mLogTango;

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

    private Tango mTangoDevice;
    private TangoConfig mTangoConfig;
    private TangoCameraPreview tangoCameraPreview;
    private Logger mTangoPoseLogger;
    private Logger mTangoDepthLogger;
    private File mDepthLoggingDir;

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
                "_" + Build.MODEL.replace(" ", "_") +
                "_" + mDataSetName);
        try {
            mLoggingDir.mkdirs();
            Log.i(TAG, "logging dir created");
        } catch (SecurityException e) {
            e.printStackTrace();
            return;
        }

        if (mLogSensor) {
            startSensors();
        } else {
            sensorsText.setText("Sensors OFF");
        }

        if (mLogCPRO) {
            cproRText.setText("CPRO R connecting");
            cproLText.setText("CPRO L connecting");
            getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                    this, Context.BIND_AUTO_CREATE);
        } else {
            cproRText.setText("CPRO R OFF");
            cproLText.setText("CPRO L OFF");
        }

        // Camera or Tango
        if (mLogTango) {
            startTango();
        } else {
            startCamera();
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
        mLogTango = mainPreferences.getBoolean(MainActivity.TNGLOG, false);
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

        if (mRboard != null) {
            mRboard.activateLogging();
        }
        if (mLboard != null) {
            mLboard.activateLogging();
        }

        Log.i(TAG, "Logging START");
    }

    private void stopLogging() {

        mLoggingON = false;

        if (mRboard != null) {
            mRboard.deactivateLogging();
        }
        if (mLboard != null) {
            mLboard.deactivateLogging();
        }

        writeSessionDescription();

        if (mLogSensor) {
            stopSensors();
        }

        if (mLogCPRO) {
            mRboard.disconnect();
            mLboard.disconnect();
            getApplicationContext().unbindService(this);
        }

        if (mLogCamera) {
            closeCamera();
        }
        if (mLogTango) {
            closeTango();
        }

        Log.i(TAG, "Logging STOP");
        Toast.makeText(getApplicationContext(), "LOGGING STOPPED", Toast.LENGTH_LONG).show();

    }

    // SENSOR FUNCTIONS
    private void startSensors() {

        Log.i(TAG, "startSensors");

        sensorsText.setText("Sensors connecting");

        String loggerFileName;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (int iSensor = 0; iSensor < mSelectedSensorList.length; iSensor++) {
            if (mSelectedSensorList[iSensor]) {
                mSensorManager.registerListener(
                        mSensorListener,
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

    private SensorEventListener mSensorListener = new SensorEventListener() {

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
    };

    private void stopSensors() {

        Log.i(TAG, "stopSensors");

        if (null != mSensorManager) {
            mSensorManager.unregisterListener(mSensorListener);
        }

        for (Map.Entry<Sensor, Logger> iLogger : mSensorLoggerMap.entrySet()) {
            try {
                iLogger.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sensorsText.setText("Sensors disconnected");
    }

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

        cameraText.setText("Camera connecting");

        // Open Camera
        try {
            mCameraDevice = Camera.open(mCameraId);
            setCameraPreviewParameters();

        } catch (RuntimeException e) {
            e.printStackTrace();
            cameraText.setText("Failed to acquire Camera");
        }

        // Setup Preview
        findViewById(R.id.tangoSurface).setVisibility(View.INVISIBLE);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.cameraView);
        mPreviewHolder = surfaceView.getHolder();
        mPreviewHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    mCameraDevice.setPreviewDisplay(holder);
                    mCameraDevice.startPreview();
                    cameraText.setText("Camera receiving");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to start preview");
                }
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });

        if (mLogCamera) {

            mCameraDevice.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (mLoggingON) {
                        long systemTime = SystemClock.elapsedRealtimeNanos();
                        String imgName = "img_" + systemTime + ".jpg";
                        // LOG DATA
                        String eventData = systemTime + "," + systemTime + "," + imgName;
                        try {
                            mCameraLogger.log(eventData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        PreviewSaver saver = new PreviewSaver();
                        saver.execute(new PreviewSaverParams(data, imgName));
                    }
                }
            });

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

    private void setCameraPreviewParameters() {

        // SET PARAMETERS PICTURE
        Camera.Parameters camParams = mCameraDevice.getParameters();

        if ((camParams.getSupportedPreviewFormats() != null) & (camParams.getSupportedPreviewFormats().contains(mOutputFormat))) {
            camParams.setPreviewFormat(mOutputFormat);
            mCameraDevice.setParameters(camParams);
            Log.i("startCamera", "Preview Output format set to " +
                    CameraSettingsActivity.getOutputFormatName(mCameraDevice.getParameters().getPreviewFormat()));
        }
        mOutputFormat = mCameraDevice.getParameters().getPreviewFormat();

        if ((camParams.getSupportedFocusModes() != null) & (camParams.getSupportedFocusModes().contains(mCameraAF))) {
            camParams.setFocusMode(mCameraAF);
            mCameraDevice.setParameters(camParams);
            Log.i("startCamera", "Focus mode set to " +
                    CameraSettingsActivity.getAFModeName(mCameraDevice.getParameters().getFocusMode()));
        }
        mCameraAF = mCameraDevice.getParameters().getFocusMode();

        if ((camParams.getSupportedPreviewSizes() != null) & (camParams.getSupportedPreviewSizes().contains(mCameraSize))) {
            camParams.setPreviewSize(mCameraSize.width, mCameraSize.height);
            mCameraDevice.setParameters(camParams);
            Log.i("startCamera", "Preview Size set to " +
                    mCameraDevice.getParameters().getPreviewSize().width + "x" +
                    mCameraDevice.getParameters().getPreviewSize().height);
        }
        mCameraSize = mCameraDevice.getParameters().getPreviewSize();

    }

    private class PreviewSaverParams {
        byte[] data;
        String imgName;

        PreviewSaverParams(byte[] inData, String inImgName) {
            this.data = inData.clone();
            this.imgName = inImgName;
        }
    }

    private class PreviewSaver extends AsyncTask<PreviewSaverParams, Void, Boolean> {
        @Override
        protected Boolean doInBackground(PreviewSaverParams... params) {
            PreviewSaverParams saverParams = params[0];
            String imgFileName = mCameraLoggingDir.getPath() + "/" + saverParams.imgName;
            YuvImage im = new YuvImage(saverParams.data,
                    mOutputFormat,
                    mCameraSize.width,
                    mCameraSize.height,
                    null);
            Rect r = new Rect(0, 0,
                    mCameraSize.width,
                    mCameraSize.height);
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

        try {
            mCameraLogger.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null != mCameraDevice) {
            mCameraDevice.stopPreview();
            mCameraDevice.setPreviewCallback(null);
            mCameraDevice.release();
            mCameraDevice = null;
        }

        try {
            mCameraLogger.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // TANGO
    private TangoConfig setTangoConfig() {

        TangoConfig outTangoConfig = mTangoDevice.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);

        // TODO: ADD TO CONFIGURATION

        // MOTION TRACKING
        outTangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        outTangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_HIGH_RATE_POSE, true);
        outTangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);
        outTangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_SMOOTH_POSE, true);

        // ACTIVATE CAMERA
        outTangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);

        // DEPTH
        outTangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        outTangoConfig.putInt(TangoConfig.KEY_INT_RUNTIME_DEPTH_FRAMERATE, 5);
        outTangoConfig.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);

        return outTangoConfig;
    }

    private void startTango() {

        findViewById(R.id.cameraView).setVisibility(View.INVISIBLE);

        mTangoDevice = new Tango(this, new Runnable() {
            @Override
            public void run() {
                synchronized (LoggingActivity.this) {
                    mTangoConfig = setTangoConfig();
                    mTangoDevice.connect(mTangoConfig);

                    // CONFIGURE CAMERA
                    tangoCameraPreview = (TangoCameraPreview) findViewById(R.id.tangoSurface);
                    if (tangoCameraPreview != null) {

                        tangoCameraPreview.connectToTangoCamera(mTangoDevice, TangoCameraIntrinsics.TANGO_CAMERA_COLOR);

                        mCameraId = mTangoDevice.getCameraIntrinsics(mCameraId).cameraId;
                        mCameraSize.width = mTangoDevice.getCameraIntrinsics(mCameraId).width;
                        mCameraSize.height = mTangoDevice.getCameraIntrinsics(mCameraId).height;
                        mCameraAF = Camera.Parameters.FOCUS_MODE_FIXED;
                        mOutputFormat = ImageFormat.UNKNOWN;
                    }

                    createTangoLoggers();

                    // SELECT FRAMES
                    // TODO: ADD TO CONFIGURATION
                    ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
                    framePairs.add(new TangoCoordinateFramePair(
                            TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                            TangoPoseData.COORDINATE_FRAME_DEVICE));

                    // CONNECT LISTENERS
                    mTangoDevice.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
                        @Override
                        public void onPoseAvailable(TangoPoseData tangoPoseData) {
                            if (mLoggingON) {
                                long systemTime = SystemClock.elapsedRealtimeNanos();
                                // LOG DATA
                                String eventData = systemTime + "," +
                                        tangoPoseData.timestamp + "," +
                                        tangoPoseData.translation[TangoPoseData.INDEX_TRANSLATION_X] + "," +
                                        tangoPoseData.translation[TangoPoseData.INDEX_TRANSLATION_Y] + "," +
                                        tangoPoseData.translation[TangoPoseData.INDEX_TRANSLATION_Z] + "," +
                                        tangoPoseData.rotation[TangoPoseData.INDEX_ROTATION_W] + "," +
                                        tangoPoseData.rotation[TangoPoseData.INDEX_ROTATION_X] + "," +
                                        tangoPoseData.rotation[TangoPoseData.INDEX_ROTATION_Y] + "," +
                                        tangoPoseData.rotation[TangoPoseData.INDEX_ROTATION_Z];
                                try {
                                    mTangoPoseLogger.log(eventData);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onXyzIjAvailable(TangoXyzIjData tangoXyzIjData) {

                        }

                        @Override
                        public void onFrameAvailable(int i) {
                            tangoCameraPreview.onFrameAvailable();
                            if (mLoggingON & mLogCamera) {
                                // TODO: SAVE IMAGES!
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
                            }
                        }

                        @Override
                        public void onTangoEvent(TangoEvent tangoEvent) {

                        }

                        @Override
                        public void onPointCloudAvailable(TangoPointCloudData tangoPointCloudData) {
                            if (mLoggingON) {
                                long systemTime = SystemClock.elapsedRealtimeNanos();
                                // CREATE NAMES
                                String depthName = "depth_" + systemTime + ".csv";
                                // LOG DATA
                                String eventData = systemTime + "," + tangoPointCloudData.timestamp + "," + depthName;
                                try {
                                    mTangoDepthLogger.log(eventData);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                DepthSaver saver = new DepthSaver();
                                saver.execute(new DepthSaverParams(tangoPointCloudData.numPoints,
                                        tangoPointCloudData.points,
                                        depthName));
                            }
                        }
                    });
                }
            }
        });
    }

    private void createTangoLoggers() {if (mLogCamera) {

        // Create Image Logging directory
        mCameraLoggingDir = new File(mLoggingDir.getPath() + "/images_" + mCameraSize.width + "x" + mCameraSize.height);
        try {
            mCameraLoggingDir.mkdirs();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

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
                mCameraLogger.log("// SYSTEM_TIME [ns], EVENT_TIMESTAMP [s], IMG_NAME");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

        // Create Tango Pose Logger
        try {
            mTangoPoseLogger = new Logger(mLoggingDir.getPath() + "/sensor_TANGO_POSE_log.csv");
            try {
                mTangoPoseLogger.log("// SYSTEM_TIME [ns], EVENT_TIMESTAMP [s], POSE_VALUES");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Create Depth Logging directory
        mDepthLoggingDir = new File(mLoggingDir.getPath() + "/depth");
        try {
            mDepthLoggingDir.mkdirs();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // Create Tango Depth Logger
        try {
            mTangoDepthLogger = new Logger(mLoggingDir.getPath() + "/sensor_TANGO_DEPTH_log.csv");
            try {
                mTangoDepthLogger.log("// SYSTEM_TIME [ns], EVENT_TIMESTAMP [s], FILENAME");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private class DepthSaverParams {
        int nPoints;
        FloatBuffer data;
        String depthName;

        DepthSaverParams(int innPoints, FloatBuffer inData, String inDepthName) {
            this.nPoints = innPoints;
            this.data = inData.duplicate();
            this.depthName = inDepthName;
        }
    }

    private class DepthSaver extends AsyncTask<DepthSaverParams, Void, Boolean> {
        @Override
        protected Boolean doInBackground(DepthSaverParams... params) {
            DepthSaverParams depthData = params[0];
            String depthFileName = mDepthLoggingDir.getPath() + "/" + depthData.depthName;
            // SAVE DEPTH DATA
            try {
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(depthFileName));
                String toWrite = depthData.nPoints+ System.lineSeparator();
                for (int i = 0; i < depthData.data.capacity(); i++) {
                     toWrite += depthData.data.get(i) + ",";
                }
                toWrite += System.lineSeparator();
                stream.write(toWrite.getBytes());
                stream.flush();
                stream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private void closeTango() {

        // Close loggers
        if (mLogCamera) {
            try {
                mCameraLogger.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            mTangoDepthLogger.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mTangoPoseLogger.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mTangoDevice.disconnect();

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

                string = "CAMERA_ID, " + mCameraId + System.lineSeparator();
                outputStream.write(string.getBytes());
                string = "CAMERA_RESOLUTION, " + mCameraSize.width + "x" + mCameraSize.height + System.lineSeparator();
                outputStream.write(string.getBytes());
                string = "CAMERA_AF_MODE, " + CameraSettingsActivity.getAFModeName(mCameraAF) + System.lineSeparator();
                outputStream.write(string.getBytes());
                string = "CAMERA_OUTPUT_FORMAT," + CameraSettingsActivity.getOutputFormatName(mOutputFormat) + System.lineSeparator();
                outputStream.write(string.getBytes());

                string = "CAMERA_N_IMAGES, " + (mCameraLogger.getnLogs() - 1) + " [" + ((float) (mCameraLogger.getnLogs() - 1) / sessionTime) + " Hz]" + System.lineSeparator();
                outputStream.write(string.getBytes());
            }

            if (mLogTango) {

                string = "POSE_N_READINGS, " + (mTangoPoseLogger.getnLogs() - 1) + " [" + ((float) (mTangoPoseLogger.getnLogs() - 1) / sessionTime) + " Hz]" + System.lineSeparator();
                outputStream.write(string.getBytes());

                string = "DEPTH_N_READINGS, " + (mTangoDepthLogger.getnLogs() - 1) + " [" + ((float) (mTangoDepthLogger.getnLogs() - 1) / sessionTime) + " Hz]" + System.lineSeparator();
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

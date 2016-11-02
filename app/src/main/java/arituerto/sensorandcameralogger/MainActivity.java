package arituerto.sensorandcameralogger;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    // Logging data
    private boolean mLoggingActive = false;
    private boolean mCameraLogging = false;
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
        startBackgroundThread();
        openCamera();

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
    protected void onDestroy() {

        Log.i(TAG, " onDestroy");

        super.onDestroy();
        closeCamera();
        stopBackgroundThread();
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

    // SENSORS FUNCTIONS
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

    private void startSensorsLogging() {

        startSensorListeners();

        EditText textEntry = (EditText) findViewById(R.id.inputDataSetName);
        dataSetName = textEntry.getText().toString();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());

        // CREATE LOGGING DIRECTORY
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

//        // CREATE IMAGES DIRECTORY
//        imageDir = new File(loggingDir.getPath() + "/images");
//        if (mCameraLogging) {
//            try {
//                imageDir.mkdirs();
//            } catch (SecurityException e) {
//                e.printStackTrace();
//            }
//        }

        // CREATE SENSOR LOGGERS
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

    private void stopSensorsLogging() {
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

    // CAMERA FUNCTIONS

    protected void startBackgroundThread() {

        Log.i(TAG, "Camera Background thread Started");

        mBackgroundThread = new HandlerThread("cameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {

        Log.i(TAG, "Camera Background thread Stopped");

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "Camera Device Opened");
            cameraDevice = camera;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.e(TAG, "Camera Device Disconnected");
            cameraDevice.close();
        }

        @Override
        public void onClosed(CameraDevice camera) {
            Log.e(TAG, "Camera Device Closed");
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "Camera Device Closed because error");
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private CameraCaptureSession.CaptureCallback captureSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.i(TAG, "onCaptureCompleted");
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.i(TAG, "onCaptureFailed");
        }
    };

    private CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                session.capture(captureRequestBuilder.build(), captureSessionCaptureCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    };

    protected void startCameraAcquisition() {

        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());

            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>();
            outputSurfaces.add(reader.getSurface());

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureRequestBuilder.addTarget(reader.getSurface());

            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.i(TAG,"onImageAvailable");
                }
            };

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            cameraDevice.createCaptureSession(outputSurfaces, captureSessionStateCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
                return;
            }
            manager.openCamera(cameraId, cameraStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "openCamera");
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    // BUTTONS FUNCTIONS
    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mLoggingActive) {
                Log.i(TAG, "Start Logging");
                startSensorsLogging();
                startCameraAcquisition();
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
                stopSensorsLogging();
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

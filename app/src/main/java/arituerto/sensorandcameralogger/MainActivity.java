package arituerto.sensorandcameralogger;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
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
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // TODO: Save configuration for next runs.
    // TODO: Add GPS logging
    // TODO: Write logging session description (Time, sensors, sensor delay, camera properties,...)
    // TODO: Add EXIT button or option

    private static final String TAG = "MAIN";

    static final int SENSORS_SETTINGS_REQUEST = 1;
    static final int CAMERA_SETTINGS_REQUEST = 2;
    static final int[] SENSOR_TYPES = new int[]{
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_STEP_DETECTOR};

    // SENSORS
    private SensorManager mSensorManager;
    private Map<String, Sensor> mSensorMap;
    private ArrayList<String> mNameSensorList;
    private boolean[] mSelectedSensorList;
    private Map<Sensor, Logger> mSensorLoggerMap;
    int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;

    // CAMERA
    private String mCameraId;
    private Size mImageSize;
    private Size[] mJpegSizeList;
    private ArrayList<String> mNameJpegSizeList;
    private ArrayList<String> mNameFocusModeList;
    int[] mFocusModeList;
    int mFocusMode;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCamCach;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mCameraRequestBuilder;
    private CaptureRequest mCameraRequest;
    private ImageReader mImgReader;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private Logger mCameraLogger;

    //PREVIEW SURFACE
    private Surface mPreviewSurface;
    private Surface mReaderSurface;
    private List<Surface> mSurfaceList = new ArrayList<Surface>();

    // LOGGING
    boolean sensorLoggingActive = false;
    long loggingStartTime;
    private File loggingDir;
    private File imageDir;
    private String dataSetName;

    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startCameraHandlerThread();
        getCameraParameters();
        setupSurfaces();

        //VISUAL
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);

        // SENSORS
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = new ArrayList<Sensor>();
        for (int iSensorType = 0; iSensorType < SENSOR_TYPES.length; iSensorType++) {
            List<Sensor> auxList = mSensorManager.getSensorList(SENSOR_TYPES[iSensorType]);
            for (Sensor iSensor : auxList) {
                sensorList.add(iSensor);
            }
        }
        mSensorLoggerMap = new HashMap<Sensor, Logger>();
        mNameSensorList = new ArrayList<String>();
        mSelectedSensorList = new boolean[sensorList.size()];
        mSensorMap = new HashMap<String, Sensor>();
        for (int i = 0; i < sensorList.size(); i++) {
            Sensor iSensor = sensorList.get(i);
            String sensorString = iSensor.getStringType().split("\\.")[2].toUpperCase() +
                    "\n" +
                    iSensor.getName();
            mSensorMap.put(sensorString, iSensor);
            mNameSensorList.add(sensorString);
            mSelectedSensorList[i] = true;
        }

        // CAMERA
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

        Log.i(TAG, "onDestroy");
        super.onDestroy();

        closeCamera();
        stopCameraHandlerThread();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SENSORS_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "Sensor Settings Received");
                Bundle bundle = data.getExtras();
                mSelectedSensorList = bundle.getBooleanArray("selectedSensors");
                sensorDelay = bundle.getInt("sensorDelay");
                setupSurfaces();
            }
        } else if (requestCode == CAMERA_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "Camera Settings Received");
                Bundle bundle = data.getExtras();
                int SelectedJpegSize = bundle.getInt("selectedSize");
                int SelectedFocusMode = bundle.getInt("selectedFocus");
                setCameraImageSize(mJpegSizeList[SelectedJpegSize]);
                setCameraAutoFocus(mFocusModeList[SelectedFocusMode]);
                mImgReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 10);
                mImgReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);
                mReaderSurface = mImgReader.getSurface();
                setupSurfaces();
            }
        }

    }

    // SESSION DESCRIPTION
    private void writeSessionDescription() {
        String sessionDescriptionName = loggingDir.getPath() + "/sessionDescription.txt";
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(sessionDescriptionName);

            String string;
            int aux;

            string = "DATA_SET_NAME                 " + dataSetName + System.lineSeparator();
            outputStream.write(string.getBytes());

            string = "DATA_SET_FOLDER               " + loggingDir + System.lineSeparator();
            outputStream.write(string.getBytes());

            string = "DATA_SET_TIME                 " + ((float) (SystemClock.elapsedRealtime() - loggingStartTime)/1000.0) + " [s]" + System.lineSeparator();
            outputStream.write(string.getBytes());

            string = "CAMERA_RESOLUTION             " + mImageSize.getWidth() + "x" + mImageSize.getHeight() + System.lineSeparator();
            outputStream.write(string.getBytes());

            string = "CAMERA_AF_MODE                " + mNameFocusModeList.get(mFocusMode) + System.lineSeparator();
            outputStream.write(string.getBytes());

            aux = mCamCach.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE);
            if (aux == CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN) {
                string = "CAMERA_TIMESTAMP_SOURCE    UNKNOWN" + System.lineSeparator();
            } else if (aux == CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME) {
                string = "CAMERA_TIMESTAMP_SOURCE    REALTIME" + System.lineSeparator();
            }
            outputStream.write(string.getBytes());

            if (null != mCamCach.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)) {
                string = "CAMERA_INTRINSIC_CALIBRATION    " + mCamCach.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION).toString() + System.lineSeparator();
                outputStream.write(string.getBytes());
            }

            if (null != mCamCach.get(CameraCharacteristics.LENS_POSE_TRANSLATION)) {
                string = "CAMERA_POSE_TRANSLATION         " + mCamCach.get(CameraCharacteristics.LENS_POSE_TRANSLATION).toString() + System.lineSeparator();
                outputStream.write(string.getBytes());
            }

            if (null != mCamCach.get(CameraCharacteristics.LENS_POSE_ROTATION)) {
                string = "CAMERA_POSE_ROTATION            " + mCamCach.get(CameraCharacteristics.LENS_POSE_ROTATION).toString() + System.lineSeparator();
                outputStream.write(string.getBytes());
            }

            string = "N_IMAGES                      " + (mCameraLogger.getnLogs()-1) + System.lineSeparator();
            outputStream.write(string.getBytes());

            for (Map.Entry<Sensor, Logger> iSensorLogger : mSensorLoggerMap.entrySet()) {
                string = "SENSOR_NAME               " + iSensorLogger.getKey().getName() + System.lineSeparator();
                outputStream.write(string.getBytes());
                string = "N_READINGS                " + (iSensorLogger.getValue().getnLogs()-1) + System.lineSeparator();
                outputStream.write(string.getBytes());
            }

            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // SENSORS FUNCTIONS
    private void startSensorListeners() {
        for (int iSensor = 0; iSensor < mSelectedSensorList.length; iSensor++) {
            if (mSelectedSensorList[iSensor]) {
                mSensorManager.registerListener(this,
                        mSensorMap.get(mNameSensorList.get(iSensor)),
                        sensorDelay);
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

        loggingStartTime = SystemClock.elapsedRealtime();

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

        // CREATE IMAGES DIRECTORY
        imageDir = new File(loggingDir.getPath() + "/images_" + mImageSize.getWidth() + "x" + mImageSize.getHeight());
        try {
            imageDir.mkdirs();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // CREATE CAMERA LOGGER
        // First line: Data description
        try {
            mCameraLogger = new Logger(loggingDir.getPath() + "/sensor_CAMERA_log.csv");
            try {
                mCameraLogger.log("// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ns], IMG_NAME");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // CREATE SENSOR LOGGERS
        String loggerFileName;
        for (int iSensor = 0; iSensor < mSelectedSensorList.length; iSensor++) {
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
        writeSessionDescription();
        try {
            mCameraLogger.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        if (sensorLoggingActive) {
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

    // CAMERA FUNCTIONS

    private void startCameraHandlerThread() {
        mHandlerThread = new HandlerThread("Camera Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private void stopCameraHandlerThread() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandlerThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void getCameraParameters() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = cameraManager.getCameraIdList()[0]; // TODO: Function to get REAR camera
            mCamCach = cameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap streamMap = mCamCach.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mJpegSizeList = streamMap.getOutputSizes(ImageFormat.JPEG);
            mNameJpegSizeList = new ArrayList<String>();
            for (Size i : mJpegSizeList) {
                String resolution = i.getWidth() + "x" + i.getHeight();
                mNameJpegSizeList.add(resolution);
            }
            setCameraImageSize(mJpegSizeList[0]);
            mFocusModeList = mCamCach.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            mNameFocusModeList = new ArrayList<String>();
            for (int i = 0; i < mFocusModeList.length; i++) {
                if (mFocusModeList[i] == CaptureRequest.CONTROL_AF_MODE_OFF) {
                    mNameFocusModeList.add("OFF");
                } else if(mFocusModeList[i] == CaptureRequest.CONTROL_AF_MODE_AUTO) {
                    mNameFocusModeList.add("AUTO");
                } else if(mFocusModeList[i] == CaptureRequest.CONTROL_AF_MODE_MACRO) {
                    mNameFocusModeList.add("MACRO");
                } else if(mFocusModeList[i] == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO) {
                    mNameFocusModeList.add("CONTINUOS VIDEO");
                } else if(mFocusModeList[i] == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {
                    mNameFocusModeList.add("CONTINUOUS PICTURE");
                } else if(mFocusModeList[i] == CaptureRequest.CONTROL_AF_MODE_EDOF) {
                    mNameFocusModeList.add("EDOF");
                } else {
                    Log.i("AF MODES", "mode Unknown");
                }
            }
            setCameraAutoFocus(mFocusModeList[0]);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setCameraImageSize(Size imgSize) {
        mImageSize = imgSize;
    }

    private void setCameraAutoFocus(int focusMode) {
        mFocusMode = focusMode;
    }

    // setupSurfaces() calls setupCamera() when the surface is ready that calls setupCaptureSession()
    // when the camera is ready
    private void setupSurfaces() {

        // IMAGE READER
        // TODO: CHECK FORMAT AVAILABLE FROM CAMERA
        mImgReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), PixelFormat.RGBA_8888, 1);
        mImgReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);
        mReaderSurface = mImgReader.getSurface();

        // PREVIEW SURFACE
        TextureView textureView = (TextureView) findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mPreviewSurface = new Surface(surface);
                if (null != surface) {
                    setupCamera();
                }
                Log.i("TextureView", "Surface Available");
                Log.i("TextureView", "Surface size" + width + "x" + height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    private void setupCamera() {

        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        //Find the right camera device
        try {
            mCameraId = mCameraManager.getCameraIdList()[0];
            if (mCameraId != null) {
                try {
                    //Try to open the camera
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                        return;
                    }
                    mCameraManager.openCamera(mCameraId, mCameraStateCallback, mHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            } else {
                Log.i("setupCamera", "Unable to access camera!");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {

        if (null != mCaptureSession) {
            mCaptureSession.close();
            // mCaptureSession = null;
        }

        if (null != mCameraDevice) {
            mCameraDevice.close();
            // mCameraDevice = null;
        }

        if (null != mImgReader) {
            mImgReader.close();
            // mImgReader = null;
        }

    }

    private void setupCaptureSession() throws CameraAccessException {
        mCameraRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mCameraRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mFocusMode);
        mCameraRequestBuilder.addTarget(mPreviewSurface);
        mCameraRequestBuilder.addTarget(mReaderSurface);
        mSurfaceList.clear();
        mSurfaceList.add(mPreviewSurface);
        mSurfaceList.add(mReaderSurface);
        mCameraDevice.createCaptureSession(mSurfaceList, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                mCaptureSession = cameraCaptureSession;
                try {
                    mCameraRequest = mCameraRequestBuilder.build();
                    mCaptureSession.setRepeatingRequest(mCameraRequest, mSessionCaptureCallback, mHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            }
        }, null);
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image img = null;
            try {
                img = reader.acquireNextImage();
                if (null != img) {
                    if (sensorLoggingActive) {
                        // CREATE NAMES
                        String imgName = "img_" + img.getTimestamp() + ".jpg";
                        String imgFileName = imageDir.getPath() + "/" + imgName;
                        // LOG DATA
                        String eventData = SystemClock.elapsedRealtimeNanos() + "," + img.getTimestamp() + "," + imgName;
                        try {
                            mCameraLogger.log(eventData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        processImage(img, imgFileName);
                    }
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                Log.e("ImageReader", "No more buffers available, skipping frame");
            }
            img.close();
        }
    };

    private void processImage(Image image, String imgFileName) {
        // TODO: SAVE IMAGE
        Bitmap bitmap;
        Image.Plane[] planes = image.getPlanes();
        Buffer buffer = planes[0].getBuffer().rewind();
        bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        try {
            FileOutputStream out = new FileOutputStream(imgFileName);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d("CameraDevice", "Camera device opened!");
            mCameraDevice = camera;
            try {
                setupCaptureSession();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d("CameraDevice", "Camera device disconnected!");
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d("CameraDevice", "Camera device error: " + error);
        }
    };

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {}
    };

    // BUTTONS FUNCTIONS
    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!sensorLoggingActive) {
                Log.i(TAG, "Start Logging");
                startLogging();
                sensorLoggingActive = true;
                mProgressBar.setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "System is already Logging");
            }
        }
    };

    private View.OnClickListener stopClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (sensorLoggingActive) {
                Log.i(TAG, "Stop Logging");
                stopLogging();
                sensorLoggingActive = false;
                mProgressBar.setVisibility(View.GONE);
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
            closeCamera();
            startActivityForResult(intent, SENSORS_SETTINGS_REQUEST);
        }
    };

    private View.OnClickListener cameraSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Camera Settings");
            Intent intent = new Intent(MainActivity.this, CameraSettingsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("sizeName", mNameJpegSizeList);
            bundle.putStringArrayList("focusName", mNameFocusModeList);
            intent.putExtras(bundle);
            closeCamera();
            startActivityForResult(intent, CAMERA_SETTINGS_REQUEST);
        }
    };
}

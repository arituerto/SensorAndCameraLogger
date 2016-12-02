package arituerto.sensorandcameralogger;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBleService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private SensorManager mSensorManager;
    List<Sensor> mSensorList;
    private boolean[] mSelectedSensorList;
    private Map<Sensor, Logger> mSensorLoggerMap = new HashMap<Sensor, Logger>();
    private int mSensorDelay;

    private String mCameraId;
    private Size mCameraSize;
    private int mCameraAF;
    private int mOutputFormat;
    private File mCameraLoggingDir;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mCameraRequestBuilder;
    private CaptureRequest mCameraRequest;
    private ImageReader mImgReader;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Logger mCameraLogger;


    private String mCPRO_Rmac;
    private String mCPRO_Lmac;
    private MetaWearBleService.LocalBinder mServiceBinder;
    private CPROboardLog mRboard;
    private CPROboardLog mLboard;

    //PREVIEW SURFACE
    private Surface mPreviewSurface;
    private Surface mReaderSurface;
    private List<Surface> mSurfaceList = new ArrayList<Surface>();

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
                    Toast.makeText(getApplicationContext(), "LOGGING", Toast.LENGTH_LONG);
                    v.setVisibility(View.INVISIBLE);
                    loggingSpinner.setVisibility(View.VISIBLE);
                    startLogging();
                }
            }
        });

        // Get configuration data
        Bundle inBundle = this.getIntent().getExtras();
        readInBundle(inBundle);

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

        startCameraHandlerThread();
        setupSurfaces(); // Chain reaction

        if (mLogCamera) {

            cameraText.setText("Camera connecting");

            // Create Image Logging directory
            mCameraLoggingDir = new File(mLoggingDir.getPath() + "/images_" + mCameraSize.getWidth() + "x" + mCameraSize.getHeight());
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

    private void readInBundle(Bundle inBundle) {

        mDataSetName = inBundle.getString("dataSetName");

        mLogSensor = inBundle.getBoolean("LogSensor");
        mSensorDelay = inBundle.getInt("SensorDelay");
        mSelectedSensorList = inBundle.getBooleanArray("SensorSelection");

        mLogCamera = inBundle.getBoolean("LogCamera");
        mCameraId = inBundle.getString("CameraId");
        mCameraSize = inBundle.getSize("CameraSize");
        mCameraAF = inBundle.getInt("CameraAF");
        mOutputFormat = inBundle.getInt("OutputFormat");

        mLogCPRO = inBundle.getBoolean("LogCPRO");
        mCPRO_Rmac = inBundle.getString("CPRORmac");
        mCPRO_Lmac = inBundle.getString("CPROLmac");

    }

    // START/STOP LOGGING
    private void startLogging() {

        mStartLoggingTime = SystemClock.elapsedRealtimeNanos();

        mLoggingON = true;

        if (mLogCPRO) {
            mRboard.activateLogging();
            mLboard.activateLogging();
        }

        Log.i(TAG, "Logging START");
        Toast.makeText(getApplicationContext(), "LOGGING", Toast.LENGTH_LONG);
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
            stopCameraHandlerThread();
        }

        Log.i(TAG, "Logging STOP");
        Toast.makeText(getApplicationContext(), "LOGGING STOPPED", Toast.LENGTH_LONG);

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

    // CPRO Sensor functions
    @Override
    public void onCPROConfigured(int state, String boardID) {

        switch (boardID) {
            case "CPRO_R":
                if (state == 1) {
                    cproRText.setText("CPRO R connected");
                } else if (state == 2) {
                    cproRText.setText("CPRO R connection failed");
                }
                break;
            case "CPRO_L":
                if (state == 1) {
                    cproLText.setText("CPRO L connected");
                } else if (state == 2) {
                    cproLText.setText("CPRO L connection failed");
                }
                break;

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
    private void startCameraHandlerThread() {
        mHandlerThread = new HandlerThread("Camera Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private void stopCameraHandlerThread() {
        if (null != mHandlerThread) {
            mHandlerThread.quitSafely();
            try {
                mHandlerThread.join();
                mHandlerThread = null;
                mHandlerThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupSurfaces() {

        Log.i(TAG, "setupSurfaces");

        // IMAGE READER
        if (mLogCamera) {
            // TODO: Solve Image format issue
//        mImgReader = ImageReader.newInstance(mCameraSize.getWidth(), mCameraSize.getHeight(), mOutputFormat, 10);
            mImgReader = ImageReader.newInstance(mCameraSize.getWidth(), mCameraSize.getHeight(), ImageFormat.JPEG, 15);
            mImgReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);
            mReaderSurface = mImgReader.getSurface();
        }

        // PREVIEW SURFACE
        TextureView textureView = (TextureView) findViewById(R.id.textureViewLogging);
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

        Log.i(TAG, "setupCamera");

        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            //Try to open the camera
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return;
            }
            mCameraManager.openCamera(mCameraId, mCameraStateCallback, mHandler);
            if (mLogCamera) {
                cameraText.setText("Camera connected");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {

        Log.i(TAG, "closeCamera");

        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }

        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (null != mImgReader) {
            mImgReader.close();
            mImgReader = null;
        }

    }

    private void setupCaptureSession() throws CameraAccessException {

        Log.i(TAG, "setupCaptureSession");

        mCameraRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mCameraRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mCameraAF);
        mCameraRequestBuilder.addTarget(mPreviewSurface);
        if (mLogCamera) {
            mCameraRequestBuilder.addTarget(mReaderSurface);
        }
        mSurfaceList.clear();
        mSurfaceList.add(mPreviewSurface);
        if (mLogCamera) {
            mSurfaceList.add(mReaderSurface);
        }
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
                    if (mLoggingON) {
                        // CREATE NAMES
                        String imgName = "img_" + img.getTimestamp() + ".jpg";
                        String imgFileName = mCameraLoggingDir.getPath() + "/" + imgName;
                        // LOG DATA
                        String eventData = SystemClock.elapsedRealtimeNanos() + "," + img.getTimestamp() + "," + imgName;
                        try {
                            mCameraLogger.log(eventData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        processImage(img, imgFileName);
                    } else {
                        img.close();
                    }
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                Log.e("ImageReader", "No more buffers available, skipping frame");
            }
        }
    };

    private void processImage(Image image, String imgFileName) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        image.close();
        try {
            FileOutputStream out = new FileOutputStream(imgFileName);
            out.write(bytes);
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

                string = "CAMERA_RESOLUTION, " + mCameraSize.getWidth() + "x" + mCameraSize.getHeight() + System.lineSeparator();
                outputStream.write(string.getBytes());

                switch (mCameraAF) {
                    case (CameraCharacteristics.CONTROL_AF_MODE_AUTO):
                        string = "CAMERA_AF_MODE, CONTROL_AF_MODE_AUTO" + System.lineSeparator();
                        break;
                    case (CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE):
                        string = "CAMERA_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE" + System.lineSeparator();
                        break;
                    case (CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO):
                        string = "CAMERA_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE" + System.lineSeparator();
                        break;
                    case (CameraCharacteristics.CONTROL_AF_MODE_EDOF):
                        string = "CAMERA_AF_MODE, CONTROL_AF_MODE_EDOF" + System.lineSeparator();
                        break;
                    case (CameraCharacteristics.CONTROL_AF_MODE_MACRO):
                        string = "CAMERA_AF_MODE, CONTROL_AF_MODE_MACRO" + System.lineSeparator();
                        break;
                    case (CameraCharacteristics.CONTROL_AF_MODE_OFF):
                        string = "CAMERA_AF_MODE, CONTROL_AF_MODE_OFF" + System.lineSeparator();
                        break;
                }
                outputStream.write(string.getBytes());

                string = "CAMERA_OUTPUT_FORMAT," + CameraSettingsActivity.getOutputFormatName(mOutputFormat) + System.lineSeparator();
                outputStream.write(string.getBytes());

                CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(mCameraId);
                switch (cc.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE)) {
                    case (CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN):
                        string = "CAMERA_TIMESTAMP_SOURCE, SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN" + System.lineSeparator();
                        break;
                    case (CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME):
                        string = "CAMERA_TIMESTAMP_SOURCE, SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME" + System.lineSeparator();
                        break;
                }
                outputStream.write(string.getBytes());

                if (null != cc.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)) {
                    string = "CAMERA_INTRINSIC_CALIBRATION, " + cc.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION).toString() + System.lineSeparator();
                    outputStream.write(string.getBytes());
                }

                if (null != cc.get(CameraCharacteristics.LENS_POSE_TRANSLATION)) {
                    string = "CAMERA_POSE_TRANSLATION, " + cc.get(CameraCharacteristics.LENS_POSE_TRANSLATION).toString() + System.lineSeparator();
                    outputStream.write(string.getBytes());
                }

                if (null != cc.get(CameraCharacteristics.LENS_POSE_ROTATION)) {
                    string = "CAMERA_POSE_ROTATION, " + cc.get(CameraCharacteristics.LENS_POSE_ROTATION).toString() + System.lineSeparator();
                    outputStream.write(string.getBytes());
                }

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

package arituerto.sensorandcameralogger;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
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

public class LoggingActivity extends AppCompatActivity implements SensorEventListener{

    // TODO: Implement a safe way of ending the activity

    private static String TAG = "LoggingActivity";

    private boolean mLoggingON;
    private long mStartLoggingTime;
    private File mLoggingDir;
    private String mDataSetName;

    private boolean mLogSensor;
    private boolean mLogSensorReady = !mLogSensor;
    private boolean mLogCamera;
    private boolean mLogCameraReady = !mLogCamera;
    private boolean mLogCPRO = false;
    private boolean mLogCPROReady = !mLogCPRO;
    private boolean mLogGPS = false;
    private boolean mLogGPSReady = !mLogGPS;

    private SensorManager mSensorManager;
    List<Sensor> mSensorList;
    private boolean[] mSelectedSensorList;
    private Map<Sensor, Logger> mSensorLoggerMap = new HashMap<Sensor, Logger>();
    private int mSensorDelay;

    private String mCameraId;
    private Size mCameraSize;
    private int mCameraAF;
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

    //PREVIEW SURFACE
    private Surface mPreviewSurface;
    private Surface mReaderSurface;
    private List<Surface> mSurfaceList = new ArrayList<Surface>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);

        // Get configuration data
        Bundle inBundle = this.getIntent().getExtras();

        mDataSetName = inBundle.getString("dataSetName");

        mLogSensor = inBundle.getBoolean("LogSensor");
        mSensorDelay = inBundle.getInt("SensorDelay");
        mSelectedSensorList = inBundle.getBooleanArray("SensorSelection");

        mLogCamera = inBundle.getBoolean("LogCamera");
        mCameraId = inBundle.getString("CameraId");
        mCameraSize = inBundle.getSize("CameraSize");
        mCameraAF = inBundle.getInt("CameraAF");

//        mLogCPRO = inBundle.getBoolean("LogCPRO");
//        mLogGPS = inBundle.getBoolean("LogGPS");

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

        if (mLogCamera) {
            startCameraHandlerThread();
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
            setupSurfaces(); // Chain reaction
            mLogCameraReady = true;
            startLogging();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLoggingON = false;
        writeSessionDescription();
        if (mLogSensor) {
            stopSensorListeners();
        }
        if (mLogCamera) {
            closeCamera();
            stopCameraHandlerThread();
        }
        Log.i(TAG, "Logging STOP");
    }

    // SESSION DESCRIPTION
    private void writeSessionDescription() {
        String sessionDescriptionName = mLoggingDir.getPath() + "/sessionDescription.txt";
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(sessionDescriptionName);

            String string;

            string = "DATA_SET_NAME                 " + mDataSetName + System.lineSeparator();
            outputStream.write(string.getBytes());

            string = "DATA_SET_FOLDER               " + mLoggingDir + System.lineSeparator();
            outputStream.write(string.getBytes());

            string = "DATA_SET_TIME                 " + ((float) (SystemClock.elapsedRealtimeNanos() - mStartLoggingTime)/1000000000.0) + " [s]" + System.lineSeparator();
            outputStream.write(string.getBytes());

            string = "CAMERA_RESOLUTION             " + mCameraSize.getWidth() + "x" + mCameraSize.getHeight() + System.lineSeparator();
            outputStream.write(string.getBytes());

//            string = "CAMERA_AF_MODE                " + mNameFocusModeList.get(mFocusMode) + System.lineSeparator();
//            outputStream.write(string.getBytes());

//            aux = mCamCach.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE);
//            if (aux == CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN) {
//                string = "CAMERA_TIMESTAMP_SOURCE    UNKNOWN" + System.lineSeparator();
//            } else if (aux == CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME) {
//                string = "CAMERA_TIMESTAMP_SOURCE    REALTIME" + System.lineSeparator();
//            }
//            outputStream.write(string.getBytes());
//
//            if (null != mCamCach.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)) {
//                string = "CAMERA_INTRINSIC_CALIBRATION    " + mCamCach.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION).toString() + System.lineSeparator();
//                outputStream.write(string.getBytes());
//            }
//
//            if (null != mCamCach.get(CameraCharacteristics.LENS_POSE_TRANSLATION)) {
//                string = "CAMERA_POSE_TRANSLATION         " + mCamCach.get(CameraCharacteristics.LENS_POSE_TRANSLATION).toString() + System.lineSeparator();
//                outputStream.write(string.getBytes());
//            }
//
//            if (null != mCamCach.get(CameraCharacteristics.LENS_POSE_ROTATION)) {
//                string = "CAMERA_POSE_ROTATION            " + mCamCach.get(CameraCharacteristics.LENS_POSE_ROTATION).toString() + System.lineSeparator();
//                outputStream.write(string.getBytes());
//            }

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

    // START LOGGING IF EVERYTHING IS READY
    private void startLogging() {
        mLoggingON = (mLogSensorReady & mLogCameraReady & mLogCPROReady & mLogGPSReady);
        if (mLoggingON) {
            mStartLoggingTime = SystemClock.elapsedRealtimeNanos();
            Log.i(TAG, "Logging START");
        }
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
        if (null != mSensorManager) {
            mSensorManager.unregisterListener(this);
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

    // setupSurfaces() calls setupCamera() when the surface is ready that calls setupCaptureSession()
    // when the camera is ready
    private void setupSurfaces() {

        // IMAGE READER
        // TODO: CHECK FORMAT AVAILABLE FROM CAMERA
        mImgReader = ImageReader.newInstance(mCameraSize.getWidth(), mCameraSize.getHeight(), PixelFormat.RGBA_8888, 1);
        mImgReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);
        mReaderSurface = mImgReader.getSurface();

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
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        //Find the right camera device
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
    }

    private void closeCamera() {

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
        mCameraRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mCameraRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mCameraAF);
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

}

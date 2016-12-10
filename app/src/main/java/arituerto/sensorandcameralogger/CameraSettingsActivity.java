package arituerto.sensorandcameralogger;

import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.List;

public class CameraSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CameraSettings";

    public static String SHRDPRFS_NAME = "CameraPrefs";
    public static String CAMID = "camID";
    public static String SIZE = "size";
    public static String FORMAT = "format";
    public static String FOCUS = "afMode";

    private SharedPreferences sharedPreferences;

    private List<Integer> mOutputFormatsList;
    private int mOutFormat;

    private int mNCameras;
    private int mCameraId;

    private List<Camera.Size> mImageSizeList;
    private int mImageSizePos;

    private List<String> mFocusModeList;
    private String mFocusMode;

    public static String getOutputFormatName(int i) {
        String outString = "";
        switch (i) {
            case (ImageFormat.DEPTH16): outString = "DEPTH16"; break;
            case (ImageFormat.DEPTH_POINT_CLOUD): outString = "DEPTH_POINT_CLOUD"; break;
            case (ImageFormat.FLEX_RGB_888): outString = "FLEX_RGB_888"; break;
            case (ImageFormat.FLEX_RGBA_8888): outString = "FLEX_RGBA_8888"; break;
            case (ImageFormat.JPEG): outString = "JPEG"; break;
            case (ImageFormat.NV16): outString = "NV16"; break;
            case (ImageFormat.NV21): outString = "NV21"; break;
            case (ImageFormat.PRIVATE): outString = "PRIVATE"; break;
            case (ImageFormat.RAW10): outString = "RAW10"; break;
            case (ImageFormat.RAW12): outString = "RAW12"; break;
            case (ImageFormat.RAW_SENSOR): outString = "RAW_SENSOR"; break;
            case (ImageFormat.RGB_565): outString = "RGB_565"; break;
            case (ImageFormat.UNKNOWN): outString = "UNKNOWN"; break;
            case (ImageFormat.YUV_420_888): outString = "YUV_420_888"; break;
            case (ImageFormat.YUV_422_888): outString = "YUV_422_888"; break;
            case (ImageFormat.YUY2): outString = "YUY2"; break;
            case (ImageFormat.YV12): outString = "YV12"; break;
            case (PixelFormat.RGBA_8888): outString = "RGBA_8888"; break;
            case (PixelFormat.RGBX_8888): outString = "RGBX_8888"; break;
            case (PixelFormat.TRANSLUCENT): outString = "TRANSLUCENT"; break;
            case (PixelFormat.TRANSPARENT): outString = "TRANSPARENT"; break;
            case (PixelFormat.RGB_888): outString = "RGB_888"; break;
            default: outString = "UNSUPPORTED"; break;
        }

        return outString;
    }

    public static String getAFModeName(String i) {
        String outString = "";
        switch (i) {
            case (Camera.Parameters.FOCUS_MODE_AUTO): outString = "AUTO"; break;
            case (Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE): outString = "CONTINUOUS_PICTURE"; break;
            case (Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO): outString = "CONTINUOUS_VIDEO"; break;
            case (Camera.Parameters.FOCUS_MODE_MACRO): outString = "MACRO"; break;
            case (Camera.Parameters.FOCUS_MODE_EDOF): outString = "EDOF"; break;
            case (Camera.Parameters.FOCUS_MODE_FIXED): outString = "FIXED"; break;
            case (Camera.Parameters.FOCUS_MODE_INFINITY): outString = "INFINITY"; break;
            default: outString = "UNSUPPORTED"; break;
        }
        return outString;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_settings);

        sharedPreferences = getSharedPreferences(SHRDPRFS_NAME, MODE_PRIVATE);

        mCameraId = sharedPreferences.getInt(CAMID, 0);
        mOutFormat = sharedPreferences.getInt(FORMAT, -1);
        mImageSizePos = sharedPreferences.getInt(SIZE, -1);
        mFocusMode = sharedPreferences.getString(FOCUS, null);

        mNCameras = Camera.getNumberOfCameras();
        configureCameraIdSpinner();

        final Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(okClick);
    }

    private View.OnClickListener okClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.i(TAG, "Camera Settings OK");

            sharedPreferences.edit().putInt(CAMID, mCameraId).commit();
            sharedPreferences.edit().putInt(FORMAT, mOutFormat).commit();
            sharedPreferences.edit().putInt(SIZE, mImageSizePos).commit();
            sharedPreferences.edit().putString(FOCUS, mFocusMode).commit();

            finish();
        }
    };

    private void configureCameraIdSpinner() {

        String[] mCameraIdList = new String[mNCameras];
        for (int i = 0; i < mNCameras; i++) {
            mCameraIdList[i] = "CAMERA " + i;
        }
        Spinner camIdSpinner = (Spinner) findViewById(R.id.spinnerCamId);
        ArrayAdapter camIdAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, mCameraIdList);
        camIdSpinner.setAdapter(camIdAdapter);
        camIdSpinner.setSelection(mCameraId);
        setCameraId(mCameraId);
        camIdSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setCameraId(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setCameraId(int camId) {

        if (mCameraId != camId) {
            Log.i(TAG, "Camera Selected " + camId);
            mCameraId = camId;
            mOutFormat = -1;
            mImageSizePos = -1;
            mFocusMode = null;
        }

        try {
            Camera camera = Camera.open(camId);
            mOutputFormatsList = camera.getParameters().getSupportedPictureFormats();
            mImageSizeList = camera.getParameters().getSupportedPictureSizes();
            mFocusModeList = camera.getParameters().getSupportedFocusModes();
            camera.release();
            configureOutputFormatSpinner();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureOutputFormatSpinner() {

        String[] outFormatStringList = new String[mOutputFormatsList.size()];
        for (int i = 0; i < mOutputFormatsList.size(); i++) {
            outFormatStringList[i] = getOutputFormatName(mOutputFormatsList.get(i));
        }

        Spinner outFormatSpinner = (Spinner) findViewById(R.id.spinnerOutputFormat);
        ArrayAdapter outFormatAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, outFormatStringList);
        outFormatSpinner.setAdapter(outFormatAdapter);
        if (mOutFormat >= 0) {
            outFormatSpinner.setSelection(outFormatAdapter.getPosition(getOutputFormatName(mOutFormat)));
            setOutputFormat(mOutFormat);
        }
        outFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setOutputFormat(mOutputFormatsList.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setOutputFormat(int outFormat) {

        if (mOutFormat != outFormat) {
            Log.i(TAG, "Output format selected " + getOutputFormatName(outFormat));
            mOutFormat = outFormat;
            mImageSizePos = -1;
            mFocusMode = null;
        }

        configureCameraPropsSpinners();
    }

    private void configureCameraPropsSpinners() {

        // SIZE
        String[] camResStringList = new String[mImageSizeList.size()];
        for (int i = 0; i < mImageSizeList.size(); i++) {
            camResStringList[i] = mImageSizeList.get(i).width + "x" + mImageSizeList.get(i).height;
        }

        Spinner camResSpinner = (Spinner) findViewById(R.id.spinnerResolution);
        ArrayAdapter camResAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, camResStringList);
        camResSpinner.setAdapter(camResAdapter);
        if (mImageSizePos >= 0) {
            camResSpinner.setSelection(mImageSizePos);
        }
        camResSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "Image Size Selected " + mImageSizeList.get(position).toString());
                mImageSizePos = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // AUTO FOCUS
        String[] camAFStringList = new String[mFocusModeList.size()];
        for (int i = 0; i < mFocusModeList.size(); i++) {
            camAFStringList[i] = getAFModeName(mFocusModeList.get(i));
        }

        Spinner camAFSpinner = (Spinner) findViewById(R.id.spinnerAF);
        ArrayAdapter camAFAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, camAFStringList);
        camAFSpinner.setAdapter(camAFAdapter);
        if (mFocusMode != null) {
            camAFSpinner.setSelection(camAFAdapter.getPosition(getAFModeName(mFocusMode)));
        }
        camAFSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "AF mode Selected " + getAFModeName(mFocusModeList.get(position)));
                mFocusMode = mFocusModeList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

}

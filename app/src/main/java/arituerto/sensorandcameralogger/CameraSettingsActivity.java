package arituerto.sensorandcameralogger;

import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class CameraSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CameraSettings";

    public static String SHRDPRFS_NAME = "CameraPrefs";
    public static String CAMID = "camID";
    public static String SIZE = "size";
    public static String FORMAT = "format";
    public static String FOCUS = "afMode";

    private SharedPreferences sharedPreferences;

    private int[] mOutputFormatsList;
    private int mOutFormat;

    private String[] mCameraIdList;
    private String mCameraId;

    private Size[] mImageSizeList;
    private int mImageSizePos;

    private int[] mFocusModeList;
    private int mFocusMode;

    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics;
    private StreamConfigurationMap mStreamConfigurationMap;

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

    public static String getAFModeName(int i) {
        String outString = "";
        switch (i) {
            case (CameraCharacteristics.CONTROL_AF_MODE_AUTO): outString = "AUTO"; break;
            case (CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE): outString = "CONTINUOUS_PICTURE"; break;
            case (CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO): outString = "CONTINUOUS_VIDEO"; break;
            case (CameraCharacteristics.CONTROL_AF_MODE_MACRO): outString = "MACRO"; break;
            case (CameraCharacteristics.CONTROL_AF_MODE_EDOF): outString = "EDOF"; break;
            case (CameraCharacteristics.CONTROL_AF_MODE_OFF): outString = "OFF"; break;
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

        mCameraId = sharedPreferences.getString(CAMID, "0");
        mOutFormat = sharedPreferences.getInt(FORMAT, -1);
        mImageSizePos = sharedPreferences.getInt(SIZE, -1);
        mFocusMode = sharedPreferences.getInt(FOCUS, -1);

        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            mCameraIdList = mCameraManager.getCameraIdList();
            configureCameraIdSpinner();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        final Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(okClick);
    }

    private View.OnClickListener okClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.i(TAG, "Camera Settings OK");

            sharedPreferences.edit().putString(CAMID, mCameraId).commit();
            sharedPreferences.edit().putInt(FORMAT, mOutFormat).commit();
            sharedPreferences.edit().putInt(SIZE, mImageSizePos).commit();
            sharedPreferences.edit().putInt(FOCUS, mFocusMode).commit();

            finish();
        }
    };

    private void configureCameraIdSpinner() {

        Spinner camIdSpinner = (Spinner) findViewById(R.id.spinnerCamId);
        ArrayAdapter camIdAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, mCameraIdList);
        camIdSpinner.setAdapter(camIdAdapter);
        if (mCameraId != null) {
            camIdSpinner.setSelection(camIdAdapter.getPosition(mCameraId));
            setCameraId(mCameraId);
        }
        camIdSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setCameraId(mCameraIdList[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setCameraId(String camId) {

        if (!mCameraId.equals(camId)) {
            Log.i(TAG, "Camera Selected " + camId);
            mCameraId = camId;
            mOutFormat = -1;
            mImageSizePos = -1;
            mFocusMode = -1;
        }

        try {

            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            mStreamConfigurationMap = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mOutputFormatsList = mStreamConfigurationMap.getOutputFormats();

            configureOutputFormatSpinner();

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureOutputFormatSpinner() {

        String[] outFormatStringList = new String[mOutputFormatsList.length];
        for (int i = 0; i < mOutputFormatsList.length; i++) {
            outFormatStringList[i] = getOutputFormatName(mOutputFormatsList[i]);
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
                setOutputFormat(mOutputFormatsList[position]);
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
            mFocusMode = -1;
        }

        mImageSizeList = mStreamConfigurationMap.getOutputSizes(mOutFormat);
        mFocusModeList = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);

        configureCameraPropsSpinners();
    }

    private void configureCameraPropsSpinners() {

        // SIZE
        String[] camResStringList = new String[mImageSizeList.length];
        for (int i = 0; i < mImageSizeList.length; i++) {
            camResStringList[i] = mImageSizeList[i].getWidth() + "x" + mImageSizeList[i].getHeight();
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
                Log.i(TAG, "Image Size Selected " + mImageSizeList[position].toString());
                mImageSizePos = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // AUTO FOCUS
        String[] camAFStringList = new String[mFocusModeList.length];
        for (int i = 0; i < mFocusModeList.length; i++) {
            camAFStringList[i] = getAFModeName(mFocusModeList[i]);
        }

        Spinner camAFSpinner = (Spinner) findViewById(R.id.spinnerAF);
        ArrayAdapter camAFAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, camAFStringList);
        camAFSpinner.setAdapter(camAFAdapter);
        if (mFocusMode >= 0) {
            camAFSpinner.setSelection(camAFAdapter.getPosition(getAFModeName(mFocusMode)));
        }
        camAFSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "AF mode Selected " + getAFModeName(mFocusModeList[position]));
                mFocusMode = mFocusModeList[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

}

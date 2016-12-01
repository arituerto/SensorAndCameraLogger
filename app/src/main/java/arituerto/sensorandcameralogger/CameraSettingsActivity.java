package arituerto.sensorandcameralogger;

import android.app.VoiceInteractor;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

public class CameraSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CameraSettings";

    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics;
    private StreamConfigurationMap mStreamConfigurationMap;

    private int[] mOutputFormatsList;
    private int mOutFormat;
    private String[] mCameraIdList;
    private String mCameraId;
    private Size[] mImageSizeList;
    private Size mImageSize;
    private int[] mFocusModeList;
    private int mFocusMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_settings);

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
            Intent returnIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("selectedCamera", mCameraId);
            bundle.putSize("selectedSize", mImageSize);
            bundle.putInt("selectedFocus", mFocusMode);
            bundle.putInt("selectedOutFormat", mOutFormat);
            returnIntent.putExtras(bundle);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    };

    private void configureCameraIdSpinner() {
        Spinner camIdSpinner = (Spinner) findViewById(R.id.spinnerCamId);
        ArrayAdapter camIdAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, mCameraIdList);
        camIdSpinner.setAdapter(camIdAdapter);
        camIdSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setCameraId(mCameraIdList[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setCameraId(mCameraIdList[0]);
            }
        });
    }

    private void configureOutputFormatSpinner() {

        String[] outFormatStringList = new String[mOutputFormatsList.length];
        for (int i = 0; i < mOutputFormatsList.length; i++) {
            outFormatStringList[i] = getOutputFormatName(mOutputFormatsList[i]);
        }
        Spinner outFormatSpinner = (Spinner) findViewById(R.id.spinnerOutputFormat);
        ArrayAdapter outFormatAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, outFormatStringList);
        outFormatSpinner.setAdapter(outFormatAdapter);
        outFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setOutputFormat(mOutputFormatsList[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setOutputFormat(mOutputFormatsList[0]);
            }
        });
    }

    private void configureCameraPropsSpinners() {

        String[] camResStringList = new String[mImageSizeList.length];
        for (int i = 0; i < mImageSizeList.length; i++) {
            camResStringList[i] = mImageSizeList[i].getWidth() + "x" + mImageSizeList[i].getHeight();
        }
        Spinner camResSpinner = (Spinner) findViewById(R.id.spinnerResolution);
        ArrayAdapter camResAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, camResStringList);
        camResSpinner.setAdapter(camResAdapter);
        camResSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "Image Size Selected" + position);
                mImageSize = mImageSizeList[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mImageSize = mImageSizeList[0];
            }
        });

        String[] camAFStringList = new String[mFocusModeList.length];
        for (int i = 0; i < mFocusModeList.length; i++) {
            switch (mFocusModeList[i]) {
                case (CameraCharacteristics.CONTROL_AF_MODE_AUTO):
                    camAFStringList[i] = "CONTROL_AF_MODE_AUTO";
                    break;
                case (CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE):
                    camAFStringList[i] = "CONTROL_AF_MODE_CONTINUOUS_PICTURE";
                    break;
                case (CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO):
                    camAFStringList[i] = "CONTROL_AF_MODE_CONTINUOUS_VIDEO";
                    break;
                case (CameraCharacteristics.CONTROL_AF_MODE_MACRO):
                    camAFStringList[i] = "CONTROL_AF_MODE_MACRO";
                    break;
                case (CameraCharacteristics.CONTROL_AF_MODE_EDOF):
                    camAFStringList[i] = "CONTROL_AF_MODE_EDOF";
                    break;
                case (CameraCharacteristics.CONTROL_AF_MODE_OFF):
                    camAFStringList[i] = "CONTROL_AF_MODE_OFF";
                    break;
            }
        }
        Spinner camAFSpinner = (Spinner) findViewById(R.id.spinnerAF);
        ArrayAdapter camAFAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, camAFStringList);
        camAFSpinner.setAdapter(camAFAdapter);
        camAFSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "AF mode Selected" + position);
                mFocusMode = mFocusModeList[position];

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mFocusMode = mFocusModeList[0];
            }
        });
    }

    private void setCameraId(String camId) {
        Log.i(TAG, "Camera Selected " + camId);
        mCameraId = camId;
        try {
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(camId);
            mStreamConfigurationMap = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mOutputFormatsList = mStreamConfigurationMap.getOutputFormats();
            configureOutputFormatSpinner();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setOutputFormat(int outFormat) {
        Log.i(TAG, "Output format selected " + outFormat);
        mOutFormat = outFormat;
        mImageSizeList = mStreamConfigurationMap.getOutputSizes(outFormat);
        mFocusModeList = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        configureCameraPropsSpinners();
    }

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
        }

        return outString;
    }

}

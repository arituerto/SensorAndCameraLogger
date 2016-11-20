package arituerto.sensorandcameralogger;

import android.app.VoiceInteractor;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
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

import java.util.ArrayList;
import java.util.List;

public class CameraSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CameraSettings";

    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics;

    private String[] mCameraIdList;
    private String mCameraId;
    private Size[] mImageSizeList;
    private Size mImageSize;
    private int[] mFocusModeList;
    private int mFocusMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // TODO: Save LENS_POSE_ROTATION and LENS_POSE_TRANSLATION

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
        });;
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
                    camAFStringList[i] = "AUTO";
                    break;
                case (CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE):
                    camAFStringList[i] = "CONTINUOUS PICTURE";
                    break;
                case (CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO):
                    camAFStringList[i] = "CONTINUOUS VIDEO";
                    break;
                case (CameraCharacteristics.CONTROL_AF_MODE_MACRO):
                    camAFStringList[i] = "MACRO";
                    break;
                case (CameraCharacteristics.CONTROL_AF_MODE_EDOF):
                    camAFStringList[i] = "EDOF";
                    break;
                case (CameraCharacteristics.CONTROL_AF_MODE_OFF):
                    camAFStringList[i] = "OFF";
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
            StreamConfigurationMap scm = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mImageSizeList = scm.getOutputSizes(ImageFormat.JPEG);
            mFocusModeList = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            configureCameraPropsSpinners();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


}

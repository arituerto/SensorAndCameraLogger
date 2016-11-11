package arituerto.sensorandcameralogger;

import android.content.Intent;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;

public class CameraSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CameraSettings";

    private ArrayList<String> mNameJpegSizeList;
    private int mSelectedJpegSize;
    private ArrayList<String> mNameFocusModeList;
    private int mSelectedFocusMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // TODO: Save LENS_POSE_ROTATION and LENS_POSE_TRANSLATION

        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_settings);

        Bundle bundle = this.getIntent().getExtras();
        if(bundle != null) {
            mNameJpegSizeList = bundle.getStringArrayList("sizeName");
            mNameFocusModeList = bundle.getStringArrayList("focusName");
        }

        Spinner spinnerSize = (Spinner) findViewById(R.id.spinnerSize);
        ArrayAdapter<String> adapterSize = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);
        for (String i : mNameJpegSizeList) {
            adapterSize.add(i);
        }
        spinnerSize.setAdapter(adapterSize);
        spinnerSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedJpegSize = i;
                Log.i(TAG, "Size: " + mNameJpegSizeList.get(i));
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                mSelectedJpegSize = 0;
            }
        });

        Spinner spinnerFocus = (Spinner) findViewById(R.id.spinnerFocusMode);
        ArrayAdapter<String> adapterFocus = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);
        for (String i : mNameFocusModeList) {
            adapterFocus.add(i);
        }
        spinnerFocus.setAdapter(adapterFocus);
        spinnerFocus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedFocusMode = i;
                Log.i(TAG, "Focus: " + mNameFocusModeList.get(i));
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                mSelectedFocusMode = 0;
            }
        });



        final Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(okClick);
    }

    private View.OnClickListener okClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Camera Settings OK");
            Intent returnIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt("selectedSize", mSelectedJpegSize);
            bundle.putInt("selectedFocus", mSelectedFocusMode);
            returnIntent.putExtras(bundle);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    };
}

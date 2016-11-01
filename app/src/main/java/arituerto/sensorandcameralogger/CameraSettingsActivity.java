package arituerto.sensorandcameralogger;

import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class CameraSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CameraSettingsActivity";

    private CameraManager mCameraManager;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCameraCharacteristics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // TODO: Save LENS_POSE_ROTATION and LENS_POSE_TRANSLATION
        // TODO: Check time reference UNKNOWN / REALTIME

        Log.i(TAG, " onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_settings);

        Bundle bundle = this.getIntent().getExtras();
        if(bundle != null) {
            mCameraId= bundle.getString("cameraId");
        }

        final Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(okClick);
    }

    private View.OnClickListener okClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Sensor Settings OK");
            Intent returnIntent = new Intent();
            Bundle bundle = new Bundle();
            returnIntent.putExtras(bundle);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    };
}

package arituerto.sensorandcameralogger;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.projecttango.tangosupport.TangoSupport;

public class TangoSettingsActivity extends AppCompatActivity {

    private static final String TAG = "TangoSettings";

    public static String SHRDPRFS_NAME = "TangoPrefs";

    private SharedPreferences sharedPreferences;

    private Tango mTango;
    private TangoConfig mTangoConfig;

    private boolean tangoMotionTracking;
    private boolean tangoAutoRecovery;
    private boolean tangoHighRatePose;
    private boolean tangoDriftCorrection;
    private boolean tangoSmoothPose;

    private boolean tangoDepth;
    private int tangoDepthFreq;
    private int tangoDepthMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tango_settings);

        sharedPreferences = getSharedPreferences(SHRDPRFS_NAME, MODE_PRIVATE);

        mTango = new Tango(TangoSettingsActivity.this, new Runnable() {
            @Override
            public void run() {

                mTangoConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_MOTION_TRACKING);

                tangoAutoRecovery = mTangoConfig.getBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY);
                tangoMotionTracking = mTangoConfig.getBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING);
                tangoHighRatePose = mTangoConfig.getBoolean(TangoConfig.KEY_BOOLEAN_HIGH_RATE_POSE);
                tangoDriftCorrection = mTangoConfig.getBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION);
                tangoSmoothPose= mTangoConfig.getBoolean(TangoConfig.KEY_BOOLEAN_SMOOTH_POSE);

                tangoDepth = mTangoConfig.getBoolean(TangoConfig.KEY_BOOLEAN_DEPTH);
                tangoDepthFreq = mTangoConfig.getInt(TangoConfig.KEY_INT_RUNTIME_DEPTH_FRAMERATE);
                tangoDepthMode= mTangoConfig.getInt(TangoConfig.KEY_INT_DEPTH_MODE);
            }
        });

        final Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(okClick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTango.disconnect();
    }

    private View.OnClickListener okClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.i(TAG, "Camera Settings OK");

            finish();
        }
    };
}

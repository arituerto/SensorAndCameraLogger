package arituerto.sensorandcameralogger;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity::";

    // Variables for sensor reading
    private SensorManager mSensorManager;
    private List<Sensor> mSensorList;
    LinearLayout linearLayoutSensorList;
    CheckBox checkBoxSensor;

    // Logging data
    private boolean mLoggingActive;
    private File loggingDir;
    private File imageDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, " onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        linearLayoutSensorList = (LinearLayout) findViewById(R.id.ScrollViewLinearLayout);

        for (int i = 0; i < mSensorList.size(); i++) {
            checkBoxSensor = new CheckBox(this);
            checkBoxSensor.setId(i);
            checkBoxSensor.setText(mSensorList.get(i).getName());
            checkBoxSensor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, String.format("checkbox onClick, isSelected: %s, Name: %s", checkBoxSensor.isSelected(), checkBoxSensor.getText()));

                }
            });
            linearLayoutSensorList.addView(checkBoxSensor);
        }

        final Button startButton = (Button) findViewById(R.id.buttonStartLogging);
        startButton.setOnClickListener(startClick);
        final Button stopButton = (Button) findViewById(R.id.buttonStopLogging);
        stopButton.setOnClickListener(stopClick);
    }

    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, " Start Logging");
        }
    };

    private View.OnClickListener stopClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, " Stop Logging");
        }
    };

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onSensorChanged(SensorEvent event) {

    }
}

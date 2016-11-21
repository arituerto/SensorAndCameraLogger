package arituerto.sensorandcameralogger;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

public class SensorSettingsActivity extends AppCompatActivity {

    private static final String TAG = "SensorSettings";

    final int[] sensorDelayArray = new int[] {
            SensorManager.SENSOR_DELAY_UI,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_GAME,
            SensorManager.SENSOR_DELAY_FASTEST};

    final String[] sensorDelayNameArray = new String[] {
            "SENSOR_DELAY_UI",
            "SENSOR_DELAY_NORMAL",
            "SENSOR_DELAY_GAME",
            "SENSOR_DELAY_FASTEST"};

    // SENSORS
    private SensorManager mSensorManager;
    private ArrayList<String> mNameSensorList;
    private boolean[] mSelectedSensorList;
    int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_settings);

        mNameSensorList = new ArrayList<String>();

        // SENSORS
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        mNameSensorList = new ArrayList<String>();
        mSelectedSensorList = new boolean[sensorList.size()];

        for (int i = 0; i < sensorList.size(); i++) {
            Sensor iSensor = sensorList.get(i);
            int sensorType = iSensor.getType();
            String sensorString = iSensor.getName() +
                    "\n" +
                    iSensor.getStringType().split("\\.")[iSensor.getStringType().split("\\.").length-1].toUpperCase();
            mNameSensorList.add(sensorString);
            switch (sensorType) {
                case Sensor.TYPE_ACCELEROMETER:
                    mSelectedSensorList[i] = true;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    mSelectedSensorList[i] = true;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mSelectedSensorList[i] = true;
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    mSelectedSensorList[i] = true;
                    break;
                case Sensor.TYPE_GAME_ROTATION_VECTOR:
                    mSelectedSensorList[i] = true;
                    break;
                case Sensor.TYPE_GRAVITY:
                    mSelectedSensorList[i] = true;
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    mSelectedSensorList[i] = true;
                    break;
                case Sensor.TYPE_PRESSURE:
                    mSelectedSensorList[i] = true;
                    break;
                case Sensor.TYPE_STEP_COUNTER:
                    mSelectedSensorList[i] = true;
                    break;
                case Sensor.TYPE_STEP_DETECTOR:
                    mSelectedSensorList[i] = true;
                    break;
                default:
                    mSelectedSensorList[i] = false;
                    break;
            }
        }

        Spinner spinner = (Spinner) findViewById(R.id.spinnerSensorDelay);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);
        for (int i = 0; i < sensorDelayNameArray.length; i++) {
            adapter.add(sensorDelayNameArray[i]);
        }
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                sensorDelay = sensorDelayArray[i];
                Log.i(TAG, "Sensor delay: " + sensorDelayNameArray[i]);
            }
            public void onNothingSelected(AdapterView<?> adapterView) {
                sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
            }
        });

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.sensorLinearLayout);
        for (int i = 0; i < mNameSensorList.size(); i++) {
            String iSensor = mNameSensorList.get(i);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(iSensor);
            checkBox.setId(i);
            if (mSelectedSensorList[i]) {
                checkBox.setChecked(true);
                Log.i(TAG, iSensor + " STARTS CHECKED");
            } else {
                checkBox.setChecked(false);
                Log.i(TAG, iSensor + " STARTS UNCHECKED");
            }
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Boolean checked = ((CheckBox) view).isChecked();
                    int checkBoxId = view.getId();
                    String SensorName = mNameSensorList.get(checkBoxId);
                    if (checked) {
                        Log.i(TAG, SensorName + " CHECKED");
                        mSelectedSensorList[checkBoxId] = true;
                    } else {
                        Log.i(TAG, SensorName + " UNCHECKED");
                        mSelectedSensorList[checkBoxId] = false;
                    }
                }
            });
            linearLayout.addView(checkBox);
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
            bundle.putBooleanArray("selectedSensors", mSelectedSensorList);
            bundle.putInt("sensorDelay", sensorDelay);
            returnIntent.putExtras(bundle);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    };
}

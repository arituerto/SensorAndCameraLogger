package arituerto.sensorandcameralogger;

import android.content.Intent;
import android.content.SharedPreferences;
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

    public static String SHRDPRFS_NAME = "SensorPrefs";
    public static String SNSSELECTION = "sensorSelection";
    public static String SNSDELAY = "sensorDelay";

    private SharedPreferences sharedPreferences;

    // SENSORS
    private SensorManager mSensorManager;
    private ArrayList<String> mNameSensorList;
    private boolean[] mSelectedSensorList;
    private int mSensorDelay;

    public String getSensorDelayName(int i) {
        String outString = "";
        switch (i) {
            case (SensorManager.SENSOR_DELAY_UI): outString =  "SENSOR_DELAY_UI"; break;
            case (SensorManager.SENSOR_DELAY_NORMAL): outString =  "SENSOR_DELAY_NORMAL"; break;
            case (SensorManager.SENSOR_DELAY_GAME): outString =  "SENSOR_DELAY_GAME"; break;
            case (SensorManager.SENSOR_DELAY_FASTEST): outString =  "SENSOR_DELAY_FASTEST"; break;
        }
        return outString;
    }

    public static void storeBooleanArray(String arrayName, boolean[] array, SharedPreferences sharedPreferences) {

        sharedPreferences.edit().putInt(arrayName +"_size", array.length).commit();

        for(int i=0;i<array.length;i++) {
            sharedPreferences.edit().putBoolean(arrayName + "_" + i, array[i]).commit();
        }
    }

    public static boolean[] loadBooleanArray(String arrayName, SharedPreferences sharedPreferences) {

        boolean[] array = null;

        if (sharedPreferences.contains(arrayName + "_size")) {
            int size = sharedPreferences.getInt(arrayName + "_size", 0);
            array = new boolean[size];
            for (int i = 0; i < size; i++)
                array[i] = sharedPreferences.getBoolean(arrayName + "_" + i, false);
        }

        return array;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_settings);

        mNameSensorList = new ArrayList<String>();

        // SENSORS
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        mNameSensorList = new ArrayList<String>();
        for (int i = 0; i < sensorList.size(); i++) {
            Sensor iSensor = sensorList.get(i);
            String sensorString = iSensor.getName() +
                    "\n" +
                    iSensor.getStringType().split("\\.")[iSensor.getStringType().split("\\.").length - 1].toUpperCase();
            mNameSensorList.add(sensorString);
        }

        sharedPreferences = getSharedPreferences(SHRDPRFS_NAME, MODE_PRIVATE);
        mSelectedSensorList = loadBooleanArray(SNSSELECTION, sharedPreferences);
        mSensorDelay = sharedPreferences.getInt(SNSDELAY, SensorManager.SENSOR_DELAY_NORMAL);

        if (mSelectedSensorList == null) {

            mSelectedSensorList = new boolean[sensorList.size()];

            for (int i = 0; i < sensorList.size(); i++) {
                Sensor iSensor = sensorList.get(i);
                int sensorType = iSensor.getType();
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
        }

        Spinner spinner = (Spinner) findViewById(R.id.spinnerSensorDelay);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);
        for (int i = 0; i < sensorDelayArray.length; i++) {
            adapter.add(getSensorDelayName(sensorDelayArray[i]));
        }
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getPosition(getSensorDelayName(mSensorDelay)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG, "Sensor delay: " + getSensorDelayName(sensorDelayArray[i]));
                mSensorDelay = sensorDelayArray[i];
            }
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.sensorLinearLayout);
        for (int i = 0; i < mNameSensorList.size(); i++) {
            String iSensor = mNameSensorList.get(i);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(iSensor);
            checkBox.setId(i);
            if (mSelectedSensorList[i]) {
                checkBox.setChecked(true);
            } else {
                checkBox.setChecked(false);
            }
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Boolean checked = ((CheckBox) view).isChecked();
                    int checkBoxId = view.getId();
                    if (checked) {
                        mSelectedSensorList[checkBoxId] = true;
                    } else {
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

            storeBooleanArray(SNSSELECTION, mSelectedSensorList, sharedPreferences);
            sharedPreferences.edit().putInt(SNSDELAY, mSensorDelay).commit();

            finish();
        }
    };
}

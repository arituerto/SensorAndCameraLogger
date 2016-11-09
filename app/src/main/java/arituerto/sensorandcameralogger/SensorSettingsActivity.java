package arituerto.sensorandcameralogger;

import android.content.Intent;
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

    // TODO: Keep track of checked/unchecked sensors. Best class??

    // SENSORS
    private ArrayList<String> mNameSensorList;
    private boolean[] mSelectedSensorList;
    private int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_settings);

        mNameSensorList = new ArrayList<String>();

        Bundle bundle = this.getIntent().getExtras();
        if(bundle != null) {
            mNameSensorList = bundle.getStringArrayList("allSensors");
            mSelectedSensorList = bundle.getBooleanArray("selectedSensors");
        }

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
        Spinner spinner = (Spinner) findViewById(R.id.spinnerSensorDelay);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);
        adapter.add("UI");
        adapter.add("NORMAL");
        adapter.add("GAME");
        adapter.add("FASTEST");
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
            } else {
                checkBox.setChecked(false);
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

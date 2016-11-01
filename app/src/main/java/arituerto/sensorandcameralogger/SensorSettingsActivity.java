package arituerto.sensorandcameralogger;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class SensorSettingsActivity extends AppCompatActivity {

    private static final String TAG = "SensorSettingsActivity";

    // TODO: Keep track of checked/unchecked sensors. Best class??

    // SENSORS
    private ArrayList<String> mNameSensorList;
    private boolean[] mSelectedSensorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, " onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_settings);

        mNameSensorList = new ArrayList<String>();

        Bundle bundle = this.getIntent().getExtras();
        if(bundle != null) {
            mNameSensorList = bundle.getStringArrayList("allSensors");
            mSelectedSensorList = bundle.getBooleanArray("selectedSensors");
        }
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

        final Button okButton = (Button) findViewById(R.id.buttonOK);
        okButton.setOnClickListener(okClick);
    }

    private View.OnClickListener okClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Sensor Settings OK");
            Intent returnIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putBooleanArray("selectedSensors", mSelectedSensorList);
            returnIntent.putExtras(bundle);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    };
}

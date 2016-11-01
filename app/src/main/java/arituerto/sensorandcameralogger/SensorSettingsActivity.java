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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorSettingsActivity extends AppCompatActivity {

    private static final String TAG = "SensorSettingsActivity";

    // TODO: Keep track of checked/unchecked sensors. Best class??

    // SENSORS
    private ArrayList<String> mAllSensors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, " onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_settings);

        mAllSensors = new ArrayList<String>();
        Bundle bundle = this.getIntent().getExtras();
        if(bundle != null) {
            mAllSensors = bundle.getParcelable("allSensors");
        }
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.sensorLinearLayout);
        for (String iSensor : mAllSensors) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(iSensor);
            checkBox.setChecked(true);
            checkBox.setTag(iSensor);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Boolean checked = ((CheckBox) view).isChecked();
                    String checkBoxTag = view.getTag().toString();
                    if (checked) {
                        Log.i(TAG, checkBoxTag + " CHECKED");
                    } else {
                        Log.i(TAG, checkBoxTag + " UNCHECKED");
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
            finish();
        }
    };
}

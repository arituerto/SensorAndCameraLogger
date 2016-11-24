package arituerto.sensorandcameralogger;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;

public class CPROSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CPROSettings";

    private String mCPRO_Rmac = "D3:27:08:FD:69:78";
    private String mCPRO_Lmac = "D0:72:37:14:3B:15";
    private float mCPROfreq = 100f;
    private boolean mCPROAccelerometer = true;
    private boolean mCPROGyroscope = true;
    private boolean mCPROBarometer = true;
    private boolean mCPROMagnetometer = false;
    private boolean mCPROThermometer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cprosettings);

        EditText textCPRO_Rmac = (EditText) findViewById(R.id.editTextCPRO_Rmac);
        textCPRO_Rmac.setText(mCPRO_Rmac);
        EditText textCPRO_Lmac = (EditText) findViewById(R.id.editTextCPRO_Lmac);
        textCPRO_Lmac.setText(mCPRO_Lmac);

        SeekBar freqBar = (SeekBar) findViewById(R.id.seekBarFreq);
        freqBar.setProgress((int) mCPROfreq);
        TextView progressText = (TextView) findViewById(R.id.textViewFreq);
        progressText.setText( mCPROfreq + " [Hz]");
        freqBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCPROfreq = progress;
                TextView progressText = (TextView) findViewById(R.id.textViewFreq);
                progressText.setText((float) progress + " [Hz]");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Switch tempSwitch = (Switch) findViewById(R.id.switchAccelerometer);
        tempSwitch.setChecked(mCPROAccelerometer);
        tempSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {mCPROAccelerometer = isChecked;}
        });
        tempSwitch = (Switch) findViewById(R.id.switchGyroscope);
        tempSwitch.setChecked(mCPROGyroscope);
        tempSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {mCPROGyroscope = isChecked;}
        });
        tempSwitch = (Switch) findViewById(R.id.switchBarometer);
        tempSwitch.setChecked(mCPROBarometer);
        tempSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {mCPROBarometer = isChecked;}
        });
        tempSwitch = (Switch) findViewById(R.id.switchMagnetometer);
        tempSwitch.setChecked(mCPROMagnetometer);
        tempSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {mCPROMagnetometer = isChecked;}
        });
        tempSwitch = (Switch) findViewById(R.id.switchThermometer);
        tempSwitch.setChecked(mCPROThermometer);
        tempSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {mCPROThermometer = isChecked;}
        });

        final Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(okClick);
    }

    private View.OnClickListener okClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "CPRO Settings OK");

            EditText textCPRO_Rmac = (EditText) findViewById(R.id.editTextCPRO_Rmac);
            mCPRO_Rmac = textCPRO_Rmac.getText().toString();
            EditText textCPRO_Lmac = (EditText) findViewById(R.id.editTextCPRO_Lmac);
            mCPRO_Lmac = textCPRO_Lmac.getText().toString();


            SeekBar freqBar = (SeekBar) findViewById(R.id.seekBarFreq);
            mCPROfreq = (float) freqBar.getProgress();

            Intent returnIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("CPRORmac", mCPRO_Rmac);
            bundle.putString("CPROLmac", mCPRO_Lmac);
            bundle.putFloat("CPROfreq", mCPROfreq);
            bundle.putBoolean("CPROAccelerometer", mCPROAccelerometer);
            bundle.putBoolean("CPROGyroscope", mCPROGyroscope);
            bundle.putBoolean("CPROBarometer", mCPROBarometer);
            bundle.putBoolean("CPROMagnetometer", mCPROMagnetometer);
            bundle.putBoolean("CPROThermometer", mCPROThermometer);
            returnIntent.putExtras(bundle);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    };
}

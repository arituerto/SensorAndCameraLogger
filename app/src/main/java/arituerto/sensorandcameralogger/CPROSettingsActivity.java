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
import android.widget.EditText;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;

public class CPROSettingsActivity extends AppCompatActivity implements ServiceConnection{

    private static final String TAG = "CPROSettings";

    private String mCPRO_Rmac = "D3:27:08:FD:69:78";
    private String mCPRO_Lmac = "D0:72:37:14:3B:15";
    private Float mCPROfreq = 100.0f;

    private MetaWearBleService.LocalBinder mServiceBinder;
    private BluetoothManager mBluetoothManager;
    private BluetoothDevice mBluetoothCPRO_R;
    private BluetoothDevice mBluetoothCPRO_L;
    private MetaWearBoard mBoard_R;
    private MetaWearBoard mBoard_L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cprosettings);

        // CPRO
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);

        EditText textCPRO_Rmac = (EditText) findViewById(R.id.editTextCPRO_Rmac);
        textCPRO_Rmac.setText(mCPRO_Rmac);
        EditText textCPRO_Lmac = (EditText) findViewById(R.id.editTextCPRO_Lmac);
        textCPRO_Lmac.setText(mCPRO_Lmac);

        final Button tryButton = (Button) findViewById(R.id.buttonTrySensors);
        tryButton.setOnClickListener(tryClick);

        final Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(okClick);
    }

    @Override
    public void onDestroy() {

        Log.i(TAG, "onDestroy");

        super.onDestroy();

        // CPRO
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mServiceBinder = (MetaWearBleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    private View.OnClickListener tryClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

            EditText textCPRO_Rmac = (EditText) findViewById(R.id.editTextCPRO_Rmac);
            mCPRO_Rmac = textCPRO_Rmac.getText().toString();
            EditText textCPRO_Lmac = (EditText) findViewById(R.id.editTextCPRO_Lmac);
            mCPRO_Lmac = textCPRO_Lmac.getText().toString();

            BluetoothDevice device = mBluetoothManager.getAdapter().getRemoteDevice(mCPRO_Rmac);
            MetaWearBoard board = mServiceBinder.getMetaWearBoard(device);
            board.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
                @Override
                public void connected() {
                    super.connected();
                    Toast.makeText(getApplicationContext(), "CPRO R connection OK!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void failure(int status, Throwable error) {
                    super.failure(status, error);
                    Toast.makeText(getApplicationContext(), "CPRO R connection Failed!", Toast.LENGTH_SHORT).show();
                }
            });
            board.connect();
            board.disconnect();

            device = mBluetoothManager.getAdapter().getRemoteDevice(mCPRO_Lmac);
            board = mServiceBinder.getMetaWearBoard(device);
            board.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
                @Override
                public void connected() {
                    super.connected();
                    Toast.makeText(getApplicationContext(), "CPRO L connection OK!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void failure(int status, Throwable error) {
                    super.failure(status, error);
                    Toast.makeText(getApplicationContext(), "CPRO L connection Failed!", Toast.LENGTH_SHORT).show();
                }
            });
            board.connect();
            board.disconnect();

        }
    };

    private View.OnClickListener okClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "CPRO Settings OK");

            EditText textCPRO_Rmac = (EditText) findViewById(R.id.editTextCPRO_Rmac);
            mCPRO_Rmac = textCPRO_Rmac.getText().toString();
            EditText textCPRO_Lmac = (EditText) findViewById(R.id.editTextCPRO_Lmac);
            mCPRO_Lmac = textCPRO_Lmac.getText().toString();

            Intent returnIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("CPRORmac", mCPRO_Rmac);
            bundle.putString("CPROLmac", mCPRO_Lmac);
            bundle.putFloat("CPROfreq", mCPROfreq);
            returnIntent.putExtras(bundle);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    };
}

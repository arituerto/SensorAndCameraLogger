package arituerto.sensorandcameralogger;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CPROSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CPROSettings";

    public static String SHRDPRFS_NAME = "cproPrefs";
    public static String CPRORMAC = "cproRmac";
    public static String CPROLMAC = "cproLmac";

    private SharedPreferences sharedPreferences;

    private String mCPRO_Rmac;
    private String mCPRO_Lmac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cprosettings);

        sharedPreferences = getSharedPreferences(SHRDPRFS_NAME, MODE_PRIVATE);
        mCPRO_Rmac = sharedPreferences.getString(CPRORMAC, "D3:27:08:FD:69:78");
        mCPRO_Lmac = sharedPreferences.getString(CPROLMAC, "D0:72:37:14:3B:15");

        EditText textCPRO_Rmac = (EditText) findViewById(R.id.editTextCPRO_Rmac);
        textCPRO_Rmac.setText(mCPRO_Rmac);
        EditText textCPRO_Lmac = (EditText) findViewById(R.id.editTextCPRO_Lmac);
        textCPRO_Lmac.setText(mCPRO_Lmac);

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

            sharedPreferences.edit().putString(CPRORMAC, mCPRO_Rmac).commit();
            sharedPreferences.edit().putString(CPROLMAC, mCPRO_Lmac).commit();

            finish();
        }
    };
}

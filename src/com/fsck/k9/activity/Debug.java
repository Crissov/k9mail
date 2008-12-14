
package com.fsck.k9.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.fsck.k9.k9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;

public class Debug extends Activity implements OnCheckedChangeListener {
    private TextView mVersionView;
    private CheckBox mEnableDebugLoggingView;
    private CheckBox mEnableSensitiveLoggingView;

    private Preferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.debug);

        mPreferences = Preferences.getPreferences(this);

        mVersionView = (TextView)findViewById(R.id.version);
        mEnableDebugLoggingView = (CheckBox)findViewById(R.id.debug_logging);
        mEnableSensitiveLoggingView = (CheckBox)findViewById(R.id.sensitive_logging);

        mEnableDebugLoggingView.setOnCheckedChangeListener(this);
        mEnableSensitiveLoggingView.setOnCheckedChangeListener(this);

        mVersionView.setText(String.format(getString(R.string.debug_version_fmt).toString(),
                getString(R.string.build_number)));

        mEnableDebugLoggingView.setChecked(k9.DEBUG);
        mEnableSensitiveLoggingView.setChecked(k9.DEBUG_SENSITIVE);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.debug_logging) {
            k9.DEBUG = isChecked;
            mPreferences.setEnableDebugLogging(k9.DEBUG);
        } else if (buttonView.getId() == R.id.sensitive_logging) {
            k9.DEBUG_SENSITIVE = isChecked;
            mPreferences.setEnableSensitiveLogging(k9.DEBUG_SENSITIVE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.dump_settings) {
            Preferences.getPreferences(this).dump();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.debug_option, menu);
        return true;
    }

}
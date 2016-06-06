package com.staf621.ki4a;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    protected static ImageButton button;
    protected static TextView text_status;
    protected static MainActivity myMainActivity;
    protected DataUpdateReceiver dataUpdateReceiver;

    // This Class is called from ki4aService to notify a status change
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ki4aService.REFRESH_STATUS_INTENT)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.refresh_status_img(ki4aService.current_status);
                    }
                });
            }
        }
    }

    // Menu Creator
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    // Handler for Config Icon Menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_config:
                Intent settingsIntent = new Intent().setClass(
                        MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            default:
                break;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        myMainActivity = this;

        if(ki4aService.toState == Util.STATUS_DISCONNECT) // First time we open the app (or disconnected)
        {
            // Let's start service at the beginning of the app
            ki4aService.toState = Util.STATUS_INIT; // Init
            Intent intent = new Intent(myMainActivity, ki4aService.class);
            myMainActivity.startService(intent);
        }

        button = (ImageButton) findViewById(R.id.imageButton_status);
        refresh_status_img(ki4aService.current_status);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean startKi4a = false;
                boolean iptables_switch = preferences.getBoolean("iptables_switch", false);

                if (!iptables_switch) {
                    // Ask for VPN permission
                    Intent intentVpn = VpnService.prepare(myMainActivity);
                    if (intentVpn != null) {
                        startActivityForResult(intentVpn, 0);
                    }
                    else
                        startKi4a = true;
                }
                else
                    startKi4a = true;

                if(startKi4a) {
                    Intent intent = new Intent(myMainActivity, ki4aService.class);
                    if (ki4aService.current_status == Util.STATUS_DISCONNECT) {
                        ki4aService.current_status = Util.STATUS_CONNECTING;
                        refresh_status_img(ki4aService.current_status);
                        ki4aService.toState = Util.STATUS_SOCKS;
                    } else if (ki4aService.current_status == Util.STATUS_CONNECTING ||
                            ki4aService.current_status == Util.STATUS_SOCKS
                            )
                        ki4aService.toState = Util.STATUS_DISCONNECT;

                    // Notify Service about the button being pushed
                    startService(intent);
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh the current status
        refresh_status_img(ki4aService.current_status);

        // Re-register to Service Updates
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(ki4aService.REFRESH_STATUS_INTENT);
        registerReceiver(dataUpdateReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister Service Updates
        if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);
    }

    /** Called when the activity is closed. */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected static void refresh_status_img(int status)
    {
        button = (ImageButton) myMainActivity.findViewById(R.id.imageButton_status);
        text_status = (TextView) myMainActivity.findViewById(R.id.textView_status);
        if(status==Util.STATUS_DISCONNECT)
        {
            text_status.setText(R.string.text_status_empty);
            button.setImageResource(R.drawable.status_red);
        }
        else if(status==Util.STATUS_INIT)
        {
            text_status.setText(R.string.text_status_initializing);
            button.setImageResource(R.drawable.status_gray);
        }
        else if(status==Util.STATUS_CONNECTING)
        {
            text_status.setText(R.string.text_status_connecting);
            button.setImageResource(R.drawable.status_orange);
        }
        else if(status==Util.STATUS_SOCKS)
        {
            text_status.setText(R.string.text_status_connected);
            button.setImageResource(R.drawable.status_blue);
        }
    }
}
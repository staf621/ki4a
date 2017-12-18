package com.staf621.ki4a;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.adclient.android.sdk.listeners.ClientAdListener;
import com.adclient.android.sdk.view.AbstractAdClientView;
import com.adclient.android.sdk.view.AdClientView;

public class MainActivity extends AppCompatActivity {

    protected static ImageButton button;
    protected static TextView text_status;
    protected static MainActivity myMainActivity;
    protected DataUpdateReceiver dataUpdateReceiver;
    protected static AdClientView adClientView;

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
            else if(intent.getAction().equals(ki4aService.ASK_FOR_PASS_INTENT)) {
                MyLog.d(Util.TAG,"Got intent for pass request!");
                ask_for_pass(context);
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
                    MyLog.i(Util.TAG, "Asking for permission to use VPN");
                    Intent intentVpn = VpnService.prepare(myMainActivity);
                    if (intentVpn != null) {
                        MyLog.i(Util.TAG, "First time VPN permission, asking...");
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

        adClientView = (AdClientView) findViewById(R.id.adClientView);
        adClientView.addClientAdListener(new ClientAdListener() {
            @Override
            public void onReceivedAd(AbstractAdClientView adClientView) {
                MyLog.d(Util.TAG, "Ad received callback.");
            }
            @Override
            public void onFailedToReceiveAd(AbstractAdClientView adClientView) {
                MyLog.d(Util.TAG, "Ad failed to be received callback.");
            }
            @Override
            public void onShowAdScreen(AbstractAdClientView adClientView) {
                MyLog.d(Util.TAG, "Ad show ad screen callback.");
            }
            @Override
            public void onLoadingAd(AbstractAdClientView adClientView, String
                    message) {
                MyLog.d(Util.TAG, "Ad loaded callback.");
            }
            @Override
            public void onClosedAd(AbstractAdClientView adClientView) {
                MyLog.d(Util.TAG, "Ad closed callback.");
            }
        });
        adClientView.load();
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
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ki4aService.REFRESH_STATUS_INTENT);
        intentFilter.addAction(ki4aService.ASK_FOR_PASS_INTENT);
        registerReceiver(dataUpdateReceiver, intentFilter);

        if (adClientView != null) {
            adClientView.resume();
        }
    }

    @Override
    public void onPause() {
        if (adClientView != null) {
            adClientView.pause();
        }
        super.onPause();
        // Unregister Service Updates
        if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);
    }

    /** Called when the activity is closed. */
    @Override
    public void onDestroy() {
        if (adClientView != null) {
            adClientView.destroy();
        }
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
            if (adClientView != null) {
                adClientView.load();
            }
        }
    }

    protected static void ask_for_pass(Context context)
    {
        final EditText pass = new EditText(context);
        int title = PreferenceManager.
                getDefaultSharedPreferences(context).
                getBoolean("key_switch", false) ?
                R.string.str_key_passphrase : R.string.pref_title_ssh_password;
        pass.setSingleLine(true);
        pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass.setImeOptions(EditorInfo.IME_ACTION_DONE);

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(pass)
                .setCancelable(false)
                .setPositiveButton(R.string.str_connect, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ki4aService.current_ssh_pass = pass.getText().toString();
                        ki4aService.got_ssh_pass = true;
                    }
                })
                .show();

        pass.setOnKeyListener(new TextView.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                }
                return false;
            }
        });
        pass.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                }
                return true;
            }
        });

        pass.requestFocus();
    }
}
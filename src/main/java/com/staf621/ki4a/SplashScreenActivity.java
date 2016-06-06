package com.staf621.ki4a;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Window;

public class SplashScreenActivity extends Activity {

    protected static String BASE = "/data/data/com.staf621.ki4a/ki4a";
    protected static String BASE_BIN = "/bin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set portrait orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Hide title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.splashscreen);

        TimerTask task = new TimerTask() {
            @Override
            public void run()
            {
                //Copy ki4a Assets, only if the directory bin does not exists
                if (!((new File(BASE + BASE_BIN)).exists()))
                    Util.CopyAssets(getApplicationContext().getAssets(), getPackageName());

                // Start the next activity
                Intent mainIntent = new Intent().setClass(
                        SplashScreenActivity.this, MainActivity.class);
                startActivity(mainIntent);

                // Close the activity so the user won't able to go back this
                // activity pressing Back button
                finish();
            }
        };

        Timer timer = new Timer();
        timer.schedule(task, 0);

    }
}
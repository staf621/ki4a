package com.staf621.ki4a;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;

public class ki4aUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Util.DeleteAssets();
        Util.CopyAssets(context.getAssets(), context.getPackageName());
    }
}

package com.staf621.ki4a;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

public class ki4aVPNService extends VpnService implements Runnable {

    protected static final String TAG = "ki4a";
    public static int FLAG_VPN_START = 0;
    public static int FLAG_VPN_STOP = 1;
    private ParcelFileDescriptor mInterface;
    private Thread mThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String prefix = getPackageName();
        int action = intent.getIntExtra(prefix + ".ACTION", 0);

        if(action==FLAG_VPN_START) {
            // Stop the previous session by interrupting the thread.
            if (mThread != null) {
                mThread.interrupt();
            }
            mThread = new Thread(this, "ki4aVPNThread");
            mThread.start();
        }
        else if(action==FLAG_VPN_STOP) {
            this.onDestroy();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Closing");

        if (mThread != null) {
            mThread.interrupt();
        }
        if (mInterface != null){
            try {
                mInterface.close();
            } catch (IOException e) { /* Ignore a close error here */ }
        }
    }

    @Override
    public void onRevoke() {
        // If vpn was revoked, let's close and destroy everything
        this.onDestroy();
        Util.reportRevoked(this);
    }

    @Override
    public void run() {
        Log.i(TAG, "Starting");
        if(protect_tunnel())
        {
            configure();
            ki4aService.vpn_ready = true; // Notify vpn is ready now
        }
        else {
            Log.i(TAG, "Failed to protect the tunnel");
            this.onDestroy();
            Util.reportDisconnection(this);
            stopSelf();
        }
    }

    private boolean protect_tunnel()
    {
        int fd;
        Log.i(TAG, "Protecting SSH fd");
        fd = Util.getSshfd();
        if(fd==0) return false;
        protect(fd);
        return true;
    }

    private void configure()
    {
        Log.i(TAG, "Configure");
        if (mInterface != null) {
            Log.i(TAG, "Using the previous interface");
            return;
        }

        Builder builder = new Builder();
        builder.setSession("ki4a");
        builder.setMtu(Util.tunVPN_MTU);
        builder.addAddress(Util.tunVPN_IP, Util.tunVPN_mask_num);
        builder.addDnsServer("8.8.8.8");
        builder.addRoute("0.0.0.0", 0); //Redirect all traffic
        builder.addRoute("8.8.0.0", 16); //Redirect DNS stuff

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            builder.allowFamily(android.system.OsConstants.AF_INET6);

        mInterface = builder.establish();

        if(!Util.run_tun2socks(mInterface.getFileDescriptor())) {
            // If we can not connect, let's close and destroy everything
            this.onDestroy();
            Util.reportDisconnection(this);
        }
    }
}
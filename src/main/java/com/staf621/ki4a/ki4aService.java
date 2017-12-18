package com.staf621.ki4a;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public class ki4aService extends Service {

    protected static String BASE = "/data/data/com.staf621.ki4a/ki4a";
    protected static String BASE_BIN = "/bin";

    protected static TelephonyManager telephonyManager;
    protected static WifiManager wifiManager;

    protected static int current_status = 0;
    protected static final int MAX_FAIL_ATTEMPTS_FOR_NEW = 3;
    protected static final int MAX_FAIL_ATTEMPTS_FOR_RECONNECT = 10;
    protected static int current_fail_attempt = 0;
    protected static final int MAX_RETRY = 30; // Aprox 60 secs
    protected static final int ASK_PASSWORD_TIMEOUT = 120; // 120 secs
    protected static int toState = 0;
    protected static final String REFRESH_STATUS_INTENT = "ki4aRefresh";
    protected static final String TOGGLE_STATUS_INTENT = "ki4aStatusToggle";
    protected static final String TOGGLE_KI4A_INTENT = TOGGLE_STATUS_INTENT;
    protected static final String START_KI4A_INTENT = "ki4aStatusStart";
    protected static final String STOP_KI4A_INTENT = "ki4aStatusStop";
    protected static final String ASK_FOR_PASS_INTENT = "ki4aAskForPass";
    protected static final int NOTIFICATION_ID = 7;
    protected static boolean first_connect = true;
    protected static boolean vpn_ready = false;
    protected static String current_ssh_pass;
    protected static boolean got_ssh_pass = false;
    protected static final int INCORRECT_PASSWORD = 5;
    protected static final int SSH_ERROR_CONNECTING = 255;
    protected static final String GOOGLE_DNS = "8.8.8.8";

    protected static SSHThread ssht;
    protected static Wait4connection w4c;
    protected static Thread w4cT;

    protected NotificationCompat.Builder notification;
    protected NotificationManager notificationManager;
    protected Context myContext;
    protected static SharedPreferences preferences;

    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
                Toast.makeText(getApplicationContext(),(String)msg.obj, Toast.LENGTH_LONG).show();
        }
    };


    protected class Wait4connection extends Thread
    {
        protected void reportConnected()
        {
            current_status = toState;
            // Only send Volatile notification if it's the first time connected
            showAToast(getString(R.string.text_status_connected), first_connect);

            MyLog.d(Util.TAG, "Wait4connection: connection detected" + (first_connect ? " [first connection]" : ""));
            if(first_connect) first_connect = false;

            notification.setContentText(getString(R.string.text_status_connected));
            current_fail_attempt = 0;

            Bitmap bm;
            Resources res = getResources();

            if(current_status==Util.STATUS_SOCKS) {
                bm = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res,R.drawable.status_blue_96),
                        (int) res.getDimension(android.R.dimen.notification_large_icon_height),
                        (int) res.getDimension(android.R.dimen.notification_large_icon_width), false);
                notification.setLargeIcon(bm);
            }

            notificationManager.notify(NOTIFICATION_ID, notification.build());

            notifyStatusChange(myContext);
        }

        @Override
        public void run() {
            boolean verify_internet_switch = PreferenceManager.
                    getDefaultSharedPreferences(myContext).
                    getBoolean("verify_internet_switch", true);

            MyLog.d(Util.TAG,"Starting new wait4connection");
            int i;

            if(!preferences.getBoolean("iptables_switch",false)) {
                for (i = 0; i < 20; i++) {
                    if (vpn_ready) break;
                    if (Thread.currentThread().isInterrupted()) return;
                    // Wait a little bit for Network to be ready
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            for (i = 0; i < MAX_RETRY; i++) {
                if (current_status == Util.STATUS_DISCONNECT) return; //We disconected
                if(Thread.currentThread().isInterrupted()) return;

                if(verify_internet_switch) {
                    MyLog.d(Util.TAG, "Wait4connection: verifying connectivity...");
                    if (Util.isOnline(myContext)) {
                        reportConnected();
                        return;
                    }
                }
                else {
                    MyLog.d(Util.TAG, "Wait4connection: verifying if socks proxy is open...");
                    if(Util.isSocksOpen(myContext)) {
                        reportConnected();
                        return;
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //Couldn't connect

            // If we are already on status disconnect, no need to cleanup
            if(current_status != Util.STATUS_DISCONNECT)
                stop_ki4aredirect();
        }
    }

    private class SSHThread extends Thread
    {
        @Override
        public void run() {
            boolean got_disconnected = false;
            int ssh_return_val = 0;
            current_fail_attempt = 0;
            boolean key_switch = preferences.getBoolean("key_switch",false);
            boolean ask_pass = preferences.getBoolean("ask_pass_switch",false);
            boolean enc_ssh_key = (key_switch && Util.isKeyEncrypted(preferences.getString("key_text", "")));

            //If we are asking for password, let's wait
            if(ask_pass || enc_ssh_key) {
                //Now, we wait for the pass
                for(int x=0; x< ASK_PASSWORD_TIMEOUT; x++)
                {
                    if(got_ssh_pass) break;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) { }
                }
            }

            while( current_status != Util.STATUS_DISCONNECT && (current_fail_attempt <
                    (first_connect ? MAX_FAIL_ATTEMPTS_FOR_NEW:MAX_FAIL_ATTEMPTS_FOR_RECONNECT)) ) {

                //We didn't receive the pass
                if((ask_pass || enc_ssh_key) && !got_ssh_pass) break;

                if(preferences.getBoolean("cellular_switch",false)) {
                    // If Telephony Manager says we are Disconnected and in the previous check we
                    // were disconnected for the same reason, let's break now
                    if (telephonyManager.getDataState() != telephonyManager.DATA_CONNECTED &&
                            telephonyManager.getDataState() != telephonyManager.DATA_CONNECTING &&
                            got_disconnected)
                        break;
                }

                got_disconnected = false;

                String server_text = preferences.getString("server_text", "");
                String user_text = preferences.getString("user_text", "");
                String password_text = preferences.getString("password_text", "");
                String proxy_host = preferences.getString("proxy_host", "");
                boolean compress = preferences.getBoolean("compress_switch", false);
                boolean proxy = preferences.getBoolean("proxy_switch", false);
                boolean iptables_switch = preferences.getBoolean("iptables_switch", false);
                boolean dns_switch = preferences.getBoolean("dns_switch",true);
                String dns_server = preferences.getString("dns_server", GOOGLE_DNS);
                String forward_string = ForwardList.getForwardString(myContext);
                int port_number = 22;
                int proxy_port = 80;

                try {
                    port_number = Integer.parseInt(preferences.getString("server_port", "22"));
                } catch (NumberFormatException e) {
                    MyLog.e(Util.TAG,"Error parsing ssh port number, using default (22)");
                }
                try {
                    proxy_port = Integer.parseInt(preferences.getString("proxy_port", "80"));
                } catch (NumberFormatException e) {
                    MyLog.e(Util.TAG,"Error parsing proxy port number, using default (80)");
                }

                //if ask_pass or private encrypted key we should get pass here
                if(ask_pass || enc_ssh_key) {
                    //We should have ssh pass inside current_ssh_pass instead of password_text
                    password_text = current_ssh_pass;
                }

                if(dns_switch) {
                    //Start DNS redirect
                    Util.runChainFireCommand(Util.BASE + Util.BASE_BIN + "/pdnsd -c " + Util.BASE + "/pdnsd.conf -d", false);
                }

                // If we are on VPN mode, we need to start it also
                if (!preferences.getBoolean("iptables_switch", false)) {
                    Util.startKi4aVPN(myContext, getPackageName());
                    vpn_ready = false;
                }

                // Stop the old wait 4 connection thread
                if(w4cT!=null) w4cT.interrupt();

                // Let's check if connection is active every time we reconnect
                w4cT = new Thread(w4c);
                w4cT.start();

                if(toState == Util.STATUS_SOCKS) {
                    ssh_return_val = Util.runChainFireCommand(
                            ((key_switch && !enc_ssh_key) ? "" : BASE + BASE_BIN + "/sshpass -p \"" + password_text + "\" ")
                                    + BASE + BASE_BIN + "/ssh " + server_text + " -p " + port_number + " -l " + user_text
                                    + " -NT -g -D " + Util.localSocksPort + (dns_switch?" -L 127.0.0.1:8163:"+dns_server+":53":"")
                                    + forward_string
                                    + (key_switch ? " -i \"" + BASE + "/id_rsa\"" : "")
                                    + (compress ? " -C" : "")
                                    + " -o \"ProxyCommand " + BASE + BASE_BIN + "/korkscrew"
                                    + (iptables_switch ? "" : " --ancillaryfile " + BASE + "/sshfd_file")
                                    + (proxy ? " --proxyhost " + proxy_host
                                    + " --proxyport " + proxy_port + " --desthost %h --destport %p"
                                    + " --headerfile " + BASE + "/header_file" + "\""
                                    : " --directconnection --desthost %h --destport %p\"")
                                    + " -o \"KeepAlive yes\" -o \"ServerAliveInterval 15\""
                                    + " -o \"StrictHostKeyChecking=no\" -o \"GlobalKnownHostsFile=/dev/null\"", false, true);
                }
                // Connection got closed

                // Stop the wait 4 connection thread
                if(w4cT!=null) w4cT.interrupt();

                if(ssh_return_val == INCORRECT_PASSWORD) {
                    got_ssh_pass = false;
                    current_ssh_pass = "";
                    showAToast(getString(R.string.str_incorrect_password),false);
                    break;
                }

                if(current_status == Util.STATUS_DISCONNECT) // We were told to disconnect
                    break;

                // Do not count reconnect as failed attempt if Telephony Manager says we are connecting
                if(telephonyManager.getDataState() != telephonyManager.DATA_CONNECTING)
                    current_fail_attempt++;

                if(!preferences.getBoolean("reconnect_switch",true)) break; // We do not want to reconnect

                if(preferences.getBoolean("cellular_switch",false)) {
                    // We disconnected because Telephony Manager says we are disconnected
                    // let's flag it and sleep 10 more secs.
                    if (telephonyManager.getDataState() != telephonyManager.DATA_CONNECTED &&
                            telephonyManager.getDataState() != telephonyManager.DATA_CONNECTING) {
                        got_disconnected = true;
                        // Let's wait a little before retrying
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                notification.setContentText(getString(R.string.text_status_connecting));
                Resources res = getResources();
                Bitmap bm = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res,R.drawable.status_orange_96),
                        (int) res.getDimension(android.R.dimen.notification_large_icon_height),
                        (int) res.getDimension(android.R.dimen.notification_large_icon_width), false);
                notification.setLargeIcon(bm);

                notificationManager.notify(NOTIFICATION_ID, notification.build());

                current_status = Util.STATUS_CONNECTING; //reconnecting
                showAToast(getString(R.string.text_status_reconnecting),false);

                notifyStatusChange(myContext);

                // If we are on VPN mode, we need to restart it also
                if (!preferences.getBoolean("iptables_switch", false))
                    Util.stopKi4aVPN(myContext, getPackageName());

                // Stop DNS redirect
                Util.runChainFireCommand(Util.BASE + Util.BASE_BIN + "/busybox killall pdnsd", false);

                // Let's wait a little before retrying
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // If we are already on status disconnect, no need to cleanup
            if(current_status != Util.STATUS_DISCONNECT)
                stop_ki4aredirect();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        myContext = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        telephonyManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);

        ssht = new SSHThread();
        w4c = new Wait4connection();
        cleanAll();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = new NotificationCompat.Builder(this)
                .setContentTitle("Ki4a")
                .setContentText("Init!")
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(pIntent)
                .setOngoing(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        MyLog.d(Util.TAG, "Service received a request! toState=" + toState + ", current_status=" + current_status);
        if(toState==Util.STATUS_INIT) // This call was made by App the first time it's started
        {
            // Do nothing
        }
        else if(toState==Util.STATUS_DISCONNECT) // We received a disconnect
        {
            stop_ki4aredirect();
        }
        else if(toState==Util.STATUS_SOCKS) // We received a connect
        {
            first_connect = true; // Reset flag for first connect notification
            notification.setContentText(getString(R.string.text_status_connecting));
            Resources res = getResources();
            Bitmap bm = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.status_orange_96),
                    (int) res.getDimension(android.R.dimen.notification_large_icon_height),
                    (int) res.getDimension(android.R.dimen.notification_large_icon_width), false);
            notification.setLargeIcon(bm);

            startForeground(NOTIFICATION_ID, notification.build());
            start_socks();
        }

        return START_NOT_STICKY;
        //return START_STICKY;
        //return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {

        Util.runChainFireCommand(BASE + BASE_BIN + "/busybox killall -9 pdnsd", true); //kill DNS
        cleanAll();

        MyLog.d(Util.TAG, "Destroy service called!");
    }

    protected void start_socks()
    {
        boolean iptables_switch = preferences.getBoolean("iptables_switch", false);

        //If we want to use iptables and we do not have root access, we can't connect
        if (iptables_switch && !Util.hasRoot()) {
            current_status = Util.STATUS_DISCONNECT; //Disconnect
            notifyStatusChange(myContext);
            ki4aQuickSettingsService.updateTile(ki4aQuickSettingsService.tileService);
            showAToast(getString(R.string.not_root_access), false);
            stopForeground(true);
            return;
        }

        if(preferences.getBoolean("cellular_switch",false))
            Util.prepareInterfaces(wifiManager, telephonyManager);

        if(iptables_switch) {
            String out_ip;
            String out_host;
            boolean proxy = preferences.getBoolean("proxy_switch",false);
            String proxy_host = preferences.getString("proxy_host", "");
            String server_text = preferences.getString("server_text", "");

            //If the proxy is set, we avoid redirecting proxy, else we avoid redirecting ssh host
            if (proxy)
                out_host = proxy_host;
            else
                out_host = server_text;

            try {
                InetAddress ia = InetAddress.getByName(out_host);
                String ip = ia.getHostAddress();
                if (ip != null && !ip.equals("")) {
                    out_ip = ip;
                    Util.runChainFireRootCommand(BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -d " + out_ip + " -j RETURN", true); //out ip
                } else
                    MyLog.e(Util.TAG, "Got Null hostname IP");
            } catch (UnknownHostException e) {
                MyLog.e(Util.TAG, "Can not get hostname IP");
            }

            //Start iptables Redirect
            Util.runChainFireRootCommand(BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -d 0.0.0.0/8 -j RETURN;" +
                            BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -d 10.0.0.0/8 -j RETURN;" +
                            BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -d 127.0.0.0/8 -j RETURN;" +
                            BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -d 169.254.0.0/16 -j RETURN;" +
                            BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -d 172.16.0.0/12 -j RETURN;" +
                            BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -d 192.168.0.0/16 -j RETURN;" +
                            BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -d 224.0.0.0/4 -j RETURN;" +
                            BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -d 240.0.0.0/4 -j RETURN;" +
                            BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -p udp --dport 53 -j " +
                                    (preferences.getBoolean("dns_switch",true)? "REDIRECT --to-ports 8153;": "RETURN;") +
                            BASE + BASE_BIN + "/iptables -t nat -A OUTPUT -p tcp -j REDIRECT --to-ports 8123;" +
                            BASE + BASE_BIN + "/iptables -t filter -F FORWARD;" + // Hotspot sharing rules
                            BASE + BASE_BIN + "/iptables -t nat -F POSTROUTING;" +
                            BASE + BASE_BIN + "/iptables -t nat -F PREROUTING;" +
                            BASE + BASE_BIN + "/iptables -t nat -I PREROUTING -p udp --dport 53 -j REDIRECT --to-ports 8153;" +
                            BASE + BASE_BIN + "/iptables -t nat -I PREROUTING -p tcp -j REDIRECT --to-ports 8123;" +
                            BASE + BASE_BIN + "/iptables -t filter -I FORWARD -j ACCEPT;" +
                            BASE + BASE_BIN + "/iptables -t nat -I POSTROUTING -j MASQUERADE",
                    true
            );

            Util.runChainFireRootCommand(BASE + BASE_BIN + "/redsocks -c " + BASE + "/redsocks.conf &", true);
        }

        //If ask_pass_switch is set or
        //ff we are using private key, let's check if it's encrypted, so we can ask for passphrase
        if(!got_ssh_pass &&
                (preferences.getBoolean("ask_pass_switch",false) ||
                    (preferences.getBoolean("key_switch",false) &&
                        Util.isKeyEncrypted(preferences.getString("key_text", ""))
                    )
                )
           )
        {
            //I need to ask for pass before trying to create SSH Thread
            // Notify MainActivity about ask for pass
            sendBroadcast(new Intent(ASK_FOR_PASS_INTENT));

        }

        new Thread(ssht).start();

    }

    protected void stop_ki4aredirect()
    {
        boolean iptables_switch = preferences.getBoolean("iptables_switch", false);
        current_status = Util.STATUS_DISCONNECT; //Disconnect
        notifyStatusChange(myContext);
        showAToast(getString(R.string.text_status_disconnected), true);

        //if we have pass saved, let's erase it unless it's a key passphrase
        if(!preferences.getBoolean("key_switch",false)) {
            got_ssh_pass = false;
            current_ssh_pass = "";
        }

        if (w4cT != null) {
            w4cT.interrupt();
        }

        Util.runChainFireCommand(BASE + BASE_BIN + "/busybox killall -9 korkscrew;" + // Stop korkscrew
                BASE + BASE_BIN + "/busybox killall -9 ssh;" + // Stop SSH
                BASE + BASE_BIN + "/busybox killall -9 sshpass;" +
                BASE + BASE_BIN + "/busybox killall pdnsd", true); // Stop DNS redirect

        if(iptables_switch) {
            Util.runChainFireRootCommand(BASE + BASE_BIN + "/busybox killall -9 redsocks;" + //kill everyting using redsocks
                    BASE + BASE_BIN + "/iptables -t nat -F OUTPUT;" + //Restore iptables Redirect
                    BASE + BASE_BIN + "/iptables -t filter -F FORWARD;" +
                    BASE + BASE_BIN + "/iptables -t nat -F POSTROUTING;" +
                    BASE + BASE_BIN + "/iptables -t nat -F PREROUTING", true);
        }
        else {
            Util.stopKi4aVPN(this, getPackageName());
        }

        stopForeground(true);
    }

    protected void cleanAll() {
        boolean iptables_switch = preferences.getBoolean("iptables_switch", false);

        Util.runChainFireCommand(BASE + BASE_BIN + "/busybox killall -9 korkscrew;" + // Stop korkscrew
                BASE + BASE_BIN + "/busybox killall -9 ssh;" + // Stop SSH
                BASE + BASE_BIN + "/busybox killall pdnsd", true); // Stop DNS redirect

        if(iptables_switch) {
            Util.runChainFireRootCommand(BASE + BASE_BIN + "/busybox killall -9 redsocks;" + //kill everyting using redsocks
                    BASE + BASE_BIN + "/iptables -t nat -F OUTPUT;" + //Restore iptables Redirect
                    BASE + BASE_BIN + "/iptables -t filter -F FORWARD;" +
                    BASE + BASE_BIN + "/iptables -t nat -F POSTROUTING;" +
                    BASE + BASE_BIN + "/iptables -t nat -F PREROUTING", true);
        }
    }

    protected static void notifyStatusChange(Context context) {
        // Notify MainActivity about status change
        context.sendBroadcast(new Intent(REFRESH_STATUS_INTENT));

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // Notify QS about status change
            ki4aQuickSettingsService.updateTile(ki4aQuickSettingsService.tileService);
        }
    }

    protected void showAToast(String message, boolean volatile_notify) {
        boolean notification_switch = preferences.getBoolean("notification_switch", true);
        if(notification_switch) {
            Message msg = handler.obtainMessage();
            msg.obj = message;
            handler.sendMessage(msg);

            MyLog.d(Util.TAG, message);

            if (volatile_notify) {
                // Any notification Id between 10 and 10000000
                int notifyId = (new Random()).nextInt(9999990) + 10;

                // Volatile Notification for wearables
                notification.setContentText(message);
                notificationManager.notify(notifyId, notification.build());
                notificationManager.cancel(notifyId);
            }
        }
    }
}
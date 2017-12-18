package com.staf621.ki4a;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

@SuppressLint("Override")
@TargetApi(Build.VERSION_CODES.N)
public class ki4aQuickSettingsService
        extends TileService {

    protected static TileService tileService;

    @Override
    public void onStartListening() {
        if(tileService==null) tileService = this;
        MyLog.d(Util.TAG, "Ki4a Start Tile listening");
    }

    @Override
    public void onClick() {
        MyLog.d(Util.TAG, "Ki4a Tile tapped");
        if(tileService==null) tileService = this;

        /* send toggle intent */
        Intent serviceIntent = new Intent(this, ki4aService.class);
        if (ki4aService.current_status == Util.STATUS_DISCONNECT) {
            ki4aService.current_status = Util.STATUS_CONNECTING;
            // Update status!
            ki4aService.toState = Util.STATUS_SOCKS;
        } else if (ki4aService.current_status == Util.STATUS_CONNECTING ||
                ki4aService.current_status == Util.STATUS_SOCKS
                )
            ki4aService.toState = Util.STATUS_DISCONNECT;
        this.startService(serviceIntent);

        updateTile(this);
    }

    protected static void updateTile(TileService context) {
        if(context == null) return;

        Tile tile = context.getQsTile();

        if (ki4aService.current_status == Util.STATUS_SOCKS) {
            tile.setLabel(context.getString(R.string.text_status_connected));
            tile.setState(Tile.STATE_ACTIVE);
        }
        else if (ki4aService.current_status == Util.STATUS_CONNECTING) {
            tile.setLabel(context.getString(R.string.text_status_connecting));
            tile.setState(Tile.STATE_INACTIVE);
        }
        else if (ki4aService.current_status == Util.STATUS_DISCONNECT) {
            tile.setLabel(context.getString(R.string.text_status_disconnected));
            tile.setState(Tile.STATE_INACTIVE);
        }

        tile.updateTile();
    }
}

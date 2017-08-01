package com.staf621.ki4a;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class ki4aWidget extends AppWidgetProvider {

    public static String ki4aWidgetIntent = "ki4aWidgetIntent";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.ki4a_widget);

        refreshIcon(context, views,ki4aService.current_status);

        Intent newIntent = new Intent(context, ki4aWidget.class);
        newIntent.setAction(ki4aWidgetIntent);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, newIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_status, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void refreshIcon(Context context, RemoteViews views, int status)
    {
        MyLog.d(Util.TAG,"ToStatus = "+status);
        final float scale = context.getResources().getDisplayMetrics().density;
        int currentIcon = (status == Util.STATUS_DISCONNECT ?
                        R.drawable.status_red:
                    status == Util.STATUS_CONNECTING?
                        R.drawable.status_orange:
                    status == Util.STATUS_SOCKS?
                        R.drawable.status_blue: R.drawable.status_gray);

        BitmapDrawable bd;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            bd = (BitmapDrawable)(context.getResources().getDrawable(currentIcon,null));
        } else {
            bd = (BitmapDrawable)(context.getResources().getDrawable(currentIcon));
        }

        if(bd!=null)
        {
            views.setImageViewBitmap(R.id.widget_status,
                    Bitmap.createScaledBitmap(bd.getBitmap(),
                            (int) (60 * scale + 0.5f),
                            (int) (60 * scale + 0.5f), false));
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.ki4a_widget);
        if(intent.getAction().equals(ki4aWidgetIntent)) {
            Intent serviceIntent = new Intent(context, ki4aService.class);
            if (ki4aService.current_status == Util.STATUS_DISCONNECT) {
                ki4aService.current_status = Util.STATUS_CONNECTING;
                refreshIcon(context, views, ki4aService.current_status);
                ki4aService.toState = Util.STATUS_SOCKS;
            } else if (ki4aService.current_status == Util.STATUS_CONNECTING ||
                    ki4aService.current_status == Util.STATUS_SOCKS
                    )
                ki4aService.toState = Util.STATUS_DISCONNECT;

            // Notify Service about the button being pushed
            context.startService(serviceIntent);

            ComponentName componentName = new ComponentName(context, ki4aWidget.class);
            AppWidgetManager.getInstance(context).updateAppWidget(componentName, views);
        } else if(intent.getAction().equals(ki4aService.REFRESH_STATUS_INTENT)) {
            refreshIcon(context, views, ki4aService.current_status);

            ComponentName componentName = new ComponentName(context, ki4aWidget.class);
            AppWidgetManager.getInstance(context).updateAppWidget(componentName, views);
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {

    }
}


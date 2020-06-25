package org.ndroi.easy163.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;

import org.ndroi.easy163.R;
import org.ndroi.easy163.core.Server;

public class ServerService extends Service
{
    private NotificationManager notificationManager;
    private String notificationId = "easy163Id";
    private String notificationName = "easy163Name";

    @Override
    public IBinder onBind(Intent intent)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Server.getInstance().start();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(notificationId, notificationName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.icon);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.icon)
                .setLargeIcon(icon)
                .setContentTitle("Easy163")
                .setContentText("Easy163 服务正在运行...");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            builder.setChannelId(notificationId);
        }
        Notification notification = builder.build();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopForeground(true);
    }
}